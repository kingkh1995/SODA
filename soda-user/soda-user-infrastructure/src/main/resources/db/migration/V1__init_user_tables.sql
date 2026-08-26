-- soda-user 写侧初始 schema
-- 决策依据：ADR-0004 / 0011 / 0017 / 0005 / 0023 / 0022；表名单数（kk-ddd 谱系），`user` 保留字一律反引号
-- 开发阶段 H2 MODE=MySQL only（DATABASE_TO_LOWER=TRUE），无外键 / 存储过程 / 视图；schema 变更改本文件 V1，禁增 V2/V3

CREATE TABLE `user`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户 ID（服务端生成）',
    `username`            VARCHAR(30) NULL COMMENT '用户名（全局唯一，可空——注销终态 R 行键释放后置空，恢复时领域补默认值 removed，ADR-0023）',
    `nickname`            VARCHAR(30)  NOT NULL COMMENT '昵称',
    `mobile`              VARCHAR(20) NULL COMMENT '手机号（全局唯一，可空；SmsAuthAccount 由此列派生）',
    `email`               VARCHAR(100) NULL COMMENT '邮箱（全局唯一，可空；EmailAuthAccount 由此列派生）',
    `sex`                 VARCHAR(1) NULL COMMENT '性别（M/F，ADR-0005 枚举短名）',
    `avatar`              VARCHAR(500) NULL COMMENT '头像 URI',
    `state`               VARCHAR(1)   NOT NULL COMMENT '用户状态（E/D/R，ADR-0017 终态 R 可持久化）',
    `password_hash`       VARCHAR(200) NOT NULL COMMENT '密码哈希（PasswordAuthAccount 落此列，ADR-0004 单表化修订）',
    `sms_login_enabled`   BOOLEAN      NOT NULL COMMENT '手机号登录开关（SmsAuthAccount.active；0=关闭手机号登录，手机号仍为账号标识，支付宝/阿里云模式）',
    `email_login_enabled` BOOLEAN      NOT NULL COMMENT '邮箱登录开关（EmailAuthAccount.active；0=关闭邮箱登录，邮箱仍为账号标识）',
    `version`             INT          NOT NULL COMMENT '乐观锁版本号（JPA @Version 自动校验递增）',
    `created_date`        DATETIME     NOT NULL COMMENT '创建时间（审计，Spring Data auditing 维护，UTC 字面值；无 DB 默认值——CURRENT_TIMESTAMP 按会话时区生成会漂移，由应用恒填充）',
    `last_modified_date`  DATETIME     NOT NULL COMMENT '最后更新时间（审计，Spring Data auditing 维护，UTC 字面值；无 DB 默认值/ON UPDATE——避免会话时区漂移）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_mobile` (`mobile`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户聚合主表';

CREATE TABLE `verification`
(
    `id`                 VARCHAR(36)  NOT NULL COMMENT '验证实体 ID（Uuid）',
    `subject`    VARCHAR(120) NOT NULL COMMENT '验证主体（裸键，无类型前缀：UCC/ULG/UPR=userId 串、URG=端点值串——subject 即唯一索引的槽位占用者，ADR-0026）',
    `scene`      VARCHAR(4)   NOT NULL COMMENT '验证场景（UCC/UPR/ULG/URG 扁平助记码，ADR-0005）',
    `channel`    VARCHAR(1)   NOT NULL COMMENT '投递通道（S=短信/E=邮箱；ADR-0005 枚举短名；channel 独立列 + target 裸值列共同还原 VerificationRecipient，ADR-0026）',
    `target`     VARCHAR(100) NOT NULL COMMENT '投递地址（手机号/邮箱裸值）',
    `code`               VARCHAR(10)  NOT NULL COMMENT '验证码',
    `state`              VARCHAR(1)   NOT NULL COMMENT '验证状态（I/P/V/U，无 E 态，过期派生判断）',
    `expire_at`          DATETIME     NOT NULL COMMENT '过期时间（UTC）',
    `active_key` VARCHAR(160) NULL COMMENT '活跃键（source.compositeKey() = scene:subject；I/P 且未过期占槽，U 终态迁移清 NULL（V 内存瞬态不落库不涉槽位）、过期 I/P 行 DELETE 释放；uk_active_key 硬保证单活跃）',
    `created_date`       DATETIME     NOT NULL COMMENT '创建时间（审计，Spring Data auditing 维护，UTC 字面值；无 DB 默认值——CURRENT_TIMESTAMP 按会话时区生成会漂移，由应用恒填充）',
    `last_modified_date` DATETIME     NOT NULL COMMENT '最后更新时间（审计，Spring Data auditing 维护，UTC 字面值；无 DB 默认值/ON UPDATE——避免会话时区漂移）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_active_key` (`active_key`),
    KEY                  `idx_subject_scene_state_expire_at` (`subject`, `scene`, `state`, `expire_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '验证码实体表（source 必填；唯一性由 uk_active_key = scene:subject 硬保证，终态不参与唯一）';

CREATE TABLE `user_archive`
(
    `id`           BIGINT      NOT NULL COMMENT '主键 = 原用户 ID（表名已含 user 语义；1:1——注销为吸收态终态，每用户至多一条归档；无 DB 外键，阿里手册强制）',
    `username`     VARCHAR(30) NOT NULL COMMENT '注销前用户名（原键快照）',
    `mobile`       VARCHAR(20) NULL COMMENT '注销前手机号（原键快照）',
    `email`        VARCHAR(100) NULL COMMENT '注销前邮箱（原键快照）',
    `archive_time` DATETIME    NOT NULL COMMENT '归档时间（insert-only 表无 update 概念，单一时间戳；Spring Data auditing 填充（@CreatedDate），UTC 字面值；无 DB 默认值——避免会话时区漂移）',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户注销归档审计表（ADR-0023：D→R 同事务写入原键快照，纯审计存储 insert-only，期满清除为演进路径）';

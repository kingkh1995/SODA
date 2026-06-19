## Parent

`.scratch/soda-user/PRD.md`

## What to build

Query Server 的 DAO 层。复用 `soda-user-infrastructure` 的 `UserMapper`，追加读侧特有的查询方法。传统三层架构的底层。

**`soda-user-query-server` 模块：**
- 依赖 `soda-user-infrastructure`（复用 Mapper）+ `soda-user-api`（复用 DTO）
- 不依赖 `soda-user-domain` / `soda-component-support`

**无新 DAO/PO 创建**，直接在 `UserMapper` 中追加（或在 qurery-server 包下声明新的 Mapper 接口引用同一表）：

```java
// 在 UserMapper 中追加，或新建 UserQueryMapper 引用同一 XML 映射
@Mapper
interface UserQueryMapper {
    UserDTO selectById(@Param("id") Long id);
    List<UserDTO> selectPage(PageParam page, @Param("username") String username,
                             @Param("mobile") String mobile, @Param("status") Integer status);
    long selectCount(PageParam page, ...);
}
```

DTO 直接映射行数据，无领域对象转换。

## Acceptance criteria

- [ ] DAO 编译通过
- [ ] 集成测试验证分页查询、详情查询、多条件过滤
- [ ] 不依赖 domain 模块

## Blocked by

`10-user-password-persistence.md`（需要 system_user 表存在）

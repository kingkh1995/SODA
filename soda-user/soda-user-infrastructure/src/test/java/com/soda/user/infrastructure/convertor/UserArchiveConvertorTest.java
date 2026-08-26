package com.soda.user.infrastructure.convertor;

import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.PasswordHash;
import com.soda.component.domain.types.Version;
import com.soda.user.domain.PasswordAuthAccount;
import com.soda.user.domain.User;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserState;
import com.soda.user.domain.types.Username;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link UserArchiveConvertor} 单向转换单测（归档快照，ADR-0023）。
 */
@DisplayName("UserArchiveConvertor 转换")
class UserArchiveConvertorTest {

    @Test
    @DisplayName("toPersistence：捕获释放前原键（ADR-0023），id = 原用户 ID")
    void should_archivePersistOriginalKeys() {
        var user = User.builder()
                .id(new UserId(46L))
                .version(Version.of(1))
                .username(new Username("grace"))
                .nickname(new Nickname("Grace"))
                .state(UserState.D)
                .mobile(Mobile.of("13900139003"))
                .email(Email.of("grace@test.com"))
                .passwordAccount(PasswordAuthAccount.builder()
                        .id(PasswordAuthAccountId.from(new UserId(46L)))
                        .active(Active.TRUE)
                        .passwordHash(PasswordHash.of("$2a$10$hash"))
                        .build())
                .build();

        var archive = UserArchiveConvertor.toPersistence(user);
        // 主键 = 原用户 ID；三键为释放前快照（此后 R 行键置空，原值仅存归档表）
        assertThat(archive.getId()).isEqualTo(46L);
        assertThat(archive.getUsername()).isEqualTo("grace");
        assertThat(archive.getMobile()).isEqualTo("13900139003");
        assertThat(archive.getEmail()).isEqualTo("grace@test.com");
    }
}

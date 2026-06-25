package com.soda.user.domain;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.soda.component.support.types.RandomString;
import com.soda.user.domain.enums.AuthAccountType;
import com.soda.user.domain.enums.Sex;
import com.soda.user.domain.enums.SocialType;
import com.soda.user.domain.enums.UserStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 在不同 Jackson {@link ObjectMapper} 配置下验证 user 模块 DP 的 JSON round-trip 稳定性。
 * <p>
 * 基线配置注册了 {@link JavaTimeModule}（生产环境必须），其余配置在其基础上叠加。
 */
class DpJacksonRoundTripTest {

    private static final ObjectMapper BASE = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    static Stream<ObjectMapper> mappers() {
        return Stream.of(
                BASE,                                                                         // 基线 — 默认 + JavaTimeModule
                BASE.copy()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true),  // 严格模式
                BASE.copy()
                        .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING),            // 枚举写 toString
                BASE.copy()
                        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)           // 禁止 null → 基本类型
        );
    }

    // ─── Record 单字段 DP ───

    @ParameterizedTest
    @MethodSource("mappers")
    void userId(ObjectMapper mapper) throws Exception {
        var original = new UserId(42L);
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, UserId.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void nickname(ObjectMapper mapper) throws Exception {
        var original = new Nickname("Alice");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, Nickname.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void username(ObjectMapper mapper) throws Exception {
        var original = new Username("test1234");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, Username.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void avatar(ObjectMapper mapper) throws Exception {
        var original = new Avatar("https://example.com/avatar.png");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, Avatar.class);
        assertEquals(original, restored);
    }

    // ─── 多字段 Record DP ───

    @ParameterizedTest
    @MethodSource("mappers")
    void verificationCode(ObjectMapper mapper) throws Exception {
        var original = new VerificationCode("123456", Instant.now().plusSeconds(300), false);
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, VerificationCode.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void verificationCodePolicy(ObjectMapper mapper) throws Exception {
        var original = new VerificationCodePolicy(6, Duration.ofMinutes(5));
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, VerificationCodePolicy.class);
        assertEquals(original, restored);
    }

    // ─── 密封继承 DP ───

    @ParameterizedTest
    @MethodSource("mappers")
    void passwordAuthAccountId(ObjectMapper mapper) throws Exception {
        var original = PasswordAuthAccountId.of("P:42");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, PasswordAuthAccountId.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void smsAuthAccountId(ObjectMapper mapper) throws Exception {
        var original = SmsAuthAccountId.of("S:13800138000");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, SmsAuthAccountId.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void emailAuthAccountId(ObjectMapper mapper) throws Exception {
        var original = EmailAuthAccountId.of("E:user@example.com");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, EmailAuthAccountId.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void socialAuthAccountId(ObjectMapper mapper) throws Exception {
        var original = SocialAuthAccountId.of("O:GE:12345");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, SocialAuthAccountId.class);
        assertEquals(original, restored);
    }

    // ─── 枚举 DP ───

    @ParameterizedTest
    @MethodSource("mappers")
    void authAccountType(ObjectMapper mapper) throws Exception {
        var original = AuthAccountType.P;
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, AuthAccountType.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void sex(ObjectMapper mapper) throws Exception {
        var original = Sex.M;
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, Sex.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void userStatus(ObjectMapper mapper) throws Exception {
        var original = UserStatus.E;
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, UserStatus.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void socialType(ObjectMapper mapper) throws Exception {
        var original = SocialType.GE;
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, SocialType.class);
        assertEquals(original, restored);
    }
}

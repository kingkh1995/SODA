package com.soda.component.support.types;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 在不同 Jackson {@link ObjectMapper} 配置下验证 DP 的 JSON round-trip 稳定性。
 * <p>
 * 目标：确保 DP 在「非极端情况」下不受 ObjectMapper 配置影响。
 */
class DpJacksonRoundTripTest {

    /**
     * 返回不同 ObjectMapper 配置的流，每种配置下对多种 DP 执行 round-trip。
     */
    static Stream<ObjectMapper> mappers() {
        return Stream.of(
                new ObjectMapper(),                                                           // 基线 — 默认配置
                new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true),  // 严格模式
                new ObjectMapper()
                        .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING),            // 枚举写 toString
                new ObjectMapper()
                        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)           // 禁止 null → 基本类型
        );
    }

    // ─── Record 单字段 DP ───

    @ParameterizedTest
    @MethodSource("mappers")
    void longId(ObjectMapper mapper) throws Exception {
        var original = new LongId(42);
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, LongId.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void mobile(ObjectMapper mapper) throws Exception {
        var original = new Mobile("13800138000");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, Mobile.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void uuId(ObjectMapper mapper) throws Exception {
        var original = new UUId("550e8400-e29b-41d4-a716-446655440000");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, UUId.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void randomString(ObjectMapper mapper) throws Exception {
        var original = new RandomString("abc123");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, RandomString.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void credentialHash(ObjectMapper mapper) throws Exception {
        var original = new CredentialHash("$2a$10$abc123");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, CredentialHash.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void smsContent(ObjectMapper mapper) throws Exception {
        var original = new SmsContent("您的验证码是 123456");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, SmsContent.class);
        assertEquals(original, restored);
    }

    // ─── Class DP（有缓存 / 无缓存）───

    @ParameterizedTest
    @MethodSource("mappers")
    void version(ObjectMapper mapper) throws Exception {
        var original = Version.of(5);
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, Version.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void positiveInt(ObjectMapper mapper) throws Exception {
        var original = PositiveInt.of(42);
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, PositiveInt.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void active(ObjectMapper mapper) throws Exception {
        var original = Active.of(true);
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, Active.class);
        assertEquals(original, restored);
    }

    @ParameterizedTest
    @MethodSource("mappers")
    void email(ObjectMapper mapper) throws Exception {
        var original = new Email("User@Example.com");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, Email.class);
        assertEquals(original, restored);
    }

    // ─── BigDecimal DP ───

    @ParameterizedTest
    @MethodSource("mappers")
    void wanYuan(ObjectMapper mapper) throws Exception {
        var original = WanYuan.of("1.5");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, WanYuan.class);
        assertEquals(original, restored);
    }

    // ─── 多字段 Record DP ───

    @ParameterizedTest
    @MethodSource("mappers")
    void emailContent(ObjectMapper mapper) throws Exception {
        var original = new EmailContent("Welcome", "Thank you");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, EmailContent.class);
        assertEquals(original, restored);
    }
}

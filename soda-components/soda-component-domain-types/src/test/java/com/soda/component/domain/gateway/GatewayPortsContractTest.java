package com.soda.component.domain.gateway;

import com.soda.component.domain.Gateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 加密族/哈希族网关端口契约测试（ADR-0033）——断言两轴：
 * 方法面宽度（四端口最小面）与入参签名纪律（禁止 String/基本类型，必须为 DP 或类型令牌）。
 */
@DisplayName("加密与哈希网关端口契约")
class GatewayPortsContractTest {

    private static Set<String> methods(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("四个场景端口均继承 Gateway 标记")
    void should_portsExtendGateway() {
        assertThat(Gateway.class).isAssignableFrom(Encryptor.class);
        assertThat(Gateway.class).isAssignableFrom(Decryptor.class);
        assertThat(Gateway.class).isAssignableFrom(PasswordHasher.class);
        assertThat(Gateway.class).isAssignableFrom(Digester.class);
    }

    @Test
    @DisplayName("Encryptor 方法面 = encrypt")
    void should_declareOnlyEncryptMethod() {
        assertThat(methods(Encryptor.class)).containsExactly("encrypt");
    }

    @Test
    @DisplayName("Decryptor 方法面 = decrypt / decryptGeneric")
    void should_declareDecryptMethods() {
        assertThat(methods(Decryptor.class)).containsExactlyInAnyOrder("decrypt", "decryptGeneric");
    }

    @Test
    @DisplayName("PasswordHasher 方法面 = hash / verify / needsRehash")
    void should_declarePasswordHasherMethods() {
        assertThat(methods(PasswordHasher.class)).containsExactlyInAnyOrder("hash", "verify", "needsRehash");
    }

    @Test
    @DisplayName("Digester 方法面 = digest / index")
    void should_declareDigestMethods() {
        assertThat(methods(Digester.class)).containsExactlyInAnyOrder("digest", "index");
    }

    @Test
    @DisplayName("签名纪律：所有端口方法入参禁止 String 与基本类型")
    void should_forbidStringAndPrimitiveParams() {
        Stream.of(Encryptor.class, Decryptor.class, PasswordHasher.class, Digester.class)
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .forEach(method -> assertThat(method.getParameterTypes())
                        .as("%s.%s", method.getDeclaringClass().getSimpleName(), method.getName())
                        .noneMatch(pt -> pt.equals(String.class) || pt.isPrimitive()));
    }
}

package com.soda.component.domain.gateway;

import com.soda.component.domain.Gateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 加密族/哈希族网关端口契约测试（ADR-0033）——端口存在性、Gateway 继承与方法面。
 * 签名纪律（ADR-0033）：入参禁止 String/基本类型，必须为 DP；出参不限。
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
    void should_encryptorSurface() {
        assertThat(methods(Encryptor.class)).containsExactly("encrypt");
    }

    @Test
    @DisplayName("Decryptor 方法面 = decrypt / decryptGeneric")
    void should_decryptorSurface() {
        assertThat(methods(Decryptor.class)).containsExactlyInAnyOrder("decrypt", "decryptGeneric");
    }

    @Test
    @DisplayName("PasswordHasher 方法面 = hash / verify / needsRehash")
    void should_passwordHasherSurface() {
        assertThat(methods(PasswordHasher.class)).containsExactlyInAnyOrder("hash", "verify", "needsRehash");
    }

    @Test
    @DisplayName("Digester 方法面 = digest / index")
    void should_digesterSurface() {
        assertThat(methods(Digester.class)).containsExactlyInAnyOrder("digest", "index");
    }
}

package com.soda.user.infrastructure.gateway;

import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.types.Alphabet;
import com.soda.component.domain.types.PositiveInt;
import com.soda.component.domain.types.RandomString;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * {@link RandomStringGenerator} 的 SecureRandom 实现 — 均匀无偏（ADR-0018）。
 * <p>
 * 字符集由调用方以 {@link Alphabet} 指定（领域拥有）；随机源为 {@link SecureRandom}。
 * {@link SecureRandom#nextInt(int)} 自 Java 17 起内部拒绝采样，保证 [0, size) 均匀无偏
 * （模运算会引入 Modulo Bias，拒绝采样消除之；对齐 {@link Alphabet#charAt} javadoc 建议）。
 */
@Component
public class RandomStringGeneratorImpl implements RandomStringGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public RandomString generate(PositiveInt length, Alphabet alphabet) {
        var builder = new StringBuilder(length.value());
        for (int i = 0; i < length.value(); i++) {
            builder.append(alphabet.charAt(random.nextInt(alphabet.size())));
        }
        return new RandomString(builder.toString());
    }
}

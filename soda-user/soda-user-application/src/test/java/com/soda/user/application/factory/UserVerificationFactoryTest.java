package com.soda.user.application.factory;

import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.types.Alphabet;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.PositiveInt;
import com.soda.component.domain.types.RandomString;
import com.soda.user.domain.types.EmailRecipient;
import com.soda.user.domain.types.SmsRecipient;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserVerificationFactory} 单元测试（2026-08-16，见 ADR-0026）。
 * <p>
 * 验证：source 构造（scene→助记码串、userId→裸键串——槽位预检与构造共用同一映射点）、
 * 按场景构造方法（方法名即场景——scene=UCC 方法内写死）、UCC 专属策略
 * （<b>双通道统一纯数字/短时效</b>，不引用通道默认 DEFAULT_SMS/DEFAULT_EMAIL）、
 * 返回完整 INITIALIZED 验证聚合（码生成 + 事件注册，经领域静态工厂单构造路径）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserVerificationFactory")
class UserVerificationFactoryTest {

    private static final UserId USER_ID = new UserId(1L);
    private static final Mobile MOBILE = new Mobile("13800138000");
    private static final Email EMAIL = new Email("user@test.com");
    private static final String VALID_CODE = "123456";

    @Mock
    private RandomStringGenerator randomStringGenerator;

    private UserVerificationFactory factory() {
        return new UserVerificationFactory(randomStringGenerator);
    }

    @Test
    @DisplayName("credentialChangeSource：scene=UCC 写死 + userId 裸键串（槽位占用者）")
    void should_constructSource() {
        assertThat(factory().newCredentialChangeSource(USER_ID))
                .isEqualTo(VerificationSource.of("UCC", "1"));
        assertThat(factory().newCredentialChangeSource(new UserId(42L)))
                .isEqualTo(VerificationSource.of("UCC", "42"));
    }

    @Test
    @DisplayName("newCredentialChangeVerification（SmsRecipient）：scene=UCC 写死，UCC 专属策略（6 位纯数字/5 分钟）")
    void should_createSms_withUccPolicy() {
        when(randomStringGenerator.generate(any(PositiveInt.class), any(Alphabet.class)))
                .thenReturn(new RandomString(VALID_CODE));

        var verification = factory().newCredentialChangeVerification(USER_ID, new SmsRecipient(MOBILE));

        verify(randomStringGenerator).generate(eq(PositiveInt.of(6)), eq(Alphabet.DIGITS));
        assertThat(verification.getState()).isEqualTo(VerificationState.I);
        assertThat(verification.getSource()).isEqualTo(VerificationSource.of("UCC", "1"));
        assertThat(verification.getRecipient()).isEqualTo(new SmsRecipient(MOBILE));
        assertThat(verification.getCode().code()).isEqualTo(VALID_CODE);
        assertThat(verification.getCode().expireAt()).isAfter(java.time.Instant.now());
    }

    @Test
    @DisplayName("newCredentialChangeVerification（EmailRecipient）：与短信同策略（双通道统一纯数字/短时效）")
    void should_createEmail_withSameUccPolicy() {
        when(randomStringGenerator.generate(any(PositiveInt.class), any(Alphabet.class)))
                .thenReturn(new RandomString(VALID_CODE));

        var verification = factory().newCredentialChangeVerification(USER_ID, new EmailRecipient(EMAIL));

        verify(randomStringGenerator).generate(eq(PositiveInt.of(6)), eq(Alphabet.DIGITS));
        assertThat(verification.getState()).isEqualTo(VerificationState.I);
        assertThat(verification.getSource()).isEqualTo(VerificationSource.of("UCC", "1"));
        assertThat(verification.getRecipient()).isEqualTo(new EmailRecipient(EMAIL));
    }
}

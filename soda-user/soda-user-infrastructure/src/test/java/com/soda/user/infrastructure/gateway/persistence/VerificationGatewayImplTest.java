package com.soda.user.infrastructure.gateway.persistence;

import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.UUId;
import com.soda.user.domain.Verification;
import com.soda.user.domain.types.SmsRecipient;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;
import com.soda.user.infrastructure.convertor.VerificationConvertor;
import com.soda.user.infrastructure.persistence.VerificationPO;
import com.soda.user.infrastructure.repository.VerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link VerificationGatewayImpl#save} 单测 — active_key 终态清理（2026-08-16 修订，见 ADR-0026 注记）：
 * convertor 恒设 active_key，V/U 迁移在 save 清 NULL（终态不参与唯一，uk_active_key 硬保证单活跃）；
 * 终态守卫（同 UserGatewayImpl.save，ADR-0023）：持久化行已为 U（吸收态）时任何写入被拒。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationGatewayImpl.save")
class VerificationGatewayImplTest {

    private static final VerificationSource UCC_SOURCE = VerificationSource.of("UCC", "1");
    private static final Mobile MOBILE = new Mobile("13800138000");
    private static final Instant EXPIRE_AT = Instant.parse("2026-08-15T12:00:00Z");

    @Mock
    private VerificationRepository verificationRepository;

    private VerificationGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        gateway = new VerificationGatewayImpl(verificationRepository);
    }

    private static Verification uccVerification(VerificationState state) {
        return Verification.builder()
                .id(UUId.random())
                .source(UCC_SOURCE)
                .state(state)
                .code(new VerificationCode("123456", EXPIRE_AT))
                .recipient(new SmsRecipient(MOBILE))
                .build();
    }

    @Test
    @DisplayName("非终态（I/P）：active_key 保留 source.compositeKey() 占槽，同事务惰性腾槽")
    void shouldKeepActiveKey_forActiveStates() {
        gateway.save(uccVerification(VerificationState.I));

        var po = captured();
        assertThat(po.getActiveKey()).isEqualTo("UCC:1");
        verify(verificationRepository).deleteExpiredByActiveKey(eq("UCC:1"), any());
    }

    @Test
    @DisplayName("终态 V：save 清 active_key（终态不参与唯一）")
    void shouldClearActiveKey_forVerified() {
        gateway.save(uccVerification(VerificationState.V));

        assertThat(captured().getActiveKey()).isNull();
        verify(verificationRepository, never()).deleteExpiredByActiveKey(any(), any());
    }

    @Test
    @DisplayName("终态 U：save 清 active_key（终态不参与唯一）")
    void shouldClearActiveKey_forUsed() {
        gateway.save(uccVerification(VerificationState.U));

        assertThat(captured().getActiveKey()).isNull();
        verify(verificationRepository, never()).deleteExpiredByActiveKey(any(), any());
    }

    @Test
    @DisplayName("终态守卫：持久化行已为 U 而聚合为 V（陈旧聚合绕过）→ 拒绝写入，不落库")
    void shouldRejectSave_whenPersistedRowTerminal() {
        var stale = uccVerification(VerificationState.V);
        var row = VerificationConvertor.toPersistence(stale);
        row.setState(VerificationState.U.name());
        when(verificationRepository.findById(stale.getId().value())).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> gateway.save(stale))
                .isInstanceOf(IllegalStateException.class);
        verify(verificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("终态守卫：持久化行非终态（P）→ 放行写入")
    void shouldAllowSave_whenPersistedRowNonTerminal() {
        var pending = uccVerification(VerificationState.P);
        when(verificationRepository.findById(pending.getId().value()))
                .thenReturn(Optional.of(VerificationConvertor.toPersistence(pending)));

        gateway.save(pending);

        assertThat(captured().getActiveKey()).isEqualTo("UCC:1");
    }

    private VerificationPO captured() {
        var captor = ArgumentCaptor.forClass(VerificationPO.class);
        verify(verificationRepository).save(captor.capture());
        return captor.getValue();
    }
}

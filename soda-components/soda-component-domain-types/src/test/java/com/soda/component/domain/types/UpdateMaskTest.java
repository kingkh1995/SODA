package com.soda.component.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("UpdateMask —— update_mask 归一化与覆盖判定")
class UpdateMaskTest extends DomainPrimitiveContractTest<UpdateMask> {

    private static final Set<String> ALLOWED = Set.of("nickname", "sex", "avatar");

    @Override
    protected Contract<UpdateMask> contract() {
        return new Contract<>(UpdateMask.class, () -> UpdateMask.parse("nickname,sex", ALLOWED),
                "[\"nickname\",\"sex\"]", "UpdateMask[fields=[nickname, sex]]",
                "[\"*\"]", () -> UpdateMask.parse("nickname", ALLOWED));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        static Stream<Arguments> tokenCases() {
            return Stream.of(
                    arguments("nickname,sex", List.of("nickname", "sex")),
                    arguments(" nickname , sex ", List.of("nickname", "sex")),
                    arguments("nickname,nickname", List.of("nickname")),
                    arguments("sex,nickname,sex", List.of("sex", "nickname")));
        }

        @Test
        @DisplayName("字段名集合创建实例")
        void should_create_when_validFields() {
            assertThat(new UpdateMask(Set.of("nickname")).fields()).containsExactly("nickname");
        }

        @Test
        @DisplayName("空集 = 省略语态")
        void should_createEmpty_when_emptySet() {
            assertThat(new UpdateMask(Set.of()).fields()).isEmpty();
        }

        @Test
        @DisplayName("构造时防御性拷贝 —— 外部集合后续变更不影响实例，返回集合不可变")
        void should_copyDefensively_when_sourceSetMutated() {
            var source = new LinkedHashSet<String>();
            source.add("nickname");
            var mask = new UpdateMask(source);

            source.add("sex");

            assertThat(mask.fields()).containsExactly("nickname");
            assertThatThrownBy(() -> mask.fields().add("avatar"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @MethodSource("tokenCases")
        @DisplayName("逗号分隔字段名解析：保序、空白归一化、重复收敛")
        void should_parseTokens_when_commaSeparated(String raw, List<String> expected) {
            assertThat(UpdateMask.parse(raw, ALLOWED).fields()).containsExactlyElementsOf(expected);
        }

        @Test
        @DisplayName("省略语态（null / 空白 / 纯分隔符）解析为空集")
        void should_parseEmpty_when_omittedOrBlank() {
            assertThat(UpdateMask.parse(null, ALLOWED).fields()).isEmpty();
            assertThat(UpdateMask.parse("   ", ALLOWED).fields()).isEmpty();
            assertThat(UpdateMask.parse(",", ALLOWED).fields()).isEmpty();
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("null 集合拒绝")
        void should_throw_when_fieldsNull() {
            assertThatThrownBy(() -> new UpdateMask(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("空白字段名拒绝")
        void should_throw_when_fieldBlank() {
            assertThatThrownBy(() -> new UpdateMask(Set.of("  ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be blank");
        }

        @Test
        @DisplayName("白名单外字段名拒绝（AIP-161 §5.3 → 400）")
        void should_throw_when_unknownField() {
            assertThatThrownBy(() -> UpdateMask.parse("password", ALLOWED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unknown update_mask fields");
        }

        @Test
        @DisplayName("未知字段名只报未知项，不误报合法项")
        void should_reportOnlyUnknown_when_mixed() {
            assertThatThrownBy(() -> UpdateMask.parse("nickname,password", ALLOWED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unknown update_mask fields: [password]");
        }

        @Test
        @DisplayName("未展开的通配符拒绝（`*` 是请求级指令，需白名单才能解析外延）")
        void should_throw_when_wildcardUnresolved() {
            assertThatThrownBy(() -> new UpdateMask(Set.of("*")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be resolved to concrete field names");
        }
    }

    @Nested
    @DisplayName("覆盖判定")
    class RichMethods {
        @Test
        @DisplayName("空掩码：值非 null 才写")
        void should_write_when_emptyMaskAndValuePresent() {
            var mask = UpdateMask.parse(null, ALLOWED);
            assertThat(mask.covers("nickname", "新昵称")).isTrue();
            assertThat(mask.covers("sex", null)).isFalse();
        }

        @Test
        @DisplayName("字段集：命中即写，null 也写（清空）")
        void should_write_when_fieldCoveredEvenIfValueNull() {
            var mask = UpdateMask.parse("sex", ALLOWED);
            assertThat(mask.covers("sex", null)).isTrue();
        }

        @Test
        @DisplayName("字段集：未命中不写，值非 null 也不写")
        void should_notWrite_when_fieldNotCovered() {
            var mask = UpdateMask.parse("sex", ALLOWED);
            assertThat(mask.covers("nickname", "新昵称")).isFalse();
        }

        @Test
        @DisplayName("全量替换：白名单内所有字段恒写，含 null")
        void should_writeAll_when_wildcard() {
            var mask = UpdateMask.parse("*", ALLOWED);
            assertThat(mask.covers("nickname", "新昵称")).isTrue();
            assertThat(mask.covers("sex", null)).isTrue();
            assertThat(mask.covers("avatar", null)).isTrue();
        }

        @Test
        @DisplayName("全量替换：白名单外字段仍不写（展开有界，非「任意字段名」）")
        void should_notWrite_when_wildcardAndFieldOutsideAllowList() {
            var mask = UpdateMask.parse("*", ALLOWED);
            assertThat(mask.covers("password", "x")).isFalse();
        }

        @Test
        @DisplayName("通配符展开为白名单全集（与显式全字段列表等值）")
        void should_expandToFullSet_when_wildcard() {
            assertThat(UpdateMask.parse("*", ALLOWED).fields())
                    .containsExactlyInAnyOrderElementsOf(ALLOWED);
            assertThat(UpdateMask.parse("*,nickname", ALLOWED).fields())
                    .containsExactlyInAnyOrderElementsOf(ALLOWED);
            assertThat(UpdateMask.parse("*", ALLOWED))
                    .isEqualTo(UpdateMask.parse("avatar,nickname,sex", ALLOWED));
        }
    }
}

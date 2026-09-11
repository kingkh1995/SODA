package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UpdateMask —— update_mask 归一化与覆盖判定")
class UpdateMaskTest {

    private static final Set<String> ALLOWED = Set.of("nickname", "sex", "avatar");

    @Nested
    @DisplayName("构造")
    class Constructor {
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
    }

    @Nested
    @DisplayName("解析")
    class Parse {
        @Test
        @DisplayName("逗号分隔字段名按序解析")
        void should_parseInOrder_when_commaSeparated() {
            assertThat(UpdateMask.parse("nickname,sex", ALLOWED).fields())
                    .containsExactly("nickname", "sex");
        }

        @Test
        @DisplayName("字段名两侧空白归一化")
        void should_trimTokens_when_padded() {
            assertThat(UpdateMask.parse(" nickname , sex ", ALLOWED).fields())
                    .containsExactly("nickname", "sex");
        }

        @Test
        @DisplayName("重复字段名收敛为一条")
        void should_deduplicate_when_repeated() {
            assertThat(UpdateMask.parse("nickname,nickname", ALLOWED).fields())
                    .containsExactly("nickname");
        }

        @Test
        @DisplayName("null（省略）解析为空集")
        void should_parseEmpty_when_null() {
            assertThat(UpdateMask.parse(null, ALLOWED).fields()).isEmpty();
        }

        @Test
        @DisplayName("空白解析为空集")
        void should_parseEmpty_when_blank() {
            assertThat(UpdateMask.parse("   ", ALLOWED).fields()).isEmpty();
        }

        @Test
        @DisplayName("纯分隔符解析为空集（与省略同语义）")
        void should_parseEmpty_when_onlySeparators() {
            assertThat(UpdateMask.parse(",", ALLOWED).fields()).isEmpty();
        }

        @Test
        @DisplayName("通配符展开为白名单全集（全量替换）")
        void should_expandToFullSet_when_wildcard() {
            assertThat(UpdateMask.parse("*", ALLOWED).fields())
                    .containsExactlyInAnyOrderElementsOf(ALLOWED);
        }

        @Test
        @DisplayName("通配符与字段名混用收敛为全集（`*` 已覆盖全部，冗余项无害）")
        void should_expandToFullSet_when_wildcardMixedWithFields() {
            assertThat(UpdateMask.parse("*,nickname", ALLOWED).fields())
                    .containsExactlyInAnyOrderElementsOf(ALLOWED);
        }

        @Test
        @DisplayName("通配符展开结果与显式全字段列表等值（客户端用 `*` 与否不可区分）")
        void should_beEquivalentToExplicitFullList_when_wildcard() {
            assertThat(UpdateMask.parse("*", ALLOWED))
                    .isEqualTo(UpdateMask.parse("avatar,nickname,sex", ALLOWED));
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
                    .hasMessageContaining("Unknown update_mask fields");
        }

        @Test
        @DisplayName("未知字段名只报未知项，不误报合法项")
        void should_reportOnlyUnknown_when_mixed() {
            assertThatThrownBy(() -> UpdateMask.parse("nickname,password", ALLOWED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown update_mask fields: [password]");
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
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("同字段集等值（与构造路径无关）")
        void should_beEqual_when_sameFields() {
            assertThat(UpdateMask.parse("nickname,sex", ALLOWED))
                    .isEqualTo(new UpdateMask(Set.of("sex", "nickname")));
        }

        @Test
        @DisplayName("不同字段集不等")
        void should_notBeEqual_when_differentFields() {
            assertThat(UpdateMask.parse("nickname", ALLOWED))
                    .isNotEqualTo(UpdateMask.parse("sex", ALLOWED));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(UpdateMask.parse("nickname,sex", ALLOWED))
                    .hasSameHashCodeAs(new UpdateMask(Set.of("nickname", "sex")));
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
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("序列化为字段名数组")
        void should_serializeAsArray() throws Exception {
            assertThat(MAPPER.writeValueAsString(UpdateMask.parse("nickname,sex", ALLOWED)))
                    .isEqualTo("[\"nickname\",\"sex\"]");
        }

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            var original = UpdateMask.parse("nickname,sex", ALLOWED);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, UpdateMask.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("空掩码 round-trip 一致")
        void should_roundTrip_when_empty() throws Exception {
            var original = UpdateMask.parse(null, ALLOWED);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, UpdateMask.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("全量替换序列化为展开后的全集（不保留 `*` 字面）")
        void should_serializeExpandedSet_when_wildcard() throws Exception {
            var original = UpdateMask.parse("*", ALLOWED);
            var json = MAPPER.writeValueAsString(original);

            assertThat(MAPPER.readValue(json, UpdateMask.class)).isEqualTo(original);
            assertThat(MAPPER.readValue(json, UpdateMask.class).fields())
                    .containsExactlyInAnyOrderElementsOf(ALLOWED);
        }

        @Test
        @DisplayName("`*` 不可离线反序列化（请求级指令需白名单解析外延）")
        void should_throw_when_jsonWildcard() {
            assertThatThrownBy(() -> MAPPER.readValue("[\"*\"]", UpdateMask.class))
                    .isInstanceOf(JacksonException.class)
                    .hasMessageContaining("must be resolved to concrete field names");
        }

        @Test
        @DisplayName("非法 JSON 拒绝（对象形态）")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{\"fields\":[\"nickname\"]}", UpdateMask.class))
                    .isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("反序列化施加形态校验（空白字段名拒绝）")
        void should_throw_when_blankField() {
            assertThatThrownBy(() -> MAPPER.readValue("[\"  \"]", UpdateMask.class))
                    .isInstanceOf(JacksonException.class)
                    .hasMessageContaining("must not be blank");
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(UpdateMask.parse("nickname,sex", ALLOWED))
                    .hasToString("UpdateMask[fields=[nickname, sex]]");
        }
    }
}

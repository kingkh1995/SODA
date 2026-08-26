---
type: Decision Record
title: 字符集升格为领域概念：Alphabet DP
description: 生成随机码/字符串需选用字符集时读——字符集升格为开集字符串 DP（Alphabet），随机源与无偏采样留生成器。
tags: [dp, random-string, alphabet]
status: stable
---

# 0018 — 字符集升格为领域概念：Alphabet DP

`RandomStringGenerator` 契约为 `generate(PositiveInt, Alphabet)`：验证码等场景由调用方表达字符集意图，Alphabet 是开集字符串
DP（只做严格索引映射，charAt 越界即 IAE），随机源与无偏采样留生成器。不变量为字符非空、唯一、size ≥ 2，常用集 `DIGITS`/
`UNAMBIGUOUS_ALPHANUMERIC` 仅为便捷默认而非闭集；生成器不绑定业务概念，码形策略由调用方按用例决定（见 ADR-0026）。

# Soda — DDD Scaffold

基于 yudao-cloud 业务功能改造的 DDD 脚手架项目。

## Language

### Entity
具有连续身份标识（identity thread）的领域对象。两个实体相等当且仅当它们类型相同且拥有同一个 **Identifier**，与其他属性值无关。

### Aggregate
聚合一致性边界内的顶层实体，负责保证聚合内部的所有不变量不被破坏。对聚合的所有操作必须通过聚合根进行。

### Type
所有领域原语（Domain Primitive）的根标记接口。扩展 `Serializable` 和 `Comparable<Type>` — 所有 DP 都是值对象，需要可比。直接实现 Type 的类需提供 `compareTo(Type)`。

### Identifier
不可变的领域原语，扩展 `Type`，在限界上下文内唯一标识一个实体。底层值类型是泛型的（`Identifier<T extends Comparable<T>>`）。`compareTo(Type)` 已有默认实现委托给底层值比较，具体类无需覆写。实现类需提供 `identifier()` 返回类型化值，以及基于值的 `equals()`/`hashCode()`。

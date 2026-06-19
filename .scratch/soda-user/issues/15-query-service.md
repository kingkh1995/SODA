## Parent

`.scratch/soda-user/PRD.md`

## What to build

Query Server 的 Service 层。调 DAO，组装 DTO 返回。

**`UserQueryService`：**
- `getUser(UserId): UserDTO` — 详情查询
- `getUserByUsername(Username): UserDTO` — 按用户名查
- `getUserPage(UserPageQuery): PageResult<UserDTO>` — 分页，支持 username / mobile / status 过滤
- `getSimpleUserList(deptId?): List<UserSimpleDTO>` — 精简列表（下拉选项用）

Service 层是传统贫血模式：调 Mapper → 返回 DTO。不含业务规则，不含领域对象。

复杂查询（如关联 dept 名称等需要 join 的场景）暂不实现，由前端的 `getSimpleUserList` 提供基础选项，完整关联在 `soda-system` 模块中补全。

## Acceptance criteria

- [ ] Service 层编译通过
- [ ] 集成测试覆盖所有查询方法
- [ ] 不依赖 domain 模块

## Blocked by

`14-query-dao.md`

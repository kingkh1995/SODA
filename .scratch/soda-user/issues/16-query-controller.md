## Parent

`.scratch/soda-user/PRD.md`

## What to build

Query Server 的 Controller 层。REST 端点暴露读接口。

```java
@RestController
@RequestMapping("/api/system/user")
class UserQueryController {

    @GetMapping("/get")
    CommonResult<UserVO> get(@RequestParam("id") Long id);

    @GetMapping("/page")
    CommonResult<PageResult<UserVO>> page(UserPageQuery query);

    @GetMapping("/simple-list")
    CommonResult<List<UserSimpleVO>> getSimpleList(
            @RequestParam(value = "deptId", required = false) Long deptId);

    @GetMapping("/list-by-nickname")
    CommonResult<List<UserSimpleVO>> getByNickname(@RequestParam("nickname") String nickname);
}
```

返回值使用与写侧 `soda-user-api` 共用的 DTO 类型，不做 VO 转换（query server 不通 adapter，直接返回 DTO）。

## Acceptance criteria

- [ ] 所有 Query Controller 端点编译通过
- [ ] 集成测试覆盖全部端点

## Blocked by

`15-query-service.md`

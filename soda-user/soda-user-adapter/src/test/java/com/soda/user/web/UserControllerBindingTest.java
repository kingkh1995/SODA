package com.soda.user.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.soda.user.api.UserAuthService;
import com.soda.user.api.UserService;
import com.soda.user.web.assembler.UserWebAssembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@link UserController} 的 MVC 绑定与校验（真实 {@code @Valid}/{@code @Validated} 注解链，service 侧 mock）。
 *
 * <p>只断言绑定/校验层的触发结果；错误渲染矩阵归 {@code soda-component-adapter-starter-web}， 端点→命令映射归 {@code
 * UserControllerTest}，请求体字段映射归 {@code UserWebAssemblerTest}。
 */
@WebMvcTest(controllers = UserController.class)
@DisplayName("UserController MVC 绑定与校验")
class UserControllerBindingTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserService userService;

    @MockitoBean private UserAuthService userAuthService;

    @MockitoBean private UserWebAssembler assembler;

    @Test
    @DisplayName("非法请求体（@Valid 违约）→ 400 problem+json")
    void should_returnProblemJson_when_requestBodyInvalid() throws Exception {
        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\"a\",\"password\":\"123\",\"nickname\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("路径变量 @Positive 违约 → 400，detail 携带违约消息而非 500")
    void should_returnBadRequest_when_pathVariableViolatesConstraint() throws Exception {
        mockMvc.perform(post("/api/users/{id}:deregister", -5L))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail", containsString("must be greater than 0")));
    }
}

package com.jobboard.user;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpSession loginAsAdmin() throws Exception {
        Map<String, String> loginBody = Map.of("email", "admin@jobboard.local", "password", "admin1234!");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getRequest().getSession(false);
    }

    @Test
    void 관리자는_회원_목록을_조회하고_권한을_변경할_수_있다() throws Exception {
        HttpSession adminSession = loginAsAdmin();

        Map<String, String> registerBody = Map.of(
                "email", "member@jobboard.com", "password", "password123", "name", "회원1");
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(registerBody)));

        MvcResult listResult = mockMvc.perform(get("/api/admin/users")
                        .session((org.springframework.mock.web.MockHttpSession) adminSession))
                .andExpect(status().isOk())
                .andReturn();

        String body = listResult.getResponse().getContentAsString();
        tools.jackson.databind.JsonNode users = objectMapper.readTree(body);
        long memberId = -1;
        for (var node : users) {
            if (node.get("email").asText().equals("member@jobboard.com")) {
                memberId = node.get("id").asLong();
            }
        }

        mockMvc.perform(patch("/api/admin/users/" + memberId + "/role")
                        .session((org.springframework.mock.web.MockHttpSession) adminSession)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void 본인의_권한을_변경하려_하면_400을_받는다() throws Exception {
        HttpSession adminSession = loginAsAdmin();

        MvcResult meResult = mockMvc.perform(get("/api/auth/me")
                        .session((org.springframework.mock.web.MockHttpSession) adminSession))
                .andExpect(status().isOk())
                .andReturn();
        long adminId = objectMapper.readTree(meResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/admin/users/" + adminId + "/role")
                        .session((org.springframework.mock.web.MockHttpSession) adminSession)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("role", "USER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("본인의 권한은 변경할 수 없습니다."));
    }

    @Test
    void 일반회원은_회원_목록_조회시_403을_받는다() throws Exception {
        Map<String, String> registerBody = Map.of(
                "email", "plain@jobboard.com", "password", "password123", "name", "일반회원");
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(registerBody)));

        Map<String, String> loginBody = Map.of("email", "plain@jobboard.com", "password", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andReturn();
        HttpSession session = loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/api/admin/users")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("관리자만 접근할 수 있습니다."));
    }
}

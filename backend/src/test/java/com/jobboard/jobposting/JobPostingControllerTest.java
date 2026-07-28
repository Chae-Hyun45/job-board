package com.jobboard.jobposting;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobPostingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession loginAsUser(String email) throws Exception {
        Map<String, String> registerBody = Map.of("email", email, "password", "password123", "name", "회원");
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(registerBody)));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "password123"))))
                .andReturn();
        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    @Test
    void 로그인한_회원은_채용공고_목록을_조회할_수_있다() throws Exception {
        MockHttpSession session = loginAsUser("list-view@jobboard.com");

        mockMvc.perform(get("/api/job-postings").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void 존재하지_않는_공고_조회시_404를_반환한다() throws Exception {
        MockHttpSession session = loginAsUser("detail-view@jobboard.com");

        mockMvc.perform(get("/api/job-postings/999999").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void 비로그인_사용자는_목록_조회시_401을_받는다() throws Exception {
        mockMvc.perform(get("/api/job-postings"))
                .andExpect(status().isUnauthorized());
    }
}

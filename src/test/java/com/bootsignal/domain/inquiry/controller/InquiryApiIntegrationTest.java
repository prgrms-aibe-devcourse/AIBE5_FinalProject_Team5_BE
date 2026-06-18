package com.bootsignal.domain.inquiry.controller;

import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 HTTP 흐름에 가깝게 사용자 문의 등록부터 관리자 답변, 사용자 답변 확인까지 검증하는 통합 테스트입니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InquiryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void userCreatesInquiryAndAdminAnswers() throws Exception {
        saveUser("flow-user@example.com", "문의사용자", UserRole.USER);
        saveUser("flow-admin@example.com", "문의관리자", UserRole.ADMIN);
        String userToken = login("flow-user@example.com", "password123");
        String adminToken = login("flow-admin@example.com", "password123");

        String createResponse = mockMvc.perform(post("/api/inquiries")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "문의 제목",
                      "content": "문의 내용"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

        long inquiryId = objectMapper.readTree(createResponse).path("data").path("inquiryId").asLong();

        mockMvc.perform(get("/api/inquiries")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].inquiryId").value(inquiryId))
            .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));

        mockMvc.perform(get("/api/admin/inquiries")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("status", "PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].inquiryId").value(inquiryId));

        mockMvc.perform(patch("/api/admin/inquiries/{inquiryId}/answer", inquiryId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "adminReply": "관리자 답변입니다."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.adminReply").value("관리자 답변입니다."));

        mockMvc.perform(get("/api/inquiries/{inquiryId}", inquiryId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.adminReply").value("관리자 답변입니다."));
    }

    private void saveUser(String email, String nickname, UserRole role) {
        User user = User.signupLocal(email, passwordEncoder.encode("password123"), nickname);
        ReflectionTestUtils.setField(user, "role", role);
        userRepository.save(user);
    }

    private String login(String email, String password) throws Exception {
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

        return objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();
    }
}

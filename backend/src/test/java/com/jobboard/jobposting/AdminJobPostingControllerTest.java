package com.jobboard.jobposting;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminJobPostingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OpenAiJobExtractionClient extractionClient;

    private MockHttpSession loginAsAdmin() throws Exception {
        Map<String, String> loginBody = Map.of("email", "admin@jobboard.local", "password", "admin1234!");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void 관리자가_PDF를_업로드하면_추출결과를_받는다() throws Exception {
        MockHttpSession session = loginAsAdmin();
        when(extractionClient.extract(any())).thenReturn(new com.jobboard.jobposting.dto.PdfExtractionResult(
                null, "테스트회사", "서울", "NEW", "BACHELOR", "FULL_TIME", "비고",
                "2026-08-01", "2026-08-31", "이메일 접수", 3000, 3500, "협의가능"));

        MockMultipartFile file = new MockMultipartFile("file", "posting.pdf", "application/pdf", createSamplePdfBytes());

        mockMvc.perform(multipart("/api/admin/job-postings/extract").file(file).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("테스트회사"))
                .andExpect(jsonPath("$.pdfFileName").isNotEmpty());
    }

    @Test
    void 관리자가_채용공고를_등록_수정_삭제한다() throws Exception {
        MockHttpSession session = loginAsAdmin();
        when(extractionClient.extract(any())).thenReturn(new com.jobboard.jobposting.dto.PdfExtractionResult(
                null, "테스트회사", "서울", "NEW", "BACHELOR", "FULL_TIME", "비고",
                "2026-08-01", "2026-08-31", "이메일 접수", 3000, 3500, "협의가능"));

        Map<String, Object> createBody = Map.ofEntries(
                Map.entry("pdfFileName", "sample.pdf"), Map.entry("companyName", "테스트회사"), Map.entry("location", "서울"),
                Map.entry("careerLevel", "NEW"), Map.entry("education", "BACHELOR"), Map.entry("employmentType", "FULL_TIME"),
                Map.entry("conditionNote", "비고"), Map.entry("applyStartDate", "2026-08-01"), Map.entry("applyEndDate", "2026-08-31"),
                Map.entry("applyMethod", "이메일 접수"), Map.entry("salaryMin", 3000), Map.entry("salaryMax", 3500), Map.entry("salaryNote", "협의가능"));

        MvcResult createResult = mockMvc.perform(post("/api/admin/job-postings")
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isOk())
                .andReturn();

        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        Map<String, Object> updateBody = Map.ofEntries(
                Map.entry("pdfFileName", "sample.pdf"), Map.entry("companyName", "수정된회사"), Map.entry("location", "부산"),
                Map.entry("careerLevel", "EXPERIENCED"), Map.entry("education", "MASTER"), Map.entry("employmentType", "CONTRACT"),
                Map.entry("conditionNote", "수정된 비고"), Map.entry("applyStartDate", "2026-09-01"), Map.entry("applyEndDate", "2026-09-30"),
                Map.entry("applyMethod", "우편 접수"), Map.entry("salaryMin", 4000), Map.entry("salaryMax", 4500), Map.entry("salaryNote", "면접후 결정"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/admin/job-postings/" + id)
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("수정된회사"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/admin/job-postings/" + id)
                        .session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    void 관리자가_더미데이터를_추가하고_삭제한다() throws Exception {
        MockHttpSession session = loginAsAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/admin/job-postings/dummy").session(session))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(created.size()).isEqualTo(10);
        created.forEach(node ->
                org.assertj.core.api.Assertions.assertThat(node.get("companyName").asText()).startsWith("[더미]"));

        MvcResult listAfterCreate = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/admin/job-postings")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listAfterCreateBody = objectMapper.readTree(listAfterCreate.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(countDummyEntries(listAfterCreateBody)).isEqualTo(10);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/admin/job-postings/dummy")
                        .session(session))
                .andExpect(status().isNoContent());

        MvcResult listAfterDelete = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/admin/job-postings")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listAfterDeleteBody = objectMapper.readTree(listAfterDelete.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(countDummyEntries(listAfterDeleteBody)).isZero();
    }

    private long countDummyEntries(JsonNode list) {
        long count = 0;
        for (JsonNode node : list) {
            if (node.get("companyName").asText().startsWith("[더미]")) {
                count++;
            }
        }
        return count;
    }

    private byte[] createSamplePdfBytes() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("TestCompany JobPosting");
                stream.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}

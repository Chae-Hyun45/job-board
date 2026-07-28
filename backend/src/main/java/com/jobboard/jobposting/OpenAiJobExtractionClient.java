package com.jobboard.jobposting;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.jobboard.common.ApiException;
import com.jobboard.jobposting.dto.PdfExtractionResult;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiJobExtractionClient {

    private static final String SYSTEM_PROMPT = """
            당신은 채용공고 PDF 텍스트에서 정보를 추출하는 도우미입니다.
            반드시 아래 JSON 형식으로만 응답하세요. 알 수 없는 값은 빈 문자열 또는 0을 사용하세요.
            {
              "companyName": "string",
              "location": "string",
              "careerLevel": "NEW|EXPERIENCED|ANY",
              "education": "NONE|HIGH_SCHOOL|ASSOCIATE|BACHELOR|MASTER",
              "employmentType": "FULL_TIME|CONTRACT|INTERN|PART_TIME",
              "conditionNote": "string",
              "applyStartDate": "yyyy-MM-dd",
              "applyEndDate": "yyyy-MM-dd",
              "applyMethod": "string",
              "salaryMin": 0,
              "salaryMax": 0,
              "salaryNote": "string"
            }
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiJobExtractionClient(RestClient.Builder builder, OpenAiProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.model = properties.getModel();
        this.restClient = builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();
    }

    public PdfExtractionResult extract(String pdfText) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", pdfText)
                )
        );

        Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        String content = extractContent(response);
        try {
            return objectMapper.readValue(content, PdfExtractionResult.class);
        } catch (JacksonException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "채용정보 추출 결과를 해석할 수 없습니다.");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}

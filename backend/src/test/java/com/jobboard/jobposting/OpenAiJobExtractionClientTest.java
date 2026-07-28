package com.jobboard.jobposting;

import tools.jackson.databind.ObjectMapper;
import com.jobboard.common.ApiException;
import com.jobboard.jobposting.dto.PdfExtractionResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiJobExtractionClientTest {

    private static final String AI_RESPONSE_JSON = """
            {
              "choices": [
                {
                  "message": {
                    "content": "{\\"companyName\\":\\"테스트회사\\",\\"location\\":\\"서울\\",\\"careerLevel\\":\\"NEW\\",\\"education\\":\\"BACHELOR\\",\\"employmentType\\":\\"FULL_TIME\\",\\"conditionNote\\":\\"우대사항 없음\\",\\"applyStartDate\\":\\"2026-08-01\\",\\"applyEndDate\\":\\"2026-08-31\\",\\"applyMethod\\":\\"이메일 접수\\",\\"salaryMin\\":3000,\\"salaryMax\\":3500,\\"salaryNote\\":\\"협의가능\\"}"
                  }
                }
              ]
            }
            """;

    @Test
    void PDF_텍스트로부터_구조화된_정보를_추출한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        OpenAiProperties properties = new OpenAiProperties();
        properties.setApiKey("test-key");
        properties.setModel("gpt-4o-mini");
        properties.setBaseUrl("https://api.openai.com/v1");

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(AI_RESPONSE_JSON, MediaType.APPLICATION_JSON));

        OpenAiJobExtractionClient client = new OpenAiJobExtractionClient(builder, properties, new ObjectMapper());

        PdfExtractionResult result = client.extract("테스트회사 채용공고 텍스트");

        assertThat(result.companyName()).isEqualTo("테스트회사");
        assertThat(result.salaryMin()).isEqualTo(3000);
        assertThat(result.applyEndDate()).isEqualTo("2026-08-31");
    }

    @Test
    void OpenAI_API가_5xx_오류를_반환하면_ApiException을_던진다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        OpenAiProperties properties = new OpenAiProperties();
        properties.setApiKey("test-key");
        properties.setModel("gpt-4o-mini");
        properties.setBaseUrl("https://api.openai.com/v1");

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withServerError());

        OpenAiJobExtractionClient client = new OpenAiJobExtractionClient(builder, properties, new ObjectMapper());

        assertThat(catchThrowable(() -> client.extract("테스트회사 채용공고 텍스트")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void OpenAI_응답의_choices가_비어있으면_ApiException을_던진다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        OpenAiProperties properties = new OpenAiProperties();
        properties.setApiKey("test-key");
        properties.setModel("gpt-4o-mini");
        properties.setBaseUrl("https://api.openai.com/v1");

        String emptyChoicesJson = """
                {
                  "choices": []
                }
                """;

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(emptyChoicesJson, MediaType.APPLICATION_JSON));

        OpenAiJobExtractionClient client = new OpenAiJobExtractionClient(builder, properties, new ObjectMapper());

        assertThat(catchThrowable(() -> client.extract("테스트회사 채용공고 텍스트")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}

package com.jobboard.jobposting;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.openai")
@Getter
@Setter
public class OpenAiProperties {
    private String apiKey;
    private String model;
    private String baseUrl;
}

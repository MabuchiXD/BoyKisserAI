package org.example.boykisserai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private final AppProperties props;

    public WebClientConfig(AppProperties props) {
        this.props = props;
    }

    @Bean
    public WebClient ollamaWebClient() {
        return WebClient.builder()
                .baseUrl(props.getOllama().getBaseUrl())
                .build();
    }
}
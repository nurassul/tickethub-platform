package dev.project.gateway.config;


import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GatewayHttpClientConfig {


    @Bean
    public HttpClientCustomizer customizer() {
        return httpClient -> {
            return httpClient.resolver(
                    resolver -> {
                        resolver.cacheMaxTimeToLive(Duration.ofSeconds(5));
                    }
            );
        };
    }

}

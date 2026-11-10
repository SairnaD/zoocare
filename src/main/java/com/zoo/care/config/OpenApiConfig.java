package com.zoo.care.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI zooCareOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ZooCare API")
                .description("REST API for species, enclosures, keepers and animals. "
                        + "Every error returns one JSON body: timestamp, status, error, message, path, fieldErrors.")
                .version("1.0"));
    }
}
package org.example.tandem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Corporate Platform API")
                        .description("API documentation for internal communication platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Kairat")
                                .email("isaevkairat1234@gmail.com")));
    }
}

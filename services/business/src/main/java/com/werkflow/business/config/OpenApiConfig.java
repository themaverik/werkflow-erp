package com.werkflow.business.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Werkflow Business Service API")
                .version("1.0.0")
                .description("Business Service API for Werkflow ERP")
            )
            .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                .addList("oauth2")
            )
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("oauth2", new SecurityScheme()
                    .type(SecurityScheme.Type.OAUTH2)
                    .description("OAuth2 authentication with Keycloak")
                    .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                            .authorizationUrl(issuerUri + "/protocol/openid-connect/auth")
                            .tokenUrl(issuerUri + "/protocol/openid-connect/token")
                            .scopes(new io.swagger.v3.oas.models.security.Scopes()
                                .addString("openid", "OpenID Connect scope")
                                .addString("profile", "Profile scope")
                                .addString("email", "Email scope")
                            )
                        )
                    )
                )
            );
    }
}

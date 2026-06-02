package com.codefactory.appstripe.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String MERCHANT_ID = "merchantId";
    private static final String PUBLIC_ID = "publicId";
    private static final String SECRET = "secret";

    @Bean
    public OpenAPI appStripeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AppStripe / Paycore API")
                        .description("""
                                API REST del backend AppStripe.

                                - **Admin / portal comercio:** header `Authorization: Bearer <JWT>` (login en `/api/v1/auth/login`).
                                - **Transacciones (API comercio):** headers `X-Merchant-Id`, `X-Public-Id`, `X-Secret`.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("CodeFactory").email("soporte@paycore.local")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido en POST /api/v1/auth/login"))
                        .addSecuritySchemes(MERCHANT_ID, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Merchant-Id")
                                .description("ID del comercio"))
                        .addSecuritySchemes(PUBLIC_ID, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Public-Id")
                                .description("Public Key de credenciales API"))
                        .addSecuritySchemes(SECRET, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Secret")
                                .description("Secret Key de credenciales API")));
    }
}

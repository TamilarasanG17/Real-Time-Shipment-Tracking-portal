package com.example.shipmenttrackingportal.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Infotact Shipment Tracker API")
                        .version("1.0.0")
                        .description("""
                                Real-Time Shipment Tracking Portal & Logistics Marketplace.
                                
                                **Roles:**
                                - `SHIPPER` — posts freight loads, accepts bids, views tracking.
                                - `CARRIER` — browses load board, submits bids, sends GPS pings.
                                
                                **Authentication:**
                                1. Register via `POST /api/auth/register`
                                2. Login via `POST /api/auth/login` → copy the `token`
                                3. Click **Authorize** → paste `<token>` (no "Bearer " prefix needed here)
                                """)
                        .contact(new Contact()
                                .name("Infotact Solutions & Co.")
                                .email("dev@infotact.com"))
                        .license(new License()
                                .name("MIT License")))
                // Register JWT Bearer as the global security scheme
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT token from POST /api/auth/login")));
    }
}

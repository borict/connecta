package connecta.social.config;

import connecta.social.exception.ApiErrorResponse;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI socialServiceOpenApi() {
        Schema<?> errorSchema = new Schema<>().$ref("#/components/schemas/ApiErrorResponse");
        Content errorContent = new Content().addMediaType(
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                new MediaType().schema(errorSchema)
        );

        return new OpenAPI()
                .info(new Info()
                        .title("Connecta Social Service")
                        .description("""
                                Follows, follow requests and the home feed.
                                Login via User Service POST /api/auth/login, copy the token,
                                then Authorize → Bearer token here.
                                Public follow → ACCEPTED. Private follow → PENDING until accepted.
                                """)
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSchemas("ApiErrorResponse", new Schema<ApiErrorResponse>()
                                .name("ApiErrorResponse")
                                .type("object"))
                        .addResponses("BadRequest", new ApiResponse()
                                .description("Bad Request")
                                .content(errorContent))
                        .addResponses("Unauthorized", new ApiResponse()
                                .description("Unauthorized")
                                .content(errorContent))
                        .addResponses("Forbidden", new ApiResponse()
                                .description("Forbidden")
                                .content(errorContent))
                        .addResponses("NotFound", new ApiResponse()
                                .description("Not Found")
                                .content(errorContent))
                        .addResponses("ServiceUnavailable", new ApiResponse()
                                .description("Service Unavailable")
                                .content(errorContent)));
    }
}

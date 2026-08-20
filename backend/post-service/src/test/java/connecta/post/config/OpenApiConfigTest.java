package connecta.post.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void documentsBearerAuthAndErrorResponses() {
        OpenAPI api = new OpenApiConfig().postServiceOpenApi();

        assertThat(api.getInfo().getTitle()).isEqualTo("Connecta Post Service");
        assertThat(api.getComponents().getSecuritySchemes()).containsKey(OpenApiConfig.BEARER_AUTH);
        assertThat(api.getComponents().getSchemas()).containsKey("ApiErrorResponse");
        assertThat(api.getComponents().getResponses())
                .containsKeys("BadRequest", "Unauthorized", "Forbidden", "NotFound");
        assertThat(api.getSecurity())
                .anySatisfy(requirement -> assertThat(requirement).containsKey(OpenApiConfig.BEARER_AUTH));
    }
}

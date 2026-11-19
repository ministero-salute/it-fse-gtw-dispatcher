package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.*;

@Tag("ISOLATION")
class OpenApiContractIsolationTest {
    @Test void publishedSpecificationEnforcesMultipartAndProblemResponseContracts() {
        var config = new OpenApiCFG();
        var settings = mock(CustomSwaggerCFG.class);
        when(settings.getTitle()).thenReturn("Dispatcher");
        when(settings.getVersion()).thenReturn("1.0");
        when(settings.getDescription()).thenReturn("API");
        when(settings.getContactName()).thenReturn("Support");
        when(settings.getContactUrl()).thenReturn("https://example.test");
        when(settings.getApiId()).thenReturn("dispatcher");
        when(settings.getFileMaxLength()).thenReturn(4096);
        ReflectionTestUtils.setField(config, "customOpenapi", settings);
        var api = config.customOpenAPI().info(new Info()).components(new Components()
                .addSchemas("ErrorResponseDTO", new ObjectSchema())).paths(new Paths())
                .servers(List.of(new Server().url("http://localhost"), new Server().url("https://example.test")));
        for (var method : List.of(PathItem.HttpMethod.POST, PathItem.HttpMethod.PATCH, PathItem.HttpMethod.PUT)) {
            var schema = new ObjectSchema().addProperty("file", new StringSchema().format("binary"));
            var operation = new Operation().responses(new ApiResponses()).requestBody(new RequestBody().content(
                    new Content().addMediaType("multipart/form-data", new MediaType().schema(schema))));
            var path = new PathItem();
            path.operation(method, operation);
            api.path("/" + method, path);
        }
        api.path("/get", new PathItem().get(new Operation().responses(new ApiResponses())));
        api.path("/noBody", new PathItem().post(new Operation().responses(new ApiResponses())));
        api.path("/noContent", new PathItem().put(new Operation().responses(new ApiResponses()).requestBody(new RequestBody())));
        api.path("/json", new PathItem().patch(new Operation().responses(new ApiResponses()).requestBody(new RequestBody().content(new Content()))));
        config.openApiCustomiser().customise(api);
        config.customerGlobalHeaderOpenApiCustomiser().customise(api);
        assertEquals("Dispatcher", api.getInfo().getTitle());
        assertEquals("1.0", api.getInfo().getVersion());
        assertEquals("Support", api.getInfo().getContact().getName());
        assertEquals("dispatcher", api.getInfo().getExtensions().get("x-api-id"));
        assertEquals(2, api.getSecurity().size());
        assertEquals(4, api.getTags().size());
        assertEquals(true, api.getServers().get(0).getExtensions().get("x-sandbox"));
        assertNull(api.getServers().get(1).getExtensions());
        assertEquals(false, api.getComponents().getSchemas().get("ErrorResponseDTO").getAdditionalProperties());
        for (String method : List.of("POST", "PATCH", "PUT")) {
            var schema = api.getPaths().get("/" + method).readOperations().get(0).getRequestBody().getContent().get("multipart/form-data").getSchema();
            assertEquals(List.of("file", "requestBody"), schema.getRequired());
            assertEquals(4096, ((Schema<?>) schema.getProperties().get("file")).getMaxLength());
            assertEquals(false, schema.getAdditionalProperties());
        }
        api.getPaths().values().forEach(path -> path.readOperations().forEach(operation ->
                assertEquals("#/components/schemas/ErrorResponseDTO", operation.getResponses().get("default")
                        .getContent().get("application/problem+json").getSchema().get$ref())));
    }
}

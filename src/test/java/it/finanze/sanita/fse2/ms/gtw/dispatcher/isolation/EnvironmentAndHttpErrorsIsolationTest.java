package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.RestTemplateResponseErrorHandler;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.audit.exclusions.ActuatorExclusion;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.CollectionNaming;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.ProfileUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;

@Tag("ISOLATION")
class EnvironmentAndHttpErrorsIsolationTest {
    @ParameterizedTest @ValueSource(strings = {"TEST", "dev", "docker", "production", "none", "missing"})
    void profilesAndCollectionNamesSeparateTestData(String profile) {
        var utility = new ProfileUtility();
        var environment = new MockEnvironment();
        if (!profile.equals("none")) environment.setActiveProfiles(profile);
        if (!profile.equals("missing")) ReflectionTestUtils.setField(utility, "environment", environment);
        assertEquals(profile.equals("TEST"), utility.isTestProfile());
        assertEquals(profile.equals("dev") || profile.equals("docker"), utility.isDevOrDockerProfile());
        var naming = new CollectionNaming();
        ReflectionTestUtils.setField(naming, "profileUtility", utility);
        String prefix = profile.equals("TEST") ? Constants.Profile.TEST_PREFIX : "";
        assertEquals(prefix + Constants.Collections.INI_EDS_INVOCATION, naming.getIniEdsInvocationCollection());
        assertEquals(prefix + Constants.Collections.VALIDATED_DOCUMENTS, naming.getValidatedDocuments());
        assertEquals(prefix + Constants.Collections.AUDIT, naming.getAudit());
        assertEquals(prefix + Constants.Collections.ENGINES, naming.getEngineCollection());
        assertEquals(prefix + Constants.Collections.ROUTING_TABLE, naming.getRoutingTableCollection());
        assertEquals(prefix + Constants.Collections.ISSUERS, naming.getIssuersCollection());
    }

    @Test void actuatorExclusionsHonorCustomMappingsAndNullUris() {
        var properties = new WebEndpointProperties();
        properties.setBasePath("");
        properties.getExposure().setInclude(new LinkedHashSet<>(List.of("health", "info")));
        properties.getPathMapping().put("health", "status");
        var exclusion = new ActuatorExclusion();
        ReflectionTestUtils.setField(exclusion, "endpoints", properties);
        assertTrue(exclusion.verify("/status/liveness", null));
        assertTrue(exclusion.verify("/info", null));
        assertFalse(exclusion.verify("/documents", null));
        assertFalse(exclusion.verify(null, null));
    }

    @ParameterizedTest @ValueSource(ints = {200, 302, 400, 404, 500, 502})
    void httpErrorHandlerDistinguishesStatusAndDomainException(int status) throws Exception {
        var handler = new RestTemplateResponseErrorHandler();
        var response = new MockClientHttpResponse("{\"detail\":\"failure\"}".getBytes(StandardCharsets.UTF_8), HttpStatus.valueOf(status));
        assertEquals(status >= 400, handler.hasError(response));
        if (status >= 400) {
            Class<? extends RuntimeException> expected = switch (status) {
                case 400 -> MetadataValidationException.class;
                case 404 -> NoRecordFoundException.class;
                default -> BusinessException.class;
            };
            assertThrows(expected, () -> handler.handleError(response));
        }
    }
}

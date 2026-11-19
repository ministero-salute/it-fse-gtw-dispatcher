package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.ConfigClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.response.WhoIsResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.ConfigClientRoutes;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ConfigItemDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ConfigItemTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.ProfileUtility;

@Tag("ISOLATION")
class ConfigurationClientBranchesIsolationTest {
    private ConfigClient client;
    private RestTemplate rest;
    private ConfigClientRoutes routes;
    private ProfileUtility profiles;

    @BeforeEach void setup() {
        var urls = mock(MicroservicesURLCFG.class);
        when(urls.getConfigHost()).thenReturn("http://config.test");
        routes = new ConfigClientRoutes();
        ReflectionTestUtils.setField(routes, "msUrlCFG", urls);
        rest = mock(RestTemplate.class);
        profiles = mock(ProfileUtility.class);
        client = new ConfigClient();
        ReflectionTestUtils.setField(client, "client", rest);
        ReflectionTestUtils.setField(client, "routes", routes);
        ReflectionTestUtils.setField(client, "profiles", profiles);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void unreachableLocalEnvironmentUsesDocumentedGatewayFallback(boolean dev) {
        when(profiles.isDevOrDockerProfile()).thenReturn(dev);
        when(profiles.isTestProfile()).thenReturn(!dev);
        when(rest.getForEntity(routes.status(), String.class)).thenThrow(new ResourceAccessException("offline"));
        assertEquals(Constants.Client.Config.MOCKED_GATEWAY_NAME, client.getGatewayName());
        verify(rest, never()).getForEntity(routes.whois(), WhoIsResponseDTO.class);
    }

    @Test void gatewayAndConfigurationResponsesAreReturned() {
        when(profiles.isTestProfile()).thenReturn(true);
        when(rest.getForEntity(routes.status(), String.class)).thenReturn(ResponseEntity.ok("up"));
        var body = mock(WhoIsResponseDTO.class);
        when(body.getGatewayName()).thenReturn("gateway");
        when(rest.getForEntity(routes.whois(), WhoIsResponseDTO.class)).thenReturn(ResponseEntity.ok(body));
        assertEquals("gateway", client.getGatewayName());
        var items = mock(ConfigItemDTO.class);
        when(rest.getForObject(routes.getConfigItems(ConfigItemTypeEnum.GENERIC), ConfigItemDTO.class)).thenReturn(items);
        assertSame(items, client.getConfigurationItems(ConfigItemTypeEnum.GENERIC));
        assertNotNull(routes.identifier());
        assertNotNull(routes.microservice());
    }

    @Test void nullAndNonSuccessfulGatewayResponsesAreNotAccepted() {
        when(rest.getForEntity(routes.whois(), WhoIsResponseDTO.class)).thenReturn(ResponseEntity.ok().build());
        assertThrows(BusinessException.class, client::getGatewayName);
        when(rest.getForEntity(routes.whois(), WhoIsResponseDTO.class))
            .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(mock(WhoIsResponseDTO.class)));
        assertThrows(BusinessException.class, client::getGatewayName);
        var failure = new IllegalStateException("bad transport");
        when(rest.getForEntity(routes.whois(), WhoIsResponseDTO.class)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, client::getGatewayName));
    }

    @Test void propertiesPreferSpecificThenGenericAndRetainPreviousWhenOffline() {
        var type = ConfigItemTypeEnum.DISPATCHER;
        when(rest.getForEntity(routes.status(), String.class)).thenReturn(ResponseEntity.ok("up"));
        when(rest.getForObject(routes.getConfigItem(type, "key"), String.class)).thenReturn("specific");
        assertEquals("specific", client.getProps("key", "previous", type));
        assertEquals("specific", client.getProps("key", "specific", type));
        when(rest.getForObject(routes.getConfigItem(type, "missing"), String.class)).thenReturn(null);
        when(rest.getForObject(routes.getConfigItem(ConfigItemTypeEnum.GENERIC, "missing"), String.class)).thenReturn("generic");
        assertEquals("generic", client.getProps("missing", "previous", type));
        assertNull(client.getProps("absent", "previous", type));
        when(rest.getForEntity(routes.status(), String.class)).thenThrow(new ResourceAccessException("offline"));
        assertEquals("previous", client.getProps("key", "previous", type));
    }
}

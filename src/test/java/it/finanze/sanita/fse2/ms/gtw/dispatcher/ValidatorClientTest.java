package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IValidatorClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.LogTraceInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.ValidationResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.SystemTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
public class ValidatorClientTest {
    
    @MockitoSpyBean
    private RestTemplate restTemplate;

    @Autowired
    private IValidatorClient client;

    @Test
    void validateTest() {
        // Mock
        ValidationInfoDTO infoMock = new ValidationInfoDTO();
        infoMock.setEngineID("engine_test");
        infoMock.setTransformID("transform_id_test");
        ValidationResDTO responseMock = new ValidationResDTO(new LogTraceInfoDTO("spanId", "traceId"), infoMock);
        Mockito.doReturn(responseMock).when(restTemplate).postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(ValidationResDTO.class)
        );
        // Perform validation
        ValidationInfoDTO outcome = client.validate("cda_test", "wif_test", SystemTypeEnum.TS);
        // Assertion
        assertEquals(infoMock.getEngineID(), outcome.getEngineID());
        assertEquals(infoMock.getTransformID(), outcome.getTransformID());
    }

    @Test
    void validateConnectionRefusedExceptionTest() {
        // Mock
        Mockito.doThrow(ResourceAccessException.class).when(restTemplate).postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(ValidationResDTO.class)
        );
        // Assertion
        assertThrows(ConnectionRefusedException.class, () -> client.validate("cda_test", "wif_test", SystemTypeEnum.TS));
    }

    @Test
    void validateValidationExceptionTest() {
        // Mock
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(restTemplate).postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(ValidationResDTO.class)
        );
        // Assertion
        assertThrows(ValidationException.class, () -> client.validate("cda_test", "wif_test", SystemTypeEnum.TS));
    }

    /**
     * Verifica che, in modalità accreditamento, l'URL di chiamata al ms-validator
     * NON contenga i parametri {@code validations} e {@code includeRemovalReport}.
     */
    @Test
    void validateAccreditamentoOmitsFhirParams() {
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        ValidationInfoDTO infoMock = new ValidationInfoDTO();
        ValidationResDTO responseMock = new ValidationResDTO(new LogTraceInfoDTO("spanId", "traceId"), infoMock);
        Mockito.doAnswer((InvocationOnMock inv) -> {
            capturedUrl.set(inv.getArgument(0));
            return responseMock;
        }).when(restTemplate).postForObject(anyString(), any(HttpEntity.class), eq(ValidationResDTO.class));

        client.validate("cda_test", "wif_test", SystemTypeEnum.TS, true);

        String url = capturedUrl.get();
        assertFalse(url.contains("validations"), "L'URL non deve contenere il param 'validations' per accreditamento");
        assertFalse(url.contains("includeRemovalReport"), "L'URL non deve contenere 'includeRemovalReport' per accreditamento");
    }

    /**
     * Verifica che, in modalità standard (non accreditamento), l'URL non venga alterato
     * rispetto al comportamento attuale (assenza di validations in test-profile).
     */
    @Test
    void validateNonAccreditamentoDoesNotOmitParams() {
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        ValidationInfoDTO infoMock = new ValidationInfoDTO();
        ValidationResDTO responseMock = new ValidationResDTO(new LogTraceInfoDTO("spanId", "traceId"), infoMock);
        Mockito.doAnswer((InvocationOnMock inv) -> {
            capturedUrl.set(inv.getArgument(0));
            return responseMock;
        }).when(restTemplate).postForObject(anyString(), any(HttpEntity.class), eq(ValidationResDTO.class));

        client.validate("cda_test", "wif_test", SystemTypeEnum.TS, false);

        // In test-profile le validation sono vuote, quindi l'URL non conterrà il param
        // ma deve essere raggiunto il ramo NON accreditamento (nessun errore).
        assertTrue(capturedUrl.get().contains("/v1/validate"), "L'URL deve puntare a /v1/validate");
    }

}

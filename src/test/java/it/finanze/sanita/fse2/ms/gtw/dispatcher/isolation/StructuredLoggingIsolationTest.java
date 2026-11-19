package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Date;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;
import com.google.gson.JsonParser;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IConfigClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.KafkaProducerPropertiesCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.LoggerHelper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;

@Tag("ISOLATION")
class StructuredLoggingIsolationTest {
    private LoggerHelper helper;
    private IConfigSRV config;
    private IConfigClient client;
    private Logger kafka;
    private KafkaProducerPropertiesCFG properties;

    @BeforeEach void setup() {
        helper = new LoggerHelper();
        config = mock(IConfigSRV.class);
        client = mock(IConfigClient.class);
        kafka = mock(Logger.class);
        properties = mock(KafkaProducerPropertiesCFG.class);
        ReflectionTestUtils.setField(helper, "configSRV", config);
        ReflectionTestUtils.setField(helper, "configClient", client);
        ReflectionTestUtils.setField(helper, "kafkaProducerCFG", properties);
        ReflectionTestUtils.setField(helper, "kafkaLog", kafka);
        ReflectionTestUtils.setField(helper, "kafkaLogEnable", true);
        ReflectionTestUtils.setField(helper, "msName", "dispatcher");
        when(properties.getMaxRequestSize()).thenReturn(1000000);
        when(client.getGatewayName()).thenReturn("gateway");
        when(config.isControlLogPersistenceEnable()).thenReturn(true);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void structuredLevelsPreserveOperationAndApplyPrivacyPolicy(boolean redact) {
        when(config.isSubjectNotAllowed()).thenReturn(redact);
        when(config.isCfOnIssuerNotAllowed()).thenReturn(redact);
        var jwt = JwtServiceIsolationTest.payload().sub("RSSMRA22A01A399Z^^^&2.16.840.1.113883.2.9.4.3.2&ISO").build();
        var date = new Date(0);
        helper.trace("control", "workflow", "trace", OperationLogEnum.PUB_CDA2, ResultLogEnum.OK, date, jwt, "doc");
        helper.debug("control", "workflow", "debug", OperationLogEnum.PUB_CDA2, ResultLogEnum.OK, date, jwt, "doc");
        helper.info("control", "workflow", "info", OperationLogEnum.PUB_CDA2, ResultLogEnum.OK, date, "REF", jwt, "extension", "doc");
        helper.warn("control", "workflow", "warn", OperationLogEnum.PUB_CDA2, ResultLogEnum.OK, date, jwt, "doc");
        helper.error("control", "workflow", "error", OperationLogEnum.PUB_CDA2, ResultLogEnum.OK, date, ErrorLogEnum.KO_EDS_CALLBACK, "REF", jwt, "doc");
        var messages = ArgumentCaptor.forClass(String.class);
        verify(kafka).info(messages.capture());
        var json = JsonParser.parseString(messages.getValue()).getAsJsonObject();
        assertEquals("workflow", json.get("workflow_instance_id").getAsString());
        assertEquals("gateway", json.get("gateway_name").getAsString());
        assertEquals("dispatcher", json.get("microservice_name").getAsString());
        assertEquals("REF", json.get("op_document_type").getAsString());
        assertEquals(!redact, json.has("op_issuer"));
        assertEquals(!redact, json.has("op_fiscal_code"));
        if (!redact) assertEquals("RSSMRA22A01A399Z", json.get("op_fiscal_code").getAsString());
        verify(kafka).trace(anyString());
        verify(kafka).debug(anyString());
        verify(kafka).warn(anyString());
        verify(kafka).error(anyString());
        verify(client).getGatewayName();
    }

    @ParameterizedTest @ValueSource(strings = {"SUCCESS", "ASYNC_RETRY", "BLOCKING_ERROR"})
    void callbacksPreserveBrokerStatusAndFallbackFiscalCode(String status) {
        helper.callback("control", "workflow", "callback", OperationLogEnum.EDS_CALLBACK, status, new Date(),
                status.equals("SUCCESS") ? null : ErrorLogEnum.KO_EDS_CALLBACK, "REF", null, "explicit-patient", "doc");
        var message = ArgumentCaptor.forClass(String.class);
        if (status.equals("SUCCESS")) verify(kafka).info(message.capture());
        else if (status.equals("ASYNC_RETRY")) verify(kafka).warn(message.capture());
        else verify(kafka).error(message.capture());
        var json = JsonParser.parseString(message.getValue()).getAsJsonObject();
        assertEquals(status, json.get("op_result").getAsString());
        assertEquals("explicit-patient", json.get("op_fiscal_code").getAsString());
    }

    @Test void nullTokensAreSupportedAndDisabledPersistenceEmitsNothing() {
        helper.trace("control", "w", "trace", OperationLogEnum.PUB_CDA2, ResultLogEnum.OK, new Date(), null, "doc");
        helper.debug("control", "w", "debug", OperationLogEnum.PUB_CDA2, ResultLogEnum.OK, new Date(), null, "doc");
        helper.info("control", "w", "info", OperationLogEnum.PUB_CDA2, ResultLogEnum.OK, new Date(), "REF", null, null, "doc");
        helper.warn("control", "w", "warn", OperationLogEnum.PUB_CDA2, ResultLogEnum.OK, new Date(), null, "doc");
        helper.error("control", "w", "error", OperationLogEnum.PUB_CDA2, ResultLogEnum.OK, new Date(), ErrorLogEnum.KO_EDS_CALLBACK, "REF", null, "doc");
        clearInvocations(kafka, client);
        when(config.isControlLogPersistenceEnable()).thenReturn(false);
        helper.info("control", "w", "ignored", null, null, null, null, null, null, null);
        helper.callback("control", "w", "ignored", null, null, null, null, null, null, null, null);
        verifyNoInteractions(kafka, client);
    }

    @Test void oversizedKafkaLogMessageIsTruncatedWithoutLosingMetadata() {
        when(properties.getMaxRequestSize()).thenReturn(1024);
        helper.info("control", "workflow", "x".repeat(2000), OperationLogEnum.PUB_CDA2, ResultLogEnum.OK,
                new Date(), "REF", null, null, "doc");
        var message = ArgumentCaptor.forClass(String.class);
        verify(kafka).info(message.capture());
        var json = JsonParser.parseString(message.getValue()).getAsJsonObject();
        assertEquals("x", json.get("message").getAsString());
        assertEquals("workflow", json.get("workflow_instance_id").getAsString());
    }
}

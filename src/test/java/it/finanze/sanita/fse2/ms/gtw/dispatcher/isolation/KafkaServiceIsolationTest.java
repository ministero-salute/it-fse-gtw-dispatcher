package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.KafkaSRV;

@Tag("ISOLATION")
@SuppressWarnings({"unchecked", "rawtypes"})
class KafkaServiceIsolationTest {
    private KafkaSRV service;
    private KafkaTemplate<String, String> tx, nonTx;
    private KafkaProducerPropertiesCFG properties;
    private KafkaTopicCFG topics;
    private RecordMetadata metadata;

    @BeforeEach void setup() {
        service = new KafkaSRV();
        tx = mock(KafkaTemplate.class);
        nonTx = mock(KafkaTemplate.class);
        properties = mock(KafkaProducerPropertiesCFG.class);
        topics = mock(KafkaTopicCFG.class);
        metadata = mock(RecordMetadata.class);
        ReflectionTestUtils.setField(service, "txKafkaTemplate", tx);
        ReflectionTestUtils.setField(service, "notxKafkaTemplate", nonTx);
        ReflectionTestUtils.setField(service, "kafkaProducerCFG", properties);
        ReflectionTestUtils.setField(service, "kafkaTopicCFG", topics);
        ReflectionTestUtils.setField(service, "msName", "dispatcher");
        when(properties.getMaxRequestSize()).thenReturn(1000000);
        when(topics.getStatusManagerTopic()).thenReturn("status");
        when(topics.getDispatcherIndexerTopic()).thenReturn("indexer");
        when(topics.getDispatcherIndexerRetryDeleteTopic()).thenReturn("retry-delete");
        when(topics.getDispatcherIndexerRetryUpdateTopic()).thenReturn("retry-update");
        when(tx.executeInTransaction(any())).thenAnswer(i -> ((KafkaOperations.OperationsCallback) i.getArgument(0)).doInOperations(tx));
        when(tx.send(any(ProducerRecord.class))).thenAnswer(i -> CompletableFuture.completedFuture(new SendResult<>(i.getArgument(0), metadata)));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void sendsStatusIndexerAndRetryRecordsWithConfiguredTransactionMode(boolean transactional) throws Exception {
        when(properties.getTransactionalId()).thenReturn(transactional ? "transaction" : "");
        var jwt = JwtServiceIsolationTest.payload().build();
        service.sendPublicationStatus("trace", "workflow", EventStatusEnum.SUCCESS, "done", FhirResourcesIsolationTest.request(), jwt, "https://callback");
        service.sendReplaceStatus("trace", "workflow", EventStatusEnum.SUCCESS, "done", FhirResourcesIsolationTest.request(), jwt, null);
        service.sendValidationStatus("trace", "workflow", EventStatusEnum.SUCCESS, "done", jwt);
        service.sendValidationStatus("trace", "workflow", EventStatusEnum.SUCCESS, "done", null, EventTypeEnum.FHIR_VALIDATION);
        service.sendDeleteStatus("trace", "workflow", "doc", "done", EventStatusEnum.SUCCESS, jwt, EventTypeEnum.DELETE);
        service.sendUpdateStatus("trace", "workflow", "doc", EventStatusEnum.SUCCESS, jwt, "done", EventTypeEnum.UPDATE);
        service.sendEdsUarStatus("workflow", EventStatusEnum.SUCCESS, "done");
        service.notifyChannel("doc", "payload", TipoDocAltoLivEnum.REF, DestinationTypeEnum.INDEXER);
        service.sendDeleteRequest("workflow", Map.of("id", "doc"));
        service.sendUpdateRequest("workflow", Map.of("id", "doc"));
        var records = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(transactional ? tx : nonTx, times(10)).send(records.capture());
        verifyNoInteractions(transactional ? nonTx : tx);
        var first = records.getAllValues().get(0);
        assertEquals("status", first.topic());
        assertEquals("workflow", first.key());
        var json = new ObjectMapper().readTree((String) first.value());
        assertEquals("PUBLICATION", json.path("eventType").asText());
        assertEquals("issuer", json.path("issuer").asText());
        assertEquals("https://callback", json.path("callbackUrl").asText());
        assertEquals("dispatcher", json.path("microserviceName").asText());
        assertEquals("indexer", records.getAllValues().get(7).topic());
        assertEquals("payload", records.getAllValues().get(7).value());
        assertEquals("retry-delete", records.getAllValues().get(8).topic());
        assertEquals("retry-update", records.getAllValues().get(9).topic());
    }

    @Test void returnsTransactionalMetadataAndTruncatesLargeStatusMessages() throws Exception {
        assertSame(metadata, service.sendMessage("topic", "key", "value", true));
        when(properties.getMaxRequestSize()).thenReturn(1024);
        service.sendValidationStatus("trace", "workflow", EventStatusEnum.SUCCESS, "x".repeat(2000), null);
        var record = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(nonTx).send(record.capture());
        assertEquals("x", new ObjectMapper().readTree((String) record.getValue().value()).path("message").asText());
    }

    @Test void wrapsSendAndTransactionFailures() {
        var failure = new IllegalStateException("broker offline");
        when(nonTx.send(any(ProducerRecord.class))).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> service.sendMessage("topic", "key", "value", false)).getCause());
        assertThrows(BusinessException.class, () -> service.sendValidationStatus("trace", "workflow", EventStatusEnum.SUCCESS, "done", null));
        assertThrows(BusinessException.class, () -> service.sendEdsUarStatus("workflow", EventStatusEnum.SUCCESS, "done"));
        assertThrows(BusinessException.class, () -> service.notifyChannel("doc", "value", TipoDocAltoLivEnum.REF, DestinationTypeEnum.INDEXER));
        when(tx.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.failedFuture(failure));
        assertThrows(BusinessException.class, () -> service.sendMessage("topic", "key", "value", true));
    }
}

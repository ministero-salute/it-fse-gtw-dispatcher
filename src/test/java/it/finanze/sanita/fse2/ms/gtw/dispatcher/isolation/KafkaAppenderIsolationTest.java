package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.*;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.read.ListAppender;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.*;

@Tag("ISOLATION")
class KafkaAppenderIsolationTest {
    private static class InMemoryAppender extends KafkaAppender<ILoggingEvent> {
        final Producer<byte[], byte[]> producer = mock(Producer.class);
        boolean fail;
        int creations;
        @Override protected Producer<byte[], byte[]> createProducer() {
            creations++;
            if (fail) throw new KafkaException("offline");
            return producer;
        }
    }

    private InMemoryAppender appender(boolean fail) {
        var appender = new InMemoryAppender();
        appender.setContext(new LoggerContext());
        appender.setName("test");
        appender.fail = fail;
        appender.setTopic("logs");
        appender.addProducerConfig("bootstrap.servers=localhost:9092");
        appender.addProducerConfig("ignored");
        var encoder = mock(Encoder.class);
        when(encoder.encode(any())).thenReturn("payload".getBytes(StandardCharsets.UTF_8));
        appender.setEncoder(encoder);
        appender.setPartition(-1);
        return appender;
    }

    private ILoggingEvent event(String name) {
        var event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn(name);
        when(event.getTimeStamp()).thenReturn(123L);
        return event;
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void lazilySendsEncodedRecordAndDefersKafkaInternalLogs(boolean timestamp) {
        var appender = appender(false);
        appender.setAppendTimestamp(timestamp);
        appender.start();
        assertTrue(appender.isStarted());
        assertEquals(0, appender.creations);
        var internal = event("org.apache.kafka.clients.NetworkClient");
        appender.doAppend(internal);
        assertEquals(0, appender.creations);
        appender.doAppend(event("application"));
        assertEquals(1, appender.creations);
        var records = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(appender.producer, times(2)).send(records.capture(), any());
        var record = records.getValue();
        assertEquals("logs", record.topic());
        assertNull(record.partition());
        assertEquals(timestamp ? 123L : null, record.timestamp());
        assertArrayEquals("payload".getBytes(StandardCharsets.UTF_8), (byte[]) record.value());
        assertNull(record.key());
        appender.stop();
        verify(appender.producer).close();
    }

    @Test void initializationFailureFallsBackAndAttachedAppendersCanBeManaged() {
        var appender = appender(true);
        var fallback = new ListAppender<ILoggingEvent>();
        fallback.setName("fallback");
        fallback.start();
        appender.addAppender(fallback);
        assertSame(fallback, appender.getAppender("fallback"));
        assertTrue(appender.isAttached(fallback));
        assertTrue(appender.iteratorForAppenders().hasNext());
        appender.start();
        var event = event("application");
        appender.doAppend(event);
        assertEquals(java.util.List.of(event), fallback.list);
        assertTrue(appender.detachAppender("fallback"));
        appender.addAppender(fallback);
        assertTrue(appender.detachAppender(fallback));
        appender.addAppender(fallback);
        appender.detachAndStopAllAppenders();
        assertFalse(fallback.isStarted());
        appender.stop();
        verifyNoInteractions(appender.producer);
    }

    @Test void incompleteConfigurationNeverStartsAndCloseFailureIsContained() {
        var incomplete = new InMemoryAppender();
        incomplete.setContext(new LoggerContext());
        incomplete.start();
        assertFalse(incomplete.isStarted());
        var appender = appender(false);
        appender.start();
        appender.doAppend(event("application"));
        doThrow(new KafkaException("close failed")).when(appender.producer).close();
        assertDoesNotThrow(appender::stop);
        assertFalse(appender.isStarted());
    }
}

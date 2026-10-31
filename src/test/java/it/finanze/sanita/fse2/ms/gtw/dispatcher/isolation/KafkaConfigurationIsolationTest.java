package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.oauth2.CustomAuthenticateCallbackHandler;
import org.apache.kafka.common.serialization.StringSerializer;

@Tag("ISOLATION")
class KafkaConfigurationIsolationTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void constructsTransactionalAndNonTransactionalFactoriesWithoutConnecting(boolean secure) {
        var config = new KafkaProducerCFG();
        var properties = mock(KafkaPropertiesCFG.class);
        var producer = mock(KafkaProducerPropertiesCFG.class);
        ReflectionTestUtils.setField(config, "kafkaPropCFG", properties);
        ReflectionTestUtils.setField(config, "kafkaProducerPropCFG", producer);
        when(producer.getClientId()).thenReturn("dispatcher");
        when(producer.getProducerBootstrapServers()).thenReturn("localhost:9092");
        when(producer.getKeySerializer()).thenReturn(StringSerializer.class.getName());
        when(producer.getValueSerializer()).thenReturn(StringSerializer.class.getName());
        when(producer.getTransactionalId()).thenReturn("transaction");
        when(producer.getAck()).thenReturn("all");
        if (secure) {
            when(properties.getProtocol()).thenReturn("SASL_SSL");
            when(properties.getMechanism()).thenReturn("OAUTHBEARER");
            when(properties.getConfigJaas()).thenReturn("jaas");
            when(properties.getTrustoreLocation()).thenReturn("truststore");
            when(properties.getTrustorePassword()).thenReturn("secret".toCharArray());
            when(properties.getTenantId()).thenReturn("tenant");
            when(properties.getAppId()).thenReturn("app");
            when(properties.getPfxPathName()).thenReturn("certificate");
            when(properties.getPwd()).thenReturn("password");
            when(properties.getCallbackHandlerClass()).thenReturn("handler");
        }
        var tx = config.producerConfigs();
        var plain = config.producerWithoutTransactionConfigs();
        assertTrue(tx.get("client.id").toString().startsWith("dispatcher-tx-"));
        assertTrue(tx.get("transactional.id").toString().endsWith("-transaction"));
        assertEquals("dispatcher-noTx", plain.get("client.id"));
        assertFalse(plain.containsKey("transactional.id"));
        for (Map<String, Object> values : java.util.List.of(tx, plain)) {
            assertEquals("localhost:9092", values.get("bootstrap.servers"));
            assertEquals(StringSerializer.class.getName(), values.get("key.serializer"));
            assertTrue(values.get("interceptor.classes").toString().contains("TracingProducerInterceptor"));
            if (secure) {
                assertEquals("SASL_SSL", values.get("security.protocol"));
                assertEquals("secret", values.get("ssl.truststore.password"));
                assertEquals(CustomAuthenticateCallbackHandler.class, values.get("sasl.login.callback.handler.class"));
                assertEquals("tenant", values.get("kafka.oauth.tenantId"));
                assertEquals("handler", values.get("sasl.client.callback.handler.class"));
            } else assertFalse(values.containsKey("security.protocol"));
        }
        var factory = config.producerFactory();
        var factoryConfig = new java.util.HashMap<>(tx);
        factoryConfig.remove("transactional.id");
        assertEquals(factoryConfig, factory.getConfigurationProperties());
        assertTrue(factory.transactionCapable());
        assertEquals(plain, config.producerFactoryWithoutTransaction().getConfigurationProperties());
        assertTrue(config.txKafkaTemplate().isTransactional());
        assertFalse(config.notxKafkaTemplate().isTransactional());
    }
}

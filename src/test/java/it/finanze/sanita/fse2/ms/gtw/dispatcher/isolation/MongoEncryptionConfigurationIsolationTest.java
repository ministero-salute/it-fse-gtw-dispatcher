package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.bson.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.client.model.vault.*;
import com.mongodb.client.vault.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.mongo.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.CloudProviderEnum;

@Tag("ISOLATION")
class MongoEncryptionConfigurationIsolationTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void azureInitializationReusesOrCreatesKeyAndEncryptsWithDeterministicAlgorithm(boolean existing) {
        var config = new MongoDatabaseCFG();
        var properties = mock(MongoPropertiesCFG.class);
        when(properties.getUri()).thenReturn("mongodb://localhost:27017/dispatcher");
        when(properties.isEncryptionEnabled()).thenReturn(true);
        var azure = mock(AzureCfg.class);
        when(azure.getTenantId()).thenReturn("tenant");
        when(azure.getClientId()).thenReturn("client");
        when(azure.getClientSecret()).thenReturn("secret");
        when(azure.getMasterKeyName()).thenReturn("master-key");
        when(azure.getKeyVaultEndpoint()).thenReturn("vault.test");
        ReflectionTestUtils.setField(config, "mongoPropsCfg", properties);
        ReflectionTestUtils.setField(config, "azureCfg", azure);
        ReflectionTestUtils.setField(config, "cloudProvider", CloudProviderEnum.AZURE);
        ReflectionTestUtils.setField(config, "dataKeyIdName", "dispatcher-key");
        var encryption = mock(ClientEncryption.class);
        var key = new BsonBinary(new byte[16]);
        when(encryption.getKeyByAltName("dispatcher-key")).thenReturn(existing ? new BsonDocument("_id", key) : null);
        when(encryption.createDataKey(eq("azure"), any())).thenReturn(key);
        var encrypted = new BsonBinary(new byte[] {4, 5, 6});
        when(encryption.encrypt(any(), any())).thenReturn(encrypted);
        try (var clients = mockStatic(ClientEncryptions.class)) {
            var settings = ArgumentCaptor.forClass(ClientEncryptionSettings.class);
            clients.when(() -> ClientEncryptions.create(any())).thenReturn(encryption);
            config.init();
            clients.verify(() -> ClientEncryptions.create(settings.capture()));
            assertEquals("encryption.__keyVault", settings.getValue().getKeyVaultNamespace());
            assertEquals("tenant", settings.getValue().getKmsProviders().get("azure").get("tenantId"));
            assertEquals("secret", settings.getValue().getKmsProviders().get("azure").get("clientSecret"));
            if (!existing) {
                var options = ArgumentCaptor.forClass(DataKeyOptions.class);
                verify(encryption).createDataKey(eq("azure"), options.capture());
                assertEquals(java.util.List.of("dispatcher-key"), options.getValue().getKeyAltNames());
                assertEquals("master-key", options.getValue().getMasterKey().getString("keyName").getValue());
            } else verify(encryption, never()).createDataKey(anyString(), any());
            var value = new BsonString("sensitive");
            assertSame(encrypted, config.encrypt(value));
            var options = ArgumentCaptor.forClass(EncryptOptions.class);
            verify(encryption).encrypt(same(value), options.capture());
            assertEquals(key, options.getValue().getKeyId());
            assertEquals("AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic", options.getValue().getAlgorithm());
        }
    }

    @Test void disabledEncryptionPreservesBinaryAndAwsMasterKeyUsesConfiguredRegion() {
        var config = new MongoDatabaseCFG();
        var properties = mock(MongoPropertiesCFG.class);
        when(properties.getUri()).thenReturn("mongodb://localhost:27017/dispatcher");
        ReflectionTestUtils.setField(config, "mongoPropsCfg", properties);
        config.init();
        var binary = new BsonBinary(new byte[] {1, 2});
        assertSame(binary, config.encrypt(binary));
        var aws = mock(AwsCfg.class);
        when(aws.getRegion()).thenReturn("eu-west-1");
        when(aws.getMasterKeyArn()).thenReturn("arn:key");
        ReflectionTestUtils.setField(config, "awsCfg", aws);
        var key = config.configureMasterKeyProperties(CloudProviderEnum.AWS);
        assertEquals("eu-west-1", key.getString("region").getValue());
        assertEquals("arn:key", key.getString("key").getValue());
    }
}

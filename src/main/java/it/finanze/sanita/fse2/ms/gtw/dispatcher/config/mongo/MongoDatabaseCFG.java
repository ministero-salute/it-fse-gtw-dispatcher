/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * Copyright (C) 2023 Ministero della Salute
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher.config.mongo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.model.vault.EncryptOptions;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.AwsCfg;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.AzureCfg;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.CloudProviderEnum;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

@Configuration
@Slf4j
@Getter
public class MongoDatabaseCFG {

	private static final String ALG = "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic";
	private static final String KEY_VAULT_NAMESPACE = "encryption.__keyVault";

	private static final String KMS_PROVIDER_AZURE = "azure";
	private static final String KMS_PROVIDER_AWS = "aws";

	private final Map<String, Map<String, Object>> kmsProviders = new HashMap<>();
	private ClientEncryption clientEncryption;
	private BsonBinary datakeyId;

	@Autowired
	private MongoPropertiesCFG mongoPropsCfg;

	@Value("${data.mongodb.crypting.datakey-id-name}")
	private String dataKeyIdName;

	@Autowired
	private AzureCfg azureCfg;
	
	@Autowired
	private AwsCfg awsCfg;

	@Value("${cloud.provider:#{null}}")
    private CloudProviderEnum cloudProvider;

	@PostConstruct
	public void init() {
		if (mongoPropsCfg.isEncryptionEnabled()) {
			configureKmsProviders(cloudProvider);
		}
	}

	/**
	 * Configura MongoDB factory
	 */
	@Bean
	public MongoDatabaseFactory mongoDatabaseFactory() {
		ConnectionString connectionString = new ConnectionString(mongoPropsCfg.getUri());
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
				.applyConnectionString(connectionString)
				.build();

		if (mongoPropsCfg.isEncryptionEnabled()) {
			generateOrRetrieveDataKeyId(mongoClientSettings,cloudProvider);
		}

		return new SimpleMongoClientDatabaseFactory(MongoClients.create(mongoClientSettings), mongoPropsCfg.getSchemaName());
	}

	/**
	 * Restituisce un MongoTemplate configurato con custom converters
	 */
	@Bean
	@Primary
	public MongoTemplate mongoTemplate(final MongoDatabaseFactory factory, final ApplicationContext appContext) {
		final MongoMappingContext mongoMappingContext = new MongoMappingContext();
		mongoMappingContext.setApplicationContext(appContext);
		MappingMongoConverter converter = new MappingMongoConverter(new DefaultDbRefResolver(factory),
				mongoMappingContext);
		converter.setTypeMapper(new DefaultMongoTypeMapper(null));
		return new MongoTemplate(factory, converter);
	}


	private void generateOrRetrieveDataKeyId(MongoClientSettings settings, CloudProviderEnum cloudProviderEnum) {

		ClientEncryptionSettings clientEncryptionSettings = ClientEncryptionSettings.builder()
				.keyVaultMongoClientSettings(settings)
				.keyVaultNamespace(KEY_VAULT_NAMESPACE)
				.kmsProviders(kmsProviders)
				.build();

		clientEncryption = ClientEncryptions.create(clientEncryptionSettings);
		BsonDocument keyDocument = clientEncryption.getKeyByAltName(dataKeyIdName);
		if (keyDocument == null) {
			log.info("No existing key found with alias 'dispatcherKey'. Creating a new key...");


			switch (cloudProviderEnum) {
			case AWS:
				datakeyId = clientEncryption.createDataKey(KMS_PROVIDER_AWS, new DataKeyOptions()
						.keyAltNames(Collections.singletonList(dataKeyIdName))
						.masterKey(configureMasterKeyProperties(CloudProviderEnum.AWS)));
				break;

			case AZURE:
				datakeyId = clientEncryption.createDataKey(KMS_PROVIDER_AZURE, 
						new DataKeyOptions().keyAltNames(Collections.singletonList(dataKeyIdName)).masterKey(configureMasterKeyProperties(CloudProviderEnum.AZURE)));
				break;

			default:
				throw new IllegalArgumentException("Unexpected value: " + cloudProviderEnum);
			}
		} else {
			log.info("Existing key found with alias 'dispatcherKey'.");
			datakeyId = keyDocument.getBinary("_id");
		}
	}

	public BsonBinary encrypt(BsonValue bsonValue) {
		if (!mongoPropsCfg.isEncryptionEnabled()) {
			return bsonValue.asBinary();
		}

		EncryptOptions options = new EncryptOptions(ALG).keyId(datakeyId);
		return clientEncryption.encrypt(bsonValue, options);
	}

	public void configureKmsProviders(CloudProviderEnum cloudProviderEnum) {
		Map<String, Object> providerDetails = new HashMap<>();

		switch (cloudProviderEnum) {
		case AWS:
			DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.builder().build();
			AwsCredentials credentials = credentialsProvider.resolveCredentials();
			providerDetails.put("accessKeyId", credentials.accessKeyId());
			providerDetails.put("secretAccessKey", credentials.secretAccessKey());
			if (credentials instanceof AwsSessionCredentials) {
				providerDetails.put("sessionToken", ((AwsSessionCredentials) credentials).sessionToken());
			}
			kmsProviders.put(KMS_PROVIDER_AWS, providerDetails);
			break;

		case AZURE:
			providerDetails.put("tenantId", azureCfg.getTenantId());  
			providerDetails.put("clientId", azureCfg.getClientId());  
			providerDetails.put("clientSecret", azureCfg.getClientSecret()); 
			kmsProviders.put(KMS_PROVIDER_AZURE, providerDetails);
			break;

		default:
			throw new IllegalArgumentException("Unexpected value: " + cloudProviderEnum);
		}
	}


	public BsonDocument configureMasterKeyProperties(CloudProviderEnum cloudProviderEnum) {
		BsonDocument masterKeyProperties = new BsonDocument();

		switch (cloudProviderEnum) {
		case AZURE:
			masterKeyProperties.put("provider", new BsonString(KMS_PROVIDER_AZURE));
			masterKeyProperties.put("keyName", new BsonString(azureCfg.getMasterKeyName()));
			masterKeyProperties.put("keyVaultEndpoint", new BsonString(azureCfg.getKeyVaultEndpoint()));
			break;

		case AWS:
			masterKeyProperties.put("region",  new BsonString(awsCfg.getRegion()));
			masterKeyProperties.put("key", new BsonString(awsCfg.getMasterKeyArn()));
			break;

		default:
			throw new IllegalArgumentException("Unsupported cloud provider: " + cloudProviderEnum);
		}

		return masterKeyProperties;
	}
}

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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.oauth2.CustomAuthenticateCallbackHandler;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

/**
 *	Kafka producer configuration.
 */
@Configuration
@Slf4j
public class KafkaProducerCFG {


	/**
	 *	Kafka properties.
	 */
	@Autowired
	private KafkaPropertiesCFG kafkaPropCFG;

	/**
	 *	Kafka producer properties.
	 */
	@Autowired
	private KafkaProducerPropertiesCFG kafkaProducerPropCFG;


	/** 
	 *  Kafka producer configurazione.
	 */
	@Bean 
	public Map<String, Object> producerConfigs() {
		Map<String, Object> props = new HashMap<>();

		InetAddress id = getLocalHost();

		props.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaProducerPropCFG.getClientId() + "-tx" + "-" + id );
		props.put(ProducerConfig.RETRIES_CONFIG, kafkaProducerPropCFG.getRetries());
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProducerPropCFG.getProducerBootstrapServers());
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProducerPropCFG.getKeySerializer());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProducerPropCFG.getValueSerializer());
		props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, id + "-" + kafkaProducerPropCFG.getTransactionalId());
		props.put(ProducerConfig.ACKS_CONFIG,kafkaProducerPropCFG.getAck());
		props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,kafkaProducerPropCFG.getIdempotence());

		if (!StringUtility.isNullOrEmpty(kafkaPropCFG.getProtocol())) {
			props.put("security.protocol", kafkaPropCFG.getProtocol());
		}
		
		if (!StringUtility.isNullOrEmpty(kafkaPropCFG.getMechanism())) {
			props.put("sasl.mechanism", kafkaPropCFG.getMechanism());
		}
		
		if (!StringUtility.isNullOrEmpty(kafkaPropCFG.getConfigJaas())) {
			props.put("sasl.jaas.config", kafkaPropCFG.getConfigJaas());
		}
		
		if (!StringUtility.isNullOrEmpty(kafkaPropCFG.getTrustoreLocation())) {
			props.put("ssl.truststore.location", kafkaPropCFG.getTrustoreLocation());
		}
		
		if (kafkaPropCFG.getTrustorePassword() != null && kafkaPropCFG.getTrustorePassword().length > 0) {
			props.put("ssl.truststore.password", String.valueOf(kafkaPropCFG.getTrustorePassword()));
			System.out.println("TRUSTORE PWD:"+String.valueOf(kafkaPropCFG.getTrustorePassword()));
		}

		if("OAUTHBEARER".equals(kafkaPropCFG.getMechanism())) {
			props.put("sasl.login.callback.handler.class", CustomAuthenticateCallbackHandler.class);
			props.put("kafka.oauth.tenantId", kafkaPropCFG.getTenantId());	
			props.put("kafka.oauth.appId", kafkaPropCFG.getAppId());
			props.put("kafka.oauth.pfxPathName", kafkaPropCFG.getPfxPathName());
			props.put("kafka.oauth.pwd", kafkaPropCFG.getPwd());
		}

		return props;
	}

	/**
	 * @param id
	 * @return
	 */
	private InetAddress getLocalHost() {
		InetAddress id = null;
		try {
			id = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			log.error("Errore durante il recupero InetAddress.getLocalHost()", e);
		}
		return id;
	}

	/**
	 * Transactional producer.
	 */
	@Bean
	@Qualifier("txkafkatemplateFactory") 
	public ProducerFactory<String, String> producerFactory() {
		log.info("Initialization of transactional Factory");
		return new DefaultKafkaProducerFactory<>(producerConfigs());
	}

	/**
	 *  Kafka template.
	 */
	@Bean
	@Qualifier("txkafkatemplate") 
	public KafkaTemplate<String, String> txKafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}

	/**
	 * Non transactional producer config.
	 */
	@Bean 
	public Map<String, Object> producerWithoutTransactionConfigs() {
		Map<String, Object> props = new HashMap<>();

		props.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaProducerPropCFG.getClientId()+ "-noTx");
		props.put(ProducerConfig.RETRIES_CONFIG, kafkaProducerPropCFG.getRetries());
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProducerPropCFG.getProducerBootstrapServers());
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProducerPropCFG.getKeySerializer());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProducerPropCFG.getValueSerializer());

		if (!StringUtility.isNullOrEmpty(kafkaPropCFG.getProtocol())) {
			props.put("security.protocol", kafkaPropCFG.getProtocol());
		}
		
		if (!StringUtility.isNullOrEmpty(kafkaPropCFG.getMechanism())) {
			props.put("sasl.mechanism", kafkaPropCFG.getMechanism());
		}
		
		if (!StringUtility.isNullOrEmpty(kafkaPropCFG.getConfigJaas())) {
			props.put("sasl.jaas.config", kafkaPropCFG.getConfigJaas());
		}
		
		if (!StringUtility.isNullOrEmpty(kafkaPropCFG.getTrustoreLocation())) {
			props.put("ssl.truststore.location", kafkaPropCFG.getTrustoreLocation());
		}
		
		if (kafkaPropCFG.getTrustorePassword() != null && kafkaPropCFG.getTrustorePassword().length > 0) {
			props.put("ssl.truststore.password", String.valueOf(kafkaPropCFG.getTrustorePassword()));
		}

		if("OAUTHBEARER".equals(kafkaPropCFG.getMechanism())) {
			props.put("sasl.login.callback.handler.class", CustomAuthenticateCallbackHandler.class);
			props.put("kafka.oauth.tenantId", kafkaPropCFG.getTenantId());	
			props.put("kafka.oauth.appId", kafkaPropCFG.getAppId());
			props.put("kafka.oauth.pfxPathName", kafkaPropCFG.getPfxPathName());
			props.put("kafka.oauth.pwd", kafkaPropCFG.getPwd());
		}


		return props;
	}

	/**
	 * Non transactional producer.
	 */ 
	@Bean
	@Qualifier("notxkafkatemplateFactory") 
	public ProducerFactory<String, String> producerFactoryWithoutTransaction() {
		log.info("Initialization of non transactional Factory");
		return new DefaultKafkaProducerFactory<>(producerWithoutTransactionConfigs());
	}

	/**
	 * Non transactional kafka template.
	 */ 
	@Bean
	@Qualifier("notxkafkatemplate") 
	public KafkaTemplate<String, String> notxKafkaTemplate() {
		return new KafkaTemplate<>(producerFactoryWithoutTransaction());
	}


}
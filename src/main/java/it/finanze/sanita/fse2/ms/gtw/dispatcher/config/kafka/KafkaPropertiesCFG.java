
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

import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.ProfileUtility;
import lombok.Data;

/**
 *	Kafka properties configuration.
 */
@Data
@Configuration
public class KafkaPropertiesCFG {
  

	/**
	 *  Boostrap server.
	 */
	@Value("${kafka.bootstrap-servers}")
	private String producerBootstrapServers;
	  
	
	/**
	 * Security protocol.
	 */
	@Value("${kafka.properties.security.protocol}")
	private String protocol;
	
	/**
	 * Sasl mechanism.
	 */
	@Value("${kafka.properties.sasl.mechanism}")
	private String mechanism;
	
	/**
	 * Jaas config.
	 */
	@Value("${kafka.properties.sasl.jaas.config}")
	private String configJaas;
	
	/**
	 * Trustore location.
	 */
	@Value("${kafka.properties.ssl.truststore.location}")
	private String trustoreLocation;
	
	/**
	 * Trustore password.
	 */
	@Value("${kafka.properties.ssl.truststore.password}")
	private transient char[] trustorePassword;
	 
	@Value("${kafka.oauth.tenantId}")
	private String tenantId;

	@Value("${kafka.oauth.appId}")
	private String appId;

	@Value("${kafka.oauth.pfxPathName}")
	private String pfxPathName;
	
	@Value("${kafka.oauth.pwd}")
	private String pwd;

	@Autowired
	private ProfileUtility profileUtility;

	@Bean
	@ConditionalOnProperty(name = "sasl.mechanism", havingValue = "OAUTHBEARER", matchIfMissing = false)
	public AdminClient client() {
		Properties configProperties = new Properties();
    	configProperties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, producerBootstrapServers);
    	if(!profileUtility.isDevOrDockerProfile() && !profileUtility.isTestProfile()) {
    		configProperties.put("security.protocol", protocol);
    		configProperties.put("sasl.mechanism", mechanism);
    		configProperties.put("sasl.jaas.config", configJaas);
    		configProperties.put("ssl.truststore.location", trustoreLocation);  
    		configProperties.put("ssl.truststore.password", String.valueOf(trustorePassword)); 
		}
		return AdminClient.create(configProperties);
	}
}

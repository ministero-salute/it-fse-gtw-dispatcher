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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 *	Kafka producer properties configuration.
 */
@Data
@Component
public class KafkaProducerPropertiesCFG {
  
	/**
	 * Boostrap server.
	 */
	@Value("${kafka.bootstrap-servers}")
	private String producerBootstrapServers;
	
	/**
	 * Client id.
	 */
	@Value("${kafka.producer.client-id}")
	private String clientId;
	
	/**
	 * Retries.
	 */
	@Value("${kafka.producer.retries}")
	private Integer retries;
	
	/**
	 * Key serializer.
	 */
	@Value("${kafka.producer.key-serializer}")
	private String keySerializer;
	
	/**
	 * Value serializer.
	 */
	@Value("${kafka.producer.value-serializer}")
	private String valueSerializer;
	
	/**
	 * Transactional id.
	 */
	@Value("${kafka.producer.transactional.id}")
	private String transactionalId;  
	
	/**
	 * Idempotence.
	 */
	@Value("${kafka.producer.enable.idempotence}")
	private Boolean idempotence;  
	
	/**
	 * Ack.
	 */
	@Value("${kafka.producer.ack}")
	private String ack;   

	/**
	 * Producer max request size in bytes.
	 */
	@Value("${kafka.producer.max.request.size:1048576}")
	private Integer maxRequestSize; 

	
	 
}

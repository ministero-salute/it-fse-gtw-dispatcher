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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.SettableListenableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.KafkaProducerPropertiesCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.KafkaTopicCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.KafkaStatusManagerDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreateReplaceMetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.AttivitaClinicaEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DestinationTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.PriorityTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.TipoDocAltoLivEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IKafkaSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.PriorityUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka management service.
 */
@Service
@Slf4j
public class KafkaSRV implements IKafkaSRV {

	@Autowired
	private KafkaTopicCFG kafkaTopicCFG;

	@Value("${spring.application.name}")
	private String msName;

	/**
	 * Transactional producer.
	 */
	@Autowired
	@Qualifier("txkafkatemplate")
	private KafkaTemplate<String, String> txKafkaTemplate;

	/**
	 * Not transactional producer.
	 */
	@Autowired
	@Qualifier("notxkafkatemplate")
	protected KafkaTemplate<String, String> notxKafkaTemplate;

	@Autowired
	private PriorityUtility priorityUtility;

	@Autowired
	private KafkaProducerPropertiesCFG kafkaProducerCFG;
 

	@SuppressWarnings("unchecked")
	@Override
	public void kafkaSendNonInTransaction(ProducerRecord<String, String> producerRecord) {
		notxKafkaTemplate.send(producerRecord);
	}
	
	@SuppressWarnings("unchecked")
	private List<RecordMetadata> kafkaSendInTransaction(List<ProducerRecord<String, String>> producerRecords) {
	    List<RecordMetadata> out = new ArrayList<>();
	    
	    txKafkaTemplate.executeInTransaction(t -> {
	        try {
	            for (ProducerRecord<String, String> producerRecord : producerRecords) {
	                SendResult<String, String> sendResult = t.send(producerRecord).get();
	                if (sendResult != null) {
	                    out.add(sendResult.getRecordMetadata());
	                    log.debug("Message sent successfully: {}", sendResult.getRecordMetadata());
	                }
	            }
	        } catch (InterruptedException e) {
	            log.error("InterruptedException caught. Interrupting thread...");
	            Thread.currentThread().interrupt();
	            throw new BusinessException(e);
	        } catch (Exception e) {
	            throw new BusinessException(e);
	        }
	        return null;
	    });

	    return out;
	}
	
	@Override
	public void notifyIndexerAndStatusInTransaction(final String key, final String kafkaValue, PriorityTypeEnum priorityFromRequest,
			TipoDocAltoLivEnum documentType, 
			String traceId, String workflowInstanceId,
			EventStatusEnum eventStatus, String message,
			PublicationCreateReplaceMetadataDTO publicationReq, JWTPayloadDTO jwtClaimDTO,
			EventTypeEnum eventTypeEnum) {
		ProducerRecord<String, String> indexerValue = notifyIndexerChannel(key, kafkaValue, priorityFromRequest, documentType, DestinationTypeEnum.INDEXER);
		ProducerRecord<String, String> statusValue = getStatus(traceId, workflowInstanceId, eventStatus, message, publicationReq, jwtClaimDTO,eventTypeEnum);
		kafkaSendInTransaction(Arrays.asList(indexerValue,statusValue));
	}

	private ProducerRecord<String, String> notifyIndexerChannel(final String key, final String kafkaValue, PriorityTypeEnum priorityFromRequest,
			TipoDocAltoLivEnum documentType, DestinationTypeEnum destinationType) {
		log.debug("Destination: {}", destinationType.name());
		String destTopic = priorityUtility.computeTopic(priorityFromRequest, destinationType, documentType);

		return getProducerRecord(destTopic, key, kafkaValue);
	 
	}

	@Override
	public void sendValidationStatus(final String traceId, final String workflowInstanceId,
			final EventStatusEnum eventStatus, final String message,
			final JWTPayloadDTO jwtClaimDTO) {
		ProducerRecord<String, String> status = getStatusMessage(traceId, workflowInstanceId, EventTypeEnum.VALIDATION, eventStatus, message, null,
				jwtClaimDTO, null);
		kafkaSendNonInTransaction(status);
	}

	@Override
	public void sendValidationStatus(final String traceId, final String workflowInstanceId,
			final EventStatusEnum eventStatus, final String message,
			final JWTPayloadDTO jwtClaimDTO, EventTypeEnum eventTypeEnum) {
		ProducerRecord<String, String> status = getStatusMessage(traceId, workflowInstanceId, eventTypeEnum, eventStatus, message, null, jwtClaimDTO, null);
		kafkaSendNonInTransaction(status);
	}

	@Override
	public ProducerRecord<String, String> getStatus(final String traceId, final String workflowInstanceId,
			final EventStatusEnum eventStatus, final String message,
			final PublicationCreateReplaceMetadataDTO publicationReq, final JWTPayloadDTO jwtClaimDTO,
			EventTypeEnum eventTypeEnum) {

		String identificativoDocumento = null;
		AttivitaClinicaEnum tipoAttivita = null;
		if (publicationReq != null) {
			if (publicationReq.getIdentificativoDoc() != null) {
				identificativoDocumento = publicationReq.getIdentificativoDoc();
			}
			if (publicationReq.getTipoAttivitaClinica() != null) {
				tipoAttivita = publicationReq.getTipoAttivitaClinica();
			}
		}
		return getStatusMessage(traceId, workflowInstanceId, eventTypeEnum, eventStatus, message, identificativoDocumento, jwtClaimDTO, tipoAttivita);
	}

	 
	@Override
	public void sendDeleteStatus(String traceId, String workflowInstanceId, String idDoc, String message,
			EventStatusEnum eventStatus, JWTPayloadDTO jwt,
			EventTypeEnum eventType) {
		ProducerRecord<String, String> status = getStatusMessage(traceId, workflowInstanceId, eventType, eventStatus, message, idDoc, jwt,
				AttivitaClinicaEnum.PHR);
		kafkaSendNonInTransaction(status);
	}

	@Override
	public void sendDeleteRequest(String workflowInstanceId, Object request) {
		ProducerRecord<String, String> status = getIndexerRetryMessage(workflowInstanceId, sendObjectAsJson(request), kafkaTopicCFG.getDispatcherIndexerRetryDeleteTopic());
		kafkaSendNonInTransaction(status);
	}

	@Override
	public void sendUpdateRequest(String workflowInstanceId, Object request) {
		ProducerRecord<String, String> status = getIndexerRetryMessage(workflowInstanceId, sendObjectAsJson(request), kafkaTopicCFG.getDispatcherIndexerRetryUpdateTopic());
		kafkaSendNonInTransaction(status);
	}

	@Override
	public void sendUpdateStatus(String traceId, String workflowInstanceId, String idDoc, EventStatusEnum eventStatus,
			JWTPayloadDTO jwt,
			String message, EventTypeEnum event) {
		getStatusMessage(traceId, workflowInstanceId, event, eventStatus, message, idDoc, jwt, null);
	}

	private String sendObjectAsJson(Object o) {
		String json;
		// Try to deserialize message
		try {
			json = new ObjectMapper().writeValueAsString(o);
		} catch (JsonProcessingException e) {
			json = "Unable to deserialize content request";
		}
		return json;
	}

	private ProducerRecord<String, String> getIndexerRetryMessage(final String workflowInstanceId, final String json, final String topic) {
		return getProducerRecord(topic, workflowInstanceId, json);
	}

	private ProducerRecord<String, String> getStatusMessage(final String traceId, final String workflowInstanceId, final EventTypeEnum eventType,
			final EventStatusEnum eventStatus, final String message, final String documentId,
			final JWTPayloadDTO jwtClaimDTO, AttivitaClinicaEnum tipoAttivita) {
		KafkaStatusManagerDTO statusManagerMessage = KafkaStatusManagerDTO.builder()
				.issuer(jwtClaimDTO != null ? jwtClaimDTO.getIss() : Constants.App.JWT_MISSING_ISSUER_PLACEHOLDER)
				.traceId(traceId).eventType(eventType).eventDate(new Date()).eventStatus(eventStatus)
				.message(message).identificativoDocumento(documentId).tipoAttivita(tipoAttivita)
				.subject(jwtClaimDTO != null ? jwtClaimDTO.getSub() : null)
				.organizzazione(jwtClaimDTO != null ? jwtClaimDTO.getSubject_organization_id() : null)
				.microserviceName(msName).build();

		String json = truncateMessageIfNecessary(statusManagerMessage);
		return getProducerRecord(kafkaTopicCFG.getStatusManagerTopic(), workflowInstanceId, json);

	}

	/**
	 * tronca il campo message di KafkaStatusManagerDTO se risulta maggiore di 1 MB
	 * (max
	 * kafka producer request size)
	 * 
	 * @param dto
	 * @return
	 */
	private String truncateMessageIfNecessary(KafkaStatusManagerDTO dto) {
		String json = StringUtility.toJSONJackson(dto);
		int maxProducerSize = kafkaProducerCFG.getMaxRequestSize();
		if (json.length() >= maxProducerSize) {
			int newTruncatedSize = maxProducerSize / 1024;
			String truncatedMessage = dto.getMessage().substring(0, newTruncatedSize);
			dto.setMessage(truncatedMessage);
			json = StringUtility.toJSONJackson(dto);
		}
		return json;
	}
	


	private ProducerRecord<String, String> getProducerRecord(String topic, String key, String value) {
		return new ProducerRecord<>(topic, key, value);
	}


}

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.KafkaProducerPropertiesCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.KafkaTopicCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.KafkaStatusManagerDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreateReplaceMetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IKafkaSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.PriorityUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Date;


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

	@Override
	public RecordMetadata sendMessage(String topic, String key, String value, boolean trans) {
		RecordMetadata out = null;
		ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);
		try {
			out = kafkaSend(producerRecord,trans);
		} catch (Exception e) {
			log.error("Send failed.", e);
			throw new BusinessException(e);
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private RecordMetadata kafkaSend(ProducerRecord<String, String> producerRecord, boolean trans) {
		RecordMetadata out = null;
		Object result = null;

		if (trans) {
			result = txKafkaTemplate.executeInTransaction(t -> {
				try {
					return t.send(producerRecord).get();
				} catch (InterruptedException e) {
					log.error("InterruptedException caught. Interrupting thread...");
					Thread.currentThread().interrupt();
					throw new BusinessException(e);
				} catch (Exception e) {
					throw new BusinessException(e);
				}
			});
		} else {
			notxKafkaTemplate.send(producerRecord);
		}

		if (result != null) {
			SendResult<String, String> sendResult = (SendResult<String, String>) result;
			out = sendResult.getRecordMetadata();
			log.debug("Message sent successfully");
		}
		return out;	
	}

	
	@Override
	public void notifyChannel(final String key,final String kafkaValue,PriorityTypeEnum priorityFromRequest,TipoDocAltoLivEnum documentType,DestinationTypeEnum destinationType) {
		log.debug("Destination: {}", destinationType.name());
		try {
			String destTopic = priorityUtility.computeTopic(priorityFromRequest, destinationType, documentType);
			
			if(StringUtility.isNullOrEmpty(kafkaProducerCFG.getTransactionalId())) {
				log.info("PRODUCER NON TRANSAZIONALE");
				sendMessage(destTopic, key, kafkaValue,false);
			} else {
				log.info("PRODUCER TRANSAZIONALE");
				sendMessage(destTopic, key, kafkaValue,true);
			}
		} catch (Exception e) {
			log.error("Error sending kafka message", e);
			throw new BusinessException(e);
		}
	}

	@Override
	public void sendValidationStatus(final String traceId,final String workflowInstanceId, final EventStatusEnum eventStatus, final String message,
									 final JWTPayloadDTO jwtClaimDTO) {
		sendStatusMessage(traceId,workflowInstanceId, EventTypeEnum.VALIDATION, eventStatus, message, null, jwtClaimDTO, null);
	}
	
	@Override
	public void sendValidationStatus(final String traceId,final String workflowInstanceId, final EventStatusEnum eventStatus, final String message,
									 final JWTPayloadDTO jwtClaimDTO, EventTypeEnum eventTypeEnum) {
		sendStatusMessage(traceId,workflowInstanceId, eventTypeEnum, eventStatus, message, null, jwtClaimDTO, null);
	}

	@Override
	public void sendPublicationStatus(final String traceId,final String workflowInstanceId, final EventStatusEnum eventStatus, final String message,
									  final PublicationCreateReplaceMetadataDTO publicationReq, final JWTPayloadDTO jwtClaimDTO) {

		String identificativoDocumento = null;
		AttivitaClinicaEnum tipoAttivita = null;
		if (publicationReq != null)  {
			if (publicationReq.getIdentificativoDoc()!=null) {
				identificativoDocumento = publicationReq.getIdentificativoDoc();
			}
			if(publicationReq.getTipoAttivitaClinica()!=null) {
				tipoAttivita = publicationReq.getTipoAttivitaClinica();
			}
		}
		sendStatusMessage(traceId,workflowInstanceId, EventTypeEnum.PUBLICATION, eventStatus, message, identificativoDocumento, jwtClaimDTO,tipoAttivita);
	}
	
	@Override
	public void sendReplaceStatus(final String traceId, final String workflowInstanceId, final EventStatusEnum eventStatus, final String message,
								  final PublicationCreateReplaceMetadataDTO publicationReq, final JWTPayloadDTO jwtClaimDTO) {

		String identificativoDocumento = null;
		AttivitaClinicaEnum tipoAttivita = null;
		if (publicationReq != null)  {
			if (publicationReq.getIdentificativoDoc()!=null) {
				identificativoDocumento = publicationReq.getIdentificativoDoc();
			}
			if(publicationReq.getTipoAttivitaClinica()!=null) {
				tipoAttivita = publicationReq.getTipoAttivitaClinica();
			}
		}
		sendStatusMessage(traceId,workflowInstanceId, EventTypeEnum.REPLACE, eventStatus, message, identificativoDocumento, jwtClaimDTO,tipoAttivita);
	}

	@Override
	public void sendDeleteStatus(String traceId, String workflowInstanceId, String idDoc, String message, EventStatusEnum eventStatus, JWTPayloadDTO jwt,
			EventTypeEnum eventType) {
		sendStatusMessage(traceId, workflowInstanceId, eventType, eventStatus, message, idDoc, jwt, AttivitaClinicaEnum.PHR);
	}

	@Override
	public void sendDeleteRequest(String workflowInstanceId, Object request) {
		sendIndexerRetryMessage(workflowInstanceId, sendObjectAsJson(request),kafkaTopicCFG.getDispatcherIndexerRetryDeleteTopic());
	}
	
	@Override
	public void sendUpdateRequest(String workflowInstanceId, Object request) {
		sendIndexerRetryMessage(workflowInstanceId, sendObjectAsJson(request),kafkaTopicCFG.getDispatcherIndexerRetryUpdateTopic());
	}
	
	@Override
	public void sendUpdateStatus(String traceId, String workflowInstanceId, String idDoc, EventStatusEnum eventStatus, JWTPayloadDTO jwt,
			String message,EventTypeEnum event) {
		sendStatusMessage(traceId, workflowInstanceId,event , eventStatus, message, idDoc, jwt,null);
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

	private void sendIndexerRetryMessage(final String workflowInstanceId, final String json,
			final String topic) {
		
		if(StringUtility.isNullOrEmpty(kafkaProducerCFG.getTransactionalId())) {
			log.info("PRODUCER NON TRANSAZIONALE");
			sendMessage(topic, workflowInstanceId, json, false);
		} else {
			log.info("PRODUCER TRANSAZIONALE");
			sendMessage(topic, workflowInstanceId, json, true);
		}
	}

	private void sendStatusMessage(final String traceId,final String workflowInstanceId, final EventTypeEnum eventType,
								   final EventStatusEnum eventStatus, final String message, final String documentId,
								   final JWTPayloadDTO jwtClaimDTO, AttivitaClinicaEnum tipoAttivita) {
		try { 
			KafkaStatusManagerDTO statusManagerMessage = KafkaStatusManagerDTO.builder().
					issuer(jwtClaimDTO != null ? jwtClaimDTO.getIss() : Constants.App.JWT_MISSING_ISSUER_PLACEHOLDER).
					traceId(traceId).
					eventType(eventType).
					eventDate(new Date()).
					eventStatus(eventStatus).
					message(message).
					identificativoDocumento(documentId).
					tipoAttivita(tipoAttivita).
					subject(jwtClaimDTO != null ? jwtClaimDTO.getSub() : null).
					organizzazione(jwtClaimDTO != null ? jwtClaimDTO.getSubject_organization_id() : null).
					microserviceName(msName).
					build();
			
			String json = truncateMessageIfNecessary(statusManagerMessage);

			if(StringUtility.isNullOrEmpty(kafkaProducerCFG.getTransactionalId())) {
				log.info("PRODUCER NON TRANSAZIONALE");
				sendMessage(kafkaTopicCFG.getStatusManagerTopic(), workflowInstanceId, json, false);
			} else {
				log.info("PRODUCER TRANSAZIONALE");
				sendMessage(kafkaTopicCFG.getStatusManagerTopic(), workflowInstanceId, json, true);
			}
		} catch(Exception ex) {
			log.error("Error while send status message : " , ex);
			throw new BusinessException(ex);
		}
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
		String json = StringUtility.toJSON(dto);
		int maxProducerSize = kafkaProducerCFG.getMaxRequestSize();
		if (json.length() >= maxProducerSize) {
			int newTruncatedSize = maxProducerSize / 1024;
			String truncatedMessage = dto.getMessage().substring(0, newTruncatedSize);
			dto.setMessage(truncatedMessage);
			json = StringUtility.toJSON(dto);
		}
		return json;
	}
 
}

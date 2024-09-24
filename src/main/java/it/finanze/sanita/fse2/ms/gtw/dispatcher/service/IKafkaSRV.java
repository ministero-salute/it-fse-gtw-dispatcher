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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.service;

import org.apache.kafka.clients.producer.ProducerRecord;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreateReplaceMetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.PriorityTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.TipoDocAltoLivEnum;

/**
 * Interface for service used to handle kafka communications
 */
public interface IKafkaSRV {

	void kafkaSendNonInTransaction(ProducerRecord<String, String> producerRecord);
	void sendValidationStatus(String traceId, String workflowInstanceId, EventStatusEnum eventStatus, String message,JWTPayloadDTO jwtClaimDTO); 
	void sendValidationStatus(String traceId,String workflowInstanceId, EventStatusEnum eventStatus, String message,
			 JWTPayloadDTO jwtClaimDTO, EventTypeEnum eventTypeEnum);
	
	ProducerRecord<String, String> getStatus(String traceId, String workflowInstanceId,
			EventStatusEnum eventStatus, String message,
			PublicationCreateReplaceMetadataDTO publicationReq, JWTPayloadDTO jwtClaimDTO,
			EventTypeEnum eventTypeEnum);
	
	void sendDeleteStatus(String traceId, String workflowInstanceId, String idDoc, String message, EventStatusEnum eventStatus, JWTPayloadDTO jwt, EventTypeEnum eventType);
	void sendDeleteRequest(String workflowInstanceId, Object request);
	
	void sendUpdateStatus(String traceId, String workflowInstanceId, String idDoc, EventStatusEnum eventStatus, JWTPayloadDTO jwt, String message,EventTypeEnum event);

	void sendUpdateRequest(String workflowInstanceId, Object request);
	
	void notifyIndexerAndStatusInTransaction(final String key, final String kafkaValue, PriorityTypeEnum priorityFromRequest,
			TipoDocAltoLivEnum documentType, 
			String traceId, String workflowInstanceId,
			EventStatusEnum eventStatus, String message,
			PublicationCreateReplaceMetadataDTO publicationReq, JWTPayloadDTO jwtClaimDTO,
			EventTypeEnum eventTypeEnum);

}

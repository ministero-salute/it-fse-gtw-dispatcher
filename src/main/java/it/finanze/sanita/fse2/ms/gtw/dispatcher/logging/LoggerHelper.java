
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IConfigClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.KafkaProducerPropertiesCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.LogDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ILogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ResultLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CfUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LoggerHelper {

	Logger kafkaLog = LoggerFactory.getLogger("kafka-logger"); 

	@Autowired
	private IConfigClient configClient;
    
    @Autowired
    private IConfigSRV configSRV;
	
	private String gatewayName;

	@Value("${log.kafka-log.enable}")
	private boolean kafkaLogEnable;

	@Value("${spring.application.name}")
	private String msName;
	
	@Autowired
	private KafkaProducerPropertiesCFG kafkaProducerCFG;

	/* 
	 * Specify here the format for the dates 
	 */
	private DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS"); 


	/* 
	 * Implements structured logs, at all logging levels
	 */
	public void trace(String logType,String workflowInstanceId, String message, ILogEnum operation, ResultLogEnum result, Date startDateOperation, JWTPayloadDTO jwtPayloadDTO) {
		
		if(configSRV.isControlLogPersistenceEnable()) {
			if(jwtPayloadDTO==null) {
				jwtPayloadDTO = new JWTPayloadDTO(); 
			}
			LogDTO logDTO = LogDTO.builder().
					op_locality(jwtPayloadDTO.getLocality()).
					message(message).
					operation(operation.getCode()).
					op_result(result.getCode()).
					op_timestamp_start(dateFormat.format(startDateOperation)).
					op_timestamp_end(dateFormat.format(new Date())).
					op_role(jwtPayloadDTO.getSubject_role()).
					gateway_name(getGatewayName()).
					microservice_name(msName).
					op_application_id(jwtPayloadDTO.getSubject_application_id()).
					op_application_vendor(jwtPayloadDTO.getSubject_application_vendor()).
					op_application_version(jwtPayloadDTO.getSubject_application_version()).
					log_type(logType).
					workflow_instance_id(workflowInstanceId).
					build();

			if(!configSRV.isSubjectNotAllowed()) {
				logDTO.setOp_fiscal_code(CfUtility.extractFiscalCodeFromJwtSub(jwtPayloadDTO.getSub()));
			}
			
			if(!configSRV.isCfOnIssuerNotAllowed()) {
				logDTO.setOp_issuer(jwtPayloadDTO.getIss());
			}
			
			final String logMessage = StringUtility.toJSON(logDTO);
			log.trace(logMessage);

			if (Boolean.TRUE.equals(kafkaLogEnable)) {
				kafkaLog.trace(truncateLogDtoMessageIfNecessary(logDTO));
			}
		}
		
		
	}

	public void debug(String logType,String workflowInstanceId, String message,  ILogEnum operation, ResultLogEnum result, Date startDateOperation,JWTPayloadDTO jwtPayloadDTO) {
		
		if(configSRV.isControlLogPersistenceEnable()) {
			if(jwtPayloadDTO==null) {
				jwtPayloadDTO = new JWTPayloadDTO(); 
			}
			LogDTO logDTO = LogDTO.builder().
					op_locality(jwtPayloadDTO.getLocality()).
					message(message).
					operation(operation.getCode()).
					op_result(result.getCode()).
					op_timestamp_start(dateFormat.format(startDateOperation)).
					op_timestamp_end(dateFormat.format(new Date())).
					op_role(jwtPayloadDTO.getSubject_role()).
					gateway_name(getGatewayName()).
					microservice_name(msName).
					op_application_id(jwtPayloadDTO.getSubject_application_id()).
					op_application_vendor(jwtPayloadDTO.getSubject_application_vendor()).
					op_application_version(jwtPayloadDTO.getSubject_application_version()).
					log_type(logType).
					workflow_instance_id(workflowInstanceId).
					build();
			
			if(!configSRV.isSubjectNotAllowed()) {
				logDTO.setOp_fiscal_code(CfUtility.extractFiscalCodeFromJwtSub(jwtPayloadDTO.getSub()));
			}
			
			if(!configSRV.isCfOnIssuerNotAllowed()) {
				logDTO.setOp_issuer(jwtPayloadDTO.getIss());
			}
			
			final String logMessage = StringUtility.toJSON(logDTO);
			log.debug(logMessage);
			if (Boolean.TRUE.equals(kafkaLogEnable)) {
				kafkaLog.debug(truncateLogDtoMessageIfNecessary(logDTO));
			}
		}
		
	} 

	public void info(String logType,String workflowInstanceId, String message, ILogEnum operation, ResultLogEnum result, Date startDateOperation,  
			String documentType, JWTPayloadDTO jwtPayloadDTO, String typeIdExtension) {
		
		if(configSRV.isControlLogPersistenceEnable()) {
			if(jwtPayloadDTO==null) {
				jwtPayloadDTO = new JWTPayloadDTO(); 
			}
			LogDTO logDTO = LogDTO.builder().
					op_locality(jwtPayloadDTO.getLocality()).
					message(message).
					operation(operation.getCode()).
					op_result(result.getCode()).
					op_timestamp_start(dateFormat.format(startDateOperation)).
					op_timestamp_end(dateFormat.format(new Date())).
					op_document_type(documentType).
					op_role(jwtPayloadDTO.getSubject_role()).
					gateway_name(getGatewayName()).
					microservice_name(msName).
					op_application_id(jwtPayloadDTO.getSubject_application_id()).
					op_application_vendor(jwtPayloadDTO.getSubject_application_vendor()).
					op_application_version(jwtPayloadDTO.getSubject_application_version()).
					log_type(logType).
					workflow_instance_id(workflowInstanceId).
					typeIdExtension(typeIdExtension).
					build();
			
			if(!configSRV.isSubjectNotAllowed()) {
				logDTO.setOp_fiscal_code(CfUtility.extractFiscalCodeFromJwtSub(jwtPayloadDTO.getSub()));
			}
			
			if(!configSRV.isCfOnIssuerNotAllowed()) {
				logDTO.setOp_issuer(jwtPayloadDTO.getIss());
			}
			
			final String logMessage = StringUtility.toJSON(logDTO);
			log.info(logMessage);
			
			if (Boolean.TRUE.equals(kafkaLogEnable)) {
				kafkaLog.info(truncateLogDtoMessageIfNecessary(logDTO));
			}
		}
		
	} 

	public void warn(String logType,String workflowInstanceId, String message, ILogEnum operation, ResultLogEnum result, Date startDateOperation,JWTPayloadDTO jwtPayloadToken) {
		if(configSRV.isControlLogPersistenceEnable()) {
			if(jwtPayloadToken==null) {
				jwtPayloadToken = new JWTPayloadDTO(); 
			}
			LogDTO logDTO = LogDTO.builder().
					op_locality(jwtPayloadToken.getLocality()).
					message(message).
					operation(operation.getCode()).
					op_result(result.getCode()).
					op_timestamp_start(dateFormat.format(startDateOperation)).
					op_timestamp_end(dateFormat.format(new Date())).
					op_role(jwtPayloadToken.getSubject_role()).
					gateway_name(getGatewayName()).
					microservice_name(msName).
					op_application_id(jwtPayloadToken.getSubject_application_id()).
					op_application_vendor(jwtPayloadToken.getSubject_application_vendor()).
					op_application_version(jwtPayloadToken.getSubject_application_version()).
					log_type(logType).
					workflow_instance_id(workflowInstanceId).
					build();
			
			if(!configSRV.isSubjectNotAllowed()) {
				logDTO.setOp_fiscal_code(CfUtility.extractFiscalCodeFromJwtSub(jwtPayloadToken.getSub()));
			}
			
			if(!configSRV.isCfOnIssuerNotAllowed()) {
				logDTO.setOp_issuer(jwtPayloadToken.getIss());
			}
			final String logMessage = StringUtility.toJSON(logDTO);
			log.warn(logMessage);
			if (Boolean.TRUE.equals(kafkaLogEnable)) {
				kafkaLog.warn(truncateLogDtoMessageIfNecessary(logDTO));
			}
		}
		
 
	} 

	public void error(String logType,String workflowInstanceId, String message, ILogEnum operation, ResultLogEnum result, Date startDateOperation,
			   ILogEnum error,  String documentType, JWTPayloadDTO jwtPayloadToken) {
		if(configSRV.isControlLogPersistenceEnable()) {
			if(jwtPayloadToken==null) {
				jwtPayloadToken = new JWTPayloadDTO(); 
			}
			LogDTO logDTO = LogDTO.builder().
					op_locality(jwtPayloadToken.getLocality()).
					message(message).
					operation(operation.getCode()).
					op_result(result.getCode()).
					op_timestamp_start(dateFormat.format(startDateOperation)).
					op_timestamp_end(dateFormat.format(new Date())).
					op_error(error.getCode()).
					op_error_description(error.getDescription()).
					op_document_type(documentType).
					op_role(jwtPayloadToken.getSubject_role()).
					gateway_name(getGatewayName()).
					microservice_name(msName).
					op_application_id(jwtPayloadToken.getSubject_application_id()).
					op_application_vendor(jwtPayloadToken.getSubject_application_vendor()).
					op_application_version(jwtPayloadToken.getSubject_application_version()).
					log_type(logType).
					workflow_instance_id(workflowInstanceId).
					build();
			
			if(!configSRV.isSubjectNotAllowed()) {
				logDTO.setOp_fiscal_code(CfUtility.extractFiscalCodeFromJwtSub(jwtPayloadToken.getSub()));
			}
			if(!configSRV.isCfOnIssuerNotAllowed()) {
				logDTO.setOp_issuer(jwtPayloadToken.getIss());
			}
			final String logMessage = StringUtility.toJSON(logDTO);
			log.error(logMessage);
			if (Boolean.TRUE.equals(kafkaLogEnable)) {
				kafkaLog.error(truncateLogDtoMessageIfNecessary(logDTO));
			}
		}
		
		
	}

	/**
	 * Returns the gateway name.
	 * 
	 * @return The GatewayName of the ecosystem.
	 */
	private String getGatewayName() {
		if (gatewayName == null) {
			gatewayName = configClient.getGatewayName();
		}
		return gatewayName;
	}

	/**
	 * tronca il campo message di logDTO se logDTO risulta maggiore di 1 MB (max
	 * kafka producer request size)
	 * 
	 * @param logDTO
	 * @return
	 */
	private String truncateLogDtoMessageIfNecessary(LogDTO logDTO) {

		String logMessage = StringUtility.toJSON(logDTO);
		int maxProducerSize = kafkaProducerCFG.getMaxRequestSize();
		if (logMessage.length() >= maxProducerSize) {
			int newTruncatedSize = maxProducerSize / 1024;
			String truncatedMessage = logDTO.getMessage().substring(0, newTruncatedSize);
			logDTO.setMessage(truncatedMessage);
			logMessage = StringUtility.toJSON(logDTO);
		}
		return logMessage;
	}

}

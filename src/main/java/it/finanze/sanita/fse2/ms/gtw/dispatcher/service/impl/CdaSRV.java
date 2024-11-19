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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationDataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.ValidatedDocumentsETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IValidatedDocumentsRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.ICdaSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

/**
 *	Cda service implementation.
 */
@Service
@Slf4j
public class CdaSRV implements ICdaSRV {

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = -1000397559663801763L;

	@Autowired 
	private IValidatedDocumentsRepo cdaRepo;
	
	
	@Override
	public void create(final String hashedCDA, final String wii, String transfID, String engineID) {
		try {
			cdaRepo.create(ValidatedDocumentsETY.setContent(hashedCDA, wii, transfID, engineID));
		} catch(Exception ex) {
			log.error("Error creating cda :" ,ex);
			throw new BusinessException("Error creating cda :" ,ex);
		}
	}
	
	@Override  
	public String get(final String hash) {
		try {
			return cdaRepo.findItemByHash(hash).getWorkflowInstanceId(); // tornava un validation DTO 
		} catch(Exception ex) {
			log.error("Error getting entity from Mongo", ex);
			throw new BusinessException("Error getting entity from Mongo", ex);
		}
	}

	@Override 
	public ValidationDataDTO getByWorkflowInstanceId(final String wid) {
		try {
			return cdaRepo.findItemByWorkflowInstanceId(wid); // tornava un validation DTO 
		} catch(Exception ex) {
			log.error("Error getting entity from Mongo", ex);
			throw new BusinessException("Error getting entity from Mongo", ex);
		}
	}
	
	@Override
	public ValidationDataDTO retrieveValidationInfo(final String hashPublication, final String wiiPublication) {
		ValidationDataDTO data = new ValidationDataDTO();

		try { 
			data = cdaRepo.findItemByHash(hashPublication);
			String wii = data.getWorkflowInstanceId();

			if (wii == null) {
				log.debug("Hash of CDA not found in Mongo, the CDA may be not validated");
			} else { 
				if (!StringUtility.isNullOrEmpty(wiiPublication) && !wiiPublication.equals(data.getWorkflowInstanceId())) {
					data.setCdaValidated(false);
				}
			}
		} catch (Exception e) {
			log.error(String.format("Error while retrieving item with transaction ID: %s from MongoDB.", wiiPublication), e);
			throw new BusinessException(String.format("Error while retrieving item with transaction ID: %s from MongoDB.", wiiPublication), e);
		}

		return data;
	}
	
	
	@Override
	public boolean consumeHash(final String hashToConsume) {
		boolean consumed = false;
		try {
			if(!StringUtility.isNullOrEmpty(hashToConsume)) {
				consumed = cdaRepo.deleteItem(hashToConsume);
			}
		} catch (Exception e) {
			log.error("Error while consuming hash from MongoDB", e);
			throw new BusinessException("Error while consuming hash from MongoDB", e);
		}
		return consumed;
	}

	 
	@Override
	public void createBenchMark(final String hashedCDA, final String wii, String transfID, String engineID) {
		try {
			cdaRepo.createBenchmark(ValidatedDocumentsETY.setContent(hashedCDA, wii, transfID, engineID));
		} catch(Exception ex) {
			log.error("Error creating cda :" ,ex);
			throw new BusinessException("Error creating cda :" ,ex);
		}
	}

	@Override
	public boolean consumeHashBenchmark(final String hashToConsume) {
		boolean consumed = false;
		try {
			if(!StringUtility.isNullOrEmpty(hashToConsume)) {
				consumed = cdaRepo.deleteItemBenchmark(hashToConsume);
			}
		} catch (Exception e) {
			log.error("Error while consuming hash from MongoDB", e);
			throw new BusinessException("Error while consuming hash from MongoDB", e);
		}
		return consumed;
	}

}

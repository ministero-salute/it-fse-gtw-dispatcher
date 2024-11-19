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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.impl;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.mongodb.MongoException;
import com.mongodb.client.result.DeleteResult;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationDataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.ValidatedDocumentsETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IValidatedDocumentsRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.DateUtility;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class ValidatedDocumentsRepo implements IValidatedDocumentsRepo {

	@Autowired
	private transient MongoTemplate mongoTemplate;


	@Override
	public void create(final ValidatedDocumentsETY ety) {
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("hash_cda").is(ety.getHashCda()));

			ValidatedDocumentsETY etyFinded = mongoTemplate.findOne(query, ValidatedDocumentsETY.class);
			if (etyFinded != null) {
				etyFinded.setPrimaryKeyTransform(ety.getPrimaryKeyTransform());
				etyFinded.setWorkflowInstanceId(ety.getWorkflowInstanceId());
				etyFinded.setInsertionDate(new Date());
				mongoTemplate.save(etyFinded);
			} else {
				ety.setInsertionDate(new Date());
				mongoTemplate.save(ety);
			}
		} catch (Exception ex) {
			log.error("Error while insert validated document : ", ex);
			throw new BusinessException("Error while insert validated document : ", ex);
		}
	}

	@Override
	public boolean isItemInserted(final String hash) {
		boolean response;
		Query query = new Query();
		query.addCriteria(where("hash_cda").is(hash));
		try {
			response = mongoTemplate.exists(query, ValidatedDocumentsETY.class);
		} catch (MongoException e) {
			log.error("Unable to verify if validate document is inserted", e);
			throw new BusinessException("Unable to verify if validate document is inserted", e);
		}
		return response;
	}

	@Override
	public boolean deleteItem(final String hash) {
		DeleteResult output;
		Query query = new Query();
		query.addCriteria(where("hash_cda").is(hash));
		try {
			output = mongoTemplate.remove(query, ValidatedDocumentsETY.class);
		} catch (MongoException e) {
			log.error("Unable to delete the item", e);
			throw new BusinessException("Unable to delete the item", e);
		}
		return output.wasAcknowledged();
	}

	@Override
	public ValidatedDocumentsETY findItemById(final String id) {
		ValidatedDocumentsETY output;
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").is(id));
		try {
			output = mongoTemplate.findOne(query, ValidatedDocumentsETY.class);
		} catch (MongoException e) {
			log.error("Unable to retrieve validated doc by id", e);
			throw new BusinessException("Unable to retrieve validated doc by id", e);
		}
		return output;
	}

	@Override
	public ValidationDataDTO findItemByWorkflowInstanceId(final String wid) {
		ValidationDataDTO output;
		ValidatedDocumentsETY ety;
		Query query = new Query();
		query.addCriteria(Criteria.where("w_id").is(wid));
		try {
			ety = mongoTemplate.findOne(query, ValidatedDocumentsETY.class);
			output = parseEtyToDTo(ety);
		} catch (MongoException e) {
			log.error("Unable to retrieve validated doc by hash", e);
			throw new BusinessException("Unable to retrieve validated doc by hash", e);
		}

		return output;
	}

	ValidationDataDTO parseEtyToDTo(ValidatedDocumentsETY ety) {
		// se output null oggetto con validatedFalse
		ValidationDataDTO dto = new ValidationDataDTO();
		if (ety == null) {
			dto.setCdaValidated(false);
		} else {
			dto.setHash(ety.getHashCda());
			dto.setCdaValidated(true);
			dto.setWorkflowInstanceId(ety.getWorkflowInstanceId());
			dto.setTransformID(ety.getPrimaryKeyTransform());
			dto.setEngineID(ety.getPrimaryKeyEngine());
			dto.setInsertionDate(ety.getInsertionDate());
		}
		return dto;
	}

	@Override
	public ValidationDataDTO findItemByHash(final String hash) {
		ValidationDataDTO output;
		ValidatedDocumentsETY ety;
		Query query = new Query();
		query.addCriteria(Criteria.where("hash_cda").is(hash));
		try {
			ety = mongoTemplate.findOne(query, ValidatedDocumentsETY.class);
			output = parseEtyToDTo(ety);
		} catch (MongoException e) {
			log.error("Unable to retrieve validated doc by hash", e);
			throw new BusinessException("Unable to retrieve validated doc by hash", e);
		}

		return output;
	}

	@Override
	public String updateInsertionDate(final String workflowInstanceId, final int days) {
		String objectId = "";
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("w_id").is(workflowInstanceId));

			ValidatedDocumentsETY ety = mongoTemplate.findOne(query, ValidatedDocumentsETY.class);

			if (ety != null) {
				Date newDate = DateUtility.addDay(ety.getInsertionDate(), -days);
				ety.setInsertionDate(newDate);
				ety = mongoTemplate.save(ety);
				objectId = ety.getId();
			}
		} catch (Exception ex) {
			log.error("Error while update validated document : ", ex);
			throw new BusinessException("Error while update validated document : ", ex);
		}
		return objectId;
	}

	@Override
	public void createBenchmark(ValidatedDocumentsETY ety) {
		ety.setInsertionDate(new Date());
		mongoTemplate.save(ety);
	}

	@Override
	public boolean deleteItemBenchmark(String hash) {
		DeleteResult output;
		Query query = new Query();
		query.addCriteria(where("hash_cda").is(hash));
		try {
			ValidatedDocumentsETY ety = mongoTemplate.findOne(query, ValidatedDocumentsETY.class);
			output = mongoTemplate.remove(ety);
		} catch (MongoException e) {
			log.error("Unable to delete the item", e);
			throw new BusinessException("Unable to delete the item", e);
		}
		return output.wasAcknowledged();
	}
}

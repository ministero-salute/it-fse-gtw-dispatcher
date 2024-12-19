
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.service.facade;

import java.io.Serializable;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationDataDTO;

/**
 *   Cda facade interface service.
 */
public interface ICdaFacadeSRV extends Serializable {

	/**
	 * Inserts a new item in the repository.
	 * 
	 * @param hashedCDA The value to be inserted.
	 * @param wii      The key of the item represented by the transaction Id.
	 * @param objectID The primary key of the item
	 */
	void create(String hashedCDA, String wii, String transformID, String engineID);

	/**
	 * Returns the value of the key: {@code hash}.
	 * 
	 * @param hash The key to search.
	 * @return The value of the key: {@code hash}.
	 */
	String get(String hash);

	ValidationDataDTO getByWorkflowInstanceId(String wid);

	/**
	 * Check presence of the hash on Mongo for the transaction Id.
	 * 
	 * @param hashToValidate The hash to validate.
	 * @param txID The key to use to search hash.
	 * @return The workflowInstanceId saved in validation time or {@code null} if never validated.
	 */
	ValidationDataDTO retrieveValidationInfo(String hashToValidate, String txID);

	/**
	 * Deletes a record on Mongo identified by its {@code hashToConsume}.
	 * 
	 * @param hashToConsume The record key to delete.
	 * @return {@code true} if deleted, {@code false} otherwise.
	 */
	boolean consumeHash(String hashToConsume);

	void createBenchMark(final String hashedCDA, final String wii, String transfID, String engineID);

	boolean consumeHashBenchmark(String hashToConsume);
}

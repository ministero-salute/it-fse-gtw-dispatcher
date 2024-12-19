
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model to save validated documents info.
 */
@Document(collection = "#{@validatedDocumentsBean}")
@Data
@NoArgsConstructor
public class ValidatedDocumentsETY {

	@Id
	private String id; 
	
	@Field(name = "hash_cda")
	private String hashCda; 
	
	@Field(name = "w_id")
	private String workflowInstanceId; 
	
	@Field(name = "pkey_transform")
	private String primaryKeyTransform;

	@Field(name = "pkey_engine")
	private String primaryKeyEngine;
	
	@Field(name = "insertion_date")
	private Date insertionDate;

	public static ValidatedDocumentsETY setContent(String hash, String wii, String transformID, String engineID) {
        ValidatedDocumentsETY entity = new ValidatedDocumentsETY();
        entity.setHashCda(hash);
		entity.setWorkflowInstanceId(wii);
		entity.setPrimaryKeyTransform(transformID);
		entity.setPrimaryKeyEngine(engineID);
        return entity;
    }
}



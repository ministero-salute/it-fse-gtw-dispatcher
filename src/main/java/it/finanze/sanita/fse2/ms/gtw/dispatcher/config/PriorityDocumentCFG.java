
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class PriorityDocumentCFG {

	/**
	 * List of low priority documents.
	 */
	@Value("${document-type.priority.low}")
	private String lowPriorityDocuments;
	
	/**
	 * List of medium priority documents.
	 */
	@Value("${document-type.priority.medium}")
	private String mediumPriorityDocuments;

	/**
	 * List of high priority documents.
	 */
	@Value("${document-type.priority.high}")
	private String highPriorityDocuments;
}

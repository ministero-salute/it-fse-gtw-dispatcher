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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationDataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.ValidatedDocumentsETY;

public interface IValidatedDocumentsRepo {
	
	void create(final ValidatedDocumentsETY ety);

	boolean deleteItem(String hash);

	ValidatedDocumentsETY findItemById(String id);

	ValidationDataDTO findItemByHash(String hash); // tornava la chiave (string)

	ValidationDataDTO findItemByWorkflowInstanceId(String wid); // tornava la chiave (string)

	boolean isItemInserted(String hash);

	String updateInsertionDate(String workflowInstanceId, int days);

	void createBenchmark(final ValidatedDocumentsETY ety);

	boolean deleteItemBenchmark(String hash);


}

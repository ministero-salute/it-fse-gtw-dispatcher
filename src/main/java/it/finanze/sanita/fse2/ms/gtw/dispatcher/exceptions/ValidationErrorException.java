
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import lombok.Getter;

/**
 * Validation error exception.
 */
public class ValidationErrorException extends RuntimeException {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = 1L;
	
	@Getter
	private final RestExecutionResultEnum result;

	@Getter
	private final String workflowInstanceId;

	@Getter
	private final String errorInstance;

	public ValidationErrorException(final RestExecutionResultEnum inResult, final String msg, final String inWorkflowInstanceId, final String inErrorInstance) {
		super(msg);
		workflowInstanceId = inWorkflowInstanceId;
		result = inResult;
		errorInstance = inErrorInstance;
	}

}

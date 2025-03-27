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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * The Class ValidationErrorResponseDTO.
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class ValidationErrorResponseDTO extends ErrorResponseDTO {

	@Schema(description = "Identificativo del workflow")
	@Size(min = 0, max = 256)
	private String workflowInstanceId;
	
	public ValidationErrorResponseDTO(final LogTraceInfoDTO traceInfo, final String inType, final String inTitle, final String inDetail, final Integer inStatus, final String inInstance, final String inWorkflowInstanceId) {
		super(traceInfo, inType, inTitle, inDetail, inStatus, inInstance);
		workflowInstanceId = inWorkflowInstanceId;
	}

}

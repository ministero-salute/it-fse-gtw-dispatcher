
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

import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import lombok.Getter;
import lombok.Setter;

/**
 *	DTO used to return validation result.
 */
@Getter
@Setter
public class ValidationResDTO extends ResponseDTO {

	@Size(min = 0, max = 256)
	@Schema(description = "Identificativo univoco della transazione")
	private String workflowInstanceId;
	
	@Size(min = 0, max = Constants.App.MAX_SIZE_WARNING)
	@Schema(description = "Dettaglio del warning")
	private String warning;

	public ValidationResDTO() {
		super();
	}

	public ValidationResDTO(final LogTraceInfoDTO traceInfo, final String inWorkflowInstanceId, final String inWarning) {
		super(traceInfo);
		workflowInstanceId = inWorkflowInstanceId;
		warning = inWarning;
	}
	
}

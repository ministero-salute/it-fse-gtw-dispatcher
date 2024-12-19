
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor 
public class PublicationResDTO extends ResponseDTO { 

	@Size(min = 0, max = 10000)
	@Schema(description = "Dettaglio del warning")
	private String warning;
		
	@Size(min = 0, max = 256)
	@Schema(description = "Identificativo del workflow instance id")
	private String workflowInstanceId;

	public PublicationResDTO(final LogTraceInfoDTO traceInfo, String inWarning,final String inWorkflowInstanceId) {
		super(traceInfo);
		warning = inWarning;
		workflowInstanceId = inWorkflowInstanceId;
	}
	
}

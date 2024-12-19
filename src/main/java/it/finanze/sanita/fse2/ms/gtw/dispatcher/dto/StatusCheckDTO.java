
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto;

import javax.validation.constraints.Size;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StatusCheckDTO {

	@Size(min = 0, max = 100)
	private String eventType;

	@Size(min = 0, max = 100)
	private String eventDate;  
	
	@Size(min = 0, max = 100)
	private String eventStatus;
	
	@Size(min = 0, max = 50000)
	private String message;

	@Size(min = 0, max = 100)
	private String identificativoDocumento;

	@Size(min = 0, max = 100)
	private String subject;

	@Size(min = 0, max = 100)
	private String subjectRole;
	
	@Size(min = 0, max = 100)
	private String tipoAttivita;
	
	@Size(min = 0, max = 100)
	private String organizzazione;
	
	@Size(min = 0, max = 256)
	private String workflowInstanceId;
	
	@Size(min = 0, max = 100)
	private String traceId;
	
	@Size(min = 0, max = 100)
	private String issuer;
	
	@Size(min = 0, max = 100)
	private String expiringDate;
	
	@Size(min = 0, max = 10000)
	private String extra;
	
}

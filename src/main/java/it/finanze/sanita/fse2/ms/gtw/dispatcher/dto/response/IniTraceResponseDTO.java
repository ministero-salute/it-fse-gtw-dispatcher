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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IniTraceResponseDTO extends ResponseDTO {

	private Boolean esito;

	private String message;

	public IniTraceResponseDTO() {
		super();
	}

	public IniTraceResponseDTO(final LogTraceInfoDTO traceInfo, final Boolean inEsito, final String inMessage) {
		super(traceInfo);
		esito = inEsito;
		message = inMessage;
	}

}

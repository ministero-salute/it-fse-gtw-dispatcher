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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The Class ErrorResponseDTO.
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ErrorResponseDTO {

	/**
	 * Trace id log.
	 */
	@Schema(description = "Indentificativo univoco della richiesta dell'utente")
	@Size(min = 0, max = 100)
	private String traceID;

	/**
	 * Span id log.
	 */
	@Schema(description = "Indentificativo univoco di un task della richiesta dell'utente (differisce dal traceID solo in caso di chiamate sincrone in cascata)")
	@Size(min = 0, max = 100)
	private String spanID;

	@Schema(description = "URI da utilizzare come identificativo del problema che si è verificato")
	@Size(min = 0, max = 100)
	private String type;

	@Schema(description = "Descrizione sintetica della tipologia d'errore")
	@Size(min = 0, max = 1000)
	private String title;

	@Schema(description = "Dettaglio della tipologia d'errore")
	@Size(min = 0, max = Integer.MAX_VALUE)
	private String detail;

	@Schema(description = "Stato http")
	@Min(value = 100)
	@Max(value = 599)
	private Integer status;

	@Schema(description = "URI che identifica la specifica occorrenza del problema")
	@Size(min = 0, max = 100)
	private String instance;

	public ErrorResponseDTO(final LogTraceInfoDTO traceInfo, final String inType, final String inTitle,
			final String inDetail, final Integer inStatus, final String inInstance) {
		traceID = traceInfo.getTraceID();
		spanID = traceInfo.getSpanID();
		type = inType;
		title = inTitle;
		detail = inDetail;
		status = inStatus;
		instance = inInstance;
	}

	public ErrorResponseDTO(final LogTraceInfoDTO traceInfo) {
		traceID = traceInfo.getTraceID();
		spanID = traceInfo.getSpanID();
	}

}

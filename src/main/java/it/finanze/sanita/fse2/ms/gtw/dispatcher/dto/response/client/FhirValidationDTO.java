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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ordered validation results returned by the fhir-mapping-engine.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FhirValidationDTO {

	private List<ValidationResultDTO> results;

	/**
	 * Result of one requested validation.
	 */
	@Data
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ValidationResultDTO {

		private String type;

		private List<ValidationIssueDTO> issues;

		/**
		 * Calcolato dalle issues (stessa regola del fhir-mapping-engine): il risultato è non valido
		 * se almeno una issue ha severity ERROR o FATAL.
		 */
		public boolean isValid() {
			if (issues == null) {
				return true;
			}
			return issues.stream().noneMatch(i -> "ERROR".equalsIgnoreCase(i.getSeverity())
					|| "FATAL".equalsIgnoreCase(i.getSeverity()));
		}

	}

	/**
	 * A diagnostic produced by a validation.
	 */
	@Data
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ValidationIssueDTO {

		private String severity;

		private String code;

		private List<String> paths;

		private String message;

	}

}

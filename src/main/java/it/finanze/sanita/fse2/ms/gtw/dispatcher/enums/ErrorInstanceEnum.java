
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorInstanceEnum {

	NO_INFO("", "No specific information for this error, refeer to type for any info"),
	CDA_EXTRACTION("/cda-extraction", "Error while extracting CDA from PDF document"),
	CDA_NOT_VALIDATED("/cda-validation", "Error while retrieving information about CDA validation"),
	DIFFERENT_HASH("/jwt-hash-match", "Hash of document different from hash in JWT"),
	MISSING_MANDATORY_ELEMENT("/request-missing-field", "Missing required field in request body"),
	INVALID_DATE_FORMAT("/request-invalid-date-format", "Field of type date not correctly inputed"),
	SEMANTIC_WARNING("/schematron-malformed/warning", "Schematron malformed with non-blocking problem"),
	SEMANTIC_ERROR("/schematron-malformed/error", "Schematron malformed with blocking error"),
	DOCUMENT_TYPE_MISMATCH("/jwt-document-type", "Mismatch on document type from JWT to CDA"),
	PERSON_ID_MISMATCH("/jwt-person-id", "Mismatch on person-id from JWT to CDA"),
	MISSING_JWT("/missing-jwt", "JWT token completely missing"),
	MISSING_JWT_FIELD("/jwt-mandatory-field-missing", "Mandatory field in JWT is missing"),
	JWT_MALFORMED_FIELD("/jwt-mandatory-field-malformed", "Malformed JWT field"),
	FHIR_RESOURCE_ERROR("/fhir-resource", "Error creating fhir resource"),
	INVALID_ID_ERROR("/invalid-id", "L'identificativo documento non è valido"),
	INVALID_ID_WII("/invalid-id", "Il wii non è valido"),
	RECORD_NOT_FOUND("/record-not-found", "Record not found"),
	INVALID_REQ_ID_ERROR("/invalid-req-id", "L'identificativo documento fornito in richiesta non è valido"),
	NON_PDF_FILE("/multipart-file", "File type must be a PDF document"),
	EMPTY_FILE("/empty-multipart-file", "File type must not be empty"),
	OLDER_DAY("/msg/max-day-limit-exceed", "Cannot publish documents older"),
	EDS_DOCUMENT_MISSING("/msg/eds-document-missing", "Document cannot be found on the Server FHIR"),
	SIMULATION_EXCEPTION("/msg/simulation-error", "Simulation error"),
	SIGN_EXCEPTION("/msg/sign-error", "Sign not found on pdf");
	
	private String instance;
	private String description;

	public static ErrorInstanceEnum get(String inInstance) {
		ErrorInstanceEnum out = null;
		for (ErrorInstanceEnum v: ErrorInstanceEnum.values()) {
			if (v.getInstance().equalsIgnoreCase(inInstance)) {
				out = v;
				break;
			}
		}
		return out;
	}


}
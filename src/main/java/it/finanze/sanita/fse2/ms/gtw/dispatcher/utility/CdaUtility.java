
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.utility;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DocumentTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum.INVALID_ID_ERROR;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum.INVALID_REQ_ID_ERROR;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum.INVALID_ID_DOC;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility.*;
import static org.apache.commons.lang3.StringUtils.isWhitespace;

@Slf4j
public final class CdaUtility {

	private static final String MASTER_ID_SEPARATOR = "^";

	private static final String WIF_SUFFIX = "^^^^urn:ihe:iti:xdw:2013:workflowInstanceId";
	private static final String WIF_SEPARATOR = ".";
	/**
	 * Private constructor to avoid instantiation.
	 */
	private CdaUtility() {
		// Constructor intentionally empty.
	}

	public static String createWorkflowInstanceId(final String idDoc) {
		return encodeSHA256Hex(idDoc) + WIF_SEPARATOR + generateWii() + WIF_SUFFIX;
	}

	public static String getWorkflowInstanceId(final org.jsoup.nodes.Document docT) {

		try {
			String cxi = extractInfo(docT);	
			return cxi + WIF_SEPARATOR + generateWii() + WIF_SUFFIX;
		} catch (Exception e) {
			final ErrorResponseDTO error = ErrorResponseDTO.builder()
				.title(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getTitle())
				.type(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getType())
				.instance(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getType())
				.detail("Errore durante l'estrazione del workflow instance id ").build();
			
			throw new ValidationException(error);
		}
	}
	
	private static String extractInfo(final Document docT) {
		String out = "";
		try {
			String id = docT.select("id").get(0).attr("root");
			String extension = docT.select("id").get(0).attr("extension");
			out = id + "." + encodeSHA256Hex(extension);
		} catch(Exception ex) {
			log.error("Error while extracting info from cda", ex);
			throw new BusinessException("Error while extracting info from cda", ex);
		}
		return out;
	}

	public static String getDocumentType(final Document cdaDocument) {
		String docType = Constants.App.MISSING_DOC_TYPE_PLACEHOLDER;
		if (cdaDocument != null) {
			final String code = cdaDocument.select("code").get(0).attr("code");
			DocumentTypeEnum extractedDocType = DocumentTypeEnum.getByCode(code);
			if (extractedDocType != null) {
				docType = extractedDocType.getDocumentType();
			}
		}
		
		return isNullOrEmpty(docType) ? Constants.App.MISSING_DOC_TYPE_PLACEHOLDER : docType;
	}


	/**
	 * Evaluate an identifier and validate it
	 *
	 * @param id The master identifier
	 * @return {@code true} if the identifier is well-formed
	 */
	public static boolean isValidMasterId(String id) {
		if (StringUtility.isNullOrEmpty(id)) return false;
		if (isWhitespace(id)) return false;
		if (!id.contains(MASTER_ID_SEPARATOR)) return true;

		String[] values = id.split("\\"+MASTER_ID_SEPARATOR);
		if (values.length != 2) return false;

		return !values[0].isEmpty() && !values[1].isEmpty();
	}

	public static ErrorResponseDTO createMasterIdError() {
		return ErrorResponseDTO.builder()
			.title(INVALID_ID_DOC.getTitle())
			.type(INVALID_ID_DOC.getType())
			.instance(INVALID_ID_ERROR.getInstance())
			.detail(INVALID_ID_ERROR.getDescription())
			.build();
	}

	public static ErrorResponseDTO createReqMasterIdError() {
		return ErrorResponseDTO.builder()
			.title(INVALID_ID_DOC.getTitle())
			.type(INVALID_ID_DOC.getType())
			.instance(INVALID_REQ_ID_ERROR.getInstance())
			.detail(INVALID_REQ_ID_ERROR.getDescription())
			.build();
	}

}

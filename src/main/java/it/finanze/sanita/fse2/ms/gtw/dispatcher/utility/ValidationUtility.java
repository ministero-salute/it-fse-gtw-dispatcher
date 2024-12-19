
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

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum.INVALID_ID_DOC;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum.OLDER_DAY;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.SubjectOrganizationEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;


public final class ValidationUtility {

	private static final String REPOSITORY_UNIQUE_ID_REGEX = "^2\\.16\\.840\\.1\\.113883\\.2\\.9\\.2\\.(\\d+)\\.4\\.5\\..*$";

	public static void checkDayAfterValidation(Date insertionDate, Integer dayAllowToPublishAfterValidation) {
		if(DateUtility.getDifferenceDays(insertionDate, new Date()) > dayAllowToPublishAfterValidation) {
			final ErrorResponseDTO error = ErrorResponseDTO.builder()
					.type(OLDER_DAY.getType())
					.title(OLDER_DAY.getTitle())
					.instance(ErrorInstanceEnum.OLDER_DAY.getInstance())
					.detail("Error: cannot publish documents older than " + dayAllowToPublishAfterValidation + " days").build();
			throw new ValidationException(error); 
		}
	}

	public static void repositoryUniqueIdValidation(String repositoryUniqueId) {
		Pattern pattern = Pattern.compile(REPOSITORY_UNIQUE_ID_REGEX);
		Matcher m = pattern.matcher(repositoryUniqueId);

		boolean output;
		if(m.matches()) {
			output = true;
			if(m.groupCount()>0) {
				String groupToCheck = StringUtils.leftPad(m.group(1), 3,"0");
				output = SubjectOrganizationEnum.getCode(groupToCheck) != null;
			}
		} else {
			output = false;
		}

		if (!output) {
			final ErrorResponseDTO error = ErrorResponseDTO.builder()
					.type(INVALID_ID_DOC.getType())
					.title(INVALID_ID_DOC.getTitle())
					.instance(ErrorInstanceEnum.INVALID_ID_ERROR.getInstance())
					.detail("Error: Invalid format for repositoryUniqueId").build();
			throw new ValidationException(error);
		}
	}
 
}
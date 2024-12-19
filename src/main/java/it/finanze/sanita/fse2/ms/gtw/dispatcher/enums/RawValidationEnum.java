
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
import lombok.Getter;

@Getter
public enum RawValidationEnum {

	OK("00", "OK"),
	SYNTAX_ERROR("01", "Errore di sintassi"),
	VOCABULARY_ERROR("02", "Errore dovuto alle terminologie utilizzate"),
	SEMANTIC_ERROR("03", "Errore semantico"),
	SCHEMATRON_NOT_FOUND("04", "Schematron not found"),
	SEMANTIC_WARNING("05", "Warning semantico");
	
	private String code;
	private String description;

	private RawValidationEnum(String inCode, String inDescription) {
		code = inCode;
		description = inDescription;
	}

}
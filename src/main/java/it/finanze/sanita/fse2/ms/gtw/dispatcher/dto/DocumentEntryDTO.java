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

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DocumentEntryDTO {

	private String mimeType;
	private String creationTime;
	private String hash;
	private Integer size;
	private List<String> administrativeRequest;
	private String author;
	private String authorRole;
	private String authorInstitution;
	private String repositoryUniqueId;
	private String patientId;
	private String conservazioneANorma;
	private List<String> description;
	private String serviceStartTime;
	private String serviceStopTime;
	private String classCode;
	private String classCodeName;
	private String confidentialityCode;
	private String confidentialityCodeDisplayName;
	private String formatCode;
	private String formatCodeName;
	private List<String> eventCodeList;
	private String healthcareFacilityTypeCode;
	private String healthcareFacilityTypeCodeName;
	private String practiceSettingCode;
	private String practiceSettingCodeName;
	private String typeCode;
	private String typeCodeName;
	private String uniqueId;
	private List<String> referenceIdList;
	 
}

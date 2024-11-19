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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeleteRequestDTO {
    private String idDoc;
    private List<String> uuid;
    private String sub;
    private String iss;
    private String subject_organization;
    private String subject_organization_id;
    private String locality;
    private String subject_role;
    private String person_id;
    private String purpose_of_use;
    private String action_id;
    private String resource_hl7_type;
    private Boolean patient_consent;
    private String documentType;
	private String subject_application_id;
	private String subject_application_vendor;
	private String subject_application_version;
	private String workflow_instance_id;
	private List<String> administrative_request;
	private String author_institution;
}

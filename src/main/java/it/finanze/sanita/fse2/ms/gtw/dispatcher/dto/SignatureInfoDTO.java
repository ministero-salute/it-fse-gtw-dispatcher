
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

import java.security.Principal;
import java.util.Date;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignatureInfoDTO {
	
	private Principal principal;
	private Date notAfter;
	private Date notBefore;
	private String fieldType;
	private String fullyQualifiedName;
	private String location;
	private String contactInfo;
	private String name;
	private String reason;
	private Date signDate;
	private Boolean valid;
	private String digestAlgOID;
	private String encrypAlgOID;
	
}
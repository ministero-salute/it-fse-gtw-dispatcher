
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

public enum EventTypeEnum {

	VALIDATION("VALIDATION"),
	VALIDATION_FOR_PUBLICATION("VALIDATION_FOR_PUBLICATION"),
	VALIDATION_FOR_REPLACE("VALIDATION_FOR_REPLACE"),
	PUBLICATION("PUBLICATION"),
	REPLACE("REPLACE"),
	FEEDING("FEEDING"),
	DELETE("DELETE"),
	RIFERIMENTI_INI("RIFERIMENTI_INI"),
	EDS_DELETE("EDS_DELETE"),
	EDS_UPDATE("EDS_UPDATE"),
	INI_DELETE("INI_DELETE"),
	INI_UPDATE("INI_UPDATE"),
	UPDATE("UPDATE"),
	GENERIC_ERROR("Generic error from dispatcher");

	@Getter
	private String name;

	private EventTypeEnum(String inName) {
		name = inName;
	}

}

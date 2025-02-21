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

public enum PurposeOfUseEnum {

	TREATMENT("TREATMENT","Trattamento di cura ordinario"),
	EMERGENCY("EMERGENCY","Trattamento in emergenza"),
	PUBEMERGENCY("PUBEMERGENCY","Trattamento per la salvaguardia di un terzo o della collettività"),
	SYSADMIN("SYSADMIN","Trasferimento del FSE"),
	PERSONAL("PERSONAL","Consultazione del FSE"),
	UPDATE("UPDATE","Invalidamento e aggiornamento di un documento"),
	CONSENT("CONSENT","Comunicazione valori consensi"),
	ADMINISTRATIVE("ADMINISTRATIVE","Operazioni amministrative"),
	ACCESS_UPDATE("ACCESS UPDATE","Operazioni amministrative");
	

	@Getter
	private String display;

	@Getter
	private String description;

	private PurposeOfUseEnum(String inDisplay, String inDescription) {
		display = inDisplay;
		description = inDescription;
	}

	public static PurposeOfUseEnum get(String inDisplay) {
		PurposeOfUseEnum out = null;
		for (PurposeOfUseEnum v:PurposeOfUseEnum.values()) {
			if (v.getDisplay().equalsIgnoreCase(inDisplay)) {
				out = v;
				break;
			}
		}
		return out;
	}
}

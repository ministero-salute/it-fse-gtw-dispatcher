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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad241.enums;

import lombok.Getter;

@Getter
public enum AdministrativeReqAd241Enum {

	SSN("SSN", "Regime SSN"),
	INPATIENT("INPATIENT", "Regime di ricovero"),
	NOSSN("NOSSN", "Regime privato"),
	SSR("SSR", "Regime SSR"),
	DONOR("DONOR", "Regime donatori"),
	AUTO("AUTO","Autoprodotto / Taccuino / Cittadino");
	 
	private String code;
	private String description;

	private AdministrativeReqAd241Enum(String inCode, String inDescription) {
		code = inCode;
		description = inDescription;
	}
	
	/**
	 * Validates if a string value is a valid code for this enum
	 */
	public static boolean isValidCode(String code) {
		if (code == null) return false;
		for (AdministrativeReqAd241Enum value : values()) {
			if (value.getCode().equals(code)) {
				return true;
			}
		}
		return false;
	}

}

// Made with Bob

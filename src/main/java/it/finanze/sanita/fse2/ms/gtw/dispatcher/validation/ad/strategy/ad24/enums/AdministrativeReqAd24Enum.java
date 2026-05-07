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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad24.enums;

import lombok.Getter;

@Getter
public enum AdministrativeReqAd24Enum{

	SSN("SSN", "Regime SSN"),
	INPATIENT("INPATIENT", "Regime di ricovero"),
	NOSSN("NOSSN", "Regime privato"),
	SSR("SSR", "Regime SSR"),
	DONOR("DONOR", "Regime donatori");
	 
	private String code;
	private String description;

	private AdministrativeReqAd24Enum(String inCode, String inDescription) {
		code = inCode;
		description = inDescription;
	}
	
	/**
	 * Validates if a string value is a valid enum constant name
	 */
	public static boolean isValidCode(String code) {
		if (code == null) return false;
		try {
			valueOf(code);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

}


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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum AccreditamentoPrefixEnum {

	CRASH_TIMEOUT("CRASH_TIMEOUT"),
	SKIP_VALIDATION("SKIP_VALIDATION"),
	CRASH_WF_EDS("CRASH_WF_EDS"),
	CRASH_INI("CRASH_INI"),
	CRASH_EDS("CRASH_EDS");
	
	private String prefix;
	 
	
	public static AccreditamentoPrefixEnum get(String inPrefix) {
		AccreditamentoPrefixEnum out = null;
		for (AccreditamentoPrefixEnum v: AccreditamentoPrefixEnum.values()) {
			if (v.getPrefix().equalsIgnoreCase(inPrefix)) {
				out = v;
				break;
			}
		}
		return out;
	}

	public static AccreditamentoPrefixEnum getStartWith(String inPrefix) {
		AccreditamentoPrefixEnum out = null;
		for (AccreditamentoPrefixEnum v: AccreditamentoPrefixEnum.values()) {
			if(inPrefix.startsWith(v.getPrefix())) {
				out = v;
				break;
				}
			}
		return out;
	}

}
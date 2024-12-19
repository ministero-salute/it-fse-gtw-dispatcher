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


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Accepted document types defined by the affinity domain: {@link http://www.hl7italia.it/hl7italia_D7/node/2359}.
 * 
 */
@Getter
@AllArgsConstructor
public enum ConfidentialityCodeEnum {

    NORMAL("N","Normal"),
    VERY_RESTRICTED("V","Very Restricted");

    private final String code;

    private final String display;

   
    
    public static String getDisplayByCode(String code) {
        for (ConfidentialityCodeEnum el : ConfidentialityCodeEnum.values()) {
            if (el.getCode().equals(code)) {
                return el.getDisplay();
            }
        }
        return "";
    }

}
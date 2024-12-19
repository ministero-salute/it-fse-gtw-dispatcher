
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

public enum SubjectOrganizationEnum {

	REGIONE_PIEMONTE("010", "Regione Piemonte"),
	REGIONE_VALLE_AOSTA("020", "Regione Valle d'Aosta"),
	REGIONE_LOMBARDIA("030", "Regione Lombardia"),
	REGIONE_BOLZANO("041", "P.A. Bolzano"),
	REGIONE_TRENTO("042", "P.A. Trento"),
	REGIONE_VENETO("050", "Regione Veneto"),
	REGIONE_FRIULI_VENEZIA_GIULIA("060", "Regione Friuli Venezia Giulia"),
	REGIONE_LIGURIA("070", "Regione Liguria"),
	REGIONE_EMILIA_ROMAGNA("080", "Regione Emilia-Romagna"),
	REGIONE_TOSCANA("090", "Regione Toscana"),
	REGIONE_UMBRIA("100", "Regione Umbria"),
	REGIONE_MARCHE("110", "Regione Marche"),
	REGIONE_LAZIO("120", "Regione Lazio"),
	REGIONE_ABRUZZO("130", "Regione Abruzzo"),
	REGIONE_MOLISE("140", "Regione Molise"),
	REGIONE_CAMPANIA("150", "Regione Campania"),
	REGIONE_PUGLIA("160", "Regione Puglia"),
	REGIONE_BASILICATA("170", "Regione Basilicata"),
	REGIONE_CALABRIA("180", "Regione Calabria"),
	REGIONE_SICILIA("190", "Regione Sicilia"),
	REGIONE_SARDEGNA("200", "Regione Sardegna"),
	INI("000", "INI"),
	SISTEMA_TS("970", "Sistema TS"),
	SASN("001", "SASN"),
	MDS("999", "MDS");

	
	@Getter
	private final String code;
	@Getter
	private final String display;

	SubjectOrganizationEnum(String inCode, String inDisplay) {
		code = inCode;
		display = inDisplay;
	}

	public static SubjectOrganizationEnum getCode(String inCode) {
		SubjectOrganizationEnum out = null;
		for (SubjectOrganizationEnum v: SubjectOrganizationEnum.values()) {
			if (v.getCode().equalsIgnoreCase(inCode)) {
				out = v;
				break;
			}
		}
		return out;
	}
	
	public static SubjectOrganizationEnum getDisplay(String inDisplay) {
		SubjectOrganizationEnum out = null;
		for (SubjectOrganizationEnum v: SubjectOrganizationEnum.values()) {
			if (v.getDisplay().equalsIgnoreCase(inDisplay)) {
				out = v;
				break;
			}
		}
		return out;
	}
}

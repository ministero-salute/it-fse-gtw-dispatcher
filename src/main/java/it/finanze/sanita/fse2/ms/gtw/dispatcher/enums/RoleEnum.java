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

public enum RoleEnum {

	AAS("AAS","Personale di assistenza ad alta specializzazione"),
	APR("APR", "Medico Medicina Generale Pediatra di Libera Scelta"),
	PSS("PSS", "Professionista del sociale"),
	INF("INF", "Personale infermieristico"),
	FAR("FAR", "Farmacista"),
	DSA("DSA", "Direttore sanitario"),
	DAM("DAM", "Direttore amministrativo"),
	OAM("OAM", "Operatore amministrativo"),
	ASS("ASS", "Assistito"),
	TUT("TUT", "Tutore"),
	ING("ING", "Informal giver (Delegato volontario)"),
	GEN("GEN", "Genitore"),
	NOR("NOR", "Nodo regionale"),
	DRS("DRS", "Dirigente sanitario"),
	RSA("RSA", "Medico RSA"),
	MRP("MRP", "Medico Rete di Patologia"),
	INI("INI", "Infrastruttura Nazionale per l’Interoperabilità"),
	OGC("OGC", "Operatore per la gestione dei consensi"),
	OPI("OPI", "Operatore di informativa."),
	MDS("MDS", "Ruolo del Ministero della Salute per la gestione del DGC"),
	GTW("GTW", "Ruolo per il Gateway EDS");
	
	@Getter
	private String code;
	@Getter
	private final String description;

	RoleEnum(String inCode, String inDescription) {
		code = inCode;
		description = inDescription;
	}

	public static RoleEnum get(String code) {
		for (RoleEnum role : RoleEnum.values()) {
			if (role.getCode().equals(code)) {
				return role;
			}
		}
		return null;
	}

}

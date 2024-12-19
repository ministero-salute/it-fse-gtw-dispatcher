
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

@Getter
public enum LowLevelDocEnum {
	
	TXT("TXT","TXT"),
	PDF("PDF","PDF"),
	EROGATO_SISTEMATS("SistemaTS-Prestazione","Erogato SistemaTS"),
	EROGATO_SISTEMATS_FARMACEUTICA("2.16.840.1.113883.2.9.10.1.13.1.1","Erogato SistemaTS farmaceutica"),
	EROGATO_SISTEMATS_SPECIALISTICA("2.16.840.1.113883.2.9.10.1.13.1.2","Erogato SistemaTS specialistica"),
	PRESCRIZIONE_SISTEMATS("SistemaTS-Prescrizione","Prescrizione Sistema TS"),
	PRESCRIZIONE_FARMACEUTICA_SISTEMATS("2.16.840.1.113883.2.9.10.1.2.1","Prescrizione farmaceutica Sistema TS"),
	PRESCRIZIONE_SPECIALISTICA_SISTEMATS("2.16.840.1.113883.2.9.10.1.13.1.1","Prescrizione specialistica Sistema TS"),
	ESENZIONE_REDDITO_SISTEMATS("SistemaTS-Esenzione","Esenzione da reddito SistemaTS"),
	PRESCRIZIONE("2.16.840.1.113883.2.9.10.1.2","Prescrizione"),
	REFERTO_LABORATORIO("2.16.840.1.113883.2.9.10.1.1","Referto di Laboratorio"),
	PROFILO_SANITARIO_SINTETICO("2.16.840.1.113883.2.9.10.1.4.1.1","Profilo Sanitario Sintetico"),
	LETTERA_DIMISSIONE_OSPEDALIERA("2.16.840.1.113883.2.9.10.1.5","Lettera di Dimissione Ospedaliera"),
	REFERTO_RADIOLOGIA("2.16.840.1.113883.2.9.10.1.7.1","Referto di Radiologia"),
	PIANO_TERAPEUTICO("2.16.840.1.113883.2.9.4.3.14","Piano Terapeutico"),
	SCHEDA_SINGOLA_VACCINAZIONE("2.16.840.1.113883.2.9.10.1.11.1.1","Scheda della singola Vaccinazione"),
	CERTIFICATO_VACCINALE("2.16.840.1.113883.2.9.10.1.11.1.2","Certificato Vaccinale"),
	VERBALE_PRONTO_SOCCORSO("2.16.840.1.113883.2.9.10.1.6.1","Verbale di Pronto Soccorso"),
	REFERTO_SPECIALISTICA_AMBULATORIALE("2.16.840.1.113883.2.9.10.1.9.1","Referto di Specialistica Ambulatoriale"),
	DOCUMENTO_GENERICO("2.16.840.1.113883.2.9.10.1.12.1","Documento generico");
	

	private String code;
	private String description;

	private LowLevelDocEnum(String inCode, String inDescription) {
		code = inCode;
		description = inDescription;
	}

}
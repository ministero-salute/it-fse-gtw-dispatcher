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
import java.util.Arrays;

import lombok.Getter;

@Getter
public enum LowLevelDocEnum {
	
	TXT("TXT", "TXT"),
	PDF("PDF", "PDF"),
	SCAN("ScanDocument", "Scansione Documento Cartaceo"),
	FHIR_TACCUINO("FHIRDocumentTaccuino", "Documento on-demand contenente i dati di Taccuino"),
	EROGATO_SISTEMATS("SistemaTS-Prestazione","Erogato Sistema TS"),
	EROGATO_SISTEMATS_FARMACEUTICA("2.16.840.1.113883.2.9.10.1.13.1.1", "Erogato Sistema TS farmaceutica"),
	EROGATO_SISTEMATS_SPECIALISTICA("2.16.840.1.113883.2.9.10.1.13.1.2", "Erogato Sistema TS specialistica"),
	PRESCRIZIONE_SISTEMATS("SistemaTS-Prescrizione", "Prescrizione Sistema TS"),
	PRESCRIZIONE_FARMACEUTICA_SISTEMATS("2.16.840.1.113883.2.9.10.1.2.1", "Prescrizione farmaceutica Sistema TS"),
	PRESCRIZIONE_SPECIALISTICA_SISTEMATS("2.16.840.1.113883.2.9.10.1.2.2", "Prescrizione specialistica Sistema TS"),
	ESENZIONE_REDDITO_SISTEMATS("SistemaTS-Esenzione", "Esenzione da reddito Sistema TS"),
	PRESCRIZIONE("2.16.840.1.113883.2.9.10.1.2" , "Prescrizione"),
	REFERTO_LABORATORIO("2.16.840.1.113883.2.9.10.1.1", "Referto di Laboratorio"),
	PROFILO_SANITARIO_SINTETICO("2.16.840.1.113883.2.9.10.1.4.1.1", "Profilo Sanitario Sintetico"),
	LETTERA_DIMISSIONE_OSPEDALIERA("2.16.840.1.113883.2.9.10.1.5", "Lettera di Dimissione Ospedaliera"),
	REFERTO_RADIOLOGIA("2.16.840.1.113883.2.9.10.1.7.1", "Referto di Radiologia"),
	PIANO_TERAPEUTICO("2.16.840.1.113883.2.9.4.3.14", "Piano Terapeutico"),
	SCHEDA_SINGOLA_VACCINAZIONE("2.16.840.1.113883.2.9.10.1.11.1.1", "Scheda della singola Vaccinazione"),
	CERTIFICATO_VACCINALE("2.16.840.1.113883.2.9.10.1.11.1.2", "Certificato Vaccinale"),
	VERBALE_PRONTO_SOCCORSO("2.16.840.1.113883.2.9.10.1.6.1", "Verbale di Pronto Soccorso"),
	REFERTO_SPECIALISTICA_AMBULATORIALE("2.16.840.1.113883.2.9.10.1.9.1", "Referto di Specialistica Ambulatoriale"),
	DOCUMENTO_GENERICO("2.16.840.1.113883.2.9.10.1.12.1", "Documento generico"),
	REFERTO_DI_ANATOMIA_PATOLOGICA("2.16.840.1.113883.2.9.10.1.8.1", "Referto di Anatomia Patologica"),
	EROGAZIONE_FARMACEUTICA_NON_SSN("2.16.840.1.113883.2.9.10.1.13.2.1", "Erogazione Farmaceutica non a carico SSN"),
	PRESCRIZIONE_FARMACEUTICA_NON_SSN("2.16.840.1.113883.2.9.10.1.3.1", "Prescrizione Farmaceutica non a carico SSN"),
	LETTERA_INVITO_VACCINAZIONE("2.16.840.1.113883.2.9.10.1.15.1", "Lettera di invito per Vaccinazione"),
	LETTERA_INVITO_SCREENING("2.16.840.1.113883.2.9.10.1.14.1", "Lettera di invito per screening o ad altri percorsi di prevenzione"),
	REFERTO_TELEVISITA("2.16.840.1.113883.2.9.10.1.16.1.1", "Referto di Televisita"),
	RELAZIONE_FINALE_TELEMONITORAGGIO("2.16.840.1.113883.2.9.10.1.16.3.1", "Relazione finale di telemonitoraggio"),
	RELAZIONE_FINALE_TELEASSISTENZA("2.16.840.1.113883.2.9.10.1.16.4.1", "Relazione finale di teleassistenza"),
	RELAZIONE_TELECONSULTO("2.16.840.1.113883.2.9.10.1.16.2.1", "Relazione di teleconsulto"),
	CARTELLA_CLINICA("2.16.840.1.113883.2.9.10.1.18.1", "Cartella Clinica"),
	TESSERA_PORTATORE_IMPIANTO("2.16.840.1.113883.2.9.10.1.17.1", "Tessera portatore di impianto"),
	FHIR_TELEVISITA("FHIRDocumentTelevisita", "Bundle contenente i dati FHIR relativi al Referto di Televisita"),
	FHIR_TELECONSULTO("FHIRDocumentTeleconsulto", "Bundle contenente i dati FHIR relativi alla Relazione collaborativa di Teleconsulto"),
	FHIR_TELEMONITORAGGIO("FHIRDocumentTelemonitoraggio", "Bundle contenente i dati FHIR relativi alla Relazione Finale di Telemonitoraggio"),
	FHIR_TELEASSISTENZA("FHIRDocumentTeleassistenza", "Bundle contenente i dati FHIR relativi alla Relazione Finale di Teleassistenza");
	
	
	

	private String code;
	private String description;

	private LowLevelDocEnum(String inCode, String inDescription) {
		code = inCode;
		description = inDescription;
	}
	
	public static LowLevelDocEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(LowLevelDocEnum.values())
                .filter(e -> e.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

}
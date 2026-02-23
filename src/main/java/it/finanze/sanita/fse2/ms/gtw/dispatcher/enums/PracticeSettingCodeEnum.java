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

public enum PracticeSettingCodeEnum {

	AD_PSC001("AD_PSC001", "Allergologia"),
	AD_PSC002("AD_PSC002", "Day Hospital"),
	AD_PSC003("AD_PSC003", "Anatomia e Istologia Patologica"),
	AD_PSC004("AD_PSC004", "Osservazione breve intensiva (OBI) e Pronto Soccorso"),
	AD_PSC005("AD_PSC005", "Angiologia"),
	AD_PSC006("AD_PSC006", "Cardiochirurgia Pediatrica"),
	AD_PSC007("AD_PSC007", "Cardiochirurgia"),
	AD_PSC008("AD_PSC008", "Cardiologia"),
	AD_PSC009("AD_PSC009", "Chirurgia Generale"),
	AD_PSC010("AD_PSC010", "Chirurgia Maxillo-Facciale"),
	AD_PSC011("AD_PSC011", "Chirurgia Pediatrica"),
	AD_PSC012("AD_PSC012", "Chirurgia Plastica"),
	AD_PSC013("AD_PSC013", "Chirurgia Toracica"),
	AD_PSC014("AD_PSC014", "Chirurgia Vascolare"),
	AD_PSC015("AD_PSC015", "Medicina Sportiva"),
	AD_PSC018("AD_PSC018", "Ematologia e Immunoematologia"),
	AD_PSC019("AD_PSC019", "Malattie Endocrine, del Ricambio e della Nutrizione"),
	AD_PSC020("AD_PSC020", "Immunologia"),
	AD_PSC021("AD_PSC021", "Geriatria"),
	AD_PSC024("AD_PSC024", "Malattie Infettive e Tropicali"),
	AD_PSC025("AD_PSC025", "Medicina del Lavoro"),
	AD_PSC026("AD_PSC026", "Medicina Generale"),
	AD_PSC027("AD_PSC027", "Medicina Legale"),
	AD_PSC028("AD_PSC028", "Unita Spinale"),
	AD_PSC029("AD_PSC029", "Nefrologia"),
	AD_PSC030("AD_PSC030", "Neurochirurgia"),
	AD_PSC031("AD_PSC031", "Nido"),
	AD_PSC032("AD_PSC032", "Neurologia"),
	AD_PSC033("AD_PSC033", "Neuropsichiatria Infantile"),
	AD_PSC034("AD_PSC034", "Oculistica"),
	AD_PSC035("AD_PSC035", "Odontoiatria e Stomatologia"),
	AD_PSC036("AD_PSC036", "Ortopedia e Traumatologia"),
	AD_PSC037("AD_PSC037", "Ostetricia e Ginecologia"),
	AD_PSC038("AD_PSC038", "Otorinolaringoiatria"),
	AD_PSC039("AD_PSC039", "Pediatria"),
	AD_PSC040("AD_PSC040", "Psichiatria"),
	AD_PSC041("AD_PSC041", "Medicina termale"),
	AD_PSC042("AD_PSC042", "Tossicologia"),
	AD_PSC043("AD_PSC043", "Urologia"),
	AD_PSC046("AD_PSC046", "Grandi Ustioni Pediatriche"),
	AD_PSC047("AD_PSC047", "Grandi Ustionati"),
	AD_PSC048("AD_PSC048", "Nefrologia (Abilitazione Trapianto Rene)"),
	AD_PSC049("AD_PSC049", "Terapia Intensiva"),
	AD_PSC050("AD_PSC050", "Unità Coronarica"),
	AD_PSC051("AD_PSC051", "Astanteria"),
	AD_PSC052("AD_PSC052", "Dermatologia"),
	AD_PSC054("AD_PSC054", "Emodialisi"),
	AD_PSC055("AD_PSC055", "Farmacologia Clinica"),
	AD_PSC056("AD_PSC056", "Recupero e Riabilitazione Funzionale"),
	AD_PSC057("AD_PSC057", "Fisiopatologia della Riabilitazione Umana"),
	AD_PSC058("AD_PSC058", "Gastroenterologia"),
	AD_PSC060("AD_PSC060", "Lungodegenti"),
	AD_PSC061("AD_PSC061", "Medicina Nucleare"),
	AD_PSC062("AD_PSC062", "Neonatologia"),
	AD_PSC064("AD_PSC064", "Oncologia"),
	AD_PSC065("AD_PSC065", "Oncoematologia Pediatrica"),
	AD_PSC066("AD_PSC066", "Oncoematologia"),
	AD_PSC067("AD_PSC067", "Pensionanti"),
	AD_PSC068("AD_PSC068", "Pneumologia, Fisiopatologia Respiratoria, Tisiologia"),
	AD_PSC069("AD_PSC069", "Radiologia"),
	AD_PSC070("AD_PSC070", "Radioterapia"),
	AD_PSC071("AD_PSC071", "Reumatologia"),
	AD_PSC072("AD_PSC072", "Terapia Intensiva pediatrica"),
	AD_PSC073("AD_PSC073", "Terapia Intensiva Neonatale"),
	AD_PSC074("AD_PSC074", "Radioterapia Oncologica"),
	AD_PSC075("AD_PSC075", "Neuro-Riabilitazione"),
	AD_PSC076("AD_PSC076", "Neurochirurgia Pediatrica"),
	AD_PSC077("AD_PSC077", "Nefrologia Pediatrica"),
	AD_PSC078("AD_PSC078", "Urologia Pediatrica"),
	AD_PSC094("AD_PSC094", "Terapia semi-intensiva"),
	AD_PSC096("AD_PSC096", "Terapia del dolore"),
	AD_PSC097("AD_PSC097", "Detenuti"),
	AD_PSC098("AD_PSC098", "Day Surgery"),
	AD_PSC099("AD_PSC099", "Cure palliative"),
	AD_PSC100("AD_PSC100", "Laboratorio Analisi Chimico Cliniche"),
	AD_PSC101("AD_PSC101", "Microbiologia e Virologia"),
	AD_PSC102("AD_PSC102", "Centro Trasfusionale e Immunoematologico"),
	AD_PSC103("AD_PSC103", "Radiodiagnostica"),
	AD_PSC104("AD_PSC104", "Neuroradiologia"),
	AD_PSC107("AD_PSC107", "Poliambulatorio"),
	AD_PSC109("AD_PSC109", "Centrale Operativa 118"),
	AD_PSC121("AD_PSC121", "Comparti Operatori - Degenza Ordinaria"),
	AD_PSC122("AD_PSC122", "Comparti Operatori - Day Surgery"),
	AD_PSC126("AD_PSC126", "Libera Professione Degenza"),
	AD_PSC129("AD_PSC129", "Trapianto Organi e Tessuti"),
	AD_PSC130("AD_PSC130", "Medicina di Base"),
	AD_PSC131("AD_PSC131", "Assistenza Territoriale"),
	AD_PSC199("AD_PSC199", "Raccolta Consenso"),
	AD_PSC999("AD_PSC999", "Altro");

	@Getter
	private final String code;
	@Getter
	private final String description;

	PracticeSettingCodeEnum(String inCode, String inDescription) {
		code = inCode;
		description = inDescription;
	}
}

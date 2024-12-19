
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request;

import java.util.List;

import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.AttivitaClinicaEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.HealthDataFormatEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.HealthcareFacilityEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.InjectionModeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.PracticeSettingCodeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.TipoDocAltoLivEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *	Request body TS Document publication creation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TSPublicationCreationReqDTO {

	@Schema(description = "Formato dei dati sanitari")
	private HealthDataFormatEnum healthDataFormat;

	@Schema(description = "Modalità di iniezione del CDA")
	private InjectionModeEnum mode;

	@Schema(description = "Tipologia struttura che ha prodotto il documento", required = true)
	private HealthcareFacilityEnum tipologiaStruttura;

	@Schema(description = "Regole di accesso")
	@Size(min = 0, max = 100)
	private List<String> regoleAccesso;

	@Schema(description = "Identificativo documento", required = true)
	@Size(min = 0, max = 100)
	private String identificativoDoc;

	@Schema(description = "Identificativo repository", required = true)
	@Size(min = 0, max = 100)
	private String identificativoRep;

	@Schema(description = "Tipo documento alto livello",required = true)
	private TipoDocAltoLivEnum tipoDocumentoLivAlto;

	@Schema(description = "Assetto organizzativo che ha portato alla creazione del documento", required = true)
	private PracticeSettingCodeEnum assettoOrganizzativo;

	@Schema(description = "Identificativo del paziente al momento della creazione del documento", required = true)
	@Size(min = 11, max = 16)
	private String identificativoPaziente;

	@Schema(description = "Data inizio prestazione")
	@Size(min = 0, max = 100)
	private String dataInizioPrestazione;

	@Schema(description = "Data fine prestazione")
	@Size(min = 0, max = 100)
	private String dataFinePrestazione;

	@Schema(description = "Conservazione a norma")
	@Size(min = 0, max = 100)
	private String conservazioneANorma;

	@Schema(description = "Tipo attività clinica",required = true)
	private AttivitaClinicaEnum tipoAttivitaClinica;

	@Schema(description = "Identificativo sottomissione",required = true)
	@Size(min = 0, max = 100)
	private String identificativoSottomissione;

    @Schema(description = "Forza pubblicazione documento" , defaultValue = "false")
    private boolean forcePublish;

	@Schema(description = "Priorità")
	private Boolean priorita;
}

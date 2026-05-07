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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
// Enums are validated in service layer; DTO holds raw String values
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMetadataReqDTO {

	@Schema(description = "Tipologia struttura che ha prodotto il documento", requiredMode = RequiredMode.REQUIRED)
	private String tipologiaStruttura;

	@Size(min = 0, max = 100)
	@ArraySchema(schema = @Schema(maxLength = 1000, description = "Regola di accesso"))
	private List<String> attiCliniciRegoleAccesso;

	@Schema(description = "Tipo documento alto livello", requiredMode = RequiredMode.REQUIRED)
	private String tipoDocumentoLivAlto;

	@Schema(description = "Assetto organizzativo che ha portato alla creazione del documento", requiredMode = RequiredMode.REQUIRED)
	private String assettoOrganizzativo;

	@Schema(description = "Data inizio prestazione")
	@Size(min = 0, max = 100)
	private String dataInizioPrestazione;

	@Schema(description = "Data fine prestazione")
	@Size(min = 0, max = 100)
	private String dataFinePrestazione;

	@Schema(description = "Conservazione a norma")
	@Size(min = 0, max = 100)
	private String conservazioneANorma;

	@Schema(description = "Tipo attività clinica", requiredMode = RequiredMode.REQUIRED)
	private String tipoAttivitaClinica;

	@Schema(description = "Identificativo sottomissione", requiredMode = RequiredMode.REQUIRED)
	@Size(min = 0, max = 100)
	private String identificativoSottomissione;
	
	@Size(min = 0, max = 100)
    @ArraySchema(schema = @Schema(maxLength = 1000, description = "Descriptions"))
	private List<String> descriptions;

	@Schema(description = "Administrative request")
	@Size(min = 0, max = 1000)
	@ArraySchema(minItems = 0, maxItems = 1000, schema = @Schema(implementation = String.class))
	private List<String> administrativeRequest;
 

}

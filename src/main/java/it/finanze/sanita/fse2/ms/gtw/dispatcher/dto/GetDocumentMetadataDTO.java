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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO restituito dall'endpoint POST /v1/get-document-metadata/{idDoc} di ini-client.
 *
 * <p>I metadati del documento dovrebbero essere estratti lato ini-client e restituiti
 * come campi tipizzati; il dispatcher non deve fare alcun parsing XML.
 * <p>In caso di errore solo {@code errorMessage} è valorizzato.
 */
@Getter
@Setter
@NoArgsConstructor
public class GetDocumentMetadataDTO {

    /**
     * Tutti gli slot dell'ExtrinsicObject come mappa slot-name → primo valore
     * (es. "creationTime", "repositoryUniqueId", "languageCode", …).
     */
    private Map<String, String> metadata;

    /** UUID del documento (attributo id dell'ExtrinsicObject). Null in caso di errore. */
    private String uuid;
    /** Valore dello slot urn:ita:fse:2025:EDSpublished: "TRUE", "FALSE", o null se assente. */
    private String edsPublished;
    private String documentType;
    private String authorInstitution;
    private List<String> administrativeRequest;

    /** Messaggio di errore. Null se l'operazione è andata a buon fine. */
    private String errorMessage;
}

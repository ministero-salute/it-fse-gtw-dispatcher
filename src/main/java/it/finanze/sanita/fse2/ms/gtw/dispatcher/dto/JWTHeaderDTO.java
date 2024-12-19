
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

import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JWTHeaderDTO {

	/**
	 * Algoritmo utilizzato per la firma del token. Valori ammessi: RS256, RS383, RS512.
	 */
	private String alg;

	/**
	 * Tipologia di token. DEVE essere valorizzato con il valore 'JWT'.
	 */
	private String typ;

	/**
	 * Un riferimento opzionale alla chiave usata per la firma del token. Anche se valorizzato non viene utilizzato nella fase di verifica.
	 */
	private String kid;

	/**
	 * certificato X.509 utilizzato per la firma del token. 
	 * Valore in formato DER, codificato in base64.
	 */
	private String x5c;

	/**
	 * Map the object from JSON to object.
	 * 
	 * @param json The JWT token raw to map.
	 * @return The object built or {@code null} if the JSON is invalid.
	 */
	public static JWTHeaderDTO extractHeader(String json) {
		
		JWTHeaderDTO jwtHeader = null;
		try {
			jwtHeader = StringUtility.fromJSON(json, JWTHeaderDTO.class); 
		} catch (Exception e) {
			log.error("Error while validating JWT header");
		}
		
		return jwtHeader;
	}
 
	
	
}

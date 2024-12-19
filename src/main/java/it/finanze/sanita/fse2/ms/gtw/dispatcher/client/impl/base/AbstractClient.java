
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.base;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientResponseException;

import com.google.gson.JsonObject;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ServerResponseException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;

/**
 * Abstract class for client implementations.
 */
public abstract class AbstractClient {

	protected void toServerResponseEx(String identifier, String microservice, RestClientResponseException ex, String endpoint) {
		String message = "";
		try {
			JsonObject jsonObj = StringUtility.fromJSON(ex.getResponseBodyAsString(), JsonObject.class);
			message = jsonObj.get("errorMessage").getAsString();
		} catch(Exception e) {
			message = ex.getResponseBodyAsString();
		}
		
		throw new ServerResponseException(
			microservice,
			endpoint,
			String.format("%s - Errore durante invocazione /%s", identifier, endpoint),
			HttpStatus.valueOf(ex.getRawStatusCode()),
			ex.getRawStatusCode(),
			message
		);

	}

    /**
	 * Error handler.
	 *
	 * @param e1 The thrown Exception.
	 * @param endpoint The endpoint that caused the error.
	 */
	protected void errorHandler(String microservice,HttpStatusCodeException e1, String endpoint) {
		String msg = "Errore durante l'invocazione dell' API " + endpoint + ". Il sistema ha restituito un " + e1.getStatusCode();
		throw new ServerResponseException(microservice, endpoint, msg, e1.getStatusCode(), e1.getRawStatusCode(), e1.getLocalizedMessage());
	}

}

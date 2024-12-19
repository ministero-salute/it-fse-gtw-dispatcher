
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.utility;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
 
@Slf4j
public class JsonUtility {


	/**
	 * Private constructor to avoid instantiation.
	 * 
	 * @throws IllegalStateException
	 */
	private JsonUtility() {}

	private static ObjectMapper mapper = new ObjectMapper(); 

	/**
	 * Methods that converts an Object to a JSON string.
	 * 
	 * @param obj Object to convert.
	 * @return JSON String representation of the Object.
	 */
	public static <T> String objectToJson(T obj) {
		String jsonString = "";

		try {
			jsonString = mapper.writeValueAsString(obj);
		} catch (Exception e) {
			log.error("Errore durante la conversione da oggetto {} a string json: {}", obj.getClass(), e);
		}

		return jsonString;
	}
	
	/**
	 * Methods that converts a JSON String to a Class of a defined type.
	 * 
	 * @param jsonString JSON String representation of the Object.
	 * @return Object created from the JSON String or {@code null} if the conversion fails.
	 */
	public static <T> T jsonToObject(String jsonString, Class<T> clazz) {
		T obj = null;
		try {
			obj = mapper.readValue(jsonString, clazz);
		} catch (Exception e) {
			log.error("Errore durante la conversione da stringa json a oggetto: {}", e);
		}

		return obj;
	}

	public static <T> T clone (Object object, Class<T> outputClass) {
        return JsonUtility.jsonToObject(JsonUtility.objectToJson(object), outputClass);
    }
}

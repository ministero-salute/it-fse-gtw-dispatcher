
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

public enum ErrorLogEnum implements ILogEnum {

	KO_GENERIC("KO-GENERIC", "Errore generico"),
	KO_TIMEOUT("KO-TIMEOUT", "Errore timeout"),
	KO_VAL("KO-VAL", "Errore nella validazione del CDA"),
	KO_PUB("KO-PUB", "Errore nella pubblicazione del CDA"),
	KO_INVALID_DATA("KO-INV-DATA", "Errore: dati di input non validi"),
	KO_SIGN_PDF("KO-SIGN_PDF", "Errore nella firma del PDF"),
	KO_FHIR("KO-FHIR", "Errore nella chiamata al gtw-fhir-mapping"),
	KO_KAFKA("KO-IND", "Errore nella chiamata a Kafka"),
	KO_MONGO_DB("KO-MONGO-DB", "Errore nella chiamata a MongoDB"),
	KO_MONGO_DB_NOT_FOUND("KO-MONGO-DB-NOT-FOUND", "Elemento non trovato sul MongoDB"),
	KO_MONGO("KO-MONGO", "Errore nella chiamata a Mongo"),
	KO_MONGO_NOT_FOUND("KO-MONGO-NOT-FOUND", "Elemento non trovato su Mongo"); 

	@Getter
	private String code;

	@Getter
	private String description;

	private ErrorLogEnum(String inCode, String inDescription) {
		code = inCode;
		description = inDescription;
	}

}

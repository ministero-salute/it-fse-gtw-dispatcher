
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

public enum OperationLogEnum implements ILogEnum {

	VAL_CDA2("VAL-CDA2", "Validazione CDA2"),
	PUB_CDA2("PUB-CDA2", "Pubblicazione CDA2"),
	REPLACE_CDA2("REPLACE-CDA2", "Replace CDA2"),
	DELETE_CDA2("DELETE-CDA2","Delete CDA2"),
	UPDATE_METADATA_CDA2("UPDATE-CDA2","Update metadata CDA2"),
	TRAS_CDA2("TRAS-CDA2", "Trasmissione CDA2"),
	KAFKA_SENDING_MESSAGE("KAFKA-SENDING-MESSAGE", "Invio Messaggio su Kafka"),
	MONGO("MONGO", "Salvataggio/Query su Mongo");

	@Getter
	private String code;

	@Getter
	private String description;

	private OperationLogEnum(String inCode, String inDescription) {
		code = inCode;
		description = inDescription;
	}

}

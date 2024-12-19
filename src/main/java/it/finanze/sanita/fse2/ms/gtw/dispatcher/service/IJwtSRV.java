
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.service;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.SystemTypeEnum;

public interface IJwtSRV {

	/**
	 * Validate the payload of the JWT token for the create operation.
	 *
	 * @param payload The payload of the JWT token.
	 */
	void validatePayloadForCreate(JWTPayloadDTO payload);

	/**
	 * Validate the payload of the JWT token for the update operation.
	 *
	 * @param payload The payload of the JWT token.
	 */
	void validatePayloadForUpdate(JWTPayloadDTO payload);

	/**
	 * Validate the payload of the JWT token for the delete operation.
	 *
	 * @param payload The payload of the JWT token.
	 */
	void validatePayloadForDelete(JWTPayloadDTO payload);

	/**
	 * Validate the payload of the JWT token for the replace operation.
	 *
	 * @param payload The payload of the JWT token.
	 */
	void validatePayloadForReplace(JWTPayloadDTO payload);

	/**
	 * Validate the payload of the JWT token for the feeding operation.
	 *
	 * @param payload The payload of the JWT token.
	 */
	void validatePayloadForValidation(JWTPayloadDTO payload);

	/**
	 * Returns the mapping between issuer and system
	 *
	 * @param issuer The issuer of the JWT token
	 * @return The specific system
	 */
	SystemTypeEnum getSystemByIssuer(String issuer);
	
	void checkFiscalCode(String givenValue, String fieldName);
}

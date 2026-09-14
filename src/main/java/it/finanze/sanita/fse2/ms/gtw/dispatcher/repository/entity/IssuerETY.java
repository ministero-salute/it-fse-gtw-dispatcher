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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Issuer entity for FHIR bundle configuration.
 * Minimal entity containing only fields needed for bundle control.
 */
@Document(collection = "#{@issuersBean}")
@Data
@NoArgsConstructor
public class IssuerETY {

    public static final String ISSUER_FIELD = "issuer";
    public static final String FHIR_BUNDLE_IN_RESPONSE = "fhirBundleInResponse";
    public static final String MOCK_UAR = "mockUar";

    @Id
    private String id;

    @Field(name = ISSUER_FIELD)
    private String issuer;

    @Field(name = FHIR_BUNDLE_IN_RESPONSE)
    private Boolean fhirBundleInResponse;

    @Field(name = MOCK_UAR)
    private Boolean mockUar;
}

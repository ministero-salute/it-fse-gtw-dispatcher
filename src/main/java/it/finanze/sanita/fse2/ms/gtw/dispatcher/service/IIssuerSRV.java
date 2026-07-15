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

/**
 * Service interface for Issuer-related business operations.
 */
public interface IIssuerSRV {
    
    /**
     * Check if FHIR bundle should be included in response for the given issuer.
     * 
     * @param issuerName The issuer name from JWT token
     * @return true if bundle should be included, false otherwise
     */
    boolean isFhirBundleEnabledForIssuer(String issuerName);

    /**
     * Check if EDS is enabled (non-mock) for the given issuer.
     * Returns true when the issuer's mockUar == false (real EDS), false otherwise.
     *
     * @param issuerName The issuer name from JWT token
     * @return true if EDS operations should be performed, false if mock regime
     */
    boolean isEdsEnabledForIssuer(String issuerName);
}

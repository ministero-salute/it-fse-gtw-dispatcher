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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.IssuerETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IIssuerRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IIssuerSRV;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for Issuer-related business operations.
 * Encapsulates business logic for issuer configuration checks.
 */
@Service
@Slf4j
public class IssuerSRV implements IIssuerSRV {

    @Autowired
    private IIssuerRepo issuerRepo;

    @Override
    public boolean isFhirBundleEnabledForIssuer(String issuerName) {
        boolean enabled = false;
        
        try {
            IssuerETY issuer = issuerRepo.getByName(issuerName);
            
            if (issuer != null) {
                Boolean config = issuer.getFhirBundleInResponse();
                enabled = Boolean.TRUE.equals(config);
                
                log.debug("FHIR bundle configuration for issuer '{}': {} (enabled: {})",
                    issuerName, config, enabled);
            } else {
                log.debug("Issuer '{}' not found in database, FHIR bundle disabled by default",
                    issuerName);
            }
        } catch (Exception e) {
            log.warn("Error checking configuration for issuer '{}', " +
                "defaulting to disabled", issuerName, e);
        }
        
        return enabled;
    }
}

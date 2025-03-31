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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class MicroservicesURLCFG {
 
	/**
	 * Ms gtw-validator host.
	 */
	@Value("${ms.url.gtw-validator-service}")
	private String validatorHost;
	
	/**
	 * Ms gtw-fhir-mapping-engine host.
	 */
	@Value("${ms.url.gtw-fhir-mapping-engine-service}")
	private String fhirMappingEngineHost;

	/**
	 * Ms gtw-eds-client host.
	 */
	@Value("${ms.url.eds-client-service}")
	private String edsClientHost;

	/**
	 * Ms gtw-ini-client host.
	 */
	@Value("${ms.url.ini-client-service}")
	private String iniClientHost;
	
	/**
	 * Ms gtw-status-check host.
	 */
	@Value("${ms.url.status-check-client-service}")
	private String statusCheckClientHost;
	
	/**
	 * Ms gtw-config host.
	 */
    @Value("${ms.url.gtw-config}")
    private String configHost;

	@Value("${ms.url.ana-service}")
	private String anaHost;

    @Value("${ms-calls.are-from-govway}")
	private Boolean fromGovway;

}

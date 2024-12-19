
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.ProfileUtility;


@Configuration
public class CollectionNaming {

    @Autowired
    private ProfileUtility profileUtility;

    @Bean("iniEdsInvocationBean")
    public String getIniEdsInvocationCollection() {
        if (profileUtility.isTestProfile()) {
            return Constants.Profile.TEST_PREFIX + Constants.Collections.INI_EDS_INVOCATION;
        }
        return Constants.Collections.INI_EDS_INVOCATION;
    }
    
    @Bean("validatedDocumentsBean")
    public String getValidatedDocuments() {
    	if(profileUtility.isTestProfile()) {
    		return Constants.Profile.TEST_PREFIX + Constants.Collections.VALIDATED_DOCUMENTS; 
    	} 
    	return Constants.Collections.VALIDATED_DOCUMENTS; 
    }
    
    @Bean("auditBean")
    public String getAudit() {
    	if(profileUtility.isTestProfile()) {
    		return Constants.Profile.TEST_PREFIX + Constants.Collections.AUDIT; 
    	} 
    	return Constants.Collections.AUDIT; 
    }

    @Bean("engineBean")
    public String getEngineCollection() {
        if (profileUtility.isTestProfile()) {
            return Constants.Profile.TEST_PREFIX + Constants.Collections.ENGINES;
        }
        return Constants.Collections.ENGINES;
    }
    
}

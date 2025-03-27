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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.config.audit.exclusions;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.audit.AuditExclusion;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class SwaggerExclusion implements AuditExclusion {

    @Autowired
    private SwaggerUiConfigProperties swagger;

    @Autowired
    private SpringDocConfigProperties api;

    @Override
    public boolean verify(String uri, HttpServletRequest req) {
        boolean skip = false;
        // Skip check if uri is null or swagger not enabled
        if(uri != null && swagger.isEnabled()) {
            // Swagger page
            String ui = swagger.getPath();
            // Generative API
            String docs = api.getApiDocs().getPath();
            // Retrieve swagger-ui exposed endpoints
            skip = uri.startsWith(ui) || uri.startsWith(docs);
        }
        return skip;
    }
}

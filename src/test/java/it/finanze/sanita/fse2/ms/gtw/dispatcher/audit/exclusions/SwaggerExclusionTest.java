
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.audit.exclusions;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.audit.exclusions.SwaggerExclusion;
import org.junit.jupiter.api.Test;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.Profile.TEST;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles(TEST)
class SwaggerExclusionTest {

    private final List<String> TEST_EP = Arrays.asList("/v1/status", "/v1/health-ready", "/test", "/not-found");

    @Autowired
    private SwaggerUiConfigProperties swagger;

    @Autowired
    private SpringDocConfigProperties api;
    @Autowired
    private SwaggerExclusion exclusion;

    @Test
    void verify() {
        for (String path : getSwaggerPath()) {
            assertTrue(exclusion.verify(path, null));
        }
        for (String path : TEST_EP) {
            assertFalse(exclusion.verify(path, null));
        }
    }

    private List<String> getSwaggerPath() {
        return Arrays.asList(swagger.getPath(), api.getApiDocs().getPath());
    }

}

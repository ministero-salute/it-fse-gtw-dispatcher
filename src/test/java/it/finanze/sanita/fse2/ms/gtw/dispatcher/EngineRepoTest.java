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
package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.engine.EngineETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IEngineRepo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
public class EngineRepoTest {

    @Autowired
	private MongoTemplate mongoTemplate;

    @Autowired
    private IEngineRepo repository;

    @BeforeEach
	void init() {
		mongoTemplate.dropCollection(EngineETY.class);
	}

    @Test
    void getLatestEngineTest() {
        // Data preparation
        EngineETY engine = new EngineETY();
        engine.setId("id_test");
        engine.setLastSync(new Date());
        engine.setAvailable(true);
        // Insert engine on DB
        mongoTemplate.insert(engine);
        // Perform getLatestEngine()
        EngineETY response = repository.getLatestEngine();
        // Assertions
        assertEquals(engine.getId(), response.getId());
        assertEquals(engine.getLastSync(), response.getLastSync());
        assertTrue(response.isAvailable());
    }
    
}

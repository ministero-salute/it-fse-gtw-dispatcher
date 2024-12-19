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

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.engine.EngineETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.engine.sub.EngineMap;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.impl.EngineRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.EngineSRV;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
class EngineSRVTest {

    @Autowired
    EngineSRV engineSRV;

    @MockBean
    EngineRepo engineRepo;

    @Test
    void testGetStructureObjectID_Exception(){
        when(engineRepo.getLatestEngine()).thenReturn(null);

        assertThrows(
                BusinessException.class,
                () -> engineSRV.getStructureObjectID("test")
        );
    }

    @Test
    void testGetStructureObjectID_NotMapException(){
        when(engineRepo.getLatestEngine()).thenReturn(new EngineETY());

        assertThrows(
                BusinessException.class,
                () -> engineSRV.getStructureObjectID("test")
        );
    }

    @Test
    void testGetStructureObjectID_Success(){
        Pair<String, String> expectedValue;
        EngineETY engine = new EngineETY();
        engine.setId("656f557479464f1e20f869a7");
        EngineMap engineMap = new EngineMap();
        engineMap.setOid("6537915bd1efbd30cc7d62bf");
        engineMap.setRoot(Arrays.asList("2.16.840.1.113883.2.9.10.1.8.1", "2.16.840.1.113883.2.9.10.1.5"));
        engineMap.setUri("http://salute.gov.it/ig/cda-fhir-maps/StructureMap/RefertodiAnatomiaPatologica");
        engineMap.setVersion("1.0");
        List<EngineMap> enginesMap = new ArrayList<>();
        enginesMap.add(engineMap);
        engine.setRoots(enginesMap);
        engine.setAvailable(true);
        engine.setLastSync(new Date());
        engine.setFiles(new ArrayList<ObjectId>());

        expectedValue = Pair.of(engine.getId(), engineMap.getOid());
        Pair<String, String> actualValue;
        when(engineRepo.getLatestEngine()).thenReturn(engine);
        actualValue = engineSRV.getStructureObjectID("2.16.840.1.113883.2.9.10.1.8.1");

        assertEquals(expectedValue, actualValue);
    }

    @Test
    void testGetStructureObjectID_NoMapException(){
        Pair<String, String> expectedValue;
        EngineETY engine = new EngineETY();
        engine.setId("656f557479464f1e20f869a7");

        Pair<String, String> actualValue;
        when(engineRepo.getLatestEngine()).thenReturn(engine);
        assertThrows(
                BusinessException.class,
                () -> engineSRV.getStructureObjectID("test"),
                String.format("Nessuna mappa con id %s è stata trovata nell'engine %s", engine.getId(), "test")
        );

    }

}

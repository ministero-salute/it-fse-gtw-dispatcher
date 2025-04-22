package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.engine.EngineETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.engine.sub.EngineMap;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.impl.EngineRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.EngineSRV;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
class EngineSRVTest {

    @Autowired
    EngineSRV engineSRV;

    @MockitoBean
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

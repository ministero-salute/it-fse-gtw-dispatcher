package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IAccreditamentoSimulationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IEngineSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.FileUtility;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
public class AccreditamentoSimulationTest {
    
    @Autowired
    private IAccreditamentoSimulationSRV service;

    @MockitoBean
	private IEngineSRV engines;

    @Test
    void simulationCrashTimeoutTest() {
        byte[] pdfAttachment = FileUtility.getFileFromInternalResources("Files/accreditamento/SIGNED_LDO1.pdf");
        assertThrows(ConnectionRefusedException.class, () -> service.runSimulation("CRASH_TIMEOUT_id", pdfAttachment, EventTypeEnum.GENERIC_ERROR));
    }

}

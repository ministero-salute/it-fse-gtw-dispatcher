package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.WarningUtility;

class WarningUtilityTest {

    @Test
    void mergesExistingAndMappingWarnings() {
        assertEquals("semantic, normalization, extraction",
                WarningUtility.merge("semantic", "normalization", "extraction"));
        assertEquals("mapping", WarningUtility.merge(null, "", "mapping", "mapping"));
        assertNull(WarningUtility.merge(null, ""));
    }

    @Test
    void limitsCombinedWarningLength() {
        String warning = WarningUtility.merge("a".repeat(Constants.App.MAX_SIZE_WARNING), "mapping");
        assertEquals(Constants.App.MAX_SIZE_WARNING, warning.length());
        assertTrue(warning.endsWith("..."));
    }
}

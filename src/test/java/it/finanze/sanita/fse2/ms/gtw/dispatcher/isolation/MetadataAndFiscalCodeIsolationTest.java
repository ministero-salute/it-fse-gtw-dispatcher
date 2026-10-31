package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.ValidationCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.UtilitySRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.AffinityDomainUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CfUtility;

@Tag("ISOLATION")
class MetadataAndFiscalCodeIsolationTest {
    @Test void creationDateIgnoresOtherSlotsAndTimeOfDay() {
        String xml = "<ExtrinsicObject><Slot name='other'/><Slot name='creationTime'><ValueList><Value> 20261031125959 </Value></ValueList></Slot></ExtrinsicObject>";
        assertEquals(LocalDate.of(2026, 10, 31), new AffinityDomainUtility().extractCreationTime(xml));
    }

    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings = {" ", "broken XML", "<root/>", "<ExtrinsicObject/>",
        "<ExtrinsicObject><Slot name='other'/></ExtrinsicObject>",
        "<ExtrinsicObject><Slot name='creationTime'/></ExtrinsicObject>",
        "<ExtrinsicObject><Slot name='creationTime'><ValueList/></Slot></ExtrinsicObject>",
        "<ExtrinsicObject><Slot name='creationTime'><ValueList><Value> </Value></ValueList></Slot></ExtrinsicObject>",
        "<ExtrinsicObject><Slot name='creationTime'><ValueList><Value>2026</Value></ValueList></Slot></ExtrinsicObject>",
        "<ExtrinsicObject><Slot name='creationTime'><ValueList><Value>invalid!</Value></ValueList></Slot></ExtrinsicObject>"})
    void malformedOrMissingCreationTimeIsRejected(String xml) {
        assertThrows(IllegalArgumentException.class, () -> new AffinityDomainUtility().extractCreationTime(xml));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void specialFiscalCodesRequireExplicitPermission(boolean allow) {
        var cfg = mock(ValidationCFG.class);
        when(cfg.getAllowSpecialFiscalCodes()).thenReturn(allow);
        var service = new UtilitySRV();
        ReflectionTestUtils.setField(service, "validationCfg", cfg);
        assertTrue(service.isValidCf("RSSMRA80A01H501U"));
        for (String cf : new String[]{"ENI1234567890123", "STP1234567890123", "00000000000"}) {
            assertEquals(allow, service.isValidCf(cf), cf);
        }
        for (String cf : new String[]{null, "", "invalid", "RSSMRA80A01H501A", "00000000001"}) {
            assertFalse(service.isValidCf(cf), cf);
        }
    }

    @Test void fiscalCodeChecksumUsesEveryAlphabeticAndNumericWeight() {
        // Independent published odd-position weight table; exercise the public validator,
        // including omocodia letters, rather than its private conversion helpers.
        int[] odd = {1,0,5,7,9,13,15,17,19,21,2,4,18,20,11,3,6,8,12,14,16,10,22,25,24,23};
        for (char c = 'A'; c <= 'Z'; c++) {
            String prefix = "" + c + c + "AAAA00A00A000";
            int sum = 0;
            for (int i = 0; i < 15; i++) {
                char ch = prefix.charAt(i);
                int index = Character.isDigit(ch) ? ch - '0' : ch - 'A';
                sum += i % 2 == 0 ? odd[index] : index;
            }
            assertEquals(CfUtility.CF_OK_16, CfUtility.validaCF(prefix + (char) ('A' + sum % 26)));
        }
        assertEquals(CfUtility.CF_OK_16, CfUtility.validaCF("rssmra80a01h501u "));
        assertEquals(CfUtility.CF_NON_CORRETTO, CfUtility.validaCF("1234567890/"));
        assertEquals(CfUtility.CF_NON_CORRETTO, CfUtility.validaCF("1234567890:"));
        assertEquals(CfUtility.CF_NON_CORRETTO, CfUtility.validaCF("1SSMRA80A01H501U"));
        assertEquals(CfUtility.CF_NON_CORRETTO, CfUtility.validaCF("RSSMRA!0A01H501U"));
        assertEquals(CfUtility.CF_NON_CORRETTO, CfUtility.validaCF("RSSMRA0!A01H501U"));
        assertNotEquals(CfUtility.CF_ENI_OK, CfUtility.validaCF("ENIABCDEFGHIJKLM"));
        assertNotEquals(CfUtility.CF_STP_OK, CfUtility.validaCF("STPABCDEFGHIJKLM"));
    }
}

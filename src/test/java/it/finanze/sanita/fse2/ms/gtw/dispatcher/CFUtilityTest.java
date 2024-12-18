package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.*;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CfUtility;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
class CFUtilityTest {

    @Test
    void testValidaCF_nullInput() {
        int result = CfUtility.validaCF(null);
        assertEquals(CfUtility.CF_NON_CORRETTO, result);
    }

    @Test
    void testValidaCF_emptyInput() {
        int result = CfUtility.validaCF("");
        assertEquals(CfUtility.CF_NON_CORRETTO, result);
    }

    @Test
    void testValidaCF_invalidShortLength() {
        int result = CfUtility.validaCF("ABC");
        assertEquals(CfUtility.CF_NON_CORRETTO, result);
    }

    @Test
    void testValidaCF_invalidLongLength() {
        int result = CfUtility.validaCF("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        assertEquals(CfUtility.CF_NON_CORRETTO, result);
    }

    @Test
    void testValidaCF_valid11Digit() {
        String validCF11 = "12345678903";
        int result = CfUtility.validaCF(validCF11);
        assertEquals(CfUtility.CF_OK_11, result);
    }

    @Test
    void testValidaCF_invalid11DigitCheck() {
        String invalidCF11 = "12345678904";
        int result = CfUtility.validaCF(invalidCF11);
        assertEquals(CfUtility.CF_CHECK_DIGIT_11, result);
    }

    @Test
    void testValidaCF_invalid11DigitFormat() {
        String invalidCF11 = "12345A78903";
        int result = CfUtility.validaCF(invalidCF11);
        assertEquals(CfUtility.CF_NON_CORRETTO, result);
    }

    @Test
    void testValidaCF_invalid16DigitCheck() {
        String invalidCF16 = "RSSMRA85M01H501X";
        int result = CfUtility.validaCF(invalidCF16);
        assertEquals(CfUtility.CF_CHECK_DIGIT_16, result);
    }

    @Test
    void testValidaCF_invalid16DigitFormat() {
        String invalidCF16 = "RSSMRA85M01H50$Z";
        int result = CfUtility.validaCF(invalidCF16);
        assertEquals(CfUtility.CF_NON_CORRETTO, result);
    }

    @Test
    void testValidaCF_eniPrefix() {
        String eniCF = "ENI1234567890123";
        int result = CfUtility.validaCF(eniCF);
        assertEquals(CfUtility.CF_ENI_OK, result);
    }

    @Test
    void testValidaCF_stpPrefix() {
        String stpCF = "STP1234567890123";
        int result = CfUtility.validaCF(stpCF);
        assertEquals(CfUtility.CF_STP_OK, result);
    }

    @Test
    void testValidaCF_invalidEniStpPrefix() {
        String invalidPrefixCF = "XYZ1234567890123";
        int result = CfUtility.validaCF(invalidPrefixCF);
        assertEquals(CfUtility.CF_NON_CORRETTO, result);
    }

}

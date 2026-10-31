package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.ValidationCDAReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;

@Tag("ISOLATION")
class DocumentUtilitiesIsolationTest {
    static byte[] pdf(String text) throws Exception {
        try (var document = new PDDocument(); var bytes = new ByteArrayOutputStream()) {
            var page = new PDPage();
            document.addPage(page);
            try (var stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 10);
                stream.newLineAtOffset(20, 700);
                stream.showText(text);
                stream.endText();
            }
            document.save(bytes);
            return bytes.toByteArray();
        }
    }

    @Test
    void extractsFhirValuesWithArraysWildcardsAndCaseInsensitiveResourceTypes() {
        String bundle = """
            {"resourceType":"Bundle","identifier":{"value":"id"},"tags":["a","b"],"entry":[{},
            {"resource":{"resourceType":"Patient","identifier":[{"value":"first"},{"value":"second"}],"names":["A","B"]}},
            {"resource":{"resourceType":"Patient","identifier":[{"value":"third"}]}},
            {"resource":{"resourceType":"Observation","valueString":"ignore"}}]}
            """;
        assertEquals(List.of("first", "second", "third"), FhirUtility.extractValues(bundle, "patient", "identifier[*].value"));
        assertEquals("second", FhirUtility.extractValue(bundle, "Patient", "identifier[1].value"));
        assertEquals(List.of("A", "B"), FhirUtility.extractValues(bundle, "Patient", "names"));
        assertEquals(List.of("a", "b"), FhirUtility.extractValues(bundle, "bundle", "tags"));
        assertEquals("id", FhirUtility.extractValue(bundle, "Bundle", "identifier.value"));
        for (String path : List.of("identifier[-1].value", "identifier[99].value", "identifier[invalid]", "missing.value", "names[0].missing", " ")) {
            assertNull(FhirUtility.extractValue(bundle, "Patient", path));
        }
        assertNull(FhirUtility.extractValue(bundle, "Patient", null));
        assertTrue(FhirUtility.extractValues("{}", "Patient", "identifier").isEmpty());
        assertThrows(BusinessException.class, () -> FhirUtility.extractValues("invalid", "Patient", "identifier"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void absentBundlesHaveNoExtractedValues(String input) {
        assertTrue(FhirUtility.extractValues(input, "Patient", "id").isEmpty());
        assertThrows(ValidationException.class, () -> FhirUtility.getWorkflowInstanceId(input));
    }

    @Test
    void workflowIdentifierIncludesHashedDocumentIdentityAndUniqueSuffix() {
        String first = FhirUtility.getWorkflowInstanceId(FhirResourcesIsolationTest.BUNDLE);
        assertTrue(first.startsWith("urn:test." + StringUtility.encodeSHA256Hex("document") + "."));
        assertTrue(first.endsWith("^^^^urn:ihe:iti:xdw:2013:workflowInstanceId"));
        assertNotEquals(first, FhirUtility.getWorkflowInstanceId(FhirResourcesIsolationTest.BUNDLE));
        assertTrue(FhirUtility.getWorkflowInstanceId("{\"identifier\":{\"value\":\"id\"}}")
                .startsWith(StringUtility.encodeSHA256Hex("id") + "."));
        assertThrows(ValidationException.class, () -> FhirUtility.getWorkflowInstanceId("{}"));
    }

    @ParameterizedTest
    @CsvSource({"document,true", "1.2.3^document,true", "^document,false", "document^,false", "a^b^c,false", "^,false"})
    void checksMasterIdentifierStructure(String input, boolean valid) {
        assertEquals(valid, FhirUtility.isValidMasterId(input));
    }

    @Test
    void extractsJsonFromPdfAndRejectsTruncatedOrNonBundleContent() throws Exception {
        String json = "{\"resourceType\":\"Bundle\",\"identifier\":{\"value\":\"id\"}}";
        assertEquals(json, FhirUtility.extractJsonFromPdf(pdf("prefix " + json + " suffix")));
        assertEquals("", FhirUtility.extractJsonFromPdf(pdf("No json")));
        assertThrows(RuntimeException.class, () -> FhirUtility.extractJsonFromPdf(pdf("{\"resourceType\":\"Bundle\"")));
        assertThrows(RuntimeException.class, () -> FhirUtility.extractJsonFromPdf(pdf("{\"other\":1}")));
        assertThrows(RuntimeException.class, () -> FhirUtility.extractJsonFromPdf(new byte[] {1, 2, 3}));
        assertTrue(PDFUtility.isPdf(pdf("test")));
        assertFalse(PDFUtility.isPdf(null));
        assertFalse(PDFUtility.isPdf(new byte[0]));
        assertFalse(PDFUtility.isPdf("not pdf".getBytes(StandardCharsets.UTF_8)));
        assertNull(PDFUtility.extractContentFromAttachments(pdf("No attachments"), "cda.xml"));
        assertNull(PDFUtility.unenvelopeA2(pdf("No XFA")));
    }

    @Test
    void decodesXmlAccordingToDeclaredEncodingAndRejectsInvalidEncoding() {
        String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><ClinicalDocument>è</ClinicalDocument>";
        assertEquals(xml, PDFUtility.detectCharsetAndExtract(xml.getBytes(StandardCharsets.ISO_8859_1)));
        assertThrows(BusinessException.class, () -> PDFUtility.detectCharsetAndExtract("<?xml version=\"1.0\" encoding=\"NO-SUCH-ENCODING\"?><x/>".getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @CsvSource({"RSSMRA22A01A399Z,1", "rssmra22a01a399z,1", "RSSMRA22A01A399A,2", "00000000000,1", "00000000001,2", "AAAAAAAAAAA,0", "ENI1234567890123,3", "STP1234567890123,4", "ENIXXXXXXXXXXXXX,2", "STPXXXXXXXXXXXXX,2", "1234567890123456,0", "RSSMRA22A01A39!Z,0"})
    void validatesFiscalCodeChecksumsAndTemporaryIdentifiers(String input, int expected) {
        assertEquals(expected, CfUtility.validaCF(input));
    }

    @Test
    void extractsOnlyMefFiscalCodesFromQualifiedSubjects() {
        String oid = "2.16.840.1.113883.2.9.4.3.2";
        assertEquals("RSSMRA22A01A399Z", CfUtility.extractFiscalCodeFromJwtSub("RSSMRA22A01A399Z^^^&" + oid + "&ISO"));
        assertEquals("RSSMRA22A01A399Z", CfUtility.extractFiscalCodeFromJwtSub("RSSMRA22A01A399Z^^^&amp;" + oid + "&amp;ISO"));
        assertEquals(CfUtility.extractFiscalCodeFromJwtSub(null), CfUtility.extractFiscalCodeFromJwtSub("foreign^^^&other&ISO"));
        assertEquals(0, CfUtility.validaCF(null));
        assertEquals(0, CfUtility.validaCF(""));
        assertEquals(0, CfUtility.validaCF("short"));
    }

    @Test
    void hashesSerializeValidateAndMergeWarningsThroughPublicApis() {
        byte[] abc = "abc".getBytes(StandardCharsets.UTF_8);
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", StringUtility.encodeSHA256(abc));
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", StringUtility.encodeSHA1(abc));
        assertEquals("YWJj", StringUtility.encodeBase64(abc));
        assertEquals("616263", StringUtility.encodeHex(abc));
        assertEquals("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=", StringUtility.encodeSHA256B64("abc"));
        assertThrows(BusinessException.class, () -> StringUtility.encodeSHA256(null));
        assertThrows(BusinessException.class, () -> StringUtility.encodeSHA1(null));
        assertThrows(BusinessException.class, () -> StringUtility.encodeSHA256B64(null));
        assertThrows(BusinessException.class, () -> StringUtility.encodeSHA256Hex(null));
        assertThrows(ValidationException.class, () -> StringUtility.fromJSONJackson("{\"unknown\":1}", ValidationCDAReqDTO.class));
        assertThrows(ValidationException.class, () -> StringUtility.fromJSONJackson("invalid", ValidationCDAReqDTO.class));
        assertEquals("a, b", StringUtility.mergeWarnings("a", "b", null, "a", ""));
        assertNull(StringUtility.mergeWarnings(null, ""));
        assertTrue(StringUtility.mergeWarnings("x".repeat(200001)).endsWith("..."));
        assertEquals("alert()", StringUtility.sanitizeMessage("<script>alert()</script>"));
        assertEquals("80", StringUtility.sanitizeSourceId("080"));
        assertEquals("180", StringUtility.sanitizeSourceId("180"));
        assertEquals("11502-2", StringUtility.extractHl7TypeCode("('11502-2^^oid')"));
        assertNull(StringUtility.extractHl7TypeCode(null));
        assertNull(StringUtility.extractHl7TypeCode(" "));
        assertNull(StringUtility.extractHl7TypeCode("^^oid"));
    }
}

package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.SignSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.*;

@Tag("ISOLATION")
class PdfSignatureBranchesIsolationTest {
    @ParameterizedTest @ValueSource(strings = {"none", "empty", "text", "unsignedSignature"})
    void unsignedAndUnpopulatedSignatureFieldsDoNotPassPadesValidation(String formType) throws Exception {
        byte[] pdf;
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            if (!formType.equals("none")) {
                var form = new PDAcroForm(document);
                document.getDocumentCatalog().setAcroForm(form);
                if (formType.equals("text")) {
                    var text = new PDTextField(form);
                    text.setPartialName("text");
                    form.getFields().add(text);
                } else if (formType.equals("unsignedSignature")) {
                    var signature = new PDSignatureField(form);
                    signature.setPartialName("signature");
                    form.getFields().add(signature);
                }
            }
            document.save(output);
            pdf = output.toByteArray();
        }
        assertEquals(formType.equals("unsignedSignature"), SignerUtility.isSigned(pdf));
        var validation = SignerUtility.validate(pdf);
        assertEquals(0, validation.getNumSignatures());
        assertThrows(ValidationException.class, () -> new SignSRV().checkPades(pdf, EventTypeEnum.PUBLICATION));
    }

    @Test void malformedPdfCannotBeReportedAsSignedOrValid() {
        byte[] invalid = {1, 2, 3};
        assertThrows(BusinessException.class, () -> SignerUtility.isSigned(invalid));
        assertFalse(SignerUtility.validate(invalid).getStatus());
    }

    @Test void repositoryPdfWithMalformedSignatureCannotPassCryptographicValidation() throws Exception {
        byte[] pdf = FileUtility.getFileFromInternalResources("Files/attachment/CDA_OK_SIGNED.pdf");
        assertTrue(SignerUtility.isSigned(pdf));
        var result = SignerUtility.validate(pdf);
        // This fixture contains a signature field, but its /Contents hex string
        // is malformed. Field presence must not bypass actual validation.
        assertFalse(result.getStatus());
        assertEquals(0, result.getNumSignatures());
        assertThrows(ValidationException.class, () -> new SignSRV().checkPades(pdf, EventTypeEnum.PUBLICATION));
    }
}

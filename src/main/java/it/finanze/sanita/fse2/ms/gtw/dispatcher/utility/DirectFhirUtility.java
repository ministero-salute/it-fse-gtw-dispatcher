package it.finanze.sanita.fse2.ms.gtw.dispatcher.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.DirectFhirDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DirectFhirSourceEnum;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Component
public class DirectFhirUtility {

    public boolean isPdf(byte[] data) {
        return data.length >= 4 &&
                data[0] == '%' && data[1] == 'P' &&
                data[2] == 'D' && data[3] == 'F';
    }

    public boolean isJson(byte[] data, String filename) {
        String content = new String(data, StandardCharsets.UTF_8).trim();
        return (filename != null && filename.toLowerCase().endsWith(".json")) ||
                content.startsWith("{") || content.startsWith("[");
    }

    public Optional<String> extractJsonFromPdf(byte[] pdfData) {
        try (PDDocument document = PDDocument.load(pdfData)) {
            PDDocumentNameDictionary names = new PDDocumentNameDictionary(document.getDocumentCatalog());
            PDEmbeddedFilesNameTreeNode efTree = names.getEmbeddedFiles();
            if (efTree == null) return Optional.empty();

            Map<String, PDComplexFileSpecification> embeddedFiles = efTree.getNames();
            if (embeddedFiles == null) return Optional.empty();

            for (Map.Entry<String, PDComplexFileSpecification> entry : embeddedFiles.entrySet()) {
                PDComplexFileSpecification fileSpec = entry.getValue();
                String filename = fileSpec.getFilename();

                if (filename != null && filename.toLowerCase().endsWith(".json")) {
                    try (InputStream is = fileSpec.getEmbeddedFile().createInputStream()) {
                        String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        return Optional.of(json);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Errore durante l'estrazione JSON dal PDF: " + e.getMessage(), e);
        }

        return Optional.empty();
    }
}

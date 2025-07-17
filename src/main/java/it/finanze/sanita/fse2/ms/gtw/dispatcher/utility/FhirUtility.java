package it.finanze.sanita.fse2.ms.gtw.dispatcher.utility;


import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.App.*;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility.generateWii;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FhirUtility {
 

	public static String getWorkflowInstanceId() {

		try {
			return generateWii() + WIF_SUFFIX;
		} catch (Exception e) {
			final ErrorResponseDTO error = ErrorResponseDTO.builder()
					.title(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getTitle())
					.type(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getType())
					.instance(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getType())
					.detail("Errore durante l'estrazione del workflow instance id ").build();

			throw new ValidationException(error);
		}
	}

	public static String getWorkflowInstanceIdFromPDF(String json) {

		try {
			return extractId(json) + JSON_EXTENSION + WIF_SEPARATOR + generateWii() + WIF_SUFFIX;
		} catch (Exception e) {
			final ErrorResponseDTO error = ErrorResponseDTO.builder()
					.title(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getTitle())
					.type(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getType())
					.instance(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getType())
					.detail("Errore durante l'estrazione del workflow instance id ").build();

			throw new ValidationException(error);
		}
	}

	public static boolean isPdf(byte[] data) {
		return data.length >= 4 &&
				data[0] == '%' && data[1] == 'P' &&
				data[2] == 'D' && data[3] == 'F';
	}

	private static boolean isPdfInternal(byte[] data) {
		return data.length >= 4 &&
				data[0] == '%' && data[1] == 'P' &&
				data[2] == 'D' && data[3] == 'F';
	}

	public static boolean isJson(byte[] data, String filename) {
		String content = new String(data, StandardCharsets.UTF_8).trim();
		return (filename != null && filename.toLowerCase().endsWith(".json")) ||
				content.startsWith("{") || content.startsWith("[");
	}

	public static String extractJsonFromPdf(byte[] pdfData) {
		try (PDDocument document = PDDocument.load(pdfData)) {
			PDFTextStripper stripper = new PDFTextStripper();
			String fullText = stripper.getText(document);

			int startIndex = fullText.indexOf('{');
			if (startIndex == -1) {
				throw new IOException("Nessuna apertura di JSON trovata nel testo del PDF.");
			}

			int braceCount = 0;
			int endIndex = -1;
			for (int i = startIndex; i < fullText.length(); i++) {
				char ch = fullText.charAt(i);
				if (ch == '{') braceCount++;
				else if (ch == '}') braceCount--;

				if (braceCount == 0) {
					endIndex = i + 1; // incluso
					break;
				}
			}

			if (endIndex == -1) {
				throw new IOException("JSON non bilanciato nel testo del PDF.");
			}

			String jsonCandidate = fullText.substring(startIndex, endIndex);

			if (!jsonCandidate.contains("\"resourceType\"") || !jsonCandidate.contains("Bundle")) {
				throw new IOException("Il contenuto estratto non sembra essere un Bundle FHIR.");
			}

			return jsonCandidate;
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	private static String extractId(String json) {
		int index = json.indexOf("\"id\"");
		if (index == -1) return null;

		int colonIndex = json.indexOf(":", index);
		if (colonIndex == -1) return null;

		int quoteStart = json.indexOf("\"", colonIndex);
		int quoteEnd = json.indexOf("\"", quoteStart + 1);
		if (quoteStart == -1 || quoteEnd == -1) return null;

		return json.substring(quoteStart + 1, quoteEnd);
	}
}
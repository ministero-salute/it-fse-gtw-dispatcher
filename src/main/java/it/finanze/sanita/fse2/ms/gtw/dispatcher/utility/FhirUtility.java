package it.finanze.sanita.fse2.ms.gtw.dispatcher.utility;


import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.App.WIF_SUFFIX;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility.generateWiiFhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FhirUtility {
 

	public static String getWorkflowInstanceId(final String fhirJson) {

		try {
			return generateWiiFhir() + WIF_SUFFIX;
		} catch (Exception e) {
			final ErrorResponseDTO error = ErrorResponseDTO.builder()
					.title(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getTitle())
					.type(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getType())
					.instance(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getType())
					.detail("Errore durante l'estrazione del workflow instance id ").build();

			throw new ValidationException(error);
		}
	}
}
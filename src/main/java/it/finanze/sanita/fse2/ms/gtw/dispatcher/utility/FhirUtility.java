package it.finanze.sanita.fse2.ms.gtw.dispatcher.utility;


import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum.INVALID_ID_ERROR;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum.INVALID_REQ_ID_ERROR;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum.INVALID_ID_DOC;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility.encodeSHA256Hex;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility.generateWii;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility.isNullOrEmpty;
import static org.apache.commons.lang3.StringUtils.isWhitespace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DocumentTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FhirUtility {
	
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static final String MASTER_ID_SEPARATOR = "^";

	private static final String WIF_SUFFIX = "^^^^urn:ihe:iti:xdw:2013:workflowInstanceId";
	private static final String WIF_SEPARATOR = ".";
	 

	public static String getWorkflowInstanceId(final String bundle) {

		try {
			String cxi = extractInfo(bundle);	
			return cxi + WIF_SEPARATOR + generateWii() + WIF_SUFFIX;
		} catch (Exception e) {
			final ErrorResponseDTO error = ErrorResponseDTO.builder()
				.title(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getTitle())
				.type(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getType())
				.instance(RestExecutionResultEnum.WORKFLOW_ID_ERROR.getType())
				.detail("Errore durante l'estrazione del workflow instance id ").build();
			
			throw new ValidationException(error);
		}
	}
	
	private static String extractInfo(final String bundleJson) {
	    try {
	        String system = extractValue(bundleJson, "Bundle", "identifier.system");
	        String value = extractValue(bundleJson, "Bundle", "identifier.value");

	        if (value == null || value.isEmpty()) {
	            throw new BusinessException("Missing identifier.value in FHIR Bundle");
	        }

	        String encoded = encodeSHA256Hex(value);
	        return (system != null ? system + "." : "") + encoded;

	    } catch (final Exception ex) {
	        log.error("Error while extracting info from FHIR bundle", ex);
	        throw new BusinessException("Error while extracting info from FHIR bundle", ex);
	    }
	}



	public static String getDocumentType(final Document cdaDocument) {
		String docType = Constants.App.MISSING_DOC_TYPE_PLACEHOLDER;
		if (cdaDocument != null) {
			final String code = cdaDocument.select("code").get(0).attr("code");
			DocumentTypeEnum extractedDocType = DocumentTypeEnum.getByCode(code);
			if (extractedDocType != null) {
				docType = extractedDocType.getDocumentType();
			}
		}
		
		return isNullOrEmpty(docType) ? Constants.App.MISSING_DOC_TYPE_PLACEHOLDER : docType;
	}


	/**
	 * Evaluate an identifier and validate it
	 *
	 * @param id The master identifier
	 * @return {@code true} if the identifier is well-formed
	 */
	public static boolean isValidMasterId(String id) {
		if (StringUtility.isNullOrEmpty(id)) return false;
		if (isWhitespace(id)) return false;
		if (!id.contains(MASTER_ID_SEPARATOR)) return true;

		String[] values = id.split("\\"+MASTER_ID_SEPARATOR);
		if (values.length != 2) return false;

		return !values[0].isEmpty() && !values[1].isEmpty();
	}

	public static ErrorResponseDTO createMasterIdError() {
		return ErrorResponseDTO.builder()
			.title(INVALID_ID_DOC.getTitle())
			.type(INVALID_ID_DOC.getType())
			.instance(INVALID_ID_ERROR.getInstance())
			.detail(INVALID_ID_ERROR.getDescription())
			.build();
	}

	public static ErrorResponseDTO createReqMasterIdError() {
		return ErrorResponseDTO.builder()
			.title(INVALID_ID_DOC.getTitle())
			.type(INVALID_ID_DOC.getType())
			.instance(INVALID_REQ_ID_ERROR.getInstance())
			.detail(INVALID_REQ_ID_ERROR.getDescription())
			.build();
	}

	/**
     * Estrae un singolo valore da una specifica entry del Bundle FHIR.
     */
	public static String extractValue(String bundleJson, String entryResourceType, String jsonPath) {
	    List<String> values = extractValues(bundleJson, entryResourceType, jsonPath);
	    return values.isEmpty() ? null : values.get(0);
	}

	public static List<String> extractValues(String bundleJson, String entryResourceType, String jsonPath) {
	    List<String> results = new ArrayList<>();

	    if (bundleJson == null || bundleJson.isBlank()) return results;

	    try {
	        JsonNode root = objectMapper.readTree(bundleJson);

	        // Caso speciale: estrazione dal root se il tipo è "Bundle"
	        if ("Bundle".equalsIgnoreCase(entryResourceType)) {
	            List<JsonNode> nodes = navigatePathEnhanced(root, jsonPath);
	            for (JsonNode node : nodes) {
	                if (node != null && !node.isMissingNode()) {
	                    if (node.isValueNode()) {
	                        results.add(node.asText());
	                    } else if (node.isArray()) {
	                        node.forEach(sub -> { if (sub.isValueNode()) results.add(sub.asText()); });
	                    }
	                }
	            }
	            return results;
	        }

	        // Estrazione dai singoli entry[].resource
	        JsonNode entries = root.path("entry");
	        if (!entries.isArray()) return results;

	        for (JsonNode entry : entries) {
	            JsonNode resource = entry.path("resource");
	            if (!resource.has("resourceType")) continue;

	            String resourceType = resource.path("resourceType").asText();
	            if (!entryResourceType.equalsIgnoreCase(resourceType)) continue;

	            List<JsonNode> valueNodes = navigatePathEnhanced(resource, jsonPath);
	            for (JsonNode node : valueNodes) {
	                if (node != null && !node.isMissingNode()) {
	                    if (node.isValueNode()) {
	                        results.add(node.asText());
	                    } else if (node.isArray()) {
	                        node.forEach(sub -> { if (sub.isValueNode()) results.add(sub.asText()); });
	                    }
	                }
	            }
	        }

	    } catch (Exception e) {
	        log.error("Errore durante l'estrazione dei campi FHIR (type={}, path={}): {}", entryResourceType, jsonPath, e.getMessage(), e);
	        throw new BusinessException("Errore durante l'estrazione dei campi FHIR: " + e.getMessage(), e);
	    }

	    return results;
	}

	/**
	 * Naviga un JsonNode usando path con supporto ad array ([0], [*])
	 */
	private static List<JsonNode> navigatePathEnhanced(JsonNode node, String path) {
	    if (node == null || path == null || path.isBlank()) return List.of();

	    String[] parts = path.split("\\.");
	    List<JsonNode> currentNodes = List.of(node);

	    for (String part : parts) {
	        List<JsonNode> nextNodes = new ArrayList<>();

	        for (JsonNode current : currentNodes) {
	            if (current == null || current.isMissingNode()) continue;

	            if (part.endsWith("]")) {
	                String fieldName = part.substring(0, part.indexOf("["));
	                JsonNode arrayNode = current.path(fieldName);
	                if (!arrayNode.isArray()) continue;

	                String indexPart = part.substring(part.indexOf("[") + 1, part.indexOf("]"));
	                if ("*".equals(indexPart)) {
	                    arrayNode.forEach(nextNodes::add);
	                } else {
	                    try {
	                        int index = Integer.parseInt(indexPart);
	                        if (index >= 0 && index < arrayNode.size()) nextNodes.add(arrayNode.get(index));
	                    } catch (NumberFormatException ignored) {}
	                }
	            } else {
	                JsonNode next = current.path(part);
	                if (!next.isMissingNode()) nextNodes.add(next);
	            }
	        }

	        currentNodes = nextNodes;
	        if (currentNodes.isEmpty()) break;
	    }

	    return currentNodes;
	}

 
    public static String extractJsonFromPdf(byte[] pdfData) {
		try (PDDocument document = PDDocument.load(pdfData)) {
			PDFTextStripper stripper = new PDFTextStripper();
			String fullText = stripper.getText(document);

			int startIndex = fullText.indexOf('{');
			if (startIndex == -1) { 
				return "";
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
}
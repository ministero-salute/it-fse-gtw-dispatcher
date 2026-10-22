package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ordered validation results returned by the fhir-mapping-engine.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FhirValidationResponseDTO {

	private List<ValidationResultDTO> results;

	/**
	 * Result of one requested validation.
	 */
	@Data
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ValidationResultDTO {

		private String type;

		private List<ValidationIssueDTO> issues;

		/**
		 * Calcolato dalle issues (stessa regola del fhir-mapping-engine): il risultato è non valido
		 * se almeno una issue ha severity ERROR o FATAL.
		 */
		public boolean isValid() {
			if (issues == null) {
				return true;
			}
			return issues.stream().noneMatch(i -> "ERROR".equalsIgnoreCase(i.getSeverity())
					|| "FATAL".equalsIgnoreCase(i.getSeverity()));
		}

	}

	/**
	 * A diagnostic produced by a validation.
	 */
	@Data
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ValidationIssueDTO {

		private String severity;

		private String code;

		private List<String> paths;

		private String message;

	}

}

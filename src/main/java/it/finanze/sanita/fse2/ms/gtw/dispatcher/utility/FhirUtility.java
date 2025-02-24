package it.finanze.sanita.fse2.ms.gtw.dispatcher.utility;


import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.App.WIF_SUFFIX;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility.generateWiiFhir;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
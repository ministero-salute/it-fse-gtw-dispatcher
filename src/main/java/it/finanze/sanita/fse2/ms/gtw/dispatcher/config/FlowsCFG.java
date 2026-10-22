package it.finanze.sanita.fse2.ms.gtw.dispatcher.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

/**
 * Configurazione delle validazioni aggiuntive da eseguire per ciascun flusso.
 * Le liste sono valorizzate da una sequenza di valori separati da virgola.
 * Se vuote le validazioni non vengono richieste.
 */
@Configuration
@Getter
public class FlowsCFG {

	/**
	 * Validazioni aggiuntive da richiedere al gtw-validator nel flusso di validazione.
	 */
	@Value("${flows.validation.validations:}")
	private List<String> validationFlowValidations;

	/**
	 * Validazioni da richiedere al fhir-mapping-engine nel flusso di pubblicazione.
	 */
	@Value("${flows.publication.validations:}")
	private List<String> publicationFlowValidations;

}

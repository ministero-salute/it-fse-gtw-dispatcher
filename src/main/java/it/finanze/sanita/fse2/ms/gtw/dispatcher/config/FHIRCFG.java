package it.finanze.sanita.fse2.ms.gtw.dispatcher.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 *  Configuration FHIR.
 */
@Configuration
@Getter
public class FHIRCFG {

    /**
     *  FHIR attachment name.
     */
    @Value("${fhir.attachment.name}")
    private String fhirAttachmentName;

    /**
     * Enable/disable FHIR bundle in publication response.
     */
    @Value("${fhir.bundle.response.enable:false}")
    private boolean enableBundleInResponse;

}
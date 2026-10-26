package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto;

import lombok.Data;

@Data
public class FhirResourceWithoutEngineDTO {

    private DocumentReferenceDTO documentReferenceDTO;
	
	private String cda;

}

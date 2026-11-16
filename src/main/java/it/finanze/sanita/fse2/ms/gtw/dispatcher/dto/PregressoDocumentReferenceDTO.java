package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto;

import java.util.List;


import lombok.Getter;
import lombok.Setter;

// DTO used to build the document reference for the pregresso flow.
@Getter 
@Setter
public class PregressoDocumentReferenceDTO {

    private String tipologiaStrutturaCode;
    private List<String> attiCliniciRegoleAccesso;
    private String assettoOrganizzativoDescription;
    private String tipoDocumentoLivAltoCode;
    private String identificativoRep;
    private String dataInizioPrestazione;
    private String dataFinePrestazione;
    private List<String> administrativeRequest;
    private String identificativoDoc;

}

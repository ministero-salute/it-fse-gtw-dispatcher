package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectFhirDTO {

    private String sourceType;

    private String fhir;

    private String wii;

    private String filename;
}

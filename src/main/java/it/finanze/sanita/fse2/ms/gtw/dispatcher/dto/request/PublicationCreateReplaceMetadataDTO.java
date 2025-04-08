package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.HealthDataFormatEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.InjectionModeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicationCreateReplaceMetadataDTO extends PublicationMetadataReqDTO {

    @Schema(description = "Identificativo documento", required = true)
    @Size(min = 0, max = 100)
    private String identificativoDoc;

    @Schema(description = "Identificativo repository", required = true)
    @Size(min = 0, max = 100)
    private String identificativoRep;

    @Schema(description = "Modalità di iniezione del CDA")
    private InjectionModeEnum mode;

    @Schema(description = "Formato dei dati sanitari")
    private HealthDataFormatEnum healthDataFormat;

}

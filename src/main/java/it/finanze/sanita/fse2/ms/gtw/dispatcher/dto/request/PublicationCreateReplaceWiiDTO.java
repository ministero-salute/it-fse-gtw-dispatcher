package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicationCreateReplaceWiiDTO extends PublicationCreateReplaceMetadataDTO {
    @Schema(description = "Identificativo del workflow")
    @Size(min = 0, max = 256)
    private String workflowInstanceId;
}

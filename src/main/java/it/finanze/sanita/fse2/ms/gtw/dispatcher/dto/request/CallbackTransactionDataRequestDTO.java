package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Date;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Builder
@Data
@ToString
public class CallbackTransactionDataRequestDTO {
    @NotNull
    private String workflowInstanceId;
    private String type;
    @NotNull
	private Date insertionDate;
    @NotNull
	private String status;
    private String message;

    /* Optional fields used to enrich control structured logs. */
    private String eventType;
    private String issuer;
    private String subject;
    private String subjectRole;
    private String idDocumento;
    private String documentType;
    private String errorCode;
    private String errorDescription;
}

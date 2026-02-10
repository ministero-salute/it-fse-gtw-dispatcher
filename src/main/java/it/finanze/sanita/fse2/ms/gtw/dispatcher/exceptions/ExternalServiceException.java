package it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;

public class ExternalServiceException extends RuntimeException {
    
    private static final long serialVersionUID = -1452410968533388546L;
	private final ErrorResponseDTO errorResponse;
    
    public ExternalServiceException(String message) {
        super(message);
        this.errorResponse = null;
    }
    
    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorResponse = null;
    }
    
    public ExternalServiceException(ErrorResponseDTO errorResponse) {
        super(errorResponse != null && errorResponse != null ? errorResponse.getDetail() : "External service error");
        this.errorResponse = errorResponse;
    }
    
    public ErrorResponseDTO getErrorResponse() {
        return errorResponse;
    }
}
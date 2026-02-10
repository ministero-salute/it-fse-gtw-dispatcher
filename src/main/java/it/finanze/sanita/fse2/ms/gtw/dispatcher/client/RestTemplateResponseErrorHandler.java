package it.finanze.sanita.fse2.ms.gtw.dispatcher.client;


import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ExternalServiceException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.NoRecordFoundException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

	@Override
	public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
		return (HttpStatus.resolve(httpResponse.getStatusCode().value()).series() == HttpStatus.Series.CLIENT_ERROR || 
				HttpStatus.resolve(httpResponse.getStatusCode().value()).series() == HttpStatus.Series.SERVER_ERROR);
	}

	@Override
	public void handleError(ClientHttpResponse httpResponse) throws IOException {
	    String result = IOUtils.toString(httpResponse.getBody(), StandardCharsets.UTF_8);
	    
	    try {
	        ErrorResponseDTO error = new Gson().fromJson(result, ErrorResponseDTO.class);
	        HttpStatusCode statusCode = httpResponse.getStatusCode();
	        
	        // Gestione errori specifici in base allo status code
	        if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
	            throw new NoRecordFoundException(error);
	        } else if (statusCode.value() == HttpStatus.BAD_REQUEST.value()) {
	            throw new ValidationException(error);
	        } else if (statusCode.value() == HttpStatus.BAD_GATEWAY.value()) {
	            throw new ExternalServiceException(error);
	        } else if (statusCode.is5xxServerError()) {
	            throw new BusinessException(error);
	        } else if (statusCode.is4xxClientError()) {
	            throw new BusinessException(error);
	        } else {
	            throw new BusinessException("Unexpected error: " + statusCode.value());
	        }
	        
	    } catch (JsonSyntaxException e) {
	        log.error("Errore parsing JSON response: {}", result, e);
	        throw new BusinessException("Errore nel parsing della risposta dal server");
	    }
	}

}
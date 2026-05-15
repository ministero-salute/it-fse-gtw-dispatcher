package it.finanze.sanita.fse2.ms.gtw.dispatcher.client;


import java.io.IOException;
import java.nio.charset.StandardCharsets;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationErrorException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;

import com.google.gson.Gson;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.NoRecordFoundException;


@Component
public class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

	@Override
	public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
		return (HttpStatus.resolve(httpResponse.getStatusCode().value()).series() == HttpStatus.Series.CLIENT_ERROR || 
				HttpStatus.resolve(httpResponse.getStatusCode().value()).series() == HttpStatus.Series.SERVER_ERROR);
	}

	@Override
	public void handleError(ClientHttpResponse httpResponse) throws IOException {
		String result = IOUtils.toString(httpResponse.getBody(), StandardCharsets.UTF_8);
		ErrorResponseDTO error = new Gson().fromJson(result, ErrorResponseDTO.class);
		if (httpResponse.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
			throw new BusinessException(error);
		} else if(httpResponse.getStatusCode() == HttpStatus.NOT_FOUND){
            throw new NoRecordFoundException(error);
        } else if(httpResponse.getStatusCode() == HttpStatus.BAD_REQUEST){
            throw new ValidationException(error);
        } else {
			throw new BusinessException("Generic error");
		}
	}

}
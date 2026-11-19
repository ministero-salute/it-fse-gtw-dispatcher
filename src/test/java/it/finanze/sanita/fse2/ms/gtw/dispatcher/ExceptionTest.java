/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * Copyright (C) 2023 Ministero della Salute
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.EdsException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.IniException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.MetadataValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.MockEnabledException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.NoAttachmentInPdfException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.NoRecordFoundException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ServerResponseException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.UnauthorizedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationErrorException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationPublicationErrorException;


class ExceptionTest {

	@Test
	void businessExceptionTest() {
		BusinessException exc = new BusinessException("Error"); 
		
		assertEquals(BusinessException.class, exc.getClass()); 
		assertEquals("Error", exc.getMessage()); 
	} 

	@Test
    void businessExceptionWithMsgAndCauseTest() {
        RuntimeException cause = new RuntimeException("cause");
        BusinessException exc = new BusinessException("Error", cause);

		assertEquals(BusinessException.class, exc.getClass());
        assertEquals("Error", exc.getMessage());
        assertEquals(cause, exc.getCause());
    }
	
	@Test
	void businessExceptionTestWithoutMsg() {
		BusinessException exc = new BusinessException(new RuntimeException()); 
		
		assertEquals(BusinessException.class, exc.getClass()); 
	}

	@Test
    void businessExceptionWithErrorResponseTest() {
        ErrorResponseDTO error = ErrorResponseDTO.builder().detail("detail").build();
        BusinessException exc = new BusinessException(error);

		assertEquals(BusinessException.class, exc.getClass());
        assertEquals(error, exc.getError());
    }
	
	@Test
	void connectionRefusedExceptionTest() {
		String url = "testUrl";
		ConnectionRefusedException exc = new ConnectionRefusedException(url, "message"); 
		
		assertEquals(ConnectionRefusedException.class, exc.getClass());
		assertEquals(url, exc.getUrl());
		assertEquals("message", exc.getMessage());
	} 
	
	@Test
	void edsExceptionTest() {
		EdsException exc = new EdsException("Error"); 
		
		assertEquals(EdsException.class, exc.getClass()); 
		assertEquals("Error", exc.getErrorMessage()); 
	}

	@Test
    void iniExceptionTest() {
        IniException exc = new IniException("ini_error", "wif_123");

		assertEquals(IniException.class, exc.getClass());
        assertEquals("ini_error", exc.getErrorMessage());
        assertEquals("wif_123", exc.getWorkflowInstanceId());
        assertEquals("ini_error", exc.getMessage());
    }

	@Test
    void metadataValidationExceptionTest() {
        ErrorResponseDTO error = ErrorResponseDTO.builder().detail("validation error").build();
        MetadataValidationException exc = new MetadataValidationException(error);

		assertEquals(MetadataValidationException.class, exc.getClass());
        assertEquals(error, exc.getError());
        assertEquals("validation error", exc.getMessage());
    }

	@Test
	void mockEnabledExceptionTest() {
		String iniError = "ini_error";
		String edsError = "eds_error";
		MockEnabledException exc = new MockEnabledException(iniError, edsError); 
		
		assertEquals(MockEnabledException.class, exc.getClass()); 
		assertEquals(iniError, exc.getIniErrorMessage()); 
		assertEquals(edsError, exc.getEdsErrorMessage());
	} 
	

	@Test
    void noAttachmentInPdfExceptionTest() {
        ErrorResponseDTO error = ErrorResponseDTO.builder().detail("no attachment").build();
        NoAttachmentInPdfException exc = new NoAttachmentInPdfException(error);

		assertEquals(NoAttachmentInPdfException.class, exc.getClass());
        assertEquals(error, exc.getError());
    }

	@Test
    void noRecordFoundExceptionTest() {
        ErrorResponseDTO error = ErrorResponseDTO.builder().detail("not found").build();
        NoRecordFoundException exc = new NoRecordFoundException(error);

		assertEquals(NoRecordFoundException.class, exc.getClass());
        assertEquals(error, exc.getError());
    }

	@Test
    void serverResponseExceptionTest() {
        ServerResponseException exc = new ServerResponseException(
            "ini", "delete", "Errore", HttpStatus.INTERNAL_SERVER_ERROR, 500, "detail"
        );

		assertEquals(ServerResponseException.class, exc.getClass());
        assertEquals("ini", exc.getMicroservice());
        assertEquals("delete", exc.getTarget());
        assertEquals("Errore", exc.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exc.getStatus());
        assertEquals(500, exc.getStatusCode());
        assertEquals("detail", exc.getDetail());
    }

	@Test
    void unauthorizedExceptionTest() {
        UnauthorizedException exc = new UnauthorizedException("Unauthorized");
		
		assertEquals(UnauthorizedException.class, exc.getClass());
        assertEquals("Unauthorized", exc.getMessage());
    }

	@Test
    void validationErrorExceptionTest() {
        ValidationErrorException exc = new ValidationErrorException(
            RestExecutionResultEnum.SEMANTIC_ERROR, "Semantic error", "wif_456", "/instance"
        );

		assertEquals(ValidationErrorException.class, exc.getClass());
        assertEquals(RestExecutionResultEnum.SEMANTIC_ERROR, exc.getResult());
        assertEquals("wif_456", exc.getWorkflowInstanceId());
        assertEquals("/instance", exc.getErrorInstance());
        assertEquals("Semantic error", exc.getMessage());
    }

	@Test
    void validationExceptionTest() {
        ErrorResponseDTO error = ErrorResponseDTO.builder().detail("validation failed").build();
        ValidationException exc = new ValidationException(error);

		assertEquals(ValidationException.class, exc.getClass());
        assertEquals(error, exc.getError());
        assertEquals("validation failed", exc.getMessage());
    }

    @Test
    void validationPublicationErrorExceptionTest() {
        ValidationPublicationErrorException exc = new ValidationPublicationErrorException(
            RestExecutionResultEnum.SYNTAX_ERROR, "Syntax error", "/instance"
        );

		assertEquals(ValidationPublicationErrorException.class, exc.getClass());
        assertEquals(RestExecutionResultEnum.SYNTAX_ERROR, exc.getResult());
        assertEquals("/instance", exc.getErrorInstance());
        assertEquals("Syntax error", exc.getMessage());
    }
}

	
 
	

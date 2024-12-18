package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationDataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.ValidatedDocumentsETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IValidatedDocumentsRepo;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.CdaSRV;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
public class CdaSRVTest {

    @MockBean
    private IValidatedDocumentsRepo cdaRepo;

    @Autowired
    private CdaSRV cdaSRV;

    @Test
    public void testCreate_Success() {
        doNothing().when(cdaRepo).create(any(ValidatedDocumentsETY.class));
        cdaSRV.create("hashedCDA", "wii", "transfID", "engineID");
        verify(cdaRepo).create(any(ValidatedDocumentsETY.class));
    }

    @Test
    public void testCreate_Exception() {
        doThrow(new RuntimeException("Mongo error")).when(cdaRepo).create(any(ValidatedDocumentsETY.class));
        assertThrows(BusinessException.class, () -> cdaSRV.create("hashedCDA", "wii", "transfID", "engineID"));
    }

    @Test
    public void testGet_Success() {
        ValidationDataDTO dto = new ValidationDataDTO();
        dto.setWorkflowInstanceId("workflowID123");
        when(cdaRepo.findItemByHash("somehash")).thenReturn(dto);
        String result = cdaSRV.get("somehash");
        assertEquals("workflowID123", result);
        verify(cdaRepo).findItemByHash("somehash");
    }

    @Test
    public void testGet_Exception() {
        when(cdaRepo.findItemByHash("somehash")).thenThrow(new RuntimeException("DB error"));
        assertThrows(BusinessException.class, () -> cdaSRV.get("somehash"));
    }

    @Test
    public void testGetByWorkflowInstanceId_Success() {
        ValidationDataDTO dto = new ValidationDataDTO();
        dto.setWorkflowInstanceId("wii123");
        when(cdaRepo.findItemByWorkflowInstanceId("wii123")).thenReturn(dto);
        ValidationDataDTO result = cdaSRV.getByWorkflowInstanceId("wii123");
        assertEquals("wii123", result.getWorkflowInstanceId());
        verify(cdaRepo).findItemByWorkflowInstanceId("wii123");
    }

    @Test
    public void testGetByWorkflowInstanceId_Exception() {
        when(cdaRepo.findItemByWorkflowInstanceId("wii123")).thenThrow(new RuntimeException("DB error"));
        assertThrows(BusinessException.class, () -> cdaSRV.getByWorkflowInstanceId("wii123"));
    }

    @Test
    public void testRetrieveValidationInfo_Success_FoundNoWiiMismatch() {
        ValidationDataDTO dto = new ValidationDataDTO();
        dto.setWorkflowInstanceId("wiiStored");
        dto.setCdaValidated(true);
        when(cdaRepo.findItemByHash("hashPub")).thenReturn(dto);

        // wiiPublication matches stored wii
        ValidationDataDTO result = cdaSRV.retrieveValidationInfo("hashPub", "wiiStored");
        assertTrue(result.isCdaValidated());

        // wiiPublication does NOT match stored wii
        result = cdaSRV.retrieveValidationInfo("hashPub", "anotherWii");
        assertFalse(result.isCdaValidated());
    }

    @Test
    public void testRetrieveValidationInfo_Success_NotFoundWii() {
        ValidationDataDTO dto = new ValidationDataDTO();
        // workflowInstanceId is null
        when(cdaRepo.findItemByHash("hashPub")).thenReturn(dto);

        ValidationDataDTO result = cdaSRV.retrieveValidationInfo("hashPub", "wiiPub");
        assertFalse(result.isCdaValidated()); // default false
    }

    @Test
    public void testRetrieveValidationInfo_Exception() {
        when(cdaRepo.findItemByHash("hashPub")).thenThrow(new RuntimeException("DB error"));
        assertThrows(BusinessException.class, () -> cdaSRV.retrieveValidationInfo("hashPub", "wiiPub"));
    }

    @Test
    public void testConsumeHash_Success() {
        when(cdaRepo.deleteItem("hashToConsume")).thenReturn(true);
        boolean result = cdaSRV.consumeHash("hashToConsume");
        assertTrue(result);
        verify(cdaRepo).deleteItem("hashToConsume");
    }

    @Test
    public void testConsumeHash_NullOrEmpty() {
        // Shouldn't call the repo if null or empty
        boolean result = cdaSRV.consumeHash("");
        assertFalse(result);
        verify(cdaRepo, never()).deleteItem(anyString());

        result = cdaSRV.consumeHash(null);
        assertFalse(result);
        verify(cdaRepo, never()).deleteItem(anyString());
    }

    @Test
    public void testConsumeHash_Exception() {
        when(cdaRepo.deleteItem("hashToConsume")).thenThrow(new RuntimeException("DB error"));
        assertThrows(BusinessException.class, () -> cdaSRV.consumeHash("hashToConsume"));
    }

    @Test
    public void testCreateBenchMark_Success() {
        doNothing().when(cdaRepo).createBenchmark(any(ValidatedDocumentsETY.class));
        cdaSRV.createBenchMark("hashedCDA", "wii", "transfID", "engineID");
        verify(cdaRepo).createBenchmark(any(ValidatedDocumentsETY.class));
    }

    @Test
    public void testCreateBenchMark_Exception() {
        doThrow(new RuntimeException("Mongo error")).when(cdaRepo).createBenchmark(any(ValidatedDocumentsETY.class));
        assertThrows(BusinessException.class, () -> cdaSRV.createBenchMark("hashedCDA", "wii", "transfID", "engineID"));
    }

    @Test
    public void testConsumeHashBenchmark_Success() {
        when(cdaRepo.deleteItemBenchmark("hashToConsume")).thenReturn(true);
        boolean result = cdaSRV.consumeHashBenchmark("hashToConsume");
        assertTrue(result);
        verify(cdaRepo).deleteItemBenchmark("hashToConsume");
    }

    @Test
    public void testConsumeHashBenchmark_NullOrEmpty() {
        boolean result = cdaSRV.consumeHashBenchmark("");
        assertFalse(result);
        verify(cdaRepo, never()).deleteItemBenchmark(anyString());

        result = cdaSRV.consumeHashBenchmark(null);
        assertFalse(result);
        verify(cdaRepo, never()).deleteItemBenchmark(anyString());
    }

    @Test
    public void testConsumeHashBenchmark_Exception() {
        when(cdaRepo.deleteItemBenchmark("hashToConsume")).thenThrow(new RuntimeException("DB error"));
        assertThrows(BusinessException.class, () -> cdaSRV.consumeHashBenchmark("hashToConsume"));
    }
}

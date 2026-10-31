package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.security.auth.callback.Callback;

import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.mongodb.client.vault.ClientEncryption;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import org.bson.BsonBinary;
import org.bson.BsonString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.oauth2.CustomAuthenticateCallbackHandler;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.mongo.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.impl.AbstractCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.LoggerHelper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.AffinityDomainUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.PresentMetadataIndexer;

class StaticAnalysisRegressionTest {
    private static final String METADATA = "<RegistryObjectList><ExtrinsicObject>"
            + "<Slot name=\"creationTime\"><ValueList><Value>20261031120000</Value></ValueList></Slot>"
            + "</ExtrinsicObject></RegistryObjectList>";

    @ParameterizedTest
    @ValueSource(ints = {200, 399, 400, 499, 500, 599, 600})
    void handlesStandardAndNonStandardHttpStatusCodes(int status) throws IOException {
        var response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatusCode.valueOf(status));
        assertEquals(status >= 400 && status < 600, new RestTemplateResponseErrorHandler().hasError(response));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void publicationErrorsAllowMissingValidationContextAndException(boolean publication) {
        var handler = new ErrorHandlerSRV();
        var kafka = mock(IKafkaSRV.class);
        var cda = mock(ICdaSRV.class);
        ReflectionTestUtils.setField(handler, "kafkaSRV", kafka);
        ReflectionTestUtils.setField(handler, "cdaSRV", cda);
        ReflectionTestUtils.setField(handler, "logger", mock(LoggerHelper.class));
        var trace = new LogTraceInfoDTO("span", "trace");
        var failure = assertThrows(ValidationPublicationErrorException.class,
                () -> handler.publicationValidationExceptionHandler(new Date(), null, null, null,
                        trace, null, publication, "REF"));
        assertEquals(RestExecutionResultEnum.GENERIC_ERROR, failure.getResult());
        assertEquals("Validation error", failure.getMessage());
        if (publication) {
            verify(kafka).sendPublicationStatus("trace", null,
                    RestExecutionResultEnum.GENERIC_ERROR.getEventStatusEnum(), "Validation error", null, null, null);
        } else {
            verify(kafka).sendReplaceStatus("trace", null,
                    RestExecutionResultEnum.GENERIC_ERROR.getEventStatusEnum(), "Validation error", null, null, null);
        }
        verifyNoInteractions(cda);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingStatusResponseCanStillReturnIniAudits(boolean auditEnabled) {
        var service = new TransactionInspectSRV();
        var status = mock(IStatusCheckClient.class);
        var ini = mock(IIniClient.class);
        var config = mock(IConfigSRV.class);
        ReflectionTestUtils.setField(service, "statusCheckClient", status);
        ReflectionTestUtils.setField(service, "iniClient", ini);
        ReflectionTestUtils.setField(service, "configSRV", config);
        when(config.isAuditIniEnable()).thenReturn(auditEnabled);
        var event = new StatusCheckDTO();
        event.setEventDate("2026-10-31");
        var audits = new IniAuditsDto();
        audits.setAudit(List.of(event));
        when(ini.callSearchEventByWorkflowInstanceId("workflow")).thenReturn(audits);
        assertEquals(auditEnabled ? List.of(event) : List.of(),
                service.callSearchEventByWorkflowInstanceId("workflow").getTransactionData());
        when(status.callSearchEventByWorkflowInstanceId("workflow")).thenReturn(new TransactionInspectResDTO());
        audits.setAudit(null);
        assertTrue(service.callSearchEventByWorkflowInstanceId("workflow").getTransactionData().isEmpty());
    }

    @Test
    void fhirFileRejectsMissingOrEmptyUploadsAndReadsBytesOnlyOnce() throws IOException {
        var controller = new FileController();
        assertThrows(ValidationException.class, () -> controller.read(null));
        assertThrows(ValidationException.class, () -> controller.read(new MockMultipartFile("file", new byte[0])));
        var file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn("not a pdf".getBytes());
        assertNotNull(controller.read(file));
        verify(file).getBytes();
    }

    @Test
    void oauthInterruptionRestoresInterruptFlagAndPropagatesFailure() throws Exception {
        var handler = new CustomAuthenticateCallbackHandler();
        var client = mock(ConfidentialClientApplication.class);
        @SuppressWarnings("unchecked")
        CompletableFuture<IAuthenticationResult> future = mock(CompletableFuture.class);
        var interruption = new InterruptedException("interrupted");
        when(future.get()).thenThrow(interruption);
        when(client.acquireToken(nullable(ClientCredentialParameters.class))).thenReturn(future);
        ReflectionTestUtils.setField(handler, "aadClient", client);
        try {
            var failure = assertThrows(IOException.class,
                    () -> handler.handle(new Callback[] {new OAuthBearerTokenCallback()}));
            assertSame(interruption, failure.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void oauthAcquisitionFailureIsNotSilentlySwallowed() {
        var handler = new CustomAuthenticateCallbackHandler();
        var client = mock(ConfidentialClientApplication.class);
        var failure = new IllegalStateException("authentication failed");
        when(client.acquireToken(nullable(ClientCredentialParameters.class))).thenReturn(CompletableFuture.failedFuture(failure));
        ReflectionTestUtils.setField(handler, "aadClient", client);
        var thrown = assertThrows(IOException.class,
                () -> handler.handle(new Callback[] {new OAuthBearerTokenCallback()}));
        assertSame(failure, thrown.getCause().getCause());
    }

    @Test
    void encryptionRunsUnderTheClientLifecycleLock() {
        var config = new MongoDatabaseCFG();
        var properties = mock(MongoPropertiesCFG.class);
        when(properties.isEncryptionEnabled()).thenReturn(true);
        var encryption = mock(ClientEncryption.class);
        var encrypted = new BsonBinary(new byte[] {1});
        when(encryption.encrypt(any(), any())).thenAnswer(invocation -> {
            assertTrue(Thread.holdsLock(config));
            return encrypted;
        });
        ReflectionTestUtils.setField(config, "mongoPropsCfg", properties);
        ReflectionTestUtils.setField(config, "clientEncryption", encryption);
        assertSame(encrypted, config.encrypt(new BsonString("sensitive")));
    }

    @Test
    void normalMetadataStillParses() {
        assertEquals(LocalDate.of(2026, 10, 31), new AffinityDomainUtility().extractCreationTime(METADATA));
        assertTrue(new PresentMetadataIndexer().extractPresentFields(METADATA)
                .getDocumentEntryFields().contains("slot:creationTime"));
    }

    @Test
    void bothXmlParsersRejectExternalEntitiesAndExternalDtds(@TempDir Path directory) throws IOException {
        var entity = directory.resolve("entity.txt");
        Files.writeString(entity, "20261031120000");
        var dtd = directory.resolve("metadata.dtd");
        Files.writeString(dtd, "<!ELEMENT RegistryObjectList ANY>");
        List<String> declarations = List.of(
                "<!DOCTYPE RegistryObjectList [<!ENTITY xxe SYSTEM \"" + entity.toUri() + "\">]>",
                "<!DOCTYPE RegistryObjectList SYSTEM \"" + dtd.toUri() + "\">",
                "<!DOCTYPE RegistryObjectList [<!ENTITY % xxe SYSTEM \"" + dtd.toUri() + "\">%xxe;]>");
        for (String declaration : declarations) {
            String xml = declaration + METADATA.replace("20261031120000", "&xxe;");
            var dateFailure = assertThrows(IllegalArgumentException.class,
                    () -> new AffinityDomainUtility().extractCreationTime(xml));
            var fieldsFailure = assertThrows(IllegalArgumentException.class,
                    () -> new PresentMetadataIndexer().extractPresentFields(xml));
            assertTrue(dateFailure.getCause().getMessage().contains("DOCTYPE is disallowed"));
            assertTrue(fieldsFailure.getCause().getMessage().contains("DOCTYPE is disallowed"));
        }
    }

    private static class FileController extends AbstractCTL {
        DirectFhirDTO read(MultipartFile file) throws IOException {
            return getAndValidateFhirFile(file, null);
        }
    }
}

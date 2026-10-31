package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.bson.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.mongo.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ResourceDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.IniEdsInvocationETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IIniEdsInvocationRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.IniEdsInvocationSRV;

@Tag("ISOLATION")
class InvocationPersistenceIsolationTest {
    private IniEdsInvocationSRV service;
    private IIniEdsInvocationRepo repository;
    private MongoDatabaseCFG encryption;
    private MongoPropertiesCFG properties;
    private ResourceDTO resource;

    @BeforeEach void setup() {
        service = new IniEdsInvocationSRV();
        repository = mock(IIniEdsInvocationRepo.class);
        encryption = mock(MongoDatabaseCFG.class);
        properties = mock(MongoPropertiesCFG.class);
        ReflectionTestUtils.setField(service, "iniInvocationRepo", repository);
        ReflectionTestUtils.setField(service, "mongoCfg", encryption);
        ReflectionTestUtils.setField(service, "mongoPropsCfg", properties);
        resource = new ResourceDTO();
        resource.setBundleJson("{\"resourceType\":\"Bundle\"}");
        resource.setSubmissionSetEntryJson("{\"id\":\"submission\"}");
        resource.setDocumentEntryJson("{\"id\":\"document\"}");
        when(repository.insert(any())).thenAnswer(i -> {
            IniEdsInvocationETY entity = i.getArgument(0);
            entity.setId("saved-id");
            return entity;
        });
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void storesPublicationAndReplacementMetadataWithOptionalEncryption(boolean encrypted) {
        when(properties.isEncryptionEnabled()).thenReturn(encrypted);
        if (encrypted) when(encryption.encrypt(any(BsonString.class))).thenReturn(new BsonBinary(new byte[] {1, 2}));
        assertTrue(service.insert("workflow", resource, JwtServiceIsolationTest.payload().build()));
        assertTrue(service.replace("replacement", resource, JwtServiceIsolationTest.payload().build(), "ini-reference"));
        var saved = ArgumentCaptor.forClass(IniEdsInvocationETY.class);
        verify(repository, times(2)).insert(saved.capture());
        var publication = saved.getAllValues().get(0);
        var replacement = saved.getAllValues().get(1);
        assertEquals("workflow", publication.getWorkflowInstanceId());
        assertEquals("issuer", publication.getIssuer());
        assertEquals("RSSMRA22A01A399Z", publication.getFiscalCode());
        assertEquals("080", publication.getRde());
        assertEquals("Bundle", publication.getData().getString("resourceType"));
        assertNull(publication.getRiferimentoIni());
        assertEquals("ini-reference", replacement.getRiferimentoIni());
        assertEquals(3, publication.getMetadata().size());
        Object submission = publication.getMetadata().get(0).get("submissionSetEntry");
        if (encrypted) {
            assertInstanceOf(BsonBinary.class, submission);
            verify(encryption, times(6)).encrypt(any(BsonString.class));
        } else {
            assertEquals("submission", ((Document) submission).getString("id"));
            assertEquals("issuer", publication.getMetadata().get(2).get("tokenEntry", Document.class)
                    .get("payload", Document.class).getString("iss"));
            verifyNoInteractions(encryption);
        }
    }

    @Test void noBundleIsAllowedButMissingDatabaseIdMeansNotSaved() {
        resource.setBundleJson(null);
        doAnswer(i -> i.getArgument(0)).when(repository).insert(any());
        assertFalse(service.insert("workflow", resource, JwtServiceIsolationTest.payload().build()));
        verify(repository).insert(argThat(e -> e.getData() == null));
    }

    @Test void wrapsStorageFailuresForBothOperations() {
        var failure = new IllegalStateException("mongo offline");
        doThrow(failure).when(repository).insert(any());
        assertSame(failure, assertThrows(BusinessException.class,
                () -> service.insert("workflow", resource, JwtServiceIsolationTest.payload().build())).getCause());
        assertSame(failure, assertThrows(BusinessException.class,
                () -> service.replace("workflow", resource, JwtServiceIsolationTest.payload().build(), "reference")).getCause());
    }
}

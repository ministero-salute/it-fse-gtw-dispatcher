package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.FhirMappingClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.FlowsCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreationReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.TransformResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.FhirSRV;

@Tag("ISOLATION")
@ExtendWith(MockitoExtension.class)
class FhirResourcesIsolationTest {
    @Mock FhirMappingClient client;
    @Mock IConfigSRV config;
    @Mock FlowsCFG flows;
    @InjectMocks FhirSRV service;
    private final ObjectMapper json = new ObjectMapper();

    static final String CDA = """
        <ClinicalDocument><id root="1.2.3" extension="document"/>
        <code code="11502-2" displayName="Laboratory report" codeSystem="2.16.840.1.113883.6.1"/>
        <templateId root="2.16.840.1.113883.2.9.10.1.1"/><title>Clinical report</title>
        <effectiveTime value="20261030120000"/><confidentialityCode code="N"/>
        <recordTarget><patientRole><id extension="RSSMRA22A01A399Z" root="2.16.840.1.113883.2.9.4.3.2"/></patientRole></recordTarget>
        <author><assignedAuthor><id extension="AUTHOR" root="1.2.3"/></assignedAuthor></author>
        <inFulfillmentOf><order><id root="2.16.840.1.113883.2.9.4.3.9" extension="ORDER"/></order></inFulfillmentOf>
        </ClinicalDocument>
        """;
    static final String BUNDLE = """
        {"resourceType":"Bundle","type":"document","identifier":{"system":"urn:test","value":"document"},"entry":[
        {"resource":{"resourceType":"Patient","identifier":[{"value":"RSSMRA22A01A399Z","system":"urn:cf"}]}},
        {"resource":{"resourceType":"Practitioner","identifier":[{"value":"AUTHOR","system":"urn:author"}]}},
        {"resource":{"resourceType":"Composition","confidentiality":"N","date":"20261030120000","title":"Clinical report",
          "encounter":{"reference":"Encounter/1"},"type":{"coding":[{"code":"11502-2","display":"Laboratory report"}]}}}]}
        """;

    static PublicationCreationReqDTO request() {
        var r = new PublicationCreationReqDTO();
        r.setIdentificativoDoc("1.2.3^document");
        r.setIdentificativoRep("2.16.840.1.113883.2.9.2.080.4.5.1234");
        r.setIdentificativoSottomissione("submission");
        r.setTipologiaStruttura(HealthcareFacilityEnum.Ospedale);
        r.setAssettoOrganizzativo(PracticeSettingCodeEnum.AD_PSC001);
        r.setTipoDocumentoLivAlto(TipoDocAltoLivEnum.REF);
        r.setTipoAttivitaClinica(AttivitaClinicaEnum.CON);
        r.setAdministrativeRequest(List.of(AdministrativeReqEnum.values()[0]));
        r.setDescriptions(List.of("description"));
        r.setAttiCliniciRegoleAccesso(List.of(EventCodeEnum.values()[0].getCode()));
        r.setDataInizioPrestazione("20261030100000");
        r.setDataFinePrestazione("20261030110000");
        return r;
    }

    @Test
    void mapsClinicalMetadataAndMappingRequestFromCda() throws Exception {
        when(client.callConvertCdaInBundle(any())).thenReturn(new TransformResDTO(null, new Document("resourceType", "Bundle")));
        var result = service.createFhirResources(CDA, "APR", request(), 123, "sha256", "map", "engine", "080", "institution", "sha1");
        var entry = json.readTree(result.getDocumentEntryJson());
        assertEquals("RSSMRA22A01A399Z^^^&2.16.840.1.113883.2.9.4.3.2&ISO", entry.path("patientId").asText());
        assertEquals("11502-2", entry.path("typeCode").asText());
        assertEquals("Clinical report", entry.path("title").asText());
        assertEquals("sha1", entry.path("hash").asText());
        assertEquals(123, entry.path("size").asInt());
        assertEquals("ORDER^^^&2.16.840.1.113883.2.9.4.3.8&ISO^urn:ihe:iti:xds:2013:order", entry.path("referenceIdList").get(0).asText());
        var submission = json.readTree(result.getSubmissionSetEntryJson());
        assertEquals("2.16.840.1.113883.2.9.2.80", submission.path("sourceId").asText());
        assertEquals("submission", submission.path("uniqueID").asText());
        var captured = ArgumentCaptor.forClass(FhirResourceDTO.class);
        verify(client).callConvertCdaInBundle(captured.capture());
        assertEquals(CDA, captured.getValue().getCda());
        assertEquals("map", captured.getValue().getObjectId());
        assertEquals("engine", captured.getValue().getEngineId());
        assertEquals("sha256", captured.getValue().getDocumentReferenceDTO().getHash());
        assertNull(result.getErrorMessage());
    }

    @Test
    void mapsDirectFhirMetadataAndAddsDocumentReference() throws Exception {
        when(client.addDocumentReferenceToBundle(any())).thenReturn(new TransformResDTO(null, new Document("resourceType", "Bundle")));
        var result = service.createFhirResourcesFromBundle(BUNDLE, "APR", request(), 12, "hash", "080", "institution", "sha1");
        var entry = json.readTree(result.getDocumentEntryJson());
        assertEquals("RSSMRA22A01A399Z", entry.path("patientId").asText());
        assertEquals("Clinical report", entry.path("title").asText());
        assertEquals("AUTHOR^^^^^^^^&urn:author&ISO", entry.path("author").asText());
        assertEquals("11502-2", entry.path("typeCode").asText());
        assertEquals("Encounter/1", entry.path("referenceIdList").get(0).asText());
        verify(client).addDocumentReferenceToBundle(argThat(r -> BUNDLE.equals(r.getBundleJson())));
        verify(client, never()).callConvertCdaInBundle(any());
        assertTrue(result.getBundleJson().contains("Bundle"));
    }

    @Test
    void removingEdsStillCreatesIniMetadataWithoutMappingCall() {
        when(config.isRemoveEds()).thenReturn(true);
        var result = service.createFhirResources("<ClinicalDocument/>", "APR", request(), 1, "hash", "", "", "080", "institution", "sha1");
        assertNotNull(result.getDocumentEntryJson());
        assertNotNull(result.getSubmissionSetEntryJson());
        assertNull(result.getBundleJson());
        verifyNoInteractions(client);
    }

    @Test
    void invalidRepositoryIdentifierFailsBeforeExternalMapping() {
        var request = request();
        request.setIdentificativoRep("invalid");
        assertThrows(ValidationException.class, () -> service.createFhirResources(CDA, "APR", request, 1, "hash", "", "", "080", "institution", "sha1"));
        verifyNoInteractions(client);
    }

    @Test
    void convertsDocumentBundleButLeavesTransactionUntouched() throws Exception {
        when(client.callConvertDocumentInTransaction(any())).thenReturn(new TransformResDTO("diagnostic", new Document("type", "transaction")));
        var result = service.convertDocumentToTransaction(BUNDLE);
        assertEquals("diagnostic", result.getErrorMessage());
        assertTrue(result.getBundleJson().contains("transaction"));
        clearInvocations(client);
        String transaction = "{\"type\":\"transaction\"}";
        assertEquals(transaction, service.convertDocumentToTransaction(transaction).getBundleJson());
        verifyNoInteractions(client);
    }

    @Test
    void conversionFailurePropagatesAndEmptyResponseDoesNotInventBundle() throws Exception {
        assertNull(service.convertDocumentToTransaction(BUNDLE).getBundleJson());
        var failure = new IllegalStateException("mapper unavailable");
        when(client.callConvertDocumentInTransaction(any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.convertDocumentToTransaction(BUNDLE)));
    }
}

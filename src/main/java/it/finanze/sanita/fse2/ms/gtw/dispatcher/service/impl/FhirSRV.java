package it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.FhirMappingClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.AuthorSlotDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.DocumentEntryDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.DocumentReferenceDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.FhirResourceDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ResourceDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.SubmissionSetEntryDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreateReplaceMetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.TransformResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.AttivitaClinicaEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ConfidentialityCodeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DocumentMetadataKeyEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.LowLevelDocEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IFhirSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.DateUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.FhirUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.ValidationUtility;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FhirSRV implements IFhirSRV {

    private static final String SOURCE_ID_PREFIX = "2.16.840.1.113883.2.9.2.";
    private static final String PATH_PATIENT_ID = "ClinicalDocument > recordTarget > patientRole> id";
    private static final String EXTENSION_ATTRIBUTE = "extension";
    private static final String REFERENCE_ID_LIST_SUFFIX = "&ISO^urn:ihe:iti:xds:2013:order";

    @Autowired private FhirMappingClient client;
    @Autowired private IConfigSRV configSrv;

    @Override
    public ResourceDTO createFhirResources(final String cda, String authorRole, final PublicationCreateReplaceMetadataDTO requestBody,
            final Integer size, final String hash, String transformId, String engineId, String organizationId, final String authorInstitution, String sha1) {

        return createBaseResource(cda, authorRole, requestBody, size, hash, transformId, engineId, organizationId, authorInstitution, sha1, true);
    }

    @Override
    public ResourceDTO createFhirResourcesFromBundle(final String fhirBundleJson, String authorRole, final PublicationCreateReplaceMetadataDTO requestBody,
            final Integer size, final String hash, String organizationId, final String authorInstitution, String sha1) {

        return createBaseResource(fhirBundleJson, authorRole, requestBody, size, hash, "", "", organizationId, authorInstitution, sha1, false);
    }

    private ResourceDTO createBaseResource(final String inputData, String authorRole, final PublicationCreateReplaceMetadataDTO requestBody,
            final Integer size, final String hash, String transformId, String engineId, String organizationId, final String authorInstitution, String sha1, boolean isCdaInput) {

        final ResourceDTO output = new ResourceDTO();

        try {
            final String encodedInput = Base64.getEncoder().encodeToString(inputData.getBytes());
            final DocumentReferenceDTO docRef = buildDocumentReferenceDTO(encodedInput, requestBody, size, hash);
            final FhirResourceDTO fhirReq = buildFhirResourceDTO(docRef, inputData, transformId, engineId);

            AuthorSlotDTO authorSlot = buildAuthorSlot(authorInstitution, authorRole, inputData, isCdaInput);
            Map<DocumentMetadataKeyEnum, Object> metadata = extractMetadata(inputData, isCdaInput);

            if (isCdaInput) {
                metadata.put(DocumentMetadataKeyEnum.RAW_CDA, inputData);
            }

            DocumentEntryDTO docEntry = setDocumentEntryFields(requestBody, size, sha1, authorSlot, metadata);
            output.setDocumentEntryJson(StringUtility.toJSON(docEntry));

            SubmissionSetEntryDTO sse = buildSubmissionSetEntry(requestBody, authorSlot, metadata, organizationId, isCdaInput);
            output.setSubmissionSetEntryJson(StringUtility.toJSON(sse));

            if (isCdaInput && !configSrv.isRemoveEds()) {
                TransformResDTO resDTO = client.callConvertCdaInBundle(fhirReq);
                if (!StringUtility.isNullOrEmpty(resDTO.getErrorMessage())) {
                    output.setErrorMessage(resDTO.getErrorMessage());
                } else {
                    output.setBundleJson(StringUtility.toJSON(resDTO.getJson()));
                }
            } else if (!isCdaInput) {
                output.setBundleJson(inputData);
            }

        } catch (ValidationException e) {
        	throw e;
		} catch (Exception ex) {
            log.error("Errore durante la creazione risorse FHIR: {}", ex.getMessage(), ex);
            throw new BusinessException("Errore durante la creazione risorse FHIR", ex);
        }

        return output;
    }

    private DocumentReferenceDTO buildDocumentReferenceDTO(final String encodedInput,
            final PublicationCreateReplaceMetadataDTO requestBody,
            final Integer size, final String hash) {

        DocumentReferenceDTO documentReferenceDTO = new DocumentReferenceDTO();
        documentReferenceDTO.setEncodedCDA(encodedInput);
        documentReferenceDTO.setSize(size);
        documentReferenceDTO.setHash(hash);
        documentReferenceDTO.setFacilityTypeCode(requestBody.getTipologiaStruttura().getCode());

        if (!CollectionUtils.isEmpty(requestBody.getAttiCliniciRegoleAccesso())) {
            documentReferenceDTO.setEventCode(requestBody.getAttiCliniciRegoleAccesso());
        }

        documentReferenceDTO.setPracticeSettingCode(requestBody.getAssettoOrganizzativo().getDescription());
        documentReferenceDTO.setTipoDocumentoLivAlto(requestBody.getTipoDocumentoLivAlto().getCode());
        ValidationUtility.repositoryUniqueIdValidation(requestBody.getIdentificativoRep());
        documentReferenceDTO.setRepositoryUniqueID(requestBody.getIdentificativoRep());
        documentReferenceDTO.setServiceStartTime(requestBody.getDataInizioPrestazione());
        documentReferenceDTO.setServiceStopTime(requestBody.getDataFinePrestazione());
        documentReferenceDTO.setIdentificativoDoc(requestBody.getIdentificativoDoc());

        return documentReferenceDTO;
    }

    private FhirResourceDTO buildFhirResourceDTO(final DocumentReferenceDTO documentReferenceDTO, final String inputData, String transformId, String engineId) {
        FhirResourceDTO req = new FhirResourceDTO();
        req.setCda(inputData);
        req.setDocumentReferenceDTO(documentReferenceDTO);
        req.setObjectId(transformId);
        req.setEngineId(engineId);
        return req;
    }

    private AuthorSlotDTO buildAuthorSlot(String authorInstitution, String authorRole, String inputData, boolean isCda) {
        if (isCda) {
            org.jsoup.nodes.Document docCDA = Jsoup.parse(inputData);
            return buildAuthorSlotDTO(authorInstitution, authorRole, docCDA);
        } else {
            return buildAuthorSlotDTOFromFhir(authorInstitution, authorRole, inputData);
        }
    }

    private Map<DocumentMetadataKeyEnum, Object> extractMetadata(String inputData, boolean isCda) {
        if (isCda) {
            org.jsoup.nodes.Document docCDA = Jsoup.parse(inputData);
            return extractMetadataFromCda(docCDA);
        } else {
            return extractMetadataFromFhirBundle(inputData);
        }
    }

    private SubmissionSetEntryDTO buildSubmissionSetEntry(PublicationCreateReplaceMetadataDTO requestBody,
            AuthorSlotDTO authorSlotDTO, Map<DocumentMetadataKeyEnum, Object> metadata,
            String organizationId, boolean isCda) {

        if (isCda) {
            org.jsoup.nodes.Document docCDA = Jsoup.parse((String) metadata.get(DocumentMetadataKeyEnum.RAW_CDA));
            return createSubmissionSetEntry(docCDA,
                    requestBody.getTipoAttivitaClinica().getCode(),
                    requestBody.getIdentificativoSottomissione(),
                    authorSlotDTO, organizationId);
        } else {
            return createSubmissionSetEntryFromFhir(metadata, requestBody, organizationId, authorSlotDTO);
        }
    }

    private List<String> buildReferenceIdList(final org.jsoup.nodes.Document docCDA, final String path) {
        List<String> out = new ArrayList<>();
        Elements elements = docCDA.select(path);
        if (!elements.isEmpty()) {
            for (Element el : elements) {
                String nre = el.attr("root");
                if ("2.16.840.1.113883.2.9.4.3.9".equals(nre)) {
                    String extension = el.attr(EXTENSION_ATTRIBUTE);
                    out.add(extension + "^^^&2.16.840.1.113883.2.9.4.3.8" + REFERENCE_ID_LIST_SUFFIX);
                }
            }
        }
        return out;
    }

    private String buildPatient(final org.jsoup.nodes.Document docCDA) {
        final Element patientIdElement = docCDA.select(PATH_PATIENT_ID).first();
        if (patientIdElement != null) {
            String cf = patientIdElement.attr(EXTENSION_ATTRIBUTE);
            String root = patientIdElement.attr("root");
            return cf + "^^^&" + root + "&ISO";
        }
        return "";
    }

    private static AuthorSlotDTO buildAuthorSlotDTO(final String authorInstitution,
            final String authorRole, final org.jsoup.nodes.Document docCDA) {

        AuthorSlotDTO author = new AuthorSlotDTO();
        author.setAuthorRole(authorRole);
        author.setAuthorInstitution(authorInstitution);

        final Element authorElement = docCDA.select("ClinicalDocument > author > assignedAuthor > id").first();
        if (authorElement != null) {
            String cfAuthor = authorElement.attr(EXTENSION_ATTRIBUTE);
            String rootAuthor = authorElement.attr("root");
            author.setAuthor(cfAuthor + "^^^^^^^^&" + rootAuthor + "&ISO");
        }

        return author;
    }

    private static AuthorSlotDTO buildAuthorSlotDTOFromFhir(final String authorInstitution,
            final String authorRole, final String fhirBundleJson) {

        AuthorSlotDTO author = new AuthorSlotDTO();
        author.setAuthorRole(authorRole);
        author.setAuthorInstitution(authorInstitution);

        try {
            String cfAuthor = FhirUtility.extractValue(fhirBundleJson, "Practitioner", "identifier[0].value");
            String rootAuthor = FhirUtility.extractValue(fhirBundleJson, "Practitioner", "identifier[0].system");

            if (!StringUtility.isNullOrEmpty(cfAuthor) && !StringUtility.isNullOrEmpty(rootAuthor)) {
                author.setAuthor(cfAuthor + "^^^^^^^^&" + rootAuthor + "&ISO");
            }

        } catch (Exception ex) {
            log.warn("Autore non trovato nel FHIR Bundle, utilizzo solo authorRole e authorInstitution");
        }

        return author;
    }

    private Map<DocumentMetadataKeyEnum, Object> extractMetadataFromCda(final org.jsoup.nodes.Document docCDA) {
        Map<DocumentMetadataKeyEnum, Object> metadata = new EnumMap<>(DocumentMetadataKeyEnum.class);

        metadata.put(DocumentMetadataKeyEnum.PATIENT_ID, buildPatient(docCDA));

        Element confidentialityElement = docCDA.selectFirst("ClinicalDocument > confidentialityCode");
        if (confidentialityElement != null) {
            String code = confidentialityElement.attr("code");
            if (!StringUtility.isNullOrEmpty(code)) {
                metadata.put(DocumentMetadataKeyEnum.CONFIDENTIALITY_CODE, code);
                metadata.put(DocumentMetadataKeyEnum.CONFIDENTIALITY_DISPLAY, ConfidentialityCodeEnum.getDisplayByCode(code));
            }
        }

        Element typeCodeElement = docCDA.selectFirst("ClinicalDocument > code");
        if (typeCodeElement != null) {
            metadata.put(DocumentMetadataKeyEnum.TYPE_CODE, typeCodeElement.attr("code"));
            metadata.put(DocumentMetadataKeyEnum.TYPE_DISPLAY, typeCodeElement.attr("displayName"));
        }

        Element formatCodeElement = docCDA.selectFirst("ClinicalDocument > templateId");
        if (formatCodeElement != null) {
            String formatCode = formatCodeElement.attr("root");
            metadata.put(DocumentMetadataKeyEnum.FORMAT_CODE, formatCode);
            LowLevelDocEnum formatEnum = Arrays.stream(LowLevelDocEnum.values())
                    .filter(e -> e.getCode().equals(formatCode))
                    .findFirst()
                    .orElse(null);
            metadata.put(DocumentMetadataKeyEnum.FORMAT_DISPLAY,
                    formatEnum != null ? formatEnum.getDescription() : null);
        }

        String effectiveTime = docCDA.select("ClinicalDocument > effectiveTime").val();
        metadata.put(DocumentMetadataKeyEnum.CREATION_TIME, DateUtility.convertDateCda(effectiveTime));

        List<String> referenceIdList = buildReferenceIdList(docCDA, "ClinicalDocument > inFulfillmentOf > order > id");
        if (!referenceIdList.isEmpty()) {
            metadata.put(DocumentMetadataKeyEnum.REFERENCE_ID_LIST, referenceIdList);
        }

        Element titleElement = docCDA.selectFirst("ClinicalDocument > title");
        if (titleElement != null) {
            metadata.put(DocumentMetadataKeyEnum.TITLE, titleElement.text());
        }

        return metadata;
    }

    private Map<DocumentMetadataKeyEnum, Object> extractMetadataFromFhirBundle(final String bundleJson) {
        Map<DocumentMetadataKeyEnum, Object> metadata = new EnumMap<>(DocumentMetadataKeyEnum.class);

        if (StringUtility.isNullOrEmpty(bundleJson)) return metadata;

        try {
            String patientId = FhirUtility.extractValue(bundleJson, "Patient", "identifier[0].value");
            if (!StringUtility.isNullOrEmpty(patientId)) metadata.put(DocumentMetadataKeyEnum.PATIENT_ID, patientId);

            String confidentialityCode = FhirUtility.extractValue(bundleJson, "Composition", "confidentiality");
            if (!StringUtility.isNullOrEmpty(confidentialityCode)) {
                metadata.put(DocumentMetadataKeyEnum.CONFIDENTIALITY_CODE, confidentialityCode);
                metadata.put(DocumentMetadataKeyEnum.CONFIDENTIALITY_DISPLAY,
                        ConfidentialityCodeEnum.getDisplayByCode(confidentialityCode));
            }

            String typeCode = FhirUtility.extractValue(bundleJson, "Composition", "type.coding[0].code");
            String typeDisplay = FhirUtility.extractValue(bundleJson, "Composition", "type.coding[0].display");
            if (!StringUtility.isNullOrEmpty(typeCode)) {
                metadata.put(DocumentMetadataKeyEnum.TYPE_CODE, typeCode);
                metadata.put(DocumentMetadataKeyEnum.TYPE_DISPLAY, typeDisplay);
            }

//            String formatCode = FhirUtility.extractValue(bundleJson, "DocumentReference", "format[0].code");
//            if (!StringUtility.isNullOrEmpty(formatCode)) {
//                metadata.put(DocumentMetadataKeyEnum.FORMAT_CODE, formatCode);
//                LowLevelDocEnum formatEnum = Arrays.stream(LowLevelDocEnum.values())
//                        .filter(e -> e.getCode().equals(formatCode))
//                        .findFirst()
//                        .orElse(null);
//                metadata.put(DocumentMetadataKeyEnum.FORMAT_DISPLAY,
//                        formatEnum != null ? formatEnum.getDescription() : null);
//            }

            String creationTime = FhirUtility.extractValue(bundleJson, "Composition", "date");
            if (!StringUtility.isNullOrEmpty(creationTime)) {
                metadata.put(DocumentMetadataKeyEnum.CREATION_TIME, DateUtility.convertDateCda(creationTime));
            }

            List<String> referenceIds = FhirUtility.extractValues(bundleJson, "Composition", "encounter.reference");
            if (!referenceIds.isEmpty()) {
                metadata.put(DocumentMetadataKeyEnum.REFERENCE_ID_LIST, referenceIds);
            }

            String title = FhirUtility.extractValue(bundleJson, "Composition", "title");
            if (!StringUtility.isNullOrEmpty(title)) {
                metadata.put(DocumentMetadataKeyEnum.TITLE, title);
            }

        } catch (Exception e) {
            log.error("Errore durante l'estrazione dei metadati dal Bundle FHIR: {}", e.getMessage(), e);
            throw new BusinessException("Errore durante l'estrazione dei metadati FHIR", e);
        }

        return metadata;
    }

    @SuppressWarnings("unchecked")
    private DocumentEntryDTO setDocumentEntryFields(final PublicationCreateReplaceMetadataDTO requestBody,
            final Integer size, final String sha1, final AuthorSlotDTO authorSlotDTO,
            final Map<DocumentMetadataKeyEnum, Object> metadata) {

        DocumentEntryDTO de = new DocumentEntryDTO();

        de.setPatientId((String) metadata.get(DocumentMetadataKeyEnum.PATIENT_ID));
        de.setConfidentialityCode((String) metadata.get(DocumentMetadataKeyEnum.CONFIDENTIALITY_CODE));
        de.setConfidentialityCodeDisplayName((String) metadata.get(DocumentMetadataKeyEnum.CONFIDENTIALITY_DISPLAY));
        de.setTypeCode((String) metadata.get(DocumentMetadataKeyEnum.TYPE_CODE));
        de.setTypeCodeName((String) metadata.get(DocumentMetadataKeyEnum.TYPE_DISPLAY));
        de.setFormatCode((String) metadata.get(DocumentMetadataKeyEnum.FORMAT_CODE));
        de.setFormatCodeName((String) metadata.get(DocumentMetadataKeyEnum.FORMAT_DISPLAY));
        de.setCreationTime((String) metadata.get(DocumentMetadataKeyEnum.CREATION_TIME));
        de.setReferenceIdList((List<String>) metadata.get(DocumentMetadataKeyEnum.REFERENCE_ID_LIST));
        de.setTitle((String) metadata.get(DocumentMetadataKeyEnum.TITLE));

        de.setConservazioneANorma(requestBody.getConservazioneANorma());
        de.setUniqueId(requestBody.getIdentificativoDoc());
        de.setMimeType("application/pdf+text/x-cda-r2+xml");
        de.setHash(sha1);
        de.setSize(size);

        if (requestBody.getAdministrativeRequest() != null) {
            List<String> administrativeRequestList = requestBody.getAdministrativeRequest().stream()
                    .map(en -> en.getCode() + "^" + en.getDescription())
                    .collect(Collectors.toList());
            de.setAdministrativeRequest(administrativeRequestList);
        }

        de.setAuthorRole(authorSlotDTO.getAuthorRole());
        de.setAuthorInstitution(authorSlotDTO.getAuthorInstitution());
        de.setAuthor(authorSlotDTO.getAuthor());

        ValidationUtility.repositoryUniqueIdValidation(requestBody.getIdentificativoRep());
        de.setRepositoryUniqueId(requestBody.getIdentificativoRep());

        if (requestBody.getDescriptions() != null) {
            de.setDescription(requestBody.getDescriptions());
        }

        de.setHealthcareFacilityTypeCode(requestBody.getTipologiaStruttura().getCode());
        de.setHealthcareFacilityTypeCodeName(requestBody.getTipologiaStruttura().getCode());

        if (!CollectionUtils.isEmpty(requestBody.getAttiCliniciRegoleAccesso())) {
            de.setEventCodeList(requestBody.getAttiCliniciRegoleAccesso());
        }

        de.setClassCode(requestBody.getTipoDocumentoLivAlto().getCode());
        de.setClassCodeName(requestBody.getTipoDocumentoLivAlto().getDescription());

        de.setPracticeSettingCode(requestBody.getAssettoOrganizzativo().name());
        de.setPracticeSettingCodeName(requestBody.getAssettoOrganizzativo().getDescription());

        if (requestBody.getDataInizioPrestazione() != null &&
                DateUtility.isValidDateFormat(requestBody.getDataInizioPrestazione(), "yyyyMMddHHmmss")) {
            de.setServiceStartTime(requestBody.getDataInizioPrestazione());
        }

        if (requestBody.getDataFinePrestazione() != null &&
                DateUtility.isValidDateFormat(requestBody.getDataFinePrestazione(), "yyyyMMddHHmmss")) {
            de.setServiceStopTime(requestBody.getDataFinePrestazione());
        }

        return de;
    }

    private SubmissionSetEntryDTO createSubmissionSetEntry(final org.jsoup.nodes.Document docCDA,
            final String contentTypeCode,
            final String identificativoSottomissione,
            AuthorSlotDTO authorSlotDTO, String organizationId) {

        SubmissionSetEntryDTO sse = new SubmissionSetEntryDTO();
        sse.setAuthor(authorSlotDTO.getAuthor());
        sse.setAuthorInstitution(authorSlotDTO.getAuthorInstitution());
        sse.setAuthorRole(authorSlotDTO.getAuthorRole());
        sse.setPatientId(buildPatient(docCDA));

        String sourceId = StringUtility.sanitizeSourceId(organizationId);
        sse.setSourceId(SOURCE_ID_PREFIX + sourceId);
        sse.setUniqueID(identificativoSottomissione);

        sse.setSubmissionTime(new SimpleDateFormat(Constants.Misc.INI_DATE_PATTERN).format(new Date()));
        sse.setContentTypeCode(contentTypeCode);
        final AttivitaClinicaEnum contentTypeCodeName = Arrays.stream(AttivitaClinicaEnum.values())
                .filter(e -> e.getCode().equals(contentTypeCode))
                .findFirst().orElse(null);
        sse.setContentTypeCodeName(contentTypeCodeName != null ? contentTypeCodeName.getDescription() : null);
        return sse;
    }

    private SubmissionSetEntryDTO createSubmissionSetEntryFromFhir(Map<DocumentMetadataKeyEnum, Object> metadata,
            PublicationCreateReplaceMetadataDTO requestBody, String organizationId, AuthorSlotDTO authorSlotDTO) {

        SubmissionSetEntryDTO sse = new SubmissionSetEntryDTO();
        sse.setAuthor(authorSlotDTO.getAuthor());
        sse.setAuthorInstitution(authorSlotDTO.getAuthorInstitution());
        sse.setAuthorRole(authorSlotDTO.getAuthorRole());
        sse.setPatientId((String) metadata.get(DocumentMetadataKeyEnum.PATIENT_ID));

        String sourceId = StringUtility.sanitizeSourceId(organizationId);
        sse.setSourceId(SOURCE_ID_PREFIX + sourceId);
        sse.setUniqueID(requestBody.getIdentificativoSottomissione());
        sse.setSubmissionTime(new SimpleDateFormat(Constants.Misc.INI_DATE_PATTERN).format(new Date()));
        sse.setContentTypeCode(requestBody.getTipoAttivitaClinica().getCode());
        sse.setContentTypeCodeName(requestBody.getTipoAttivitaClinica().getDescription());
        return sse;
    }
}
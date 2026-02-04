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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad263;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AbstractAffinityDomainStrategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad21.enums.AttivitaClinicaAd21Enum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad21.enums.HealthcareFacilityAd21Enum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad21.enums.PracticeSettingCodeAd21Enum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad21.enums.TipoDocAltoLivAd21Enum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad24.enums.AdministrativeReqAd24Enum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.MetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Affinity Domain Strategy for version 2.6.3 (effective from March 2026)
 * Based on IHE ITI specifications for metadata update (ITI-57)
 * Includes validation logic specific to this AD version
 * 
 * <p>Note: The attributes on the ExtrinsicObject/RegistryPackage elements themselves
 * (like mimeType, id, objectType) are structural requirements of the ebXML
 * format rather than AD-specific metadata fields. These are validated by:
 * <ul>
 *   <li>The XML schema validation (XSD)</li>
 *   <li>The ITI-57 transaction itself</li>
 *   <li>The INI service that processes the metadata</li>
 * </ul>
 * 
 * @see AbstractAffinityDomainStrategy
 */
@Slf4j
@Component
public class Ad263Strategy extends AbstractAffinityDomainStrategy {

    // Version metadata
    private static final String VERSION_ID = "2.6.3";
    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2026, 3, 1);

    // Mandatory field definitions - immutable sets
    private static final Set<String> MANDATORY_DOCUMENT_ENTRY_FIELDS = Set.of(
            // Slots
            "slot:creationTime",
            "slot:repositoryUniqueId", // XDSDocumentEntry.repositoryUniqueId
            "slot:sourcePatientId", // XDSDocumentEntry.sourcePatientId
            "slot:urn:ihe:iti:xds:2024:SubjectApplication", // XDSDocumentEntry.SubjectApplication (New in 2.6.3)
            "slot:urn:ita:2022:documentSigned", // XDSDocumentEntry.documentSigned
            "slot:urn:ita:2022:administrativeRequest", // XDSDocumentEntry.administrativeRequest
            // Classifications (by scheme URN)
            "classification:urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d", // XDSDocumentEntry.author
            "classification:urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d:slot:authorInstitution", // XDSDocumentEntry.author.authorInstitution
            "classification:urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d:slot:authorPerson", // XDSDocumentEntry.author.authorPerson
            "classification:urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a", // XDSDocumentEntry.classCode
            "classification:urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a:slot:codingScheme", // XDSDocumentEntry.classCode.value
            "classification:urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f", // XDSDocumentEntry.confidentialityCode
            "classification:urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f:slot:codingScheme", // XDSDocumentEntry.confidentialityCode.value
            "classification:urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d", // XDSDocumentEntry.formatCode
            "classification:urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d:slot:codingScheme", // XDSDocumentEntry.formatCode.value
            "classification:urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1", // XDSDocumentEntry.healthcareFacilityTypeCode
            "classification:urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1:slot:codingScheme", // XDSDocumentEntry.healthcareFacilityTypeCode.value
            "classification:urn:uuid:cccf5598-8b07-4b77-a05e-ae952c785ead", // XDSDocumentEntry.practiceSettingCode
            "classification:urn:uuid:cccf5598-8b07-4b77-a05e-ae952c785ead:slot:codingScheme", // XDSDocumentEntry.practiceSettingCode.value
            "classification:urn:uuid:f0306f51-975f-434e-a61c-c59651d33983", // XDSDocumentEntry.typeCode
            "classification:urn:uuid:f0306f51-975f-434e-a61c-c59651d33983:slot:codingScheme", // XDSDocumentEntry.typeCode.value
            // ExternalIdentifiers (by scheme URN)
            "externalId:urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427", // XDSDocumentEntry.patientId
            "externalId:urn:uuid:2e82c1f6-a085-4c72-9da3-8640a32e42ab" // XDSDocumentEntry.uniqueId
    );

    private static final Set<String> MANDATORY_SUBMISSION_SET_FIELDS = Set.of(
            // Slots
            "slot:submissionTime",
            // Classifications (by scheme URN)
            "classification:urn:uuid:aa543740-bdda-424e-8c96-df4873be8500", // XDSSubmissionSet.contentTypeCode
            "classification:urn:uuid:a7058bb9-b4e4-4307-ba5b-e3f0ab85e12d", // XDSSubmissionSet.author
            "classification:urn:uuid:a7058bb9-b4e4-4307-ba5b-e3f0ab85e12d:slot:authorInstitution", // XDSSubmissionSet.author.authorInstitution
            "classification:urn:uuid:a7058bb9-b4e4-4307-ba5b-e3f0ab85e12d:slot:authorPerson", // XDSSubmissionSet.author.authorPerson
            // ExternalIdentifiers (by scheme URN)
            "externalId:urn:uuid:554ac39e-e3fe-47fe-b233-965d2a147832", // XDSSubmissionSet.sourceId
            "externalId:urn:uuid:96fdda7c-d067-4183-912e-bf5ee74998a8", // XDSSubmissionSet.uniqueId
            "externalId:urn:uuid:6b5aea1a-874d-4603-a4bc-96a0a7b38446" // XDSSubmissionSet.patientId
    );

    @Override
    public String versionId() {
        return VERSION_ID;
    }

    @Override
    public LocalDate effectiveFrom() {
        return EFFECTIVE_FROM;
    }

    @Override
    public ValidationResultDTO validateMandatoryMetadataIti57Request(MetadataDTO metadata) {
        return validateMandatoryFieldsTemplate(
                metadata,
                MANDATORY_DOCUMENT_ENTRY_FIELDS,
                MANDATORY_SUBMISSION_SET_FIELDS
        );
    }

    @Override
    public ValidationResultDTO validateUpdateMetadataReqDTO(UpdateMetadataReqDTO request) {
        return validateUpdateMetadataReqDTOTemplate(request);
    }

    @Override
    protected void validateValueSetsInternal(UpdateMetadataReqDTO request, List<String> validationErrors) {

        validateFieldList(request.getAdministrativeRequest(), AdministrativeReqAd24Enum::isValidCode,
                "administrativeRequest", "AdministrativeRequest", validationErrors);

        validateField(request.getTipologiaStruttura(), HealthcareFacilityAd21Enum::isValidCode,
                "tipologiaStruttura", "HealthcareFacility", validationErrors);

        validateField(request.getAssettoOrganizzativo(),
                PracticeSettingCodeAd21Enum::isValidCode,
                "assettoOrganizzativo", "PracticeSettingCode", validationErrors);

        validateField(request.getTipoAttivitaClinica(),
                AttivitaClinicaAd21Enum::isValidCode,
                "tipoAttivitaClinica", "AttivitaClinica", validationErrors);

        validateField(request.getTipoDocumentoLivAlto(),
                TipoDocAltoLivAd21Enum::isValidCode,
                "tipoDocumentoLivAlto", "TipoDocAltoLiv", validationErrors);
    }

}
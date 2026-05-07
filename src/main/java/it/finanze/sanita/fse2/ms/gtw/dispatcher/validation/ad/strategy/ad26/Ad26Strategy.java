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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad26;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AbstractAffinityDomainStrategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad26.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.MetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Affinity Domain Strategy for version 2.6 (effective from June 2025)
 * Based on IHE ITI specifications for metadata update (ITI-57)
 *
 * @see AbstractAffinityDomainStrategy
 */
@Slf4j
@Component
public class Ad26Strategy extends AbstractAffinityDomainStrategy {

    // Version metadata
    private static final String VERSION_ID = "2.6";
    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2025, 2, 27);

    // Mandatory field definitions - immutable sets
    private static final Set<String> MANDATORY_DOCUMENT_ENTRY_FIELDS = Set.of(
            // Slots
            "slot:repositoryUniqueId", // XDSDocumentEntry.repositoryUniqueId
            "slot:sourcePatientId", // XDSDocumentEntry.sourcePatientId
            "slot:urn:ita:2022:documentSigned", // XDSDocumentEntry.documentSigned
            "slot:urn:ita:2022:administrativeRequest", // XDSDocumentEntry.administrativeRequest
            // Classifications (by scheme URN)
            "classification:urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d", // XDSDocumentEntry.author
            "classification:urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d:slot:authorInstitution", // XDSDocumentEntry.author.authorInstitution
            "classification:urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d:slot:authorPerson", // XDSDocumentEntry.author.authorPerson
            "classification:urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a", // XDSDocumentEntry.classCode
            "classification:urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f", // XDSDocumentEntry.confidentialityCode
            "classification:urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d", // XDSDocumentEntry.formatCode
            "classification:urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1", // XDSDocumentEntry.healthcareFacilityTypeCode (NEW in 2.6)
            "classification:urn:uuid:f0306f51-975f-434e-a61c-c59651d33983", // XDSDocumentEntry.typeCode
            // ExternalIdentifiers (by scheme URN)
            "externalId:urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427", // XDSDocumentEntry.patientId
            "externalId:urn:uuid:2e82c1f6-a085-4c72-9da3-8640a32e42ab" // XDSDocumentEntry.uniqueId
    );

    private static final Set<String> MANDATORY_SUBMISSION_SET_FIELDS = Set.of(
            // Slots
            "slot:submissionTime", // XDSSubmissionSet.submissionTime (NEW in 2.6)
            // Classifications (by scheme URN)
            "classification:urn:uuid:a7058bb9-b4e4-4307-ba5b-e3f0ab85e12d", // XDSSubmissionSet.author
            // ExternalIdentifiers (by scheme URN)
            "externalId:urn:uuid:554ac39e-e3fe-47fe-b233-965d2a147832", // XDSSubmissionSet.sourceId
            "externalId:urn:uuid:96fdda7c-d067-4183-912e-bf5ee74998a8" // XDSSubmissionSet.uniqueId
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
    public ValidationResultDTO validateUpdateMetadataReqDTO(UpdateMetadataReqDTO request,
                    JWTPayloadDTO jwtPayloadToken) {
        return validateUpdateMetadataReqDTOTemplate(request);
    }

    @Override
    protected void validateValueSetsInternal(UpdateMetadataReqDTO request, List<String> validationErrors) {

        validateFieldList(request.getAdministrativeRequest(), AdministrativeReqAd26Enum::isValidCode,
                "administrativeRequest", "XDSDocumentEntry.Slot – administrativeRequest", validationErrors);

        validateField(request.getTipologiaStruttura(), HealthcareFacilityAd26Enum::isValidCode,
                "tipologiaStruttura", "XDSDocumentEntry.healthcareFacilityTypeCode", validationErrors);

        validateField(request.getAssettoOrganizzativo(),
                PracticeSettingCodeAd26Enum::isValidCode,
                "assettoOrganizzativo", "XDSDocumentEntry.practiceSettingCode", validationErrors);

        validateField(request.getTipoAttivitaClinica(),
                AttivitaClinicaAd26Enum::isValidCode,
                "tipoAttivitaClinica", "XDSSubmissionSet.contentTypeCode", validationErrors);

        validateField(request.getTipoDocumentoLivAlto(),
                TipoDocAltoLivAd26Enum::isValidCode,
                "tipoDocumentoLivAlto", "XDSDocumentEntry.classCode", validationErrors);
    }
}

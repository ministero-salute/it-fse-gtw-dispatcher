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

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AbstractAffinityDomainStrategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad263.enums.*;
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

    @Override
    public String versionId() {
        return VERSION_ID;
    }

    @Override
    public LocalDate effectiveFrom() {
        return EFFECTIVE_FROM;
    }

    @Override
    public ValidationResultDTO validateUpdateMetadataReqDTO(UpdateMetadataReqDTO request,
                    JWTPayloadDTO jwtPayloadToken) {
                CorrelationDocumentType263Validator.isValid(
                        DocumentType263Enum.getByCode(
                                StringUtility.extractHl7TypeCode(jwtPayloadToken.getResource_hl7_type())),
                        TipoDocAltoLivAd263Enum.valueOf(request.getTipoDocumentoLivAlto()));

            return validateUpdateMetadataReqDTOTemplate(request);
    }

    @Override
    protected void validateValueSetsInternal(UpdateMetadataReqDTO request, List<String> validationErrors) {

        validateFieldList(request.getAdministrativeRequest(), AdministrativeReqAd263Enum::isValidCode,
                "administrativeRequest", "XDSDocumentEntry.Slot – administrativeRequest", validationErrors);

        validateField(request.getTipologiaStruttura(), HealthcareFacilityAd263Enum::isValidCode,
                "tipologiaStruttura", "XDSDocumentEntry.healthcareFacilityTypeCod", validationErrors);

        validateField(request.getAssettoOrganizzativo(),
                PracticeSettingCodeAd263Enum::isValidCode,
                "assettoOrganizzativo", "XDSDocumentEntry.practiceSettingCode", validationErrors);

        validateField(request.getTipoAttivitaClinica(),
                AttivitaClinicaAd263Enum::isValidCode,
                "tipoAttivitaClinica", "XDSSubmissionSet.contentTypeCode", validationErrors);

        validateField(request.getTipoDocumentoLivAlto(),
                TipoDocAltoLivAd263Enum::isValidCode,
                "tipoDocumentoLivAlto", "XDSDocumentEntry.classCode", validationErrors);
    }

}
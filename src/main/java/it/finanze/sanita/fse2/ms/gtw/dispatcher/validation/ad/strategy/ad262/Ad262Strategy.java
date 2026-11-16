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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad262;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.PregressoDocumentReferenceDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationPregressoReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.MetadataValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AbstractAffinityDomainStrategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad262.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.MetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Affinity Domain Strategy for version 2.6.2 (effective from December 2025)
 * Based on IHE ITI specifications for metadata update (ITI-57)
 * 
 * @see AbstractAffinityDomainStrategy
 */
@Slf4j
@Component
public class Ad262Strategy extends AbstractAffinityDomainStrategy {

    // Version metadata
    private static final String VERSION_ID = "2.6.2";
    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2025, 12, 1);

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
        return validateUpdateMetadataReqDTOTemplate(request);
    }

    @Override
    protected void validateValueSetsInternal(UpdateMetadataReqDTO request, List<String> validationErrors) {

        validateFieldList(request.getAdministrativeRequest(), AdministrativeReqAd262Enum::isValidCode,
                "administrativeRequest", "XDSDocumentEntry.Slot - administrativeRequest", validationErrors);

        validateField(request.getTipologiaStruttura(), HealthcareFacilityAd262Enum::isValidCode,
                "tipologiaStruttura", "XDSDocumentEntry.healthcareFacilityTypeCode", validationErrors);

        validateField(request.getAssettoOrganizzativo(),
                PracticeSettingCodeAd262Enum::isValidCode,
                "assettoOrganizzativo", "XDSDocumentEntry.practiceSettingCode", validationErrors);

        validateField(request.getTipoAttivitaClinica(),
                AttivitaClinicaAd262Enum::isValidCode,
                "tipoAttivitaClinica", "XDSSubmissionSet.contentTypeCode", validationErrors);

        validateField(request.getTipoDocumentoLivAlto(),
                TipoDocAltoLivAd262Enum::isValidCode,
                "tipoDocumentoLivAlto", "XDSDocumentEntry.classCode", validationErrors);
    }

    @Override
    public PregressoDocumentReferenceDTO validatePublicationPregressoReqDTO(PublicationPregressoReqDTO request,
            JWTPayloadDTO jwtPayloadToken) {
        ValidationResultDTO result = validatePublicationPregressoDTOTemplate(request);

        if (!result.isValid()) {
            String detail = result.getErrorMessage() != null ? result.getErrorMessage() : "Invalid value in request";
            final ErrorResponseDTO error = ErrorResponseDTO.builder()
                    .type(RestExecutionResultEnum.VALIDATOR_ERROR.getType())
                    .title(RestExecutionResultEnum.VALIDATOR_ERROR.getTitle())
                    .instance(ErrorInstanceEnum.AD_MISSING_MANDATORY_FIELD.getInstance())
                    .detail(detail)
                    .build();
            throw new MetadataValidationException(error);
        }

        return buildPregressoDocumentReference(request);
    }

    /**
     * Validates the pregresso request: mandatory fields and match of the Affinity Domain codes
     * against the enums of this AD version.
     */
    @Override
    protected void validateValueSetsInternal(PublicationPregressoReqDTO request, List<String> validationErrors) {
        validatePregressoMandatoryFields(request, validationErrors);

        validateFieldList(request.getAdministrativeRequest(), AdministrativeReqAd262Enum::isValidCode,
                "administrativeRequest", "XDSDocumentEntry.Slot - administrativeRequest", validationErrors);

        validateField(request.getTipologiaStruttura(), HealthcareFacilityAd262Enum::isValidCode,
                "tipologiaStruttura", "XDSDocumentEntry.healthcareFacilityTypeCode", validationErrors);

        validateField(request.getAssettoOrganizzativo(),
                PracticeSettingCodeAd262Enum::isValidCode,
                "assettoOrganizzativo", "XDSDocumentEntry.practiceSettingCode", validationErrors);

        validateField(request.getTipoAttivitaClinica(),
                AttivitaClinicaAd262Enum::isValidCode,
                "tipoAttivitaClinica", "XDSSubmissionSet.contentTypeCode", validationErrors);

        validateField(request.getTipoDocumentoLivAlto(),
                TipoDocAltoLivAd262Enum::isValidCode,
                "tipoDocumentoLivAlto", "XDSDocumentEntry.classCode", validationErrors);
    }

    private PregressoDocumentReferenceDTO buildPregressoDocumentReference(PublicationPregressoReqDTO request) {
        PregressoDocumentReferenceDTO doc = new PregressoDocumentReferenceDTO();

        String tipologiaStrutturaCode = HealthcareFacilityAd262Enum.valueOf(request.getTipologiaStruttura()).getCode();
        String assettoOrganizzativoDescription = PracticeSettingCodeAd262Enum.valueOf(request.getAssettoOrganizzativo()).getDescription();
        String tipoDocumentoLivAltoCode = TipoDocAltoLivAd262Enum.valueOf(request.getTipoDocumentoLivAlto()).getCode();

        doc.setTipologiaStrutturaCode(tipologiaStrutturaCode);
        doc.setAttiCliniciRegoleAccesso(request.getAttiCliniciRegoleAccesso());
        doc.setAssettoOrganizzativoDescription(assettoOrganizzativoDescription);
        doc.setTipoDocumentoLivAltoCode(tipoDocumentoLivAltoCode);
        doc.setIdentificativoDoc(request.getIdentificativoDoc());
        doc.setIdentificativoRep(request.getIdentificativoRep());
        doc.setDataInizioPrestazione(request.getDataInizioPrestazione());
        doc.setDataFinePrestazione(request.getDataFinePrestazione());
        if (request.getAdministrativeRequest() != null && !request.getAdministrativeRequest().isEmpty()) {
            doc.setAdministrativeRequest(request.getAdministrativeRequest().stream()
                    .map(e -> AdministrativeReqAd262Enum.valueOf(e).name())
                    .collect(Collectors.toList()));
        }

        return doc;
    }

}

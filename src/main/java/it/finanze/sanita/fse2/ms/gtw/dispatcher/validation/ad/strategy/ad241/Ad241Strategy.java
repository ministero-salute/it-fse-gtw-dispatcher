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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad241;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AbstractAffinityDomainStrategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad241.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.MetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Affinity Domain Strategy for version 2.4.1 (effective from March 2023)
 * Based on IHE ITI specifications for metadata update (ITI-57)
 * Adds administrativeRequest slot requirement compared to v2.4
 *
 * @see AbstractAffinityDomainStrategy
 */
@Slf4j
@Component
public class Ad241Strategy extends AbstractAffinityDomainStrategy {

    // Version metadata
    private static final String VERSION_ID = "2.4.1";
    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2023, 2, 21);

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

        validateFieldList(request.getAdministrativeRequest(), AdministrativeReqAd241Enum::isValidCode,
                "administrativeRequest", "XDSDocumentEntry.Slot – administrativeRequest", validationErrors);

        validateField(request.getTipologiaStruttura(), HealthcareFacilityAd241Enum::isValidCode,
                "tipologiaStruttura", "XDSDocumentEntry.healthcareFacilityTypeCode", validationErrors);

        validateField(request.getAssettoOrganizzativo(),
                PracticeSettingCodeAd241Enum::isValidCode,
                "assettoOrganizzativo", "XDSDocumentEntry.practiceSettingCode", validationErrors);

        validateField(request.getTipoAttivitaClinica(),
                AttivitaClinicaAd241Enum::isValidCode,
                "tipoAttivitaClinica", "XDSSubmissionSet.contentTypeCode", validationErrors);

        validateField(request.getTipoDocumentoLivAlto(),
                TipoDocAltoLivAd241Enum::isValidCode,
                "tipoDocumentoLivAlto", "XDSDocumentEntry.classCode", validationErrors);
    }

}

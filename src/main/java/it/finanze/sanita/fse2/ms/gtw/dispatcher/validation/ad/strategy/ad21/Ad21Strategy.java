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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad21;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AbstractAffinityDomainStrategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad21.enums.AttivitaClinicaAd21Enum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad21.enums.HealthcareFacilityAd21Enum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad21.enums.PracticeSettingCodeAd21Enum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad21.enums.TipoDocAltoLivAd21Enum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.MetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Affinity Domain Strategy for version 2.1 (effective from November 2017).
 * Based on IHE ITI specifications for metadata update (ITI-57).
 * This is the base version with minimal mandatory fields.
 *
 * <p>
 * This strategy validates:
 * <ul>
 * <li>Mandatory DocumentEntry fields according to XDS specifications</li>
 * <li>Mandatory SubmissionSet fields according to XDS specifications</li>
 * <li>Value set constraints for metadata update requests</li>
 * </ul>
 *
 * @see AbstractAffinityDomainStrategy
 * @since 2.1
 */
@Slf4j
@Component
public class Ad21Strategy extends AbstractAffinityDomainStrategy {

        // Version metadata
        private static final String VERSION_ID = "2.1";
        private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2017, 11, 1);

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

        /**
         * Validates all value sets in the request against AD 2.1-specific enums.
         * This method is called by the template method in the abstract class.
         */
        @Override
        protected void validateValueSetsInternal(UpdateMetadataReqDTO request, List<String> validationErrors) {

                validateUnsupportedField(request.getAdministrativeRequest(), "AdministrativeRequest", validationErrors);

                validateField(request.getTipologiaStruttura(),
                                HealthcareFacilityAd21Enum::isValidCode,
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
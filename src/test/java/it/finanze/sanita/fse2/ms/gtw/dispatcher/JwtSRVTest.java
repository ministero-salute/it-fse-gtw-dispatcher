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
package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.JwtCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ActionEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.PurposeOfUseEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RoleEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.SystemTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.JwtSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.UtilitySRV;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
public class JwtSRVTest {

    @Autowired
    private JwtSRV jwtSRV;

    @MockBean
    private UtilitySRV utilitySrv;

    @MockBean
    private JwtCFG jwtCFG;

    @BeforeEach
    public void setup() {
        when(utilitySrv.isValidCf(Mockito.anyString())).thenReturn(true);
        when(jwtCFG.getTsIssuer()).thenReturn("TS_ISSUER");
    }

    /**
     * A flexible builder method for JWTPayloadDTO.
     * We can add more parameters as needed for different test scenarios.
     */
    private JWTPayloadDTO buildValidPayload(
            PurposeOfUseEnum purposeOfUse,
            ActionEnum action,
            String locality,
            String sub,
            String subjectOrganizationId,
            String subjectOrganization
    ) {
        return new JWTPayloadDTO(
                "ISS_CODE",
                1680000000L,
                1680003600L,
                "JTI_CODE",
                "AUDIENCE",
                sub != null ? sub : "ABCDEF12G34H567I",
                subjectOrganizationId != null ? subjectOrganizationId : "010",
                subjectOrganization != null ? subjectOrganization : "Regione Piemonte",
                locality != null ? locality : "GTW_NAME^^^^^&2.16.840.1.113883.2.9.4.1.3&ISO^^^^080908",
                RoleEnum.RSA.name(),
                "person",
                true,
                purposeOfUse != null ? purposeOfUse.name() : null,
                "RES_TYPE",
                action != null ? action.name() : null,
                "attachment_hash",
                "app_id",
                "app_vendor",
                "1.0"
        );
    }

    /**
     * Overloaded method for convenience when all defaults are fine except purpose and action.
     */
    private JWTPayloadDTO buildValidPayload(PurposeOfUseEnum purposeOfUse, ActionEnum action) {
        return buildValidPayload(purposeOfUse, action, null, null, null, null);
    }

    @Test
    public void testValidatePayloadForValidation_Success() {
        JWTPayloadDTO validPayload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.CREATE);
        jwtSRV.validatePayloadForValidation(validPayload);
    }

    @Test
    public void testValidatePayloadForValidation_MissingFields() {
        // Missing purpose_of_use
        JWTPayloadDTO payload = buildValidPayload(null, ActionEnum.CREATE);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForValidation(payload));
    }

    @Test
    public void testValidatePayloadForValidation_WrongAction() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.UPDATE);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForValidation(payload));
    }

    @Test
    public void testValidatePayloadForValidation_WrongPurpose() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.UPDATE, ActionEnum.CREATE);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForValidation(payload));
    }

    @Test
    public void testGetSystemByIssuer_TS() {
        SystemTypeEnum system = jwtSRV.getSystemByIssuer("TS_ISSUER");
        org.junit.jupiter.api.Assertions.assertEquals(SystemTypeEnum.TS, system);
    }

    @Test
    public void testGetSystemByIssuer_None() {
        SystemTypeEnum system = jwtSRV.getSystemByIssuer("UNKNOWN_ISSUER");
        org.junit.jupiter.api.Assertions.assertEquals(SystemTypeEnum.NONE, system);
    }

    @Test
    public void testValidatePayloadForCreate_Success() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.CREATE);
        jwtSRV.validatePayloadForCreate(payload);
    }

    @Test
    public void testValidatePayloadForCreate_WrongAction() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.UPDATE);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForCreate(payload));
    }

    @Test
    public void testValidatePayloadForCreate_WrongPurpose() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.UPDATE, ActionEnum.CREATE);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForCreate(payload));
    }

    @Test
    public void testValidatePayloadForCreate_InvalidLocality() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.CREATE, "InvalidLocalityString", null, null, null);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForCreate(payload));
    }

    @Test
    public void testValidatePayloadForReplace_Success() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.UPDATE, ActionEnum.UPDATE);
        jwtSRV.validatePayloadForReplace(payload);
    }

    @Test
    public void testValidatePayloadForReplace_WrongAction() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.UPDATE, ActionEnum.CREATE);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForReplace(payload));
    }

    @Test
    public void testValidatePayloadForReplace_WrongPurpose() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.UPDATE);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForReplace(payload));
    }

    @Test
    public void testValidatePayloadForReplace_InvalidLocality() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.UPDATE, ActionEnum.UPDATE, "InvalidLocalityString", null, null, null);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForReplace(payload));
    }

    @Test
    public void testValidatePayloadForUpdate_Success() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.UPDATE, ActionEnum.UPDATE);
        jwtSRV.validatePayloadForUpdate(payload);
    }

    @Test
    public void testValidatePayloadForUpdate_WrongAction() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.UPDATE, ActionEnum.CREATE);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForUpdate(payload));
    }

    @Test
    public void testValidatePayloadForUpdate_WrongPurpose() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.UPDATE);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForUpdate(payload));
    }

    @Test
    public void testValidatePayloadForDelete_Success() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.UPDATE, ActionEnum.DELETE);
        jwtSRV.validatePayloadForDelete(payload);
    }

    @Test
    public void testValidatePayloadForDelete_WrongAction() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.UPDATE, ActionEnum.CREATE);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForDelete(payload));
    }

    @Test
    public void testValidatePayloadForDelete_WrongPurpose() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.DELETE);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForDelete(payload));
    }

    @Test
    public void testCheckFiscalCode_Invalid() {
        when(utilitySrv.isValidCf(Mockito.anyString())).thenReturn(false);
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.CREATE, null, "INVALIDCF", null, null);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForCreate(payload));
    }

    @Test
    public void testValidateSubjectOrganizationCoherence_Mismatch() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.CREATE, null, null, "ORG", "DIFFERENT");
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForValidation(payload));
    }

    @Test
    public void testCheckNullField() {
        JWTPayloadDTO payload = buildValidPayload(null, ActionEnum.CREATE);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForValidation(payload));
    }

    @Test
    public void testIsValidLocality_Failure() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.CREATE, "NO_CARET_SIGNS", null, null, null);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForCreate(payload));
    }

    @Test
    public void testIsValidLocality_Success() {
        jwtSRV.isValidLocality("GTW_NAME^^^^^&2.16.840.1.113883.2.9.4.1.3&ISO^^^^080908");
    }

    @Test
    public void testCheckEnumValue_InvalidAction() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, null, null, null, null, null);
        payload = new JWTPayloadDTO(
                payload.getIss(), payload.getIat(), payload.getExp(), payload.getJti(), payload.getAud(),
                payload.getSub(), payload.getSubject_organization_id(), payload.getSubject_organization(),
                payload.getLocality(), payload.getSubject_role(), payload.getPerson_id(), payload.getPatient_consent(),
                payload.getPurpose_of_use(), payload.getResource_hl7_type(), "NON_EXISTENT_ACTION",
                payload.getAttachment_hash(), payload.getSubject_application_id(), payload.getSubject_application_vendor(),
                payload.getSubject_application_version()
        );
        JWTPayloadDTO finalPayload = payload;
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForValidation(finalPayload));
    }

    @Test
    public void testCheckEnumValue_InvalidPurpose() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.CREATE);
        payload = new JWTPayloadDTO(
                payload.getIss(), payload.getIat(), payload.getExp(), payload.getJti(), payload.getAud(),
                payload.getSub(), payload.getSubject_organization_id(), payload.getSubject_organization(),
                payload.getLocality(), payload.getSubject_role(), payload.getPerson_id(), payload.getPatient_consent(),
                "NON_EXISTENT_PURPOSE", payload.getResource_hl7_type(), payload.getAction_id(),
                payload.getAttachment_hash(), payload.getSubject_application_id(), payload.getSubject_application_vendor(),
                payload.getSubject_application_version()
        );
        JWTPayloadDTO finalPayload = payload;
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForCreate(finalPayload));
    }

    @Test
    public void testCheckEnumValue_InvalidRole() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.CREATE);
        payload = new JWTPayloadDTO(
                payload.getIss(), payload.getIat(), payload.getExp(), payload.getJti(), payload.getAud(),
                payload.getSub(), payload.getSubject_organization_id(), payload.getSubject_organization(),
                payload.getLocality(), "NON_EXISTENT_ROLE", payload.getPerson_id(), payload.getPatient_consent(),
                payload.getPurpose_of_use(), payload.getResource_hl7_type(), payload.getAction_id(),
                payload.getAttachment_hash(), payload.getSubject_application_id(), payload.getSubject_application_vendor(),
                payload.getSubject_application_version()
        );
        JWTPayloadDTO finalPayload = payload;
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForValidation(finalPayload));
    }

    @Test
    public void testCheckEnumValue_InvalidOrganizationId() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.CREATE, null, null, "NON_EXISTENT_ORG", null);
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForValidation(payload));
    }

    @Test
    public void testCheckEnumValue_InvalidOrganizationDescription() {
        JWTPayloadDTO payload = buildValidPayload(PurposeOfUseEnum.TREATMENT, ActionEnum.CREATE, null, null, null, "NON_EXISTENT_ORG");
        assertThrows(ValidationException.class, () -> jwtSRV.validatePayloadForValidation(payload));
    }

}

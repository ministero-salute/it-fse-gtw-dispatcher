
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.SignatureValidationDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.FileUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.SignerUtility;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(Constants.Profile.TEST)
class SignTest {
    
    @Test
    @Disabled
    void validSignatureTest() throws Exception {
        byte[] fileByte = FileUtility.getFileFromInternalResources("Files/attachment/CDA_OK_SIGNED.pdf");

        File file = File.createTempFile("temp", "signer");
        file.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(fileByte);
		}

        assertTrue(SignerUtility.isSigned(fileByte), "The file should result as signed");
        SignatureValidationDTO result = SignerUtility.validate(fileByte);

        assertEquals(1, result.getNumSignatures(), "The file should be signed once");
        assertTrue(result.getStatus(), "The file should be valid");
    }

    @Test
    @Disabled
    void invalidSignatureTest() throws Exception {
        byte[] fileByte = FileUtility.getFileFromInternalResources("Files/attachment/LAB_OK.pdf");

        File file = File.createTempFile("temp", "signer");
        file.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(fileByte);
		}

        assertFalse(SignerUtility.isSigned(fileByte), "The file should result as not signed");
        SignatureValidationDTO result = SignerUtility.validate(fileByte);

        assertEquals(0, result.getNumSignatures(), "The file should be not signed");
        assertFalse(result.getStatus(), "The file should be invalid");
    }
}

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

import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.PresentMetadataIndexer;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.MetadataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Present Metadata Indexer Test")
class PresentMetadataIndexerTest {

    private PresentMetadataIndexer indexer;
    
    @BeforeEach
    void setUp() {
        indexer = new PresentMetadataIndexer();
    }

    @Test
    @DisplayName("Should extract creationTime slot from DocumentEntry")
    void shouldExtractCreationTimeSlot() throws Exception {
        String xml = Files.readString(Paths.get("src/main/resources/merged_metadata.xml"));
        
        MetadataDTO result = indexer.extractPresentFields(xml);
        
        assertNotNull(result);
        assertTrue(result.getDocumentEntryFields().contains("slot:creationTime"),
            "DocumentEntry should contain creationTime slot");
    }

    @Test
    @DisplayName("Should extract ExternalIdentifier schemes from DocumentEntry")
    void shouldExtractExternalIdentifierSchemes() throws Exception {
        String xml = Files.readString(Paths.get("src/main/resources/merged_metadata.xml"));
        
        MetadataDTO result = indexer.extractPresentFields(xml);
        
        assertTrue(result.getDocumentEntryFields().contains("externalId:urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427"),
            "DocumentEntry should contain patientId ExternalIdentifier");
        assertTrue(result.getDocumentEntryFields().contains("externalId:urn:uuid:2e82c1f6-a085-4c72-9da3-8640a32e42ab"),
            "DocumentEntry should contain uniqueId ExternalIdentifier");
    }

    @Test
    @DisplayName("Should extract Classification schemes from DocumentEntry")
    void shouldExtractClassificationSchemes() throws Exception {
        String xml = Files.readString(Paths.get("src/main/resources/merged_metadata.xml"));
        
        MetadataDTO result = indexer.extractPresentFields(xml);
        
        assertTrue(result.getDocumentEntryFields().contains("classification:urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d"),
            "DocumentEntry should contain author Classification");
        assertTrue(result.getDocumentEntryFields().contains("classification:urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f"),
            "DocumentEntry should contain confidentialityCode Classification");
    }

    @Test
    @DisplayName("Should extract submissionTime slot from SubmissionSet")
    void shouldExtractSubmissionTimeSlot() throws Exception {
        String xml = Files.readString(Paths.get("src/main/resources/merged_metadata.xml"));
        
        MetadataDTO result = indexer.extractPresentFields(xml);
        
        assertTrue(result.getSubmissionSetFields().contains("slot:submissionTime"),
            "SubmissionSet should contain submissionTime slot");
    }

    @Test
    @DisplayName("Should not extract slot with empty ValueList")
    void shouldNotExtractSlotWithEmptyValueList() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<ns3:SubmitObjectsRequest xmlns:ns3=\"urn:oasis:names:tc:ebxml-regrep:xsd:lcm:3.0\">" +
            "<RegistryObjectList>" +
            "<ExtrinsicObject id=\"Document1\">" +
            "<Slot name=\"serviceStartTime\"><ValueList/></Slot>" +
            "</ExtrinsicObject>" +
            "</RegistryObjectList>" +
            "</ns3:SubmitObjectsRequest>";
        
        MetadataDTO result = indexer.extractPresentFields(xml);
        
        assertFalse(result.getDocumentEntryFields().contains("slot:serviceStartTime"),
            "Should not extract slot with empty ValueList");
    }

    @Test
    @DisplayName("Should extract nested slots within Classifications")
    void shouldExtractNestedSlotsWithinClassifications() throws Exception {
        String xml = Files.readString(Paths.get("src/main/resources/merged_metadata.xml"));
        
        MetadataDTO result = indexer.extractPresentFields(xml);
        
        // Verify author classification and its nested slots in DocumentEntry
        assertTrue(result.getDocumentEntryFields().contains("classification:urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d"),
            "DocumentEntry should contain author Classification");
        assertTrue(result.getDocumentEntryFields().contains("classification:urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d:slot:authorInstitution"),
            "DocumentEntry should contain author.authorInstitution nested slot");
        assertTrue(result.getDocumentEntryFields().contains("classification:urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d:slot:authorPerson"),
            "DocumentEntry should contain author.authorPerson nested slot");
        
        // Verify author classification and its nested slots in SubmissionSet
        assertTrue(result.getSubmissionSetFields().contains("classification:urn:uuid:a7058bb9-b4e4-4307-ba5b-e3f0ab85e12d"),
            "SubmissionSet should contain author Classification");
        assertTrue(result.getSubmissionSetFields().contains("classification:urn:uuid:a7058bb9-b4e4-4307-ba5b-e3f0ab85e12d:slot:authorInstitution"),
            "SubmissionSet should contain author.authorInstitution nested slot");
        assertTrue(result.getSubmissionSetFields().contains("classification:urn:uuid:a7058bb9-b4e4-4307-ba5b-e3f0ab85e12d:slot:authorPerson"),
            "SubmissionSet should contain author.authorPerson nested slot");
    }

    @Test
    @DisplayName("Should separate DocumentEntry and SubmissionSet fields")
    void shouldSeparateDocumentEntryAndSubmissionSetFields() throws Exception {
        String xml = Files.readString(Paths.get("src/main/resources/merged_metadata.xml"));
        
        MetadataDTO result = indexer.extractPresentFields(xml);
        
        assertNotNull(result.getDocumentEntryFields());
        assertNotNull(result.getSubmissionSetFields());
        assertFalse(result.getDocumentEntryFields().isEmpty());
        assertFalse(result.getSubmissionSetFields().isEmpty());
        
        // Verify separation: creationTime should be in DocumentEntry, not SubmissionSet
        assertTrue(result.getDocumentEntryFields().contains("slot:creationTime"));
        assertFalse(result.getSubmissionSetFields().contains("slot:creationTime"));
        
        // Verify separation: submissionTime should be in SubmissionSet, not DocumentEntry
        assertTrue(result.getSubmissionSetFields().contains("slot:submissionTime"));
        assertFalse(result.getDocumentEntryFields().contains("slot:submissionTime"));
    }
}

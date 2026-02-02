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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.MetadataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Parses ebXML metadata to extract present fields from ExtrinsicObject (DocumentEntry)
 * and RegistryPackage (SubmissionSet).
 */
@Slf4j
@Component
public class PresentMetadataIndexer {

    private static final String EXTRINSIC_OBJECT = "ExtrinsicObject";
    private static final String REGISTRY_PACKAGE = "RegistryPackage";
    private static final String SLOT = "Slot";
    private static final String CLASSIFICATION = "Classification";
    private static final String EXTERNAL_IDENTIFIER = "ExternalIdentifier";
    private static final String VALUE_LIST = "ValueList";
    private static final String VALUE = "Value";

    /**
     * Parses the merged metadata XML and extracts present fields.
     * 
     * @param metadataXml The merged metadata XML string
     * @return MetadataDTO containing sets of present fields
     */
    public MetadataDTO extractPresentFields(String metadataXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(metadataXml.getBytes(StandardCharsets.UTF_8)));

            Set<String> documentEntryFields = new HashSet<>();
            Set<String> submissionSetFields = new HashSet<>();

            // Extract DocumentEntry fields from ExtrinsicObject
            NodeList extrinsicObjects = doc.getElementsByTagName(EXTRINSIC_OBJECT);
            for (int i = 0; i < extrinsicObjects.getLength(); i++) {
                Element extrinsicObject = (Element) extrinsicObjects.item(i);
                extractFieldsFromElement(extrinsicObject, documentEntryFields);
            }

            // Extract SubmissionSet fields from RegistryPackage
            NodeList registryPackages = doc.getElementsByTagName(REGISTRY_PACKAGE);
            for (int i = 0; i < registryPackages.getLength(); i++) {
                Element registryPackage = (Element) registryPackages.item(i);
                extractFieldsFromElement(registryPackage, submissionSetFields);
            }

            log.debug("Extracted {} DocumentEntry fields and {} SubmissionSet fields",
                documentEntryFields.size(), submissionSetFields.size());

            return new MetadataDTO(documentEntryFields, submissionSetFields);

        } catch (Exception e) {
            log.error("Error parsing metadata XML", e);
            throw new IllegalArgumentException("Failed to parse metadata XML: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts fields from a parent element (ExtrinsicObject or RegistryPackage).
     */
    private void extractFieldsFromElement(Element parentElement, Set<String> fields) {
        // Extract Slots (direct children only)
        NodeList slots = parentElement.getElementsByTagName(SLOT);
        for (int i = 0; i < slots.getLength(); i++) {
            Element slot = (Element) slots.item(i);
            // Only process direct children of the parent element
            if (slot.getParentNode().equals(parentElement)) {
                String slotName = slot.getAttribute("name");
                if (slotName != null && !slotName.isEmpty()) {
                    // Check if ValueList has at least one Value
                    if (hasNonEmptyValueList(slot)) {
                        fields.add("slot:" + slotName);
                    }
                }
            }
        }

        // Extract Classifications and their nested slots
        NodeList classifications = parentElement.getElementsByTagName(CLASSIFICATION);
        for (int i = 0; i < classifications.getLength(); i++) {
            Element classification = (Element) classifications.item(i);
            // Only process direct children of the parent element
            if (classification.getParentNode().equals(parentElement)) {
                String scheme = classification.getAttribute("classificationScheme");
                if (scheme != null && !scheme.isEmpty()) {
                    fields.add("classification:" + scheme);
                    
                    // Extract nested slots within this Classification
                    NodeList nestedSlots = classification.getElementsByTagName(SLOT);
                    for (int j = 0; j < nestedSlots.getLength(); j++) {
                        Element nestedSlot = (Element) nestedSlots.item(j);
                        // Only process direct children of this classification
                        if (nestedSlot.getParentNode().equals(classification)) {
                            String nestedSlotName = nestedSlot.getAttribute("name");
                            if (nestedSlotName != null && !nestedSlotName.isEmpty()) {
                                if (hasNonEmptyValueList(nestedSlot)) {
                                    // Format: classification:scheme:slotName
                                    fields.add("classification:" + scheme + ":slot:" + nestedSlotName);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Extract ExternalIdentifiers
        NodeList externalIds = parentElement.getElementsByTagName(EXTERNAL_IDENTIFIER);
        for (int i = 0; i < externalIds.getLength(); i++) {
            Element externalId = (Element) externalIds.item(i);
            // Only process direct children of the parent element
            if (externalId.getParentNode().equals(parentElement)) {
                String scheme = externalId.getAttribute("identificationScheme");
                if (scheme != null && !scheme.isEmpty()) {
                    fields.add("externalId:" + scheme);
                }
            }
        }
    }

    /**
     * Checks if a Slot has a non-empty ValueList (at least one Value element).
     */
    private boolean hasNonEmptyValueList(Element slot) {
        NodeList valueLists = slot.getElementsByTagName(VALUE_LIST);
        if (valueLists.getLength() == 0) {
            return false;
        }

        Element valueList = (Element) valueLists.item(0);
        NodeList values = valueList.getElementsByTagName(VALUE);
        return values.getLength() > 0;
    }
}

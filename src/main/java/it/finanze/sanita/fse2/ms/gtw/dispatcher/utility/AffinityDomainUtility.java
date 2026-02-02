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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.utility;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for Affinity Domain validation operations.
 */
@Slf4j
@Component
public class AffinityDomainUtility {

    private static final DateTimeFormatter CREATION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Extracts the creationTime from the merged metadata XML.
     * The creationTime is found in the ExtrinsicObject/Slot[@name='creationTime']/ValueList/Value element.
     * Format is typically YYYYMMDDHHmmss, we extract only the date part (YYYYMMDD).
     * 
     * @param metadataXml The merged metadata XML string
     * @return LocalDate representing the document creation date
     * @throws IllegalArgumentException if creationTime cannot be extracted
     */
    public LocalDate extractCreationTime(String metadataXml) {
        if (metadataXml == null || metadataXml.trim().isEmpty()) {
            throw new IllegalArgumentException("Metadata XML is null or empty");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(metadataXml.getBytes(StandardCharsets.UTF_8)));

            // Find ExtrinsicObject elements
            NodeList extrinsicObjects = doc.getElementsByTagName("ExtrinsicObject");
            if (extrinsicObjects.getLength() == 0) {
                throw new IllegalArgumentException("No ExtrinsicObject found in metadata XML");
            }

            // Get the first ExtrinsicObject (DocumentEntry)
            Element extrinsicObject = (Element) extrinsicObjects.item(0);

            // Find Slot with name="creationTime"
            NodeList slots = extrinsicObject.getElementsByTagName("Slot");
            for (int i = 0; i < slots.getLength(); i++) {
                Element slot = (Element) slots.item(i);
                String slotName = slot.getAttribute("name");
                
                if ("creationTime".equals(slotName)) {
                    // Get ValueList/Value
                    NodeList valueLists = slot.getElementsByTagName("ValueList");
                    if (valueLists.getLength() > 0) {
                        Element valueList = (Element) valueLists.item(0);
                        NodeList values = valueList.getElementsByTagName("Value");
                        
                        if (values.getLength() > 0) {
                            Element value = (Element) values.item(0);
                            String creationTimeStr = value.getTextContent().trim();
                            
                            if (creationTimeStr.isEmpty()) {
                                throw new IllegalArgumentException("creationTime value is empty");
                            }
                            
                            // Extract date part (first 8 characters: YYYYMMDD)
                            if (creationTimeStr.length() < 8) {
                                throw new IllegalArgumentException(
                                    "creationTime format invalid: " + creationTimeStr + " (expected at least YYYYMMDD)"
                                );
                            }
                            
                            String datePart = creationTimeStr.substring(0, 8);
                            LocalDate creationDate = LocalDate.parse(datePart, CREATION_TIME_FORMATTER);
                            
                            log.debug("Extracted creationTime: {} -> {}", creationTimeStr, creationDate);
                            return creationDate;
                        }
                    }
                    
                    throw new IllegalArgumentException("creationTime slot found but has no Value");
                }
            }

            throw new IllegalArgumentException("creationTime slot not found in ExtrinsicObject");

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error extracting creationTime from metadata XML", e);
            throw new IllegalArgumentException("Failed to extract creationTime: " + e.getMessage(), e);
        }
    }
}

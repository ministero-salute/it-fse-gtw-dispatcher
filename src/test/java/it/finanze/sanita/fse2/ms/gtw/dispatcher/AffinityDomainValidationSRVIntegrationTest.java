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

import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.AffinityDomainValidationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.AffinityDomainUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AffinityDomainStrategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AffinityDomainStrategyResolver;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.PresentMetadataIndexer;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad21.Ad21Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad262.Ad262Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad263.Ad263Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for AffinityDomainValidationSRV with real (non-mocked) dependencies.
 * This test allows debugging the actual validation logic flow.
 */
@DisplayName("Affinity Domain Validation Service Integration Test")
class AffinityDomainValidationSRVIntegrationTest {

    private AffinityDomainValidationSRV service;
    private AffinityDomainUtility utility;
    private AffinityDomainStrategyResolver resolver;
    private PresentMetadataIndexer indexer;

    @BeforeEach
    void setUp() throws Exception {
        // Create real instances of all dependencies
        utility = new AffinityDomainUtility();
        indexer = new PresentMetadataIndexer();
        
        // Create resolver with real strategies
        resolver = new AffinityDomainStrategyResolver();
        
        // Use reflection to inject strategy list and initialize
        Field strategyListField = AffinityDomainStrategyResolver.class.getDeclaredField("strategyList");
        strategyListField.setAccessible(true);
        strategyListField.set(resolver, Arrays.asList(new Ad21Strategy(), new Ad263Strategy()));
        resolver.init();
        
        // Create service and inject real dependencies
        service = new AffinityDomainValidationSRV();
        
        Field utilityField = AffinityDomainValidationSRV.class.getDeclaredField("affinityDomainUtility");
        utilityField.setAccessible(true);
        utilityField.set(service, utility);
        
        Field resolverField = AffinityDomainValidationSRV.class.getDeclaredField("strategyResolver");
        resolverField.setAccessible(true);
        resolverField.set(service, resolver);
        
        Field indexerField = AffinityDomainValidationSRV.class.getDeclaredField("presentMetadataIndexer");
        indexerField.setAccessible(true);
        indexerField.set(service, indexer);
    }

    // Helper methods

    private String loadMergedMetadataXml() throws IOException, URISyntaxException {
        return new String(Files.readAllBytes(
            Paths.get(getClass().getClassLoader()
                .getResource("merged_metadata-2.6.3.xml").toURI())));
    }

    private String loadMergedMetadataXmlOld() throws IOException, URISyntaxException {
        return new String(Files.readAllBytes(
            Paths.get(getClass().getClassLoader()
                .getResource("merged_metadata.xml").toURI())));
    }

    private String buildIncompleteXml() {
        // Minimal XML with ExtrinsicObject but missing many mandatory fields
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<ns3:SubmitObjectsRequest xmlns=\"urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0\" " +
            "xmlns:ns3=\"urn:oasis:names:tc:ebxml-regrep:xsd:lcm:3.0\">\n" +
            "  <RegistryObjectList>\n" +
            "    <ExtrinsicObject id=\"Document1\">\n" +
            "      <Slot name=\"creationTime\">\n" +
            "        <ValueList><Value>20220210173023</Value></ValueList>\n" +
            "      </Slot>\n" +
            "      <!-- Missing many mandatory fields -->\n" +
            "    </ExtrinsicObject>\n" +
            "    <RegistryPackage id=\"SubmissionSet1\">\n" +
            "      <Slot name=\"submissionTime\">\n" +
            "        <ValueList><Value>20260122123311</Value></ValueList>\n" +
            "      </Slot>\n" +
            "      <!-- Missing many mandatory fields -->\n" +
            "    </RegistryPackage>\n" +
            "  </RegistryObjectList>\n" +
            "</ns3:SubmitObjectsRequest>";
    }

    private String buildXmlWithoutCreationTime() {
        // XML without creationTime slot
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<ns3:SubmitObjectsRequest xmlns=\"urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0\" " +
            "xmlns:ns3=\"urn:oasis:names:tc:ebxml-regrep:xsd:lcm:3.0\">\n" +
            "  <RegistryObjectList>\n" +
            "    <ExtrinsicObject id=\"Document1\">\n" +
            "      <!-- No creationTime slot -->\n" +
            "      <Slot name=\"hash\">\n" +
            "        <ValueList><Value>abc123</Value></ValueList>\n" +
            "      </Slot>\n" +
            "    </ExtrinsicObject>\n" +
            "    <RegistryPackage id=\"SubmissionSet1\">\n" +
            "      <Slot name=\"submissionTime\">\n" +
            "        <ValueList><Value>20260122123311</Value></ValueList>\n" +
            "      </Slot>\n" +
            "    </RegistryPackage>\n" +
            "  </RegistryObjectList>\n" +
            "</ns3:SubmitObjectsRequest>";
    }
}
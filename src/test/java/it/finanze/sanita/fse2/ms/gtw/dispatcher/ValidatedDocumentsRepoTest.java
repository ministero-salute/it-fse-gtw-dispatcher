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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.ValidatedDocumentsETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IValidatedDocumentsRepo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(Constants.Profile.TEST)
class ValidatedDocumentsRepoTest extends AbstractTest {

	@Autowired
	private IValidatedDocumentsRepo repo;

	@Autowired
	private MongoTemplate mongoTemplate;

	@MockitoSpyBean
	private RestTemplate restTemplate;

	@BeforeEach
	void init() {
		mongoTemplate.dropCollection(ValidatedDocumentsETY.class);
	}

	@Test
	@DisplayName("Update document's data")
	void updateDataRepoTest() {
		// Data preparation
		ValidatedDocumentsETY ety = new ValidatedDocumentsETY();
		ety.setWorkflowInstanceId("test");
		ety.setInsertionDate(new Date());
		// Insert on DB
		mongoTemplate.insert(ety);
		// Assertion
		assertEquals(repo.updateInsertionDate("test", 7), ety.getId());
	}

	@Test
	void isItemInsertedTest() {
		String hash = "hash_test";
		// Data preparation
		ValidatedDocumentsETY ety = new ValidatedDocumentsETY();
		ety.setHashCda(hash);
		ety.setInsertionDate(new Date());
		// Insert on DB
		mongoTemplate.insert(ety);
		// Assertion
		assertTrue(repo.isItemInserted(hash));
	}

	@Test
	void findItemByIdTest() {
		String id = "id_test";
		String hash = "hash_test";
		// Data preparation
		ValidatedDocumentsETY ety = new ValidatedDocumentsETY();
		ety.setId(id);
		ety.setHashCda(hash);
		// Insert on DB
		mongoTemplate.insert(ety);
		// Assertions
		ValidatedDocumentsETY response = repo.findItemById(id);
		assertEquals(id, response.getId());
		assertEquals(hash, response.getHashCda());
	}
	
}

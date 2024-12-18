package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import com.mongodb.MongoException;
import com.mongodb.client.result.DeleteResult;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationDataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.ValidatedDocumentsETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.impl.ValidatedDocumentsRepo;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.Profile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profile.TEST)
class ValidatedDocumentsRepoTest {

	@InjectMocks
	private ValidatedDocumentsRepo repo;

	@Mock
	private MongoTemplate mongoTemplate;

	private ValidatedDocumentsETY ety;

	@BeforeEach
	void setUp() {
		ety = new ValidatedDocumentsETY();
		ety.setId(new ObjectId().toHexString());
		ety.setHashCda("testHash");
		ety.setWorkflowInstanceId("WID-123");
		ety.setPrimaryKeyTransform("transformID");
		ety.setPrimaryKeyEngine("engineID");
		ety.setInsertionDate(new Date());
	}

	@Test
	void testCreate_newDocument() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(null);
		doAnswer(invocation -> invocation.getArgument(0)).when(mongoTemplate).save(any(ValidatedDocumentsETY.class));

		repo.create(ety);

		verify(mongoTemplate).findOne(any(Query.class), eq(ValidatedDocumentsETY.class));
		verify(mongoTemplate).save(ety);
	}

	@Test
	void testCreate_existingDocument() {
		ValidatedDocumentsETY existing = new ValidatedDocumentsETY();
		existing.setId("existingID");
		existing.setHashCda("testHash");
		existing.setWorkflowInstanceId("OLD_WID");
		existing.setPrimaryKeyTransform("oldTransform");
		existing.setInsertionDate(new Date());

		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(existing);
		when(mongoTemplate.save(any(ValidatedDocumentsETY.class))).thenReturn(existing);

		repo.create(ety);

		verify(mongoTemplate).save(existing);
		assertEquals("WID-123", existing.getWorkflowInstanceId());
		assertEquals("transformID", existing.getPrimaryKeyTransform());
	}

	@Test
	void testCreate_exception() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class)))
				.thenThrow(new MongoException("error"));

		BusinessException ex = assertThrows(BusinessException.class, () -> repo.create(ety));
		assertTrue(ex.getMessage().contains("Error while insert validated document"));
	}

	@Test
	void testIsItemInserted_true() {
		when(mongoTemplate.exists(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(true);
		boolean result = repo.isItemInserted("testHash");
		assertTrue(result);
	}

	@Test
	void testIsItemInserted_false() {
		when(mongoTemplate.exists(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(false);
		boolean result = repo.isItemInserted("testHash");
		assertFalse(result);
	}

	@Test
	void testIsItemInserted_exception() {
		when(mongoTemplate.exists(any(Query.class), eq(ValidatedDocumentsETY.class)))
				.thenThrow(new MongoException("exists error"));
		BusinessException ex = assertThrows(BusinessException.class, () -> repo.isItemInserted("testHash"));
		assertTrue(ex.getMessage().contains("Unable to verify if validate document is inserted"));
	}

	@Test
	void testDeleteItem_true() {
		DeleteResult deleteResult = mock(DeleteResult.class);
		when(deleteResult.wasAcknowledged()).thenReturn(true);
		when(mongoTemplate.remove(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(deleteResult);

		boolean result = repo.deleteItem("testHash");
		assertTrue(result);
	}

	@Test
	void testDeleteItem_false() {
		DeleteResult deleteResult = mock(DeleteResult.class);
		when(deleteResult.wasAcknowledged()).thenReturn(false);
		when(mongoTemplate.remove(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(deleteResult);

		boolean result = repo.deleteItem("testHash");
		assertFalse(result);
	}

	@Test
	void testDeleteItem_exception() {
		when(mongoTemplate.remove(any(Query.class), eq(ValidatedDocumentsETY.class)))
				.thenThrow(new MongoException("remove error"));
		BusinessException ex = assertThrows(BusinessException.class, () -> repo.deleteItem("testHash"));
		assertTrue(ex.getMessage().contains("Unable to delete the item"));
	}

	@Test
	void testFindItemById_found() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(ety);

		ValidatedDocumentsETY result = repo.findItemById("someID");
		assertEquals(ety, result);
	}

	@Test
	void testFindItemById_null() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(null);
		ValidatedDocumentsETY result = repo.findItemById("unknownID");
		assertNull(result);
	}

	@Test
	void testFindItemById_exception() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class)))
				.thenThrow(new MongoException("find error"));
		BusinessException ex = assertThrows(BusinessException.class, () -> repo.findItemById("someID"));
		assertTrue(ex.getMessage().contains("Unable to retrieve validated doc by id"));
	}

	@Test
	void testFindItemByWorkflowInstanceId_found() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(ety);

		ValidationDataDTO dto = repo.findItemByWorkflowInstanceId("WID-123");
		assertTrue(dto.isCdaValidated());
		assertEquals("testHash", dto.getHash());
	}

	@Test
	void testFindItemByWorkflowInstanceId_notFound() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(null);
		ValidationDataDTO dto = repo.findItemByWorkflowInstanceId("WID-999");
		assertFalse(dto.isCdaValidated());
		assertNull(dto.getHash());
	}

	@Test
	void testFindItemByWorkflowInstanceId_exception() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class)))
				.thenThrow(new MongoException("error"));
		BusinessException ex = assertThrows(BusinessException.class, () -> repo.findItemByWorkflowInstanceId("WID-ERROR"));
		assertTrue(ex.getMessage().contains("Unable to retrieve validated doc by hash"));
	}

	@Test
	void testFindItemByHash_found() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(ety);
		ValidationDataDTO dto = repo.findItemByHash("testHash");
		assertTrue(dto.isCdaValidated());
		assertEquals("testHash", dto.getHash());
	}

	@Test
	void testFindItemByHash_notFound() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(null);
		ValidationDataDTO dto = repo.findItemByHash("noHash");
		assertFalse(dto.isCdaValidated());
	}

	@Test
	void testFindItemByHash_exception() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class)))
				.thenThrow(new MongoException("error"));
		BusinessException ex = assertThrows(BusinessException.class, () -> repo.findItemByHash("errorHash"));
		assertTrue(ex.getMessage().contains("Unable to retrieve validated doc by hash"));
	}

	@Test
	void testUpdateInsertionDate_found() {
		ValidatedDocumentsETY found = new ValidatedDocumentsETY();
		found.setId("foundID");
		found.setHashCda("someHash");
		found.setWorkflowInstanceId("WID-123");
		found.setInsertionDate(new Date());

		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(found);
		when(mongoTemplate.save(any(ValidatedDocumentsETY.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String result = repo.updateInsertionDate("WID-123", 2);
		assertEquals(found.getId(), result);
		// Insertion date should be updated
		// Not checking exact date since it's dependent on DateUtility.addDay logic
		assertNotNull(found.getInsertionDate());
	}

	@Test
	void testUpdateInsertionDate_notFound() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(null);
		String result = repo.updateInsertionDate("WID-999", 2);
		assertEquals("", result);
	}

	@Test
	void testUpdateInsertionDate_exception() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class)))
				.thenThrow(new MongoException("error"));

		BusinessException ex = assertThrows(BusinessException.class, () -> repo.updateInsertionDate("WID-ERR", 2));
		assertTrue(ex.getMessage().contains("Error while update validated document"));
	}

	@Test
	void testCreateBenchmark() {
		doAnswer(invocation -> invocation.getArgument(0)).when(mongoTemplate).save(any(ValidatedDocumentsETY.class));
		repo.createBenchmark(ety);
		verify(mongoTemplate).save(ety);
	}

	@Test
	void testDeleteItemBenchmark_found() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(ety);
		DeleteResult deleteResult = mock(DeleteResult.class);
		when(deleteResult.wasAcknowledged()).thenReturn(true);
		when(mongoTemplate.remove(ety)).thenReturn(deleteResult);

		boolean result = repo.deleteItemBenchmark("testHash");
		assertTrue(result);
		verify(mongoTemplate).remove(ety);
	}

	@Test
	void testDeleteItemBenchmark_exception() {
		when(mongoTemplate.findOne(any(Query.class), eq(ValidatedDocumentsETY.class)))
				.thenThrow(new MongoException("error"));

		BusinessException ex = assertThrows(BusinessException.class, () -> repo.deleteItemBenchmark("errorHash"));
		assertTrue(ex.getMessage().contains("Unable to delete the item"));
	}

}

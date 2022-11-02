package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo;

import java.io.Serializable;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationDataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.ValidatedDocumentsETY;

public interface IValidatedDocumentsRepo extends Serializable {
	
	void create(final ValidatedDocumentsETY ety);

	boolean deleteItem(String hash);

	ValidatedDocumentsETY findItemById(String id);

	ValidationDataDTO findItemByHash(String hash); // tornava la chiave (string)

	ValidationDataDTO findItemByWorkflowInstanceId(String wid); // tornava la chiave (string)

	boolean isItemInserted(String hash);

	String updateInsertionDate(String workflowInstanceId, int days);
}
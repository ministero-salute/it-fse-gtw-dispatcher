package it.finanze.sanita.fse2.ms.gtw.dispatcher.service;

import java.util.List;
import java.util.Optional;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.LogCollectorControlETY;

public interface ILogCollectorSRV {

    /**
     * Returns all structured logs associated to the given document ID.
     *
     * @param idDoc the document identifier
     * @return list of {@link LogCollectorControlETY} matching the document ID
     */
    List<LogCollectorControlETY> findStructuredLogsByIdDoc(String idDoc);

     /**
     * Returns all structured logs associated to the given workflow instance id.
     *
     * @param idDoc the document identifier
     * @return list of {@link LogCollectorControlETY} matching workflow instance id
     */
    List<LogCollectorControlETY> findStructuredLogsByWii(String wii);


    boolean isDocSentToEDS(String wii);

    boolean isDocSentToINI(String wii);

}

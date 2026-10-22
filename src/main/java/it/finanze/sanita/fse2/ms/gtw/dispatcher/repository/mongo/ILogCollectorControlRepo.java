package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo;

import java.util.List;
import java.util.Optional;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.LogCollectorControlETY;

public interface ILogCollectorControlRepo {

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
     * @return list of {@link LogCollectorControlETY} matching the workflow instance id
     */
    List<LogCollectorControlETY> findStructuredLogsByWii(String wii);

    /**
     * Returns true if at least one OK EDS log exists for the given workflow instance ID.
     *
     * @param wii the workflow instance ID
     * @return {@code true} if a matching log entry exists
     */
    boolean existsOKEDSLog(String wii);

    /**
     * Returns true if at least one OK INI log exists for the given workflow instance ID.
     *
     * @param wii the workflow instance ID
     * @return {@code true} if a matching log entry exists
     */
    boolean existsOKINILog(String wii);

    
    

}

package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.impl;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.OperationLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ResultLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.LogCollectorControlETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.ILogCollectorControlRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
public class LogCollectorControlRepo implements ILogCollectorControlRepo {

    @Autowired
    private MongoTemplate mongo;

    private String iniClientName = "gtw-ini-client";

    @Override
    public List<LogCollectorControlETY> findStructuredLogsByIdDoc(String idDoc) {
        try {
            Query query = new Query(
                Criteria.where(Constants.Mongo.Fields.ID_DOCUMENTO).is(idDoc)
                    .and(Constants.Mongo.Fields.OP_RESULT).is(ResultLogEnum.OK.getCode())
            );
            query.with(Sort.by(Constants.Mongo.Fields.OP_TIMESTAMP_END).descending());
            return mongo.find(query, LogCollectorControlETY.class);
        } catch (Exception ex) {
            log.error("Error while retrieving structured logs for idDoc: {}", idDoc, ex);
            throw new BusinessException("Error while retrieving structured logs", ex);
        }
    }

    @Override
    public List<LogCollectorControlETY> findStructuredLogsByWii(String wii) {
        try {
            Query query = new Query(
                Criteria.where(Constants.Mongo.Fields.WORKFLOW_INSTANCE_ID).is(wii)
                    .and(Constants.Mongo.Fields.OP_RESULT).is(ResultLogEnum.OK.getCode())
            );
            query.with(Sort.by(Constants.Mongo.Fields.OP_TIMESTAMP_END).descending());
            return mongo.find(query, LogCollectorControlETY.class);
        } catch (Exception ex) {
            log.error("Error while retrieving structured logs for wii: {}", wii, ex);
            throw new BusinessException("Error while retrieving structured logs", ex);
        }
       
    }

    @Override
    public boolean existsOKEDSLog(String wii) {
        try {
            Query query = new Query(
                Criteria.where(Constants.Mongo.Fields.WORKFLOW_INSTANCE_ID).is(wii)
                    .and(Constants.Mongo.Fields.OPERATION).is(OperationLogEnum.PUB_CDA2.getCode())
                    .and(Constants.Mongo.Fields.OP_RESULT).is(ResultLogEnum.OK.getCode())
            );
            return mongo.exists(query, LogCollectorControlETY.class);
        } catch (Exception ex) {
            log.error("Error while checking existence of OK EDS log for wii: {}", wii, ex);
            throw new BusinessException("Error while checking existence of OK EDS log", ex);
        }
    }

    @Override
    public boolean existsOKINILog(String wii) {
        try {
            Query query = new Query(
                Criteria.where(Constants.Mongo.Fields.WORKFLOW_INSTANCE_ID).is(wii)
                    .and(Constants.Mongo.Fields.OPERATION).is(OperationLogEnum.PUB_CDA2.getCode())
                    .and(Constants.Mongo.Fields.OP_RESULT).is(ResultLogEnum.OK.getCode())
                    .and(Constants.Mongo.Fields.MICROSERVICE_NAME).is(iniClientName)
            );
            return mongo.exists(query, LogCollectorControlETY.class);
        } catch (Exception ex) {
            log.error("Error while checking existence of OK INI log for wii: {}", wii, ex);
            throw new BusinessException("Error while checking existence of OK INI log", ex);
        }
        
    }

    
}

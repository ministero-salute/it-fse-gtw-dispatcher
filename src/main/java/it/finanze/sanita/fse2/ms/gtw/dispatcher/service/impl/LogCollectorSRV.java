package it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.LogCollectorControlETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.ILogCollectorControlRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.ILogCollectorSRV;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LogCollectorSRV implements ILogCollectorSRV {

    @Autowired
    private ILogCollectorControlRepo logCollectorControlRepo;

    @Override
    public List<LogCollectorControlETY> findStructuredLogsByIdDoc(String idDoc) {
        log.info("Retrieving structured logs for idDoc: {}", idDoc);
        List<LogCollectorControlETY> logs = logCollectorControlRepo.findStructuredLogsByIdDoc(idDoc);
        log.info("Found {} structured log(s) for idDoc: {}", logs.size(), idDoc);
        return logs;
    }

    @Override
    public List<LogCollectorControlETY> findStructuredLogsByWii(String wii) {
        log.info("Retrieving structured logs for wii: {}", wii);
        List<LogCollectorControlETY> logs = logCollectorControlRepo.findStructuredLogsByWii(wii);
        log.info("Found {} structured log(s) for wii: {}", logs.size(), wii);
        return logs;
    }

    @Override
    public boolean isDocSentToEDS(String wii) {
        log.info("Checking OK validation log existence for {}", wii);
        boolean exists = logCollectorControlRepo.existsOKEDSLog(wii);
        log.info("OK validation log {} for {}", exists ? "found" : "not found", wii);
        return exists;
    }

    @Override
    public boolean isDocSentToINI(String wii) {
        log.info("Checking OK validation log existence for {}", wii);
        boolean exists = logCollectorControlRepo.existsOKINILog(wii);
        log.info("OK validation log {} for {}", exists ? "found" : "not found", wii);
        return exists;
    }



    

}

package it.finanze.sanita.fse2.ms.gtw.dispatcher.service;

public interface IConfigSRV {

    Boolean isAuditEnable();
    
    Boolean isControlLogPersistenceEnable();
    
    Boolean isCfOnIssuerNotAllowed();
    
    Boolean isSubjectNotAllowed();
    
    Boolean isRemoveEds();

    long getRefreshRate();
    
    Boolean isAuditIniEnable();
    
}

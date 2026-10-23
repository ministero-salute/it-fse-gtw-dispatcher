package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.SubjApplicationDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogCollectorBase {


    @Id
    private String id;

    @Field(name = Constants.Mongo.Fields.LOG_TYPE)
    @JsonProperty(Constants.Mongo.Fields.LOG_TYPE)
    private String logType;

    @Field(name = Constants.Mongo.Fields.MESSAGE)
    @JsonProperty(Constants.Mongo.Fields.MESSAGE)
    private String message;

    @Field(name = Constants.Mongo.Fields.OPERATION)
    @JsonProperty(Constants.Mongo.Fields.OPERATION)
    private String operation;

    @Field(name = Constants.Mongo.Fields.OP_RESULT)
    @JsonProperty(Constants.Mongo.Fields.OP_RESULT)
    private String result;

    @Field(name = Constants.Mongo.Fields.OP_TIMESTAMP_START)
    @JsonProperty(Constants.Mongo.Fields.OP_TIMESTAMP_START)
    @DateTimeFormat(pattern = Constants.App.Custom.DATE_PATTERN)
    private Date timestampStart;

    @Field(name = Constants.Mongo.Fields.OP_TIMESTAMP_END)
    @JsonProperty(Constants.Mongo.Fields.OP_TIMESTAMP_END)
    @DateTimeFormat(pattern = Constants.App.Custom.DATE_PATTERN)
    private Date timestampEnd;

    @Field(name = Constants.Mongo.Fields.OP_DOCUMENT_TYPE)
    @JsonProperty(Constants.Mongo.Fields.OP_DOCUMENT_TYPE)
    private String documentType;

    @Field(name = Constants.Mongo.Fields.OP_ROLE)
    @JsonProperty(Constants.Mongo.Fields.OP_ROLE)
    private String role;

    @Field(name = Constants.Mongo.Fields.OP_FISCAL_CODE)
    @JsonProperty(Constants.Mongo.Fields.OP_FISCAL_CODE)
    private String fiscalCode;

    @Field(name = Constants.Mongo.Fields.MICROSERVICE_NAME)
    @JsonProperty(Constants.Mongo.Fields.MICROSERVICE_NAME)
    private String microserviceName;

    @Field(name = Constants.Mongo.Fields.OP_SUBJ_APPLICATION)
    @JsonProperty(Constants.Mongo.Fields.OP_SUBJ_APPLICATION)
    private SubjApplicationDTO opSubjApplication;
    
    @Field(name = Constants.Mongo.Fields.PROCESSED)
    @JsonProperty(Constants.Mongo.Fields.PROCESSED)
    private String processed;

    @Field(name = Constants.Mongo.Fields.WORKFLOW_INSTANCE_ID)
    @JsonProperty(Constants.Mongo.Fields.WORKFLOW_INSTANCE_ID)
    private String workflowInstanceId;
}

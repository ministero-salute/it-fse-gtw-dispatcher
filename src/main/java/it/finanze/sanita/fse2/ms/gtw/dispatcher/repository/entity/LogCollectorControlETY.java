package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.IssuerDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.LocalityDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Document(collection = "#{@logCollectorControlBean}")
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = false)
@Data
public class LogCollectorControlETY extends LogCollectorBase {

    @Field(name = Constants.Mongo.Fields.OP_ERROR)
    @JsonProperty(Constants.Mongo.Fields.OP_ERROR)
    private String error;

    @Field(name = Constants.Mongo.Fields.OP_ERROR_DESCRIPTION)
    @JsonProperty(Constants.Mongo.Fields.OP_ERROR_DESCRIPTION)
    private String errorDescription;

    @Field(name = Constants.Mongo.Fields.OP_ISSUER)
    @JsonProperty(Constants.Mongo.Fields.OP_ISSUER)
    private IssuerDTO issuer;

    @Field(name = Constants.Mongo.Fields.OP_LOCALITY)
    @JsonProperty(Constants.Mongo.Fields.OP_LOCALITY)
    private LocalityDTO locality;

    @Field(name = Constants.Mongo.Fields.GATEWAY_NAME)
    @JsonProperty(Constants.Mongo.Fields.GATEWAY_NAME)
    private String gatewayName;

    @Field(name = Constants.Mongo.Fields.OP_WARNING)
    @JsonProperty(Constants.Mongo.Fields.OP_WARNING)
    private String warning;

    @Field(name = Constants.Mongo.Fields.OP_WARNING_DESCRIPTION)
    @JsonProperty(Constants.Mongo.Fields.OP_WARNING_DESCRIPTION)
    private String warningDescription;

    @Field(name = Constants.Mongo.Fields.TYPE_ID_EXTENSION)
    @JsonProperty(Constants.Mongo.Fields.TYPE_ID_EXTENSION)
    private String typeIdExtension;

    @Field(name = Constants.Mongo.Fields.ID_DOCUMENTO)
    @JsonProperty(Constants.Mongo.Fields.ID_DOCUMENTO)
    private String idDocumento;

}
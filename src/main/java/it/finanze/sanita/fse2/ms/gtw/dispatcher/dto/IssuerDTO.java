package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssuerDTO {

    @Field("raw_value")
    private String rawValue;

    @Field("region")
    private String region;

    @Field("asl")
    private String asl;

    @Field("enterprise")
    private String enterprise;

    @Field("fiscal_code")
    private String fiscalCode;

}

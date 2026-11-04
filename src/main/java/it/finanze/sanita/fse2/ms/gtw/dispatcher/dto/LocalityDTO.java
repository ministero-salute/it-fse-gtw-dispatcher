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
public class LocalityDTO {

    @Field("raw_value")
    private String rawValue;

    @Field("asl_code")
    private String aslCode;

    @Field("structure")
    private String structure;

}

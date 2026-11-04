package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjApplicationDTO {

    @Field("subject_application_id")
    private String subject_application_id;

    @Field("subject_application_vendor")
    private String subject_application_vendor;

    @Field("subject_application_version")
    private String subject_application_version;
    public static SubjApplicationDTO fromString(String str) {
        String[] parts = str.split(", ");
        String id = parts[0].substring(parts[0].indexOf('=') + 1);
        String vendor = parts[1].substring(parts[1].indexOf('=') + 1);
        String version = parts[2].substring(parts[2].indexOf('=') + 1, parts[2].length() - 1);
        return new SubjApplicationDTO(id, vendor, version);
    }
}

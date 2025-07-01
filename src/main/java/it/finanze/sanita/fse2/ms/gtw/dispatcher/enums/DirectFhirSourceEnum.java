package it.finanze.sanita.fse2.ms.gtw.dispatcher.enums;

import lombok.Getter;

public enum DirectFhirSourceEnum {

    JSON("JSON"),
    PDF("PDF");

    @Getter
    private final String source;

    private DirectFhirSourceEnum(String inSource) {
        source = inSource;
    }
}

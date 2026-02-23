package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfidentialityCodeDto {

	private String code;
    private String display;
}

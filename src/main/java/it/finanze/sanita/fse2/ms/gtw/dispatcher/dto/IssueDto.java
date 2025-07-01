package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.IssueSeverityEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueDto {

	private IssueSeverityEnum severity;
	private String path;
	private String detail;
	 
}

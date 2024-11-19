package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class IniAuditsDto {

	private List<StatusCheckDTO> audit;
	
}

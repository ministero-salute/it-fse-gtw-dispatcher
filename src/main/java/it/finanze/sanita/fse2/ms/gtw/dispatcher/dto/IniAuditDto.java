package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto;

import java.util.Date;

import lombok.Data;


@Data
public class IniAuditDto {

	private String workflow_instance_id;
	private String eventType;
	private Date eventDate;
	private String soapRequest;
	private String soapResponse;
}

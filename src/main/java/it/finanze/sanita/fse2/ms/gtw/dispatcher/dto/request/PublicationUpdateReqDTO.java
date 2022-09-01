package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request;

import java.util.List;

import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.AttivitaClinicaEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.HealthDataFormatEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.HealthcareFacilityEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.InjectionModeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.PracticeSettingCodeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.TipoDocAltoLivEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author CPIERASC
 *
 *	Request body publication creation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicationUpdateReqDTO extends PublicationMetadataReqDTO {

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = -5437339267676346343L;

	@Schema(description = "Formato dei dati sanitari")
	private HealthDataFormatEnum healthDataFormat;

	@Schema(description = "Modalità di iniezione del CDA")
	private InjectionModeEnum mode;

	@Schema(description = "Identificativo repository", required = true)
	@Size(min = 0, max = 100)
	private String identificativoRep;

    
    @Builder
    public PublicationUpdateReqDTO(
    		String workflowInstanceId,
    		HealthDataFormatEnum healthDataFormat,
    		InjectionModeEnum mode,
    		String identificativoDoc,
    		String identificativoRep,
    		boolean forcePublish,
    		Boolean priorita,
    		HealthcareFacilityEnum tipologiaStruttura, 
    		List<String> attiCliniciRegoleAccesso, 
    		TipoDocAltoLivEnum tipoDocumentoLivAlto, 
    		PracticeSettingCodeEnum assettoOrganizzativo, 
    		String dataInizioPrestazione, 
    		String dataFinePrestazione, 
    		String conservazioneANorma,
    		AttivitaClinicaEnum tipoAttivitaClinica,
    		String identificativoSottomissione) {
    	super(tipologiaStruttura, attiCliniciRegoleAccesso, tipoDocumentoLivAlto, assettoOrganizzativo, dataInizioPrestazione, dataFinePrestazione, conservazioneANorma, tipoAttivitaClinica, identificativoSottomissione);
    	this.healthDataFormat = healthDataFormat;
    	this.mode = mode;
    	this.identificativoRep = identificativoRep;
    }
    
}
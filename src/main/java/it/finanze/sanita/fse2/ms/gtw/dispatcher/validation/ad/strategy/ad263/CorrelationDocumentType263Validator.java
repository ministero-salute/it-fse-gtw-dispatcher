package it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad263;

import java.util.Map;
import java.util.Set;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad263.enums.DocumentType263Enum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.MetadataValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad263.enums.TipoDocAltoLivAd263Enum;

public class CorrelationDocumentType263Validator {

	private static final Map<DocumentType263Enum, Set<TipoDocAltoLivAd263Enum>> CORRELATION_MAP =
			Map.ofEntries(
					Map.entry(DocumentType263Enum.CODE_57833_6, Set.of(TipoDocAltoLivAd263Enum.PRS)),
					Map.entry(DocumentType263Enum.CODE_60591_5, Set.of(TipoDocAltoLivAd263Enum.SUM)),
					Map.entry(DocumentType263Enum.CODE_11502_2, Set.of(TipoDocAltoLivAd263Enum.REF)),
					Map.entry(DocumentType263Enum.CODE_57829_4, Set.of(TipoDocAltoLivAd263Enum.PRS)),
					Map.entry(DocumentType263Enum.CODE_34105_7, Set.of(TipoDocAltoLivAd263Enum.LDO)),
					Map.entry(DocumentType263Enum.CODE_18842_5, Set.of(TipoDocAltoLivAd263Enum.LDO)),
					Map.entry(DocumentType263Enum.CODE_59258_4, Set.of(TipoDocAltoLivAd263Enum.VRB)),
					Map.entry(DocumentType263Enum.CODE_68604_8, Set.of(TipoDocAltoLivAd263Enum.REF)),
					Map.entry(DocumentType263Enum.CODE_11526_1, Set.of(TipoDocAltoLivAd263Enum.REF)),
					Map.entry(DocumentType263Enum.CODE_59284_0, Set.of(TipoDocAltoLivAd263Enum.CON)),
					Map.entry(DocumentType263Enum.CODE_104531_9, Set.of(TipoDocAltoLivAd263Enum.CRT)),
					Map.entry(DocumentType263Enum.CODE_57832_8, Set.of(TipoDocAltoLivAd263Enum.PRS)),
					Map.entry(DocumentType263Enum.CODE_29304_3, Set.of(TipoDocAltoLivAd263Enum.PRE)),
					Map.entry(DocumentType263Enum.CODE_11488_4, Set.of(TipoDocAltoLivAd263Enum.REF)),
					Map.entry(DocumentType263Enum.CODE_57827_8, Set.of(TipoDocAltoLivAd263Enum.ESE)),
					Map.entry(DocumentType263Enum.CODE_81223_0, Set.of(TipoDocAltoLivAd263Enum.PRE)),
					Map.entry(DocumentType263Enum.CODE_18776_5, Set.of(TipoDocAltoLivAd263Enum.PDC)),
					Map.entry(DocumentType263Enum.CODE_97500_3, Set.of(TipoDocAltoLivAd263Enum.CER)),
					Map.entry(DocumentType263Enum.CODE_87273_9, Set.of(TipoDocAltoLivAd263Enum.VAC)),
					Map.entry(DocumentType263Enum.CODE_82593_5, Set.of(TipoDocAltoLivAd263Enum.VAC)),
					Map.entry(DocumentType263Enum.CODE_97499_8, Set.of(TipoDocAltoLivAd263Enum.CER)),
					Map.entry(DocumentType263Enum.CODE_55750_4, Set.of(TipoDocAltoLivAd263Enum.SUM)),
					Map.entry(DocumentType263Enum.CODE_68814_3, Set.of(TipoDocAltoLivAd263Enum.CNT)),
					Map.entry(DocumentType263Enum.CODE_103140_0, Set.of(TipoDocAltoLivAd263Enum.TAC)),
					Map.entry(DocumentType263Enum.CODE_103144_2, Set.of(TipoDocAltoLivAd263Enum.PRE)),
					Map.entry(DocumentType263Enum.CODE_103145_9, Set.of(TipoDocAltoLivAd263Enum.PRE)),
					Map.entry(DocumentType263Enum.CODE_103146_7, Set.of(TipoDocAltoLivAd263Enum.PRE)),
					Map.entry(DocumentType263Enum.CODE_103147_5, Set.of(TipoDocAltoLivAd263Enum.PRE)),
					Map.entry(DocumentType263Enum.CODE_101136_0, Set.of(TipoDocAltoLivAd263Enum.LET)),
					Map.entry(DocumentType263Enum.CODE_101134_5, Set.of(TipoDocAltoLivAd263Enum.PRO)),
					Map.entry(DocumentType263Enum.CODE_101133_7, Set.of(TipoDocAltoLivAd263Enum.CON)),
					Map.entry(DocumentType263Enum.CODE_53576_5, Set.of(TipoDocAltoLivAd263Enum.TAC)),
					Map.entry(DocumentType263Enum.CODE_100971_1, Set.of(TipoDocAltoLivAd263Enum.COL)),
					Map.entry(DocumentType263Enum.CODE_101881_1, Set.of(TipoDocAltoLivAd263Enum.SUM)),
					Map.entry(DocumentType263Enum.CODE_108276_7, Set.of(TipoDocAltoLivAd263Enum.LET)),
					Map.entry(DocumentType263Enum.LOINC_CODE_1, Set.of(TipoDocAltoLivAd263Enum.LET)),
					Map.entry(DocumentType263Enum.LOINC_CODE_2, Set.of(TipoDocAltoLivAd263Enum.CNT)),
					Map.entry(DocumentType263Enum.CODE_75496_0, Set.of(TipoDocAltoLivAd263Enum.REF)),
					Map.entry(DocumentType263Enum.CODE_85208_7, Set.of(TipoDocAltoLivAd263Enum.REF)),
					Map.entry(DocumentType263Enum.LOINC_CODE_3, Set.of(TipoDocAltoLivAd263Enum.REF)),
					Map.entry(DocumentType263Enum.LOINC_CODE_4, Set.of(TipoDocAltoLivAd263Enum.CNT))
					);

	public static boolean isValid(DocumentType263Enum documentType, TipoDocAltoLivAd263Enum documentClass) {

		if (documentType == null || documentClass == null) {
			final ErrorResponseDTO error = ErrorResponseDTO.builder()
					.title(RestExecutionResultEnum.VALIDATOR_ERROR.getTitle())
					.type(RestExecutionResultEnum.VALIDATOR_ERROR.getType())
					.instance(RestExecutionResultEnum.VALIDATOR_ERROR.getType())
					.detail("Uno tra tipo documento o tipo documento livello alto risulta essere null").build();
			throw new MetadataValidationException(error);
		}

		Set<TipoDocAltoLivAd263Enum> correlation = CORRELATION_MAP.get(documentType);
		boolean allowed = correlation != null && correlation.contains(documentClass);
		
		if(!allowed) {
			final ErrorResponseDTO error = ErrorResponseDTO.builder()
					.title(RestExecutionResultEnum.VALIDATOR_ERROR.getTitle())
					.type(RestExecutionResultEnum.VALIDATOR_ERROR.getType())
					.instance(RestExecutionResultEnum.VALIDATOR_ERROR.getType())
					.detail("Non c'è correlazione tra tipo documento e tipo documento livello alto").build();
			throw new MetadataValidationException(error);
			
		}
		return allowed;
	}

}
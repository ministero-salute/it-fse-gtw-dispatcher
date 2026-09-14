package it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad264;

import java.util.Map;
import java.util.Set;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DocumentTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.MetadataValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad264.enums.DocumentType264Enum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad264.enums.TipoDocAltoLivAd264Enum;


public class CorrelationDocumentType264Validator {

	private static final Map<DocumentType264Enum, Set<TipoDocAltoLivAd264Enum>> CORRELATION_MAP =
			Map.ofEntries(
					Map.entry(DocumentType264Enum.CODE_57833_6, Set.of(TipoDocAltoLivAd264Enum.PRS)),
					Map.entry(DocumentType264Enum.CODE_60591_5, Set.of(TipoDocAltoLivAd264Enum.SUM)),
					Map.entry(DocumentType264Enum.CODE_11502_2, Set.of(TipoDocAltoLivAd264Enum.REF)),
					Map.entry(DocumentType264Enum.CODE_57829_4, Set.of(TipoDocAltoLivAd264Enum.PRS)),
					Map.entry(DocumentType264Enum.CODE_34105_7, Set.of(TipoDocAltoLivAd264Enum.LDO)),
					Map.entry(DocumentType264Enum.CODE_18842_5, Set.of(TipoDocAltoLivAd264Enum.LDO)),
					Map.entry(DocumentType264Enum.CODE_59258_4, Set.of(TipoDocAltoLivAd264Enum.VRB)),
					Map.entry(DocumentType264Enum.CODE_68604_8, Set.of(TipoDocAltoLivAd264Enum.REF)),
					Map.entry(DocumentType264Enum.CODE_11526_1, Set.of(TipoDocAltoLivAd264Enum.REF)),
					Map.entry(DocumentType264Enum.CODE_59284_0, Set.of(TipoDocAltoLivAd264Enum.CON)),
					Map.entry(DocumentType264Enum.CODE_104531_9, Set.of(TipoDocAltoLivAd264Enum.CRT)),
					Map.entry(DocumentType264Enum.CODE_57832_8, Set.of(TipoDocAltoLivAd264Enum.PRS)),
					Map.entry(DocumentType264Enum.CODE_60593_1, Set.of(TipoDocAltoLivAd264Enum.PRE)),
					Map.entry(DocumentType264Enum.CODE_11488_4, Set.of(TipoDocAltoLivAd264Enum.REF)),
					Map.entry(DocumentType264Enum.CODE_57827_8, Set.of(TipoDocAltoLivAd264Enum.ESE)),
					Map.entry(DocumentType264Enum.CODE_81223_0, Set.of(TipoDocAltoLivAd264Enum.PRE)),
					Map.entry(DocumentType264Enum.CODE_18776_5, Set.of(TipoDocAltoLivAd264Enum.PDC)),
					Map.entry(DocumentType264Enum.CODE_97500_3, Set.of(TipoDocAltoLivAd264Enum.CER)),
					Map.entry(DocumentType264Enum.CODE_87273_9, Set.of(TipoDocAltoLivAd264Enum.VAC)),
					Map.entry(DocumentType264Enum.CODE_82593_5, Set.of(TipoDocAltoLivAd264Enum.VAC)),
					Map.entry(DocumentType264Enum.CODE_97499_8, Set.of(TipoDocAltoLivAd264Enum.CER)),
					Map.entry(DocumentType264Enum.CODE_55750_4, Set.of(TipoDocAltoLivAd264Enum.SUM)),
					Map.entry(DocumentType264Enum.CODE_68814_3, Set.of(TipoDocAltoLivAd264Enum.CNT)),
					Map.entry(DocumentType264Enum.CODE_103140_0, Set.of(TipoDocAltoLivAd264Enum.TAC)),
					Map.entry(DocumentType264Enum.CODE_103144_2, Set.of(TipoDocAltoLivAd264Enum.PRE)),
					Map.entry(DocumentType264Enum.CODE_103145_9, Set.of(TipoDocAltoLivAd264Enum.PRE)),
					Map.entry(DocumentType264Enum.CODE_103146_7, Set.of(TipoDocAltoLivAd264Enum.PRE)),
					Map.entry(DocumentType264Enum.CODE_103147_5, Set.of(TipoDocAltoLivAd264Enum.PRE)),
					Map.entry(DocumentType264Enum.CODE_101136_0, Set.of(TipoDocAltoLivAd264Enum.LET)),
					Map.entry(DocumentType264Enum.CODE_101134_5, Set.of(TipoDocAltoLivAd264Enum.PRO)),
					Map.entry(DocumentType264Enum.CODE_101133_7, Set.of(TipoDocAltoLivAd264Enum.CON)),
					Map.entry(DocumentType264Enum.CODE_53576_5, Set.of(TipoDocAltoLivAd264Enum.TAC)),
					Map.entry(DocumentType264Enum.CODE_100971_1, Set.of(TipoDocAltoLivAd264Enum.COL)),
					Map.entry(DocumentType264Enum.CODE_101881_1, Set.of(TipoDocAltoLivAd264Enum.SUM)),
					Map.entry(DocumentType264Enum.CODE_108276_7, Set.of(TipoDocAltoLivAd264Enum.LET)),
					Map.entry(DocumentType264Enum.CODE_111490_9, Set.of(TipoDocAltoLivAd264Enum.LET)),
					Map.entry(DocumentType264Enum.CODE_112062_5, Set.of(TipoDocAltoLivAd264Enum.CNT)),
					Map.entry(DocumentType264Enum.CODE_75496_0, Set.of(TipoDocAltoLivAd264Enum.REF)),
					Map.entry(DocumentType264Enum.CODE_85208_7, Set.of(TipoDocAltoLivAd264Enum.REF)),
					Map.entry(DocumentType264Enum.CODE_112063_3, Set.of(TipoDocAltoLivAd264Enum.REF)),
					Map.entry(DocumentType264Enum.CODE_111827_2, Set.of(TipoDocAltoLivAd264Enum.CNT))
					);

	public static boolean isValid(DocumentType264Enum documentType, TipoDocAltoLivAd264Enum documentClass) {

		if (documentType == null || documentClass == null) {
			final ErrorResponseDTO error = ErrorResponseDTO.builder()
					.title(RestExecutionResultEnum.VALIDATOR_ERROR.getTitle())
					.type(RestExecutionResultEnum.VALIDATOR_ERROR.getType())
					.instance(RestExecutionResultEnum.VALIDATOR_ERROR.getType())
					.detail("Uno tra tipo documento o tipo documento livello alto risulta essere null").build();
			throw new MetadataValidationException(error);
		}

		Set<TipoDocAltoLivAd264Enum> correlation = CORRELATION_MAP.get(documentType);
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
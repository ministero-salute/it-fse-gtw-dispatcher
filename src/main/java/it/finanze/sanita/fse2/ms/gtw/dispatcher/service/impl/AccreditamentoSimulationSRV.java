/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * Copyright (C) 2023 Ministero della Salute
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IStatusCheckClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.CDACFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.AccreditamentoSimulationDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.AccreditamentoPrefixEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IAccreditamentoSimulationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.ICdaSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IEngineSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CdaUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.PDFUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AccreditamentoSimulationSRV implements IAccreditamentoSimulationSRV {


	@Autowired
	private ICdaSRV cdaSRV;

	@Autowired
	private IEngineSRV engines;

	@Autowired
	private CDACFG cdaCFG;
	
	@Autowired
	private IStatusCheckClient statusCheckClient;

	@Override
	public AccreditamentoSimulationDTO runSimulation(final String idDocumento, final byte[] pdf, final EventTypeEnum eventType) {
		AccreditamentoSimulationDTO output = null;
		
		if(EventTypeEnum.REPLACE.equals(eventType) && !idDocumento.equals("TRIAL_ID_SKIP_ACCREDITATIONCHECK")) {
			statusCheckClient.callSearchEventByIdDocumento(idDocumento);
		}
		
		AccreditamentoPrefixEnum prefixEnum = AccreditamentoPrefixEnum.getStartWith(idDocumento);
		if(prefixEnum!=null) {
			checkIdDocumento(idDocumento, prefixEnum);

			
			switch (prefixEnum) {
			case CRASH_TIMEOUT:
				simulateTimeout();
				break;
			case SKIP_VALIDATION:
			case CRASH_WF_EDS:
			case CRASH_INI:
			case CRASH_EDS:
				String wii = simulateSkipValidation(pdf);
				output = new AccreditamentoSimulationDTO(wii);  
				break;
			default:
				break;
			}
		}
		return output;
	}

	private void checkIdDocumento(final String idDocumento, AccreditamentoPrefixEnum prefixEnum) {
		boolean val = idDocumento.split(prefixEnum.getPrefix()).length > 1;
		if(!val) {
			final ErrorResponseDTO error = ErrorResponseDTO.builder()
					.type(RestExecutionResultEnum.SIMULATION_EXCEPTION.getType())
					.title(RestExecutionResultEnum.SIMULATION_EXCEPTION.getTitle())
					.instance(ErrorInstanceEnum.SIMULATION_EXCEPTION.getInstance())
					.detail("Inserire una string randomica dopo l'id documento").build();
			throw new ValidationException(error);
		}
	}

	private void simulateTimeout() {
		log.info("Timeout simulated");
		throw new ConnectionRefusedException(null, "Timeout simulation");
	}

	private String simulateSkipValidation(byte[] pdf) {
		log.info("Skip validation simulation");
		String cda = PDFUtility.extractCDAFromAttachments(pdf,cdaCFG.getCdaAttachmentName());
		Document docT = Jsoup.parse(cda);
		String templateIdRoot = docT.select("templateid").get(0).attr("root");
		String workflowInstanceId = CdaUtility.getWorkflowInstanceId(docT);
		// If we skip validation, we won't have transformID and engineID in our
		// ValidationInfoDTO, so we have to pick them by ourselves
		// emulating the gtw-validator mechanism.
		// getKey() returns engineID, getValue() returns transformID
		Pair<String, String> id = engines.getStructureObjectID(templateIdRoot);
		final String hashedCDA = StringUtility.encodeSHA256B64(cdaWithoutLegalAuthenticator(cda));
		cdaSRV.create(hashedCDA, workflowInstanceId,id.getValue() , id.getKey());
		return workflowInstanceId;
	}

	private String cdaWithoutLegalAuthenticator(final String cda) {
		Document doc = Jsoup.parse(cda, "", Parser.xmlParser());
		Element authenticator = doc.selectFirst("LegalAuthenticator");
		if(authenticator != null) {
			authenticator.forEach(e -> {
				// Reset attributes
				e.attributes().forEach(a -> a.setValue("PLACEHOLDER"));
				// Reset values on node without children and with text
				if(e.children().isEmpty() && !e.text().isEmpty()) e.text("PLACEHOLDER");
			});
		} else {
			log.warn("Unable to calculate cda-hash correctly because LegalAuthenticator doesn't exists");
		}
		return doc.toString();
	}
}

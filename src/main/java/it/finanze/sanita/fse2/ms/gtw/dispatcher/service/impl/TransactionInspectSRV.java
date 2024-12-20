
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

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IStatusCheckClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.IniAuditsDto;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.StatusCheckDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.TransactionInspectResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.ITransactionInspectSRV;

@Service
public class TransactionInspectSRV implements ITransactionInspectSRV {

	@Autowired
	private IStatusCheckClient statusCheckClient;
	 
	@Autowired
	private IIniClient iniClient;
	
	@Autowired
	private IConfigSRV configSRV;
	

	@Override
	public TransactionInspectResDTO callSearchEventByWorkflowInstanceId(final String workflowInstanceId) {
		TransactionInspectResDTO out = statusCheckClient.callSearchEventByWorkflowInstanceId(workflowInstanceId);
		
		String issuerBenchmark =  (out!=null && out.getTransactionData()!=null && !out.getTransactionData().isEmpty()) ? out.getTransactionData().get(0).getIssuer() : "";
		
		if(configSRV.isAuditIniEnable() && !issuerBenchmark.contains(Constants.App.BENCHMARK_ISSUER)) {
			IniAuditsDto auditsIniDto = iniClient.callSearchEventByWorkflowInstanceId(workflowInstanceId);
			if(auditsIniDto!=null && !auditsIniDto.getAudit().isEmpty()) {
				Objects.requireNonNull(out).getTransactionData().addAll(auditsIniDto.getAudit());
			}
		}
		Objects.requireNonNull(out).setTransactionData(out.getTransactionData().stream().sorted(Comparator.comparing(StatusCheckDTO::getEventDate)).collect(Collectors.toList()));
		return out;
	}

	@Override
	public TransactionInspectResDTO callSearchEventByTraceId(final String traceId) {
		return statusCheckClient.callSearchEventByTraceId(traceId);
	}

	@Override
	public TransactionInspectResDTO callSearchEventByIdDocumento(final String idDocumento) {
		return statusCheckClient.callSearchEventByTraceId(idDocumento);
	}
}

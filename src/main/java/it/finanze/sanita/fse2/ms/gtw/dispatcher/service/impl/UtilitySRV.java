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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.AnaClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.ValidationCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationCfResDto;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CfUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;

@Service
public class UtilitySRV extends AbstractService {
	
	@Autowired
	private transient ValidationCFG validationCfg;
	
	@Autowired
	private MicroservicesURLCFG msUrlCfg;
	
	@Autowired
	private AnaClient anaClient;

	public boolean isValidCf(final String fiscalCode) {
		boolean out = false;
		
		if(!StringUtility.isNullOrEmpty(msUrlCfg.getAnaHost())) {
			ValidationCfResDto cfValidationResult = anaClient.validateByFiscalCode(fiscalCode);
			out = cfValidationResult.isValid();
		} else {
			if (fiscalCode != null) {
				if (Boolean.TRUE.equals(validationCfg.getAllowSpecialFiscalCodes())) {
					out = CfUtility.validaCF(fiscalCode) == CfUtility.CF_OK_16
							|| CfUtility.validaCF(fiscalCode) == CfUtility.CF_OK_11
							|| CfUtility.validaCF(fiscalCode) == CfUtility.CF_ENI_OK
							|| CfUtility.validaCF(fiscalCode) == CfUtility.CF_STP_OK;
				} else {
					out = (fiscalCode.length() == 16 && CfUtility.validaCF(fiscalCode) == CfUtility.CF_OK_16);
				}
			} else {
				out = false;
			}
		}
		
		return out;
	}
}

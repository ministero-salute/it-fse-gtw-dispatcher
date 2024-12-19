
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

import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.SignatureValidationDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.ISignSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.SignerUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;

@Service
public class SignSRV implements ISignSRV{

	
	@Override
	public String checkPades(final byte[] pdf,final EventTypeEnum eventTypeEnum) {
		String out = "";
		
 
		boolean checkIsSigned = SignerUtility.isSigned(pdf);
		if(!checkIsSigned) {
			out = "Il pdf non risulta firmato";
		}

		SignatureValidationDTO esitoSign = SignerUtility.validate(pdf);
		if(StringUtility.isNullOrEmpty(out) && Boolean.FALSE.equals(esitoSign.getStatus())) {
			out = "La firma del pdf non risulta valida";
		}
		
		if(!StringUtility.isNullOrEmpty(out)) {
			ErrorResponseDTO error = ErrorResponseDTO.builder()
					.type(ErrorInstanceEnum.SIGN_EXCEPTION.getInstance())
					.detail(out)
					.instance(ErrorInstanceEnum.SIGN_EXCEPTION.getInstance())
					.title(ErrorInstanceEnum.SIGN_EXCEPTION.getInstance()).build();
			throw new ValidationException(error);
		}
		
		return out;
	}

}

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

import java.util.ArrayList;
import java.util.List;

import org.bson.BsonBinary;
import org.bson.BsonString;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.mongo.MongoDatabaseCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.mongo.MongoPropertiesCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ResourceDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.IniEdsInvocationETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IIniEdsInvocationRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IIniEdsInvocationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IniEdsInvocationSRV implements IIniEdsInvocationSRV {


	@Autowired
	private IIniEdsInvocationRepo iniInvocationRepo;

	@Autowired
	private MongoDatabaseCFG mongoCfg;
	
	@Autowired
	private MongoPropertiesCFG mongoPropsCfg;

	@Override
	public Boolean insert(final String workflowInstanceId, final ResourceDTO fhirResourceDTO, final JWTPayloadDTO jwtPayloadToken) {
		Boolean output = false;
		try {

			IniEdsInvocationETY etyToSave = buildETY(workflowInstanceId, fhirResourceDTO.getBundleJson(), fhirResourceDTO.getSubmissionSetEntryJson(),
					fhirResourceDTO.getDocumentEntryJson(), StringUtility.toJSON(jwtPayloadToken), null, jwtPayloadToken.getIss());

			etyToSave = iniInvocationRepo.insert(etyToSave);
			output = !StringUtility.isNullOrEmpty(etyToSave.getId());
		} catch(Exception ex) {
			log.error("Error while insert ini invocation item : " , ex);
			throw new BusinessException("Error while insert ini invocation item : " , ex);
		}
		return output; 
	}

	private IniEdsInvocationETY buildETY(final String workflowInstanceId, final String bundleJson, final String submissionSetEntryJson,
			final String documentEntryJson, final String tokenEntryJson, final String rifIni, final String issuer) {
		IniEdsInvocationETY out = new IniEdsInvocationETY();

		out.setWorkflowInstanceId(workflowInstanceId);
		if(!StringUtility.isNullOrEmpty(bundleJson)) {
			out.setData(Document.parse(bundleJson));	
		}

		out.setIssuer(issuer);
		if (!StringUtility.isNullOrEmpty(rifIni)) {
			out.setRiferimentoIni(rifIni);
		}

		List<Document> metadata = new ArrayList<>();
		
		Document submissionSetEntryDoc = null;
		Document documentEntryDoc = null;
		Document tokenEntry = null;
		if(!mongoPropsCfg.isEncryptionEnabled()){
			submissionSetEntryDoc = new Document("submissionSetEntry" ,Document.parse(submissionSetEntryJson));
			documentEntryDoc = new Document("documentEntry" ,Document.parse(documentEntryJson));
			tokenEntry = new Document("tokenEntry", new Document("payload",Document.parse(tokenEntryJson)));
		} else {
			BsonBinary encryptedSubmissionSetEntry = mongoCfg.encrypt(new BsonString(submissionSetEntryJson));
			submissionSetEntryDoc = new Document("submissionSetEntry", encryptedSubmissionSetEntry);
			BsonBinary encryptedDocumentEntry = mongoCfg.encrypt(new BsonString(documentEntryJson));
			documentEntryDoc = new Document("documentEntry" ,encryptedDocumentEntry);
			BsonBinary encryptedTokenEntry = mongoCfg.encrypt(new BsonString(tokenEntryJson));
			tokenEntry = new Document("tokenEntry", new Document("payload",encryptedTokenEntry));	
		}
		
		metadata.add(submissionSetEntryDoc);
		metadata.add(documentEntryDoc);
		metadata.add(tokenEntry);

		out.setMetadata(metadata);
		return out;
	}

	@Override
	public Boolean replace(String workflowInstanceId, ResourceDTO fhirResourceDTO, JWTPayloadDTO jwtPayloadToken, final String identificativoDocumento) {
		Boolean output = false;
		try {
			IniEdsInvocationETY etyToSave = buildETY(workflowInstanceId, fhirResourceDTO.getBundleJson(), fhirResourceDTO.getSubmissionSetEntryJson(),
					fhirResourceDTO.getDocumentEntryJson(), StringUtility.toJSON(jwtPayloadToken), identificativoDocumento,
					jwtPayloadToken.getIss());
			etyToSave = iniInvocationRepo.insert(etyToSave);
			output = !StringUtility.isNullOrEmpty(etyToSave.getId());
		} catch(Exception ex) {
			log.error("Error in replace while insert ini invocation item : " , ex);
			throw new BusinessException("Error in replace while insert ini invocation item : " , ex);
		}
		return output; 
	}
}

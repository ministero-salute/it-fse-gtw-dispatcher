
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.utility;

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.SignatureInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.SignatureValidationDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignerUtility {

	/******************************************************
	*	VALIDATE PADES SIGNATURE
	*******************************************************/
	@SuppressWarnings("unchecked")
	public static SignatureValidationDTO validate(byte[] file) {
		SignatureValidationDTO output = null;
		Boolean status = true;
		List<SignatureInfoDTO> signatures = new ArrayList<>();
		try {
			PDDocument documentCDA = PDDocument.load(new ByteArrayInputStream(file));
			PDDocumentCatalog docCatalog = documentCDA.getDocumentCatalog();
			PDAcroForm acroForm = docCatalog.getAcroForm();
			
			if(acroForm!=null && acroForm.getFields()!=null) {
				for (PDField field:acroForm.getFields()) {
					if (field instanceof PDSignatureField) {
						PDSignatureField pf = (PDSignatureField)field;
						
						PDSignature signature = pf.getSignature();
						byte[] signatureAsBytes = signature.getContents(file);
						byte[] signedContentAsBytes = signature.getSignedContent(file);
						
						CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(signedContentAsBytes), signatureAsBytes);
						SignerInformation signerInfo = (SignerInformation) cms.getSignerInfos().getSigners().iterator().next();
						
						X509CertificateHolder certHolder = (X509CertificateHolder) cms.getCertificates().getMatches(signerInfo.getSID()).iterator().next();
						SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder().setProvider(new BouncyCastleProvider()).build(certHolder);
						X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);
						SignatureInfoDTO info = SignatureInfoDTO.builder().
								principal(cert.getIssuerX500Principal()).
								notBefore(cert.getNotBefore()).
								notAfter(cert.getNotAfter()).
								contactInfo(pf.getSignature().getContactInfo()).
								fieldType(field.getFieldType()).
								fullyQualifiedName(field.getFullyQualifiedName()).
								location(pf.getSignature().getLocation()).
								name(pf.getSignature().getName()).
								reason(pf.getSignature().getReason()).
								signDate(pf.getSignature().getSignDate().getTime()).
								encrypAlgOID(signerInfo.getEncryptionAlgOID()).
								digestAlgOID(signerInfo.getDigestAlgOID()).
								valid(signerInfo.verify(verifier)).
								build();
						signatures.add(info);
						status = info.getValid();
					}
				}
			} else {
				status = false;
			}
		} catch(Exception ex) {
			log.error("Error while validate signature : " , ex);
			status = false;
		}
		output = SignatureValidationDTO.builder().status(status).signatures(signatures).build(); 
        return output;
	}
     
	public static boolean isSigned(byte[] pdf) {
		boolean hasAnySignature = false;
		try(PDDocument document = PDDocument.load(pdf);) {
			hasAnySignature = !document.getSignatureDictionaries().isEmpty();
		} catch (Exception e) {
			log.error("Error while searching signature for file with name", e);
			throw new BusinessException("Error while searching signature for file with name", e);
		}

		return hasAnySignature;
	}

}
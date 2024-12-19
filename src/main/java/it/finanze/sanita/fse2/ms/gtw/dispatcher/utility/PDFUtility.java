
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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

import com.lowagie.text.pdf.PRStream;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfIndirectReference;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfString;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.AttachmentDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PDFUtility {

	
	public static String extractCDAFromAttachments(final byte[] pdf, final String cdaAttachmentName) {
	    String out = null;
	    final Map<String, AttachmentDTO> attachments = extractAttachments(pdf);
	    if (!attachments.isEmpty()) {
	        for (Entry<String, AttachmentDTO> att:attachments.entrySet()) {
	            if (cdaAttachmentName.equals(att.getValue().getName())||cdaAttachmentName.equals(att.getValue().getFileName())) {
	                out = PDFUtility.detectCharsetAndExtract(att.getValue().getContent());
	                break;
	            }
	        }
	    }
	    return out;
	}
	

	private static Map<String, AttachmentDTO> extractAttachments(byte[] bytePDF) {
		Map<String,AttachmentDTO> out = new HashMap<>();
	    try (PDDocument document = PDDocument.load(bytePDF)) {
	        PDDocumentCatalog catalog = document.getDocumentCatalog();
	        PDDocumentNameDictionary names = catalog.getNames();
	        PDEmbeddedFilesNameTreeNode embeddedFiles = names.getEmbeddedFiles();

	        if (embeddedFiles != null) {
	            Map<String, PDComplexFileSpecification> embeddedFileNames = embeddedFiles.getNames();
	            if (embeddedFileNames != null) {
	            	out.putAll(extractFiles(embeddedFileNames));
	            } else {
	            	out.putAll(extractFilesFromKids(embeddedFiles.getKids()));
	            }
	        }
	    } catch (Exception e) {
	        log.warn("Errore in fase di estrazione allegati da pdf.", e);
	    }
	    
	    return out;
	}

	private static Map<String, AttachmentDTO> extractFiles(Map<String, PDComplexFileSpecification> embeddedFileNames) throws IOException {
	    Map<String, AttachmentDTO> attachments = new HashMap<>();
	    for (Map.Entry<String, PDComplexFileSpecification> entry : embeddedFileNames.entrySet()) {
	        AttachmentDTO attachment = createAttachmentDTO(entry);
	        attachments.put(entry.getKey().toLowerCase(), attachment);
	    }
	    return attachments;
	}

	private static Map<String, AttachmentDTO> extractFilesFromKids(List<PDNameTreeNode<PDComplexFileSpecification>> embeddedFileKids) throws IOException {
	    Map<String, AttachmentDTO> attachments = new HashMap<>();
	    for (PDNameTreeNode<PDComplexFileSpecification> kid : embeddedFileKids) {
	        Map<String, PDComplexFileSpecification> kidFiles = kid.getNames();
	        if (kidFiles != null) {
	            attachments.putAll(extractFiles(kidFiles));
	        }
	    }
	    return attachments;
	}

	private static AttachmentDTO createAttachmentDTO(Map.Entry<String, PDComplexFileSpecification> entry) throws IOException {
	    PDComplexFileSpecification fileSpec = entry.getValue();
	    return AttachmentDTO.builder()
	            .fileName(fileSpec.getFilename())
	            .name(entry.getKey())
	            .mimeType(fileSpec.getEmbeddedFile().getSubtype())
	            .content(fileSpec.getEmbeddedFile().toByteArray())
	            .build();
	}



	public static String unenvelopeA2(byte[] pdf) {
		String out = null;
		String errorMsg = "No CDA found.";
		try (PdfReader pdfReader = new PdfReader(pdf)) {
			PdfDictionary catalog = pdfReader.getCatalog();
			if (catalog != null) {
				PdfDictionary names = catalog.getAsDict(PdfName.NAMES);
				if (names != null) {
					PdfIndirectReference xfaResourcesIndirect = names.getAsIndirectObject(new PdfName("XFAResources"));
					if (xfaResourcesIndirect != null) {
						PdfDictionary xfaResourcesDictionary = (PdfDictionary) pdfReader.getPdfObject(xfaResourcesIndirect.getNumber());
						if (xfaResourcesDictionary != null) {
							PdfArray pdfArrayDataset = (PdfArray) xfaResourcesDictionary.get(PdfName.NAMES);
							if (pdfArrayDataset != null) {
								if (pdfArrayDataset.size() >= 2) {
									PdfString pdfStringDataset = (PdfString) pdfArrayDataset.getPdfObject(0);
									if (pdfStringDataset != null) {
										String datasets = new String(pdfStringDataset.getBytes());
										if ("datasets".equals(datasets)) {
											PdfIndirectReference cdaIndirect = (PdfIndirectReference) pdfArrayDataset.getPdfObject(1);
											if (cdaIndirect != null) {
												PRStream cdaPRStream = (PRStream) pdfReader.getPdfObject(cdaIndirect.getNumber());
												if (cdaPRStream != null) {
													out = new String(PdfReader.getStreamBytes(cdaPRStream), StandardCharsets.UTF_8);
													org.jsoup.nodes.Document doc = Jsoup.parse(out, "", Parser.xmlParser());
													out = doc.select("ClinicalDocument").first().toString();
												} else {
													errorMsg = "PRStream cdaPRStream [element 1] is null";
												}
											} else {
												errorMsg = "PdfIndirectReference cdaIndirect [element 1] is null";
											}
										} else {
											errorMsg = "PdfString pdfStringDataset [element 0] String content is not equals datasets";
										}
									} else {
										errorMsg = "PdfString pdfStringDataset [element 0] is null";
									}
								} else {
									errorMsg = "PdfArray pdfArrayDataset has less than 2 elements";
								}
							} else {
								errorMsg = "PdfArray pdfArrayDataset is null";
							}
						} else {
							errorMsg = "PdfDictionary xfaResourcesDictionary is null";
						}
					} else {
						errorMsg = "PdfIndirectReference xfaResourcesIndirect inside Names is null";
					}
				} else {
					errorMsg = "PdfDictionary names inside catalog is null";
				}
			} else {
				errorMsg = "PdfDictionary catalog is null";
			}
		} catch (Exception e) {
			log.warn("Errore in fase di recupero CDA da risorsa.", e);
		} finally {
			log.warn(errorMsg);
		}
		return out;
	}

	public static boolean isPdf(byte[] pdf) {
		boolean out = false;
		if (pdf!=null && pdf.length > 4) {
			byte[] magicNumber = Arrays.copyOf(pdf, 4);
			String strMagicNumber = new String(magicNumber);
			out = strMagicNumber.equals("%PDF");
		}
		return out;
	}

	public static String detectCharsetAndExtract(byte[] bytes) {
		Charset detectedCharset = StandardCharsets.UTF_8;
		try {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
			final XMLStreamReader xmlStreamReader = factory.createXMLStreamReader(new ByteArrayInputStream(bytes)); 
			final String fileEncoding = xmlStreamReader.getEncoding(); 
			detectedCharset = Charset.forName(fileEncoding);

			return new String(bytes, detectedCharset);
		} catch (Exception ex) {
			log.error(String.format("Error while reading extracted CDA using detected encode: %s", detectedCharset), ex);
			throw new BusinessException(String.format("Error while reading extracted CDA using detected encode: %s", detectedCharset), ex);
		}
	}
}

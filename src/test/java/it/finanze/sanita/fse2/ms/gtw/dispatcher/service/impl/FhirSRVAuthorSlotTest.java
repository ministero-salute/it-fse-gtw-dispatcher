package it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.AuthorSlotDTO;

class FhirSRVAuthorSlotTest {

	private static AuthorSlotDTO build(String cda) throws Exception {
		Method m = FhirSRV.class.getDeclaredMethod("buildAuthorSlotDTO", String.class, String.class, org.jsoup.nodes.Document.class);
		m.setAccessible(true);
		return (AuthorSlotDTO) m.invoke(null, "INST", "ROLE", Jsoup.parse(cda));
	}

	@Test
	void authorIdWithoutPrefix() throws Exception {
		AuthorSlotDTO out = build("<ClinicalDocument xmlns='urn:hl7-org:v3'><author><assignedAuthor>"
				+ "<id root='2.16.840.1.113883.2.9.4.3.2' extension='RSSMRA80A01H501U'/></assignedAuthor></author></ClinicalDocument>");
		assertEquals("RSSMRA80A01H501U^^^^^^^^&2.16.840.1.113883.2.9.4.3.2&ISO", out.getAuthor());
	}

	@Test
	void authorIdWithHl7Prefix() throws Exception {
		AuthorSlotDTO out = build("<ClinicalDocument xmlns='urn:hl7-org:v3' xmlns:hl7='urn:hl7-org:v3'><author><assignedAuthor>"
				+ "<hl7:id root='2.16.840.1.113883.2.9.4.3.2' extension='RSSMRA80A01H501U'/></assignedAuthor></author></ClinicalDocument>");
		assertEquals("RSSMRA80A01H501U^^^^^^^^&2.16.840.1.113883.2.9.4.3.2&ISO", out.getAuthor());
		assertEquals("INST", out.getAuthorInstitution());
		assertEquals("ROLE", out.getAuthorRole());
	}

	@Test
	void authorIdMissing() throws Exception {
		assertNull(build("<ClinicalDocument><author><assignedAuthor/></author></ClinicalDocument>").getAuthor());
	}
}

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtility {
	
	public static long getDifferenceDays(Date d1, Date d2) {
		long diff = d2.getTime() - d1.getTime();
		return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
	}

	public static Date addDay(final Date date, final Integer nDays) {
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(date);
			c.add(Calendar.DATE, nDays);
		} catch(Exception ex) {
			log.error("Error while perform addDay : " , ex);
			throw new BusinessException("Error while perform addDay : " , ex);
		}
		return c.getTime();

	}

	public static boolean isValidDateFormat(String dateStr, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setLenient(false);  
		try {
			sdf.parse(dateStr); 
			return true; 
		} catch (Exception e) {
			log.error("Error while perform isValidDate: " , e);
			return false; 
		}
	}
	
 
//	public static String convertDateCda(String data) {
//        SimpleDateFormat sdfInput;
//        if (data.contains("+") || data.contains("-")) {
//            sdfInput = new SimpleDateFormat("yyyyMMddHHmmssX");  
//        } else {
//            sdfInput = new SimpleDateFormat(INI_DATE_PATTERN);
//        }
//        sdfInput.setTimeZone(TimeZone.getTimeZone("UTC"));
//        
//        Date parsedDate = null;
//		try {
//			parsedDate = sdfInput.parse(data);
//		} catch (ParseException e) {
//			log.error("Error while perform convertDateCda",e);
//			throw new BusinessException(e);
//		}
//		
//        SimpleDateFormat sdfOutput = new SimpleDateFormat(INI_DATE_PATTERN);
//        sdfOutput.setTimeZone(TimeZone.getTimeZone("UTC"));
//        return sdfOutput.format(parsedDate);
//    }
	
	
	private static final String UTC = "UTC";

    // Pattern di date CDA/FHIR più comuni (immutabili)
    private static final List<String> SUPPORTED_DATE_PATTERNS = List.of("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss", "yyyyMMddHHmmssX", "yyyyMMddHHmmssZ", "yyyyMMddHHmmss", "yyyyMMdd");

    public static final String INI_DATE_PATTERN = "yyyyMMddHHmmss";

    public static String convertDateCda(String data) {
        if (data == null || data.trim().isEmpty()) {
            log.warn("convertDateCda: data nulla o vuota");
            return null;
        }

        Date parsedDate = null;

        for (String pattern : SUPPORTED_DATE_PATTERNS) {
            try {
                SimpleDateFormat sdfInput = new SimpleDateFormat(pattern);
                sdfInput.setLenient(false);
                parsedDate = sdfInput.parse(data);
                if (parsedDate != null) break; // trovato il formato corretto
            } catch (ParseException ignored) {
                // continua col prossimo formato
            }
        }

        if (parsedDate == null) {
            log.error("convertDateCda: formato data non riconosciuto [{}]", data);
            throw new BusinessException("Formato data non valido: " + data);
        }

        SimpleDateFormat sdfOutput = new SimpleDateFormat(INI_DATE_PATTERN);
        sdfOutput.setTimeZone(TimeZone.getTimeZone(UTC));
        return sdfOutput.format(parsedDate);
    }

 
}

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

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.Misc.INI_DATE_PATTERN;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

	public static String convertDateCda(String data) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat(INI_DATE_PATTERN);

		if (data.length() == INI_DATE_PATTERN.length() + 5) { 
			sdf = new SimpleDateFormat(INI_DATE_PATTERN + "Z");
			data = data.substring(0, INI_DATE_PATTERN.length()) + data.substring(INI_DATE_PATTERN.length()).replace(":", ""); //Se nel fusoorario mi vengono passati i : li rimuovo
		} 

		sdf.setTimeZone(TimeZone.getTimeZone("GMT")); 
		return new SimpleDateFormat(INI_DATE_PATTERN).format(sdf.parse(data));
	}

}

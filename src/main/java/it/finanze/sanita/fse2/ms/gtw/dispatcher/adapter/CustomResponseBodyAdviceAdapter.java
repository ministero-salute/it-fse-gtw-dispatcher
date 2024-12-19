
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.adapter;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.audit.AuditManager;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class CustomResponseBodyAdviceAdapter implements ResponseBodyAdvice<Object> {

	@Autowired
	private IConfigSRV config;

	@Autowired
	private AuditManager manager;

	@Override
	public boolean supports(MethodParameter param, Class<? extends HttpMessageConverter<?>> clazz) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(
		Object body,
		MethodParameter param,
		MediaType type,
		Class<? extends HttpMessageConverter<?>> clazz,
		ServerHttpRequest req,
		ServerHttpResponse res
	) {
		if (
			req instanceof ServletServerHttpRequest &&
			res instanceof ServletServerHttpResponse
		) {
			try {
				if (config.isAuditEnable()) {
					// Cast to HTTP request
					ServletServerHttpRequest out = (ServletServerHttpRequest) req;
					// Invoke processor
					manager.process(out.getServletRequest(), body);
				}
			} catch(Exception ex) {
				log.error("Unable to invoke before-body write ", ex);
				throw new BusinessException("Unable to invoke before-body write " + ex);
			}
		}

		return body;
	}
}

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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class CustomSwaggerCFG {

    @Value("${docs.info.api-id}")
	private String apiId;

	@Value("${docs.info.summary}")
	private String apiSummary;

    @Value("${docs.info.title}")
    private String title;

    @Value("${info.app.version}")
    private String version;

    @Value("${docs.info.description}")
    private String description;

    @Value("${docs.info.termsOfService}")
    private String termsOfService;

    @Value("${docs.info.contact.name}")
    private String contactName;

    @Value("${docs.info.contact.url}")
    private String contactUrl;

    @Value("${server.port}")
    private Integer port;

    @Value("${validation.file-max-size}")
    private Integer fileMaxLength;

}

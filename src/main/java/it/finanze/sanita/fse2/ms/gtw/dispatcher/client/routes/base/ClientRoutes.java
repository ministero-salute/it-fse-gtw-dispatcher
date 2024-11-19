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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.base;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class ClientRoutes {

    @NoArgsConstructor(access = PRIVATE)
    public static final class Ini {
        // COMMON
        public static final String IDENTIFIER_MS = "ini";
        public static final String IDENTIFIER = "[INI]";
        // ENDPOINT
        public static final String API_VERSION = "v1";
        public static final String DELETE_PATH = "ini-delete";
        public static final String UPDATE_PATH = "ini-update";
        public static final String REFERENCE_PATH = "get-reference";
        public static final  String METADATA_PATH = "get-merged-metadati";
    }

    @NoArgsConstructor(access = PRIVATE)
    public static final class Config {
        // COMMON
        public static final String IDENTIFIER_MS = "cfg";
        public static final String IDENTIFIER = "[CFG]";
        // ENDPOINT
        public static final String API_VERSION = "v1";
        public static final String API_CONFIG_ITEMS = "config-items";
        public static final String API_PROPS = "props";
        public static final String API_STATUS = "status";
        public static final String API_WHOIS = "whois";
        // QP
        public static final String QP_TYPE = "type";
        public static final String QP_PROPS = "props";
        // VALUES
        public static final String PROPS_NAME_AUDIT_ENABLED = "audit-enabled";
        public static final String PROPS_NAME_CONTROL_LOG_ENABLED = "control-log-persistence-enabled";
        public static final String PROPS_NAME_ISSUER_CF = "issuer-cf-cleaning";
        public static final String PROPS_NAME_AUDIT_INI_ENABLED = "audit-ini-enabled";
        public static final String PROPS_NAME_SUBJECT = "subject-cleaning";
        public static final String PROPS_NAME_REMOVE_EDS_ENABLE = "remove-eds-enabled";


    }

}

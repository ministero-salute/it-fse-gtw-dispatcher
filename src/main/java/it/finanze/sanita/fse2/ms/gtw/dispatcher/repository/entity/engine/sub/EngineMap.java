
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.engine.sub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EngineMap {

    public static final String FIELD_OID = "oid";
    public static final String FIELD_ROOT = "template_id_root";
    public static final String FIELD_URI = "uri";
    public static final String FIELD_VERSION = "version";


    @Field(FIELD_OID)
    private String oid;

    @Field(FIELD_ROOT)
    private List<String> root;

    @Field(FIELD_URI)
    private String uri;

    @Field(FIELD_VERSION)
    private String version;

}

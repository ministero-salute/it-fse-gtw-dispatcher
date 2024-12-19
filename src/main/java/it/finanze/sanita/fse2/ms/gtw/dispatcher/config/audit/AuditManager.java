
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.config.audit;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.AuditETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IAuditRepo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Component
public class AuditManager {

    @Autowired
    private List<AuditExclusion> exclusions;

    @Autowired
    private List<AuditFilter> filters;

    @Autowired
    private IAuditRepo repository;

    public void process(HttpServletRequest req, Object body) {
        // Decoded request url
        String uri = uri(req);
        // If the request must be skipped
        boolean excluded = exclusions.stream().anyMatch(rule -> rule.verify(uri, req));
        // Match with appropriate filter
        if(!excluded) {
            Optional<AuditFilter> filter = filters.stream().filter(f -> f.match(req)).findFirst();
            // Check filter exists
            if(filter.isPresent()) {
                // Retrieve audit
                AuditETY entity = filter.get().apply(uri, req, body);
                // Save
                if(entity!=null){
                    repository.save(entity);
                }
            } else {
                log.debug("No filter found matching the request type");
                log.debug("Skipping audit on path: {}", uri);
            }
        } else {
            log.debug("Audit excluded on path: {}", uri);
        }
    }

    @SneakyThrows
    private String uri(HttpServletRequest req) {
        // Retrieve URI
        String uri = req.getRequestURI();
        // Decode URI request (if not null)
        if(uri != null) uri = URLDecoder.decode(uri, UTF_8.name());
        return uri;
    }

}

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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.config.audit.filters;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.BenchmarkCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.audit.AuditFilter;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.AuditETY;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;

@Component
public class DeleteFilter implements AuditFilter {
  
    @Autowired
    private BenchmarkCFG benchmarkCFG;

    @Override
    public boolean match(HttpServletRequest req) {
        return HttpMethod.DELETE.toString().equals(req.getMethod());
    }

    @Override
    public AuditETY apply(String uri, HttpServletRequest req, Object body) {

        String issuer = req.getAttribute("JWT_ISSUER")!=null ? (String)req.getAttribute("JWT_ISSUER") : "";
        
        if (benchmarkCFG.isBenchmarkEnable() && issuer.contains(Constants.App.BENCHMARK_ISSUER)) {
            return null;
        }

        AuditETY audit = new AuditETY();
        audit.setServizio(uri);
        audit.setStart_time(new Date());
        audit.setEnd_time(new Date());
        audit.setResponse(body);
        audit.setHttpMethod(req.getMethod());
        return audit;
    }
}

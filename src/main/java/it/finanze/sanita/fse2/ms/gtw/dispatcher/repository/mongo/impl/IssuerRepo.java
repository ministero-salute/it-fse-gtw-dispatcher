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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.IssuerETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IIssuerRepo;
import lombok.extern.slf4j.Slf4j;

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.IssuerETY.ISSUER_FIELD;

/**
 * Repository implementation for Issuer data access.
 */
@Repository
@Slf4j
public class IssuerRepo implements IIssuerRepo {

    @Autowired
    private MongoTemplate mongo;

    @Override
    public IssuerETY getByName(String name) {
        IssuerETY out = null;
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where(ISSUER_FIELD).is(name));
            out = mongo.findOne(query, IssuerETY.class);
        } catch (Exception ex) {
            log.error("Error while performing getByName on issuer collection for name: {}", name, ex);
            throw new BusinessException("Error while performing getByName on issuer collection", ex);
        }
        return out;
    }
}

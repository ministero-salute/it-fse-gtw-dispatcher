
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


import static it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.engine.EngineETY.FIELD_AVAILABLE;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.engine.EngineETY.FIELD_LAST_SYNC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.stereotype.Repository;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.engine.EngineETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IEngineRepo;

@Repository
public class EngineRepo implements IEngineRepo {

	@Autowired
	private MongoTemplate mongo;
 
	@Override
	public EngineETY getLatestEngine() {

		EngineETY out;

		TypedAggregation<EngineETY> q = new TypedAggregation<>(
			EngineETY.class,
			sort(DESC, FIELD_LAST_SYNC),
			match(where(FIELD_AVAILABLE).is(true)),
			limit(1)
		);

		try {
			out = mongo.aggregate(q, EngineETY.class).getUniqueMappedResult();
		} catch(Exception ex) {
			throw new BusinessException("Error while perform find structure map by name : " , ex);
		}
		return out;
	}

}
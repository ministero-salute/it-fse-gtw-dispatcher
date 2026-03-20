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

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.mongodb.client.result.DeleteResult;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.RoutingTableETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IRoutingTableRepo;
import lombok.extern.slf4j.Slf4j;

/**
 * Routing Table Repository implementation
 */
@Repository
@Slf4j
public class RoutingTableRepo implements IRoutingTableRepo {
    
    @Autowired
    private MongoTemplate mongo;
    
    @Override
    public RoutingTableETY insert(RoutingTableETY entity) {
        log.debug("Inserting routing table entry for issuer: {}", entity.getIssuer());
        try {
            entity.setInsertionDate(new Date());
            RoutingTableETY inserted = mongo.insert(entity);
            log.info("Successfully inserted routing table entry for issuer: {} with endpoint: {}", 
                entity.getIssuer(), entity.getNotificationEndpoint());
            return inserted;
        } catch (Exception ex) {
            log.error("Error inserting routing table entry for issuer: {}", entity.getIssuer(), ex);
            throw new BusinessException("Error inserting routing table entry", ex);
        }
    }
    
    @Override
    public RoutingTableETY findByIssuer(String issuer) {
        log.debug("Finding routing table entry by issuer: {}", issuer);
        try {
            Query query = new Query(Criteria.where(RoutingTableETY.FIELD_ISSUER).is(issuer));
            RoutingTableETY entity = mongo.findOne(query, RoutingTableETY.class);
            
            if (entity != null) {
                log.debug("Found routing table entry for issuer {}: endpoint={}", 
                    issuer, entity.getNotificationEndpoint());
            } else {
                log.debug("No routing table entry found for issuer: {}", issuer);
            }
            
            return entity;
        } catch (Exception ex) {
            log.error("Error finding routing table entry for issuer: {}", issuer, ex);
            throw new BusinessException("Error finding routing table entry", ex);
        }
    }
    
    @Override
    public List<RoutingTableETY> findAll() {
        log.debug("Finding all routing table entries");
        try {
            List<RoutingTableETY> entities = mongo.findAll(RoutingTableETY.class);
            log.debug("Found {} routing table entries", entities.size());
            return entities;
        } catch (Exception ex) {
            log.error("Error finding all routing table entries", ex);
            throw new BusinessException("Error finding all routing table entries", ex);
        }
    }
    
    @Override
    public RoutingTableETY update(String issuer, RoutingTableETY entity) {
        log.debug("Updating routing table entry for issuer: {}", issuer);
        try {
            Query query = new Query(Criteria.where(RoutingTableETY.FIELD_ISSUER).is(issuer));
            
            Update update = new Update();
            if (entity.getNotificationEndpoint() != null) {
                update.set(RoutingTableETY.FIELD_NOTIFICATION_ENDPOINT, entity.getNotificationEndpoint());
            }
            if (entity.getDescription() != null) {
                update.set(RoutingTableETY.FIELD_DESCRIPTION, entity.getDescription());
            }
            
            mongo.updateFirst(query, update, RoutingTableETY.class);
            
            RoutingTableETY updated = findByIssuer(issuer);
            log.info("Successfully updated routing table entry for issuer: {}", issuer);
            return updated;
        } catch (Exception ex) {
            log.error("Error updating routing table entry for issuer: {}", issuer, ex);
            throw new BusinessException("Error updating routing table entry", ex);
        }
    }
    
    @Override
    public boolean deleteByIssuer(String issuer) {
        log.debug("Deleting routing table entry for issuer: {}", issuer);
        try {
            Query query = new Query(Criteria.where(RoutingTableETY.FIELD_ISSUER).is(issuer));
            DeleteResult result = mongo.remove(query, RoutingTableETY.class);
            
            boolean deleted = result.getDeletedCount() > 0;
            if (deleted) {
                log.info("Successfully deleted routing table entry for issuer: {}", issuer);
            } else {
                log.warn("No routing table entry found to delete for issuer: {}", issuer);
            }
            
            return deleted;
        } catch (Exception ex) {
            log.error("Error deleting routing table entry for issuer: {}", issuer, ex);
            throw new BusinessException("Error deleting routing table entry", ex);
        }
    }
    
    @Override
    public boolean existsByIssuer(String issuer) {
        log.debug("Checking if routing table entry exists for issuer: {}", issuer);
        try {
            Query query = new Query(Criteria.where(RoutingTableETY.FIELD_ISSUER).is(issuer));
            boolean exists = mongo.exists(query, RoutingTableETY.class);
            log.debug("Routing table entry exists for issuer {}: {}", issuer, exists);
            return exists;
        } catch (Exception ex) {
            log.error("Error checking if routing table entry exists for issuer: {}", issuer, ex);
            throw new BusinessException("Error checking routing table entry existence", ex);
        }
    }
}
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Resolves the appropriate Affinity Domain strategy based on a reference date.
 * Uses a TreeMap with floorEntry to find the most recent strategy effective before or on the given date.
 */
@Slf4j
@Component
public class AffinityDomainStrategyResolver {

    private final NavigableMap<LocalDate, AffinityDomainStrategy> strategies = new TreeMap<>();

    @Autowired
    private List<AffinityDomainStrategy> strategyList;

    /**
     * Initialize the resolver by loading all available strategies.
     * Validates that no duplicate effective dates exist.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing Affinity Domain Strategy Resolver");
        
        for (AffinityDomainStrategy strategy : strategyList) {
            LocalDate effectiveDate = strategy.effectiveFrom();
            
            if (strategies.containsKey(effectiveDate)) {
                AffinityDomainStrategy existing = strategies.get(effectiveDate);
                throw new IllegalStateException(
                    String.format("Duplicate effective date %s found for strategies: %s and %s",
                        effectiveDate, existing.versionId(), strategy.versionId())
                );
            }
            
            strategies.put(effectiveDate, strategy);
            log.info("Registered AD strategy: version={}, effectiveFrom={}", 
                strategy.versionId(), effectiveDate);
        }
        
        log.info("Affinity Domain Strategy Resolver initialized with {} strategies", strategies.size());
    }

    /**
     * Resolves the appropriate strategy for the given reference date.
     * Uses floorEntry to find the most recent strategy that became effective on or before the date.
     * 
     * @param referenceDate The date to resolve the strategy for (typically document creationTime)
     * @return The applicable AffinityDomainStrategy
     * @throws IllegalArgumentException if no strategy is found for the given date
     */
    public AffinityDomainStrategy resolve(LocalDate referenceDate) {
        if (referenceDate == null) {
            throw new IllegalArgumentException("Reference date cannot be null");
        }
        
        Map.Entry<LocalDate, AffinityDomainStrategy> entry = strategies.floorEntry(referenceDate);
        
        if (entry == null) {
            throw new IllegalArgumentException(
                String.format("No Affinity Domain strategy found for date %s. " +
                    "The document creation date is before any registered strategy effective date.", 
                    referenceDate)
            );
        }
        
        AffinityDomainStrategy strategy = entry.getValue();
        log.debug("Resolved AD strategy for date {}: version={}, effectiveFrom={}", 
            referenceDate, strategy.versionId(), entry.getKey());
        
        return strategy;
    }

    /**
     * Returns all registered strategies for testing/debugging purposes.
     */
    public NavigableMap<LocalDate, AffinityDomainStrategy> getAllStrategies() {
        return new TreeMap<>(strategies);
    }
}

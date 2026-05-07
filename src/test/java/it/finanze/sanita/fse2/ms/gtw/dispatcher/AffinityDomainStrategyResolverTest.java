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
package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AffinityDomainStrategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AffinityDomainStrategyResolver;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad262.Ad262Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad263.Ad263Strategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Affinity Domain Strategy Resolver Test")
class AffinityDomainStrategyResolverTest {

    private AffinityDomainStrategyResolver resolver;
    
    @BeforeEach
    void setUp() {
        // Create strategies
        AffinityDomainStrategy ad262 = new Ad262Strategy();
        AffinityDomainStrategy ad263 = new Ad263Strategy();
        
        // Initialize resolver manually (simulating Spring's @PostConstruct)
        resolver = new AffinityDomainStrategyResolver();
        
        // Use Spring's ReflectionTestUtils to set the strategyList field
        ReflectionTestUtils.setField(resolver, "strategyList", Arrays.asList(ad262, ad263));
        
        // Call init() to populate the strategies map
        resolver.init();
    }

    @Test
    @DisplayName("Should resolve AD 2.6.2 for date on effective date")
    void shouldResolveAd262OnEffectiveDate() {
        // Date exactly on AD 2.6.2 effective date (2025-12-01)
        LocalDate testDate = LocalDate.of(2025, 12, 1);
        
        AffinityDomainStrategy strategy = resolver.resolve(testDate);
        
        assertNotNull(strategy);
        assertEquals("2.6.2", strategy.versionId());
    }

    @Test
    @DisplayName("Should resolve AD 2.6.2 for date between 2.6.2 and 2.6.3")
    void shouldResolveAd262ForIntermediateDate() {
        // Date between AD 2.6.2 (2025-12-01) and AD 2.6.3 (2026-03-01)
        LocalDate testDate = LocalDate.of(2026, 1, 15);
        
        AffinityDomainStrategy strategy = resolver.resolve(testDate);
        
        assertNotNull(strategy);
        assertEquals("2.6.2", strategy.versionId());
    }

    @Test
    @DisplayName("Should resolve AD 2.6.3 for date on or after 2.6.3 effective date")
    void shouldResolveAd263OnOrAfterEffectiveDate() {
        // Date on AD 2.6.3 effective date (2026-03-01)
        LocalDate testDate = LocalDate.of(2026, 3, 1);
        
        AffinityDomainStrategy strategy = resolver.resolve(testDate);
        
        assertNotNull(strategy);
        assertEquals("2.6.3", strategy.versionId());
    }

    @Test
    @DisplayName("Should resolve AD 2.6.3 for future date")
    void shouldResolveAd263ForFutureDate() {
        // Future date after AD 2.6.3
        LocalDate testDate = LocalDate.of(2027, 1, 1);
        
        AffinityDomainStrategy strategy = resolver.resolve(testDate);
        
        assertNotNull(strategy);
        assertEquals("2.6.3", strategy.versionId());
    }

    @Test
    @DisplayName("Should throw exception for date before any strategy")
    void shouldThrowExceptionForDateBeforeAnyStrategy() {
        // Date before any registered strategy
        LocalDate testDate = LocalDate.of(2020, 1, 1);
        
        assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolve(testDate);
        });
    }

    @Test
    @DisplayName("Should throw exception for null date")
    void shouldThrowExceptionForNullDate() {
        assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolve(null);
        });
    }
}


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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.config.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@ConditionalOnProperty(name = "sasl.mechanism", havingValue = "OAUTHBEARER", matchIfMissing = false)
public class KafkaHealthIndicator implements HealthIndicator {

	@Autowired
	private AdminClient client;

	@Override
	public Health health() {
		Health health = null;
		try {
			client.listTopics().listings().get();
			health = Health.up().build();
		} catch (InterruptedException e) {
			log.warn("Interrupted!", e);
			health = Health.down(e).build();
			// Restore interrupted state...
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			health = Health.down(e).build();
		}
		return health;
	}
}
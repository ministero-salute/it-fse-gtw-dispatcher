
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.ProfileUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

import javax.annotation.PostConstruct;

/**
 *	Kafka topic configuration.
 */
@Data
@Component
public class KafkaTopicCFG {

	@Autowired
	private ProfileUtility profileUtility;

	/**
	 * Topic status manager.
	 */
	@Value("${kafka.statusmanager.topic}")
	private String statusManagerTopic;
	
	/**
	 * Topic indexer.
	 */
	@Value("${kafka.dispatcher-indexer.base-topic}")
	private String dispatcherIndexerTopic;

	/**
	 * Retry indexer topic
	 */
	@Value("${kafka.dispatcher-indexer.delete-retry-topic}")
	private String dispatcherIndexerRetryDeleteTopic;

	/**
	 * Topic publisher.
	 */
	@Value("${kafka.dispatcher-publisher.base-topic}")
	private String dispatcherPublisherTopic;
	
	/**
	 * Log publisher.
	 */
	@Value("${kafka.log.base-topic}")
	private String logTopic;
	
	/**
	 * Retry indexer topic update
	 */
	@Value("${kafka.dispatcher-indexer.update-retry-topic}")
	private String dispatcherIndexerRetryUpdateTopic;
	

	@PostConstruct
	public void afterInit() {
		if (profileUtility.isTestProfile()) {
			statusManagerTopic = Constants.Profile.TEST_PREFIX + statusManagerTopic;
			dispatcherIndexerTopic = Constants.Profile.TEST_PREFIX + dispatcherIndexerTopic;
			dispatcherIndexerRetryDeleteTopic = Constants.Profile.TEST_PREFIX + dispatcherIndexerRetryDeleteTopic;
			dispatcherPublisherTopic = Constants.Profile.TEST_PREFIX + dispatcherPublisherTopic;
			logTopic = Constants.Profile.TEST_PREFIX + logTopic;
			dispatcherIndexerRetryUpdateTopic = Constants.Profile.TEST_PREFIX + dispatcherIndexerRetryUpdateTopic;
		}
	}
}

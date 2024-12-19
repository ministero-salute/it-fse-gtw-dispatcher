
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.logging;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * Interface for DeliveryStrategies.
 * @since 0.0.1
 */
public interface DeliveryStrategy {

    /**
     * Sends a message to a kafka producer and somehow deals with failures.
     *
     * @param producer the backing kafka producer
     * @param record the prepared kafka message (ready to ship)
     * @param event the originating logging event
     * @param failedDeliveryCallback a callback that handles messages that could not be delivered with best-effort.
     * @param <K> the key type of a persisted log message.
     * @param <V> the value type of a persisted log message.
     * @param <E> the type of the logging event.
     * @return {@code true} if the message could be sent successfully, {@code false} otherwise.
     */
    <K,V,E> boolean send(Producer<K,V> producer, ProducerRecord<K, V> record, E event, FailedDeliveryCallback<E> failedDeliveryCallback);

}
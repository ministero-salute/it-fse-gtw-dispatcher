
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
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;

import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.AppenderAttachable;

public abstract class KafkaAppenderConfig<E> extends UnsynchronizedAppenderBase<E> implements AppenderAttachable<E> {

    protected String topic = null;

    protected Encoder<E> encoder = null;
    protected KeyingStrategy<? super E> keyingStrategy = null;
    protected DeliveryStrategy deliveryStrategy;

    protected Integer partition = null;

    protected boolean appendTimestamp = true;

    protected Map<String,Object> producerConfig = new HashMap<>();

    protected boolean checkPrerequisites() {
        boolean errorFree = true;

        if (producerConfig.get(BOOTSTRAP_SERVERS_CONFIG) == null) {
            addError("No \"" + BOOTSTRAP_SERVERS_CONFIG + "\" set for the appender named [\""
                    + name + "\"].");
            errorFree = false;
        }

        if (topic == null) {
            addError("No topic set for the appender named [\"" + name + "\"].");
            errorFree = false;
        }

        if (encoder == null) {
            addError("No encoder set for the appender named [\"" + name + "\"].");
            errorFree = false;
        }
        
        if (keyingStrategy == null) {
            addInfo("No explicit keyingStrategy set for the appender named [\"" + name + "\"]. Using default NoKeyKeyingStrategy.");
            keyingStrategy = new NoKeyKeyingStrategy();
        }

        if (deliveryStrategy == null) {
            addInfo("No explicit deliveryStrategy set for the appender named [\""+name+"\"]. Using default asynchronous strategy.");
            deliveryStrategy = new AsynchronousDeliveryStrategy();
        }

        return errorFree;
    }

    public void setEncoder(Encoder<E> encoder) {
        this.encoder = encoder;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setKeyingStrategy(KeyingStrategy<? super E> keyingStrategy) {
        this.keyingStrategy = keyingStrategy;
    }

    public void addProducerConfig(String keyValue) {
        String[] split = keyValue.split("=", 2);
        if(split.length == 2)
            addProducerConfigValue(split[0], split[1]);
    }

    public void addProducerConfigValue(String key, Object value) {
        this.producerConfig.put(key,value);
    }

    public Map<String, Object> getProducerConfig() {
        return producerConfig;
    }

    public void setDeliveryStrategy(DeliveryStrategy deliveryStrategy) {
        this.deliveryStrategy = deliveryStrategy;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public boolean isAppendTimestamp() {
        return appendTimestamp;
    }

    public void setAppendTimestamp(boolean appendTimestamp) {
        this.appendTimestamp = appendTimestamp;
    }

}
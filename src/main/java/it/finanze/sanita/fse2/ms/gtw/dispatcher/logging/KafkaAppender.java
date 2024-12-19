
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaAppender<E> extends KafkaAppenderConfig<E> {

    /**
     * Kafka clients uses this prefix for its slf4j logging.
     * This appender defers appends of any Kafka logs since it could cause harmful infinite recursion/self feeding effects.
     */
    private static final String KAFKA_LOGGER_PREFIX = KafkaProducer.class.getPackage().getName().replaceFirst("\\.producer$", "");

    private LazyProducer lazyProducer = null;
    private final AppenderAttachableImpl<E> aai = new AppenderAttachableImpl<>();
    private final ConcurrentLinkedQueue<E> queue = new ConcurrentLinkedQueue<>();
    private final FailedDeliveryCallback<E> failedDeliveryCallback = new FailedDeliveryCallback<E>() {
        @Override
        public void onFailedDelivery(E evt, Throwable throwable) {
            aai.appendLoopOnAppenders(evt);
        }
    };

    public KafkaAppender() {
        addProducerConfigValue(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        addProducerConfigValue(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    }

    @Override
    public void doAppend(E e) {
        ensureDeferredAppends();
        if (e instanceof ILoggingEvent && ((ILoggingEvent)e).getLoggerName().startsWith(KAFKA_LOGGER_PREFIX)) {
            deferAppend(e);
        } else {
            super.doAppend(e);
        }
    }

    @Override
    public void start() {
        // only error free appenders should be activated
        if (!checkPrerequisites()) return;
        if (partition != null && partition < 0) {
            partition = null;
        }

        lazyProducer = new LazyProducer();

        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (lazyProducer != null && lazyProducer.isInitialized()) {
            try {
                lazyProducer.get().close();
            } catch (KafkaException e) {
            	log.error("Failed to shut down kafka producer: " + e.getMessage(), e);
            }
            lazyProducer = null;
        }
    }

    @Override
    public void addAppender(Appender<E> newAppender) {
        aai.addAppender(newAppender);
    }

    @Override
    public Iterator<Appender<E>> iteratorForAppenders() {
        return aai.iteratorForAppenders();
    }

    @Override
    public Appender<E> getAppender(String name) {
        return aai.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<E> appender) {
        return aai.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        aai.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<E> appender) {
        return aai.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return aai.detachAppender(name);
    }

    @Override
    protected void append(E e) {
        final byte[] payload = encoder.encode(e);
        final byte[] key = keyingStrategy.createKey(e);

        final Long timestamp = isAppendTimestamp() ? getTimestamp(e) : null;

        final ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(topic, partition, timestamp, key, payload);

        final Producer<byte[], byte[]> producer = lazyProducer.get();
        if (producer != null) {
        	log.debug("Send message log with lazy producer");
        	log.debug("Delivery strategy : " + deliveryStrategy);
            deliveryStrategy.send(lazyProducer.get(), record, e, failedDeliveryCallback);
        } else {
        	log.warn("Failed send msg");
            failedDeliveryCallback.onFailedDelivery(e, null);
        }
    }

    protected Long getTimestamp(E e) {
        if (e instanceof ILoggingEvent) {
            return ((ILoggingEvent) e).getTimeStamp();
        } else {
            return System.currentTimeMillis();
        }
    }

    protected Producer<byte[], byte[]> createProducer() {
    	Producer<byte[], byte[]> out = null;
    	try {
    		log.debug("Create producer log");
    		for(Entry<String, Object> prod : producerConfig.entrySet()) {
    			log.debug("KEY : " + prod.getKey() + " VALUE:" + prod.getValue());
    		}
    		out = new KafkaProducer<>(new HashMap<>(producerConfig));
    	} catch(Exception ex) {
    		log.error("Error while create producer with props : " , ex);
    		throw new BusinessException("Error while create producer with props : " + ex);
    	}
        return out;
    }

    private void deferAppend(E event) {
        queue.add(event);
    }

    // drains queue events to super
    private void ensureDeferredAppends() {
        E event;

        while ((event = queue.poll()) != null) {
            super.doAppend(event);
        }
    }
 
    private class LazyProducer {

        private volatile Producer<byte[], byte[]> producer;

        public Producer<byte[], byte[]> get() {
            Producer<byte[], byte[]> result = this.producer;
            if (result == null) {
                synchronized(this) {
                    result = this.producer;
                    if(result == null) {
                        this.producer = result = this.initialize();
                    }
                }
            }

            return result;
        }

        protected Producer<byte[], byte[]> initialize() {
            Producer<byte[], byte[]> producer = null;
            try {
            	log.debug("Inizializzazione producer log");
                producer = createProducer();
            } catch (Exception e) {
            	log.error("Error creating producer :", e);
            	addError("error creating producer", e);
            }
            return producer;
        }

        public boolean isInitialized() { return producer != null; }
    }

}
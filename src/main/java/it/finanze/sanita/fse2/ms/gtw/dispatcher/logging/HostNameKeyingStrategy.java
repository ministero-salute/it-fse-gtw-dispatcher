
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

import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.spi.ContextAwareBase;

import ch.qos.logback.core.spi.LifeCycle;
import java.nio.ByteBuffer;

/**
 * This strategy uses the HOSTNAME as kafka message key.
 * This is useful because it ensures that all log messages issued by this host will remain in the correct order for any consumer.
 * But this strategy can lead to uneven log distribution for a small number of hosts (compared to the number of partitions).
 * @since 0.0.1
 */
public class HostNameKeyingStrategy extends ContextAwareBase implements KeyingStrategy<Object>, LifeCycle {

    private byte[] hostnameHash = null;
    
    private boolean errorWasShown = false;

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        final String hostname = context.getProperty(CoreConstants.HOSTNAME_KEY);
        if (hostname == null) {
            if (!errorWasShown) {
            	addError("Hostname could not be found in context. HostNamePartitioningStrategy will not work.");
                errorWasShown = true;
            }
        } else {
            hostnameHash = ByteBuffer.allocate(4).putInt(hostname.hashCode()).array();
        }
    }

    @Override
    public byte[] createKey(Object e) {
        return hostnameHash;
    }

    @Override
    public void start() { }

    @Override
    public void stop() {
        errorWasShown = false;
    }

    @Override
    public boolean isStarted() { return true; }
}

/*
 * Copyright (c) 2013 Plexxi, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.utils.IPProtocols;

public class HostStats {

    private Map<Byte, Long> byteCounts;
    private Map<Byte, Long> packetCounts;
    private Map<Byte, Double> durations;

    public HostStats() {
        this.byteCounts = new HashMap<Byte, Long>();
        this.packetCounts = new HashMap<Byte, Long>();
        this.durations = new HashMap<Byte, Double>();
    }

    // Return the set of all protocols
    public Set<Byte> getProtocols() {
        return this.byteCounts.keySet();
    }

    // Returns the total byte count across all protocols
    public long getByteCount() {
        long totalByteCount = 0;
        for (Byte protocol : this.byteCounts.keySet())
            totalByteCount += this.byteCounts.get(protocol);
        return totalByteCount;
    }

    // Returns the byte count for a particular protocol
    public long getByteCount(Byte protocol) {
        Long byteCount = this.byteCounts.get(protocol);
        if (byteCount == null)
            byteCount = (long) 0;
        return byteCount;
    }

    // Returns the total packet count across all protocols
    public long getPacketCount() {
        long totalPacketCount = 0;
        for (Byte protocol : this.packetCounts.keySet())
            totalPacketCount += this.packetCounts.get(protocol);
        return totalPacketCount;
    }

    // Returns the packet count for a particular protocol
    public long getPacketCount(Byte protocol) {
        Long packetCount = this.packetCounts.get(protocol);
        if (packetCount == null)
            packetCount = (long) 0;
        return packetCount;
    }
    
    // Returns the maximum duration across all protocols
    public double getDuration() {
        if (this.durations.isEmpty())
            return 0.0;
        return Collections.max(this.durations.values());
    }

    // Returns the duration for a particular protocol
    public double getDuration(Byte protocol) {
        Double duration = this.durations.get(protocol);
        if (duration == null)
            duration = (double) 0.0;
        return duration;
    }

    // Returns the bit rate across all protocols
    public double getBitRate() {
        return getBitRateInternal(getByteCount(), getDuration());
    }

    // Returns the bit rate for a particular protocol
    public double getBitRate(Byte protocol) {
        return getBitRateInternal(getByteCount(protocol), getDuration(protocol));
    }

    // Return all bit rates
    public Map<Byte, Double> getAllBitRates() {
        Map <Byte, Double> bitRates = new HashMap<Byte, Double>();
        for (Byte protocol : this.byteCounts.keySet())
            bitRates.put(protocol, getBitRate(protocol));
        return bitRates;
    }

    // Internal method to calculate bit rate
    private double getBitRateInternal(long byteCount, double duration) {
        if (duration == 0)
            return 0.0;
        return (byteCount * 8)/duration;
    }

    // Sets byte count, packet count, and duration given a flow
    public void setStatsFromFlow(FlowOnNode flow) {
        MatchField protocolField = flow.getFlow().getMatch().getField(MatchType.NW_PROTO);
        Byte protocolNumber;
        if (protocolField == null)
            protocolNumber = IPProtocols.ANY.byteValue();
        else
            protocolNumber = (Byte) protocolField.getValue();

        // Prevent stats from getting overwritten by zero-byte flows.
        Long currentByteCount = this.byteCounts.get(protocolNumber);
        Long thisByteCount = flow.getByteCount();
        Long thisPacketCount = flow.getPacketCount();
        if (thisByteCount > 0 && (currentByteCount == null || currentByteCount <= thisByteCount)) {
            this.byteCounts.put(protocolNumber, thisByteCount);
            this.packetCounts.put(protocolNumber, thisPacketCount);
            this.durations.put(protocolNumber, flow.getDurationSeconds() + .000000001 * flow.getDurationNanoseconds());
        }
    }
}

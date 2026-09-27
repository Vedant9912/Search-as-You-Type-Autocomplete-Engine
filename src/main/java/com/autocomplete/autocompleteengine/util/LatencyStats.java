package com.autocomplete.autocompleteengine.util;

import java.util.Collections;
import java.util.List;

/** Simple percentile calculator used to benchmark search latency (p50/p95/p99). */
public class LatencyStats {

    public static long getPercentile(List<Long> latencies, double percentile) {
        Collections.sort(latencies);
        int index = (int) ((percentile / 100) * latencies.size());
        if (index >= latencies.size()) {
            index = latencies.size() - 1;
        }
        return latencies.get(index);
    }
}

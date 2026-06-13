package com.zhiwu.dynamicthreadpollermanager.experiment.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PressureSnapshotSerializationTest {

    @Test
    void shouldRoundTripSixFieldSnapshot() {
        PressureSnapshot original = new PressureSnapshot(
                Instant.parse("2026-06-13T10:00:00Z"), 3, 4, 5, 100L, 0.5);

        Map<String, Object> map = original.toMap();
        PressureSnapshot restored = PressureSnapshot.fromMap(map);

        assertEquals(original, restored);
    }

    @Test
    void shouldRoundTripFourFieldSnapshot() {
        PressureSnapshot original = new PressureSnapshot(
                Instant.parse("2026-06-13T10:00:00Z"), 3, 5, 0.5);

        Map<String, Object> map = original.toMap();
        PressureSnapshot restored = PressureSnapshot.fromMap(map);

        assertEquals(original, restored);
    }

    @Test
    void shouldSerializeTimestampAsIso8601() {
        PressureSnapshot snapshot = new PressureSnapshot(
                Instant.parse("2026-06-13T10:00:00Z"), 1, 2, 3, 50L, 0.0);

        Map<String, Object> map = snapshot.toMap();

        assertEquals("2026-06-13T10:00:00Z", map.get("timestamp"));
    }

    @Test
    void shouldDeserializeFromJsonParsedMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", "2026-06-13T10:00:00Z");
        map.put("activeThreads", 3);
        map.put("poolSize", 4);
        map.put("queueSize", 5);
        map.put("completedTaskCount", 100L);
        map.put("cpuUtilization", 0.5);

        PressureSnapshot snapshot = PressureSnapshot.fromMap(map);

        assertEquals(Instant.parse("2026-06-13T10:00:00Z"), snapshot.timestamp());
        assertEquals(3, snapshot.activeThreads());
        assertEquals(4, snapshot.poolSize());
        assertEquals(5, snapshot.queueSize());
        assertEquals(100L, snapshot.completedTaskCount());
        assertEquals(0.5, snapshot.cpuUtilization());
    }

    @Test
    void shouldHandleNumberTypeConversion() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", "2026-06-13T10:00:00Z");
        map.put("activeThreads", 3);          // Integer
        map.put("poolSize", 4L);              // Long → int
        map.put("queueSize", 5.0);            // Double → int
        map.put("completedTaskCount", 100);   // Integer → long
        map.put("cpuUtilization", 0);         // Integer → double

        PressureSnapshot snapshot = PressureSnapshot.fromMap(map);

        assertEquals(3, snapshot.activeThreads());
        assertEquals(4, snapshot.poolSize());
        assertEquals(5, snapshot.queueSize());
        assertEquals(100L, snapshot.completedTaskCount());
        assertEquals(0.0, snapshot.cpuUtilization());
    }
}

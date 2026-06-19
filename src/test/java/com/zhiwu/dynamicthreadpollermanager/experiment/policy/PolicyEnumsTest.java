package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PolicyEnumsTest {

    @Test
    void policyActionShouldExposeExactlyThreeValues() {
        PolicyAction[] values = PolicyAction.values();
        assertEquals(3, values.length);
        assertNotNull(PolicyAction.valueOf("SCALE_UP"));
        assertNotNull(PolicyAction.valueOf("SCALE_DOWN"));
        assertNotNull(PolicyAction.valueOf("HOLD"));
    }

    @Test
    void gateStatusShouldExposeExactlyFourValues() {
        GateStatus[] values = GateStatus.values();
        assertEquals(4, values.length);
        assertNotNull(GateStatus.valueOf("ACCEPTED"));
        assertNotNull(GateStatus.valueOf("CAPPED"));
        assertNotNull(GateStatus.valueOf("HOLD"));
        assertNotNull(GateStatus.valueOf("REJECTED"));
    }
}

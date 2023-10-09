/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VoltageLimitsOverrideTest {

    @Test
    void validVoltageLimitOverrideBuilderTest() {
        VoltageLimitOverrideBuilder builder = new VoltageLimitOverrideBuilder();
        VoltageLimitOverride vlo;

        // if one side of override specified, valid voltage limit override
        builder.withLowLimitKind(VoltageLimitOverride.OverrideKind.RELATIVE)
                .withLowLimitOverride(-1);
        vlo = builder.build();
        assertEquals(vlo.getLowLimitKind(), VoltageLimitOverride.OverrideKind.RELATIVE);
        assertEquals(vlo.getLowLimitOverride(), -1);

        // if two sides of override specified, valid voltage limit override
        builder.withHighLimitKind(VoltageLimitOverride.OverrideKind.ABSOLUTE)
                .withHighLimitOverride(410);
        vlo = builder.build();
        assertEquals(vlo.getHighLimitKind(), VoltageLimitOverride.OverrideKind.ABSOLUTE);
        assertEquals(vlo.getHighLimitOverride(), 410);
    }

    @Test
    void invalidVoltageLimitOverrideBuilderTest() {
        VoltageLimitOverrideBuilder builder = new VoltageLimitOverrideBuilder();

        // if no override kind specified, invalid voltage limit override
        assertThrows(IllegalStateException.class, builder::build);

        // if no override value specified, invalid voltage limit override
        builder.withLowLimitKind(VoltageLimitOverride.OverrideKind.RELATIVE);
        builder.withHighLimitKind(VoltageLimitOverride.OverrideKind.ABSOLUTE);
        assertThrows(IllegalStateException.class, builder::build);

        // if low override value is undefined, invalid voltage limit override
        builder.withLowLimitOverride(5)
                .withHighLimitOverride(Double.NaN);
        assertThrows(IllegalStateException.class, builder::build);

        // if high override value is undefined, invalid voltage limit override
        builder.withLowLimitOverride(Double.NaN)
                .withHighLimitOverride(5);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void equalsVoltageLimitOverride() {
        VoltageLimitOverrideBuilder builder = new VoltageLimitOverrideBuilder().withLowLimitKind(VoltageLimitOverride.OverrideKind.RELATIVE)
                .withLowLimitOverride(5)
                .withHighLimitKind(VoltageLimitOverride.OverrideKind.ABSOLUTE)
                .withHighLimitOverride(410);

        assertEquals(builder.build(), builder.build());
        assertNotEquals(builder.build(), builder.withHighLimitKind(VoltageLimitOverride.OverrideKind.RELATIVE).build());
    }

}

/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.openreac.exceptions.InvalidParametersException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoltageLimitsOverrideTest {

    @Test
    void testToStringOverrideType() {
        VoltageLimitOverride.VoltageLimitType os = VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT;
        assertEquals("HIGH_VOLTAGE_LIMIT", os.toString());
        os = VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT;
        assertEquals("LOW_VOLTAGE_LIMIT", os.toString());
    }

    @Test
    void invalidVoltageLimitOverride() {
        // Verify it is impossible to create voltage limit override with NaN value...
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, true, Double.NaN));
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, false, Double.NaN));
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, true, Double.NaN));
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, false, Double.NaN));

        // Verify it is impossible to create absolute voltage limit override with negative value
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, false, -1));
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, false, -1));

        // Verify it is impossible to create voltage limit override with undefined override type
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, null, 2));
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, null, -5));
    }

    @Test
    void validVoltageLimitOverride() {
        VoltageLimitOverride vlo = new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, true, -4);
        assertEquals(VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, vlo.getVoltageLimitType());
        assertTrue(vlo.isRelative());
        assertEquals(-4, vlo.getLimit());

        VoltageLimitOverride vlo2 = new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, false, 400);
        assertEquals(VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, vlo2.getVoltageLimitType());
        assertFalse(vlo2.isRelative());
        assertEquals(400, vlo2.getLimit());
    }

    @Test
    void equalsVoltageLimitOverride() {
        VoltageLimitOverride vlo1 = new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, true, 5);
        VoltageLimitOverride vlo2 = new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, true, 5);
        assertEquals(vlo1, vlo2);

        VoltageLimitOverride vlo3 = new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, true, 5);
        assertNotEquals(vlo1, vlo3);

        VoltageLimitOverride vlo4 = new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, false, 5);
        assertNotEquals(vlo1, vlo4);

        VoltageLimitOverride vlo5 = new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, true, 6);
        assertNotEquals(vlo1, vlo5);
    }
}

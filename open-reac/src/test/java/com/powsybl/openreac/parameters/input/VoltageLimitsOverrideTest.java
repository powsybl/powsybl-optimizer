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

public class VoltageLimitsOverrideTest {

    @Test
    void testToStringOverrideType() {
        VoltageLimitOverride.VoltageLimitType os = VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT;
        assertEquals(os.toString(), "HIGH_VOLTAGE_LIMIT");
        os = VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT;
        assertEquals(os.toString(), "LOW_VOLTAGE_LIMIT");
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
    }

    @Test
    void validVoltageLimitOverride() {
        VoltageLimitOverride vlo = new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, true, -4);
        assertEquals(vlo.getVoltageLimitType(), VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT);
        assertTrue(vlo.isRelative());
        assertEquals(vlo.getLimit(), -4);

        VoltageLimitOverride vlo2 = new VoltageLimitOverride("vl", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, false, 400);
        assertEquals(vlo2.getVoltageLimitType(), VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT);
        assertFalse(vlo2.isRelative());
        assertEquals(vlo2.getLimit(), 400);
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

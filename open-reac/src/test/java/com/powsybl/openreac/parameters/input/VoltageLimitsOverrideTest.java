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
    void testToStringOverrideSide() {
        VoltageLimitOverride.OverrideSide os = VoltageLimitOverride.OverrideSide.HIGH;
        assertEquals(os.toString(), "HIGH");
        os = VoltageLimitOverride.OverrideSide.LOW;
        assertEquals(os.toString(), "LOW");
    }

    @Test
    void invalidVoltageLimitOverride() {
        // Verify it is impossible to create voltage limit override with NaN value...
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.HIGH, true, Double.NaN));
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.HIGH, false, Double.NaN));
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.LOW, true, Double.NaN));
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.LOW, false, Double.NaN));

        // Verify it is impossible to create absolute voltage limit override with negative value
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.HIGH, false, -1));
        assertThrows(InvalidParametersException.class,
                () -> new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.LOW, false, -1));
    }

    @Test
    void validVoltageLimitOverride() {
        VoltageLimitOverride vlo = new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.HIGH, true, -4);
        assertEquals(vlo.getSide(), VoltageLimitOverride.OverrideSide.HIGH);
        assertTrue(vlo.isRelative());
        assertEquals(vlo.getLimitOverride(), -4);

        VoltageLimitOverride vlo2 = new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.LOW, false, 400);
        assertEquals(vlo2.getSide(), VoltageLimitOverride.OverrideSide.LOW);
        assertFalse(vlo2.isRelative());
        assertEquals(vlo2.getLimitOverride(), 400);
    }

    @Test
    void equalsVoltageLimitOverride() {
        VoltageLimitOverride vlo1 = new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.HIGH, true, 5);
        VoltageLimitOverride vlo2 = new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.HIGH, true, 5);
        assertEquals(vlo1, vlo2);

        VoltageLimitOverride vlo3 = new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.LOW, true, 5);
        assertNotEquals(vlo1, vlo3);

        VoltageLimitOverride vlo4 = new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.HIGH, false, 5);
        assertNotEquals(vlo1, vlo4);

        VoltageLimitOverride vlo5 = new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.HIGH, true, 6);
        assertNotEquals(vlo1, vlo5);
    }

}

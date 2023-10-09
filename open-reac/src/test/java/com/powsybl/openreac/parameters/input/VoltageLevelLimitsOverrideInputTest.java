/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import com.google.common.io.ByteStreams;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.converter.AmplUtil;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.openreac.exceptions.InvalidParametersException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class VoltageLevelLimitsOverrideInputTest {

    @Test
    void testRelativeOverrideOnUndefinedLimit() {
        Network network = EurostagTutorialExample1Factory.create();
        VoltageLevel vlLoad = network.getVoltageLevel("VLLOAD");

        // if relative override is intended on undefined low voltage limit, throws exception
        vlLoad.setLowVoltageLimit(Double.NaN)
                .setHighVoltageLimit(150);

        Map<String, VoltageLimitOverride> voltageLimitsOverride = Map.of("VLLOAD",
                new VoltageLimitOverrideBuilder()
                        .withLowLimitKind(VoltageLimitOverride.OverrideKind.RELATIVE)
                        .withLowLimitOverride(-1.3)
                        .build());
        assertThrows(IllegalArgumentException.class,
                () -> new VoltageLevelLimitsOverrideInput(voltageLimitsOverride, network));

        // if relative override is intended on undefined high voltage limit, throws exception
        vlLoad.setLowVoltageLimit(130)
                .setHighVoltageLimit(Double.NaN);

        Map<String, VoltageLimitOverride> voltageLimitsOverride2 = Map.of("VLLOAD",
                new VoltageLimitOverrideBuilder()
                        .withHighLimitKind(VoltageLimitOverride.OverrideKind.RELATIVE)
                        .withHighLimitOverride(4.2)
                        .build());

        assertThrows(IllegalArgumentException.class,
                () -> new VoltageLevelLimitsOverrideInput(voltageLimitsOverride2, network));
    }

    @Test
    void testRelativeOverride() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();

        // verify relative override can be applied on both defined low/high limits
        VoltageLevel vlGen = network.getVoltageLevel("VLGEN");
        vlGen.setLowVoltageLimit(20)
                .setHighVoltageLimit(26);

        Map<String, VoltageLimitOverride> voltageLimitsOverride = Map.of("VLGEN",
                new VoltageLimitOverrideBuilder()
                        .withLowLimitKind(VoltageLimitOverride.OverrideKind.RELATIVE)
                        .withLowLimitOverride(-1)
                        .withHighLimitKind(VoltageLimitOverride.OverrideKind.RELATIVE)
                        .withHighLimitOverride(2)
                        .build());

        VoltageLevelLimitsOverrideInput input = new VoltageLevelLimitsOverrideInput(voltageLimitsOverride, network);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);
        try (var is = input.getParameterFileAsStream(mapper)) {
            String data = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
            String ref = String.join(System.lineSeparator(), "#num minV (pu) maxV (pu) id",
                    "1 0.7916666666666666 1.1666666666666667 'VLGEN'") + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    @Test
    void testAbsoluteOverride() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();

        // change high voltage limits to undefined values
        VoltageLevel vlgen = network.getVoltageLevel("VLGEN");
        vlgen.setLowVoltageLimit(Double.NaN)
                .setHighVoltageLimit(Double.NaN);

        // verify absolute override can be applied on both undefined low/high limits
        VoltageLimitOverride vlo = new VoltageLimitOverrideBuilder().withLowLimitKind(VoltageLimitOverride.OverrideKind.ABSOLUTE)
                .withHighLimitKind(VoltageLimitOverride.OverrideKind.ABSOLUTE)
                .withLowLimitOverride(20)
                .withHighLimitOverride(26)
                .build();

        Map<String, VoltageLimitOverride> voltageLimtisOverride = Map.of("VLGEN", vlo);

        VoltageLevelLimitsOverrideInput input = new VoltageLevelLimitsOverrideInput(voltageLimtisOverride, network);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);
        try (var is = input.getParameterFileAsStream(mapper)) {
            String data = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
            String ref = String.join(System.lineSeparator(), "#num minV (pu) maxV (pu) id",
                    "1 0.8333333333333334 1.0833333333333333 'VLGEN'") + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    @Test
    void testUnsupportedNetwork() {
        Network network = IeeeCdfNetworkFactory.create118();
        VoltageLevel vl = network.getVoltageLevels().iterator().next();
        OpenReacParameters params = new OpenReacParameters();

        // if only low voltage limit of one voltage level is undefined, invalid OpenReacParameters
        vl.setLowVoltageLimit(Double.NaN);
        vl.setHighVoltageLimit(480);
        assertThrows(PowsyblException.class, () -> params.checkIntegrity(network));

        // if only high voltage limit of one voltage level is undefined, invalid OpenReacParameters
        vl.setLowVoltageLimit(480);
        vl.setHighVoltageLimit(Double.NaN);
        assertThrows(PowsyblException.class, () -> params.checkIntegrity(network));
    }

    @Test
    void testNegativeLowOverride() {
        Network network = IeeeCdfNetworkFactory.create118();
        VoltageLevel vl = network.getVoltageLevels().iterator().next();
        vl.setLowVoltageLimit(400);
        vl.setHighVoltageLimit(480);

        // if low relative voltage override leads to negative voltage limit, throws exception
        OpenReacParameters params = new OpenReacParameters();
        params.addSpecificVoltageLimits(Map.of(vl.getId(),
                new VoltageLimitOverrideBuilder()
                        .withLowLimitKind(VoltageLimitOverride.OverrideKind.RELATIVE)
                        .withLowLimitOverride(-410)
                        .withHighLimitKind(VoltageLimitOverride.OverrideKind.RELATIVE)
                        .withHighLimitOverride(0)
                        .build()));
        assertThrows(InvalidParametersException.class, () -> params.checkIntegrity(network));

        // if low absolute voltage override leads to negative voltage limit, throws exception
        OpenReacParameters params2 = new OpenReacParameters();
        params2.addSpecificVoltageLimits(Map.of(vl.getId(),
                new VoltageLimitOverrideBuilder()
                        .withLowLimitKind(VoltageLimitOverride.OverrideKind.ABSOLUTE)
                        .withLowLimitOverride(-10)
                        .withHighLimitKind(VoltageLimitOverride.OverrideKind.RELATIVE)
                        .withHighLimitOverride(0)
                        .build()));
        assertThrows(InvalidParametersException.class, () -> params2.checkIntegrity(network));
    }

    @Test
    void testInvalidVoltageLevelOverride() {
        Network network = IeeeCdfNetworkFactory.create118();

        // if voltage level (on which is applied override) is not in the network, throws exception
        OpenReacParameters params3 = new OpenReacParameters();
        params3.addSpecificVoltageLimits(Map.of("UNKNOWN_ID",
                new VoltageLimitOverrideBuilder()
                        .withLowLimitKind(VoltageLimitOverride.OverrideKind.RELATIVE)
                        .withLowLimitOverride(1)
                        .withHighLimitKind(VoltageLimitOverride.OverrideKind.RELATIVE)
                        .withHighLimitOverride(0)
                        .build()));
        assertThrows(InvalidParametersException.class, () -> params3.checkIntegrity(network));
    }
}

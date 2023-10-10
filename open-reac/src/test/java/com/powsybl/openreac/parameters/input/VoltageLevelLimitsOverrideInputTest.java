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
import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class VoltageLevelLimitsOverrideInputTest {

    @Test
    void testValidRelativeVoltageOverride() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        // verify relative override can be applied on both defined low/high limits
        VoltageLevel vlGen = network.getVoltageLevel("VLGEN");
        vlGen.setLowVoltageLimit(20)
                .setHighVoltageLimit(26);

        List<Pair<String, VoltageLimitOverride>> voltageLimitsOverride = new ArrayList<>();
        voltageLimitsOverride.add(new Pair<>("VLGEN",
                new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.LOW, true, -1)));
        voltageLimitsOverride.add(new Pair<>("VLGEN",
                new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.HIGH, true, 2)));

        VoltageLevelLimitsOverrideInput input = new VoltageLevelLimitsOverrideInput(voltageLimitsOverride, network);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);
        try (var is = input.getParameterFileAsStream(mapper)) {
            String data = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
            String ref = String.join(System.lineSeparator(), "#num minV (pu) maxV (pu) id",
                    "1 0.7916666666666667 1.1666666666666665 'VLGEN'") + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    @Test
    void testValidAbsoluteVoltageOverride() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        // change high voltage limits to undefined values
        VoltageLevel vlgen = network.getVoltageLevel("VLGEN");
        vlgen.setLowVoltageLimit(Double.NaN)
                .setHighVoltageLimit(Double.NaN);

        List<Pair<String, VoltageLimitOverride>> voltageLimitsOverride = new ArrayList<>();
        voltageLimitsOverride.add(new Pair<>("VLGEN",
                new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.LOW, false, 20)));
        voltageLimitsOverride.add(new Pair<>("VLGEN",
                new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.HIGH, false, 26)));

        VoltageLevelLimitsOverrideInput input = new VoltageLevelLimitsOverrideInput(voltageLimitsOverride, network);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);
        try (var is = input.getParameterFileAsStream(mapper)) {
            String data = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
            String ref = String.join(System.lineSeparator(), "#num minV (pu) maxV (pu) id",
                    "1 0.8333333333333334 1.0833333333333333 'VLGEN'") + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    @Test
    void testUndefinedVoltageLimitsWithoutOverride() {
        Network network = IeeeCdfNetworkFactory.create118();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        VoltageLevel vl = network.getVoltageLevels().iterator().next();
        OpenReacParameters params = new OpenReacParameters();

        // if one low voltage limit is undefined, invalid OpenReacParameters
        vl.setLowVoltageLimit(Double.NaN);
        vl.setHighVoltageLimit(480);
        assertThrows(PowsyblException.class, () -> params.checkIntegrity(network));

        // if one high voltage limit is undefined, invalid OpenReacParameters
        vl.setLowVoltageLimit(480);
        vl.setHighVoltageLimit(Double.NaN);
        assertThrows(PowsyblException.class, () -> params.checkIntegrity(network));

        // if both low/high voltage limit are undefined, invalid OpenReacParameters
        vl.setLowVoltageLimit(Double.NaN);
        vl.setHighVoltageLimit(Double.NaN);
        assertThrows(PowsyblException.class, () -> params.checkIntegrity(network));
    }

    @Test
    void testVoltageOverrideWithNegativeLowVoltageValue() {
        Network network = IeeeCdfNetworkFactory.create118();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        VoltageLevel vl = network.getVoltageLevels().iterator().next();
        vl.setHighVoltageLimit(480);
        vl.setLowVoltageLimit(400);

        // if low relative voltage override leads to negative voltage limit, throws exception
        OpenReacParameters params = new OpenReacParameters();
        List<Pair<String, VoltageLimitOverride>> voltageLimitsOverride = new ArrayList<>();
        voltageLimitsOverride.add(new Pair<>(vl.getId(), new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.LOW, true, -410)));
        params.addSpecificVoltageLimits(voltageLimitsOverride);
        assertThrows(InvalidParametersException.class, () -> params.checkIntegrity(network));
    }

    @Test
    void testVoltageOverrideOnInvalidVoltageLevel() {
        Network network = IeeeCdfNetworkFactory.create118();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        // if voltage level (on which is applied override) is not in the network, throws exception
        OpenReacParameters params3 = new OpenReacParameters();
        List<Pair<String, VoltageLimitOverride>> voltageLimitsOverride = new ArrayList<>();
        voltageLimitsOverride.add(new Pair<>("UNKNOWN_ID", new VoltageLimitOverride(VoltageLimitOverride.OverrideSide.LOW, true, 1)));
        params3.addSpecificVoltageLimits(voltageLimitsOverride);
        assertThrows(InvalidParametersException.class, () -> params3.checkIntegrity(network));
    }

    void setDefaultVoltageLimits(Network network) {
        for (VoltageLevel vl : network.getVoltageLevels()) {
            if (vl.getLowVoltageLimit() <= 0 || Double.isNaN(vl.getLowVoltageLimit())) {
                vl.setLowVoltageLimit(0.8 * vl.getNominalV());
            }
            if (Double.isNaN(vl.getHighVoltageLimit())) {
                vl.setHighVoltageLimit(1.2 * vl.getNominalV());
            }
        }
    }
}

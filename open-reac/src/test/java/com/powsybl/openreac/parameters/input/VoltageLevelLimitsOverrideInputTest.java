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
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class VoltageLevelLimitsOverrideInputTest {

    @Test
    void test() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        VoltageLevel vlgen = network.getVoltageLevel("VLGEN");
        vlgen.setLowVoltageLimit(20)
                .setHighVoltageLimit(26);
        VoltageLevel vlload = network.getVoltageLevel("VLLOAD");
        vlload.setLowVoltageLimit(130);
        Map<String, VoltageLimitOverride> voltageLimitsOverride = Map.of("VLGEN", new VoltageLimitOverride(-1, 2),
                                                                         "VLHV1", new VoltageLimitOverride(-1.3, 2.5),
                                                                         "VLLOAD", new VoltageLimitOverride(-1.7, 4.2));
        VoltageLevelLimitsOverrideInput input = new VoltageLevelLimitsOverrideInput(voltageLimitsOverride, network);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);
        try (var is = input.getParameterFileAsStream(mapper)) {
            String data = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
            String ref = String.join(System.lineSeparator(), "#num minV (pu) maxV (pu) id",
                    "1 0.7916666666666666 1.1666666666666667 'VLGEN'",
                    "4 0.8553333333333334 -99999.0 'VLLOAD'") + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }
}

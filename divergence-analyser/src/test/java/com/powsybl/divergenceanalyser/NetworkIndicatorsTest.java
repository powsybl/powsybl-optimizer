/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser;

import com.powsybl.divergenceanalyser.network.VoltageControlNetworkFactory;
import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class NetworkIndicatorsTest {

    void assertKeysIndicatorsAreCorrect(DivergenceAnalyserResults results) {
        assertEquals(results.getNetworkIndicators().get(0).getFirst(), "num_main_cc");
        assertEquals(results.getNetworkIndicators().get(1).getFirst(), "num_buses");
        assertEquals(results.getNetworkIndicators().get(2).getFirst(), "num_PV_buses");
        assertEquals(results.getNetworkIndicators().get(3).getFirst(), "num_branches");
        assertEquals(results.getNetworkIndicators().get(4).getFirst(), "num_rtc");
        assertEquals(results.getNetworkIndicators().get(5).getFirst(), "num_pst");
        assertEquals(results.getNetworkIndicators().get(6).getFirst(), "num_3wt");
    }

    void assertValuesIndicatorsAreCorrect(DivergenceAnalyserResults results, int mainCC, int numBus, int numPVBus,
                                          int numBranches, int numRtc, int numPst, int num3wt) {
        assertEquals(Integer.parseInt(results.getNetworkIndicators().get(0).getSecond()), mainCC);
        assertEquals(Integer.parseInt(results.getNetworkIndicators().get(1).getSecond()), numBus);
        assertEquals(Integer.parseInt(results.getNetworkIndicators().get(2).getSecond()), numPVBus);
        assertEquals(Integer.parseInt(results.getNetworkIndicators().get(3).getSecond()), numBranches);
        assertEquals(Integer.parseInt(results.getNetworkIndicators().get(4).getSecond()), numRtc);
        assertEquals(Integer.parseInt(results.getNetworkIndicators().get(5).getSecond()), numPst);
        assertEquals(Integer.parseInt(results.getNetworkIndicators().get(6).getSecond()), num3wt);
    }

    void areNetworkIndicatorsCorrect(Network network, int mainCC, int numBus, int numPVBus,
                               int numBranches, int numRtc, int numPst, int num3wt) throws IOException {
        // Run DA with no penal
        DivergenceAnalyserResults results = DivergenceAnalyser.runDivergenceAnalysis(network,
                new DivergenceAnalyserParameters());

        // Verify network indicators
        assertKeysIndicatorsAreCorrect(results);
        assertValuesIndicatorsAreCorrect(results, mainCC, numBus, numPVBus, numBranches, numRtc, numPst, num3wt);
    }

    @Test
    void testNetworkIEEE14() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        areNetworkIndicatorsCorrect(network, 0, 14, 5, 20, 0, 0, 0);
    }

    @Test
    void testSmallNetwork() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithShuntAndGeneratorVoltageControl();
        areNetworkIndicatorsCorrect(network, 0, 3, 2, 2, 0, 0, 0);
    }

    @Test
    void networkWith3wtInfo() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWithT3wt();
        areNetworkIndicatorsCorrect(network, 0, 5, 1, 4, 1, 0, 1);
    }

}

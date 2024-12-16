/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.optimization;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test OpenReac optimization with branches opened on one side.
 *
 * @author Pierre ARVY {@literal <pierre.arvy at artelys.com>}
 */
public class OpenReacBranchWithOneSideOpenedTest extends AbstractOpenReacRunnerTest {

    @Test
    void testOpenLineSide1OpenReac() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithSimpleRemoteControl();
        network.getLine("l43").setG2(0.1f).setB2(0.1f).getTerminal1().disconnect();
        testAllModifAndLoadFlow(network, "optimization/opened-branches/line-open-side-1", new OpenReacParameters(), ReportNode.NO_OP);
    }

    @Test
    void testZeroImpedanceOpenBranchSide2OpenReac() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithTwoVoltageControls();
        network.getLine("l45").setX(1e-8).setB1(1).setG1(0.1).getTerminal2().disconnect();

        OpenReacResult result = runOpenReac(network, "optimization/opened-branches/zero-impedance-open-side-2");
        // opened branch is considered as non impedant
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_branch_in_AC_CC_side_2_opened")));
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_branch_with_zero_or_small_impedance")));

        assertEquals(OpenReacStatus.OK, result.getStatus());
        result.applyAllModifications(network);
        LoadFlowResult loadFlowResult = LoadFlow.run(network);
        assertTrue(loadFlowResult.isFullyConverged());
    }

    @Test
    void testZeroImpedanceOpenBranchSide1OpenReac() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        network.getTwoWindingsTransformer("T2wT2").setR(0.0).setX(1e-8).setG(0.01).setB(0.1).getTerminal1().disconnect();

        OpenReacResult result = runOpenReac(network, "optimization/opened-branches/zero-impedance-open-side-1");
        // opened branch is considered as non impedance branch
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_branch_in_AC_CC_side_1_opened")));
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_branch_with_zero_or_small_impedance")));

        assertEquals(OpenReacStatus.OK, result.getStatus());
        result.applyAllModifications(network);
        LoadFlowResult loadFlowResult = LoadFlow.run(network);
        assertTrue(loadFlowResult.isFullyConverged());
    }

    @Test
    void testRatioTapChangerOpenSide2OpenReac() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWithT2wt();
        network.getTwoWindingsTransformer("T2wT").getTerminal2().disconnect();

        OpenReacResult result = runOpenReac(network, "optimization/opened-branches/rtc-open-side-2");
        // verify ratio tap changer is considered in optimization
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_branch_in_AC_CC_side_2_opened")));
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_transformers_with_fixed_ratio")));

        assertEquals(OpenReacStatus.OK, result.getStatus());
        result.applyAllModifications(network);
        LoadFlowResult loadFlowResult = LoadFlow.run(network);
        assertTrue(loadFlowResult.isFullyConverged());
    }

    @Test
    void testRatioTapChangerOpenSide1OpenReac() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        network.getTwoWindingsTransformer("T2wT1").getTerminal1().disconnect();

        OpenReacResult result = runOpenReac(network, "optimization/opened-branches/rtc-open-side-1");
        // verify ratio tap changer on T2wT1 is ignored in optimization
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_branch_in_AC_CC_side_1_opened")));
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_transformers_with_fixed_ratio")));

        assertEquals(OpenReacStatus.OK, result.getStatus());
        result.applyAllModifications(network);
        LoadFlowResult loadFlowResult = LoadFlow.run(network);
        assertTrue(loadFlowResult.isFullyConverged());
    }

    @Test
    void testRatioTapChangerNotOptimizedOnOpenBranch() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        network.getTwoWindingsTransformer("T2wT1").getTerminal2().disconnect();
        network.getTwoWindingsTransformer("T2wT1").getRatioTapChanger().setTapPosition(0);
        network.getTwoWindingsTransformer("T2wT2").getRatioTapChanger().setTapPosition(3);

        OpenReacParameters parameters = new OpenReacParameters();
        // try to optimize both ratio tap changers
        parameters.addVariableTwoWindingsTransformers(List.of("T2wT1", "T2wT2"));
        OpenReacResult result = runOpenReac(network, "optimization/opened-branches/rtc-not-optimized", parameters, false);

        // verify only one rtc has been optimized
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_transformers_with_variable_ratio")));
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_transformers_with_fixed_ratio")));

        // verify only tap of optimized rtc has changed
        result.applyAllModifications(network);
        assertEquals(0, network.getTwoWindingsTransformer("T2wT1").getRatioTapChanger().getTapPosition());
        assertEquals(0, network.getTwoWindingsTransformer("T2wT2").getRatioTapChanger().getTapPosition());
    }

}

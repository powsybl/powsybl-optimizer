/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.openreac.parameters.output.ReactiveSlackOutput;

import java.util.Map;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public final class OpenReacTest {

    public static void main(String[] args) throws Exception {

        Network network = IeeeCdfNetworkFactory.create14Solved();
        // Setup parameters
        OpenReacParameters parameters = new OpenReacParameters();

        parameters.addVariableTwoWindingsTransformers("T4-7-1");
        parameters.addTargetQGenerators("B1-G");
//        parameters.addVariableShuntCompensators("shunt_id"); // No shunt in IEEE14

        OpenReacResult openReacResult = OpenReacRunner.run(network,
                network.getVariantManager().getWorkingVariantId(), parameters);

        // Exploiting OpenReac output
        System.out.println(openReacResult.getStatus());
        for (ReactiveSlackOutput.ReactiveSlack investment : openReacResult.getReactiveSlacks()) {
            System.out.println(
                    "investment : " + investment.busId + " " + investment.substationId + " " + investment.slack);
        }
        for (Map.Entry<String, String> entry : openReacResult.getIndicators().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());

        }
    }

    private OpenReacTest() {
    }
}

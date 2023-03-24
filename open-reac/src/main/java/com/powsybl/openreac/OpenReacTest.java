/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.ReactiveSlackOutput;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public final class OpenReacTest {

    public static void main(String[] args) throws Exception {

        Network network = IeeeCdfNetworkFactory.create118();

        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableTwoWindingsTransformers(network.getTwoWindingsTransformerStream().map(TwoWindingsTransformer::getId).limit(10).collect(Collectors.toList()));
        parameters.addTargetQGenerators(network.getGeneratorStream().map(Generator::getId).limit(3).collect(Collectors.toList()));
        parameters.addVariableShuntCompensators(network.getShuntCompensatorStream().map(ShuntCompensator::getId).limit(10).collect(Collectors.toList()));

        // parameters.addSpecificVoltageLimitDelta("vl", 10, 20);

        OpenReacResult openReacResult = OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), parameters);

        System.out.println(openReacResult.getStatus());
        for (ReactiveSlackOutput.ReactiveSlack investment : openReacResult.getReactiveSlacks()) {
            System.out.println("investment : " + investment.busId + " " + investment.substationId + " " + investment.slack);
        }
        for (Map.Entry<String, String> entry : openReacResult.getIndicators().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private OpenReacTest() {
    }
}

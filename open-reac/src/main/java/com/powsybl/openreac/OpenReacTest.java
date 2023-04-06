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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public final class OpenReacTest {

    public static void main(String[] args) {

        Network network = IeeeCdfNetworkFactory.create118();

        OpenReacParameters parameters = new OpenReacParameters();
//        parameters.addAlgorithmParam(List.of(new OptimisationVoltageRatio(1), OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_PROFILE))
        parameters.addRatioVoltageObjective(1)
                .addVariableTwoWindingsTransformers(network.getTwoWindingsTransformerStream().limit(1).map(TwoWindingsTransformer::getId).collect(Collectors.toList()))
                .addConstantQGerenartors(network.getGeneratorStream().limit(1).map(Generator::getId).collect(Collectors.toList()))
                .addVariableShuntCompensators(network.getShuntCompensatorStream().limit(1).map(ShuntCompensator::getId).collect(Collectors.toList()));

        OpenReacResult openReacResult = OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), parameters);

        System.out.println(openReacResult.getStatus());
        for (ReactiveSlackOutput.ReactiveSlack investment : openReacResult.getReactiveSlacks()) {
            System.out.println("investment : " + investment.busId + " " + investment.voltageLevelId + " " + investment.slack);
        }
        List<Map.Entry<String, String>> sortedEntries = openReacResult.getIndicators().entrySet()
                                                            .stream()
                                                            .sorted(Comparator.comparing(Map.Entry::getKey))
                                                            .collect(Collectors.toList());
        for (Map.Entry<String, String> entry : sortedEntries) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private OpenReacTest() {
    }
}

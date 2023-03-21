/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResults;
import com.powsybl.openreac.parameters.output.ReactiveInvestmentOutput;

import java.util.Map;
import java.util.Properties;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        Network network = Importers.importData("XIIDM", "./", "ieee14", new Properties());
        // Setup parameters
        OpenReacParameters parameters = new OpenReacParameters();

        // Not working for now because the AMPL .dat file is not up-to-date
        //        parameters.addVariableTransformator("transformer_id");
        //        parameters.addFixedReactanceGenerators("generator_id");
        //        parameters.addVariableReactanceShunts("shunt_id");

        OpenReacResults openReacResults = OpenReacRunner.runOpenReac(network,
                network.getVariantManager().getWorkingVariantId(), parameters);

        // Exploiting OpenReac output
        System.out.println(openReacResults.getStatus());
        for (ReactiveInvestmentOutput.ReactiveInvestment investment : openReacResults.getReactiveInvestments()) {
            System.out.println(
                    "investment : " + investment.busId + " " + investment.substationId + " " + investment.slack);
        }
        for (Map.Entry<String, String> entry : openReacResults.getIndicators().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());

        }
    }

}

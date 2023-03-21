/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.ampl.executor.AmplModel;
import com.powsybl.ampl.executor.AmplModelRunner;
import com.powsybl.ampl.executor.AmplResults;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.parameters.OpenReacAmplIOFiles;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public final class OpenReacRunner {

    private OpenReacRunner() {
    }

    public static OpenReacResult run(Network network, String variant, OpenReacParameters parameters) {
        AmplModel reactiveOpf = OpenReacModel.buildModel();
        ComputationManager manager = LocalComputationManager.getDefault();
        OpenReacAmplIOFiles amplIoInterface = new OpenReacAmplIOFiles(parameters);
        before(network, parameters);
        AmplResults run = AmplModelRunner.run(network, variant, reactiveOpf, manager, amplIoInterface);
        after(network, parameters);
        return new OpenReacResult(run.isSuccess() ? OpenReacResult.OpenReacStatus.OK : OpenReacResult.OpenReacStatus.NOT_OK,
                amplIoInterface.getReactiveInvestments(), amplIoInterface.getIndicators());
    }

    /**
     * This function allows network modifications before the run.
     * <p>
     * For now, we only modify the voltage limits of voltage levels.
     */
    private static void before(Network network, OpenReacParameters params) {
        for (String voltageLevelId : params.getSpecificVoltageDelta().keySet()) {
            Pair<Double, Double> bounds = params.getSpecificVoltageDelta().get(voltageLevelId);
            double nominalV = network.getVoltageLevel(voltageLevelId).getNominalV();
            network.getVoltageLevel(voltageLevelId)
                   .setLowVoltageLimit(nominalV * bounds.getLeft())
                   .setHighVoltageLimit(nominalV * bounds.getRight());
        }
    }

    /**
     * This function allows network modifications after the run
     * <p>
     * For now, we only modify the bounds of voltage levels.
     */
    private static void after(Network network, OpenReacParameters params) {
        for (String voltageLevelId : params.getSpecificVoltageDelta().keySet()) {
            Pair<Double, Double> bounds = params.getSpecificVoltageDelta().get(voltageLevelId);
            double nominalV = network.getVoltageLevel(voltageLevelId).getNominalV();
            network.getVoltageLevel(voltageLevelId)
                   .setLowVoltageLimit(nominalV * bounds.getLeft())
                   .setHighVoltageLimit(nominalV * bounds.getRight());
        }
    }
}

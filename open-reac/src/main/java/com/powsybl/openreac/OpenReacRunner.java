package com.powsybl.openreac;

import com.powsybl.ampl.executor.AmplModel;
import com.powsybl.ampl.executor.AmplModelRunner;
import com.powsybl.ampl.executor.AmplResults;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.parameters.OpenReacAmplIOFiles;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResults;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.apache.commons.lang3.tuple.Pair;

public final class OpenReacRunner {

    private OpenReacRunner() {
    }

    public static OpenReacResults runOpenReac(Network network, String variant, OpenReacParameters parameters) {
        AmplModel reactiveOpf = OpenReacModel.buildModel();
        ComputationManager manager = LocalComputationManager.getDefault();
        OpenReacAmplIOFiles amplIoInterface = new OpenReacAmplIOFiles(parameters);
        preRunModifications(network, parameters);
        AmplResults run = AmplModelRunner.run(network, variant, reactiveOpf, manager, amplIoInterface);
        postRunModifications(network, parameters);
        return new OpenReacResults(run.isSuccess() ? OpenReacStatus.OK : OpenReacStatus.NOT_OK,
                amplIoInterface.getReactiveInvestments(), amplIoInterface.getIndicators());
    }

    /**
     * This function allows modifications before running the Ampl Run.
     * <p>
     * For now, we only modify the bounds of voltage levels.
     */
    private static void preRunModifications(Network network, OpenReacParameters params) {
        for (String voltageLevelId : params.getSpecificVoltageDelta().keySet()) {
            Pair<Double, Double> bounds = params.getSpecificVoltageDelta().get(voltageLevelId);
            double nominalV = network.getVoltageLevel(voltageLevelId).getNominalV();
            network.getVoltageLevel(voltageLevelId)
                   .setLowVoltageLimit(nominalV * bounds.getLeft())
                   .setHighVoltageLimit(nominalV * bounds.getRight());
        }
    }

    /**
     * This function allows modifications before running the Ampl Run.
     * <p>
     * For now, we only modify the bounds of voltage levels.
     */
    private static void postRunModifications(Network network, OpenReacParameters params) {
        for (String voltageLevelId : params.getSpecificVoltageDelta().keySet()) {
            Pair<Double, Double> bounds = params.getSpecificVoltageDelta().get(voltageLevelId);
            double nominalV = network.getVoltageLevel(voltageLevelId).getNominalV();
            network.getVoltageLevel(voltageLevelId)
                   .setLowVoltageLimit(nominalV * bounds.getLeft())
                   .setHighVoltageLimit(nominalV * bounds.getRight());
        }
    }
}

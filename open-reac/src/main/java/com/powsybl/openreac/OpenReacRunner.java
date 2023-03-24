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

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public final class OpenReacRunner {

    private OpenReacRunner() {
    }

    public static OpenReacResult run(Network network, String variant, OpenReacParameters parameters) {
        AmplModel reactiveOpf = OpenReacModel.buildModel();
        ComputationManager manager = LocalComputationManager.getDefault();
        parameters.checkIntegrity(network);
        OpenReacAmplIOFiles amplIoInterface = new OpenReacAmplIOFiles(parameters, network);
        AmplResults run;
        try {
            run = AmplModelRunner.run(network, variant, reactiveOpf, manager, amplIoInterface);
        } catch (Exception e) {
            // Ampl run crashed
            run = new AmplResults(false);
        }
        return new OpenReacResult(run.isSuccess() && amplIoInterface.checkErrors() ? OpenReacResult.OpenReacStatus.OK : OpenReacResult.OpenReacStatus.NOT_OK,
                amplIoInterface.getReactiveInvestments(), amplIoInterface.getIndicators());
    }
}

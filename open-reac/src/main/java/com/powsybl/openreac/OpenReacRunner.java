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
import com.powsybl.openreac.parameters.output.OpenReacStatus;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public final class OpenReacRunner {

    private OpenReacRunner() {
    }

    public static OpenReacResult run(Network network, String variantId, OpenReacParameters parameters) {
        return run(network, variantId, parameters, new OpenReacConfig(false), LocalComputationManager.getDefault());
    }

    public static OpenReacResult run(Network network, String variantId, OpenReacParameters parameters, OpenReacConfig config, ComputationManager manager) {
        parameters.checkIntegrity(network);
        AmplModel reactiveOpf = OpenReacModel.buildModel();
        OpenReacAmplIOFiles amplIoInterface = new OpenReacAmplIOFiles(parameters, network, config.isDebug());
        AmplResults run = AmplModelRunner.run(network, variantId, reactiveOpf, manager, amplIoInterface);
        return new OpenReacResult(run.isSuccess() && amplIoInterface.checkErrors() ? OpenReacStatus.OK : OpenReacStatus.NOT_OK,
                amplIoInterface, run.getIndicators());
    }
}

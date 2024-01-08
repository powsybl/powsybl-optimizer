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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public final class OpenReacRunner {

    private OpenReacRunner() {
    }

    /**
     * Run OpenReac on the given network. It will NOT modify the network.
     * @param variantId the network variant to use. It will set the variant on the network.
     * @param parameters Parameters to customize the OpenReac run.
     * @return All information about the run and possible modifications to apply.
     */
    public static OpenReacResult run(Network network, String variantId, OpenReacParameters parameters) {
        return run(network, variantId, parameters, new OpenReacConfig(false), LocalComputationManager.getDefault());
    }

    /**
     * Run OpenReac on the given network. It will NOT modify the network.
     * @param variantId the network variant to use. It will set the variant on the network.
     * @param parameters Parameters to customize the OpenReac run.
     * @param config allows debugging
     * @return All information about the run and possible modifications to apply.
     */
    public static OpenReacResult run(Network network, String variantId, OpenReacParameters parameters, OpenReacConfig config, ComputationManager manager) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(variantId);
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(config);
        Objects.requireNonNull(manager);
        parameters.checkIntegrity(network);
        AmplModel reactiveOpf = OpenReacModel.buildModel();
        OpenReacAmplIOFiles amplIoInterface = new OpenReacAmplIOFiles(parameters, network, config.isDebug());
        AmplResults run = AmplModelRunner.run(network, variantId, reactiveOpf, manager, amplIoInterface);
        return new OpenReacResult(run.isSuccess() && amplIoInterface.checkErrors() ? OpenReacStatus.OK : OpenReacStatus.NOT_OK,
                amplIoInterface, run.getIndicators());
    }

    /**
     * Run OpenReac on the given network. It will NOT modify the network.
     * @param variantId the network variant to use. It will set the variant on the network.
     * @param parameters Parameters to customize the OpenReac run.
     * @param config allows debugging
     * @return All information about the run and possible modifications to apply.
     */
    public static CompletableFuture<OpenReacResult> runAsync(Network network, String variantId, OpenReacParameters parameters, OpenReacConfig config, ComputationManager manager) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(variantId);
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(config);
        Objects.requireNonNull(manager);
        parameters.checkIntegrity(network);
        AmplModel reactiveOpf = OpenReacModel.buildModel();
        OpenReacAmplIOFiles amplIoInterface = new OpenReacAmplIOFiles(parameters, network, config.isDebug());
        CompletableFuture<AmplResults> runAsync = AmplModelRunner.runAsync(network, variantId, reactiveOpf, manager, amplIoInterface);
        return runAsync.thenApply(run -> new OpenReacResult(run.isSuccess() && amplIoInterface.checkErrors() ? OpenReacStatus.OK : OpenReacStatus.NOT_OK,
                amplIoInterface, run.getIndicators()));
    }
}

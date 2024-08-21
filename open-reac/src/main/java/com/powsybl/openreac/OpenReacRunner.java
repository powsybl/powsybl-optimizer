/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.ampl.converter.AmplExportConfig;
import com.powsybl.ampl.executor.AmplModel;
import com.powsybl.ampl.executor.AmplModelRunner;
import com.powsybl.ampl.executor.AmplResults;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.openloadflow.FullVoltageInitializer;
import com.powsybl.openloadflow.ac.VoltageMagnitudeInitializer;
import com.powsybl.openloadflow.dc.DcValueVoltageInitializer;
import com.powsybl.openloadflow.dc.equations.DcApproximationType;
import com.powsybl.openloadflow.network.*;
import com.powsybl.openloadflow.network.impl.LfGeneratorImpl;
import com.powsybl.openloadflow.network.impl.LfNetworkLoaderImpl;
import com.powsybl.openreac.parameters.OpenReacAmplIOFiles;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
 */
public final class OpenReacRunner {

    private static final String WITH_DC_LOAD_FLOW_ID_VARIANT_SUFFIX = "WithDcLoadFlow";

    private OpenReacRunner() {
    }

    /**
     * Run OpenReac on the given network. It will NOT modify the network.
     *
     * @param variantId  the network variant to use. It will set the variant on the network.
     * @param parameters parameters to customize the OpenReac run.
     * @return All information about the run and possible modifications to apply.
     */
    public static OpenReacResult run(Network network, String variantId, OpenReacParameters parameters) {
        return run(network, variantId, parameters, new OpenReacConfig(false), LocalComputationManager.getDefault(), ReportNode.NO_OP, null);
    }

    /**
     * Run OpenReac on the given network. It will NOT modify the network.
     *
     * @param variantId  the network variant to use. It will set the variant on the network.
     * @param parameters parameters to customize the OpenReac run.
     * @param config     allows debugging
     * @param manager    the ComputationManager to use
     * @return All information about the run and possible modifications to apply.
     */
    public static OpenReacResult run(Network network, String variantId, OpenReacParameters parameters, OpenReacConfig config, ComputationManager manager) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(variantId);
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(config);
        Objects.requireNonNull(manager);
        return run(network, variantId, parameters, config, manager, ReportNode.NO_OP, null);
    }

    /**
     * Run OpenReac on the given network. It will NOT modify the network.
     *
     * @param variantId         the network variant to use. It will set the variant on the network.
     * @param parameters        parameters to customize the OpenReac run.
     * @param config            allows debugging
     * @param manager           the ComputationManager to use
     * @param reportNode        aggregates functional logging
     * @param amplExportConfig  enables tuning of Ampl exporter
     * @return All information  about the run and possible modifications to apply.
     */
    public static OpenReacResult run(Network network, String variantId, OpenReacParameters parameters, OpenReacConfig config, ComputationManager manager, ReportNode reportNode, AmplExportConfig amplExportConfig) {
        checkParameters(network, variantId, parameters, config, manager, reportNode);
        AmplModel reactiveOpf = OpenReacModel.buildModel();
        OpenReacAmplIOFiles amplIoInterface = new OpenReacAmplIOFiles(parameters, amplExportConfig, network, config.isDebug(), Reports.createOpenReacReporter(reportNode, network.getId(), parameters.getObjective()));

        AmplResults run;
        if (parameters.isDcLoadFlowBeforeOptimization()) {
            // create new variant to avoid modifying the network
            VariantManager variantManager = network.getVariantManager();
            variantManager.cloneVariant(variantId, WITH_DC_LOAD_FLOW_ID_VARIANT_SUFFIX);
            variantManager.setWorkingVariant(WITH_DC_LOAD_FLOW_ID_VARIANT_SUFFIX);
            // warm start the optimization with dc load flow results
            runDcLf(network, parameters);
            run = AmplModelRunner.run(network, WITH_DC_LOAD_FLOW_ID_VARIANT_SUFFIX, reactiveOpf, manager, amplIoInterface);
            // remove the variant created to store dc load flow results
            variantManager.setWorkingVariant(variantId);
            variantManager.removeVariant(WITH_DC_LOAD_FLOW_ID_VARIANT_SUFFIX);
        } else {
            run = AmplModelRunner.run(network, variantId, reactiveOpf, manager, amplIoInterface);
        }

        OpenReacResult result = new OpenReacResult(run.isSuccess() && amplIoInterface.checkErrors() ? OpenReacStatus.OK : OpenReacStatus.NOT_OK, amplIoInterface, run.getIndicators());
        Reports.createShuntModificationsReporter(reportNode, network.getId(), amplIoInterface.getNetworkModifications().getShuntsWithDeltaDiscreteOptimalOverThreshold());
        return result;
    }

    /**
     * Run OpenReac on the given network. It will NOT modify the network.
     *
     * @param variantId     the network variant to use. It will set the variant on the network.
     * @param parameters    parameters to customize the OpenReac run.
     * @param config        allows debugging
     * @param manager       the ComputationManager to use
     * @return All information about the run and possible modifications to apply.
     */
    public static CompletableFuture<OpenReacResult> runAsync(Network network, String variantId, OpenReacParameters parameters, OpenReacConfig config, ComputationManager manager) {
        return runAsync(network, variantId, parameters, config, manager, ReportNode.NO_OP, null);
    }

    /**
     * Run OpenReac on the given network. It will NOT modify the network.
     *
     * @param variantId         the network variant to use. It will set the variant on the network.
     * @param parameters        parameters to customize the OpenReac run.
     * @param config            allows debugging
     * @param reportNode        aggregates functional logging
     * @param amplExportConfig  enables tuning of Ampl exporter
     * @return All information about the run and possible modifications to apply.
     */
    public static CompletableFuture<OpenReacResult> runAsync(Network network, String variantId, OpenReacParameters parameters, OpenReacConfig config, ComputationManager manager, ReportNode reportNode, AmplExportConfig amplExportConfig) {
        checkParameters(network, variantId, parameters, config, manager, reportNode);
        AmplModel reactiveOpf = OpenReacModel.buildModel();
        OpenReacAmplIOFiles amplIoInterface = new OpenReacAmplIOFiles(parameters, amplExportConfig, network, config.isDebug(), Reports.createOpenReacReporter(reportNode, network.getId(), parameters.getObjective()));

        CompletableFuture<AmplResults> runAsync;
        if (parameters.isDcLoadFlowBeforeOptimization()) {
            // create new variant to avoid modifying the network
            VariantManager variantManager = network.getVariantManager();
            variantManager.cloneVariant(variantId, WITH_DC_LOAD_FLOW_ID_VARIANT_SUFFIX);
            variantManager.setWorkingVariant(WITH_DC_LOAD_FLOW_ID_VARIANT_SUFFIX);
            // warm start the optimization with dc load flow results
            runDcLf(network, parameters);
            runAsync = AmplModelRunner.runAsync(network, WITH_DC_LOAD_FLOW_ID_VARIANT_SUFFIX, reactiveOpf, manager, amplIoInterface);
            // remove the variant created to store dc load flow results
        } else {
            runAsync = AmplModelRunner.runAsync(network, variantId, reactiveOpf, manager, amplIoInterface);
        }

        return runAsync.thenApply(run -> {
            network.getVariantManager().setWorkingVariant(variantId);
            network.getVariantManager().removeVariant(WITH_DC_LOAD_FLOW_ID_VARIANT_SUFFIX);
            OpenReacResult result = new OpenReacResult(run.isSuccess() && amplIoInterface.checkErrors() ? OpenReacStatus.OK : OpenReacStatus.NOT_OK, amplIoInterface, run.getIndicators());
            Reports.createShuntModificationsReporter(reportNode, network.getId(), amplIoInterface.getNetworkModifications().getShuntsWithDeltaDiscreteOptimalOverThreshold());
            return result;
        });
    }

    private static void checkParameters(Network network, String variantId, OpenReacParameters parameters, OpenReacConfig config, ComputationManager manager, ReportNode reportNode) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(variantId);
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(config);
        Objects.requireNonNull(manager);
        Objects.requireNonNull(reportNode);
        parameters.checkIntegrity(network, Reports.createParameterIntegrityReporter(reportNode, network.getId()));
    }

    private static void runDcLf(Network network, OpenReacParameters openReacParameters) {
        // slack bus selection
        SlackBusSelector slackBusSelector = new MostMeshedSlackBusSelector();
        LfNetwork lfNetwork = LfNetwork.load(network, new LfNetworkLoaderImpl(), slackBusSelector).get(0);
        MatrixFactory matrixFactory = new DenseMatrixFactory();

        // full voltage initializer, to initialize voltage magnitudes and angles
        FullVoltageInitializer initializer = new FullVoltageInitializer(
                new VoltageMagnitudeInitializer(false, matrixFactory, openReacParameters.getLowImpedanceThreshold()),
                new DcValueVoltageInitializer(new LfNetworkParameters().setSlackBusSelector(slackBusSelector),
                        true,
                        LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P,
                        true,
                        DcApproximationType.IGNORE_R,
                        matrixFactory,
                        1)); // FIXME : which number is optimal ?

        // compute initialization
        initializer.prepare(lfNetwork);

        // update the state of the buses and of the generators to warm start AC optimization
        LfNetworkStateUpdateParameters updateParameters = new LfNetworkStateUpdateParameters(false, false, true, false, false,
                false, true, false, ReactivePowerDispatchMode.Q_EQUAL_PROPORTION, false, ReferenceBusSelectionMode.FIRST_SLACK, false);
        lfNetwork.getBuses().forEach(bus -> bus.updateState(updateParameters));
        lfNetwork.getBuses().stream()
                .flatMap(lfBus -> lfBus.getGenerators().stream())
                .filter(lfGenerator -> lfGenerator instanceof LfGeneratorImpl) // only active power of generators is optimized in OpenReac, not one of batteries and vsc
                .forEach(lfGenerator -> lfGenerator.updateState(updateParameters));
    }
}

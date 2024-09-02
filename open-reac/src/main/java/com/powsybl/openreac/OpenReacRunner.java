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
import com.powsybl.iidm.network.extensions.ActivePowerControlAdder;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.math.matrix.SparseMatrixFactory;
import com.powsybl.openloadflow.FullVoltageInitializer;
import com.powsybl.openloadflow.ac.VoltageMagnitudeInitializer;
import com.powsybl.openloadflow.dc.DcValueVoltageInitializer;
import com.powsybl.openloadflow.dc.equations.DcApproximationType;
import com.powsybl.openloadflow.network.*;
import com.powsybl.openloadflow.network.impl.LfNetworkLoaderImpl;
import com.powsybl.openloadflow.network.util.UniformValueVoltageInitializer;
import com.powsybl.openloadflow.network.util.VoltageInitializer;
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

    private static final String WITH_USER_INITIALIZATION_ID_VARIANT = "WithDcLoadFlow";

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
        // current voltage values are used to initialize ACOPF optimization
        if (parameters.getVoltageInitialization() == OpenReacParameters.OpenReacVoltageInitialization.PREVIOUS_VALUES) {
            run = AmplModelRunner.run(network, variantId, reactiveOpf, manager, amplIoInterface);

        // initialize optimization with user option
        } else {
            // create new variant to avoid modifications of the network
            VariantManager variantManager = network.getVariantManager();
            variantManager.cloneVariant(variantId, WITH_USER_INITIALIZATION_ID_VARIANT);
            variantManager.setWorkingVariant(WITH_USER_INITIALIZATION_ID_VARIANT);

            // initialize the optimization with given process
            initializeVoltageBeforeOptimization(network, parameters);

            // execute ampl code
            run = AmplModelRunner.run(network, WITH_USER_INITIALIZATION_ID_VARIANT, reactiveOpf, manager, amplIoInterface);

            // remove the variant created to store dc load flow results
            variantManager.setWorkingVariant(variantId);
            variantManager.removeVariant(WITH_USER_INITIALIZATION_ID_VARIANT);
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
        if (parameters.getVoltageInitialization() == OpenReacParameters.OpenReacVoltageInitialization.PREVIOUS_VALUES) {
            runAsync = AmplModelRunner.runAsync(network, variantId, reactiveOpf, manager, amplIoInterface);
        } else {
            // create new variant to avoid modifications of the network
            VariantManager variantManager = network.getVariantManager();
            variantManager.cloneVariant(variantId, WITH_USER_INITIALIZATION_ID_VARIANT);
            variantManager.setWorkingVariant(WITH_USER_INITIALIZATION_ID_VARIANT);

            // initialize the optimization with given process
            initializeVoltageBeforeOptimization(network, parameters);

            // execute ampl code
            runAsync = AmplModelRunner.runAsync(network, WITH_USER_INITIALIZATION_ID_VARIANT, reactiveOpf, manager, amplIoInterface);
        }

        return runAsync.thenApply(run -> {
            if (parameters.getVoltageInitialization() != OpenReacParameters.OpenReacVoltageInitialization.PREVIOUS_VALUES) {
                // remove the variant created to store dc load flow results
                network.getVariantManager().setWorkingVariant(variantId);
                network.getVariantManager().removeVariant(WITH_USER_INITIALIZATION_ID_VARIANT);
            }
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

    private static void initializeVoltageBeforeOptimization(Network network, OpenReacParameters openReacParameters) {
        // gsk to only distribute on generators
        network.getGeneratorStream()
                .forEach(generator -> generator.newExtension(ActivePowerControlAdder.class)
                        .withParticipate(true)
                        .withParticipationFactor(generator.getTargetP()));

        // slack bus selection
        SlackBusSelector slackBusSelector = new MostMeshedSlackBusSelector();

        // get lfNetwork to apply voltage initialization
        LfNetwork lfNetwork = LfNetwork.load(network, new LfNetworkLoaderImpl(), slackBusSelector).get(0);

        // get initializer depending on user option
        VoltageInitializer initializer;
        MatrixFactory matrixFactory = new SparseMatrixFactory();
        switch (openReacParameters.getVoltageInitialization()) {
            // uniform voltage initializer, for flat start voltage angles
            case UNIFORM_VALUES -> initializer = new UniformValueVoltageInitializer();
            // direct current initializer, to initialize voltage angles
            case DC_VALUES -> initializer = new DcValueVoltageInitializer(new LfNetworkParameters().setSlackBusSelector(slackBusSelector),
                    true,
                                LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_PARTICIPATION_FACTOR,
                                true,
                                DcApproximationType.IGNORE_R,
                                matrixFactory,
                                1);
            // full voltage initializer, to initialize voltage magnitudes and angles
            case FULL_VOLTAGE -> initializer = new FullVoltageInitializer(
                    new VoltageMagnitudeInitializer(false, matrixFactory, openReacParameters.getLowImpedanceThreshold()),
                    new DcValueVoltageInitializer(new LfNetworkParameters().setSlackBusSelector(slackBusSelector),
                            true,
                            LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_PARTICIPATION_FACTOR,
                            true,
                            DcApproximationType.IGNORE_R,
                            matrixFactory,
                            1));
            default -> throw new IllegalStateException("Unexpected value: " + openReacParameters.getVoltageInitialization());
        }

        // initialize voltage values
        initializer.prepare(lfNetwork);

        // update the network with initialization
        LfNetworkStateUpdateParameters updateParameters = new LfNetworkStateUpdateParameters(false, false, true, false, false,
                false, true, false, ReactivePowerDispatchMode.Q_EQUAL_PROPORTION, false, ReferenceBusSelectionMode.FIRST_SLACK, false);
        lfNetwork.updateState(updateParameters);
    }
}

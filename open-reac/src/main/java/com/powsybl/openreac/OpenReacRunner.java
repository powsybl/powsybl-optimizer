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
import com.powsybl.openreac.parameters.OpenReacAmplIOFiles;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public final class OpenReacRunner {

    private static final String INITIALIZED_VARIANT_PREFIX = "OpenReac_AcopfInit_";

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
    public static OpenReacResult run(Network network, String variantId, OpenReacParameters parameters, OpenReacConfig config,
                                     ComputationManager manager, ReportNode reportNode, AmplExportConfig amplExportConfig) {
        checkParameters(network, variantId, parameters, config, manager, reportNode);
        ReportNode openReacReportNode = Reports.createOpenReacReporter(reportNode, network.getId(), parameters.getObjective());
        AmplModel reactiveOpf = OpenReacModel.buildModel();
        InitializedVariant initializedVariant = createInitializedVariant(network, variantId, openReacReportNode);
        try {
            OpenReacAmplIOFiles amplIoInterface = buildIoFilesOnVariant(network, initializedVariant, parameters, amplExportConfig, config, openReacReportNode);
            AmplResults run = AmplModelRunner.run(network, initializedVariant.id(), reactiveOpf, manager, amplIoInterface);
            return buildResult(network, run, amplIoInterface, reportNode);
        } finally {
            removeInitializedVariant(network, variantId, initializedVariant.id());
        }
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
    public static CompletableFuture<OpenReacResult> runAsync(Network network, String variantId, OpenReacParameters parameters,
                                                             OpenReacConfig config, ComputationManager manager, ReportNode reportNode, AmplExportConfig amplExportConfig) {
        checkParameters(network, variantId, parameters, config, manager, reportNode);
        ReportNode openReacReportNode = Reports.createOpenReacReporter(reportNode, network.getId(), parameters.getObjective());
        AmplModel reactiveOpf = OpenReacModel.buildModel();
        InitializedVariant initializedVariant = createInitializedVariant(network, variantId, openReacReportNode);
        boolean submitted = false;
        try {
            OpenReacAmplIOFiles amplIoInterface = buildIoFilesOnVariant(network, initializedVariant, parameters, amplExportConfig, config, openReacReportNode);
            CompletableFuture<OpenReacResult> runAsync = AmplModelRunner.runAsync(network, initializedVariant.id(), reactiveOpf, manager, amplIoInterface)
                    .thenApply(run -> buildResult(network, run, amplIoInterface, reportNode))
                    .whenComplete((result, throwable) -> removeInitializedVariant(network, variantId, initializedVariant.id()));
            submitted = true;
            return runAsync;
        } finally {
            if (!submitted) {
                // the asynchronous run never started: nothing else will remove the initialized variant
                removeInitializedVariant(network, variantId, initializedVariant.id());
            }
        }
    }

    private static OpenReacResult buildResult(Network network, AmplResults run, OpenReacAmplIOFiles amplIoInterface, ReportNode reportNode) {
        OpenReacResult result = new OpenReacResult(run.isSuccess() && amplIoInterface.checkErrors() ? OpenReacStatus.OK : OpenReacStatus.NOT_OK,
                amplIoInterface, run.getIndicators());
        Reports.createShuntModificationsReporter(reportNode, network.getId(), amplIoInterface.getNetworkModifications().getShuntsWithDeltaDiscreteOptimalOverThreshold());
        return result;
    }

    /**
     * Clones the requested variant and writes the ACOPF starting point on the clone, so that the AMPL
     * export carries it while the requested variant is left untouched. The working variant is restored
     * before returning.
     *
     * @return the initialized variant, to be removed with {@link #removeInitializedVariant}.
     */
    private static InitializedVariant createInitializedVariant(Network network, String variantId, ReportNode openReacReportNode) {
        VariantManager variantManager = network.getVariantManager();
        String previousVariantId = variantManager.getWorkingVariantId();
        String initializedVariantId = INITIALIZED_VARIANT_PREFIX + UUID.randomUUID();
        variantManager.cloneVariant(variantId, initializedVariantId);
        variantManager.setWorkingVariant(initializedVariantId);
        String slackBusId = null;
        try {
            slackBusId = AcopfInitializer.initialize(network, Reports.createAcopfInitializationReporter(openReacReportNode, network.getId()));
        } finally {
            variantManager.setWorkingVariant(previousVariantId);
            if (slackBusId == null) {
                variantManager.removeVariant(initializedVariantId);
            }
        }
        return new InitializedVariant(initializedVariantId, slackBusId);
    }

    /**
     * Variant carrying the ACOPF starting point, and the bus of that starting point used as angle reference.
     */
    private record InitializedVariant(String id, String slackBusId) {
    }

    /**
     * Removes the initialized variant. The working variant is set to the requested one, as the AMPL
     * executor used to leave it before the initialization was introduced.
     */
    private static void removeInitializedVariant(Network network, String variantId, String initializedVariantId) {
        VariantManager variantManager = network.getVariantManager();
        variantManager.setWorkingVariant(variantId);
        variantManager.removeVariant(initializedVariantId);
    }

    /**
     * Builds the AMPL IO files on the requested variant: the parallel transformers detection
     * reads the network through its current working variant (bus view, current taps), so the
     * variant is set for the time of the construction and restored right after.
     */
    private static OpenReacAmplIOFiles buildIoFilesOnVariant(Network network, InitializedVariant variant, OpenReacParameters parameters,
                                                             AmplExportConfig amplExportConfig, OpenReacConfig config, ReportNode openReacReportNode) {
        String previousVariantId = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().setWorkingVariant(variant.id());
        try {
            return new OpenReacAmplIOFiles(parameters, amplExportConfig, network, variant.slackBusId(), config.isDebug(), openReacReportNode);
        } finally {
            network.getVariantManager().setWorkingVariant(previousVariantId);
        }
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
}

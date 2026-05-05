/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.RatioTapChanger;
import com.powsybl.iidm.network.RatioTapChangerStep;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openreac.parameters.input.VoltageLevelLimitInfo;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.output.network.ShuntCompensatorNetworkOutput;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector;
import org.apache.commons.lang3.tuple.Pair;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini_externe at rte-france.com>}
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public final class Reports {

    private static final String NETWORK_ID = "networkId";
    private static final String SIZE = "size";
    private static final String BRANCH_ID = "branchId";
    private static final String RESISTANCE = "r";
    private static final String REACTANCE = "x";
    private static final String RATIO = "ratio";

    private static final DecimalFormat VALUE_FORMAT = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ROOT));
    private static final DecimalFormat VALUE_FORMAT_ACCURATE = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));
    private static final DecimalFormat VALUE_FORMAT_SCIENTIFIC = new DecimalFormat("0.00E00", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private Reports() {
        // Should not be instantiated
    }

    public static ReportNode createOpenReacReporter(ReportNode reportNode, String networkId, OpenReacOptimisationObjective objective) {
        return reportNode.newReportNode()
            .withMessageTemplate("optimizer.openreac.openReac")
            .withUntypedValue(NETWORK_ID, networkId)
            .withUntypedValue("objective", objective.toString())
            .add();
    }

    public static ReportNode createParameterIntegrityReporter(ReportNode reportNode, String networkId) {
        return reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.openReacParameterIntegrity")
                .withUntypedValue(NETWORK_ID, networkId)
                .add();
    }

    public static void createShuntModificationsReporter(ReportNode reportNode, String networkId, List<ShuntCompensatorNetworkOutput.ShuntWithDeltaDiscreteOptimalOverThreshold> shuntsWithDeltaDiscreteOptimalOverThresholds) {
        if (!shuntsWithDeltaDiscreteOptimalOverThresholds.isEmpty()) {
            ReportNode reportShunts = reportNode.newReportNode()
                    .withMessageTemplate("optimizer.openreac.shuntCompensatorDeltaOverThreshold")
                    .withUntypedValue(NETWORK_ID, networkId)
                    .add();
            reportShunts.newReportNode()
                    .withMessageTemplate("optimizer.openreac.shuntCompensatorDeltaOverThresholdCount")
                    .withUntypedValue("shuntsCount", shuntsWithDeltaDiscreteOptimalOverThresholds.size())
                    .withSeverity(TypedValue.INFO_SEVERITY)
                    .add();

            shuntsWithDeltaDiscreteOptimalOverThresholds.forEach(shunt ->
                    reportShunts.newReportNode()
                            .withMessageTemplate("optimizer.openreac.shuntCompensatorDeltaDiscretizedOptimizedOverThreshold")
                            .withUntypedValue("shuntCompensatorId", shunt.id())
                            .withUntypedValue("maxSectionCount", shunt.maximumSectionCount())
                            .withUntypedValue("discretizedValue", VALUE_FORMAT.format(shunt.discretizedReactiveValue()))
                            .withUntypedValue("optimalValue", VALUE_FORMAT.format(shunt.optimalReactiveValue()))
                            .withSeverity(TypedValue.DETAIL_SEVERITY)
                            .add());
        }
    }

    public static void reportConstantQGeneratorsSize(ReportNode reportNode, int constantQGeneratorsSize) {
        reportNode.newReportNode()
            .withMessageTemplate("optimizer.openreac.constantQGeneratorsSize")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .withUntypedValue(SIZE, constantQGeneratorsSize)
            .add();
    }

    public static void reportInconsistentLimitsOnVoltageLevel(ReportNode reportNode, String vlId, Pair<Double, Double> limits) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.voltageLevelWithInconsistentLimits")
                .withSeverity(TypedValue.DETAIL_SEVERITY)
                .withUntypedValue("vlId", vlId)
                .withUntypedValue("low", limits.getLeft())
                .withUntypedValue("high", limits.getRight())
                .add();
    }

    public static void reportMissingLimitsOnVoltageLevel(ReportNode reportNode, String messageKey, String vlId) {
        reportNode.newReportNode()
                .withMessageTemplate(messageKey)
                .withSeverity(TypedValue.DETAIL_SEVERITY)
                .withUntypedValue("vlId", vlId)
                .add();
    }

    public static void reportNbVoltageLevelsWithInconsistentLimits(ReportNode reportNode, int voltageLevelsWithInconsistentLimitsSize) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.nbVoltageLevelsWithInconsistentLimits")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .withUntypedValue(SIZE, voltageLevelsWithInconsistentLimitsSize)
                .add();
    }

    public static void reportNbVoltageLevelsWithMissingLimits(ReportNode reportNode, int voltageLevelsWithMissingLimitsSize) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.nbVoltageLevelsWithMissingLimits")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .withUntypedValue(SIZE, voltageLevelsWithMissingLimitsSize)
                .add();
    }

    public static void reportVariableShuntCompensatorsSize(ReportNode reportNode, int variableShuntCompensatorsSize) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.variableShuntCompensatorsSize")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .withUntypedValue(SIZE, variableShuntCompensatorsSize)
                .add();
    }

    public static void reportVariableTwoWindingsTransformersSize(ReportNode reportNode, int variableTwoWindingsTransformersSize) {
        reportNode.newReportNode()
            .withMessageTemplate("optimizer.openreac.variableTwoWindingsTransformersSize")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .withUntypedValue(SIZE, variableTwoWindingsTransformersSize)
            .add();
    }

    public static void reportVoltageLevelsWithLimitsOutOfNominalVRange(ReportNode reportNode, Map<String, VoltageLevelLimitInfo> voltageLevelsWithLimitsOutOfNominalVRange) {
        if (!voltageLevelsWithLimitsOutOfNominalVRange.isEmpty()) {
            ReportNode reportLimitsOutOfRange = reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.voltageLevelsLimitsOutOfNominalVRange")
                .add();

            // Do not change this report key "nbVoltageLevelsWithLimitsOutOfNominalVRange", as it is used elsewhere

            reportLimitsOutOfRange.newReportNode()
                .withMessageTemplate("optimizer.openreac.nbVoltageLevelsWithLimitsOutOfNominalVRange")
                .withSeverity(TypedValue.WARN_SEVERITY)
                .withUntypedValue(SIZE, voltageLevelsWithLimitsOutOfNominalVRange.size())
                .add();

            voltageLevelsWithLimitsOutOfNominalVRange.forEach((voltageLevelId, voltageLevelLimitInfo) -> reportLimitsOutOfRange.newReportNode()
                .withMessageTemplate("optimizer.openreac.voltageLevelWithLimitsOutOfNominalVRange")
                .withSeverity(TypedValue.DETAIL_SEVERITY)
                .withUntypedValue("vID", voltageLevelLimitInfo.voltageLevelId())
                .withUntypedValue("lowVoltageLimit", VALUE_FORMAT.format(voltageLevelLimitInfo.lowLimit()))
                .withUntypedValue("highVoltageLimit", VALUE_FORMAT.format(voltageLevelLimitInfo.highLimit()))
                .withUntypedValue("nominalVoltage", voltageLevelLimitInfo.nominalV())
                .add());
        }
    }

    public static void reportNbFrenchBranchesWithHighImpedanceRatio(ReportNode reportNode, int size) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.nbFrenchBranchesWithHighImpedanceRatio")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .withUntypedValue(SIZE, size)
                .add();
    }

    public static void reportFrenchBranchWithHighImpedanceRatio(ReportNode reportNode, String branchId,
                                                                double r, double x, double ratio) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.frenchBranchWithHighImpedanceRatio")
                .withSeverity(TypedValue.DETAIL_SEVERITY)
                .withUntypedValue(BRANCH_ID, branchId)
                .withUntypedValue(RESISTANCE, VALUE_FORMAT_ACCURATE.format(r))
                .withUntypedValue(REACTANCE, VALUE_FORMAT_ACCURATE.format(x))
                .withUntypedValue(RATIO, VALUE_FORMAT_ACCURATE.format(ratio))
                .add();
    }

    public static void reportNbFrenchBranchesWithAcceptableHighImpedanceRatio(ReportNode reportNode, int size) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.nbFrenchBranchesWithAcceptableHighImpedanceRatio")
                .withSeverity(TypedValue.WARN_SEVERITY)
                .withUntypedValue(SIZE, size)
                .add();
    }

    public static void reportFrenchBranchWithAcceptableHighImpedanceRatio(ReportNode reportNode, String branchId,
                                                                          double r, double x, double ratio) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.frenchBranchWithAcceptableHighImpedanceRatio")
                .withSeverity(TypedValue.DETAIL_SEVERITY)
                .withUntypedValue(BRANCH_ID, branchId)
                .withUntypedValue(RESISTANCE, VALUE_FORMAT_ACCURATE.format(r))
                .withUntypedValue(REACTANCE, VALUE_FORMAT_ACCURATE.format(x))
                .withUntypedValue(RATIO, VALUE_FORMAT_ACCURATE.format(ratio))
                .add();
    }

    public static void reportNbNonFrenchBranchesWithHighImpedanceRatio(ReportNode reportNode, int size) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.nbNonFrenchBranchesWithHighImpedanceRatio")
                .withSeverity(TypedValue.WARN_SEVERITY)
                .withUntypedValue(SIZE, size)
                .add();
    }

    public static void reportNonFrenchBranchWithHighImpedanceRatio(ReportNode reportNode, String branchId,
                                                                   double r, double x, double ratio) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.nonFrenchBranchWithHighImpedanceRatio")
                .withSeverity(TypedValue.DETAIL_SEVERITY)
                .withUntypedValue(BRANCH_ID, branchId)
                .withUntypedValue(RESISTANCE, VALUE_FORMAT_ACCURATE.format(r))
                .withUntypedValue(REACTANCE, VALUE_FORMAT_ACCURATE.format(x))
                .withUntypedValue(RATIO, VALUE_FORMAT_ACCURATE.format(ratio))
                .add();
    }

    public static void reportNbBranchesWithLowImpedance(ReportNode reportNode, int size) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.nbBranchesWithLowImpedance")
                .withSeverity(TypedValue.WARN_SEVERITY)
                .withUntypedValue(SIZE, size)
                .add();
    }

    public static void reportBranchWithLowImpedance(ReportNode reportNode, String branchId,
                                                    double r, double x, double threshold) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.branchWithLowImpedance")
                .withSeverity(TypedValue.DETAIL_SEVERITY)
                .withUntypedValue(BRANCH_ID, branchId)
                .withUntypedValue(RESISTANCE, VALUE_FORMAT_SCIENTIFIC.format(r))
                .withUntypedValue(REACTANCE, VALUE_FORMAT_SCIENTIFIC.format(x))
                .withUntypedValue("threshold", VALUE_FORMAT_SCIENTIFIC.format(threshold))
                .add();
    }

    public static void reportParallelTwoWindingsTransformers(ReportNode reportNode,
                                                             List<ParallelTwoWindingsTransformersDetector.ParallelGroup> groups,
                                                             Network network,
                                                             Set<String> variableTransformerIds) {
        if (groups.isEmpty()) {
            return;
        }
        int totalTransformers = groups.stream().mapToInt(g -> g.transformerIds().size()).sum();
        ReportNode root = reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.parallelTwoWindingsTransformers")
                .withUntypedValue("groupsCount", groups.size())
                .withUntypedValue("transformersCount", totalTransformers)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();

        int groupIndex = 0;
        for (var group : groups) {
            TypedValue groupSeverity = switch (group.status()) {
                case LARGE -> TypedValue.DETAIL_SEVERITY;
                case POINT, EMPTY -> TypedValue.WARN_SEVERITY;
            };

            String range = group.status() == ParallelTwoWindingsTransformersDetector.IntersectionStatus.EMPTY
                ? "(no overlap)"
                : "[" + VALUE_FORMAT_ACCURATE.format(group.low()) + ", " + VALUE_FORMAT_ACCURATE.format(group.high()) + "]";

            ReportNode groupNode = root.newReportNode()
                    .withMessageTemplate("optimizer.openreac.parallelTwoWindingsTransformersGroup")
                    .withUntypedValue("groupIndex", groupIndex++)
                    .withUntypedValue("count", group.transformerIds().size())
                    .withUntypedValue("intersectionStatus", group.status().name())
                    .withUntypedValue("intersectionRange", range)
                    .withSeverity(groupSeverity)
                    .add();

            group.transformerIds().stream().sorted().forEach(twtId -> {
                TwoWindingsTransformer twt = network.getTwoWindingsTransformer(twtId);
                RatioTapChanger rtc = twt != null ? twt.getRatioTapChanger() : null;
                double rhoMin = rtc == null ? Double.NaN
                    : rtc.getAllSteps().values().stream().mapToDouble(RatioTapChangerStep::getRho).min().orElse(Double.NaN);
                double rhoMax = rtc == null ? Double.NaN
                    : rtc.getAllSteps().values().stream().mapToDouble(RatioTapChangerStep::getRho).max().orElse(Double.NaN);
                String status = variableTransformerIds.contains(twtId) ? "VARIABLE" : "FIXED";
                groupNode.newReportNode()
                        .withMessageTemplate("optimizer.openreac.parallelTwoWindingsTransformerItem")
                        .withUntypedValue("transformerId", twtId)
                        .withUntypedValue("ratioStatus", status)
                        .withUntypedValue("rhoMin", Double.isNaN(rhoMin) ? "N/A" : VALUE_FORMAT_ACCURATE.format(rhoMin))
                        .withUntypedValue("rhoMax", Double.isNaN(rhoMax) ? "N/A" : VALUE_FORMAT_ACCURATE.format(rhoMax))
                        .withSeverity(TypedValue.DETAIL_SEVERITY)
                        .add();
            });
        }
    }
}

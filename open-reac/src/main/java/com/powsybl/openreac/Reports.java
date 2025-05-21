/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openreac.parameters.input.VoltageLevelLimitInfo;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.output.network.ShuntCompensatorNetworkOutput;
import org.apache.commons.lang3.tuple.Pair;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini_externe at rte-france.com>}
 */
public final class Reports {

    private static final String NETWORK_ID = "networkId";
    private static final DecimalFormat VALUE_FORMAT = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ROOT));

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
            .withUntypedValue("size", constantQGeneratorsSize)
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
                .withUntypedValue("size", voltageLevelsWithInconsistentLimitsSize)
                .add();
    }

    public static void reportNbVoltageLevelsWithMissingLimits(ReportNode reportNode, int voltageLevelsWithMissingLimitsSize) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.nbVoltageLevelsWithMissingLimits")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .withUntypedValue("size", voltageLevelsWithMissingLimitsSize)
                .add();
    }

    public static void reportVariableShuntCompensatorsSize(ReportNode reportNode, int variableShuntCompensatorsSize) {
        reportNode.newReportNode()
                .withMessageTemplate("optimizer.openreac.variableShuntCompensatorsSize")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .withUntypedValue("size", variableShuntCompensatorsSize)
                .add();
    }

    public static void reportVariableTwoWindingsTransformersSize(ReportNode reportNode, int variableTwoWindingsTransformersSize) {
        reportNode.newReportNode()
            .withMessageTemplate("optimizer.openreac.variableTwoWindingsTransformersSize")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .withUntypedValue("size", variableTwoWindingsTransformersSize)
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
                .withUntypedValue("size", voltageLevelsWithLimitsOutOfNominalVRange.size())
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
}

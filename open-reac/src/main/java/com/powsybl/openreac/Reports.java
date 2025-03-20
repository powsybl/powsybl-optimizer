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
            .withMessageTemplate("openReac", "Open Reac on network '${networkId}' with ${objective} objective")
            .withUntypedValue(NETWORK_ID, networkId)
            .withUntypedValue("objective", objective.toString())
            .add();
    }

    public static ReportNode createParameterIntegrityReporter(ReportNode reportNode, String networkId) {
        return reportNode.newReportNode()
                .withMessageTemplate("openReacParameterIntegrity", "Open reac parameter integrity on network '${networkId}'")
                .withUntypedValue(NETWORK_ID, networkId)
                .add();
    }

    public static void createShuntModificationsReporter(ReportNode reportNode, String networkId, List<ShuntCompensatorNetworkOutput.ShuntWithDeltaDiscreteOptimalOverThreshold> shuntsWithDeltaDiscreteOptimalOverThresholds) {
        if (!shuntsWithDeltaDiscreteOptimalOverThresholds.isEmpty()) {
            ReportNode reportShunts = reportNode.newReportNode()
                    .withMessageTemplate("shuntCompensatorDeltaOverThreshold", "Shunt compensator reactive delta over threshold")
                    .withUntypedValue(NETWORK_ID, networkId)
                    .add();
            reportShunts.newReportNode()
                    .withMessageTemplate("shuntCompensatorDeltaOverThresholdCount", "For ${shuntsCount} shunt compensators, there is a significant difference between the updated discretized reactive power value and the theoretical optimal reactive power value.")
                    .withUntypedValue("shuntsCount", shuntsWithDeltaDiscreteOptimalOverThresholds.size())
                    .withSeverity(TypedValue.INFO_SEVERITY)
                    .add();

            shuntsWithDeltaDiscreteOptimalOverThresholds.forEach(shunt ->
                    reportShunts.newReportNode()
                            .withMessageTemplate("shuntCompensatorDeltaDiscretizedOptimizedOverThreshold", "After discretization, shunt compensator ${shuntCompensatorId} with ${maxSectionCount} available section(s) has been set to ${discretizedValue} MVar (optimal value : ${optimalValue} MVar)")
                            .withUntypedValue("shuntCompensatorId", shunt.id())
                            .withUntypedValue("maxSectionCount", shunt.maximumSectionCount())
                            .withUntypedValue("discretizedValue", VALUE_FORMAT.format(shunt.discretizedReactiveValue()))
                            .withUntypedValue("optimalValue", VALUE_FORMAT.format(shunt.optimalReactiveValue()))
                            .withSeverity(TypedValue.TRACE_SEVERITY)
                            .add());
        }
    }

    public static void reportConstantQGeneratorsSize(ReportNode reportNode, int constantQGeneratorsSize) {
        reportNode.newReportNode()
            .withMessageTemplate("constantQGeneratorsSize", "Reactive power target is considered fixed for ${size} generators")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .withUntypedValue("size", constantQGeneratorsSize)
            .add();
    }

    public static void reportInconsistentLimitsOnVoltageLevel(ReportNode reportNode, String vlId, Pair<Double, Double> limits) {
        reportNode.newReportNode()
                .withMessageTemplate("voltageLevelWithInconsistentLimits", "${vlId} has one or two inconsistent voltage limits (low voltage limit = ${low}, high voltage limit = ${high})")
                .withSeverity(TypedValue.TRACE_SEVERITY)
                .withUntypedValue("vlId", vlId)
                .withUntypedValue("low", limits.getLeft())
                .withUntypedValue("high", limits.getRight())
                .add();
    }

    public static void reportMissingLimitsOnVoltageLevel(ReportNode reportNode, String messageKey, String vlId, String messageSuffix) {
        reportNode.newReportNode()
                .withMessageTemplate(messageKey, "${vlId} " + messageSuffix)
                .withSeverity(TypedValue.TRACE_SEVERITY)
                .withUntypedValue("vlId", vlId)
                .add();
    }

    public static void reportNbVoltageLevelsWithInconsistentLimits(ReportNode reportNode, int voltageLevelsWithInconsistentLimitsSize) {
        reportNode.newReportNode()
                .withMessageTemplate("nbVoltageLevelsWithInconsistentLimits", "${size} voltage level(s) have inconsistent low and/or high voltage limits")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .withUntypedValue("size", voltageLevelsWithInconsistentLimitsSize)
                .add();
    }

    public static void reportNbVoltageLevelsWithMissingLimits(ReportNode reportNode, int voltageLevelsWithMissingLimitsSize) {
        reportNode.newReportNode()
                .withMessageTemplate("nbVoltageLevelsWithMissingLimits", "${size} voltage level(s) have undefined low and/or high voltage limits")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .withUntypedValue("size", voltageLevelsWithMissingLimitsSize)
                .add();
    }

    public static void reportVariableShuntCompensatorsSize(ReportNode reportNode, int variableShuntCompensatorsSize) {
        reportNode.newReportNode()
                .withMessageTemplate("variableShuntCompensatorsSize", "There are ${size} shunt compensators with section considered as variable")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .withUntypedValue("size", variableShuntCompensatorsSize)
                .add();
    }

    public static void reportVariableTwoWindingsTransformersSize(ReportNode reportNode, int variableTwoWindingsTransformersSize) {
        reportNode.newReportNode()
            .withMessageTemplate("variableTwoWindingsTransformersSize", "There are ${size} two-winding transformers with tap position considered as variable")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .withUntypedValue("size", variableTwoWindingsTransformersSize)
            .add();
    }

    public static void reportVoltageLevelsWithLimitsOutOfNominalVRange(ReportNode reportNode, Map<String, VoltageLevelLimitInfo> voltageLevelsWithLimitsOutOfNominalVRange) {
        if (!voltageLevelsWithLimitsOutOfNominalVRange.isEmpty()) {
            ReportNode reportLimitsOutOfRange = reportNode.newReportNode()
                .withMessageTemplate("voltageLevelsLimitsOutOfNominalVRange", "Voltage levels limits out of nominal voltage range")
                .add();

            // Do not change this report key "nbVoltageLevelsWithLimitsOutOfNominalVRange", as it is used elsewhere

            reportLimitsOutOfRange.newReportNode()
                .withMessageTemplate("nbVoltageLevelsWithLimitsOutOfNominalVRange", "Acceptable voltage range for ${size} voltage levels seems to be inconsistent with nominal voltage")
                .withSeverity(TypedValue.WARN_SEVERITY)
                .withUntypedValue("size", voltageLevelsWithLimitsOutOfNominalVRange.size())
                .add();

            voltageLevelsWithLimitsOutOfNominalVRange.forEach((voltageLevelId, voltageLevelLimitInfo) -> reportLimitsOutOfRange.newReportNode()
                .withMessageTemplate("voltageLevelWithLimitsOutOfNominalVRange", "Acceptable voltage range for voltage level ${vID} seems to be inconsistent with nominal voltage : low voltage limit = ${lowVoltageLimit} kV, high voltage limit = ${highVoltageLimit} kV, nominal voltage = ${nominalVoltage} kV")
                .withSeverity(TypedValue.TRACE_SEVERITY)
                .withUntypedValue("vID", voltageLevelLimitInfo.voltageLevelId())
                .withUntypedValue("lowVoltageLimit", VALUE_FORMAT.format(voltageLevelLimitInfo.lowLimit()))
                .withUntypedValue("highVoltageLimit", VALUE_FORMAT.format(voltageLevelLimitInfo.highLimit()))
                .withUntypedValue("nominalVoltage", voltageLevelLimitInfo.nominalV())
                .add());
        }
    }
}

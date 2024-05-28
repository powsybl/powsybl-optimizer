/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.output.network.ShuntCompensatorNetworkOutput;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * @author Joris Mancini {@literal <joris.mancini_externe at rte-france.com>}
 */
public final class Reports {

    private static final String NETWORK_ID = "networkId";
    private static final DecimalFormat REACTIVE_VALUE_FORMAT = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ROOT));

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

    public static void reportConstantQGeneratorsSize(ReportNode reportNode, int constantQGeneratorsSize) {
        reportNode.newReportNode()
                .withMessageTemplate("constantQGeneratorsSize", "Reactive power target is considered fixed for ${size} generators")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .withUntypedValue("size", constantQGeneratorsSize)
                .add();
    }

    public static void reportVariableTwoWindingsTransformersSize(ReportNode reportNode, int variableTwoWindingsTransformersSize) {
        reportNode.newReportNode()
                .withMessageTemplate("variableTwoWindingsTransformersSize", "There are ${size} two-winding transformers with tap position considered as variable")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .withUntypedValue("size", variableTwoWindingsTransformersSize)
                .add();
    }

    public static void reportVariableShuntCompensatorsSize(ReportNode reportNode, int variableShuntCompensatorsSize) {
        reportNode.newReportNode()
                .withMessageTemplate("variableShuntCompensatorsSize", "There are ${size} shunt compensators with section considered as variable")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .withUntypedValue("size", variableShuntCompensatorsSize)
                .add();
    }

    public static ReportNode createParameterIntegrityReporter(ReportNode reportNode, String networkId) {
        return reportNode.newReportNode()
            .withMessageTemplate("openReacParameterIntegrity", "Open reac parameter integrity on network '${networkId}'")
            .withUntypedValue(NETWORK_ID, networkId)
            .add();
    }

    public static void createShuntModificationsReporter(ReportNode reportNode, String networkId, List<ShuntCompensatorNetworkOutput.ShuntWithDeltaDiscreteOptimalOverThrehold> shuntsWithDeltaDiscreteOptimalOverThreholds) {
        if (!shuntsWithDeltaDiscreteOptimalOverThreholds.isEmpty()) {
            ReportNode reportShunts = reportNode.newReportNode()
                .withMessageTemplate("shuntCompensatorDeltaOverThreshold", "Shunt compensator reactive delta over threshold")
                .withUntypedValue(NETWORK_ID, networkId)
                .add();
            reportShunts.newReportNode()
                .withMessageTemplate("shuntCompensatorDeltaOverThresholdCount", "For ${shuntsCount} shunt compensators, there is a significant difference between the updated discretized reactive power value and the theoretical optimal reactive power value.")
                .withUntypedValue("shuntsCount", shuntsWithDeltaDiscreteOptimalOverThreholds.size())
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();

            shuntsWithDeltaDiscreteOptimalOverThreholds.forEach(shunt ->
                reportShunts.newReportNode()
                    .withMessageTemplate("shuntCompensatorDeltaDiscretizedOptimizedOverThreshold", "After discretization, shunt compensator ${shuntCompensatorId} with ${maxSectionCount} available section(s) has been set to ${discretizedValue} MVar (optimal value : ${optimalValue} MVar)")
                    .withUntypedValue("shuntCompensatorId", shunt.id())
                    .withUntypedValue("maxSectionCount", shunt.maximumSectionCount())
                    .withUntypedValue("discretizedValue", REACTIVE_VALUE_FORMAT.format(shunt.discretizedReactiveValue()))
                    .withUntypedValue("optimalValue", REACTIVE_VALUE_FORMAT.format(shunt.optimalReactiveValue()))
                    .withSeverity(TypedValue.TRACE_SEVERITY)
                    .add());
        }
    }
}

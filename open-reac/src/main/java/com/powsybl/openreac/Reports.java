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

/**
 * @author Joris Mancini <joris.mancini_externe at rte-france.com>
 */
public final class Reports {

    private Reports() {
        // Should not be instantiated
    }

    public static ReportNode createOpenReacReporter(ReportNode reportNode, String networkId, OpenReacOptimisationObjective objective) {
        return reportNode.newReportNode()
                .withMessageTemplate("openReac", "Open Reac on network '${networkId}' with ${objective} objective")
                .withUntypedValue("networkId", networkId)
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
}

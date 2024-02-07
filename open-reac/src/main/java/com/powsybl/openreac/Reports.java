/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.commons.reporter.Report;
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.commons.reporter.TypedValue;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;

import java.util.Map;

/**
 * @author Joris Mancini <joris.mancini_externe at rte-france.com>
 */
public final class Reports {

    private Reports() {
        // Should not be instantiated
    }

    public static Reporter createOpenReacReporter(Reporter reporter, String networkId, OpenReacOptimisationObjective objective) {
        return reporter.createSubReporter(
            "openReac",
            "Open Reac on network '${networkId}' with ${objective} objective",
            Map.of(
                "networkId", new TypedValue(networkId, TypedValue.UNTYPED),
                "objective", new TypedValue(objective.toString(), TypedValue.UNTYPED)
            )
        );
    }

    public static void reportConstantQGeneratorsSize(Reporter reporter, int constantQGeneratorsSize) {
        reporter.report(Report.builder()
            .withKey("constantQGeneratorsSize")
            .withDefaultMessage("Reactive power target is considered fixed for ${size} generators")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .withValue("size", constantQGeneratorsSize)
            .build());
    }

    public static void reportVariableTwoWindingsTransformersSize(Reporter reporter, int variableTwoWindingsTransformersSize) {
        reporter.report(Report.builder()
            .withKey("variableTwoWindingsTransformersSize")
            .withDefaultMessage("There are ${size} tap positions considered as variable on two-winding transformers")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .withValue("size", variableTwoWindingsTransformersSize)
            .build());
    }

    public static void reportVariableShuntCompensatorsSize(Reporter reporter, int variableShuntCompensatorsSize) {
        reporter.report(Report.builder()
            .withKey("variableShuntCompensatorsSize")
            .withDefaultMessage("There are ${size} shunt sections considered as variable on shunt compensators")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .withValue("size", variableShuntCompensatorsSize)
            .build());
    }
}

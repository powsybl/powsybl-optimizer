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

/**
 * @author Joris Mancini <joris.mancini_externe at rte-france.com>
 */
public final class Reports {

    private Reports() {
        // Should not be instantiated
    }

    public static Reporter createVoltageInitReporter(Reporter reporter, String networkId) {
        return reporter.createSubReporter("voltageInit", "Voltage init on network '${networkId}'", "networkId", networkId);
    }

    public static void reportConstantQGeneratorsSize(Reporter reporter, int constantQGeneratorsSize) {
        reporter.report(Report.builder()
            .withKey("constantQGeneratorsSize")
            .withDefaultMessage("Reactive power set point is considered fixed for ${size} generators")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .withValue("size", constantQGeneratorsSize)
            .build());
    }

    public static void reportVariableTwoWindingsTransformersSize(Reporter reporter, int variableTwoWindingsTransformersSize) {
        reporter.report(Report.builder()
            .withKey("variableTwoWindingsTransformersSize")
            .withDefaultMessage("${size} two-windings transformers are considered as variable")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .withValue("size", variableTwoWindingsTransformersSize)
            .build());
    }

    public static void reportVariableShuntCompensatorsSize(Reporter reporter, int variableShuntCompensatorsSize) {
        reporter.report(Report.builder()
            .withKey("variableShuntCompensatorsSize")
            .withDefaultMessage("${size} shunt compensators are considered as variable")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .withValue("size", variableShuntCompensatorsSize)
            .build());
    }
}

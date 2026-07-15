/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.commons.report.ReportNode;

/**
 * Helper utilities for testing ReportNode
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public final class ReportTestHelper {

    private ReportTestHelper() {
        // Utility class
    }

    /**
     * Check if a ReportNode tree contains a report with a specific message key
     *
     * @param reportNode the root report node to search in
     * @param messageKey the message key to search for
     * @return true if the message key is found in the tree, false otherwise
     */
    public static boolean hasReportWithKey(ReportNode reportNode, String messageKey) {
        if (reportNode.getMessageKey() != null && reportNode.getMessageKey().equals(messageKey)) {
            return true;
        }
        for (ReportNode child : reportNode.getChildren()) {
            if (hasReportWithKey(child, messageKey)) {
                return true;
            }
        }
        return false;
    }
}

/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openreac.parameters.input;
/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public final class AmplWriterUtils {
    private static final String QUOTE = "'";

    public static String addQuotes(String str) {
        return QUOTE + str + QUOTE;
    }

    private AmplWriterUtils() { }
}

/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters;

import com.powsybl.commons.PowsyblException;

import java.util.Objects;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
 */
public final class AmplIOUtils {

    private static final String QUOTE = "\"";

    public static String addQuotes(String str) {
        return QUOTE + str + QUOTE;
    }

    /**
     * removes quotes on strings
     */
    public static String removeQuotes(String str) {
        Objects.requireNonNull(str);
        if (str.length() < 2) {
            throw new PowsyblException("Too small string while trying to remove quotes on : " + str);
        }
        return str.substring(1, str.length() - 1);
    }

    private AmplIOUtils() {
    }
}

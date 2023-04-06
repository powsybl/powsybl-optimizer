/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.exceptions;

import com.powsybl.commons.PowsyblException;

/**
 * Throw this error when the interface between ampl and java is not correct.
 * This is an internal OpenReac error. It is not the user fault.
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class IncompatibleModelException extends PowsyblException {
    public IncompatibleModelException(String message) {
        super("Error of compatibility between the ampl model and the interface, this is a OpenReac issue.\n" + message);
    }
}

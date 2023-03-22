/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters;

import com.powsybl.commons.PowsyblException;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class InvalidParametersException extends PowsyblException {
    public InvalidParametersException(String message){
        super(message);
    }
}

/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public abstract class AbstractNoThrowOutput implements NoThrowAmplOutput {
    private boolean errorState = false;

    @Override
    public boolean isErrorState() {
        return errorState;
    }

    protected void triggerErrorState() {
        errorState = true;
    }
}

/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.openreac.parameters.output.ReactiveInvestmentOutput.ReactiveInvestment;

import java.util.List;
import java.util.Map;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class OpenReacResult {

    public enum OpenReacStatus {
        OK,
        NOT_OK
    }

    private final OpenReacStatus status;
    private final List<ReactiveInvestment> reactiveInvestments;
    private final Map<String, String> indicators;

    public OpenReacResult(OpenReacStatus status, List<ReactiveInvestment> reactiveInvestments,
                          Map<String, String> indicators) {
        this.status = status;
        this.reactiveInvestments = reactiveInvestments;
        this.indicators = indicators;
    }

    public OpenReacStatus getStatus() {
        return status;
    }

    public List<ReactiveInvestment> getReactiveInvestments() {
        return reactiveInvestments;
    }

    public Map<String, String> getIndicators() {
        return indicators;
    }
}

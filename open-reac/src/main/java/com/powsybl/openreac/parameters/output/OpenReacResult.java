/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.openreac.parameters.output.ReactiveSlackOutput.ReactiveSlack;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class OpenReacResult {

    private final OpenReacStatus status;
    private final List<ReactiveSlack> reactiveSlacks;
    private final Map<String, String> indicators;

    public OpenReacResult(OpenReacStatus status, List<ReactiveSlack> reactiveSlacks, Map<String, String> indicators) {
        this.status = Objects.requireNonNull(status);
        this.reactiveSlacks = Objects.requireNonNull(reactiveSlacks);
        this.indicators = Objects.requireNonNull(indicators);
    }

    public OpenReacStatus getStatus() {
        return status;
    }

    public List<ReactiveSlack> getReactiveSlacks() {
        return reactiveSlacks;
    }

    public Map<String, String> getIndicators() {
        return indicators;
    }
}

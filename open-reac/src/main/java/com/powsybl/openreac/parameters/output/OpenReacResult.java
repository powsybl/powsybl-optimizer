/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.iidm.modification.*;
import com.powsybl.iidm.modification.tapchanger.RatioTapPositionModification;
import com.powsybl.openreac.parameters.OpenReacAmplIOFiles;
import com.powsybl.openreac.parameters.output.ReactiveSlackOutput.ReactiveSlack;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OpenReac user interface to get results information.
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class OpenReacResult {

    private final OpenReacStatus status;
    private final List<ReactiveSlack> reactiveSlacks;
    private final Map<String, String> indicators;
    private final List<GeneratorModification> generatorModifications;
    private final List<ShuntCompensatorPositionModification> shuntsModifications;
    private final List<VscConverterStationModification> vscModifications;
    private final List<StaticVarCompensatorModification> svcModifications;
    private final List<RatioTapPositionModification> tapModifications;

    /**
     * @param status the final status of the OpenReac run.
     * @param amplIOFiles a file interface to fetch output file information.
     * @param indicators a standard map written by the OpenReac ampl model.
     */
    public OpenReacResult(OpenReacStatus status, OpenReacAmplIOFiles amplIOFiles, Map<String, String> indicators) {
        Objects.requireNonNull(amplIOFiles);
        this.status = Objects.requireNonNull(status);
        this.indicators = Map.copyOf(Objects.requireNonNull(indicators));
        this.reactiveSlacks = List.copyOf(amplIOFiles.getReactiveSlackOutput().getSlacks());
        this.generatorModifications = List.copyOf(amplIOFiles.getNetworkModifOuputs().getGeneratorModifications());
        this.shuntsModifications = List.copyOf(amplIOFiles.getNetworkModifOuputs().getShuntModifications());
        this.vscModifications = List.copyOf(amplIOFiles.getNetworkModifOuputs().getVscModifications());
        this.svcModifications = List.copyOf(amplIOFiles.getNetworkModifOuputs().getSvcModifications());
        this.tapModifications = List.copyOf(amplIOFiles.getNetworkModifOuputs().getTapModifications());
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

    public List<GeneratorModification> getGeneratorModifications() {
        return generatorModifications;
    }

    public List<ShuntCompensatorPositionModification> getShuntsModifications() {
        return shuntsModifications;
    }

    public List<StaticVarCompensatorModification> getSvcModifications() {
        return svcModifications;
    }

    public List<RatioTapPositionModification> getTapModifications() {
        return tapModifications;
    }

    public List<VscConverterStationModification> getVscModifications() {
        return vscModifications;
    }
}

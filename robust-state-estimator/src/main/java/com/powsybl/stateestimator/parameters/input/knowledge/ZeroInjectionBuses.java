/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.input.knowledge;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class ZeroInjectionBuses implements AmplInputFile {

    Map<Integer, String> zeroInjectionBuses;

    public ZeroInjectionBuses(Map<Integer, String> zeroInjectionBuses) {
        this.zeroInjectionBuses = zeroInjectionBuses;
    }

    @Override
    public String getFileName() {
        return "ampl_zero_injection_buses.txt";
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        // Expected format : "num" "zero_injection_bus_id"
        writer.write("#\"num\" \"zero_injection_bus_id\" ");
        writer.newLine();
        // For each zero-injection bus
        for (var zeroInjectionBus : zeroInjectionBuses.entrySet()) {
            String line = zeroInjectionBus.getKey().toString() + " " + zeroInjectionBus.getValue();
            writer.write(line);
            writer.newLine();
        }
        //add new line at the end of the file !
        writer.newLine();
        writer.flush();
    }
}

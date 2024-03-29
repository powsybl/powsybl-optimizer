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
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class ReactivePowerInjectedMeasures implements AmplInputFile {

    Map<Integer, ArrayList<String>> measures;

    public ReactivePowerInjectedMeasures(Map<Integer, ArrayList<String>> measures) {
        this.measures = measures;
    }

    @Override
    public String getFileName() {
        return "ampl_measures_Q.txt";
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        // Expected format : "num" "type" "bus_id" "value (MVar)" "variance (MVar^2)"
        writer.write("#\"num\" \"type\" \"bus_id\" \"value (MVar)\" \"variance (MVar^2)\"");
        writer.newLine();
        // For each measurement
        for (var measure : measures.entrySet()) {
            StringBuilder line = new StringBuilder(measure.getKey().toString());
            for (String var : measure.getValue()) {
                line.append(" ").append(var);
            }
            writer.write(line.toString());
            writer.newLine();
        }
        //add new line at the end of the file !
        writer.newLine();
        writer.flush();
    }

    public void print() {
        System.out.println("Printing reactive power injected measurements : ");
        // Print the table header
        System.out.format("%n%-15s%-15s%-15s%-20s%n", "Type", "BusID", "Value (MVar)", "Variance (MVar^2)");
        System.out.format("%-15s%-15s%-15s%-20s%n",   "----", "-----", "------------", "-----------------");
        // Print each measurement
        for (var measure : measures.entrySet()) {
            System.out.format("%-15s%-15s%-15s%-15s%n",
                    measure.getValue().stream().map(
                            String -> {
                                if (String.length() <= 12) {
                                    return String;
                                } else {
                                    return String.substring(0, 6) + "..." + String.substring(String.length() - 3);
                                }
                            }).toArray());
        }
        System.out.println();
    }
}

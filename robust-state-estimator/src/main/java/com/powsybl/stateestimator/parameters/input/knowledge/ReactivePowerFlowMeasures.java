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
import org.jgrapht.alg.util.Pair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class ReactivePowerFlowMeasures implements AmplInputFile {

    Map<Integer, ArrayList<String>> measures;

    Map<Integer, ArrayList<String>> measuresWithResiduals;

    public ReactivePowerFlowMeasures(Map<Integer, ArrayList<String>> measures) {
        this.measures = measures;
    }

    public ReactivePowerFlowMeasures(Map<Integer, ArrayList<String>> measures, Map<Integer, String> allResiduals) {
        this.measuresWithResiduals = new HashMap<>();
        for (Integer measurementNumber : measures.keySet()) {
            for (Integer residualNumber : allResiduals.keySet()) {
                if (residualNumber.equals(measurementNumber)) {
                    ArrayList<String> measureWithResidual = new ArrayList<>(measures.get(measurementNumber));
                    measureWithResidual.add(allResiduals.get(residualNumber));
                    this.measuresWithResiduals.put(measurementNumber, measureWithResidual);
                }
            }
        }
    }

    @Override
    public String getFileName() {
        return "ampl_measures_Qf.txt";
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        // Expected format : "num" "type" "branch_id" "first_bus_id" "second_bus_id" "value (MVar)" "variance (MVar^2)"
        writer.write("#\"num\" \"type\" \"branch_id\" \"first_bus_id\" \"second_bus_id\" \"value (MVar)\" \"variance (MVar^2)\"");
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
        System.out.println("Printing reactive power flow measurements : ");
        // Print the table header
        System.out.format("%n%-15s%-15s%-15s%-15s%-15s%-20s%n", "Type", "BranchID", "FirstBusID", "SecondBusID", "Value (MVar)", "Variance (MVar^2)");
        System.out.format("%-15s%-15s%-15s%-15s%-15s%-20s%n", "----", "--------", "----------", "-----------", "------------", "-----------------");
        // Print each measurement
        for (var measure : measures.entrySet()) {
            System.out.format("%-15s%-15s%-15s%-15s%-15s%-20s%n",
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

    public void printWithResiduals() {
        System.out.println("Printing reactive power flow measurements : ");
        // Print the table header
        System.out.format("%n%-15s%-15s%-15s%-15s%-15s%-20s%-15s%n", "Type", "BranchID", "FirstBusID", "SecondBusID", "Value (MVar)", "Variance (MVar^2)", "Residual (MVar)");
        System.out.format("%-15s%-15s%-15s%-15s%-15s%-20s%-15s%n",        "----", "--------", "----------", "-----------", "------------", "-----------------", "--------------");
        // Print each measurement
        for (var measureWithResidual : measuresWithResiduals.entrySet()) {
            System.out.format("%-15s%-15s%-15s%-15s%-15s%-20s%-15s%n",
                    measureWithResidual.getValue().stream().map(
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

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
import java.util.*;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class ActivePowerFlowMeasures implements AmplInputFile {

    Map<Integer, ArrayList<String>> measures;

    Map<Integer, ArrayList<String>> measuresWithEstimatesAndResiduals;

    public ActivePowerFlowMeasures(Map<Integer, ArrayList<String>> measures) {
        this.measures = measures;
    }

    public ActivePowerFlowMeasures(Map<Integer, ArrayList<String>> measures, Map<Integer, ArrayList<String>> estimatesAndResiduals) {
        // Measure : <Number, <Type, BranchID, FirstBusID, SecondBusID, Value, Variance, Estimate, Residual>>
        this.measuresWithEstimatesAndResiduals = new HashMap<>();
        for (Integer measurementNumber : measures.keySet()) {
            for (Integer residualNumber : estimatesAndResiduals.keySet()) {
                if (residualNumber.equals(measurementNumber)) {
                    ArrayList<String> measureWithEstimateAndResidual = new ArrayList<>(measures.get(measurementNumber));
                    measureWithEstimateAndResidual.add(estimatesAndResiduals.get(residualNumber).get(0));
                    measureWithEstimateAndResidual.add(estimatesAndResiduals.get(residualNumber).get(1));
                    this.measuresWithEstimatesAndResiduals.put(measurementNumber, measureWithEstimateAndResidual);
                }
            }
        }
    }

    @Override
    public String getFileName() {
        return "ampl_measures_Pf.txt";
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        // Expected format : "num" "type" "branch_id" "first_bus_id" "second_bus_id" "value (MW)" "variance (MW^2)"
        writer.write("#\"num\" \"type\" \"branch_id\" \"first_bus_id\" \"second_bus_id\" \"value (MW)\" \"variance (MW^2)\"");
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
        System.out.println("Printing active power flow measurements : ");
        // Print the table header
        System.out.format("%n%-15s%-15s%-15s%-15s%-15s%-15s%n", "Type", "BranchID", "FirstBusID", "SecondBusID", "Value (MW)", "Variance (MW^2)");
        System.out.format("%-15s%-15s%-15s%-15s%-15s%-15s%n", "----", "--------", "----------", "-----------", "----------", "---------------");
        // Print each measurement
        for (var measure : measures.entrySet()) {
            System.out.format("%-15s%-15s%-15s%-15s%-15s%-15s%n",
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

    public void printWithEstimatesAndResiduals() {
        System.out.println("Printing active power flow measurements : ");
        // Print the table header
        System.out.format("%n%-15s%-15s%-15s%-15s%-15s%-20s%-15s%-15s%n", "Type", "BranchID", "FirstBusID", "SecondBusID", "Value (MW)", "Variance (MW^2)",     "Estimate (MW)", "Residual (MW)");
        System.out.format("%-15s%-15s%-15s%-15s%-15s%-20s%-15s%-15s%n",        "----", "--------", "----------", "-----------", "----------", "---------------","-------------", "-------------");
        // Print each measurement
        for (var measureWithEstimateAndResidual : measuresWithEstimatesAndResiduals.entrySet()) {
            System.out.format("%-15s%-15s%-15s%-15s%-15s%-20s%-15s%-15s%n",
                    measureWithEstimateAndResidual.getValue().stream().map(
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

    public Map<Integer, ArrayList<String>> getMeasuresWithEstimatesAndResiduals() {
        return measuresWithEstimatesAndResiduals;
    }
}
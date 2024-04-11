/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator;

import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.stateestimator.StateEstimator;
import com.powsybl.stateestimator.StateEstimatorConfig;
import com.powsybl.stateestimator.StateEstimatorResults;
import com.powsybl.stateestimator.parameters.input.knowledge.StateEstimatorKnowledge;
import com.powsybl.stateestimator.parameters.input.knowledge.RandomMeasuresGenerator;
import com.powsybl.stateestimator.parameters.input.options.StateEstimatorOptions;
import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.Test;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static com.powsybl.openloadflow.OpenLoadFlowParameters.LowImpedanceBranchMode.REPLACE_BY_MIN_IMPEDANCE_LINE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class Ieee118BusesTests {

    @Test
    void ieee118BusesTests() throws IOException {

        // Initialize the dataframe that will store the results
        List<String> headers = List.of("RatioMeasuresToBuses", "Seed",
                "MeanVError(pu)", "StdVError(pu)", "MedianVError(pu)", "MaxVError(pu)",
                "5percentileVError(pu)", "95percentileVError(pu)",
                "MeanThetaError(deg)", "StdThetaError(deg)", "MedianThetaError(deg)", "MaxThetaError(deg)",
                "5percentileThetaError(deg)", "95percentileThetaError(deg)",
                "NbVMeasures");
        List<List<String>> data = new ArrayList<>();

        Network network = IeeeCdfNetworkFactory.create118();

        // Load Flow parameters (note : we mimic the way the AMPL code deals with zero-impedance branches)
        LoadFlowParameters parametersLf = new LoadFlowParameters();
        OpenLoadFlowParameters parametersExt = OpenLoadFlowParameters.create(parametersLf);
        parametersExt.setAlwaysUpdateNetwork(true)
                .setLowImpedanceBranchMode(REPLACE_BY_MIN_IMPEDANCE_LINE)
                .setLowImpedanceThreshold(1e-4);

        // Solve the Load Flow problem for the network
        LoadFlowResult loadFlowResult = LoadFlow.run(network, parametersLf);
        assertTrue(loadFlowResult.isFullyConverged());

        // All MeasuresToBuses ratios tested
        List<Double> ratiosTested = Arrays.asList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0);

        for (Double ratioTested : ratiosTested) {
            for (int seed = 0; seed < 100; seed++) {
                // Create "knowledge" instance
                StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network);
                // For IEEE 118 bus, slack is "VL69_0": our state estimator must use the same slack
                knowledge.setSlack("VL69_0", network);
                // Randomly generate measurements out of load flow results using proper seed and Z to N ratio
                RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network,
                        Optional.of(seed), Optional.of(ratioTested),
                        Optional.of(false), Optional.of(false));
                // Define the solving options for the state estimation
                StateEstimatorOptions options = new StateEstimatorOptions().setSolvingMode(2).setMaxTimeSolving(30);
                // Run the state estimation and print the results
                StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                        knowledge, new StateEstimatorOptions(), new StateEstimatorConfig(true), new LocalComputationManager());
                // Save statistics on the accuracy of the state estimation w.r.t load flow solution
                List<Double> voltageErrorStats = results.computeVoltageErrorStatsPu(network);
                List<Double> angleErrorStats = results.computeAngleErrorStatsDegree(network);
                data.add(List.of(String.valueOf(ratioTested), String.valueOf(seed),
                        String.valueOf(voltageErrorStats.get(0)), String.valueOf(voltageErrorStats.get(1)),
                        String.valueOf(voltageErrorStats.get(2)), String.valueOf(voltageErrorStats.get(3)),
                        String.valueOf(voltageErrorStats.get(4)), String.valueOf(voltageErrorStats.get(5)),
                        String.valueOf(angleErrorStats.get(0)), String.valueOf(angleErrorStats.get(1)),
                        String.valueOf(angleErrorStats.get(2)), String.valueOf(angleErrorStats.get(3)),
                        String.valueOf(angleErrorStats.get(4)), String.valueOf(angleErrorStats.get(5)),
                        String.valueOf(knowledge.getVoltageMagnitudeMeasures().size())));
            }
        }

        // Export the results in a CSV file
        try (FileWriter fileWriter = new FileWriter("ZtoNratio_test_IEEE118_v2.csv");
             CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT)) {
            csvPrinter.printRecord(headers);

            for (List<String> row : data) {
                csvPrinter.printRecord(row);
            }

            System.out.println("CSV file has been created successfully!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

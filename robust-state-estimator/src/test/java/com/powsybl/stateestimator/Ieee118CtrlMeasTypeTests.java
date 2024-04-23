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
public class Ieee118CtrlMeasTypeTests {

    @Test
    void ieee118BusesTests() throws IOException {

        // Initialize the dataframe that will store the results
        List<String> headers = List.of("RatioMeasuresToBuses", "Seed",
                "MeanVError(%)", "StdVError(%)", "MedianVError(%)", "MaxVError(%)",
                "5percentileVError(%)", "95percentileVError(%)",
                "MeanThetaError(deg)", "StdThetaError(deg)", "MedianThetaError(deg)", "MaxThetaError(deg)",
                "5percentileThetaError(deg)", "95percentileThetaError(deg)",
                "MeanPfError(%)", "StdPfError(%)", "MedianPfError(%)", "MaxPfError(%)",
                "5percentilePfError(%)", "95percentilePfError(%)",
                "MeanQfError(%)", "StdQfError(%)", "MedianQfError(%)", "MaxQfError(%)",
                "5percentileQfError(%)", "95percentileQfError(%)",
                "NbVMeasures","NbPfMeasures","NbQfMeasures","NbPMeasures","NbQMeasures",
                "PerformanceIndex");

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

        // All ratioForCtrlMeasType to be tested:
        // For V measurements
        //List<Double> ratiosTested = Arrays.asList(0.0, 0.05, 0.10, 0.15, 0.20, 0.25);
        // For P/Q measurements (take the non-measurability of zero-injection buses into account)
        //List<Double> ratiosTested = Arrays.asList(0.0, 0.04, 0.09, 0.14, 0.19, 0.23);
        // For Pf/Qf measurements
        List<Double> ratiosTested = Arrays.asList(0.0, 0.15, 0.30, 0.45, 0.60, 0.75);

        for (Double ratioTested : ratiosTested) {

            System.out.println(ratioTested);

            for (int seed = 0; seed < 100; seed++) {

                // Create "knowledge" instance
                StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network);
                // For IEEE 118 bus, slack is "VL69_0": our state estimator must use the same slack
                knowledge.setSlack("VL69_0", network);

                // Randomly generate measurements out of load flow results using proper seed and Z to N ratio
                RandomMeasuresGenerator.generateRandomMeasurementsWithCtrlMeasureRatio(knowledge, network,
                        ratioTested, "Qf",
                        Optional.of(seed), Optional.of(4.0),
                        Optional.empty(), Optional.of(true),
                        Optional.empty(), Optional.empty());

                // Define the solving options for the state estimation
                StateEstimatorOptions options = new StateEstimatorOptions()
                        .setSolvingMode(2).setMaxTimeSolving(30).setMaxNbTopologyChanges(5);

                // Run the state estimation and save the results
                StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                        knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager());

                // Save statistics on the accuracy of the state estimation w.r.t load flow solution
                StateEstimatorEvaluator evaluator = new StateEstimatorEvaluator(network, knowledge, results);
                List<Double> voltageErrorStats = evaluator.computeVoltageRelativeErrorStats();
                List<Double> angleErrorStats = evaluator.computeAngleDegreeErrorStats();
                List<Double> PfErrorStats = evaluator.computeActivePowerFlowsRelativeErrorsStats();
                List<Double> QfErrorStats = evaluator.computeReactivePowerFlowsRelativeErrorsStats();
                data.add(List.of(String.valueOf(ratioTested), String.valueOf(seed),
                        String.valueOf(voltageErrorStats.get(0)), String.valueOf(voltageErrorStats.get(1)),
                        String.valueOf(voltageErrorStats.get(2)), String.valueOf(voltageErrorStats.get(3)),
                        String.valueOf(voltageErrorStats.get(4)), String.valueOf(voltageErrorStats.get(5)),
                        String.valueOf(angleErrorStats.get(0)), String.valueOf(angleErrorStats.get(1)),
                        String.valueOf(angleErrorStats.get(2)), String.valueOf(angleErrorStats.get(3)),
                        String.valueOf(angleErrorStats.get(4)), String.valueOf(angleErrorStats.get(5)),
                        String.valueOf(PfErrorStats.get(0)), String.valueOf(PfErrorStats.get(1)),
                        String.valueOf(PfErrorStats.get(2)), String.valueOf(PfErrorStats.get(3)),
                        String.valueOf(PfErrorStats.get(4)), String.valueOf(PfErrorStats.get(5)),
                        String.valueOf(QfErrorStats.get(0)), String.valueOf(QfErrorStats.get(1)),
                        String.valueOf(QfErrorStats.get(2)), String.valueOf(QfErrorStats.get(3)),
                        String.valueOf(QfErrorStats.get(4)), String.valueOf(QfErrorStats.get(5)),
                        String.valueOf(knowledge.getVoltageMagnitudeMeasures().size()),
                        String.valueOf(knowledge.getActivePowerFlowMeasures().size()),
                        String.valueOf(knowledge.getReactivePowerFlowMeasures().size()),
                        String.valueOf(knowledge.getActivePowerInjectedMeasures().size()),
                        String.valueOf(knowledge.getReactivePowerInjectedMeasures().size()),
                        String.valueOf(evaluator.computePerformanceIndex())));
            }
        }

        // Export the results in a CSV file
        try (FileWriter fileWriter = new FileWriter("Qf_ctrlMeasRatio_IEEE118.csv");
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

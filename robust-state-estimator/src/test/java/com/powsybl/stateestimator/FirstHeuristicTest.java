/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator;

import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.stateestimator.parameters.input.knowledge.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static com.powsybl.openloadflow.OpenLoadFlowParameters.LowImpedanceBranchMode.REPLACE_BY_MIN_IMPEDANCE_LINE;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class FirstHeuristicTest {

    @Test
    void unitTest() throws IOException {

        Network network = Network.read(Path.of("D:", "Projet", "Réseaux_tests", "IIDM", "pglib_opf_case1354_pegase.xiidm"));

        LoadFlowParameters parametersLf = new LoadFlowParameters();
        OpenLoadFlowParameters parametersExt = OpenLoadFlowParameters.create(parametersLf);
        parametersExt.setAlwaysUpdateNetwork(true)
                .setLowImpedanceBranchMode(REPLACE_BY_MIN_IMPEDANCE_LINE)
                .setLowImpedanceThreshold(1e-4);

        network.getLine("LINE-6757-6036").disconnect(); // for case1354_pegase
        LoadFlowResult loadFlowResult = LoadFlow.run(network, parametersLf);
        assertTrue(loadFlowResult.isFullyConverged());
        network.getLine("LINE-6757-6036").connect();

        StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network, "VL-4231_0"); // for case1354_pegase
        for (Branch branch: network.getBranches()) {
            knowledge.setSuspectBranch(branch.getId(), true, "PRESUMED CLOSED");
        }
        RandomMeasuresGenerator.generateRandomMeasurementsWithCtrlMeasureRatio(knowledge, network,
                0.1991137371, "P",
                Optional.of(41), Optional.of(5.0),
                Optional.empty(), Optional.of(true),
                Optional.empty(), Optional.empty());

        Pair<StateEstimatorResults, StateEstimatorKnowledge> firstHeuristicResults = StateEstimatorHeuristic.firstHeuristic(knowledge, network);

        StateEstimatorResults finalResults = firstHeuristicResults.getFirst();
        StateEstimatorKnowledge finalKnowledge = firstHeuristicResults.getSecond();

        finalResults.printNetworkTopology();
    }

    @Test
    void test() throws IOException {

        // TODO : don't forget to change solving modes in StateEstimatorHeuristic before launching the test

        // Initialize the dataframe that will store the results
        List<String> headers = List.of("RatioMeasuresToBuses", "Seed",
                "FalseLineDetected", "NbTopologyErrors",
                "MeanVError(%)", "StdVError(%)", "MedianVError(%)", "MaxVError(%)",
                "5percentileVError(%)", "95percentileVError(%)",
                "MeanThetaError(deg)", "StdThetaError(deg)", "MedianThetaError(deg)", "MaxThetaError(deg)",
                "5percentileThetaError(deg)", "95percentileThetaError(deg)",
                "MeanPfError(%)", "StdPfError(%)", "MedianPfError(%)", "MaxPfError(%)",
                "5percentilePfError(%)", "95percentilePfError(%)",
                "MeanQfError(%)", "StdQfError(%)", "MedianQfError(%)", "MaxQfError(%)",
                "5percentileQfError(%)", "95percentileQfError(%)",
                "NbVMeasures","NbPfMeasures","NbQfMeasures","NbPMeasures","NbQMeasures",
                "ObjectiveFunctionValue",
                "PerformanceIndex"
        );
        List<List<String>> data = new ArrayList<>();

        Network network = IeeeCdfNetworkFactory.create118();

        String erroneousLine = "L45-46-1";
        // TODO : delete if erroneousLine added
        //String erroneousLine = "_";

        // Disconnect the erroneous line
        network.getLine(erroneousLine).disconnect();

        // Load Flow parameters (note : we mimic the way the AMPL code deals with zero-impedance branches)
        LoadFlowParameters parametersLf = new LoadFlowParameters();
        OpenLoadFlowParameters parametersExt = OpenLoadFlowParameters.create(parametersLf);
        parametersExt.setAlwaysUpdateNetwork(true)
                .setLowImpedanceBranchMode(REPLACE_BY_MIN_IMPEDANCE_LINE)
                .setLowImpedanceThreshold(1e-4);

        // Solve the Load Flow problem for the network
        LoadFlowResult loadFlowResult = LoadFlow.run(network, parametersLf);
        assertTrue(loadFlowResult.isFullyConverged());

        // Reconnect the erroneous line, if any
        network.getLine(erroneousLine).connect();

        double ratioTested = 5.0;

        for (int seed = 0; seed < 100; seed++) {

            System.out.println();
            System.out.println(seed);

            // Create "knowledge" instance : for IEEE 118 bus, the slack is "VL69_0"
            StateEstimatorKnowledge knowledgeV1 = new StateEstimatorKnowledge(network, "VL69_0");

            // Add a gross error on measure Pf(VL27 --> VL28) : 80 MW (false) instead of 32.6 MW (true)
            Map<String, String> grossMeasure1 = Map.of("BranchID", "L27-28-1", "FirstBusID", "VL27_0", "SecondBusID", "VL28_0",
                    "Value", "80.0", "Variance", "0.1306", "Type", "Pf");
            knowledgeV1.addMeasure(1, grossMeasure1, network);

            // Add a gross error on measure V(VL60) : 225 kV (false) instead of 137 kV (true)
            //Map<String, String> grossMeasure2 = Map.of("BusID", "VL60_0",
            //        "Value", "225.0", "Variance", "0.488", "Type", "V");
            //knowledgeV1.addMeasure(2, grossMeasure2, network);

            // Randomly generate measurements out of load flow results
            RandomMeasuresGenerator.generateRandomMeasurements(knowledgeV1, network,
                    Optional.of(seed), Optional.of(ratioTested),
                    Optional.of(false), Optional.of(true),
                    Optional.empty(), Optional.empty(),
                    Optional.of(false));

            // Run heuristic SE on knowledgeV1
            Pair<StateEstimatorResults, StateEstimatorKnowledge> firstHeuristicResults = StateEstimatorHeuristic.firstHeuristic(knowledgeV1, network);

            StateEstimatorResults finalResults = firstHeuristicResults.getFirst();
            StateEstimatorKnowledge finalKnowledge = firstHeuristicResults.getSecond();

            // Compute the number of topology errors, and find if the erroneous line (if any) was given the correct status ("OPENED")
            int falseLineDetected = 0;
            int nbTopologyErrors = 0;
            for (Branch branch : network.getBranches()) {
                if (branch.getId().equals(erroneousLine)) {
                    if (finalResults.getBranchStatusEstimate(erroneousLine).getEstimatedStatus().equals("OPENED")) {
                        falseLineDetected = 1;
                    } else {
                        nbTopologyErrors += 1;
                    }
                }
                else {
                    if (finalResults.getBranchStatusEstimate(branch.getId()).getEstimatedStatus().equals("OPENED")) {
                        nbTopologyErrors += 1;
                    }
                }
            }

            // Save statistics on the accuracy of the state estimation w.r.t load flow solution
            StateEstimatorEvaluator evaluator = new StateEstimatorEvaluator(network, finalKnowledge, finalResults);
            List<Double> voltageErrorStats = evaluator.computeVoltageRelativeErrorStats();
            List<Double> angleErrorStats = evaluator.computeAngleDegreeErrorStats();
            List<Double> PfErrorStats = evaluator.computeActivePowerFlowsRelativeErrorsStats();
            List<Double> QfErrorStats = evaluator.computeReactivePowerFlowsRelativeErrorsStats();
            data.add(List.of(String.valueOf(ratioTested), String.valueOf(seed),
                    String.valueOf(falseLineDetected), String.valueOf(nbTopologyErrors),
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
                    String.valueOf(finalKnowledge.getVoltageMagnitudeMeasures().size()),
                    String.valueOf(finalKnowledge.getActivePowerFlowMeasures().size()),
                    String.valueOf(finalKnowledge.getReactivePowerFlowMeasures().size()),
                    String.valueOf(finalKnowledge.getActivePowerInjectedMeasures().size()),
                    String.valueOf(finalKnowledge.getReactivePowerInjectedMeasures().size()),
                    String.valueOf(finalResults.getObjectiveFunctionValue()),
                    String.valueOf(evaluator.computePerformanceIndex())
            ));
        }

        // Export the results in a CSV file
        try (FileWriter fileWriter = new FileWriter("WithNoise_Pf-27-28_WithTopoError_ZN5_Heuristic_IEEE118.csv");
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


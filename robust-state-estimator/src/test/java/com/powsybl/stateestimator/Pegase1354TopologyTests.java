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
import com.powsybl.stateestimator.parameters.output.estimates.BranchStatusEstimate;
import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.Test;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openloadflow.OpenLoadFlowParameters.LowImpedanceBranchMode.REPLACE_BY_MIN_IMPEDANCE_LINE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class Pegase1354TopologyTests {

    @Test
    void pegase1354BusesTests() throws IOException {

        // Initialize the dataframe that will store the results
        List<String> headers = List.of("RatioMeasuresToBuses", "Seed",
                "FalseLineDetected", "NbTopologyErrors",
                "FalseLineNearDetection",
                "MeanVError(%)", "StdVError(%)", "MedianVError(%)", "MaxVError(%)",
                "5percentileVError(%)", "95percentileVError(%)",
                "MeanThetaError(deg)", "StdThetaError(deg)", "MedianThetaError(deg)", "MaxThetaError(deg)",
                "5percentileThetaError(deg)", "95percentileThetaError(deg)",
                "MeanPfError(%)", "StdPfError(%)", "MedianPfError(%)", "MaxPfError(%)",
                "5percentilePfError(%)", "95percentilePfError(%)",
                "MeanQfError(%)", "StdQfError(%)", "MedianQfError(%)", "MaxQfError(%)",
                "5percentileQfError(%)", "95percentileQfError(%)",
                "NbVMeasures","NbPfMeasures","NbQfMeasures","NbPMeasures","NbQMeasures"
                //"PerformanceIndex"
        );
        List<List<String>> data = new ArrayList<>();

        Network network = Network.read(Path.of("D:", "Projet", "Réseaux_tests", "IIDM", "pglib_opf_case1354_pegase.xiidm"));

        String erroneousLine = "LINE-6757-6036";

        // Disconnect the erroneous line
        network.getLine(erroneousLine).disconnect();

        // Load Flow parameters (note : we mimic the way the AMPL code deals with zero-impedance branches)
        LoadFlowParameters parametersLf = new LoadFlowParameters();
        OpenLoadFlowParameters parametersExt = OpenLoadFlowParameters.create(parametersLf);
        parametersExt.setAlwaysUpdateNetwork(true)
                .setLowImpedanceBranchMode(REPLACE_BY_MIN_IMPEDANCE_LINE)
                .setLowImpedanceThreshold(1e-4);

        // Solve the Load Flow problem for the network with the disconnected line: use these results as measurements
        LoadFlowResult loadFlowResult = LoadFlow.run(network, parametersLf);
        assertTrue(loadFlowResult.isFullyConverged());

        // Reconnect the erroneous line
        network.getLine(erroneousLine).connect();

        // All MeasuresToBuses ratios to be tested
        //List<Double> ratiosTested = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        List<Double> ratiosTested = Arrays.asList(5.0);

        for (Double ratioTested : ratiosTested) {

            System.out.println(ratioTested);

            for (int seed = 0; seed < 10; seed++) {

                // Create "knowledge" instance and indicate slack bus
                StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network, "VL-4231_0");

                // Make all branches suspects and presumed to be closed
                for (Branch branch: network.getBranches()) {
                    knowledge.setSuspectBranch(branch.getId(), true, "PRESUMED CLOSED");
                }

                // Randomly generate measurements out of load flow results, with all P measures (ensure observability)
                RandomMeasuresGenerator.generateRandomMeasurementsWithCtrlMeasureRatio(knowledge, network,
                        0.1991137371, "P",
                        Optional.of(seed), Optional.of(ratioTested),
                        Optional.empty(), Optional.of(false),
                        Optional.empty(), Optional.empty());

                // Define the solving options for the state estimation
                StateEstimatorOptions options = new StateEstimatorOptions()
                        .setSolvingMode(2).setMaxTimeSolving(60).setMaxNbTopologyChanges(5);

                // Run the state estimation and save the results
                StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                        knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager());

                // Find if the erroneous line is detected or at least is the first neighbour of a changed line
                int falseLineNearDetection = 0;
                List<String> changedBranchesID = new ArrayList<>();
                // Build the list of changed branches
                for (Branch branch : network.getBranches()) {
                    BranchStatusEstimate branchEstimate = results.getBranchStatusEstimate(branch.getId());
                    if (! branchEstimate.getPresumedStatus().equals("PRESUMED " + branchEstimate.getEstimatedStatus())) {
                        changedBranchesID.add(branch.getId());
                    }
                } // Build the list of all neighbouring branches to the erroneous line
                Bus erroneousLineEnd1 = network.getBranch(erroneousLine).getTerminal1().getBusView().getConnectableBus();
                Bus erroneousLineEnd2 = network.getBranch(erroneousLine).getTerminal2().getBusView().getConnectableBus();
                List<String> neighboursIDs = new ArrayList<>(erroneousLineEnd1.getLineStream().map(Identifiable::getId).toList());
                neighboursIDs.addAll(erroneousLineEnd1.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                neighboursIDs.addAll(erroneousLineEnd2.getLineStream().map(Identifiable::getId).toList());
                neighboursIDs.addAll(erroneousLineEnd2.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                for (String ID : changedBranchesID) {
                    if (neighboursIDs.contains(ID)) {
                        falseLineNearDetection = 1;
                        break;
                    }
                }

                // TODO : delete if erroneousLine specified
                //erroneousLine = "_";

                // Compute the number of topology errors, and find if the erroneous line was given the correct status ("OPENED")
                int falseLineDetected = 0;
                int nbTopologyErrors = 0;
                for (Branch branch : network.getBranches()) {
                    if (branch.getId().equals(erroneousLine)) {
                        if (results.getBranchStatusEstimate(erroneousLine).getEstimatedStatus().equals("OPENED")) {
                            falseLineDetected = 1;
                        } else {
                            nbTopologyErrors += 1;
                        }
                    }
                    else {
                        if (results.getBranchStatusEstimate(branch.getId()).getEstimatedStatus().equals("OPENED")) {
                            nbTopologyErrors += 1;
                        }
                    }
                }

                // TODO : delete if erroneousLine specified
                //erroneousLine = "LINE-6757-6036";

                // Save statistics on the accuracy of the state estimation w.r.t load flow solution
                StateEstimatorEvaluator evaluator = new StateEstimatorEvaluator(network, knowledge, results);
                List<Double> voltageErrorStats = evaluator.computeVoltageRelativeErrorStats();
                List<Double> angleErrorStats = evaluator.computeAngleDegreeErrorStats();
                List<Double> PfErrorStats = evaluator.computeActivePowerFlowsRelativeErrorsStats();
                List<Double> QfErrorStats = evaluator.computeReactivePowerFlowsRelativeErrorsStats();
                data.add(List.of(String.valueOf(ratioTested), String.valueOf(seed),
                        String.valueOf(falseLineDetected), String.valueOf(nbTopologyErrors),
                        String.valueOf(falseLineNearDetection),
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
                        String.valueOf(knowledge.getReactivePowerInjectedMeasures().size())
                        //String.valueOf(evaluator.computePerformanceIndex())
                ));
            }
        }

        // Export the results in a CSV file
        try (FileWriter fileWriter = new FileWriter("SM2_5NbTopoChanges_ZN5_AllLinesSuspect_NoNoise_L6757-6036-OPENED_Pegase1354.csv");
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

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
public class Ieee118TopologyTests {

    @Test
    void ieee118BusesTests() throws IOException {

        // Initialize the dataframe that will store the results
        List<String> headers = List.of("RatioMeasuresToBuses", "Seed",
                "FalseLineDetected", "NbTopologyErrors",
                "FalseLineNearDetection",
                "MeanVError(%)", "StdVError(%)", "MedianVError(%)", "MaxVError(%)",
                "5percentileVError(%)", "95percentileVError(%)",
                "MeanThetaError(deg)", "StdThetaError(deg)", "MedianThetaError(deg)", "MaxThetaError(deg)",
                "5percentileThetaError(deg)", "95percentileThetaError(deg)",
                "MeanPfError(%)", "StdPfError(%)", "MedianPfError(%)", "MaxPfError(%)",
                "5percentilePfError(%)", "95percentilePfError(%)", "MaxPfAbsoluteError(MW)",
                "MeanQfError(%)", "StdQfError(%)", "MedianQfError(%)", "MaxQfError(%)",
                "5percentileQfError(%)", "95percentileQfError(%)", "MaxQfAbsoluteError(MVar)",
                "NbVMeasures","NbPfMeasures","NbQfMeasures","NbPMeasures","NbQMeasures",
                "ObjectiveFunctionValue"
                //,"PerformanceIndex"
        );
        List<List<String>> data = new ArrayList<>();

        Network network = IeeeCdfNetworkFactory.create118();

        String erroneousLine = "L45-46-1";

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

            for (int seed = 0; seed < 100; seed++) {

                // Create "knowledge" instance : for IEEE 118 bus, slack is "VL69_0": our state estimator must use the same slack
                StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network, "VL69_0");

                // Make all branches suspects and presumed to be closed
                for (Branch branch: network.getBranches()) {
                    knowledge.setSuspectBranch(branch.getId(), false, "PRESUMED CLOSED");
                }

                // Make only branches around the erroneous one suspects
                List<String> localSuspectBranches = new ArrayList<>(List.of(
                        "L45-46-1","L44-45-1", "L45-49-1","L46-48-1","L46-47-1",
                        "L47-49-1","L48-49-1","L43-44-1","L47-69-1","L49-69-1",
                        "L34-43-1"
                ));
                for (String localSuspectBranchID : localSuspectBranches) {
                    knowledge.setSuspectBranch(localSuspectBranchID, true, "PRESUMED CLOSED");
                }

                // Randomly generate measurements out of LF results using proper seed and Z to N ratio
                RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network,
                        Optional.of(seed), Optional.of(ratioTested),
                        Optional.of(false), Optional.empty(),
                        Optional.empty(), Optional.of(true));

                //RandomMeasuresGenerator.generateRandomMeasurementsWithCtrlMeasureRatio(knowledge, network,
                //        0.18644067796, "P",
                //        Optional.of(seed), Optional.of(ratioTested),
                //        Optional.empty(), Optional.of(false),
                //        Optional.empty(), Optional.empty());

                // Define the solving options for the state estimation
                StateEstimatorOptions options = new StateEstimatorOptions()
                        .setSolvingMode(0).setMaxTimeSolving(120).setMaxNbTopologyChanges(2);

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
                erroneousLine = "L45-46-1";

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
                        String.valueOf(PfErrorStats.get(6)),
                        String.valueOf(QfErrorStats.get(0)), String.valueOf(QfErrorStats.get(1)),
                        String.valueOf(QfErrorStats.get(2)), String.valueOf(QfErrorStats.get(3)),
                        String.valueOf(QfErrorStats.get(4)), String.valueOf(QfErrorStats.get(5)),
                        String.valueOf(QfErrorStats.get(6)),
                        String.valueOf(knowledge.getVoltageMagnitudeMeasures().size()),
                        String.valueOf(knowledge.getActivePowerFlowMeasures().size()),
                        String.valueOf(knowledge.getReactivePowerFlowMeasures().size()),
                        String.valueOf(knowledge.getActivePowerInjectedMeasures().size()),
                        String.valueOf(knowledge.getReactivePowerInjectedMeasures().size()),
                        String.valueOf(results.getObjectiveFunctionValue())
                        //,String.valueOf(evaluator.computePerformanceIndex())
                ));
            }
        }

        // Export the results in a CSV file
        try (FileWriter fileWriter = new FileWriter("ZN5_InjBus_MinimizTopoChanges_NoMS_120secMax_EnsObs_NoInterVar_SM0_2TopoMax_11Lines_NoNoise_L45-46.csv");
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

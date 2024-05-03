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
import com.powsybl.stateestimator.parameters.input.knowledge.*;
import com.powsybl.stateestimator.parameters.input.options.StateEstimatorOptions;
import com.powsybl.stateestimator.parameters.output.estimates.BranchStatusEstimate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static com.powsybl.openloadflow.OpenLoadFlowParameters.LowImpedanceBranchMode.REPLACE_BY_MIN_IMPEDANCE_LINE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class FirstHeuristicTest {

    @Test
    void test() throws IOException {

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
                "NbVMeasures","NbPfMeasures","NbQfMeasures","NbPMeasures","NbQMeasures"
                //"PerformanceIndex"
        );
        List<List<String>> data = new ArrayList<>();

        Network network = IeeeCdfNetworkFactory.create118();

        //String erroneousLine = "L24-72-1";

        // Disconnect the erroneous line
        //network.getLine(erroneousLine).disconnect();

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
        //network.getLine(erroneousLine).connect();

        double ratioTested = 5.0;

        for (int seed = 0; seed < 100; seed++) {

            System.out.println();
            System.out.println(seed);

            // Create "knowledge" instance
            StateEstimatorKnowledge knowledgeV1 = new StateEstimatorKnowledge(network);

            // For IEEE 118 bus, the slack is "VL69_0": our state estimator must use the same slack
            knowledgeV1.setSlack("VL69_0", network);

            // Add a gross error on measure Pf(VL27 --> VL28) : 80 MW (false) instead of 32.6 MW (true)
            //Map<String, String> grossMeasure1 = Map.of("BranchID", "L27-28-1", "FirstBusID", "VL27_0", "SecondBusID", "VL28_0",
            //        "Value", "80.0", "Variance", "0.1306", "Type", "Pf");
            //knowledgeV1.addMeasure(1, grossMeasure1, network);

            // Add a gross error on measure V(VL60) : 225 kV (false) instead of 137 kV (true)
            //Map<String, String> grossMeasure2 = Map.of("BusID", "VL60_0",
            //        "Value", "225.0", "Variance", "0.488", "Type", "V");
            //knowledgeV1.addMeasure(2, grossMeasure2, network);

            // Randomly generate measurements out of load flow results
            RandomMeasuresGenerator.generateRandomMeasurements(knowledgeV1, network,
                    Optional.of(seed), Optional.of(ratioTested),
                    Optional.of(false), Optional.of(true),
                    Optional.empty(), Optional.empty());

            // Run heuristic SE on knowledgeV1
            Pair<StateEstimatorResults, StateEstimatorKnowledge> firstHeuristicResults = firstHeuristic(knowledgeV1, network);

            StateEstimatorResults finalResults = firstHeuristicResults.getFirst();
            StateEstimatorKnowledge finalKnowledge = firstHeuristicResults.getSecond();

            // TODO : delete if erroneousLine added
            String erroneousLine = "_";

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
                    String.valueOf(finalKnowledge.getReactivePowerInjectedMeasures().size())
                    //String.valueOf(evaluator.computePerformanceIndex())
            ));
        }

        // Export the results in a CSV file
        try (FileWriter fileWriter = new FileWriter("Vanilla_WithNoise_NoError_ZtoN5_Heuristic_IEEE118.csv");
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

    Pair<StateEstimatorResults, StateEstimatorKnowledge> firstHeuristic(StateEstimatorKnowledge knowledgeV1, Network network) throws IOException {

        // BEGINNING OF THE HEURISTIC PROCESS

        // Step 1 : first SE run, NbTopologyChanges = 10, all branches suspected

        // Make all branches suspects and presumed to be closed
        // + Save initial status
        Map<String, String> initialStatus = new HashMap<>();
        for (Branch branch : network.getBranches()) {
            knowledgeV1.setSuspectBranch(branch.getId(), true, "PRESUMED CLOSED");
            initialStatus.put(branch.getId(), "CLOSED");
        }
        // Define the solving options for the state estimation
        StateEstimatorOptions optionsV1 = new StateEstimatorOptions()
                .setSolvingMode(2).setMaxTimeSolving(30).setMaxNbTopologyChanges(10);

        // Run the state estimation
        StateEstimatorResults resultsV1 = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                knowledgeV1, optionsV1, new StateEstimatorConfig(true), new LocalComputationManager());

        StateEstimatorEvaluator evaluatorV1 = new StateEstimatorEvaluator(network, knowledgeV1, resultsV1);

        // Step 2 : remove measure with LNR and suspect only changed lines and neighbouring lines to changed lines.
        // maxNbTopoChanges = 1 by zone

        // Find measure with LNR in step 1
        List<Map.Entry<Integer, Double>> sortedResiduals = computeAndSortNormalizedResiduals(knowledgeV1, resultsV1);
        int nbMeasureLNR = sortedResiduals.get(0).getKey();
        double LNR = sortedResiduals.get(0).getValue();

        // Compute and print objective function value
        double objectiveFunctionValue = sortedResiduals.stream().mapToDouble(e-> Math.pow(e.getValue(),2)).sum();
        System.out.printf("%nObjective function value : %f %n", objectiveFunctionValue);

        // Build the list of branches changed during step 1, with and without their neighbours
        Set<String> changedBranches = new HashSet<>();
        Set<String> step1changedBranchesAndNeighbours = new HashSet<>();
        // Build a map linking each changed branch to itself and its neighbours
        Map<String, Set<String>> neighboursOfChangedBranches = new HashMap<>();
        for (Branch branch : network.getBranches()) {
            BranchStatusEstimate branchEstimate = resultsV1.getBranchStatusEstimate(branch.getId());
            // If a change of status is detected
            if (!branchEstimate.getPresumedStatus().equals("PRESUMED " + branchEstimate.getEstimatedStatus())) {
                // Update the first list
                changedBranches.add(branch.getId());
                // Update the second list
                step1changedBranchesAndNeighbours.add(branch.getId());
                Bus end1 = network.getBranch(branch.getId()).getTerminal1().getBusView().getBus();
                Bus end2 = network.getBranch(branch.getId()).getTerminal2().getBusView().getBus();
                step1changedBranchesAndNeighbours.addAll(end1.getLineStream().map(Identifiable::getId).toList());
                step1changedBranchesAndNeighbours.addAll(end1.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                step1changedBranchesAndNeighbours.addAll(end2.getLineStream().map(Identifiable::getId).toList());
                step1changedBranchesAndNeighbours.addAll(end2.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                // Update the map
                Set<String> neighboursOfChangedBranch = new HashSet<>();
                neighboursOfChangedBranch.add(branch.getId());
                neighboursOfChangedBranch.addAll(end1.getLineStream().map(Identifiable::getId).toList());
                neighboursOfChangedBranch.addAll(end1.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                neighboursOfChangedBranch.addAll(end2.getLineStream().map(Identifiable::getId).toList());
                neighboursOfChangedBranch.addAll(end2.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                neighboursOfChangedBranches.put(branch.getId(), neighboursOfChangedBranch);
            }
        }

        // Find all the disjoint sets of suspect branches
        Map<String, Set<String>> connectedComponentOfChangedBranches = new HashMap<>();
        // Initialize the connected component of each changed branch (by default, equals the changed branch and its neighbours)
        for (String branch1 : changedBranches) {
            connectedComponentOfChangedBranches.put(branch1, new HashSet<>(neighboursOfChangedBranches.get(branch1)));
        }
        boolean hasProcessConverged = false;
        // Iterative process performed as long as connected components keep getting changed
        while (!hasProcessConverged) {
            hasProcessConverged = true;
            for (String branch1 : changedBranches) {
                Set<String> s1 = connectedComponentOfChangedBranches.get(branch1);
                for (String branch2 : changedBranches) {
                    if (!branch1.equals(branch2)) {
                        Set<String> s2 = connectedComponentOfChangedBranches.get(branch2);
                        // Compute intersection between s1 and s2
                        Set<String> intersection = new HashSet<>(s1); // use the copy constructor
                        intersection.retainAll(s2);
                        // If intersection is not empty, merge s2 with s1
                        if (!intersection.isEmpty()) {
                            Set<String> union = new HashSet<>(s1);
                            union.addAll(s2);
                            // Put the union of the two sets as the new connected component for both branches
                            connectedComponentOfChangedBranches.put(branch1, union);
                            connectedComponentOfChangedBranches.put(branch2, union);
                            // If there was indeed a change, detect it
                            if (!s1.equals(union) | !s2.equals(union)) {
                                hasProcessConverged = false;
                            }
                        }
                    }
                }
            }
        }

        // Make a list of the disjoint sets of suspect branches
        Set<Set<String>> disjointSetsOfSuspectBranches = new HashSet<>(connectedComponentOfChangedBranches.values().stream().toList());
        List<Set<String>> listOfSuspectRegions = new ArrayList<>(disjointSetsOfSuspectBranches);

        // Prepare iterative loop
        StateEstimatorKnowledge knowledgeV2 = knowledgeV1;
        StateEstimatorResults resultsV2 = resultsV1;

        // Initialize the set of branches that have been inspected during the cycle and whose status is fixed for this cycle
        Set<String> inspectedBranches = new HashSet<>();
        // By default, it contains all branches that do not belong to changedBranchesAndNeighbours
        for (Branch branch : network.getBranches()) {
            if (!step1changedBranchesAndNeighbours.contains(branch.getId())) {
                inspectedBranches.add(branch.getId());
            }
        }

        // Define the maximum number of iterations
        int nbIter = 0;
        int nbIterMax = 3 + 2 * listOfSuspectRegions.size(); // At least 3 iterations + 2 cycles of "topology inspection"

        // While the Largest Normalized Residual exceeds a given threshold
        while (LNR > 3 && nbIter < nbIterMax) {

            // Make a deep copy of knowledgeV1
            knowledgeV2 = new StateEstimatorKnowledge(knowledgeV1);

            // For current iteration, remove the measurement with LNR of the previous iteration
            // Exception 1 : if LNR < 3, do not remove the LNR measure
            // Exception 2 : if the LNR measure is a power flow and is directly related to a branch that was changed
            // at the previous iteration, do not remove it
            // ==> At most 1 measurement can be removed at each iteration
            ArrayList<String> measureLNR =  knowledgeV2.getMeasure(nbMeasureLNR);
            int tmpIndex = 1;
            while(LNR > 3 && tmpIndex < sortedResiduals.size()) {
                // If LNR measure is directly related to a change of topology, do not remove it...
                if ((measureLNR.get(0).equals("Pf") | measureLNR.get(0).equals("Qf"))
                        && changedBranches.contains(measureLNR.get(1))) {
                    // ... and consider the measure with second LNR
                    nbMeasureLNR = sortedResiduals.get(tmpIndex).getKey();
                    LNR = sortedResiduals.get(tmpIndex).getValue();
                    measureLNR =  knowledgeV2.getMeasure(nbMeasureLNR);
                    tmpIndex++;
                } else {
                    knowledgeV2.removeMeasure(nbMeasureLNR);
                    break;
                }
            }

            // For current iteration, pick one set of suspect branches ("suspectRegion")
            // and allow topology changes (1) only for this set of branches.
            // Done in a cyclic way : if listOfSuspectRegions.size=3, we pick region 1, then region 2, then region 3, then region 1, etc
            Set<String> suspectRegion;
            if (listOfSuspectRegions.isEmpty()) {
                suspectRegion = Collections.emptySet();
            } else {
                suspectRegion = listOfSuspectRegions.get(nbIter % listOfSuspectRegions.size());
            }
            for (Branch branch : network.getBranches()) {
                // If we are in the first cycle:
                if (nbIter < listOfSuspectRegions.size()) {
                    // Give "step 1" presumed status for any branch in the suspect region
                    if (suspectRegion.contains(branch.getId())) {
                        knowledgeV2.setSuspectBranch(branch.getId(), true,
                                "PRESUMED CLOSED");
                    }
                    // If the branch is not in the suspect region and has already been inspected
                    // in the current cycle, give it its last found status
                    else if (inspectedBranches.contains(branch.getId())) {
                        knowledgeV2.setSuspectBranch(branch.getId(), false,
                                "PRESUMED " + resultsV2.getBranchStatusEstimate(branch.getId()).getEstimatedStatus());
                    }
                    // If a branch is yet to be inspected during this cycle (but not at this iteration),
                    // give it "step 1" presumed status
                    else {
                        knowledgeV2.setSuspectBranch(branch.getId(), false,
                                "PRESUMED CLOSED");
                    }
                }
                // If we are not in the first cycle:
                else {
                    // Give last found status for any branch in the suspect region
                    if (suspectRegion.contains(branch.getId())) {
                        knowledgeV2.setSuspectBranch(branch.getId(), true,
                                "PRESUMED " + resultsV2.getBranchStatusEstimate(branch.getId()).getEstimatedStatus());
                    }
                    // Give last found status for any branch not in the suspect region
                    else {
                        knowledgeV2.setSuspectBranch(branch.getId(), false,
                                "PRESUMED " + resultsV2.getBranchStatusEstimate(branch.getId()).getEstimatedStatus());
                    }
                }
            }
            // Update the set of inspected branches
            inspectedBranches.addAll(suspectRegion);

            // Define new solving options, with MaxNbTopologyChanges = 1 this time
            StateEstimatorOptions optionsV2 = new StateEstimatorOptions()
                    .setSolvingMode(2).setMaxTimeSolving(30).setMaxNbTopologyChanges(1);

            // Run the state estimation again
            resultsV2 = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                    knowledgeV2, optionsV2, new StateEstimatorConfig(true), new LocalComputationManager());

            // Make a shallow copy of knowledgeV2, so that next knowledge update will be based on it
            knowledgeV1 = knowledgeV2;

            // Update normalized residuals and LNR
            sortedResiduals = computeAndSortNormalizedResiduals(knowledgeV2, resultsV2);
            nbMeasureLNR = sortedResiduals.get(0).getKey();
            LNR = sortedResiduals.get(0).getValue();

            // Compute and print objective function value
            objectiveFunctionValue = sortedResiduals.stream().mapToDouble(e-> Math.pow(e.getValue(),2)).sum();
            System.out.printf("%nObjective function value : %f %n", objectiveFunctionValue);

            // Update changedBranches (w.r.t initial status) with new results obtained
            changedBranches.clear();
            for (Branch branch : network.getBranches()) {
                BranchStatusEstimate branchEstimate = resultsV2.getBranchStatusEstimate(branch.getId());
                if (!branchEstimate.getEstimatedStatus().equals(initialStatus.get(branch.getId()))) {
                    changedBranches.add(branch.getId());
                }
            }

            nbIter++;

            // If we reach the end of a topology cycle (i.e. nbIter = 0 modulo listOfSuspectRegions.size),
            // reset the set of inspected branches
            if (!listOfSuspectRegions.isEmpty() && nbIter % listOfSuspectRegions.size() == 0) {
                inspectedBranches.clear();
                for (Branch branch : network.getBranches()) {
                    if (!step1changedBranchesAndNeighbours.contains(branch.getId())) {
                        inspectedBranches.add(branch.getId());
                    }
                }
            }
        }

        // END OF THE ITERATIVE PROCESS

        // Print indication on the iterative process
        if (nbIter==nbIterMax) {
            System.out.println("Process has not converged : nbIter = nbIterMax");
        }

        // Return the results and knowledge obtained
        return Pair.of(resultsV2, knowledgeV2);

    }


    List<Map.Entry<Integer,Double>> computeAndSortNormalizedResiduals (StateEstimatorKnowledge knowledge, StateEstimatorResults results){

        // Store measurement residuals given by the SE results
        ActivePowerFlowMeasures resultsPf = new ActivePowerFlowMeasures(knowledge.getActivePowerFlowMeasures(), results.getMeasurementEstimatesAndResiduals());
        ReactivePowerFlowMeasures resultsQf = new ReactivePowerFlowMeasures(knowledge.getReactivePowerFlowMeasures(), results.getMeasurementEstimatesAndResiduals());
        ActivePowerInjectedMeasures resultsP = new ActivePowerInjectedMeasures(knowledge.getActivePowerInjectedMeasures(), results.getMeasurementEstimatesAndResiduals());
        ReactivePowerInjectedMeasures resultsQ = new ReactivePowerInjectedMeasures(knowledge.getReactivePowerInjectedMeasures(), results.getMeasurementEstimatesAndResiduals());
        VoltageMagnitudeMeasures resultsV = new VoltageMagnitudeMeasures(knowledge.getVoltageMagnitudeMeasures(), results.getMeasurementEstimatesAndResiduals());

        // Compute and store all the normalized residuals (NR = residual / sqrt(variance))
        Map<Integer, Double> measureNumberToNormalizedResidual = new HashMap<>();

        // Active power flows
        for (var entry : resultsPf.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(7)) / Math.sqrt(Double.parseDouble(val.get(5)));
            measureNumberToNormalizedResidual.put(entry.getKey(), normalizedResidual);
        }
        // Reactive power flows
        for (var entry : resultsQf.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(7)) / Math.sqrt(Double.parseDouble(val.get(5)));
            measureNumberToNormalizedResidual.put(entry.getKey(), normalizedResidual);
        }
        // Active injected powers
        for (var entry : resultsP.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(5)) / Math.sqrt(Double.parseDouble(val.get(3)));
            measureNumberToNormalizedResidual.put(entry.getKey(), normalizedResidual);
        }
        // Reactive injected powers
        for (var entry : resultsQ.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(5)) / Math.sqrt(Double.parseDouble(val.get(3)));
            measureNumberToNormalizedResidual.put(entry.getKey(), normalizedResidual);
        }
        // Voltage magnitudes
        for (var entry : resultsV.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(5)) / Math.sqrt(Double.parseDouble(val.get(3)));
            measureNumberToNormalizedResidual.put(entry.getKey(), normalizedResidual);
        }

        // Sort normalized residuals by descending order and return the corresponding list
        return measureNumberToNormalizedResidual.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .toList();
    }

}


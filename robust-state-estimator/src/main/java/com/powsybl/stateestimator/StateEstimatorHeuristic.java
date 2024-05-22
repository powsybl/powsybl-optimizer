/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator;

import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.*;
import com.powsybl.stateestimator.parameters.input.knowledge.*;
import com.powsybl.stateestimator.parameters.input.options.StateEstimatorOptions;
import com.powsybl.stateestimator.parameters.output.estimates.BranchStatusEstimate;
import org.jgrapht.alg.util.Pair;

import java.io.IOException;
import java.util.*;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class StateEstimatorHeuristic {

    static Pair<StateEstimatorResults, StateEstimatorKnowledge> firstHeuristic(StateEstimatorKnowledge knowledgeInit, Network network) throws IOException {

        // BEGINNING OF THE HEURISTIC PROCESS

        // Step 1 : first SE run, NbTopologyChanges = 10, all branches suspected

        // Make all branches suspects and presumed to be closed
        // + Save initial status
        Map<String, String> initialStatus = new HashMap<>();
        for (Branch branch : network.getBranches()) {
            knowledgeInit.setSuspectBranch(branch.getId(), true, "PRESUMED CLOSED");
            initialStatus.put(branch.getId(), "CLOSED");
        }
        // Define the solving options for the state estimation
        // TODO : solvingMode 0 or 2 ? What max number of topo changes ? what max time solving ?
        StateEstimatorOptions optionsV1 = new StateEstimatorOptions()
                .setSolvingMode(2).setMaxTimeSolving(30).setMaxNbTopologyChanges(10);

        // Run the state estimation
        StateEstimatorResults resultsV1 = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                knowledgeInit, optionsV1, new StateEstimatorConfig(true), new LocalComputationManager());

        // Step 2 : remove measure with LNR and suspect only changed lines and neighbouring lines to changed lines.
        // maxNbTopoChanges = 1 by zone

        // Find measure with LNR in step 1
        List<Map.Entry<Integer, Double>> sortedResiduals = computeAndSortNormalizedResiduals(knowledgeInit, resultsV1);
        int nbMeasureLNR = sortedResiduals.get(0).getKey();
        double LNR = sortedResiduals.get(0).getValue();

        // Compute and print objective function value
        double objectiveFunctionValue = sortedResiduals.stream().mapToDouble(e-> Math.pow(e.getValue(),2)).sum();
        System.out.printf("Objective function value : %f %n", objectiveFunctionValue);

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

        // TODO : remove
        System.out.println("List of branches changed at first iteration");
        System.out.println(changedBranches);


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

        // TODO : check this !
        // Note : even if maxNbTopoChanges = 10 at step 1, it is possible to have more than 10 suspect regions
        // if the solver has not converged at this step.
        // We do not want more than 10 suspect regions. To ensure this, we merge the regions between them as long as needed.
        if (listOfSuspectRegions.size() > 5) {
            boolean keepMerging = true;
            int tmp = 0;
            while (keepMerging) {
                int listSize = listOfSuspectRegions.size();
                Set<String> firstMergedRegion = listOfSuspectRegions.get(tmp % listSize);
                Set<String> secondMergedRegion = listOfSuspectRegions.remove((tmp+1) % listSize);
                firstMergedRegion.addAll(secondMergedRegion);
                listOfSuspectRegions.set(tmp % listSize, firstMergedRegion);
                tmp += 2;
                if (listOfSuspectRegions.size() <= 5) {
                    keepMerging = false;
                }
            }
        }

        // Prepare iterative loop
        StateEstimatorKnowledge knowledgeV2 = knowledgeInit;
        StateEstimatorResults resultsV2 = resultsV1;

        // Initialize the set of branches that have been inspected during the cycle and whose status is fixed for this cycle
        Set<String> inspectedBranches = new HashSet<>();
        // By default, it contains all branches that do not belong to changedBranchesAndNeighbours
        for (Branch branch : network.getBranches()) {
            if (!step1changedBranchesAndNeighbours.contains(branch.getId())) {
                inspectedBranches.add(branch.getId());
            }
        }

        // TODO : remove this !
        System.out.printf("%nNumber of suspect regions : %d%n",listOfSuspectRegions.size());
        System.out.println(listOfSuspectRegions);
        System.out.println("Sorted normalized residuals :");
        System.out.println(sortedResiduals);

        // Define the maximum number of iterations
        int nbIter = 0;
        int nbIterMax = 2 + 2 * listOfSuspectRegions.size(); // At least 2 iterations + 2 cycles of "topology inspection"


        System.out.printf("%nMax number of iterations : %d %n", nbIterMax);
        System.out.printf("%nLNR : %f%n", LNR);

        boolean allowMeasureRemoval = true;
        
        // While the Largest Normalized Residual exceeds a given threshold
        while (LNR > 3 && nbIter < nbIterMax) {

            System.out.printf("%n  Iteration n°%d (max. number of iterations : %d) :%n", nbIter+2, nbIterMax);
            System.out.println();

            // Make a deep copy of knowledgeInit
            knowledgeV2 = new StateEstimatorKnowledge(knowledgeInit);

            // For current iteration, remove the measurement with LNR of the previous iteration
            // Exception 1 : if LNR < 3, do not remove the LNR measure
            // Exception 2 : if the LNR measure is a power flow and is directly related to a branch that was changed
            // at the previous iteration, do not remove it
            // ==> At most 1 measurement can be removed at each iteration
            ArrayList<String> measureLNR =  knowledgeV2.getMeasure(nbMeasureLNR);
            int tmpIndex = 1;
            while(allowMeasureRemoval && LNR > 3 && tmpIndex < sortedResiduals.size()) {
                System.out.printf("Measure n°%d (%s at %s) has ", nbMeasureLNR, measureLNR.get(0), measureLNR.get(1));
                // If LNR measure is directly related to a change of topology, do not remove it...
                if ((measureLNR.get(0).equals("Pf") | measureLNR.get(0).equals("Qf"))
                        && changedBranches.contains(measureLNR.get(1))) {
                    System.out.printf("not been removed.%n");
                    // ... and consider the measure with second LNR
                    nbMeasureLNR = sortedResiduals.get(tmpIndex).getKey();
                    LNR = sortedResiduals.get(tmpIndex).getValue();
                    measureLNR =  knowledgeV2.getMeasure(nbMeasureLNR);
                    tmpIndex++;
                } else {
                    System.out.printf(" been removed.%n");
                    knowledgeV2.removeMeasure(nbMeasureLNR);
                    break;
                }
            }

            // For current iteration, pick a new set of suspect branches ("suspectRegion")
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

            // TODO : which solving mode ?
            // Define new solving options, with MaxNbTopologyChanges = 1 this time
            StateEstimatorOptions optionsV2 = new StateEstimatorOptions()
                    .setSolvingMode(0).setMaxTimeSolving(30).setMaxNbTopologyChanges(1);

            // Run the state estimation again
            StateEstimatorResults resultsTmp = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                    knowledgeV2, optionsV2, new StateEstimatorConfig(true), new LocalComputationManager());

            // Make a shallow copy of knowledgeV2, so that next knowledge update will be based on it
            knowledgeInit = knowledgeV2;

            // Compute the Objective Function Value (OFV) obtained
            List<Map.Entry<Integer, Double>> sortedResidualsTmp = computeAndSortNormalizedResiduals(knowledgeV2, resultsTmp);
            double objectiveFunctionValueTmp = sortedResidualsTmp.stream().mapToDouble(e-> Math.pow(e.getValue(),2)).sum();
            System.out.printf("Objective function value : %f %n", objectiveFunctionValueTmp);

            // Results will be updated only if OFV has decreased
            if (objectiveFunctionValueTmp > objectiveFunctionValue) {
                System.out.println("Results of current iteration will not be taken into account : objective function value has not decreased.");
                // If OFV has not decreased, no measure will be removed at next iteration : only a new suspect region will be inspected
                allowMeasureRemoval = false;
                // As a result, there is no need to update residuals and changedBranches
            }
            else {
                // Update OFV and results
                objectiveFunctionValue = objectiveFunctionValueTmp;
                resultsV2 = resultsTmp;
                // ALlow measurement removal at next iteration
                allowMeasureRemoval = true;
                // Update normalized residuals and LNR
                sortedResiduals = computeAndSortNormalizedResiduals(knowledgeV2, resultsV2);
                nbMeasureLNR = sortedResiduals.get(0).getKey();
                LNR = sortedResiduals.get(0).getValue();
                // Update changedBranches (w.r.t initial status) with new results obtained
                changedBranches.clear();
                for (Branch branch : network.getBranches()) {
                    BranchStatusEstimate branchEstimate = resultsV2.getBranchStatusEstimate(branch.getId());
                    if (!branchEstimate.getEstimatedStatus().equals(initialStatus.get(branch.getId()))) {
                        changedBranches.add(branch.getId());
                    }
                }
                // TODO : remove
                System.out.println("List of branches changed at current iteration");
                System.out.println(changedBranches);
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

        // Indicate if the process has converged or not
        if (nbIter==nbIterMax) {
            System.out.println("Process has not converged : nbIter = nbIterMax");
        }

        // TODO : remove this
        System.out.println();
        System.out.println("Final topology changes made :");
        System.out.println(changedBranches);

        // Return the results and knowledge obtained
        return Pair.of(resultsV2, knowledgeV2);

    }

    static List<Map.Entry<Integer,Double>> computeAndSortNormalizedResiduals (StateEstimatorKnowledge knowledge, StateEstimatorResults results){

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
            double normalizedResidual = Math.abs(Double.parseDouble(val.get(7))) / Math.sqrt(Double.parseDouble(val.get(5)));
            measureNumberToNormalizedResidual.put(entry.getKey(), normalizedResidual);
        }
        // Reactive power flows
        for (var entry : resultsQf.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Math.abs(Double.parseDouble(val.get(7))) / Math.sqrt(Double.parseDouble(val.get(5)));
            measureNumberToNormalizedResidual.put(entry.getKey(), normalizedResidual);
        }
        // Active injected powers
        for (var entry : resultsP.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Math.abs(Double.parseDouble(val.get(5))) / Math.sqrt(Double.parseDouble(val.get(3)));
            measureNumberToNormalizedResidual.put(entry.getKey(), normalizedResidual);
        }
        // Reactive injected powers
        for (var entry : resultsQ.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Math.abs(Double.parseDouble(val.get(5))) / Math.sqrt(Double.parseDouble(val.get(3)));
            measureNumberToNormalizedResidual.put(entry.getKey(), normalizedResidual);
        }
        // Voltage magnitudes
        for (var entry : resultsV.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Math.abs(Double.parseDouble(val.get(5))) / Math.sqrt(Double.parseDouble(val.get(3)));
            measureNumberToNormalizedResidual.put(entry.getKey(), normalizedResidual);
        }

        // Sort normalized residuals by descending order and return the corresponding list
        return measureNumberToNormalizedResidual.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .toList();
    }


}


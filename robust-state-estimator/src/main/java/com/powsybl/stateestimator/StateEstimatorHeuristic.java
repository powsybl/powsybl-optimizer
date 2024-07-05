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
import com.powsybl.stateestimator.parameters.output.estimates.BusStateEstimate;
import org.jgrapht.alg.util.Pair;

import java.io.IOException;
import java.util.*;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class StateEstimatorHeuristic {

    public static double DECAY_INDEX_THRESHOLD = 1.15; // default value : 1.15

    public static int NB_ITER_MAX = 8; // default value : 8

    public static double RESIDUAL_REMOVAL_THRESHOLD = 10.0; // default value : 10

    static HashMap<String, Object> runHeuristic(StateEstimatorKnowledge knowledgeV1, Network network) throws IOException {

        // BEGINNING OF THE HEURISTIC PROCESS

        // Step 1 : first SE run, no topology changes, all branches are fixed

        // Fix each branch to its initial presumed status + not suspected
        for (var entry : knowledgeV1.getSuspectBranches().entrySet()) {
            String branchID = entry.getValue().get(0);
            String initialPresumedStatus = entry.getValue().get(2).equals("1") ? "PRESUMED CLOSED" : "PRESUMED OPENED";
            knowledgeV1.setSuspectBranch(branchID, false, initialPresumedStatus);
        }

        // Define the solving options for the state estimation (default values)
        StateEstimatorOptions options = new StateEstimatorOptions()
                .setSolvingMode(0).setMaxTimeSolving(30).setMipMultistart(0);

        // Run the state estimation
        StateEstimatorResults resultsV1 = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                knowledgeV1, options, new StateEstimatorConfig(true), new LocalComputationManager());

        // Find measure with LNR in step 1
        List<Map.Entry<Integer, Double>> sortedNormalizedResiduals = computeAndSortNormalizedResiduals(knowledgeV1, resultsV1);
        int nbMeasureLNR = sortedNormalizedResiduals.get(0).getKey();
        double LNR = sortedNormalizedResiduals.get(0).getValue();

        // Compute and print objective function value
        double objectiveFunctionValue = sortedNormalizedResiduals.stream().mapToDouble(e-> Math.pow(e.getValue(),2)).sum();
        System.out.println("ROOT NODE (STATE ESTIMATION n°0) :");
        System.out.printf("Objective function value divided by number of measurements : %f %n",
                objectiveFunctionValue / sortedNormalizedResiduals.size());

        // Prepare the heuristic search tree
        StateEstimatorKnowledge knowledgeV2 = knowledgeV1;
        StateEstimatorResults resultsV2 = resultsV1;
        Map<String, Pair<Double, Double>> newStartingPoint = new HashMap<>();

        // Save last estimates as new starting point
        for (BusStateEstimate busStateEstimate : resultsV1.getStateVectorEstimate()) {
            newStartingPoint.put(busStateEstimate.getBusId(),
                    Pair.of(busStateEstimate.getV(), busStateEstimate.getTheta()));
        }
        knowledgeV2.setStateVectorStartingPoint(newStartingPoint, network);

        // Define the maximum number of iterations
        int nbIter = 0;
        int nbIterMax = NB_ITER_MAX;

        // STEP 2 : fix issues iteratively (measure/topology) until no large residual remains

        // While the Largest Normalized Residual computed exceeds a given threshold
        while (LNR > 3 && nbIter < nbIterMax) {

            // Display the measure under investigation
            ArrayList<String> measureLNR = knowledgeV2.getMeasure(nbMeasureLNR);
            System.out.printf("%n%n    One measurement is under investigation (LNR) : %n");
            System.out.printf(    "    %s at %s - Normalized residual = %f %n", measureLNR.get(0),  measureLNR.get(1), LNR);

            // Determine if LNR is most likely due to a gross measurement error or a topology error:
            // decayIndexLNR corresponds to the coefficient of the power law of the sequence of decreasing residuals
            double decayIndexLNR = computeResidualsDecayIndex(nbMeasureLNR,
                    sortedNormalizedResiduals, knowledgeV2, network);

            System.out.printf(    "    Decay index : %f%n%n", decayIndexLNR);

            boolean caseA = decayIndexLNR > DECAY_INDEX_THRESHOLD;
            boolean caseB = !caseA;
            boolean caseC = false;
            boolean caseD = false;

            // Initialize variables to save results from case A if ever reaching case C (no need to run the SE again)
            boolean check1ForCaseC = false;
            StateEstimatorResults resultsTmpForCaseC = null;
            List<Map.Entry<Integer, Double>> sortedNormalizedResidualsTmpForCaseC = null;
            double objectiveFunctionValueTmpForCaseC = 0.; // value without any importance

            boolean keepInvestigating = true;

            while (keepInvestigating) {

                if (caseA) {

                    nbIter++;

                    // Gross measurement error is likely:
                    System.out.printf("%nTESTING CASE A : GROSS MEASUREMENT ERROR ? (SE n°%d/%d)%n",
                            nbIter, nbIterMax);

                    // Make a deep copy of "knowledgeV2"
                    StateEstimatorKnowledge knowledgeTmp = new StateEstimatorKnowledge(knowledgeV2);

                    // Delete LNR measure from knowledgeTmp
                    knowledgeTmp.removeMeasure(nbMeasureLNR);

                    // Make sure suspicion status for each branch is set to "false"
                    for (Branch branch : network.getBranches()) {
                        String branchID = branch.getId();
                        // Use last found status (OPENED/CLOSED) as new presumed status
                        String presumedStatus = "PRESUMED " + resultsV2.getBranchStatusEstimate(branchID).getEstimatedStatus();
                        knowledgeTmp.setSuspectBranch(branchID, false, presumedStatus);
                    }

                    options.setMaxTimeSolving(30).setMipMultistart(0);

                    // Run the SE with this knowledge
                    StateEstimatorResults resultsTmp = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                            knowledgeTmp, options, new StateEstimatorConfig(true), new LocalComputationManager());

                    // Check if the objective function value has decreased by an amount equal to the square of the LNR
                    List<Map.Entry<Integer, Double>> sortedNormalizedResidualsTmp = computeAndSortNormalizedResiduals(knowledgeTmp, resultsTmp);
                    double objectiveFunctionValueTmp = sortedNormalizedResidualsTmp.stream().mapToDouble(e -> Math.pow(e.getValue(), 2)).sum();
                    boolean check1 = objectiveFunctionValueTmp < objectiveFunctionValue - Math.pow(LNR, 2);

                    // Check n°2 : is there still a large normalized residual (not necessarily the LNR) in the suspect region ?
                    List<Double> localNormalizedResiduals = new ArrayList<>();
                    localNormalizedResiduals.add(0.);
                    Set<String> busesOfMeasureLNR = new HashSet<>();
                    // Find buses of LNR measure
                    if (measureLNR.get(0).equals("Pf") | measureLNR.get(0).equals("Qf")) {
                        busesOfMeasureLNR.add(measureLNR.get(2));
                        busesOfMeasureLNR.add(measureLNR.get(3));
                    }
                    else {
                        busesOfMeasureLNR.add(measureLNR.get(1));
                    }
                    // Find residuals directly linked to buses of LNR measure
                    for (var normalizedResidual : sortedNormalizedResidualsTmp) {
                        ArrayList<String> measure = knowledgeTmp.getMeasure(normalizedResidual.getKey());
                        if (measure.get(0).equals("Pf") | measure.get(0).equals("Qf")) {
                            if (busesOfMeasureLNR.contains(measure.get(2))
                                | busesOfMeasureLNR.contains(measure.get(3))) {
                                localNormalizedResiduals.add(normalizedResidual.getValue());
                            }
                        }
                        else {
                            if (busesOfMeasureLNR.contains(measure.get(1))) {
                                localNormalizedResiduals.add(normalizedResidual.getValue());
                            }
                        }
                    }
                    // Then find the largest residual among these local residuals, and check if it is below a certain threshold
                    boolean check2 = Collections.max(localNormalizedResiduals) < Math.min(RESIDUAL_REMOVAL_THRESHOLD, LNR);

                    if (check1 && check2) {
                        // Measurement removal was a success : save the change and move on
                        keepInvestigating = false;
                        knowledgeV2 = knowledgeTmp;
                        resultsV2 = resultsTmp;
                        sortedNormalizedResiduals = sortedNormalizedResidualsTmp;
                        nbMeasureLNR = sortedNormalizedResiduals.get(0).getKey();
                        LNR = sortedNormalizedResiduals.get(0).getValue();
                        objectiveFunctionValue = objectiveFunctionValueTmp;
                        // Save last estimates as new starting point
                        newStartingPoint = new HashMap<>();
                        for (BusStateEstimate busStateEstimate : resultsV2.getStateVectorEstimate()) {
                            newStartingPoint.put(busStateEstimate.getBusId(),
                                    Pair.of(busStateEstimate.getV(), busStateEstimate.getTheta()));
                        }
                        knowledgeV2.setStateVectorStartingPoint(newStartingPoint, network);
                        // Print some verbatim
                        System.out.println("[INFO] Current LNR due to gross measurement error : measurement was removed.");
                        // Make sure following cases are not investigated
                        caseB = false;
                    }
                    else {
                        // If deleting the measure has not worked, two cases:
                        if (caseB) {
                            // If coming from case B, switch to case D
                            caseA = false;
                            caseB = false;
                            caseD = true;
                        } else {
                            // Else, switch to case B (keep caseA = true so that caseB knows we come from caseA)
                            caseB = true;
                        }
                        // Save results for case C, so that there is no need to run SE again when reaching case C
                        check1ForCaseC = check1;
                        resultsTmpForCaseC = resultsTmp;
                        sortedNormalizedResidualsTmpForCaseC = sortedNormalizedResidualsTmp;
                        objectiveFunctionValueTmpForCaseC = objectiveFunctionValueTmp;
                    }
                }

                if (caseB) {

                    nbIter++;

                    // Topology error is likely:
                    System.out.printf("%nTESTING CASE B : TOPOLOGY ERROR ? (SE n°%d/%d)%n",
                            nbIter, nbIterMax);

                    // Find all branches and buses closely related to the LNR measurement
                    Set<String> suspectBranches = new HashSet<>();
                    Set<String> busesOfSuspectBranches = new HashSet<>();
                    // ... with two different cases, depending on whether LRN measurement is located on a branch or a bus
                    // TODO : add code lines to take into account other type of lines !
                    if (measureLNR.get(0).equals("Pf") | measureLNR.get(0).equals("Qf")) {
                        String branchID = measureLNR.get(1);
                        suspectBranches.add(branchID);
                        Bus end1 = network.getBranch(branchID).getTerminal1().getBusView().getConnectableBus();
                        Bus end2 = network.getBranch(branchID).getTerminal2().getBusView().getConnectableBus();
                        suspectBranches.addAll(end1.getLineStream().map(Identifiable::getId).toList());
                        suspectBranches.addAll(end1.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                        suspectBranches.addAll(end2.getLineStream().map(Identifiable::getId).toList());
                        suspectBranches.addAll(end2.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                        for (String suspectBranch : suspectBranches) {
                            busesOfSuspectBranches.add(network.getBranch(suspectBranch).getTerminal1().getBusView().getConnectableBus().getId());
                            busesOfSuspectBranches.add(network.getBranch(suspectBranch).getTerminal2().getBusView().getConnectableBus().getId());
                        }
                    }
                    else {
                        Bus bus = network.getBusView().getBus(measureLNR.get(1));
                        suspectBranches.addAll(bus.getLineStream().map(Identifiable::getId).toList());
                        suspectBranches.addAll(bus.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                        for (String suspectBranch : suspectBranches) {
                            busesOfSuspectBranches.add(network.getBranch(suspectBranch).getTerminal1().getBusView().getConnectableBus().getId());
                            busesOfSuspectBranches.add(network.getBranch(suspectBranch).getTerminal2().getBusView().getConnectableBus().getId());
                        }
                        for (String busTmpID : busesOfSuspectBranches) {
                            Bus busTmp = network.getBusView().getBus(busTmpID);
                            suspectBranches.addAll(busTmp.getLineStream().map(Identifiable::getId).toList());
                            suspectBranches.addAll(busTmp.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                        }
                    }

                    System.out.printf("Suspect branches (%d) : %n", suspectBranches.size());
                    System.out.println(suspectBranches);

                    // Make a copy of "knowledgeV2"
                    StateEstimatorKnowledge knowledgeTmp = new StateEstimatorKnowledge(knowledgeV2);

                    // Changed suspicion status to "true" for branches in suspectBranches and to "false" otherwise
                    for (Branch branch : network.getBranches()) {
                        String branchID = branch.getId();
                        // Use last found status (OPENED/CLOSED) as new presumed status
                        String presumedStatus = "PRESUMED " + resultsV2.getBranchStatusEstimate(branchID).getEstimatedStatus();
                        knowledgeTmp.setSuspectBranch(branchID, suspectBranches.contains(branchID), presumedStatus);
                    }

                    options.setSolvingMode(0).setMaxTimeSolving(60).setMaxNbTopologyChanges(2).setMipMultistart(1);

                    // Run the SE with this knowledge
                    StateEstimatorResults resultsTmp = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                            knowledgeTmp, options, new StateEstimatorConfig(true), new LocalComputationManager());

                    // Check n°1 : has the objective function value decreased ?
                    List<Map.Entry<Integer, Double>> sortedNormalizedResidualsTmp = computeAndSortNormalizedResiduals(knowledgeTmp, resultsTmp);
                    double objectiveFunctionValueTmp = sortedNormalizedResidualsTmp.stream().mapToDouble(e -> Math.pow(e.getValue(), 2)).sum();
                    boolean check1 = objectiveFunctionValueTmp < objectiveFunctionValue;

                    // Check n°2 : is there still a large normalized residual (not necessarily the LNR) in the suspect region ?
                    List<Double> localNormalizedResiduals = new ArrayList<>();
                    localNormalizedResiduals.add(0.);
                    // Find residuals directly linked to one of the buses of the changed branch
                    for (var normalizedResidual : sortedNormalizedResidualsTmp) {
                        ArrayList<String> measure = knowledgeTmp.getMeasure(normalizedResidual.getKey());
                        if (measure.get(0).equals("Pf") | measure.get(0).equals("Qf")) {
                            // If measured branch is in suspectBranches, add residual
                            if (suspectBranches.contains(measure.get(1))) {
                                localNormalizedResiduals.add(normalizedResidual.getValue());
                            }
                        }
                        else {
                            // If measured bus is in busesOfSuspectBranches, add residual
                            if (busesOfSuspectBranches.contains(measure.get(1))) {
                                localNormalizedResiduals.add(normalizedResidual.getValue());
                            }
                        }
                    }
                    // Then find the largest residual among these local residuals, and check if it is below a certain threshold
                    boolean check2 = Collections.max(localNormalizedResiduals) < Math.min(RESIDUAL_REMOVAL_THRESHOLD, LNR);


                    if (check1 && check2) {
                        // Topology change was a success : save it and move on
                        keepInvestigating = false;
                        knowledgeV2 = knowledgeTmp;
                        resultsV2 = resultsTmp;
                        sortedNormalizedResiduals = sortedNormalizedResidualsTmp;
                        nbMeasureLNR = sortedNormalizedResiduals.get(0).getKey();
                        LNR = sortedNormalizedResiduals.get(0).getValue();
                        objectiveFunctionValue = objectiveFunctionValueTmp;
                        // Save last estimates as new starting point
                        newStartingPoint = new HashMap<>();
                        for (BusStateEstimate busStateEstimate : resultsV2.getStateVectorEstimate()) {
                            newStartingPoint.put(busStateEstimate.getBusId(),
                                    Pair.of(busStateEstimate.getV(), busStateEstimate.getTheta()));
                        }
                        knowledgeV2.setStateVectorStartingPoint(newStartingPoint, network);
                        // Print some verbatim
                        System.out.println("[INFO] Current LNR due to topology error. Status of following branch(es) were changed:");
                        for (Branch branch : network.getBranches()) {
                            BranchStatusEstimate branchStatusEstimate = resultsV2.getBranchStatusEstimate(branch.getId());
                            if (!branchStatusEstimate.getPresumedStatus().equals(
                                    "PRESUMED " + branchStatusEstimate.getEstimatedStatus())
                            ) {
                                System.out.println(branchStatusEstimate.getBranchId());
                            }
                        }
                    }
                    // If the topology correction has not worked, two cases:
                    else {
                        if (caseA) {
                            // If coming from case A, switch to case C
                            caseA = false;
                            caseB = false;
                            caseC = true;
                        } else {
                            // Else, switch to case A (keep caseB = true)
                            caseA = true;
                        }
                    }
                }

                if (caseC) {

                    // Note : nbIter is not incremented as no SE run is made in case C

                    // Gross measurement error is likely: investigate but remove check n°2 from case A
                    System.out.printf("%nTESTING CASE C : GROSS MEASUREMENT ERROR ? (case A with check n°2 removed, no SE run) (SE n°%d/%d)%n",
                                        nbIter, nbIterMax);

                    // Make a deep copy of "knowledgeV2"
                    StateEstimatorKnowledge knowledgeTmp = new StateEstimatorKnowledge(knowledgeV2);

                    // Delete LNR measure from knowledgeTmp
                    knowledgeTmp.removeMeasure(nbMeasureLNR);

                    // Make sure suspicion status for each branch is set to "false"
                    for (Branch branch : network.getBranches()) {
                        String branchID = branch.getId();
                        // Use last found status (OPENED/CLOSED) as new presumed status
                        String presumedStatus = "PRESUMED " + resultsV2.getBranchStatusEstimate(branchID).getEstimatedStatus();
                        knowledgeTmp.setSuspectBranch(branchID, false, presumedStatus);
                    }

                    if (check1ForCaseC) {
                        // Measurement removal was a success : save the change and move on
                        keepInvestigating = false;
                        knowledgeV2 = knowledgeTmp;
                        resultsV2 = resultsTmpForCaseC;
                        sortedNormalizedResiduals = sortedNormalizedResidualsTmpForCaseC;
                        nbMeasureLNR = sortedNormalizedResiduals.get(0).getKey();
                        LNR = sortedNormalizedResiduals.get(0).getValue();
                        objectiveFunctionValue = objectiveFunctionValueTmpForCaseC;
                        // Save last estimates as new starting point
                        newStartingPoint = new HashMap<>();
                        for (BusStateEstimate busStateEstimate : resultsV2.getStateVectorEstimate()) {
                            newStartingPoint.put(busStateEstimate.getBusId(),
                                    Pair.of(busStateEstimate.getV(), busStateEstimate.getTheta()));
                        }
                        knowledgeV2.setStateVectorStartingPoint(newStartingPoint, network);
                        // Print some verbatim
                        System.out.println("[INFO] Current LNR due to gross measurement error : measurement was removed.");
                        // Make sure following cases are not investigated
                        caseD = false;
                    }
                    else {
                        // If the measurement removal has not worked, two cases:
                        if (caseD) {
                            // All options have been tested without finding a way to correct the error detected : end of the process
                            System.out.println("[WARNING] Heuristic process has failed to correct detected error : last found estimates returned.");
                            HashMap<String, Object> functionReturns = new HashMap<>();
                            functionReturns.put("Results", resultsV2);
                            functionReturns.put("Knowledge", knowledgeV2);
                            functionReturns.put("NbIter", nbIter);
                            return functionReturns;
                        }
                        else {
                            // Switch to case D (keep caseC = true)
                            caseD = true;
                        }

                    }
                }

                if (caseD) {

                    nbIter++;

                    // Topology error is likely: investigate an extended set of suspect branches (w.r.t. what is defined in case B)
                    System.out.printf("%nTESTING CASE D : TOPOLOGY ERROR ? (case B with suspect region extended) (SE n°%d/%d)%n",
                            nbIter, nbIterMax);

                    // Find all branches and buses closely related to the LNR measurement
                    Set<String> suspectBranches = new HashSet<>();
                    Set<String> busesOfSuspectBranches = new HashSet<>();
                    // ... with two different cases, depending on whether LRN measurement is located on a branch or a bus
                    // TODO : add code lines to take into account other type of lines !
                    if (measureLNR.get(0).equals("Pf") | measureLNR.get(0).equals("Qf")) {
                        String branchID = measureLNR.get(1);
                        suspectBranches.add(branchID);
                        Bus end1 = network.getBranch(branchID).getTerminal1().getBusView().getConnectableBus();
                        Bus end2 = network.getBranch(branchID).getTerminal2().getBusView().getConnectableBus();
                        suspectBranches.addAll(end1.getLineStream().map(Identifiable::getId).toList());
                        suspectBranches.addAll(end1.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                        suspectBranches.addAll(end2.getLineStream().map(Identifiable::getId).toList());
                        suspectBranches.addAll(end2.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                        for (String suspectBranch : suspectBranches) {
                            busesOfSuspectBranches.add(network.getBranch(suspectBranch).getTerminal1().getBusView().getConnectableBus().getId());
                            busesOfSuspectBranches.add(network.getBranch(suspectBranch).getTerminal2().getBusView().getConnectableBus().getId());
                        }
                    }
                    else {
                        Bus bus = network.getBusView().getBus(measureLNR.get(1));
                        suspectBranches.addAll(bus.getLineStream().map(Identifiable::getId).toList());
                        suspectBranches.addAll(bus.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                        for (String suspectBranch : suspectBranches) {
                            busesOfSuspectBranches.add(network.getBranch(suspectBranch).getTerminal1().getBusView().getConnectableBus().getId());
                            busesOfSuspectBranches.add(network.getBranch(suspectBranch).getTerminal2().getBusView().getConnectableBus().getId());
                        }
                        for (String busTmpID : busesOfSuspectBranches) {
                            Bus busTmp = network.getBusView().getBus(busTmpID);
                            suspectBranches.addAll(busTmp.getLineStream().map(Identifiable::getId).toList());
                            suspectBranches.addAll(busTmp.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                        }
                    }
                    // Extend suspect region
                    Set<String> extendedSuspectBranches = new HashSet<>();
                    for (String suspectBranchID : suspectBranches) {
                        Branch suspectBranch = network.getBranch(suspectBranchID);
                        extendedSuspectBranches.addAll(suspectBranch.getTerminal1().getBusView().getConnectableBus()
                                .getLineStream().map(Identifiable::getId).toList());
                        extendedSuspectBranches.addAll(suspectBranch.getTerminal1().getBusView().getConnectableBus()
                                .getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                        extendedSuspectBranches.addAll(suspectBranch.getTerminal2().getBusView().getConnectableBus()
                                .getLineStream().map(Identifiable::getId).toList());
                        extendedSuspectBranches.addAll(suspectBranch.getTerminal2().getBusView().getConnectableBus()
                                .getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                    }

                    System.out.printf("Suspect branches - Extended (%d) : %n", extendedSuspectBranches.size());
                    System.out.println(extendedSuspectBranches);

                    // Make a copy of "knowledgeV2"
                    StateEstimatorKnowledge knowledgeTmp = new StateEstimatorKnowledge(knowledgeV2);

                    // Changed suspicion status to "true" for branches in extendedSuspectBranches and to "false" otherwise
                    for (Branch branch : network.getBranches()) {
                        String branchID = branch.getId();
                        // Use last found status (OPENED/CLOSED) as new presumed status
                        String presumedStatus = "PRESUMED " + resultsV2.getBranchStatusEstimate(branchID).getEstimatedStatus();
                        knowledgeTmp.setSuspectBranch(branchID, extendedSuspectBranches.contains(branchID), presumedStatus);
                    }

                    options.setMaxNbTopologyChanges(2).setSolvingMode(0).setMaxTimeSolving(60).setMipMultistart(1);

                    // Run the SE with this knowledge
                    StateEstimatorResults resultsTmp = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                            knowledgeTmp, options, new StateEstimatorConfig(true), new LocalComputationManager());

                    // Check n°1 : has the objective function value decreased ?
                    List<Map.Entry<Integer, Double>> sortedNormalizedResidualsTmp = computeAndSortNormalizedResiduals(knowledgeTmp, resultsTmp);
                    double objectiveFunctionValueTmp = sortedNormalizedResidualsTmp.stream().mapToDouble(e -> Math.pow(e.getValue(), 2)).sum();
                    boolean check1 = objectiveFunctionValueTmp < objectiveFunctionValue;

                    // Check n°2 : is there still a large normalized residual (not necessarily the LNR) in the region defined by suspectBranches ?
                    List<Double> localNormalizedResiduals = new ArrayList<>();
                    localNormalizedResiduals.add(0.);
                    // Find residuals directly linked to suspectBranches
                    for (var normalizedResidual : sortedNormalizedResidualsTmp) {
                        ArrayList<String> measure = knowledgeTmp.getMeasure(normalizedResidual.getKey());
                        if (measure.get(0).equals("Pf") | measure.get(0).equals("Qf")) {
                            // If measured branch is in suspectBranches, add residual
                            if (suspectBranches.contains(measure.get(1))) {
                                localNormalizedResiduals.add(normalizedResidual.getValue());
                            }
                        }
                        else {
                            // If measured bus is in busesOfSuspectBranches, add residual
                            if (busesOfSuspectBranches.contains(measure.get(1))) {
                                localNormalizedResiduals.add(normalizedResidual.getValue());
                            }
                        }
                    }
                    // Then find the largest residual among these local residuals, and check if it is below a certain threshold
                    boolean check2 = Collections.max(localNormalizedResiduals) < Math.min(RESIDUAL_REMOVAL_THRESHOLD, LNR);

                    if (check1 && check2) {
                        // Topology change was a success : save it and move on
                        keepInvestigating = false;
                        knowledgeV2 = knowledgeTmp;
                        resultsV2 = resultsTmp;
                        sortedNormalizedResiduals = sortedNormalizedResidualsTmp;
                        nbMeasureLNR = sortedNormalizedResiduals.get(0).getKey();
                        LNR = sortedNormalizedResiduals.get(0).getValue();
                        objectiveFunctionValue = objectiveFunctionValueTmp;
                        // Save last estimates as new starting point
                        newStartingPoint = new HashMap<>();
                        for (BusStateEstimate busStateEstimate : resultsV2.getStateVectorEstimate()) {
                            newStartingPoint.put(busStateEstimate.getBusId(),
                                    Pair.of(busStateEstimate.getV(), busStateEstimate.getTheta()));
                        }
                        knowledgeV2.setStateVectorStartingPoint(newStartingPoint, network);
                        // Print some verbatim
                        System.out.println("[INFO] Current LNR due to topology error. Status of following branch(es) were changed:");
                        for (Branch branch : network.getBranches()) {
                            BranchStatusEstimate branchStatusEstimate = resultsV2.getBranchStatusEstimate(branch.getId());
                            if (!branchStatusEstimate.getPresumedStatus().equals(
                                    "PRESUMED " + branchStatusEstimate.getEstimatedStatus())
                            ) {
                                System.out.println(branchStatusEstimate.getBranchId());
                            }
                        }
                    }
                    // If the topology correction has not worked, two cases:
                    else {
                        if (caseC) {
                            // All options have been tested without finding a way to correct the error detected : end of the process
                            System.out.println("[WARNING] Heuristic process is out of solutions to correct errors detected and has ended : last found estimates returned.");
                            HashMap<String, Object> functionReturns = new HashMap<String,Object>();
                            functionReturns.put("Results", resultsV2);
                            functionReturns.put("Knowledge", knowledgeV2);
                            functionReturns.put("NbIter", nbIter);
                            return functionReturns;
                        }
                        else {
                            // Else, switch to case C (keep caseD = true)
                            caseC = true;
                        }
                    }
                }

            }

            System.out.printf("%nObjective function value divided by number of measurements : %f %n",
                    objectiveFunctionValue / sortedNormalizedResiduals.size());
        }


        // END OF THE HEURISTIC PROCESS

        // Indicate if the process has converged or not
        if (nbIter>=nbIterMax && LNR > 3) {
            System.out.println("[WARNING] Heuristic process has not converged (nbIter = nbIterMax)");
        }

        else{
            System.out.println("[INFO] The heuristic process has converged.");
        }

        // Return the results and knowledge obtained, and the number of iterations needed (take initial resolution into account)
        HashMap<String, Object> functionReturns = new HashMap<String,Object>();
        functionReturns.put("Results", resultsV2);
        functionReturns.put("Knowledge", knowledgeV2);
        functionReturns.put("NbIter", nbIter + 1);
        return functionReturns;
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

    static double computeResidualsDecayIndex(Integer measurementNumber, List<Map.Entry<Integer, Double>> normalizedResidualsList, StateEstimatorKnowledge knowledge, Network network) {

        // Goal : Determine if LNR is most likely due to a gross measurement error or a topology error

        ArrayList<String> measureLNR =  knowledge.getMeasure(measurementNumber);
        String typeLNR = measureLNR.get(0);

        // Make a map <MeasurementNumber, NormalizedResidual> out of the list of residuals given as input
        Map<Integer, Double> normalizedResiduals = new HashMap<>();
        for (Map.Entry<Integer, Double> residual : normalizedResidualsList) {
            normalizedResiduals.put(residual.getKey(), residual.getValue());
        }

        ArrayList<Double> residualsAround = new ArrayList<>();

        Set<String> relatedBuses = new HashSet<>();

        // Find all neighbouring buses to the location of the LNR measure
        // If LNR measure is on a branch :
        if (typeLNR.equals("Pf") | typeLNR.equals("Qf")) {
            // Find the buses related to LNR measure
            Bus bus1 = network.getBusView().getBus(measureLNR.get(2));
            Bus bus2 = network.getBusView().getBus(measureLNR.get(3));
            relatedBuses.add(bus1.getId());
            relatedBuses.add(bus2.getId());
            for (Line line : bus1.getLines()) {
                relatedBuses.add(line.getTerminal1().getBusView().getConnectableBus().getId());
                relatedBuses.add(line.getTerminal2().getBusView().getConnectableBus().getId());
            }
            for (TwoWindingsTransformer twt : bus1.getTwoWindingsTransformers()) {
                relatedBuses.add(twt.getTerminal1().getBusView().getConnectableBus().getId());
                relatedBuses.add(twt.getTerminal2().getBusView().getConnectableBus().getId());
            }
            for (Line line : bus2.getLines()) {
                relatedBuses.add(line.getTerminal1().getBusView().getConnectableBus().getId());
                relatedBuses.add(line.getTerminal2().getBusView().getConnectableBus().getId());
            }
            for (TwoWindingsTransformer twt : bus2.getTwoWindingsTransformers()) {
                relatedBuses.add(twt.getTerminal1().getBusView().getConnectableBus().getId());
                relatedBuses.add(twt.getTerminal2().getBusView().getConnectableBus().getId());
            }
        } else {
            // Find the bus related to LNR measure
            Bus bus = network.getBusView().getBus(measureLNR.get(1));
            relatedBuses.add(bus.getId());
            for (Line line : bus.getLines()) {
                relatedBuses.add(line.getTerminal1().getBusView().getConnectableBus().getId());
                relatedBuses.add(line.getTerminal2().getBusView().getConnectableBus().getId());
            }
            for (TwoWindingsTransformer twt : bus.getTwoWindingsTransformers()) {
                relatedBuses.add(twt.getTerminal1().getBusView().getConnectableBus().getId());
                relatedBuses.add(twt.getTerminal2().getBusView().getConnectableBus().getId());
            }
        }

        // Find residuals of measurements linked to related buses
        for (var measure : knowledge.getActivePowerFlowMeasures().entrySet()) {
            if (relatedBuses.contains(measure.getValue().get(2))
                    | relatedBuses.contains(measure.getValue().get(3))) {
                residualsAround.add(normalizedResiduals.get(measure.getKey()));
            }
        }
        for (var measure : knowledge.getReactivePowerFlowMeasures().entrySet()) {
            if (relatedBuses.contains(measure.getValue().get(2))
                    | relatedBuses.contains(measure.getValue().get(3))) {
                residualsAround.add(normalizedResiduals.get(measure.getKey()));
            }
        }
        for (var measure : knowledge.getActivePowerInjectedMeasures().entrySet()) {
            if (relatedBuses.contains(measure.getValue().get(1))) {
                residualsAround.add(normalizedResiduals.get(measure.getKey()));
            }
        }
        for (var measure : knowledge.getReactivePowerInjectedMeasures().entrySet()) {
            if (relatedBuses.contains(measure.getValue().get(1))) {
                residualsAround.add(normalizedResiduals.get(measure.getKey()));
            }
        }
        for (var measure : knowledge.getVoltageMagnitudeMeasures().entrySet()) {
            if (relatedBuses.contains(measure.getValue().get(1))) {
                residualsAround.add(normalizedResiduals.get(measure.getKey()));
            }
        }

        // Once all related residuals are found, sort them in decreasing order and keep the first X values
        // TODO : check maxSize and filter used
        List<Double> sortedResiduals = residualsAround.stream()
                .sorted(Collections.reverseOrder()).limit(5)
                //.filter(res -> res>=3)
                .toList();

        // If only 1 residual remains, return a value leading to measurement removal (easiest solution)
        if (sortedResiduals.size() <= 1) {
            return DECAY_INDEX_THRESHOLD + 1;
        }

        double maxResidual = sortedResiduals.get(0);

        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < sortedResiduals.size(); i++) {
            numerator += Math.log(i+1) * Math.log(sortedResiduals.get(i)/maxResidual);
            denominator += Math.pow(Math.log(i+1), 2);
        }

        return - numerator / denominator;

    }

}


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
import com.powsybl.stateestimator.parameters.output.estimates.BusStateEstimate;
import org.jgrapht.alg.util.Pair;

import java.io.IOException;
import java.util.*;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class StateEstimatorSecondHeuristic {

    static Pair<StateEstimatorResults, StateEstimatorKnowledge> secondHeuristic(StateEstimatorKnowledge knowledgeV1, Network network) throws IOException {

        // BEGINNING OF THE HEURISTIC PROCESS

        // Step 1 : first SE run, no topology changes, all branches are fixed

        // TODO : Use initial presumed status, not "closed" status by default ==> OK
        // Fix each branch to its initial presumed status + not suspected
        for (var entry : knowledgeV1.getSuspectBranches().entrySet()) {
            String branchID = entry.getValue().get(0);
            String initialPresumedStatus = entry.getValue().get(2).equals("1") ? "PRESUMED CLOSED" : "PRESUMED OPENED";
            knowledgeV1.setSuspectBranch(branchID, false, initialPresumedStatus);
        }

        // Define the solving options for the state estimation
        StateEstimatorOptions options = new StateEstimatorOptions()
                .setSolvingMode(0).setMaxTimeSolving(30).setMaxNbTopologyChanges(0);

        // Run the state estimation
        StateEstimatorResults resultsV1 = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                knowledgeV1, options, new StateEstimatorConfig(true), new LocalComputationManager());

        // Find measure with LNR in step 1
        List<Map.Entry<Integer, Double>> sortedNormalizedResiduals = computeAndSortNormalizedResiduals(knowledgeV1, resultsV1);
        int nbMeasureLNR = sortedNormalizedResiduals.get(0).getKey();
        double LNR = sortedNormalizedResiduals.get(0).getValue();

        // Compute and print objective function value
        double objectiveFunctionValue = sortedNormalizedResiduals.stream().mapToDouble(e-> Math.pow(e.getValue(),2)).sum();
        System.out.println("   OPENING STEP :");
        System.out.printf("%nObjective function value : %f %n", objectiveFunctionValue);

        // Prepare the heuristic search tree
        StateEstimatorKnowledge knowledgeV2 = knowledgeV1;
        StateEstimatorResults resultsV2 = resultsV1;
        Set<String> branchesNotToBeTestedAgain = new HashSet<>();
        Map<String, Pair<Double, Double>> newStartingPoint = new HashMap<>();

        // TODO : use of new starting point at first iteration ??
        // Save last estimates as new starting point
        for (BusStateEstimate busStateEstimate : resultsV1.getStateVectorEstimate()) {
            newStartingPoint.put(busStateEstimate.getBusId(),
                    Pair.of(busStateEstimate.getV(), busStateEstimate.getTheta()));
        }
        knowledgeV2.setStateVectorStartingPoint(newStartingPoint, network);

        // Define the maximum number of iterations
        int nbIter = 0;
        int nbIterMax = 5;

        // While the Largest Normalized Residual computed exceeds a given threshold
        while (LNR > 3 && nbIter < nbIterMax) {

            boolean checkTopologyError = false;
            boolean checkMeasurementError = false;

            System.out.printf("%n%n  ITERATION N°%d (max. number of iterations : %d) :%n", nbIter+1, nbIterMax);
            System.out.println();

            // Display the measure under investigation
            ArrayList<String> measureLNR = knowledgeV2.getMeasure(nbMeasureLNR);
            System.out.println("Measurement under investigation (LNR) : ");
            System.out.printf("%s at %s - Normalized residual = %f %n", measureLNR.get(0),  measureLNR.get(1), LNR);

            // Determine if LNR is most likely due to a gross measurement error or a topology error
            // ==> Observe distribution of residuals around the location of the LNR measure
            //double harmonicMeanAroundLNR = computeHarmonicMeanOfResidualsAround(nbMeasureLNR,
            //        sortedNormalizedResiduals, knowledgeV2, network);

            // decayIndexLNR corresponds to the coefficient of the power law of the sequence of decreasing residuals
            double decayIndexLNR = computeResidualsDecayIndex(nbMeasureLNR,
                    sortedNormalizedResiduals, knowledgeV2, network);

            //System.out.printf("%nHarmonic mean of surrounding residuals : %f%n", harmonicMeanAroundLNR);
            System.out.printf("%nDecay index : %f%n", decayIndexLNR);

            boolean keepInvestigating = true;

            while (keepInvestigating) {

                if (decayIndexLNR > 1.15 | checkMeasurementError) {
                    System.out.println("TEST : RETRAIT DE MESURE");
                    // Gross measurement error is likely:
                    // Make a deep copy of "knowledgeV2"
                    StateEstimatorKnowledge knowledgeTmp = new StateEstimatorKnowledge(knowledgeV2);
                    // Delete LNR measure
                    knowledgeTmp.removeMeasure(nbMeasureLNR);
                    // Run the SE with this knowledge
                    StateEstimatorResults resultsTmp = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                            knowledgeTmp, options, new StateEstimatorConfig(true), new LocalComputationManager());
                    // Check if the objective function value has decreased by an amount equal to the square of the LNR
                    List<Map.Entry<Integer, Double>> sortedNormalizedResidualsTmp = computeAndSortNormalizedResiduals(knowledgeTmp, resultsTmp);
                    double objectiveFunctionValueTmp = sortedNormalizedResidualsTmp.stream().mapToDouble(e -> Math.pow(e.getValue(), 2)).sum();
                    // TODO : add another check ?
                    if (objectiveFunctionValueTmp < objectiveFunctionValue - Math.pow(LNR, 2)) {
                        // Measurement removal was a success : save the change and move on
                        keepInvestigating = false;
                        checkMeasurementError = false;
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
                        System.out.println("Current LNR due to gross measurement error : measurement was removed.");
                        // TODO : improve this
                        // Change value (false) of decay index so that topology investigation will not be launched after
                        decayIndexLNR = 1000.;
                    }
                    else {
                        if (checkMeasurementError) {
                            // The end of the heuristic procedure has been reached without finding a way to reduce the OFV
                            System.out.println("[WARNING] Heuristic process has not converged.");
                            return Pair.of(resultsV2, knowledgeV2);
                        } else {
                            checkTopologyError = true;
                        }
                    }
                }

                if (decayIndexLNR <= 1.15 | checkTopologyError) {
                    System.out.println("TEST : CHANGEMENT DE TOPOLOGIE");
                    // Topology error is likely:
                    // Find all branches closely related to the LNR measurement
                    Set<String> suspectBranches = new HashSet<>();
                    if (measureLNR.get(0).equals("Pf") | measureLNR.get(0).equals("Qf")) {
                        String branchID = measureLNR.get(1);
                        suspectBranches.add(branchID);
                        Bus end1 = network.getBranch(branchID).getTerminal1().getBusView().getConnectableBus();
                        Bus end2 = network.getBranch(branchID).getTerminal2().getBusView().getConnectableBus();
                        suspectBranches.addAll(end1.getLineStream().map(Identifiable::getId).toList());
                        suspectBranches.addAll(end1.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                        suspectBranches.addAll(end2.getLineStream().map(Identifiable::getId).toList());
                        suspectBranches.addAll(end2.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                    } else {
                        Bus bus = network.getBusView().getBus(measureLNR.get(1));
                        suspectBranches.addAll(bus.getLineStream().map(Identifiable::getId).toList());
                        suspectBranches.addAll(bus.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                        Set<String> busesOfSuspectBranches = new HashSet<>();
                        for (String suspectBranch : suspectBranches) {
                            busesOfSuspectBranches.add(network.getBranch(suspectBranch).getTerminal1().getBusView().getConnectableBus().getId());
                            busesOfSuspectBranches.add(network.getBranch(suspectBranch).getTerminal2().getBusView().getConnectableBus().getId());
                        }
                        for (String busTmpID : busesOfSuspectBranches) {
                            Bus busTmp = network.getBusView().getBus(busTmpID);
                            suspectBranches.addAll(busTmp.getLineStream().map(Identifiable::getId).toList());
                            suspectBranches.addAll(busTmp.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                        }
                        // TODO : add code lines to take into account other type of lines !!!
                    }

                    // Remove branches already tested
                    suspectBranches.removeAll(branchesNotToBeTestedAgain);

                    // Initialize a boolean indicating if one of the topology changes has worked
                    boolean isTopologyInspectionSuccessful = false;
                    // Make a priority queue, to first pick branches that are the most suspicious (order based on decreasing maxResidual, then decreasing meanResidual)
                    List<String> orderedSuspectBranches = orderByDegreeOfDistrust(suspectBranches, sortedNormalizedResiduals, knowledgeV2, network);

                    // TODO : try only 5 first branches, then change mode of inspection (mode measurement error) ???
                    orderedSuspectBranches = orderedSuspectBranches.stream().limit(5).toList();

                    // Pick a suspect branch in the given order
                    for (String changedBranch : orderedSuspectBranches) {
                        System.out.println(changedBranch);
                        // Add the branch to the set of branches not to be inspected again
                        branchesNotToBeTestedAgain.add(changedBranch);
                        // Make a copy of "knowledgeV2"
                        StateEstimatorKnowledge knowledgeTmp = new StateEstimatorKnowledge(knowledgeV2);
                        // Change the status of the suspect branch
                        String newPresumedStatus = "PRESUMED ";
                        if (resultsV2.getBranchStatusEstimate(changedBranch).getEstimatedStatus().equals("CLOSED")) {
                            newPresumedStatus += "OPENED";
                        } else {
                            newPresumedStatus += "CLOSED";
                        }
                        knowledgeTmp.setSuspectBranch(changedBranch, false, newPresumedStatus);
                        // Run the SE with this knowledge
                        StateEstimatorResults resultsTmp = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                                knowledgeTmp, options, new StateEstimatorConfig(true), new LocalComputationManager());

                        // Check n°1 : has the objective function value decreased ?
                        List<Map.Entry<Integer, Double>> sortedNormalizedResidualsTmp = computeAndSortNormalizedResiduals(knowledgeTmp, resultsTmp);
                        double objectiveFunctionValueTmp = sortedNormalizedResidualsTmp.stream().mapToDouble(e -> Math.pow(e.getValue(), 2)).sum();
                        boolean check1 = objectiveFunctionValueTmp < objectiveFunctionValue;

                        // Check n°2 : is there still a large normalized residual (not necessarily the LNR) close to the changed branch ?
                        List<Double> localNormalizedResiduals = new ArrayList<>();
                        localNormalizedResiduals.add(0.);
                        Set<String> localBuses = new HashSet<>();
                        localBuses.add(network.getBranch(changedBranch).getTerminal1().getBusView().getConnectableBus().getId());
                        localBuses.add(network.getBranch(changedBranch).getTerminal2().getBusView().getConnectableBus().getId());
                        // Find residuals directly linked to one of the buses of the changed branch
                        for (var normalizedResidual : sortedNormalizedResidualsTmp) {
                            ArrayList<String> measure = knowledgeTmp.getMeasure(normalizedResidual.getKey());
                            if (measure.get(0).equals("Pf") | measure.get(0).equals("Qf")) {
                                if (localBuses.contains(measure.get(2)) | localBuses.contains(measure.get(3))) {
                                    localNormalizedResiduals.add(normalizedResidual.getValue());
                                }
                            }
                            else {
                                if (localBuses.contains(measure.get(1))) {
                                    localNormalizedResiduals.add(normalizedResidual.getValue());
                                }
                            }
                        }
                        // Then find the largest residual among these local residuals, and check if it is below a certain threshold
                        // TODO : choose value for the threshold : 10 ? Leaves space for the possibility of another error at intermediate distance
                        boolean check2 = Collections.max(localNormalizedResiduals) < 10;


                        if (check1 && check2) {
                            // Topology change was a success : save it and move on
                            keepInvestigating = false;
                            checkTopologyError = false;
                            isTopologyInspectionSuccessful = true;
                            knowledgeV2 = knowledgeTmp;
                            resultsV2 = resultsTmp;
                            sortedNormalizedResiduals = sortedNormalizedResidualsTmp;
                            nbMeasureLNR = sortedNormalizedResiduals.get(0).getKey();
                            LNR = sortedNormalizedResiduals.get(0).getValue();
                            objectiveFunctionValue = objectiveFunctionValueTmp;
                            // TODO : use of new starting point
                            // Save last estimates as new starting point
                            newStartingPoint = new HashMap<>();
                            for (BusStateEstimate busStateEstimate : resultsV2.getStateVectorEstimate()) {
                                newStartingPoint.put(busStateEstimate.getBusId(),
                                        Pair.of(busStateEstimate.getV(), busStateEstimate.getTheta()));
                            }
                            knowledgeV2.setStateVectorStartingPoint(newStartingPoint, network);
                            System.out.printf("%nCurrent LNR due to topology error : status of branch %s was changed.%n", changedBranch);
                            break;
                        }
                    }
                    // If the topology inspection has not worked, two cases
                    if (!isTopologyInspectionSuccessful) {
                        if (checkTopologyError) {
                            // The end of the heuristic procedure has been reached without finding a way to reduce the OFV
                            System.out.println("[WARNING] Heuristic process has not converged.");
                            return Pair.of(resultsV2, knowledgeV2);
                        } else {
                            checkMeasurementError = true;
                        }
                    }
                }
            }

            System.out.printf("%nObjective function value : %f%n", objectiveFunctionValue);

            nbIter ++;
        }

        // END OF THE ITERATIVE PROCESS

        // Indicate if the process has converged or not
        if (nbIter==nbIterMax && LNR > 3) {
            System.out.println("[WARNING] Process has not converged (nbIter = nbIterMax)");
        }

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

    static double computeResidualsDecayIndex(Integer measurementNumber, List<Map.Entry<Integer, Double>> normalizedResidualsList, StateEstimatorKnowledge knowledge, Network network) {

        // TODO : adjust decay index computation so that it works with any Z/N ratio (especially, Z/N=4) !
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

        // Residuals considered for the harmonic mean depend on whether measurement is at a node or a branch

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

        // Once all related residuals are found, sort them in decreasing order and keep the first 30 values
        List<Double> sortedResiduals = residualsAround.stream()
                .sorted(Collections.reverseOrder()).limit(30)
                .filter(res -> res>=1)
                .toList();

        // If not enough valuable residuals found, return a value leading to measurement removal (easiest solution)
        if (sortedResiduals.size() <= 1) {
            return 10;
        }

        double maxResidual = sortedResiduals.get(0);

        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < sortedResiduals.size(); i++) {
            numerator += Math.log(sortedResiduals.get(i) / maxResidual);
            denominator += Math.log(i+1);
        }

        return - numerator / denominator;

    }

    static List<String> orderByDegreeOfDistrust(Set<String> suspectBranches, List<Map.Entry<Integer, Double>> normalizedResidualsList, StateEstimatorKnowledge knowledge, Network network) {

        Map<String, ArrayList<Double>> listOfResiduals = new HashMap<>();
        Map<String, Set<String>> endsOfBranches = new HashMap<>();
        for (String suspectBranch : suspectBranches) {
            listOfResiduals.put(suspectBranch, new ArrayList<>());
            Set<String> endsOfBranch = new HashSet<>();
            endsOfBranch.add(network.getBranch(suspectBranch).getTerminal1().getBusView().getConnectableBus().getId());
            endsOfBranch.add(network.getBranch(suspectBranch).getTerminal2().getBusView().getConnectableBus().getId());
            endsOfBranches.put(suspectBranch, endsOfBranch);
        }

        // Find all residuals related to each suspect branch
        for (Map.Entry<Integer, Double> residual : normalizedResidualsList) {
            ArrayList<String> measure = knowledge.getMeasure(residual.getKey());
            if (measure.get(0).equals("Pf") | measure.get(0).equals("Qf")) {
                String bus1 = measure.get(2);
                String bus2 = measure.get(3);
                for (String suspectBranch : suspectBranches) {
                    if (endsOfBranches.get(suspectBranch).contains(bus1)
                        | endsOfBranches.get(suspectBranch).contains(bus2)) {
                        ArrayList<Double> tmp = listOfResiduals.get(suspectBranch);
                        tmp.add(residual.getValue());
                        listOfResiduals.put(suspectBranch, tmp);
                    }
                }
            } else {
                String bus1 = network.getBusView().getBus(measure.get(1)).getId();
                for (String suspectBranch : suspectBranches) {
                    if (endsOfBranches.get(suspectBranch).contains(bus1)) {
                        ArrayList<Double> tmp = listOfResiduals.get(suspectBranch);
                        tmp.add(residual.getValue());
                        listOfResiduals.put(suspectBranch, tmp);
                    }
                }
            }
        }

        // TODO : remove
        System.out.println("List of residuals for distrust degree computation");
        System.out.println(listOfResiduals);

        // Compute two degrees of distrust for each branch : <maximum residual>, <mean of residuals>
        Map<String, Pair<Double, Double>> degreesOfDistrust = new HashMap<>();
        for (String suspectBranch : suspectBranches) {
            double maxResidual = Collections.max(listOfResiduals.get(suspectBranch));
            double meanResidual = listOfResiduals.get(suspectBranch).stream().mapToDouble(a->a).sum() / Math.max(listOfResiduals.get(suspectBranch).size(), 1);
            degreesOfDistrust.put(suspectBranch, Pair.of(maxResidual, meanResidual));
        }

        // Build comparators
        final Comparator<Map.Entry<String, Pair<Double, Double>>> byMaxResidual =
                Comparator.comparing(e->e.getValue().getFirst(), Comparator.reverseOrder());
        final Comparator<Map.Entry<String, Pair<Double, Double>>> byMeanResidual =
                Comparator.comparing(e->e.getValue().getSecond(), Comparator.reverseOrder());

        // Sort branches by decreasing order of maxResidual, then by decreasing order of meanResidual
        List<String> orderedSuspectBranchesAsList = degreesOfDistrust.entrySet().stream()
                .sorted(byMaxResidual.thenComparing(byMeanResidual))
                .map(Map.Entry::getKey).toList();


        System.out.println("Degrees of distrust for suspect branches (ORDERED) : ");
        System.out.println(orderedSuspectBranchesAsList);

        return orderedSuspectBranchesAsList;
    }


}


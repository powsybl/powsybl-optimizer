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
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static com.powsybl.openloadflow.OpenLoadFlowParameters.LowImpedanceBranchMode.REPLACE_BY_MIN_IMPEDANCE_LINE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class FirstHeuristic {

    @Test
    void firstHeuristic() throws IOException {

        // TODO : voir s'il ne faut pas faire un run initial avec les y fixé + un run avec les y libres, et en tirer des infos

        // Load your favorite network (IIDM format preferred)
        Network network = IeeeCdfNetworkFactory.create118();

        String erroneousLine = "L62-67-1";

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

        // Reconnect the erroneous line
        network.getLine(erroneousLine).connect();

        int seed = 120;
        double ratioTested = 5.0;

        // Create "knowledge" instance
        StateEstimatorKnowledge knowledgeV1 = new StateEstimatorKnowledge(network);

        // For IEEE 118 bus, slack is "VL69_0": our state estimator must use the same slack
        knowledgeV1.setSlack("VL69_0", network); // for IEEE118

        // Add a gross error on measure Pf(VL27 --> VL28) : 80 MW (false) instead of 32.6 MW (true)
        Map<String, String> grossMeasure = Map.of("BranchID","L27-28-1","FirstBusID","VL27_0","SecondBusID","VL28_0",
                "Value","80.0","Variance","0.1306","Type","Pf");
        knowledgeV1.addMeasure(1, grossMeasure, network);

        // Randomly generate measurements (useful for test cases) out of load flow results
        RandomMeasuresGenerator.generateRandomMeasurements(knowledgeV1, network,
                Optional.of(seed), Optional.of(ratioTested),
                Optional.of(false), Optional.of(true),
                Optional.empty(), Optional.empty());


        // BEGINNING OF THE HEURISTIC PROCESS

        // Step 1 : first SE run, NbTopologyChanges = 5, all branches suspected

        // Make all branches suspects and presumed to be closed
        for (Branch branch : network.getBranches()) {
            knowledgeV1.setSuspectBranch(branch.getId(), true, "PRESUMED CLOSED");
        }
        // Define the solving options for the state estimation
        StateEstimatorOptions optionsV1 = new StateEstimatorOptions()
                .setSolvingMode(2).setMaxTimeSolving(30).setMaxNbTopologyChanges(10);

        // Run the state estimation
        StateEstimatorResults resultsV1 = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                knowledgeV1, optionsV1, new StateEstimatorConfig(true), new LocalComputationManager());

        resultsV1.printNetworkTopology();
        //resultsV1.printAllMeasurementEstimatesAndResidualsSi(knowledgeV1);

        StateEstimatorEvaluator evaluatorV1 = new StateEstimatorEvaluator(network, knowledgeV1, resultsV1);

        // Print some indicators on the accuracy of the state estimation w.r.t load flow solution
        List<Double> voltageErrorStatsV1 = evaluatorV1.computeVoltageRelativeErrorStats();
        List<Double> angleErrorStatsV1 = evaluatorV1.computeAngleDegreeErrorStats();
        List<Double> activePowerFlowErrorStatsV1 = evaluatorV1.computeActivePowerFlowsRelativeErrorsStats();
        List<Double> reactivePowerFlowErrorStatsV1 = evaluatorV1.computeReactivePowerFlowsRelativeErrorsStats();
        System.out.printf("%nMean voltage relative error : %f %% %n", voltageErrorStatsV1.get(0));
        System.out.printf("%nMean angle absolute error : %f degrees %n", angleErrorStatsV1.get(0));
        System.out.printf("%nMean active power flow relative error : %f %% %n", activePowerFlowErrorStatsV1.get(0));
        System.out.printf("%nMean reactive power flow relative error : %f %% %n", reactivePowerFlowErrorStatsV1.get(0));

        // Step 2 : remove measure with LNR and suspect only neighbouring lines to changed lines. maxNbTopoChanges = 1

        // Find measure with LNR in step 1
        List<Number> resultsLNR = computeLNRandSSR(knowledgeV1, resultsV1, network);
        int nbMeasureLNR = (Integer) resultsLNR.get(0);
        double LNR = (Double) resultsLNR.get(1);

        // Build the list of branches changed during step 1, with and without their neighbours
        List<String> changedBranches = new ArrayList<>();
        Set<String> changedBranchesAndNeighbours = new HashSet<>(); // to avoid duplicates
        for (Branch branch : network.getBranches()) {
            BranchStatusEstimate branchEstimate = resultsV1.getBranchStatusEstimate(branch.getId());
            if (!branchEstimate.getPresumedStatus().equals("PRESUMED " + branchEstimate.getEstimatedStatus())) {
                // Add the ID of the changed branch to both lists
                changedBranches.add(branch.getId());
                changedBranchesAndNeighbours.add(branch.getId());
                // Add the IDs of neighbouring branches to the second list
                Bus end1 = network.getBranch(branch.getId()).getTerminal1().getBusView().getBus();
                Bus end2 = network.getBranch(branch.getId()).getTerminal2().getBusView().getBus();
                changedBranchesAndNeighbours.addAll(end1.getLineStream().map(Identifiable::getId).toList());
                changedBranchesAndNeighbours.addAll(end1.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                changedBranchesAndNeighbours.addAll(end2.getLineStream().map(Identifiable::getId).toList());
                changedBranchesAndNeighbours.addAll(end2.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
            }
        }

        // Prepare iterative loop
        StateEstimatorKnowledge knowledgeV2 = knowledgeV1;
        StateEstimatorResults resultsV2 = resultsV1;

        int nbIter = 0;
        int nbIterMax = 5;

        while ((LNR > 3 | changedBranches.size() > 1) && nbIter < nbIterMax) {

            System.out.println(nbIter);
            System.out.println(LNR);
            System.out.println(changedBranches);
            System.out.println(changedBranchesAndNeighbours);

            // Make a deep copy of knowledgeV1
            knowledgeV2 = new StateEstimatorKnowledge(knowledgeV1);

            // For second iteration, remove the measurement of the first iteration with LNR
            // Exception : if the measure is a power flow and is directly related to a changed branch, do not remove it
            ArrayList<String> measureLNR =  knowledgeV2.getMeasure(nbMeasureLNR);
            System.out.println(measureLNR);
            if ((measureLNR.get(0).equals("Pf") | measureLNR.get(0).equals("Qf"))
                    && changedBranches.contains(measureLNR.get(1))) {
                // Do nothing
                System.out.println("Was not removed");
            } else {
                knowledgeV2.removeMeasure(nbMeasureLNR);
                System.out.println("Was removed");
            }
            //knowledgeV2.removeMeasure(nbMeasureLNR);

            // For second iteration, suspect only branches in changedBranchesAndNeighbours
            // Give previous estimated status as new presumed status
            for (Branch branch : network.getBranches()) {
                if (changedBranchesAndNeighbours.contains(branch.getId())) {
                    knowledgeV2.setSuspectBranch(branch.getId(), true, "PRESUMED "
                            + resultsV2.getBranchStatusEstimate(branch.getId()).getEstimatedStatus());
                } else {
                    knowledgeV2.setSuspectBranch(branch.getId(), false, "PRESUMED "
                            + resultsV2.getBranchStatusEstimate(branch.getId()).getEstimatedStatus());
                }
            }

            // Define new solving options, with MaxNbTopologyChanges = 1
            StateEstimatorOptions optionsV2 = new StateEstimatorOptions()
                    .setSolvingMode(2).setMaxTimeSolving(30).setMaxNbTopologyChanges(1);

            // Run the state estimation a second time
            resultsV2 = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                    knowledgeV2, optionsV2, new StateEstimatorConfig(true), new LocalComputationManager());

            // Make a shallow copy of knowledgeV2, so that next knowledge update will be based on it
            knowledgeV1 = knowledgeV2;

            // Update LNR
            resultsLNR = computeLNRandSSR(knowledgeV2, resultsV2, network);
            nbMeasureLNR = (Integer) resultsLNR.get(0);
            LNR = (Double) resultsLNR.get(1);

            // Update changedBranches and changedBranchesAndNeighbours
            changedBranches.clear();
            changedBranchesAndNeighbours.clear();
            for (Branch branch : network.getBranches()) {
                BranchStatusEstimate branchEstimate = resultsV2.getBranchStatusEstimate(branch.getId());
                if (!branchEstimate.getPresumedStatus().equals("PRESUMED " + branchEstimate.getEstimatedStatus())) {
                    // Add the ID of the changed branch to both lists
                    changedBranches.add(branch.getId());
                    changedBranchesAndNeighbours.add(branch.getId());
                    // Add the IDs of neighbouring branches to the second list
                    Bus end1 = network.getBranch(branch.getId()).getTerminal1().getBusView().getBus();
                    Bus end2 = network.getBranch(branch.getId()).getTerminal2().getBusView().getBus();
                    changedBranchesAndNeighbours.addAll(end1.getLineStream().map(Identifiable::getId).toList());
                    changedBranchesAndNeighbours.addAll(end1.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                    changedBranchesAndNeighbours.addAll(end2.getLineStream().map(Identifiable::getId).toList());
                    changedBranchesAndNeighbours.addAll(end2.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                }
            }

            nbIter++;
            }

        // Print results at the end of the iterative process
        System.out.println("[END OF THE ITERATIVE PROCESS]");
        resultsV2.printNetworkTopology();
        resultsV2.printAllMeasurementEstimatesAndResidualsSi(knowledgeV2);
        StateEstimatorEvaluator evaluatorV2 = new StateEstimatorEvaluator(network, knowledgeV2, resultsV2);
        // Print some indicators on the accuracy of the state estimation w.r.t load flow solution
        List<Double> voltageErrorStatsV2 = evaluatorV2.computeVoltageRelativeErrorStats();
        List<Double> angleErrorStatsV2 = evaluatorV2.computeAngleDegreeErrorStats();
        List<Double> activePowerFlowErrorStatsV2 = evaluatorV2.computeActivePowerFlowsRelativeErrorsStats();
        List<Double> reactivePowerFlowErrorStatsV2 = evaluatorV2.computeReactivePowerFlowsRelativeErrorsStats();
        System.out.printf("%nMean voltage relative error : %f %% %n", voltageErrorStatsV2.get(0));
        System.out.printf("%nMean angle absolute error : %f degrees %n", angleErrorStatsV2.get(0));
        System.out.printf("%nMean active power flow relative error : %f %% %n", activePowerFlowErrorStatsV2.get(0));
        System.out.printf("%nMean reactive power flow relative error : %f %% %n", reactivePowerFlowErrorStatsV2.get(0));

        ArrayList<String> measureLNR =  knowledgeV2.getMeasure(nbMeasureLNR);
        System.out.println(measureLNR);
        System.out.println(LNR);
    }

    double computeSSR (StateEstimatorKnowledge knowledge, StateEstimatorResults results){

        // Store measurement residuals given by the SE results
        ActivePowerFlowMeasures resultsPf = new ActivePowerFlowMeasures(knowledge.getActivePowerFlowMeasures(), results.measurementEstimatesAndResiduals);
        ReactivePowerFlowMeasures resultsQf = new ReactivePowerFlowMeasures(knowledge.getReactivePowerFlowMeasures(), results.measurementEstimatesAndResiduals);
        ActivePowerInjectedMeasures resultsP = new ActivePowerInjectedMeasures(knowledge.getActivePowerInjectedMeasures(), results.measurementEstimatesAndResiduals);
        ReactivePowerInjectedMeasures resultsQ = new ReactivePowerInjectedMeasures(knowledge.getReactivePowerInjectedMeasures(), results.measurementEstimatesAndResiduals);
        VoltageMagnitudeMeasures resultsV = new VoltageMagnitudeMeasures(knowledge.getVoltageMagnitudeMeasures(), results.measurementEstimatesAndResiduals);

        // Compute the value of the Sum of Squared Residuals (SNR)
        double SSR = 0;

        // Active power flows
        for (var entry : resultsPf.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(7)) / Math.sqrt(Double.parseDouble(val.get(5)));
            SSR += Math.pow(normalizedResidual, 2);
        }
        // Reactive power flows
        for (var entry : resultsQf.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(7)) / Math.sqrt(Double.parseDouble(val.get(5)));
            SSR += Math.pow(normalizedResidual, 2);
        }
        // Active injected powers
        for (var entry : resultsP.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(5)) / Math.sqrt(Double.parseDouble(val.get(3)));
            SSR += Math.pow(normalizedResidual, 2);
        }
        // Reactive injected powers
        for (var entry : resultsQ.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(5)) / Math.sqrt(Double.parseDouble(val.get(3)));
            SSR += Math.pow(normalizedResidual, 2);
        }
        // Voltage magnitudes
        for (var entry : resultsV.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(5)) / Math.sqrt(Double.parseDouble(val.get(3)));
            SSR += Math.pow(normalizedResidual, 2);
        }
        return (SSR);
    }

    List<Number> computeLNRandSSR (StateEstimatorKnowledge knowledge, StateEstimatorResults results, Network
    network){

        // Store measurement residuals given by the SE results
        ActivePowerFlowMeasures resultsPf = new ActivePowerFlowMeasures(knowledge.getActivePowerFlowMeasures(), results.measurementEstimatesAndResiduals);
        ReactivePowerFlowMeasures resultsQf = new ReactivePowerFlowMeasures(knowledge.getReactivePowerFlowMeasures(), results.measurementEstimatesAndResiduals);
        ActivePowerInjectedMeasures resultsP = new ActivePowerInjectedMeasures(knowledge.getActivePowerInjectedMeasures(), results.measurementEstimatesAndResiduals);
        ReactivePowerInjectedMeasures resultsQ = new ReactivePowerInjectedMeasures(knowledge.getReactivePowerInjectedMeasures(), results.measurementEstimatesAndResiduals);
        VoltageMagnitudeMeasures resultsV = new VoltageMagnitudeMeasures(knowledge.getVoltageMagnitudeMeasures(), results.measurementEstimatesAndResiduals);

        // Compute the normalized residuals (NR = residual / sqrt(variance))
        // ... while finding the largest normalized residual (LNR), the measurement related to it, the lines related to the location of the measurement
        double LNR = 0;
        int measureNumberLNR = -1;
        ArrayList<String> linesRelatedToLNR = new ArrayList<>();
        // ... while computing the value of the Sum of Squared Residuals (SNR)
        double SSR = 0;

        // Active power flows
        for (var entry : resultsPf.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(7)) / Math.sqrt(Double.parseDouble(val.get(5)));
            val.add(String.valueOf(normalizedResidual));
            entry.setValue(val);
            SSR += Math.pow(normalizedResidual, 2);
            if (normalizedResidual > LNR) {
                LNR = normalizedResidual;
                // Store measurement number
                measureNumberLNR = entry.getKey();
                // Store the lines close to where the measure was taken
                Bus bus1 = network.getBranch(val.get(1)).getTerminal1().getBusView().getBus();
                Bus bus2 = network.getBranch(val.get(1)).getTerminal2().getBusView().getBus();
                linesRelatedToLNR.clear();
                linesRelatedToLNR.addAll(bus1.getLineStream().map(Identifiable::getId).toList());
                linesRelatedToLNR.addAll(bus1.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                linesRelatedToLNR.addAll(bus2.getLineStream().map(Identifiable::getId).toList());
                linesRelatedToLNR.addAll(bus2.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
            }
        }
        // Reactive power flows
        for (var entry : resultsQf.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(7)) / Math.sqrt(Double.parseDouble(val.get(5)));
            val.add(String.valueOf(normalizedResidual));
            entry.setValue(val);
            SSR += Math.pow(normalizedResidual, 2);
            if (normalizedResidual > LNR) {
                LNR = normalizedResidual;
                // Store measurement number
                measureNumberLNR = entry.getKey();
                // Store the lines close to where the measure was taken
                Bus bus1 = network.getBranch(val.get(1)).getTerminal1().getBusView().getBus();
                Bus bus2 = network.getBranch(val.get(1)).getTerminal2().getBusView().getBus();
                linesRelatedToLNR.clear();
                linesRelatedToLNR.addAll(bus1.getLineStream().map(Identifiable::getId).toList());
                linesRelatedToLNR.addAll(bus1.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
                linesRelatedToLNR.addAll(bus2.getLineStream().map(Identifiable::getId).toList());
                linesRelatedToLNR.addAll(bus2.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
            }
        }
        // Active injected powers
        for (var entry : resultsP.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(5)) / Math.sqrt(Double.parseDouble(val.get(3)));
            val.add(String.valueOf(normalizedResidual));
            entry.setValue(val);
            SSR += Math.pow(normalizedResidual, 2);
            if (normalizedResidual > LNR) {
                LNR = normalizedResidual;
                // Store measurement number
                measureNumberLNR = entry.getKey();
                // Store the lines close to where the measure was taken
                Bus bus = network.getBusView().getBus(val.get(1));
                linesRelatedToLNR.clear();
                linesRelatedToLNR.addAll(bus.getLineStream().map(Identifiable::getId).toList());
                linesRelatedToLNR.addAll(bus.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
            }
        }
        // Reactive injected powers
        for (var entry : resultsQ.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(5)) / Math.sqrt(Double.parseDouble(val.get(3)));
            val.add(String.valueOf(normalizedResidual));
            entry.setValue(val);
            SSR += Math.pow(normalizedResidual, 2);
            if (normalizedResidual > LNR) {
                LNR = normalizedResidual;
                // Store measurement number
                measureNumberLNR = entry.getKey();
                // Store the lines close to where the measure was taken
                Bus bus = network.getBusView().getBus(val.get(1));
                linesRelatedToLNR.clear();
                linesRelatedToLNR.addAll(bus.getLineStream().map(Identifiable::getId).toList());
                linesRelatedToLNR.addAll(bus.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
            }
        }
        // Voltage magnitudes
        for (var entry : resultsV.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> val = entry.getValue();
            double normalizedResidual = Double.parseDouble(val.get(5)) / Math.sqrt(Double.parseDouble(val.get(3)));
            val.add(String.valueOf(normalizedResidual));
            entry.setValue(val);
            SSR += Math.pow(normalizedResidual, 2);
            if (normalizedResidual > LNR) {
                LNR = normalizedResidual;
                // Store measurement number
                measureNumberLNR = entry.getKey();
                // Store the lines close to where the measure was taken
                Bus bus = network.getBusView().getBus(val.get(1));
                linesRelatedToLNR.clear();
                linesRelatedToLNR.addAll(bus.getLineStream().map(Identifiable::getId).toList());
                linesRelatedToLNR.addAll(bus.getTwoWindingsTransformerStream().map(Identifiable::getId).toList());
            }
        }
        // TODO : add linesRelaredToLNR in return object
        return List.of(measureNumberLNR, LNR, SSR);
    }

}


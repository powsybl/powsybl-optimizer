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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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
public class DecayIndexTest {

    @Test
    void grossErrorTest() throws IOException {

        // Test : IEEE118, seed n°0
        // Variables: Z/N=4, 5 ou 6. Erreur de mesure = +10, +20, +40 sigma
        // Test every measurement.
        // Add noise = true. Ensure observability = true.

        // Initialize the dataframe that will store the results
        List<String> headers = List.of(
                "Seed", "RatioMeasuresToBuses", "MeasureID",
                "MeasureType", "GrossErrorMagnitude(nbStds)",
                "GrossErrorMagnitude(SI)", "DecayIndex"
        );
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

        int seed = 0;

        double ratioTested = 6.0;

        double grossErrorMagnitude = 20;

        // Test errors on all possible voltage magnitudes
        for (Bus bus : network.getBusView().getBuses()) {

            // Define the solving options
            StateEstimatorOptions options = new StateEstimatorOptions().setMaxTimeSolving(30);

            StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network, "VL69_0");

            // Add a gross error on measure at hand
            double variance = Math.pow(RandomMeasuresGenerator.RELATIVE_STD_BY_MEAS_TYPE.get("V")
                            * Math.max(Math.abs(bus.getV()), RandomMeasuresGenerator.MIN_VOLTAGE_KV)
                    , 2);
            double error = grossErrorMagnitude * Math.sqrt(variance);
            Map<String, String> grossMeasure = Map.of(
                    "Type", "V",
                    "BusID", bus.getId(),
                    "Variance", String.valueOf(variance),
                    "Value", String.valueOf(bus.getV() + error)
                    );
            knowledge.addMeasure(1, grossMeasure, network);

            // Create string containing all info on gross measurement
            String grossMeasureID = "V_" + bus.getId();

            // Randomly generate measurements out of load flow results
            RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network,
                    Optional.of(seed), Optional.of(ratioTested),
                    Optional.of(true), Optional.empty(),
                    Optional.empty(), Optional.of(true));

            // Run SE
            StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                    knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager());

            // Compute normalized residuals
            List<Map.Entry<Integer, Double>> normalizedResiduals = StateEstimatorHeuristic.computeAndSortNormalizedResiduals(knowledge, results);

            // Compute decay index for gross error
            double decayIndex = StateEstimatorHeuristic.computeResidualsDecayIndex(1,
                    normalizedResiduals, knowledge, network);

            data.add(List.of(
                    String.valueOf(seed), String.valueOf(ratioTested),
                    grossMeasureID, "V",
                    String.valueOf(grossErrorMagnitude), String.valueOf(error),
                    String.valueOf(decayIndex)
            ));
        }

        // Test errors on all possible active power injections
        for (Bus bus : network.getBusView().getBuses()) {

            // Define the solving options
            StateEstimatorOptions options = new StateEstimatorOptions().setMaxTimeSolving(30);

            StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network, "VL69_0");

            if (!knowledge.getZeroInjectionBuses().containsValue(bus.getId())) {

                // Add a gross error on measure at hand
                double variance = Math.pow(RandomMeasuresGenerator.RELATIVE_STD_BY_MEAS_TYPE.get("P")
                                * Math.max(Math.abs(bus.getP()), RandomMeasuresGenerator.MIN_ACTIVE_POWER_MW)
                        , 2);
                double error = grossErrorMagnitude * Math.sqrt(variance);
                Map<String, String> grossMeasure = Map.of(
                        "Type", "P",
                        "BusID", bus.getId(),
                        "Variance", String.valueOf(variance),
                        "Value", String.valueOf(-bus.getP()>0 ? -bus.getP() + error : -bus.getP() - error)
                );
                knowledge.addMeasure(1, grossMeasure, network);

                // Create string containing all info on gross measurement
                String grossMeasureID = "P_" + bus.getId();

                // Randomly generate measurements out of load flow results
                RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network,
                        Optional.of(seed), Optional.of(ratioTested),
                        Optional.of(true), Optional.empty(),
                        Optional.empty(), Optional.of(true));

                // Run SE
                StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                        knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager());

                // Compute normalized residuals
                List<Map.Entry<Integer, Double>> normalizedResiduals = StateEstimatorHeuristic.computeAndSortNormalizedResiduals(knowledge, results);

                // Compute decay index for gross error
                double decayIndex = StateEstimatorHeuristic.computeResidualsDecayIndex(1,
                        normalizedResiduals, knowledge, network);

                data.add(List.of(
                        String.valueOf(seed), String.valueOf(ratioTested),
                        grossMeasureID, "P",
                        String.valueOf(grossErrorMagnitude), String.valueOf(error),
                        String.valueOf(decayIndex)
                ));
            }
        }

        // Test errors on all possible reactive power injections
        for (Bus bus : network.getBusView().getBuses()) {

            // Define the solving options
            StateEstimatorOptions options = new StateEstimatorOptions().setMaxTimeSolving(30);

            StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network, "VL69_0");

            if (!knowledge.getZeroInjectionBuses().containsValue(bus.getId())) {

                // Add a gross error on measure at hand
                double variance = Math.pow(RandomMeasuresGenerator.RELATIVE_STD_BY_MEAS_TYPE.get("Q")
                                * Math.max(Math.abs(bus.getQ()), RandomMeasuresGenerator.MIN_REACTIVE_POWER_MVAR)
                        , 2);
                double error = grossErrorMagnitude * Math.sqrt(variance);
                Map<String, String> grossMeasure = Map.of(
                        "Type", "Q",
                        "BusID", bus.getId(),
                        "Variance", String.valueOf(variance),
                        "Value", String.valueOf(-bus.getQ()>0 ? -bus.getQ() + error : -bus.getQ() - error)
                );
                knowledge.addMeasure(1, grossMeasure, network);

                // Create string containing all info on gross measurement
                String grossMeasureID = "Q_" + bus.getId();

                // Randomly generate measurements out of load flow results
                RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network,
                        Optional.of(seed), Optional.of(ratioTested),
                        Optional.of(true), Optional.empty(),
                        Optional.empty(), Optional.of(true));

                // Run SE
                StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                        knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager());

                // Compute normalized residuals
                List<Map.Entry<Integer, Double>> normalizedResiduals = StateEstimatorHeuristic.computeAndSortNormalizedResiduals(knowledge, results);

                // Compute decay index for gross error
                double decayIndex = StateEstimatorHeuristic.computeResidualsDecayIndex(1,
                        normalizedResiduals, knowledge, network);

                data.add(List.of(
                        String.valueOf(seed), String.valueOf(ratioTested),
                        grossMeasureID, "Q",
                        String.valueOf(grossErrorMagnitude), String.valueOf(error),
                        String.valueOf(decayIndex)
                ));
            }
        }

        // Test errors on all possible active power flows (from side 1)
        for (Branch branch : network.getBranches()) {

            // Define the solving options
            StateEstimatorOptions options = new StateEstimatorOptions().setMaxTimeSolving(30);

            StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network, "VL69_0");

            // Add a gross error on measure at hand
            double variance = Math.pow(RandomMeasuresGenerator.RELATIVE_STD_BY_MEAS_TYPE.get("Pf")
                            * Math.max(Math.abs(branch.getTerminal1().getP()), RandomMeasuresGenerator.MIN_ACTIVE_POWER_MW)
                    , 2);
            double error = grossErrorMagnitude * Math.sqrt(variance);
            Map<String, String> grossMeasure = Map.of(
                    "Type", "Pf",
                    "BranchID", branch.getId(),
                    "FirstBusID", branch.getTerminal1().getBusView().getBus().getId(),
                    "SecondBusID", branch.getTerminal2().getBusView().getBus().getId(),
                    "Variance", String.valueOf(variance),
                    "Value", String.valueOf(branch.getTerminal1().getP()>0 ? branch.getTerminal1().getP() + error : branch.getTerminal1().getP() - error)
            );
            knowledge.addMeasure(1, grossMeasure, network);

            // Create string containing all info on gross measurement
            String grossMeasureID = "Pf_" + branch.getId() + "_" + branch.getTerminal1().getBusView().getBus().getId();

            // Randomly generate measurements out of load flow results
            RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network,
                    Optional.of(seed), Optional.of(ratioTested),
                    Optional.of(true), Optional.empty(),
                    Optional.empty(), Optional.of(true));

            // Run SE
            StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                    knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager());

            // Compute normalized residuals
            List<Map.Entry<Integer, Double>> normalizedResiduals = StateEstimatorHeuristic.computeAndSortNormalizedResiduals(knowledge, results);

            // Compute decay index for gross error
            double decayIndex = StateEstimatorHeuristic.computeResidualsDecayIndex(1,
                    normalizedResiduals, knowledge, network);

            data.add(List.of(
                    String.valueOf(seed), String.valueOf(ratioTested),
                    grossMeasureID, "Pf",
                    String.valueOf(grossErrorMagnitude), String.valueOf(error),
                    String.valueOf(decayIndex)
            ));
        }

        // Test errors on all possible active power flows (from side 2)
        for (Branch branch : network.getBranches()) {

            // Define the solving options
            StateEstimatorOptions options = new StateEstimatorOptions().setMaxTimeSolving(30);

            StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network, "VL69_0");

            // Add a gross error on measure at hand
            double variance = Math.pow(RandomMeasuresGenerator.RELATIVE_STD_BY_MEAS_TYPE.get("Pf")
                            * Math.max(Math.abs(branch.getTerminal2().getP()), RandomMeasuresGenerator.MIN_ACTIVE_POWER_MW)
                    , 2);
            double error = grossErrorMagnitude * Math.sqrt(variance);
            Map<String, String> grossMeasure = Map.of(
                    "Type", "Pf",
                    "BranchID", branch.getId(),
                    "FirstBusID", branch.getTerminal2().getBusView().getBus().getId(),
                    "SecondBusID", branch.getTerminal1().getBusView().getBus().getId(),
                    "Variance", String.valueOf(variance),
                    "Value", String.valueOf(branch.getTerminal2().getP()>0 ? branch.getTerminal2().getP() + error : branch.getTerminal2().getP() - error)
            );
            knowledge.addMeasure(1, grossMeasure, network);

            // Create string containing all info on gross measurement
            String grossMeasureID = "Pf_" + branch.getId() + "_" + branch.getTerminal2().getBusView().getBus().getId();

            // Randomly generate measurements out of load flow results
            RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network,
                    Optional.of(seed), Optional.of(ratioTested),
                    Optional.of(true), Optional.empty(),
                    Optional.empty(), Optional.of(true));

            // Run SE
            StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                    knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager());

            // Compute normalized residuals
            List<Map.Entry<Integer, Double>> normalizedResiduals = StateEstimatorHeuristic.computeAndSortNormalizedResiduals(knowledge, results);

            // Compute decay index for gross error
            double decayIndex = StateEstimatorHeuristic.computeResidualsDecayIndex(1,
                    normalizedResiduals, knowledge, network);

            data.add(List.of(
                    String.valueOf(seed), String.valueOf(ratioTested),
                    grossMeasureID, "Pf",
                    String.valueOf(grossErrorMagnitude), String.valueOf(error),
                    String.valueOf(decayIndex)
            ));
        }

        // Test errors on all possible reactive power flows (from side 1)
        for (Branch branch : network.getBranches()) {

            // Define the solving options
            StateEstimatorOptions options = new StateEstimatorOptions().setMaxTimeSolving(30);

            StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network, "VL69_0");

            // Add a gross error on measure at hand
            double variance = Math.pow(RandomMeasuresGenerator.RELATIVE_STD_BY_MEAS_TYPE.get("Qf")
                            * Math.max(Math.abs(branch.getTerminal1().getQ()), RandomMeasuresGenerator.MIN_REACTIVE_POWER_MVAR)
                    , 2);
            double error = grossErrorMagnitude * Math.sqrt(variance);
            Map<String, String> grossMeasure = Map.of(
                    "Type", "Qf",
                    "BranchID", branch.getId(),
                    "FirstBusID", branch.getTerminal1().getBusView().getBus().getId(),
                    "SecondBusID", branch.getTerminal2().getBusView().getBus().getId(),
                    "Variance", String.valueOf(variance),
                    "Value", String.valueOf(branch.getTerminal1().getQ()>0 ? branch.getTerminal1().getQ() + error : branch.getTerminal1().getQ() - error)
            );
            knowledge.addMeasure(1, grossMeasure, network);

            // Create string containing all info on gross measurement
            String grossMeasureID = "Qf_" + branch.getId() + "_" + branch.getTerminal1().getBusView().getBus().getId();

            // Randomly generate measurements out of load flow results
            RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network,
                    Optional.of(seed), Optional.of(ratioTested),
                    Optional.of(true), Optional.empty(),
                    Optional.empty(), Optional.of(true));

            // Run SE
            StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                    knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager());

            // Compute normalized residuals
            List<Map.Entry<Integer, Double>> normalizedResiduals = StateEstimatorHeuristic.computeAndSortNormalizedResiduals(knowledge, results);

            // Compute decay index for gross error
            double decayIndex = StateEstimatorHeuristic.computeResidualsDecayIndex(1,
                    normalizedResiduals, knowledge, network);

            data.add(List.of(
                    String.valueOf(seed), String.valueOf(ratioTested),
                    grossMeasureID, "Qf",
                    String.valueOf(grossErrorMagnitude), String.valueOf(error),
                    String.valueOf(decayIndex)
            ));
        }

        // Test errors on all possible reactive power flows (from side 2)
        for (Branch branch : network.getBranches()) {

            // Define the solving options
            StateEstimatorOptions options = new StateEstimatorOptions().setMaxTimeSolving(30);

            StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network, "VL69_0");

            // Add a gross error on measure at hand
            double variance = Math.pow(RandomMeasuresGenerator.RELATIVE_STD_BY_MEAS_TYPE.get("Qf")
                            * Math.max(Math.abs(branch.getTerminal2().getQ()), RandomMeasuresGenerator.MIN_REACTIVE_POWER_MVAR)
                    , 2);
            double error = grossErrorMagnitude * Math.sqrt(variance);
            Map<String, String> grossMeasure = Map.of(
                    "Type", "Qf",
                    "BranchID", branch.getId(),
                    "FirstBusID", branch.getTerminal2().getBusView().getBus().getId(),
                    "SecondBusID", branch.getTerminal1().getBusView().getBus().getId(),
                    "Variance", String.valueOf(variance),
                    "Value", String.valueOf(branch.getTerminal2().getQ()>0 ? branch.getTerminal2().getQ() + error : branch.getTerminal2().getQ() - error)
            );
            knowledge.addMeasure(1, grossMeasure, network);

            // Create string containing all info on gross measurement
            String grossMeasureID = "Qf_" + branch.getId() + "_" + branch.getTerminal2().getBusView().getBus().getId();

            // Randomly generate measurements out of load flow results
            RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network,
                    Optional.of(seed), Optional.of(ratioTested),
                    Optional.of(true), Optional.empty(),
                    Optional.empty(), Optional.of(true));

            // Run SE
            StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                    knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager());

            // Compute normalized residuals
            List<Map.Entry<Integer, Double>> normalizedResiduals = StateEstimatorHeuristic.computeAndSortNormalizedResiduals(knowledge, results);

            // Compute decay index for gross error
            double decayIndex = StateEstimatorHeuristic.computeResidualsDecayIndex(1,
                    normalizedResiduals, knowledge, network);

            data.add(List.of(
                    String.valueOf(seed), String.valueOf(ratioTested),
                    grossMeasureID, "Qf",
                    String.valueOf(grossErrorMagnitude), String.valueOf(error),
                    String.valueOf(decayIndex)
            ));
        }

        // Export the results in a CSV file
        try (FileWriter fileWriter = new FileWriter("V2_ZN6_20sigma_seed0_AllMeasErr_WithNoise_EnsObs_DECAYINDEX.csv");
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

    @Test
    void topoErrorTest() throws IOException {

        // Test : IEEE118, seed n°0
        // Variables: Z/N=4, 5 ou 6.
        // Test every topology error possible.
        // Add noise = true. Ensure observability = true.
        // Decay Index is computed for the V measure of the second bus of the line.

        // Initialize the dataframe that will store the results
        List<String> headers = List.of(
                "Seed", "RatioMeasuresToBuses", "BranchID",
                "TopoErrorType", "SecondBusID", "DecayIndexForVoltageMeasure"
        );
        List<List<String>> data = new ArrayList<>();

        int seed = 0;

        double ratioTested = 4.0;

        // A) Test topology error type : "is presumed open but is in fact closed"

        Network network = IeeeCdfNetworkFactory.create118();

        // Load Flow parameters
        LoadFlowParameters parametersLf = new LoadFlowParameters();
        OpenLoadFlowParameters parametersExt = OpenLoadFlowParameters.create(parametersLf);
        parametersExt.setAlwaysUpdateNetwork(true)
                .setLowImpedanceBranchMode(REPLACE_BY_MIN_IMPEDANCE_LINE)
                .setLowImpedanceThreshold(1e-4);

        // Solve the Load Flow problem for the network (only once for case A)
        LoadFlowResult loadFlowResult = LoadFlow.run(network, parametersLf);
        assertTrue(loadFlowResult.isFullyConverged());

        for (Line line : network.getLines()) {

            Bus relatedBus = line.getTerminal2().getBusView().getConnectableBus();

            // Define the solving options
            StateEstimatorOptions options = new StateEstimatorOptions().setMaxTimeSolving(30);

            StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network, "VL69_0");

            // Add V measure on selected line
            double variance = Math.pow(RandomMeasuresGenerator.RELATIVE_STD_BY_MEAS_TYPE.get("V")
                            * Math.max(Math.abs(relatedBus.getV()), RandomMeasuresGenerator.MIN_VOLTAGE_KV)
                    , 2);
            Map<String, String> measure = Map.of(
                    "Type", "V",
                    "BusID", relatedBus.getId(),
                    "Variance", String.valueOf(variance),
                    "Value", String.valueOf(relatedBus.getV())
            );
            knowledge.addMeasure(1, measure, network);

            // Randomly generate measurements out of load flow results
            RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network,
                    Optional.of(seed), Optional.of(ratioTested),
                    Optional.of(true), Optional.empty(),
                    Optional.of(line.getId()), Optional.of(true));

            // Introduce topology error
            knowledge.setSuspectBranch(line.getId(), false, "PRESUMED OPENED");

            // Run SE
            StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                    knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager());

            // Cancel topology error
            knowledge.setSuspectBranch(line.getId(), false, "PRESUMED CLOSED");

            // Compute normalized residuals
            List<Map.Entry<Integer, Double>> normalizedResiduals = StateEstimatorHeuristic.computeAndSortNormalizedResiduals(knowledge, results);

            // Compute decay index for V measure (second bus) of selected line
            double decayIndex = StateEstimatorHeuristic.computeResidualsDecayIndex(1,
                    normalizedResiduals, knowledge, network);

            data.add(List.of(
                    String.valueOf(seed), String.valueOf(ratioTested),
                    line.getId(), "PRESUMED_OPENED",
                    relatedBus.getId(), String.valueOf(decayIndex)
            ));
        }


        // B) Test topology error type : "is presumed closed but is in fact open"

        network = IeeeCdfNetworkFactory.create118();

        Set<String> allTestLines = new HashSet<>();
        for (Line line : network.getLines()) {
            allTestLines.add(line.getId());
        }
        List.of("L8-9-1","L9-10-1","L68-116-1","L71-73-1","L110-112-1",
                "L85-86-1","L86-87-1","L110-111-1","L12-117-1")
                .forEach(allTestLines::remove);

        for (String lineID : allTestLines) {

            System.out.printf("%n%nLine tested : %s %n", lineID);

            // Load Flow parameters (note : we mimic the way the AMPL code deals with zero-impedance branches)
            parametersLf = new LoadFlowParameters();
            parametersExt = OpenLoadFlowParameters.create(parametersLf);
            parametersExt.setAlwaysUpdateNetwork(true)
                    .setLowImpedanceBranchMode(REPLACE_BY_MIN_IMPEDANCE_LINE)
                    .setLowImpedanceThreshold(1e-4);

            // Disconnect the line
            network.getLine(lineID).disconnect();

            // Solve the Load Flow problem for the network (for each new topology)
            loadFlowResult = LoadFlow.run(network, parametersLf);
            assertTrue(loadFlowResult.isFullyConverged());

            // Reconnect the line
            network.getLine(lineID).connect();

            // Define the solving options
            StateEstimatorOptions options = new StateEstimatorOptions().setMaxTimeSolving(30);

            StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network, "VL69_0");

            double measureValue;
            Bus relatedBus = network.getLine(lineID).getTerminal1().getBusView().getConnectableBus();
            try {
                measureValue = relatedBus.getV();
            } catch (Exception exception) {
                relatedBus = network.getLine(lineID).getTerminal2().getBusView().getConnectableBus();
                measureValue = relatedBus.getV();
            }

            // Add V measure on selected line
            double variance = Math.pow(RandomMeasuresGenerator.RELATIVE_STD_BY_MEAS_TYPE.get("V")
                            * Math.max(Math.abs(measureValue), RandomMeasuresGenerator.MIN_VOLTAGE_KV)
                    , 2);
            Map<String, String> measure = Map.of(
                    "Type", "V",
                    "BusID", relatedBus.getId(),
                    "Variance", String.valueOf(variance),
                    "Value", String.valueOf(measureValue)
            );
            knowledge.addMeasure(1, measure, network);

            // Randomly generate measurements out of load flow results
            RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network,
                    Optional.of(seed), Optional.of(ratioTested),
                    Optional.of(true), Optional.empty(),
                    Optional.empty(), Optional.of(true));

            // Run SE (erroneous branch is not suspected and presumed closed by default)
            StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                    knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager());

            // Compute normalized residuals
            List<Map.Entry<Integer, Double>> normalizedResiduals = StateEstimatorHeuristic.computeAndSortNormalizedResiduals(knowledge, results);

            // Compute decay index for V measure of selected line (second bus)
            double decayIndex = StateEstimatorHeuristic.computeResidualsDecayIndex(1,
                    normalizedResiduals, knowledge, network);

            data.add(List.of(
                    String.valueOf(seed), String.valueOf(ratioTested),
                    lineID, "PRESUMED_CLOSED",
                    relatedBus.getId(), String.valueOf(decayIndex)
            ));

        }


        // Export the results in a CSV file
        try (FileWriter fileWriter = new FileWriter("V2_ZN4_seed0_AllTopoErr_NoPickBranchCaseA_WithNoise_EnsObs_DECAYINDEX.csv");
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


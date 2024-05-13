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
import com.powsybl.stateestimator.parameters.input.knowledge.StateEstimatorKnowledge;
import com.powsybl.stateestimator.parameters.input.knowledge.RandomMeasuresGenerator;
import com.powsybl.stateestimator.parameters.input.options.StateEstimatorOptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static com.powsybl.iidm.network.Network.read;
import static com.powsybl.openloadflow.OpenLoadFlowParameters.LowImpedanceBranchMode.REPLACE_BY_MIN_IMPEDANCE_LINE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class UseExample {

    @Test
    void useExample() throws IOException {

        // TODO !!  dans AMPL, round les R et X à 1e-6 ? Attention : round(, 6) garde les 6 premiers digits
        // Faire : round( mod(x, 1), 6) pour arrondir la partie flottante
        // Source d'erreur (légère) : car per-unitage puis déper-unitage amène à nombres décimaux avec plus de 6 chiffres après la virgule

        // Load your favorite network (IIDM format preferred)
        //Network network = IeeeCdfNetworkFactory.create30();
        Network network = IeeeCdfNetworkFactory.create118();
        //Network network = IeeeCdfNetworkFactory.create300();
        //Network network = Network.read(Path.of("D:", "Projet", "Réseaux_tests", "IIDM", "pglib_opf_case1354_pegase.xiidm"));

        // Load Flow parameters (note : we mimic the way the AMPL code deals with zero-impedance branches)
        LoadFlowParameters parametersLf = new LoadFlowParameters();
        OpenLoadFlowParameters parametersExt = OpenLoadFlowParameters.create(parametersLf);
        parametersExt.setAlwaysUpdateNetwork(true)
                .setLowImpedanceBranchMode(REPLACE_BY_MIN_IMPEDANCE_LINE)
                .setLowImpedanceThreshold(1e-4)
        ;

        // Want to introduce a topology change ? Disconnect a line
        // Don't forget to RECONNECT IT before running the state estimation
        //network.getLine("L45-46-1").disconnect();

        // Solve the Load Flow problem for the network
        LoadFlowResult loadFlowResult = LoadFlow.run(network, parametersLf);
        assertTrue(loadFlowResult.isFullyConverged());

        // IMPORTANT ! Reconnect the line before running the state estimation (line won't be considered in AMPL script otherwise)
        //network.getLine("L45-46-1").connect();

        // Create "knowledge" instance, containing the slackBus (most meshed bus by default)
        // as well as the sets of measurements and suspect branches
        StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network);

        // For IEEE 118 bus, slack is "VL69_0": our state estimator must use the same slack
        //knowledge.setSlack("VL1_0", network); // for IEEE30
        knowledge.setSlack("VL69_0", network); // for IEEE118
        //knowledge.setSlack("VL7049_0", network); // for IEEE300
        //knowledge.setSlack("VL-4231_0", network); // for case1354_pegase

        // Make all branches suspects and presumed to be closed
        for (Branch branch: network.getBranches()) {
            knowledge.setSuspectBranch(branch.getId(), true, "PRESUMED CLOSED");
        }

        // Add a gross error on measure Pf(VL27 --> VL28) : 80 MW (false) instead of 32.6 MW (true)
        //Map<String, String> grossMeasure = Map.of("BranchID","L27-28-1","FirstBusID","VL27_0","SecondBusID","VL28_0",
        //        "Value","80.0","Variance","0.1306","Type","Pf");
        //knowledge.addMeasure(1, grossMeasure, network);

        // Randomly generate measurements (useful for test cases) out of load flow results
        //RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network, Optional.empty(), Optional.empty(), Optional.empty());
        RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network,
                Optional.of(1), Optional.of(4.0),
                Optional.of(false), Optional.of(false),
                Optional.empty(), Optional.empty());

        // We can also add by hand our measurements, and complete them with generated measurements until observability is ensured
        // Note: if some measurements are added after random generation, one might get more measurements than expected
        //knowledge.addMeasure(measurementNumber, Map<("BranchID"), "FirstBusID", ("SecondBusID"), "Value", "Variance", "Type">, network);

        // Print all the measurements
        //knowledge.printAllMeasures();
        System.out.printf("%nTotal number of measurements : %d%n", knowledge.getMeasuresCount());

        // Save "knowledge" object as a JSON
        //knowledge.write(new FileOutputStream("D:/Projet/Tests/knowledge_14bus_seed2.json"));
        // Read the JSON file as an StateEstimatorKnowledge object
        //StateEstimatorKnowledge test = StateEstimatorKnowledge.read("D:/Projet/Tests/knowledge_14bus_seed2.json");

        // Define the solving options for the state estimation
        StateEstimatorOptions options = new StateEstimatorOptions()
                .setSolvingMode(2).setMaxTimeSolving(60).setMaxNbTopologyChanges(5);

        // Run the state estimation and print the results
        StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager());
        results.printAllResultsSi(network);

        // Print measurement estimates along with residuals for all measures
        //results.printAllMeasurementEstimatesAndResidualsSi(knowledge);

        // In our testing cases, as we know the true values, we can build a StateEstimatorEvaluator
        // to compute statistics on the errors made by the State Estimation
        StateEstimatorEvaluator evaluator = new StateEstimatorEvaluator(network, knowledge, results);

        // Print some indicators on the accuracy of the state estimation w.r.t load flow solution
        List<Double> voltageErrorStats = evaluator.computeVoltageRelativeErrorStats();
        List<Double> angleErrorStats = evaluator.computeAngleDegreeErrorStats();
        List<Double> activePowerFlowErrorStats = evaluator.computeActivePowerFlowsRelativeErrorsStats();
        List<Double> reactivePowerFlowErrorStats = evaluator.computeReactivePowerFlowsRelativeErrorsStats();
        System.out.printf("%nMedian voltage relative error : %f %% %n", voltageErrorStats.get(2));
        System.out.printf("%nMedian angle absolute error : %f degrees %n", angleErrorStats.get(2));
        System.out.printf("%nMedian active power flow relative error : %f %% %n", activePowerFlowErrorStats.get(2));
        System.out.printf("%nMedian reactive power flow relative error : %f %% %n", reactivePowerFlowErrorStats.get(2));
        //System.out.printf("%nPerformance index : %f %n", evaluator.computePerformanceIndex()); // Only if noise added to measures
        System.out.printf("%n95th percentile active power flow : %f %% %n", activePowerFlowErrorStats.get(5));
        System.out.printf("%n95th percentile reactive power flow : %f %% %n", reactivePowerFlowErrorStats.get(5));
        System.out.printf("%n5th percentile voltage relative error : %f %% %n", voltageErrorStats.get(4));
        System.out.printf("%n95th percentile voltage relative error : %f %% %n", voltageErrorStats.get(5));
        System.out.printf("%n5th percentile angle absolute error : %f degrees %n", angleErrorStats.get(4));
        System.out.printf("%n95th percentile angle absolute error : %f degrees %n", angleErrorStats.get(5));
    }
}

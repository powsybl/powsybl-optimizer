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
import com.powsybl.stateestimator.parameters.input.measuresgeneration.RandomMeasuresGenerator;
import com.powsybl.stateestimator.parameters.input.options.StateEstimatorOptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static com.powsybl.iidm.network.Network.read;
import static com.powsybl.openloadflow.OpenLoadFlowParameters.LowImpedanceBranchMode.REPLACE_BY_MIN_IMPEDANCE_LINE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class UseExample {

    @Test
    void useExample() throws IOException {

        // Load your favorite network (IIDM format preferred)
        Network network = IeeeCdfNetworkFactory.create118();
        //Network network = Network.read(Path.of("D:", "Projet", "Réseaux_tests", "IIDM", "pglib_opf_case1354_pegase.xiidm"));

        // Load Flow parameters (note : we mimic the way the AMPL code deals with zero-impedance branches)
        LoadFlowParameters parametersLf = new LoadFlowParameters();
        OpenLoadFlowParameters parametersExt = OpenLoadFlowParameters.create(parametersLf);
        parametersExt.setAlwaysUpdateNetwork(true)
                .setLowImpedanceBranchMode(REPLACE_BY_MIN_IMPEDANCE_LINE)
                .setLowImpedanceThreshold(1e-4);

        // Want to introduce a topology change ? Disconnect a line (don't forget to RECONNECT IT before running the state estimation)
        //network.getLine("L45-46-1").disconnect(); // for IEEE118
        //network.getLine("LINE-6757-6036").disconnect(); // for case1354_pegase

        // Solve the Load Flow problem for the network and the referenceNetwork
        LoadFlowResult loadFlowResult = LoadFlow.run(network, parametersLf);
        assertTrue(loadFlowResult.isFullyConverged());

        // Reconnect the line before running the state estimation (line won't be considered in AMPL script otherwise)
        //network.getLine("L45-46-1").connect();
        //network.getLine("LINE-6757-6036").connect();

        long startTime = System.nanoTime();

        // Create "knowledge" instance, containing all priori knowledge on the network
        // (reference bus, measurements, zero injection buses, presumed topology, state vector starting point)
        StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network);

        // Make sure the state estimator and OpenLoadFlow use the same slack bus
        knowledge.setSlack("VL69_0", network); // for IEEE118
        //knowledge.setSlack("VL-4231_0", network); // for case1354_pegase
        //knowledge.setSlack("VL-3853_0", network); // for case8387_pegase

        // Make all branches suspects and presumed to be closed
        for (Branch branch: network.getBranches()) {
            knowledge.setSuspectBranch(branch.getId(), false, "PRESUMED CLOSED");
        }
        //knowledge.setSuspectBranch("L45-46-1", true, "PRESUMED OPENED");

        // Add a gross error on measure Pf(VL27 --> VL28) : 80 MW (false) instead of 32.6 MW (true)
        Map<String, String> grossMeasure = Map.of("BranchID","L27-28-1","FirstBusID","VL27_0","SecondBusID","VL28_0",
                "Value","80.0","Variance","0.1306","Type","Pf");
        knowledge.addMeasure(1, grossMeasure, network);
        // Add a gross error on measure Pf(VL45 --> VL46) : 0 MW (false) instead of -36,32 MW (true)
        //Map<String, String> grossMeasure = Map.of("BranchID","L45-46-1","FirstBusID","VL45_0","SecondBusID","VL46_0",
        //        "Value","30.0","Variance","0.1596","Type","Pf");
        //knowledge.addMeasure(1, grossMeasure, network);
        // Add a gross error on measure V(VL45) : 200 kV (false) instead of 136,16 kV (true)
        //Map<String, String> grossMeasure = Map.of("BusID","VL45_0","Value","200.0","Variance","0.4822","Type","V");
        //knowledge.addMeasure(1, grossMeasure, network);
        // Add a gross error on measure P(VL45) : 100 MW (false) instead of -53 MW (true)
        //Map<String, String> grossMeasure = Map.of("BusID","VL45_0","Value","100.0","Variance","0.34","Type","P");
        //knowledge.addMeasure(1, grossMeasure, network);

        // Randomly generate measurements (useful for test cases) out of load flow results
        var parameters = new RandomMeasuresGenerator.RandomMeasuresGeneratorParameters();
        parameters.withSeed(1).withRatioMeasuresToBuses(4.0)
                .withEnsureObservability(true);
        RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network, parameters);

        long endTime   = System.nanoTime();
        long totalTime = endTime - startTime;
        System.out.println("Time to generate measurements :");
        System.out.println(totalTime / 1e9);

        // Or randomly generate measurements out of load flow results, with a controlled number of measurements for a given measurement type
        //RandomMeasuresGenerator.generateRandomMeasurementsWithCtrlMeasureRatio(knowledge, network,
        //        0.1991137371, "P",
        //        Optional.of(1), Optional.of(5.0),
        //        Optional.empty(), Optional.of(false),
        //        Optional.empty(), Optional.empty());

        // We can also add by hand our measurements, and complete them with generated measurements until observability is ensured
        // Note: if some measurements are added after random generation, one might get more measurements than expected
        //knowledge.addMeasure(measurementNumber, Map<("BranchID"), "FirstBusID", ("SecondBusID"), "Value", "Variance", "Type">, network);

        // Print all the measurements
        //knowledge.printAllMeasures();
        System.out.printf("%nTotal number of measurements : %d%n", knowledge.getMeasuresCount());

        // Define the solving options for the state estimation
        StateEstimatorOptions options = new StateEstimatorOptions()
                .setSolvingMode(0).setMaxTimeSolving(30).setMaxNbTopologyChanges(5).setMipMultistart(0);

        // Run the state estimation and print the results
        StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager());
        //results.printAllResultsSi(network);
        results.printIndicators();
        results.printNetworkTopology();

        // Print measurement estimates along with residuals for all measures
        //results.printAllMeasurementEstimatesAndResidualsSi(knowledge);
        //results.exportAllMeasurementEstimatesAndResidualsSi(knowledge);

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
        System.out.printf("%n95th percentile voltage relative error : %f %% %n", voltageErrorStats.get(5));
        System.out.printf("%n95th percentile angle absolute error : %f degrees %n", angleErrorStats.get(5));
        System.out.printf("%n95th percentile active power flow : %f %% %n", activePowerFlowErrorStats.get(5));
        System.out.printf("%n95th percentile reactive power flow : %f %% %n", reactivePowerFlowErrorStats.get(5));
        System.out.printf("%nObjective function value : %f %n", results.getObjectiveFunctionValue());
        System.out.printf("%nMax active power flow error : %f %% %n", activePowerFlowErrorStats.get(3));
        System.out.printf("%nMax reactive power flow error : %f %% %n", reactivePowerFlowErrorStats.get(3));
        System.out.printf("%nMax active power flow absolute error : %f MW %n", activePowerFlowErrorStats.get(6));
        System.out.printf("%nMax reactive power flow absolute error : %f MVar %n", reactivePowerFlowErrorStats.get(6));
    }
}

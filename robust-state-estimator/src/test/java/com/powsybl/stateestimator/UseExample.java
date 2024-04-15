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
import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

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

        // Load your favorite network (IIDM format preferred)
        //Network network = IeeeCdfNetworkFactory.create14();
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

        // Create "knowledge" instance, containing the slackBus (most meshed bus by default)
        // as well as the sets of measurements and suspect branches
        StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network);

        // For IEEE 118 bus, slack is "VL69_0": our state estimator must use the same slack
        knowledge.setSlack("VL69_0", network);

        // Randomly generate measurements (useful for test cases) out of load flow results
        //RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network, Optional.empty(), Optional.empty(), Optional.empty());
        RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network,
                Optional.of(4), Optional.of(4.0),
                Optional.of(false), Optional.of(true));

        // We can also add by hand our measurements, and complete them with generated measurements until observability is ensured
        // If some measurements are added after random generation, one might get more measurements than expected
        //knowledge.addMeasure(measurementNumber, Map<("BranchID"), "FirstBusID", ("SecondBusID"), "Value", "Variance", "Type">, network);

        // Save "knowledge" object as a JSON
        //knowledge.write(new FileOutputStream("D:/Projet/Tests/knowledge_14bus_seed2.json"));
        // Read the JSON file as an StateEstimatorKnowledge object
        //StateEstimatorKnowledge test = StateEstimatorKnowledge.read("D:/Projet/Tests/knowledge_14bus_seed2.json");

        // Print all the measurements
        //knowledge.printAllMeasures();
        System.out.printf("%nTotal number of measurements : %d%n", knowledge.getMeasuresCount());

        // Make a branch suspect and change its presumed status
        //knowledge.setSuspectBranch("L1-2-1", true, "PRESUMED OPENED");
        //knowledge.setSuspectBranch("L1-5-1", true, "PRESUMED CLOSED");
        //knowledge.setSuspectBranch("L2-3-1", true, "PRESUMED OPENED");
        //knowledge.setSuspectBranch("L2-4-1", true, "PRESUMED CLOSED");

        // Define the solving options for the state estimation
        StateEstimatorOptions options = new StateEstimatorOptions().setSolvingMode(2).setMaxTimeSolving(30);

        // Run the state estimation and print the results
        StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                knowledge, new StateEstimatorOptions(), new StateEstimatorConfig(true), new LocalComputationManager());
        results.printAllResultsSi(network);

        // Print measurements along with residuals
        results.printResidualsSi(knowledge);

        // Print some indicators on the accuracy of the state estimation w.r.t load flow solution
        List<Double> voltageErrorStats = results.computeVoltageRelativeErrorStats(network);
        List<Double> angleErrorStats = results.computeAngleDegreeErrorStats(network);
        List<Double> activePowerFlowErrorStats = results.computeActivePowerFlowsRelativeErrorsStats(network);
        List<Double> reactivePowerFlowErrorStats = results.computeReactivePowerFlowsRelativeErrorsStats(network);
        System.out.printf("%nMedian voltage relative error : %f %% %n", voltageErrorStats.get(2));
        System.out.printf("%nMedian angle absolute error : %f degrees %n", angleErrorStats.get(2));
        System.out.printf("%nMedian active power flow relative error : %f %% %n", activePowerFlowErrorStats.get(2));
        System.out.printf("%nMedian reactive power flow relative error : %f %% %n", reactivePowerFlowErrorStats.get(2));
        System.out.printf("%nNumber of voltage magnitude measurements : %d%n", knowledge.getVoltageMagnitudeMeasures().size());

    }
}

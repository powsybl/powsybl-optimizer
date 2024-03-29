/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator;

import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.stateestimator.StateEstimator;
import com.powsybl.stateestimator.StateEstimatorConfig;
import com.powsybl.stateestimator.StateEstimatorResults;
import com.powsybl.stateestimator.parameters.input.knowledge.StateEstimatorKnowledge;
import com.powsybl.stateestimator.parameters.input.options.StateEstimatorOptions;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
        //Network network = Network.read("your favorite network");
        Network network = IeeeCdfNetworkFactory.create14();
        //network.write();

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
        // as well as sets of measurements and suspect branches
        StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network);

        // Randomly generate measurements (useful for test cases) out of load flow results
        knowledge.generateRandomMeasurements(network, 3);
        // Note : we can also add by hand our measurements, and complete them with generated measurements until observability is ensured
        //knowledge.addActivePowerFlowMeasure(measurementNumber, Map<"BranchID", "FirstBusID", "SecondBusID", "Value", "Variance", "Type">, network);

        // Save "knowledge" in the desired folder as a JSON
        //knowledge.write(new FileOutputStream("D:/Projet/Tests/knowledge_14bus_seed2.json"));
        // Read the JSON file as an StateEstimatorKnowledge instance
        //StateEstimatorKnowledge test = StateEstimatorKnowledge.read("D:/Projet/Tests/knowledge_14bus_seed2.json");

        // Print these measurements
        //knowledge.printAllMeasures();
        knowledge.printActivePowerFlowMeasures();
        knowledge.printReactivePowerFlowMeasures();
        knowledge.printActivePowerInjectedMeasures();
        knowledge.printReactivePowerInjectedMeasures();
        knowledge.printVoltageMagnitudeMeasures();
        System.out.printf("%nTotal number of measurements : %d%n", knowledge.getMeasuresCount());

        // Add suspect branches if wanted
        //knowledge.addSuspectBranch(1, "L1-2-1", network);

        // Define the options for the state estimation
        //StateEstimatorOptions options = new StateEstimatorOptions().setSolvingMode(2).setMaxTimeSolving(30);

        // Run the state estimation and print the results
        StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(),
                knowledge, new StateEstimatorOptions(), new StateEstimatorConfig(true), new LocalComputationManager());
        results.printAllResultsSi(network);

        // Print some indicators on the accuracy of the state estimation w.r.t load flow solution
        long nbBuses = network.getBusView().getBusStream().count();
        double avgVoltageError = 0;
        double squaredVoltageError = 0;
        double avgAngleErrror = 0;
        double squaredAngleError = 0;
        for (Bus bus : network.getBusView().getBuses()) {
            avgVoltageError += Math.abs(bus.getV()/bus.getVoltageLevel().getNominalV() - results.getBusStateEstimate(bus.getId()).getV());
            squaredVoltageError += Math.pow(bus.getV()/bus.getVoltageLevel().getNominalV() - results.getBusStateEstimate(bus.getId()).getV(), 2);
            avgAngleErrror += Math.abs(bus.getAngle() - Math.toDegrees(results.getBusStateEstimate(bus.getId()).getTheta()));
            squaredAngleError += Math.pow(bus.getAngle() - Math.toDegrees(results.getBusStateEstimate(bus.getId()).getTheta()), 2);
        }
        avgVoltageError = avgVoltageError / nbBuses;
        avgAngleErrror = avgAngleErrror / nbBuses;
        double stdVoltageError = Math.sqrt(squaredVoltageError/nbBuses - Math.pow(avgVoltageError, 2));
        double stdAngleError = Math.sqrt(squaredAngleError/nbBuses - Math.pow(avgAngleErrror, 2));
        System.out.printf("%nAverage voltage error : %f p.u (std = %f)%n", avgVoltageError, stdVoltageError);
        System.out.printf("%nAverage angle error : %f degrees (std = %f)%n", avgAngleErrror, stdAngleError);
        System.out.printf("%nNumber of voltage magnitude measurements : %d%n", knowledge.getVoltageMagnitudeMeasures().size());

        for (Bus bus : network.getBusView().getBuses()) {
            System.out.printf("%nBus %s : voltage = %f p.u, angle = %f rad %n", bus.getId(), bus.getV()/bus.getVoltageLevel().getNominalV(), Math.toRadians(bus.getAngle()));
        }
    }
}

/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.stateestimator.parameters.input.knowledge.*;
import com.powsybl.stateestimator.parameters.output.estimates.BranchPowersEstimate;

import java.util.*;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 * To only be used when results returned by the state estimator can be compared with LF results previously computed.
 */
public class StateEstimatorEvaluator {

    Network network;
    StateEstimatorKnowledge knowledge;
    StateEstimatorResults results;

    public StateEstimatorEvaluator(Network network, StateEstimatorKnowledge knowledge, StateEstimatorResults results) {
        this.network = network;
        this.knowledge = knowledge;
        this.results = results;
    }

    /**
     * Compute statistics for voltage magnitude relative errors (%) of the state estimate, taking OLF results as ground truth
     * @return (meanVoltageError, stdVoltageError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError)
     */
    public List<Double> computeVoltageRelativeErrorStats() {
        long nbBuses = this.network.getBusView().getBusStream().count();
        // Initialize a list containing all errors
        List<Double> allErrors = new ArrayList<>();
        // Compute mean and standard deviation (and fill the list)
        double meanVoltageError = 0;
        double squaredVoltageError = 0;
        for (Bus bus : this.network.getBusView().getBuses()) {
            double tmpVoltageError = Math.abs((this.results.getBusStateEstimate(bus.getId()).getV() * bus.getVoltageLevel().getNominalV() - bus.getV())
                    / bus.getV()) * 100;
            meanVoltageError += tmpVoltageError;
            squaredVoltageError += Math.pow(tmpVoltageError, 2);
            allErrors.add(tmpVoltageError);
        }
        meanVoltageError = meanVoltageError / nbBuses;
        double stdVoltageError = Math.sqrt(squaredVoltageError / nbBuses - Math.pow(meanVoltageError, 2));
        // Compute 5th, 50th (median) and 95th percentiles
        Collections.sort(allErrors);
        double fifthPercentileError = percentile(allErrors, 5);
        double medianError = percentile(allErrors, 50);
        double ninetyFifthPercentileError = percentile(allErrors, 95);
        // Compute maximum
        double maxError = allErrors.get(allErrors.size() - 1);
        return List.of(meanVoltageError, stdVoltageError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError);
    }

    /**
     * Compute statistics for voltage angle absolute errors (degrees) of the state estimate, taking OLF results as ground truth
     * @return (meanAngleError, stdVAngleError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError)
     */
    public List<Double> computeAngleDegreeErrorStats() {
        long nbBuses = this.network.getBusView().getBusStream().count();
        // Initialize a list containing all errors
        List<Double> allErrors = new ArrayList<>();
        // Compute mean and standard deviation (and fill the list)
        double meanAngleErrror = 0;
        double squaredAngleError = 0;
        for (Bus bus : this.network.getBusView().getBuses()) {
            double tmpAngleError = Math.abs(bus.getAngle() - Math.toDegrees(this.results.getBusStateEstimate(bus.getId()).getTheta()));
            meanAngleErrror += tmpAngleError;
            squaredAngleError += Math.pow(tmpAngleError, 2);
            allErrors.add(tmpAngleError);
        }
        meanAngleErrror = meanAngleErrror / nbBuses;
        double stdAngleError = Math.sqrt(squaredAngleError / nbBuses - Math.pow(meanAngleErrror, 2));
        // Compute 5th, 50th (median) and 95th percentiles
        Collections.sort(allErrors);
        double fifthPercentileError = percentile(allErrors, 5);
        double medianError = percentile(allErrors, 50);
        double ninetyFifthPercentileError = percentile(allErrors, 95);
        // Compute maximum
        double maxError = allErrors.get(allErrors.size() - 1);
        return List.of(meanAngleErrror, stdAngleError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError);
    }

    /**
     * Compute statistics for active power flows (on both sides) relative errors (%) based on the state estimate, taking OLF results as ground truth
     * @return (meanPfError, stdPfError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError)
     */
    public List<Double> computeActivePowerFlowsRelativeErrorsStats() {
        // Define the threshold for minimal tolerated error on power flows
        double epsilonPf = 10.;
        // Get number of active/reactive power flows (two sides for each branch)
        int nbPowerFlows = this.network.getBranchCount() * 2;
        // Initialize a list containing all relative and absolute errors
        List<Double> allErrors = new ArrayList<>();
        List<Double> allAbsoluteErrors = new ArrayList<>();
        // Compute mean and standard deviation (and fill the list)
        double meanPfErrror = 0;
        double squaredPfError = 0;
        for (Branch branch : this.network.getBranches()) {
            BranchPowersEstimate branchPowersEstimate = this.results.getBranchPowersEstimate(branch.getId());

            // Extra step, to make sure that (end1,end2) of the branch coincides between the network and the estimates data (although estimates are based on AMPL export, which respect how branches' ends are indicated in "network")
            if (!branchPowersEstimate.getFirstBusID().equals(branch.getTerminal1().getBusView().getConnectableBus().getId())) {
                throw new IllegalArgumentException("Branch terminals as indicated in se_network_powers_estimate.csv are inverted w.r.t the way they are indicating in Network.");
            }
            if (!branchPowersEstimate.getSecondBusID().equals(branch.getTerminal2().getBusView().getConnectableBus().getId())) {
                throw new IllegalArgumentException("Branch terminals as indicated in se_network_powers_estimate.csv are inverted w.r.t the way they are indicating in Network.");
            }

            double truePfEnd1 = branch.getTerminal1().getP();
            if (Double.isNaN(truePfEnd1)) {
                truePfEnd1 = 0;
            }
            double truePfEnd2 = branch.getTerminal2().getP();
            if (Double.isNaN(truePfEnd2)) {
                truePfEnd2 = 0;
            }

            double tmpPfErrorEnd1Absolute = Math.abs(branchPowersEstimate.getActivePowerEnd1() - truePfEnd1);
            double tmpPfErrorEnd1 = tmpPfErrorEnd1Absolute / Math.max(Math.abs(truePfEnd1), epsilonPf) * 100;
            double tmpPfErrorEnd2Absolute = Math.abs(branchPowersEstimate.getActivePowerEnd2() - truePfEnd2);
            double tmpPfErrorEnd2 = tmpPfErrorEnd2Absolute / Math.max(Math.abs(truePfEnd2), epsilonPf) * 100;

            meanPfErrror += tmpPfErrorEnd1;
            squaredPfError += Math.pow(tmpPfErrorEnd1, 2);
            allErrors.add(tmpPfErrorEnd1);
            allAbsoluteErrors.add(tmpPfErrorEnd1Absolute);

            meanPfErrror += tmpPfErrorEnd2;
            squaredPfError += Math.pow(tmpPfErrorEnd2, 2);
            allErrors.add(tmpPfErrorEnd2);
            allAbsoluteErrors.add(tmpPfErrorEnd2Absolute);
        }
        meanPfErrror = meanPfErrror / nbPowerFlows;
        double stdPfError = Math.sqrt(squaredPfError / nbPowerFlows - Math.pow(meanPfErrror, 2));
        // Compute 5th, 50th (median) and 95th percentiles
        Collections.sort(allErrors);
        double fifthPercentileError = percentile(allErrors, 5);
        double medianError = percentile(allErrors, 50);
        double ninetyFifthPercentileError = percentile(allErrors, 95);
        // Compute maximum
        double maxError = allErrors.get(allErrors.size() - 1);
        // Compute maximum absolute errors
        double maxAbsoluteError = Collections.max(allAbsoluteErrors);
        return List.of(meanPfErrror, stdPfError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError, maxAbsoluteError);
    }

    /**
     * Compute statistics for reactive power flows (on both sides) relative errors (%) based on the state estimate, taking OLF results as ground truth
     * @return (meanQfError, stdQfError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError)
     */
    public List<Double> computeReactivePowerFlowsRelativeErrorsStats() {
        // Define the threshold for minimal tolerated error on power flows
        double epsilonQf = 1.;
        // Get number of active/reactive power flows (two sides for each branch)
        int nbPowerFlows = this.network.getBranchCount() * 2;
        // Initialize a list containing all relative and absolute errors
        List<Double> allErrors = new ArrayList<>();
        List<Double> allAbsoluteErrors = new ArrayList<>();
        // Compute mean and standard deviation (and fill the list)
        double meanQfErrror = 0;
        double squaredQfError = 0;
        for (Branch branch : this.network.getBranches()) {
            BranchPowersEstimate branchPowersEstimate = this.results.getBranchPowersEstimate(branch.getId());

            // Extra step, to make sure that (end1,end2) of the branch coincides between the network and the estimates data (although estimates are based on AMPL export, which respect how branches' ends are indicated in "network")
            if (!branchPowersEstimate.getFirstBusID().equals(branch.getTerminal1().getBusView().getConnectableBus().getId())) {
                throw new IllegalArgumentException("Branch terminals as indicated in se_network_powers_estimate.csv are inverted w.r.t the way they are indicating in Network.");
            }
            if (!branchPowersEstimate.getSecondBusID().equals(branch.getTerminal2().getBusView().getConnectableBus().getId())) {
                throw new IllegalArgumentException("Branch terminals as indicated in se_network_powers_estimate.csv are inverted w.r.t the way they are indicating in Network.");
            }

            double trueQfEnd1 = branch.getTerminal1().getQ();
            if (Double.isNaN(trueQfEnd1)) {
                trueQfEnd1 = 0;
            }
            double trueQfEnd2 = branch.getTerminal2().getQ();
            if (Double.isNaN(trueQfEnd2)) {
                trueQfEnd2 = 0;
            }

            double tmpQfErrorEnd1Absolute = Math.abs(branchPowersEstimate.getReactivePowerEnd1() - trueQfEnd1);
            double tmpQfErrorEnd1 = tmpQfErrorEnd1Absolute / Math.max(Math.abs(trueQfEnd1), epsilonQf) * 100;
            double tmpQfErrorEnd2Absolute = Math.abs(branchPowersEstimate.getReactivePowerEnd2() - trueQfEnd2);
            double tmpQfErrorEnd2 = tmpQfErrorEnd2Absolute / Math.max(Math.abs(trueQfEnd2), epsilonQf) * 100;

            meanQfErrror += tmpQfErrorEnd1;
            squaredQfError += Math.pow(tmpQfErrorEnd1, 2);
            allErrors.add(tmpQfErrorEnd1);
            allAbsoluteErrors.add(tmpQfErrorEnd1Absolute);

            meanQfErrror += tmpQfErrorEnd2;
            squaredQfError += Math.pow(tmpQfErrorEnd2, 2);
            allErrors.add(tmpQfErrorEnd2);
            allAbsoluteErrors.add(tmpQfErrorEnd2Absolute);
        }
        meanQfErrror = meanQfErrror / nbPowerFlows;
        double stdQfError = Math.sqrt(squaredQfError / nbPowerFlows - Math.pow(meanQfErrror, 2));
        // Compute 5th, 50th (median) and 95th percentiles
        Collections.sort(allErrors);
        double fifthPercentileError = percentile(allErrors, 5);
        double medianError = percentile(allErrors, 50);
        double ninetyFifthPercentileError = percentile(allErrors, 95);
        // Compute maximum
        double maxError = allErrors.get(allErrors.size() - 1);
        // Compute maximum absolute error
        double maxAbsoluteError = Collections.max(allAbsoluteErrors);
        return List.of(meanQfErrror, stdQfError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError, maxAbsoluteError);
    }

    public static double percentile(List<Double> array, double percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * array.size());
        return array.get(index - 1);
    }

    /**
     * @return Performance index of the state estimation A/B, with A = sum of squares of residuals, B = sum of squares of measurement noises
     */
    public double computePerformanceIndex() {

        double numerator = 0;
        double denominator = 0;

        // Computation of A : sum of (estimated_measure - true_measure)^2

        // Get the sets of measurements extended with their estimates and residuals
        ActivePowerFlowMeasures activePowerFlowEstimates = this.results.getActivePowerFlowMeasuresExtended();
        ReactivePowerFlowMeasures reactivePowerFlowEstimates = this.results.getReactivePowerFlowMeasuresExtended();
        ActivePowerInjectedMeasures activePowerInjectedEstimates = this.results.getActivePowerInjectedMeasuresExtended();
        ReactivePowerInjectedMeasures reactivePowerInjectedEstimates = this.results.getReactivePowerInjectedMeasuresExtended();
        VoltageMagnitudeMeasures voltageMagnitudeEstimates = this.results.getVoltageMagnitudeMeasuresExtended();

        // Structure of an extended power flow measure : "Type","BranchID","FirstBusID","SecondBusID","Value","Variance","Estimate","Residual"
        for (var estimate : activePowerFlowEstimates.getMeasuresWithEstimatesAndResiduals().entrySet()) {

            ArrayList<String> values = estimate.getValue();
            // If power flow measure is on side 1
            if (this.network.getBranch(values.get(1)).getTerminal1().getBusView().getConnectableBus().getId().equals(values.get(2))) {
                double truePfEnd1 = this.network.getBranch(values.get(1)).getTerminal1().getP();
                if (Double.isNaN(truePfEnd1)) {
                    truePfEnd1 = 0;
                }
                numerator += Math.pow(truePfEnd1 - Double.parseDouble(values.get(6)), 2);
            }
            // If power flow measure is on side 2
            else if (this.network.getBranch(values.get(1)).getTerminal2().getBusView().getConnectableBus().getId().equals(values.get(2))) {
                double truePfEnd2 = this.network.getBranch(values.get(1)).getTerminal2().getP();
                if (Double.isNaN(truePfEnd2)) {
                    truePfEnd2 = 0;
                }
                numerator += Math.pow(truePfEnd2 - Double.parseDouble(values.get(6)), 2);
            }
            else {
                throw new IllegalArgumentException(String.format("Estimate %d can not be valid.", estimate.getKey()));
            }
        }
        for (var estimate : reactivePowerFlowEstimates.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            ArrayList<String> values = estimate.getValue();
            // If power flow measure is on side 1
            if (this.network.getBranch(values.get(1)).getTerminal1().getBusView().getConnectableBus().getId().equals(values.get(2))) {
                double trueQfEnd1 = this.network.getBranch(values.get(1)).getTerminal1().getQ();
                if (Double.isNaN(trueQfEnd1)) {
                    trueQfEnd1 = 0;
                }
                numerator += Math.pow(trueQfEnd1 - Double.parseDouble(values.get(6)), 2);
            }
            // If power flow measure is on side 2
            else if (this.network.getBranch(values.get(1)).getTerminal2().getBusView().getConnectableBus().getId().equals(values.get(2))) {
                double trueQfEnd2 = this.network.getBranch(values.get(1)).getTerminal2().getQ();
                if (Double.isNaN(trueQfEnd2)) {
                    trueQfEnd2 = 0;
                }
                numerator += Math.pow(trueQfEnd2 - Double.parseDouble(values.get(6)), 2);
            }
            else {
                throw new IllegalArgumentException(String.format("Estimate %d can not be valid.", estimate.getKey()));
            }
        }
        // Structure of an extended voltage/injected power measure : "Type","BusID","Value","Variance","Estimate","Residual"
        // Note : for active power injected measures, revert sign
        for (var estimate : activePowerInjectedEstimates.getMeasuresWithEstimatesAndResiduals().values()) {
            numerator += Math.pow(-this.network.getBusView().getBus(estimate.get(1)).getP()
                            - Double.parseDouble(estimate.get(4)), 2);
        }
        // Note : for reactive power injected measures, revert sign
        for (var estimate : reactivePowerInjectedEstimates.getMeasuresWithEstimatesAndResiduals().values()) {
            numerator += Math.pow(-this.network.getBusView().getBus(estimate.get(1)).getQ()
                            - Double.parseDouble(estimate.get(4)), 2);
        }
        for (var estimate : voltageMagnitudeEstimates.getMeasuresWithEstimatesAndResiduals().values()) {
            numerator += Math.pow(this.network.getBusView().getBus(estimate.get(1)).getV()
                            - Double.parseDouble(estimate.get(4)), 2);
        }

        // Computation of B : sum of (noisy_measure - true_measure)^2

        // Structure of a power flow measure : "Type","BranchID","FirstBusID","SecondBusID","Value","Variance"
        for (Map.Entry<Integer, ArrayList<String>> measure : this.knowledge.getActivePowerFlowMeasures().entrySet()) {
            ArrayList<String> values = measure.getValue();
            // If power flow measure is on side 1
            if (this.network.getBranch(values.get(1)).getTerminal1().getBusView().getConnectableBus().getId().equals(values.get(2))) {
                double noisyPfEnd1 = this.network.getBranch(values.get(1)).getTerminal1().getP();
                if (Double.isNaN(noisyPfEnd1)) {
                    noisyPfEnd1 = 0;
                }
                denominator += Math.pow(noisyPfEnd1 - Double.parseDouble(values.get(4)), 2);
            }
            // If power flow measure is on side 2
            else if (this.network.getBranch(values.get(1)).getTerminal2().getBusView().getConnectableBus().getId().equals(values.get(2))) {
                double noisyPfEnd2 = this.network.getBranch(values.get(1)).getTerminal2().getP();
                if (Double.isNaN(noisyPfEnd2)) {
                    noisyPfEnd2 = 0;
                }
                denominator += Math.pow(noisyPfEnd2 - Double.parseDouble(values.get(4)), 2);
            }
            else {
                throw new IllegalArgumentException(String.format("Measure %d can not be valid.", measure.getKey()));
            }
        }
        for (Map.Entry<Integer, ArrayList<String>> measure : this.knowledge.getReactivePowerFlowMeasures().entrySet()) {
            ArrayList<String> values = measure.getValue();
            // If power flow measure is on side 1
            if (this.network.getBranch(values.get(1)).getTerminal1().getBusView().getConnectableBus().getId().equals(values.get(2))) {
                double noisyQfEnd1 = this.network.getBranch(values.get(1)).getTerminal1().getQ();
                if (Double.isNaN(noisyQfEnd1)) {
                    noisyQfEnd1 = 0;
                }
                denominator += Math.pow(noisyQfEnd1 - Double.parseDouble(values.get(4)), 2);
            }
            // If power flow measure is on side 2
            else if (this.network.getBranch(values.get(1)).getTerminal2().getBusView().getConnectableBus().getId().equals(values.get(2))) {
                double noisyQfEnd2 = this.network.getBranch(values.get(1)).getTerminal2().getQ();
                if (Double.isNaN(noisyQfEnd2)) {
                    noisyQfEnd2 = 0;
                }
                denominator += Math.pow(noisyQfEnd2 - Double.parseDouble(values.get(4)), 2);
            }
            else {
                throw new IllegalArgumentException(String.format("Measure %d can not be valid.", measure.getKey()));
            }
        }
        // Structure of voltage/injected power measures : "Type","BusID","Value","Variance"
        // Note : for active power injected measures, revert sign
        for (ArrayList<String> measure : this.knowledge.getActivePowerInjectedMeasures().values()) {
            denominator += Math.pow(-this.network.getBusView().getBus(measure.get(1)).getP()
                            - Double.parseDouble(measure.get(2)), 2);
        }
        // Note : for reactive power injected measures, revert sign
        for (ArrayList<String> measure : this.knowledge.getReactivePowerInjectedMeasures().values()) {
            denominator += Math.pow(-this.network.getBusView().getBus(measure.get(1)).getQ()
                            - Double.parseDouble(measure.get(2)), 2);
        }
        for (ArrayList<String> measure : this.knowledge.getVoltageMagnitudeMeasures().values()) {
            denominator += Math.pow(this.network.getBusView().getBus(measure.get(1)).getV()
                            - Double.parseDouble(measure.get(2)), 2);
        }

        if (denominator == 0) {
            throw new IllegalArgumentException("The performance index cannot be calculated, as no noise seems to have been added to the measurements.");
        }
        return numerator / denominator;
    }

}


/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.stateestimator.parameters.StateEstimatorAmplIOFiles;
//import main.java.com.powsybl.stateestimator.parameters.output.modifications.StateBus;
//import main.java.com.powsybl.stateestimator.parameters.output.modifications.NetworkTopology;
import com.powsybl.iidm.network.Network;
import com.powsybl.stateestimator.parameters.input.knowledge.*;
import com.powsybl.stateestimator.parameters.output.estimates.BranchPowersEstimate;
import com.powsybl.stateestimator.parameters.output.estimates.BranchStatusEstimate;
import com.powsybl.stateestimator.parameters.output.estimates.BusStateEstimate;

import org.jgrapht.alg.util.Pair;

import java.util.*;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class StateEstimatorResults {
    private final boolean status;

    // State vector, network topology and power flows estimates (MINLP variables written and calculated in AMPL)
    List<BusStateEstimate> stateVectorEstimate;
    List<BranchStatusEstimate> networkTopologyEstimate;
    List<BranchPowersEstimate> networkPowersEstimate;

    // Indicators returned by the AMPL code
    private final List<Pair<String, String>> runIndicators;
    private final List<Pair<String, String>> networkIndicators;

    // Measurement residuals at the end of the state estimation
    Map<Integer, String> measurementResiduals;

    /**
     * @param status      The final status of the state estimation run.
     * @param amplIOFiles A file interface to fetch output file information.
     * @param runIndicators  A standard map written by the state estimation AMPL model.
     */
    public StateEstimatorResults(boolean status, StateEstimatorAmplIOFiles amplIOFiles, Map<String, String> runIndicators) {
        Objects.requireNonNull(amplIOFiles);
        this.status = status;
        this.stateVectorEstimate = amplIOFiles.getStateVectorEstimateOutput().getStateVectorEstimate();
        this.networkTopologyEstimate = amplIOFiles.getNetworkTopologyEstimateOutput().getNetworkTopologyEstimate();
        this.networkPowersEstimate = amplIOFiles.getNetworkPowersEstimateOutput().getNetworkPowersEstimate();
        this.networkIndicators = amplIOFiles.getNetworkIndicatorsOutput().getIndicators();
        this.measurementResiduals = amplIOFiles.getMeasurementResidualsOutput().getMeasurementResiduals();

        // TODO : change code about runIndicators, to use ours (se_run_indic.txt) and not those returned by AMPL/Java interface
        Objects.requireNonNull(runIndicators);
        this.runIndicators = new ArrayList<>();
        for (Map.Entry<String, String> entry : runIndicators.entrySet()) {
            this.runIndicators.add(Pair.of(entry.getKey(), entry.getValue()));
        }
    }

    public boolean getStatus() {
        return status;
    }

    /**
     * Print all results, i.e. state vector and network topology, in p.u. for the former.
     */
    public void printAllResultsPu() {
        printStateVectorPu();
        printNetworkTopology();
    }

    public void printStateVectorPu() {
        System.out.println("Printing state estimate : ");
        // Print the table header
        System.out.format("%n%-25s%-25s%-25s%n", "BusID", "Estimated V (p.u.)", "Estimated angle (rad)");
        System.out.format("%-25s%-25s%-25s%n", "-----", "------------------", "---------------------");
        // Print state estimation for each bus
        for (BusStateEstimate busStateEstimate : stateVectorEstimate) {
            String V = String.valueOf(busStateEstimate.getV());
            String theta = String.valueOf(busStateEstimate.getTheta());
            if (V.length() > 12) {
                V = V.substring(0, 6) + "..." + V.substring(V.length() - 3);
            }
            if (theta.length() > 12) {
                theta = theta.substring(0, 6) + "..." + theta.substring(theta.length() - 3);
            }
            System.out.format("%-25s%-25s%-25s%n", busStateEstimate.getBusId(), V, theta);
        }
        System.out.println();
    }

    /**
     * Print all results, voltage state vector and network topology, with SI units for the former.
     * @param network The network on which was performed the state estimation
     */
    public void printAllResultsSi(Network network) {
        printStateVectorSi(network);
        printNetworkTopology();
    }

    public void printStateVectorSi(Network network) {
        System.out.println("Printing state estimate : ");
        // Print the table header
        System.out.format("%n%-25s%-25s%-25s%n", "BusID", "Estimated V (kV)", "Estimated angle (rad)");
        System.out.format("%-25s%-25s%-25s%n", "-----", "----------------", "---------------------");
        // Print state estimation for each bus
        for (BusStateEstimate busStateEstimate : stateVectorEstimate) {
            String V = String.valueOf(busStateEstimate.getV()
                    * network.getBusView().getBus(busStateEstimate.getBusId()).getVoltageLevel().getNominalV());
            String theta = String.valueOf(busStateEstimate.getTheta());
            if (V.length() > 12) {
                V = V.substring(0, 6) + "..." + V.substring(V.length() - 3);
            }
            if (theta.length() > 12) {
                theta = theta.substring(0, 6) + "..." + theta.substring(theta.length() - 3);
            }
            System.out.format("%-25s%-25s%-25s%n", busStateEstimate.getBusId(), V, theta);
        }
        System.out.println();
    }

    public void printNetworkTopology() {
        System.out.println("Printing network topology estimate : ");
        // Print the table header
        System.out.format("%n%-20s%-20s%-20s%-20s%n", "BranchID", "Was suspected", "Presumed status", "Estimated status");
        System.out.format("%-20s%-20s%-20s%-20s%n",   "--------", "-------------", "--------------", "----------------");
        // Print state estimation for each branch
        for (BranchStatusEstimate branchStatusEstimate : networkTopologyEstimate) {
            System.out.format("%-20s%-20s%-20s%-20s%n", branchStatusEstimate.getBranchId(), branchStatusEstimate.getIsSuspected(),
                    branchStatusEstimate.getPresumedStatus(), branchStatusEstimate.getEstimatedStatus());
        }
        System.out.println();
    }

    /**
     * Print all residuals (in SI) related to the measurements
     * @param knowledge The network on which was performed the state estimation
     */
    public void printResidualsSi(StateEstimatorKnowledge knowledge) {
        new ActivePowerFlowMeasures(knowledge.getActivePowerFlowMeasures(), this.measurementResiduals)
                .printWithResiduals();
        new ReactivePowerFlowMeasures(knowledge.getReactivePowerFlowMeasures(), this.measurementResiduals)
                .printWithResiduals();
        new ActivePowerInjectedMeasures(knowledge.getActivePowerInjectedMeasures(), this.measurementResiduals)
                .printWithResiduals();
        new ReactivePowerInjectedMeasures(knowledge.getReactivePowerInjectedMeasures(), this.measurementResiduals)
                .printWithResiduals();
        new VoltageMagnitudeMeasures(knowledge.getVoltageMagnitudeMeasures(), this.measurementResiduals)
                .printWithResiduals();
    }

    /**
     * Print all the indicators of the state estimation.
     */
    public void printIndicators() {
        printIndicator(runIndicators);
        printIndicator(networkIndicators);
    }

    /**
     * Print the given indicators in a beautiful box.
     * @param indicators The list of indicator to print.
     */
    public void printIndicator(List<Pair<String, String>> indicators) {
        String nameColumn1 = "Indicators";
        String nameColumn2 = "Values";

        // Calculate the width of columns based on the longest key and value of indicators
        int column1Width = Math.max(nameColumn1.length(),
                indicators.stream().mapToInt(entry -> entry.getFirst().length()).max().orElse(0));
        int column2Width = Math.max(nameColumn2.length(),
                indicators.stream().mapToInt(entry -> entry.getSecond().length()).max().orElse(0));

        String separator = "═".repeat(column1Width + column2Width + 5);

        // Print header box
        System.out.println("╔" + separator + "╗");
        System.out.println("║ " + nameColumn1 + " ".repeat(column1Width - nameColumn1.length()) // Column 1
                + " ║ " + nameColumn2 + " ".repeat(column2Width - nameColumn2.length()) + " ║"); // Column 2
        System.out.println("╠" + separator + "╣");

        // Print indicators
        indicators.forEach(pair -> {
            String key = pair.getFirst();
            String value = pair.getSecond();
            System.out.println("║ " + key + " ".repeat(column1Width - key.length()) // Column 1
                    + " ║ " + " ".repeat(column2Width - value.length()) + value + " ║"); // Column 2
        });

        // Print foot box
        System.out.println("╚" + separator + "╝");
    }

    /**
     * Compute statistics for voltage magnitude relative errors (%) of the state estimate, taking OLF results as ground truth
     * @param network The network on which was performed the state estimation
     * @return (meanVoltageError, stdVoltageError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError)
     */
    public List<Double> computeVoltageRelativeErrorStats(Network network) {
        long nbBuses = network.getBusView().getBusStream().count();
        // Initialize a list containing all errors
        List<Double> allErrors = new ArrayList<>();
        // Compute mean and standard deviation (and fill the list)
        double meanVoltageError = 0;
        double squaredVoltageError = 0;
        for (Bus bus : network.getBusView().getBuses()) {
            double tmpVoltageError = Math.abs((this.getBusStateEstimate(bus.getId()).getV() * bus.getVoltageLevel().getNominalV() - bus.getV())
                    / bus.getV()) * 100;
            meanVoltageError += tmpVoltageError;
            squaredVoltageError += Math.pow(tmpVoltageError, 2);
            allErrors.add(tmpVoltageError);
        }
        meanVoltageError = meanVoltageError / nbBuses;
        double stdVoltageError = Math.sqrt(squaredVoltageError/nbBuses - Math.pow(meanVoltageError, 2));
        // Compute 5th, 50th (median) and 95th percentiles
        Collections.sort(allErrors);
        double fifthPercentileError = percentile(allErrors, 5);
        double medianError = percentile(allErrors, 50);
        double ninetyFifthPercentileError = percentile(allErrors, 95);
        // Compute maximum
        double maxError = allErrors.get(allErrors.size()-1);
        return List.of(meanVoltageError, stdVoltageError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError);
    }

    /**
     * Compute statistics for voltage angle absolute errors (degrees) of the state estimate, taking OLF results as ground truth
     * @param network The network on which was performed the state estimation
     * @return (meanAngleError, stdVAngleError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError)
     */
    public List<Double> computeAngleDegreeErrorStats(Network network) {
        long nbBuses = network.getBusView().getBusStream().count();
        // Initialize a list containing all errors
        List<Double> allErrors = new ArrayList<>();
        // Compute mean and standard deviation (and fill the list)
        double meanAngleErrror = 0;
        double squaredAngleError = 0;
        for (Bus bus : network.getBusView().getBuses()) {
            double tmpAngleError = Math.abs(bus.getAngle() - Math.toDegrees(this.getBusStateEstimate(bus.getId()).getTheta()));
            meanAngleErrror += tmpAngleError;
            squaredAngleError += Math.pow(tmpAngleError, 2);
            allErrors.add(tmpAngleError);
        }
        meanAngleErrror = meanAngleErrror / nbBuses;
        double stdAngleError = Math.sqrt(squaredAngleError/nbBuses - Math.pow(meanAngleErrror, 2));
        // Compute 5th, 50th (median) and 95th percentiles
        Collections.sort(allErrors);
        double fifthPercentileError = percentile(allErrors, 5);
        double medianError = percentile(allErrors, 50);
        double ninetyFifthPercentileError = percentile(allErrors, 95);
        // Compute maximum
        double maxError = allErrors.get(allErrors.size()-1);
        return List.of(meanAngleErrror, stdAngleError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError);
    }

    /**
     * Compute statistics for active power flows (on both sides) relative errors (%) based on the state estimate, taking OLF results as ground truth
     * @param network The network on which was performed the state estimation
     * @return (meanPfError, stdPfError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError)
     */
    public List<Double> computeActivePowerFlowsRelativeErrorsStats(Network network) {
        // Define the threshold for minimal tolerated error on power flows
        double Pf_EPSILON = 1e-3;
        // Get number of active/reactive power flows (two sides for each branch)
        int nbPowerFlows = network.getBranchCount() * 2;
        // Initialize a list containing all errors
        List<Double> allErrors = new ArrayList<>();
        // Compute mean and standard deviation (and fill the list)
        double meanPfErrror = 0;
        double squaredPfError = 0;
        for (Branch branch : network.getBranches()) {
            BranchPowersEstimate branchPowersEstimate = this.getBranchPowersEstimate(branch.getId());
            double tmpPfErrorEnd1 = Math.abs(branchPowersEstimate.getActivePowerEnd1() - branch.getTerminal1().getP())
                                            / (Math.abs(branch.getTerminal1().getP()) + Pf_EPSILON) * 100;
            double tmpPfErrorEnd2 = Math.abs(branchPowersEstimate.getActivePowerEnd2() - branch.getTerminal2().getP())
                    / (Math.abs(branch.getTerminal2().getP()) + Pf_EPSILON) * 100;
            meanPfErrror += tmpPfErrorEnd1 + tmpPfErrorEnd2;
            squaredPfError += Math.pow(tmpPfErrorEnd1, 2) + Math.pow(tmpPfErrorEnd2, 2);
            allErrors.add(tmpPfErrorEnd1);
            allErrors.add(tmpPfErrorEnd2);
        }
        meanPfErrror = meanPfErrror / nbPowerFlows;
        double stdPfError = Math.sqrt(squaredPfError/nbPowerFlows - Math.pow(meanPfErrror, 2));
        // Compute 5th, 50th (median) and 95th percentiles
        Collections.sort(allErrors);
        double fifthPercentileError = percentile(allErrors, 5);
        double medianError = percentile(allErrors, 50);
        double ninetyFifthPercentileError = percentile(allErrors, 95);
        // Compute maximum
        double maxError = allErrors.get(allErrors.size()-1);
        return List.of(meanPfErrror, stdPfError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError);
    }

    /**
     * Compute statistics for reactive power flows (on both sides) relative errors (%) based on the state estimate, taking OLF results as ground truth
     * @param network The network on which was performed the state estimation
     * @return (meanQfError, stdQfError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError)
     */
    public List<Double> computeReactivePowerFlowsRelativeErrorsStats(Network network) {
        // Define the threshold for minimal tolerated error on power flows
        double Qf_EPSILON = 1e-3;
        // Get number of active/reactive power flows (two sides for each branch)
        int nbPowerFlows = network.getBranchCount() * 2;
        // Initialize a list containing all errors
        List<Double> allErrors = new ArrayList<>();
        // Compute mean and standard deviation (and fill the list)
        double meanQfErrror = 0;
        double squaredQfError = 0;
        for (Branch branch : network.getBranches()) {
            BranchPowersEstimate branchPowersEstimate = this.getBranchPowersEstimate(branch.getId());
            double tmpQfErrorEnd1 = Math.abs(branchPowersEstimate.getReactivePowerEnd1() - branch.getTerminal1().getQ())
                    / (Math.abs(branch.getTerminal1().getQ()) + Qf_EPSILON) * 100;
            double tmpQfErrorEnd2 = Math.abs(branchPowersEstimate.getReactivePowerEnd2() - branch.getTerminal2().getQ())
                    / (Math.abs(branch.getTerminal2().getQ()) + Qf_EPSILON) * 100;
            meanQfErrror += tmpQfErrorEnd1 + tmpQfErrorEnd2;
            squaredQfError += Math.pow(tmpQfErrorEnd1, 2) + Math.pow(tmpQfErrorEnd2, 2);
            allErrors.add(tmpQfErrorEnd1);
            allErrors.add(tmpQfErrorEnd2);
        }
        meanQfErrror = meanQfErrror / nbPowerFlows;
        double stdQfError = Math.sqrt(squaredQfError/nbPowerFlows - Math.pow(meanQfErrror, 2));
        // Compute 5th, 50th (median) and 95th percentiles
        Collections.sort(allErrors);
        double fifthPercentileError = percentile(allErrors, 5);
        double medianError = percentile(allErrors, 50);
        double ninetyFifthPercentileError = percentile(allErrors, 95);
        // Compute maximum
        double maxError = allErrors.get(allErrors.size()-1);
        return List.of(meanQfErrror, stdQfError, medianError, maxError, fifthPercentileError, ninetyFifthPercentileError);
    }

    public static double percentile(List<Double> array, double percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * array.size());
        return array.get(index-1);
    }

    // Getters
    public List<BusStateEstimate> getStateVectorEstimate() {
        return stateVectorEstimate;
    }

    public BusStateEstimate getBusStateEstimate(String busId) {
        for (BusStateEstimate busStateEstimate : stateVectorEstimate) {
            if (busStateEstimate.getBusId().equals(busId)) {
                return busStateEstimate;
            }
        }
        return null;
    }

    public List<BranchStatusEstimate> getNetworkTopologyEstimate() {
        return networkTopologyEstimate;
    }

    public BranchStatusEstimate getBranchStatusEstimate(String branchId) {
        for (BranchStatusEstimate branchStatusEstimate : networkTopologyEstimate) {
            if (branchStatusEstimate.getBranchId().equals(branchId)) {
                return branchStatusEstimate;
            }
        }
        return null;
    }

    public List<BranchPowersEstimate> getNetworkPowersEstimate() {
        return networkPowersEstimate;
    }

    public BranchPowersEstimate getBranchPowersEstimate(String branchId) {
        for (BranchPowersEstimate branchPowersEstimate : networkPowersEstimate) {
            if (branchPowersEstimate.getBranchID().equals(branchId)) {
                return branchPowersEstimate;
            }
        }
        return null;
    }

    public List<Pair<String, String>> getRunIndicators() {
        return runIndicators;
    }

    public List<Pair<String, String>> getNetworkIndicators() {
        return networkIndicators;
    }

    public Map<Integer, String> getMeasurementResiduals() {
        return measurementResiduals;
    }

}


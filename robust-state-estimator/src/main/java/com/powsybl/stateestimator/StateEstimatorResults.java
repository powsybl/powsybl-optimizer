/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator;

import com.powsybl.iidm.network.Bus;
import com.powsybl.stateestimator.parameters.StateEstimatorAmplIOFiles;
//import main.java.com.powsybl.stateestimator.parameters.output.modifications.StateBus;
//import main.java.com.powsybl.stateestimator.parameters.output.modifications.NetworkTopology;
import com.powsybl.iidm.network.Network;
import com.powsybl.stateestimator.parameters.output.estimates.BranchStatusEstimate;
import com.powsybl.stateestimator.parameters.output.estimates.BusStateEstimate;
import org.jgrapht.alg.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class StateEstimatorResults {
    private final boolean status;

    // State vector and network topology estimates (MINLP variables written and calculated in AMPL)
    List<BusStateEstimate> stateVectorEstimate;
    List<BranchStatusEstimate> networkTopologyEstimate;

    // Indicators returned by the AMPL code
    private final List<Pair<String, String>> runIndicators;
    private final List<Pair<String, String>> networkIndicators;

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
        this.networkIndicators = amplIOFiles.getNetworkIndicatorsOutput().getIndicators();

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
        System.out.println("Printing estimates on branch status : ");
        // Print the table header
        System.out.format("%n%-25s%-25s%n", "BranchID", "Estimated status");
        System.out.format("%-25s%-25s%n", "--------", "----------------");
        // Print state estimation for each branch
        for (BranchStatusEstimate branchStatusEstimate : networkTopologyEstimate) {
            System.out.format("%-25s%-25s%n", branchStatusEstimate.getBranchId(), branchStatusEstimate.getStatus());
        }
        System.out.println();
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

    public List<Pair<String, String>> getRunIndicators() {
        return runIndicators;
    }

    public List<Pair<String, String>> getNetworkIndicators() {
        return networkIndicators;
    }

}


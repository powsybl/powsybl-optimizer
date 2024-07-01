/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator;

import com.powsybl.stateestimator.parameters.StateEstimatorAmplIOFiles;
import com.powsybl.iidm.network.Network;
import com.powsybl.stateestimator.parameters.input.knowledge.*;
import com.powsybl.stateestimator.parameters.output.estimates.BranchPowersEstimate;
import com.powsybl.stateestimator.parameters.output.estimates.BranchStatusEstimate;
import com.powsybl.stateestimator.parameters.output.estimates.BusStateEstimate;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jgrapht.alg.util.Pair;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
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

    // Measurement estimates and residuals returned by the state estimation
    Map<Integer, ArrayList<String>> measurementEstimatesAndResiduals;

    // Bounds (p.u.) on voltages used in the state estimation : if one estimate reaches one of these bounds, a warning message is printed
    private final double MIN_ALLOWED_VOLTAGE = 0.5;
    private final double MAX_ALLOWED_VOLTAGE = 1.5;

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
        this.measurementEstimatesAndResiduals = amplIOFiles.getMeasurementEstimatesAndResidualsOutput().getMeasurementEstimatesAndResiduals();
        Objects.requireNonNull(runIndicators);
        this.runIndicators = new ArrayList<>();
        for (Map.Entry<String, String> entry : runIndicators.entrySet()) {
            this.runIndicators.add(Pair.of(entry.getKey(), entry.getValue()));
        }
    }

    public boolean getStatus() {
        return status;
    }

    public double getObjectiveFunctionValue() {
        Map<String, String> runIndicatorsMap = new HashMap<>();
        for (Pair<String, String> entry : this.runIndicators) {
            runIndicatorsMap.put(entry.getFirst(), entry.getSecond());
        }
        return Double.parseDouble(runIndicatorsMap.get("final_objective_function_value"));
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
        // Print a warning message if one of the estimated voltages equals MIN_ALLOWED_VOLTAGE or MAX_ALLOWED_VOLTAGE
        for (BusStateEstimate busStateEstimate : stateVectorEstimate) {
            if (busStateEstimate.getV() == MIN_ALLOWED_VOLTAGE) {
                System.out.printf("%n[WARNING] Estimated voltage at bus %s equals the minimum value allowed by the estimator (%f p.u.).  [WARNING]%n",
                        busStateEstimate.getBusId(), MIN_ALLOWED_VOLTAGE);
            }
            if (busStateEstimate.getV() == MAX_ALLOWED_VOLTAGE) {
                System.out.printf("%n[WARNING] Estimated voltage at bus %s equals the maximum value allowed by the estimator (%f p.u.).  [WARNING]%n",
                        busStateEstimate.getBusId(), MAX_ALLOWED_VOLTAGE);
            }
        }
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
        // Print a warning message if one of the estimated voltages equals MIN_ALLOWED_VOLTAGE or MAX_ALLOWED_VOLTAGE
        for (BusStateEstimate busStateEstimate : stateVectorEstimate) {
            if (busStateEstimate.getV() == MIN_ALLOWED_VOLTAGE) {
                System.out.printf("%n[WARNING] Estimated voltage at bus %s equals the minimum value allowed by the estimator (%f p.u.).  [WARNING]%n",
                        busStateEstimate.getBusId(), MIN_ALLOWED_VOLTAGE);
            }
            if (busStateEstimate.getV() == MAX_ALLOWED_VOLTAGE) {
                System.out.printf("%n[WARNING] Estimated voltage at bus %s equals the maximum value allowed by the estimator (%f p.u.).  [WARNING]%n",
                        busStateEstimate.getBusId(), MAX_ALLOWED_VOLTAGE);
            }
        }
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
     * Print all estimates and residuals (in SI) for all measurements
     * @param knowledge The knowledge object from which was performed the state estimation
     */
    public void printAllMeasurementEstimatesAndResidualsSi(StateEstimatorKnowledge knowledge) {
        new ActivePowerFlowMeasures(knowledge.getActivePowerFlowMeasures(), this.measurementEstimatesAndResiduals)
                .printWithEstimatesAndResiduals();
        new ReactivePowerFlowMeasures(knowledge.getReactivePowerFlowMeasures(), this.measurementEstimatesAndResiduals)
                .printWithEstimatesAndResiduals();
        new ActivePowerInjectedMeasures(knowledge.getActivePowerInjectedMeasures(), this.measurementEstimatesAndResiduals)
                .printWithEstimatesAndResiduals();
        new ReactivePowerInjectedMeasures(knowledge.getReactivePowerInjectedMeasures(), this.measurementEstimatesAndResiduals)
                .printWithEstimatesAndResiduals();
        new VoltageMagnitudeMeasures(knowledge.getVoltageMagnitudeMeasures(), this.measurementEstimatesAndResiduals)
                .printWithEstimatesAndResiduals();
    }

    /**
     * Export in a CSV file all estimates, residuals and normalized residuals (in SI) for all measurements
     * @param knowledge The knowledge object from which was performed the state estimation
     */
    public void exportAllMeasurementEstimatesAndResidualsSi(StateEstimatorKnowledge knowledge) {
        ActivePowerFlowMeasures activePowerFlowMeasures = new ActivePowerFlowMeasures(knowledge.getActivePowerFlowMeasures(), this.measurementEstimatesAndResiduals);
        ReactivePowerFlowMeasures reactivePowerFlowMeasures = new ReactivePowerFlowMeasures(knowledge.getReactivePowerFlowMeasures(), this.measurementEstimatesAndResiduals);
        ActivePowerInjectedMeasures activePowerInjectedMeasures = new ActivePowerInjectedMeasures(knowledge.getActivePowerInjectedMeasures(), this.measurementEstimatesAndResiduals);
        ReactivePowerInjectedMeasures reactivePowerInjectedMeasures = new ReactivePowerInjectedMeasures(knowledge.getReactivePowerInjectedMeasures(), this.measurementEstimatesAndResiduals);
        VoltageMagnitudeMeasures voltageMagnitudeMeasures = new VoltageMagnitudeMeasures(knowledge.getVoltageMagnitudeMeasures(), this.measurementEstimatesAndResiduals);

        // Initialize the dataframe that will store the results
        List<String> headers = List.of("MeasurementNumber", "MeasurementLocation",
                "Residual", "Normalized residual"
        );
        List<List<String>> data = new ArrayList<>();
        // Add a line for each measure
        for (var measure : activePowerFlowMeasures.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            int measurementNumber = measure.getKey();
            String measurementLocation = measure.getValue().get(1);
            double residual = Math.abs(Double.parseDouble(measure.getValue().get(7)));
            double standardDeviation = Math.sqrt(Double.parseDouble(measure.getValue().get(5)));
            double normalizedResidual = residual / standardDeviation;
            data.add(List.of(String.valueOf(measurementNumber), String.valueOf(measurementLocation),
                    String.valueOf(residual), String.valueOf(normalizedResidual)
            ));
        }
        for (var measure : reactivePowerFlowMeasures.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            int measurementNumber = measure.getKey();
            String measurementLocation = measure.getValue().get(1);
            double residual = Math.abs(Double.parseDouble(measure.getValue().get(7)));
            double standardDeviation = Math.sqrt(Double.parseDouble(measure.getValue().get(5)));
            double normalizedResidual = residual / standardDeviation;
            data.add(List.of(String.valueOf(measurementNumber), String.valueOf(measurementLocation),
                    String.valueOf(residual), String.valueOf(normalizedResidual)
            ));
        }
        for (var measure : activePowerInjectedMeasures.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            int measurementNumber = measure.getKey();
            String measurementLocation = measure.getValue().get(1);
            double residual = Math.abs(Double.parseDouble(measure.getValue().get(5)));
            double standardDeviation = Math.sqrt(Double.parseDouble(measure.getValue().get(3)));
            double normalizedResidual = residual / standardDeviation;
            data.add(List.of(String.valueOf(measurementNumber), String.valueOf(measurementLocation),
                    String.valueOf(residual), String.valueOf(normalizedResidual)
            ));
        }
        for (var measure : reactivePowerInjectedMeasures.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            int measurementNumber = measure.getKey();
            String measurementLocation = measure.getValue().get(1);
            double residual = Math.abs(Double.parseDouble(measure.getValue().get(5)));
            double standardDeviation = Math.sqrt(Double.parseDouble(measure.getValue().get(3)));
            double normalizedResidual = residual / standardDeviation;
            data.add(List.of(String.valueOf(measurementNumber), String.valueOf(measurementLocation),
                    String.valueOf(residual), String.valueOf(normalizedResidual)
            ));
        }
        for (var measure : voltageMagnitudeMeasures.getMeasuresWithEstimatesAndResiduals().entrySet()) {
            int measurementNumber = measure.getKey();
            String measurementLocation = measure.getValue().get(1);
            double residual = Math.abs(Double.parseDouble(measure.getValue().get(5)));
            double standardDeviation = Math.sqrt(Double.parseDouble(measure.getValue().get(3)));
            double normalizedResidual = residual / standardDeviation;
            data.add(List.of(String.valueOf(measurementNumber), String.valueOf(measurementLocation),
                    String.valueOf(residual), String.valueOf(normalizedResidual)
            ));
        }
        // Export the results in a CSV file
        try (FileWriter fileWriter = new FileWriter("EstimatesAndResiduals_Export.csv");
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

    /**
     * Print the main indicators on the state estimation.
     */
    public void printIndicators() {
        printIndicator(runIndicators);
        printIndicator(networkIndicators);
    }

    /**
     * Print the given indicators in a beautiful box.
     * @param indicators The list of indicator to print.
     */
    private void printIndicator(List<Pair<String, String>> indicators) {
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

    public Map<Integer, ArrayList<String>> getMeasurementEstimatesAndResiduals() {
        return measurementEstimatesAndResiduals;
    }

}


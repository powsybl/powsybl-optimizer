package com.powsybl.divergenceanalyser.parameters.output;

import com.powsybl.divergenceanalyser.parameters.DivergenceAnalyserAmplIOFiles;
import com.powsybl.divergenceanalyser.parameters.output.modifications.BranchPenalization;
import com.powsybl.divergenceanalyser.parameters.output.modifications.BusPenalization;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DivergenceAnalyserResults {
    private final boolean status;

    List<BusPenalization> busPenalization;
    List<BranchPenalization> branchPenalization;

    private final Map<String, String> networkIndicators;
    private final Map<String, String> runIndicators;

    /**
     * @param status      the final status of the Divergence Analysis run.
     * @param amplIOFiles a file interface to fetch output file information.
     * @param runIndicators  a standard map written by the Divergence Analysis ampl model.
     */
    public DivergenceAnalyserResults(boolean status, DivergenceAnalyserAmplIOFiles amplIOFiles, Map<String, String> runIndicators) {
        Objects.requireNonNull(amplIOFiles);
        this.status = status;
        this.busPenalization = amplIOFiles.getBusPenalizationOutput().getPenalisation();
        this.branchPenalization = amplIOFiles.getBranchModificationsOutput().getPenalisation();

        this.networkIndicators = amplIOFiles.getNetworkIndicatorsOutput().networkIndicators;
        this.runIndicators = Map.copyOf(Objects.requireNonNull(runIndicators));
    }

    public boolean getStatus() {
        return status;
    }

    /**
     * Print all penalization.
     */
    public void printPenalization(){
        printBusPenalization();
        printBranchPenalization();
    }

    /**
     * Print penalization on buses.
     */
    public void printBusPenalization(){
        for (BusPenalization penal : busPenalization) {
            penal.print();
        }
    }


    /**
     * Print penalization on branches.
     */
    public void printBranchPenalization(){
        for (BranchPenalization penal : branchPenalization) {
            penal.print();
        }
    }

    public void printIndicators(Map<String, String> indicators){
        String nameColumn1 = "Indicators";
        String nameColumn2 = "Values";

        // Calculate the width of columns based on the longest key and value of indicators
        int column1Width = Math.max(nameColumn1.length(),
                indicators.keySet().stream().mapToInt(String::length).max().orElse(0));
        int column2Width = Math.max(nameColumn2.length(),
                indicators.values().stream().mapToInt(String::length).max().orElse(0));

        String separator = "═".repeat(column1Width + column2Width + 5);

        // Print header box
        System.out.println("╔" + separator + "╗");
        System.out.println("║ " + nameColumn1 + " ".repeat(column1Width - nameColumn1.length()) // Column 1
                + " ║ " + nameColumn2 + " ".repeat(column2Width - nameColumn2.length()) + " ║"); // Column 2
        System.out.println("╠" + separator + "╣");

        // Print indicators
        indicators.forEach((key, value) -> System.out.println("║ " + key + " ".repeat(column1Width - key.length()) // Column 1
                + " ║ " + " ".repeat(column2Width - value.length()) + value + " ║")); // Column 2

        // Print foot box
        System.out.println("╚" + separator + "╝");
    }

    public Map<String, String> getRunIndicators() {
        return runIndicators;
    }
    public Map<String, String> getNetworkIndicators() {
        return networkIndicators;
    }

}


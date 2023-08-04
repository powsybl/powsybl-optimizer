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

    private final Map<String, String> indicators;

    /**
     * @param status      the final status of the Divergence Analysis run.
     * @param amplIOFiles a file interface to fetch output file information.
     * @param indicators  a standard map written by the Divergence Analysis ampl model.
     */
    public DivergenceAnalyserResults(boolean status, DivergenceAnalyserAmplIOFiles amplIOFiles, Map<String, String> indicators) {
        Objects.requireNonNull(amplIOFiles);
        this.status = status;
        this.busPenalization = amplIOFiles.getBusPenalizationOutput().getPenalisation();
        this.branchPenalization = amplIOFiles.getBranchModificationsOutput().getPenalisation();
        this.indicators = Map.copyOf(Objects.requireNonNull(indicators));
    }

    public boolean getStatus() {
        return status;
    }

    public Map<String, String> getIndicators() {
        return indicators;
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


    /**
     * Print indicators and their values.
     */
    public void printIndicators() {

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

}


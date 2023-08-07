package com.powsybl.divergenceanalyser;

import com.powsybl.divergenceanalyser.parameters.DivergenceAnalyserAmplIOFiles;
import com.powsybl.divergenceanalyser.parameters.output.modifications.BranchPenalization;
import com.powsybl.divergenceanalyser.parameters.output.modifications.BusPenalization;
import org.jgrapht.alg.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DivergenceAnalyserResults {
    private final boolean status;

    // Penalization calculated by the MINLP written in AMPL
    List<BusPenalization> busPenalization;
    List<BranchPenalization> branchPenalization;

    // Indicators returned by the AMPL code
    private final List<Pair<String, String>> runIndicators;
    private final List<Pair<String, String>> networkIndicators;
    private final List<Pair<String, String>> penalizationIndicators;

    /**
     * @param status      the final status of the Divergence Analysis run.
     * @param amplIOFiles a file interface to fetch output file information.
     * @param runIndicators  a standard map written by the Divergence Analysis ampl model.
     */
    public DivergenceAnalyserResults(boolean status, DivergenceAnalyserAmplIOFiles amplIOFiles, Map<String, String> runIndicators) {
        Objects.requireNonNull(amplIOFiles);
        this.status = status;
        this.busPenalization = amplIOFiles.getBusPenalizationOutput().getPenalization();
        this.branchPenalization = amplIOFiles.getBranchModificationsOutput().getPenalization();

        Objects.requireNonNull(runIndicators);
        this.runIndicators = new ArrayList<>();
        for(Map.Entry<String, String> entry : runIndicators.entrySet()) {
            this.runIndicators.add(Pair.of(entry.getKey(), entry.getValue()));
        }
        this.penalizationIndicators = amplIOFiles.getPenalizationIndicatorsOutput().getPenalizationIndicators();
        this.networkIndicators = amplIOFiles.getNetworkIndicatorsOutput().getNetworkIndicators();
    }

    public boolean getStatus() {
        return status;
    }

    /**
     * Print all penalization.
     */
    public void printPenalization() {
        printBusPenalization();
        printBranchPenalization();
    }

    /**
     * Print penalization on buses.
     */
    public void printBusPenalization() {
        for (BusPenalization penal : busPenalization) {
            penal.print();
        }
    }

    /**
     * Print penalization on branches.
     */
    public void printBranchPenalization() {
        for (BranchPenalization penal : branchPenalization) {
            penal.print();
        }
    }

    /**
     * Print all the indicators of the run.
     */
    public void printIndicators() {
        printIndicator(runIndicators);
        printIndicator(networkIndicators);
        printIndicator(penalizationIndicators);
    }

    /**
     * Print the given indicators in a beautiful box.
     * @param indicators the list of indicator to print.
     */
    public void printIndicator(List<Pair<String, String>> indicators){
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

    public List<Pair<String, String>> getRunIndicators() {
        return runIndicators;
    }

    public List<Pair<String, String>> getNetworkIndicators() {
        return networkIndicators;
    }

    public List<Pair<String, String>> getPenalizationIndicators() {
        return penalizationIndicators;
    }

}


package com.powsybl.divergenceanalyser.parameters.output;

import com.powsybl.divergenceanalyser.parameters.DivergenceAnalyserAMPLIOFiles;
import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DivergenceAnalyserResults {
    private final boolean status;

    List<BranchModificationsOutput.BranchModification> branchModifications;

    private final Map<String, String> indicators;


    /**
     * @param status      the final status of the Divergence Analysis run.
     * @param amplIOFiles a file interface to fetch output file information.
     * @param indicators  a standard map written by the Divergence Analysis ampl model.
     */
    public DivergenceAnalyserResults(boolean status, DivergenceAnalyserAMPLIOFiles amplIOFiles, Map<String, String> indicators) {
        Objects.requireNonNull(amplIOFiles);
        this.status = Objects.requireNonNull(status);
        this.branchModifications = amplIOFiles.getBranchModificationsOutput().getModificiations();
        this.indicators = Map.copyOf(Objects.requireNonNull(indicators));
    }

    public boolean getStatus() {
        return status;
    }

    public Map<String, String> getIndicators() {
        return indicators;
    }

    public void applyBranchModifications(Network network){

        for(BranchModificationsOutput.BranchModification modif : branchModifications){

            if (network.getLine(modif.getBranchId()) != null) {

                Line b = network.getLine(modif.getBranchId());

                double vNom2 = b.getTerminal2().getBusView().getConnectableBus().getVoltageLevel().getNominalV();
                double dePerUnit = AmplConstants.SB / (vNom2 * vNom2);

                b.setR(Math.cos(Math.PI / 2 - modif.getNewXi()) / (modif.getNewY() / dePerUnit));
                b.setX(Math.sin(Math.PI / 2 - modif.getNewXi()) / (modif.getNewY() / dePerUnit));
                b.setG1(modif.getNewG1() * dePerUnit);
                b.setB1(modif.getNewB1() * dePerUnit);
                b.setG2(modif.getNewG2() * dePerUnit);
                b.setB2(modif.getNewB2() * dePerUnit);

            } else if (network.getTieLine(modif.getBranchId()) != null){

                throw new IllegalAccessError("Modifications of tie lines not implemented yet.");

            } else if (network.getTwoWindingsTransformer(modif.getBranchId()) != null) {
                TwoWindingsTransformer twt = network.getTwoWindingsTransformer(modif.getBranchId());

                double vNom2 = twt.getTerminal2().getBusView().getConnectableBus().getVoltageLevel().getNominalV();
                double dePerUnit = AmplConstants.SB / (vNom2 * vNom2);

                // TODO : Must change the modifications that are applied when there is a 2wt
                if (twt.getPhaseTapChanger() != null) {
                    twt.getPhaseTapChanger().getCurrentStep().setR(Math.cos(Math.PI / 2 - modif.getNewXi()) / (modif.getNewY() / dePerUnit));
                    twt.getPhaseTapChanger().getCurrentStep().setX(Math.sin(Math.PI / 2 - modif.getNewXi()) / (modif.getNewY() / dePerUnit));
                }

                if (twt.getRatioTapChanger() != null) {
                    twt.getRatioTapChanger().getCurrentStep().setR(Math.cos(Math.PI / 2 - modif.getNewXi()) / (modif.getNewY() / dePerUnit));
                    twt.getRatioTapChanger().getCurrentStep().setX(Math.sin(Math.PI / 2 - modif.getNewXi()) / (modif.getNewY() / dePerUnit));
                }

                twt.setR(Math.cos(Math.PI / 2 - modif.getNewXi()) / (modif.getNewY() / dePerUnit));
                twt.setX(Math.sin(Math.PI / 2 - modif.getNewXi()) / (modif.getNewY() / dePerUnit));

                twt.setG(modif.getNewG1() * dePerUnit);
                twt.setB(modif.getNewB1() * dePerUnit);

            } else if (network.getThreeWindingsTransformer(modif.getBranchId()) != null){

                throw new IllegalAccessError("Modifications of 3wt not implemented yet.");

            } else if (network.getDanglingLine(modif.getBranchId()) != null){
                DanglingLine dl = network.getDanglingLine(modif.getBranchId());

                double vNom2 = dl.getTerminal().getVoltageLevel().getNominalV();
                double dePerUnit = AmplConstants.SB / (vNom2 * vNom2);

                dl.setR(Math.cos(Math.PI / 2 - modif.getNewXi()) / (modif.getNewY() / dePerUnit));
                dl.setX(Math.sin(Math.PI / 2 - modif.getNewXi()) / (modif.getNewY() / dePerUnit));
                dl.setG(modif.getNewG1() * dePerUnit);
                dl.setB(modif.getNewB1() * dePerUnit);

            }
        }
    }

    public void applyDivAnalysisResults(Network network){
        applyBranchModifications(network);
    }

    /**
     * Print indicators and their values.
     */
    public void printIndicators(){

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
                + " ║ " + nameColumn2 + " ".repeat(column2Width - nameColumn2.length())+ " ║"); // Column 2
        System.out.println("╠" + separator + "╣");

        // Print indicators
        indicators.forEach((key, value) -> System.out.println("║ " + key + " ".repeat(column1Width - key.length()) // Column 1
                + " ║ " + " ".repeat(column2Width - value.length()) + value + " ║")); // Column 2

        // Print foot box
        System.out.println("╚" + separator + "╝");
    }

}


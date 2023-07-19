package com.powsybl.divergenceanalyser.parameters.output;

import com.powsybl.divergenceanalyser.parameters.DivergenceAnalyserAMPLIOFiles;
import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.divergenceanalyser.parameters.output.modifications.BranchPenalisation;
import com.powsybl.iidm.network.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DivergenceAnalyserResults {
    private final boolean status;

    List<BranchPenalisation> branchPenalisation;

    private final Map<String, String> indicators;


    /**
     * @param status      the final status of the Divergence Analysis run.
     * @param amplIOFiles a file interface to fetch output file information.
     * @param indicators  a standard map written by the Divergence Analysis ampl model.
     */
    public DivergenceAnalyserResults(boolean status, DivergenceAnalyserAMPLIOFiles amplIOFiles, Map<String, String> indicators) {
        Objects.requireNonNull(amplIOFiles);
        this.status = Objects.requireNonNull(status);
        this.branchPenalisation = amplIOFiles.getBranchModificationsOutput().getPenalisation();
        this.indicators = Map.copyOf(Objects.requireNonNull(indicators));
    }

    public boolean getStatus() {
        return status;
    }

    public Map<String, String> getIndicators() {
        return indicators;
    }

    public void applyBranchPenalisation(Network network){

        for(BranchPenalisation penal : branchPenalisation){

            if (network.getLine(penal.getBranchId()) != null)
                applyLinePenalisation(network.getLine(penal.getBranchId()), penal);

            else if (network.getTieLine(penal.getBranchId()) != null)
                applyTieLinePenalisation(network.getTieLine(penal.getBranchId()), penal);

            else if (network.getTwoWindingsTransformer(penal.getBranchId()) != null)
                applyTwoWindingsTransformerPenalisation(network.getTwoWindingsTransformer(penal.getBranchId()), penal);

            else if (network.getThreeWindingsTransformer(penal.getBranchId()) != null)
                applyThreeWindingsTransformerPenalisation(network.getThreeWindingsTransformer(penal.getBranchId()), penal);

            else if (network.getDanglingLine(penal.getBranchId()) != null)
                applyDanglingLinePenalisation(network.getDanglingLine(penal.getBranchId()), penal);

            else throw new IllegalStateException("Branch not in the network");

        }
    }

    public void applyLinePenalisation(Line l, BranchPenalisation penal){
        double vNom2 = l.getTerminal2().getBusView().getConnectableBus().getVoltageLevel().getNominalV();
        double dePerUnit = AmplConstants.SB / (vNom2 * vNom2);

        // Update Impedance of the line
        if (penal.isYPenalised() || penal.isXiPenalised()){
            double angle = Math.PI / 2 - penal.getXi();
            double y = penal.getY() / dePerUnit;

            l.setR(Math.cos(angle) / y);
            l.setX(Math.sin(angle) / y);
        }

        // Update shunt on left side
        if (penal.isG1Penalised()) l.setG1(penal.getG1() * dePerUnit);
        if (penal.isB1Penalised()) l.setB1(penal.getB1() * dePerUnit);

        // Update shunt on right side
        if (penal.isG2Penalised()) l.setG2(penal.getG2() * dePerUnit);
        if (penal.isB2Penalised()) l.setB2(penal.getB2() * dePerUnit);
    }

    // TODO
    public void applyTieLinePenalisation(TieLine tl, BranchPenalisation penal) {
        return;
    }

    public void applyTwoWindingsTransformerPenalisation(TwoWindingsTransformer twt, BranchPenalisation penal) {

        double vNom2 = twt.getTerminal2().getBusView().getConnectableBus().getVoltageLevel().getNominalV();
        double dePerUnit = AmplConstants.SB / (vNom2 * vNom2);


        if (twt.getRatioTapChanger() != null && twt.getPhaseTapChanger() == null) {

            // Update rho value of rtc
            if (penal.isRhoPenalised()) twt.getRatioTapChanger().getCurrentStep().setRho(penal.getRho());

            // Update impedance of transformer
            if (penal.isYPenalised() || penal.isXiPenalised()){

                double angle = Math.PI / 2 - penal.getXi();
                double y = penal.getY() / dePerUnit;

                twt.getRatioTapChanger().getCurrentStep().setR(Math.cos(angle) / y);
                twt.getRatioTapChanger().getCurrentStep().setX(Math.sin(angle) / y);
            }
        } else if (twt.getRatioTapChanger() == null && twt.getPhaseTapChanger() != null){

            // Update transformer value pst
            if (penal.isRhoPenalised()) twt.getPhaseTapChanger().getCurrentStep().setRho(penal.getRho());
            if (penal.isAlphaPenalised()) twt.getPhaseTapChanger().getCurrentStep().setAlpha(penal.getAlpha());

            // Update impedance of transformer
            if (penal.isYPenalised() || penal.isXiPenalised()){

                double angle = Math.PI / 2 - penal.getXi();
                double y = penal.getY() / dePerUnit;

                twt.getPhaseTapChanger().getCurrentStep().setR(Math.cos(angle) / y);
                twt.getPhaseTapChanger().getCurrentStep().setX(Math.sin(angle) / y);
            }
        }

        // Update impedance of the line
        if (penal.isYPenalised() || penal.isXiPenalised()){

            double angle = Math.PI / 2 - penal.getXi();
            double y = penal.getY() / dePerUnit;

            twt.setR(Math.cos(angle) / y);
            twt.setX(Math.sin(angle) / y);
        }

        // Update shunt on left side
        if (penal.isG1Penalised()) twt.setG(penal.getG1() * dePerUnit);
        if (penal.isB1Penalised()) twt.setB(penal.getB1() * dePerUnit);
    }

    // TODO
    public void applyThreeWindingsTransformerPenalisation(ThreeWindingsTransformer t3wt, BranchPenalisation penal) {
        return;
    }

    public void applyDanglingLinePenalisation(DanglingLine dl, BranchPenalisation penal) {

        double vNom2 = dl.getTerminal().getVoltageLevel().getNominalV();
        double dePerUnit = AmplConstants.SB / (vNom2 * vNom2);

        if (penal.isYPenalised() || penal.isXiPenalised()){
            double angle = Math.PI / 2 - penal.getXi();
            double y = penal.getY() / dePerUnit;

            dl.setR(Math.cos(angle) / y);
            dl.setX(Math.sin(angle) / y);
        }

        if (penal.isG1Penalised()) dl.setG(penal.getG1() * dePerUnit);
        if (penal.isB1Penalised()) dl.setB(penal.getB1() * dePerUnit);
    }

    public void applyDivergenceAnalysisPenalisation(Network network){
        applyBranchPenalisation(network);
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


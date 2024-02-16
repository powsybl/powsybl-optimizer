/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser.parameters.output.modifications;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.iidm.network.*;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class BranchPenalization {

    String branchId;

    double slackRho;
    double slackY;
    double slackAlpha;
    double slackXi;
    double slackG1;
    double slackB1;
    double slackG2;
    double slackB2;

    double newRho;
    double newY;
    double newAlpha;
    double newXi;
    double newG1;
    double newB1;
    double newG2;
    double newB2;

    public BranchPenalization(String branchId, double slackRho, double slackY, double slackAlpha, double slackXi, double slackG1, double slackB1, double slackG2, double slackB2,
                              double newRho, double newY, double newAlpha, double newXi, double newG1, double newB1, double newG2, double newB2) {
        this.branchId = branchId;
        this.slackRho = slackRho;
        this.slackY = slackY;
        this.slackAlpha = slackAlpha;
        this.slackXi = slackXi;
        this.slackG1 = slackG1;
        this.slackB1 = slackB1;
        this.slackG2 = slackG2;
        this.slackB2 = slackB2;
        this.newRho = newRho;
        this.newY = newY;
        this.newAlpha = newAlpha;
        this.newXi = newXi;
        this.newG1 = newG1;
        this.newB1 = newB1;
        this.newG2 = newG2;
        this.newB2 = newB2;
    }

    public void printPu() {
        System.out.println("For branch " + getBranchId() + " : ");

        printRhoPu();
        printAlpha();
        printImpedancePu();
        printG1Pu();
        printB1Pu();
        printG2Pu();
        printB2Pu();
    }

    public void printSi(Network network) {

        if (network.getLine(getBranchId()) != null) {
            System.out.println("For line " + getBranchId() + " : ");
            Line l = network.getLine(getBranchId());
            double vNom2 = l.getTerminal2().getVoltageLevel().getNominalV();

            // Print only parameters penalized on lines
            printImpedanceSi(vNom2);
            printG1Si(vNom2);
            printB1Si(vNom2);
            printG2Si(vNom2);
            printB2Si(vNom2);

        } else if (network.getTieLine(getBranchId()) != null) {
            System.out.println("For tie line " + getBranchId() + " : ");
            TieLine tl = network.getTieLine(getBranchId());
            double vNom2 = tl.getTerminal2().getVoltageLevel().getNominalV();

            // Print only parameters penalized on tie lines
            printImpedanceSi(vNom2);
            printG1Si(vNom2);
            printB1Si(vNom2);
            printG2Si(vNom2);
            printB2Si(vNom2);

        } else if (network.getTwoWindingsTransformer(getBranchId()) != null) {
            System.out.println("For t2wt " + getBranchId() + " : ");
            TwoWindingsTransformer t2wt = network.getTwoWindingsTransformer(getBranchId());
            double vNom1 = t2wt.getTerminal1().getVoltageLevel().getNominalV();
            double vNom2 = t2wt.getTerminal2().getVoltageLevel().getNominalV();

            // Print only parameters penalized on two windings transformers
            printRhoSi(vNom1, vNom2);
            printAlpha();
            printImpedanceSi(vNom2);
            printG1Si(vNom2);
            printB1Si(vNom2);
            // TODO : check if G2/B2 are penalized in divergence analysis
            printG2Si(vNom2);
            printB2Si(vNom2);

        } else if (network.getThreeWindingsTransformer(getBranchId().replace("_leg[1-3]$", "")) != null) {
            System.out.println("For t3wt leg " + getBranchId() + " : ");
            ThreeWindingsTransformer t3wt = network.getThreeWindingsTransformer(getBranchId().replace("_leg[1-3]$", ""));
            // Get the corresponding leg of the t3wt
            int legNum = Integer.parseInt(String.valueOf(getBranchId().charAt(getBranchId().length() - 1)));
            ThreeWindingsTransformer.Leg leg = t3wt.getLegs().get(legNum - 1);

            double vNom1 = leg.getTerminal().getVoltageLevel().getNominalV();
            double vNom2 = t3wt.getRatedU0();

            printRhoSi(vNom1, vNom2);
            printAlpha();
            printImpedanceSi(vNom2);
            printG1Si(vNom2);
            printB1Si(vNom2);
            // TODO : check if G2/B2 are penalized in divergence analysis
            printG2Si(vNom2);
            printB2Si(vNom2);

        } else if (network.getDanglingLine(getBranchId()) != null) {
            System.out.println("For dangling line " + getBranchId() + " : ");
            DanglingLine dl = network.getDanglingLine(getBranchId());
            double vNom = dl.getTerminal().getVoltageLevel().getNominalV();

            // Print only parameters penalized on dangling lines
            printImpedanceSi(vNom);
            printG1Si(vNom);
            printB1Si(vNom);
            // TODO : check if G2/B2 are penalized in divergence analysis
            printG2Si(vNom);
            printB2Si(vNom);

        } else {
            System.out.println("Branch " + getBranchId() + " not found in network " + network.getId() + ", print in PU:");
            printRhoPu();
            printAlpha();
            printImpedancePu();
            printG1Pu();
            printB1Pu();
            printG2Pu();
            printB2Pu();
        }

    }

    void printAlpha() {
        if (Math.abs(getSlackAlpha()) > 0) {
            System.out.println("\t\tPhase shift (alpha) modification : ");
            System.out.println("\t\t\t\tNew value = " + getNewAlpha() + " rad / Old value = "
                    + (getNewAlpha() - getSlackAlpha()) + " rad (difference = " + getNewAlpha() + " rad)");
        }
    }

    void printRhoPu() {
        if (Math.abs(getSlackRho()) > 0) {
            System.out.println("\t\tTransformer ratio (rho) modification : ");
            System.out.println("\t\t\t\tNew value = " + getNewRho() + " p.u. / Old value = "
                    + (getNewRho() - getSlackRho()) + " p.u. (difference = " + getSlackRho() + " p.u.)");
        }
    }

    void printImpedancePu() {
        if (Math.abs(getSlackY()) > 0 || Math.abs(getSlackXi()) > 0) {
            System.out.println("\t\tImpedance (Z) modification : ");

            double newRPu = Math.cos(Math.PI / 2 - getNewXi()) / getNewY();
            double newXPu = Math.sin(Math.PI / 2 - getNewXi()) / getNewY();
            double oldRPu = Math.cos(Math.PI / 2 - (getNewXi() - getSlackXi())) / (getNewY() - getSlackY());
            double oldXPu = Math.sin(Math.PI / 2 - (getNewXi() - getSlackXi())) / (getNewY() - getSlackY());

            System.out.println("\t\t\t\tNew value of R = " + newRPu + " p.u. / Old value = "
                    + oldRPu + " p.u. (difference = " + Math.abs(newRPu - oldRPu) + " p.u.)");
            System.out.println("\t\t\t\tNew value of X = " + newXPu + " p.u. / Old value = "
                    + oldXPu + " p.u. (difference = " + Math.abs(newXPu - oldXPu) + " p.u.)");
        }
    }

    void printG1Pu() {
        if (Math.abs(getSlackG1()) > 0) {
            System.out.println("\t\tShunt 1 conductance (G1) modification : ");
            System.out.println("\t\t\t\tNew value = " + getNewG1() + " p.u. / Old value = "
                    + (getNewG1() - getSlackG1()) + " p.u. (difference = " + getNewG1() + " p.u.)");
        }
    }

    void printB1Pu() {
        if (Math.abs(getSlackB1()) > 0) {
            System.out.println("\t\tShunt 1 susceptance (B1) modification : ");
            System.out.println("\t\t\t\tNew value = " + getB1() + " p.u. / Old value = "
                    + (getB1() - getSlackB1()) + " p.u. (difference = " + getB1() + " p.u.)");
        }
    }

    void printG2Pu() {
        if (Math.abs(getSlackG2()) > 0) {
            System.out.println("\t\tShunt 2 conductance (G2) modification : ");
            System.out.println("\t\t\t\tNew value = " + getNewG2() + " p.u. / Old value = "
                    + (getNewG2() - getSlackG2()) + " p.u. (difference = " + getNewG2() + " p.u.)");
        }
    }

    void printB2Pu() {
        if (Math.abs(getSlackB2()) > 0) {
            System.out.println("\t\tShunt 2 susceptance (B2) modification : ");
            System.out.println("\t\t\t\tNew value = " + getNewB2() + " p.u. / Old value = "
                    + (getNewB2() - getSlackB2()) + " p.u. (difference = " + getNewB2() + " p.u.)");
        }
    }

    void printRhoSi(double vNom1, double vNom2) {
        if (Math.abs(getSlackRho()) > 0) {
            double newRho = getNewRho() * vNom2 / vNom1;
            double oldRho = (getNewRho() - getSlackRho()) * vNom2 / vNom1;

            System.out.println("\t\tTransformer ratio (rho) modification : ");
            System.out.println("\t\t\t\tNew value = " + newRho + " / Old value = "
                    + oldRho + " (difference = " + Math.abs(newRho - oldRho) + ")");
        }
    }

    void printImpedanceSi(double vNom2) {
        if (Math.abs(getSlackY()) > 0 || Math.abs(getSlackXi()) > 0) {
            System.out.println("\t\tImpedance (Z) modification : ");

            double dePerUnit = AmplConstants.SB / (vNom2 * vNom2);
            double newRPu = Math.cos(Math.PI / 2 - getNewXi()) / (getNewY() / dePerUnit);
            double newXPu = Math.sin(Math.PI / 2 - getNewXi()) / (getNewY() / dePerUnit);
            double oldRPu = Math.cos(Math.PI / 2 - (getNewXi() - getSlackXi())) / ((getNewY() - getSlackY()) / dePerUnit);
            double oldXPu = Math.sin(Math.PI / 2 - (getNewXi() - getSlackXi())) / ((getNewY() - getSlackY()) / dePerUnit);

            System.out.println("\t\t\t\tNew value of R = " + newRPu + " Ω / Old value = "
                    + oldRPu + " Ω (difference = " + Math.abs(newRPu - oldRPu) + " Ω)");
            System.out.println("\t\t\t\tNew value of X = " + newXPu + " Ω / Old value = "
                    + oldXPu + " Ω (difference = " + Math.abs(newXPu - oldXPu) + " Ω)");
        }
    }

    void printG1Si(double vNom2) {
        if (Math.abs(getSlackG1()) > 0) {
            double dePerUnit = AmplConstants.SB / (vNom2 * vNom2);
            double newG1 = getNewG1() * dePerUnit;
            double oldG1 = (getNewG1() - getSlackG1()) * dePerUnit;

            System.out.println("\t\tShunt 1 conductance (G1) modification : ");
            System.out.println("\t\t\t\tNew value = " + newG1 + " S / Old value = "
                    + oldG1 + " S (difference = " + Math.abs(newG1 - oldG1) + " S)");
        }
    }

    void printB1Si(double vNom2) {
        if (Math.abs(getSlackB1()) > 0) {
            double dePerUnit = AmplConstants.SB / (vNom2 * vNom2);
            double newB1 = getB1() * dePerUnit;
            double oldB1 = (getB1() - getSlackB1()) * dePerUnit;

            System.out.println("\t\tShunt 1 susceptance (B1) modification : ");
            System.out.println("\t\t\t\tNew value = " + newB1 + " S / Old value = "
                    + oldB1 + " S (difference = " + Math.abs(newB1 - oldB1) + " S)");
        }
    }

    void printG2Si(double vNom2) {
        if (Math.abs(getSlackG2()) > 0) {
            double dePerUnit = AmplConstants.SB / (vNom2 * vNom2);
            double newG2 = getNewG2() * dePerUnit;
            double oldG2 = (getNewG2() - getSlackG2()) * dePerUnit;

            System.out.println("\t\tShunt 2 conductance (G2) modification : ");
            System.out.println("\t\t\t\tNew value = " + newG2 + " S / Old value = "
                    + oldG2 + " S (difference = " + Math.abs(newG2 - oldG2) + " S)");
        }
    }

    void printB2Si(double vNom2) {
        if (Math.abs(getSlackB2()) > 0) {
            double dePerUnit = AmplConstants.SB / (vNom2 * vNom2);
            double newB2 = getNewB2() * dePerUnit;
            double oldB2 = (getNewB2() - getSlackB2()) * dePerUnit;

            System.out.println("\t\tShunt 2 susceptance (B2) modification : ");
            System.out.println("\t\t\t\tNew value = " + newB2 + " S / Old value = "
                    + oldB2 + " S (difference = " + Math.abs(newB2 - oldB2) + " S)");
        }
    }

    public String getBranchId() {
        return branchId;
    }

    public double getSlackRho() {
        return slackRho;
    }

    public double getSlackY() {
        return slackY;
    }

    public double getSlackAlpha() {
        return slackAlpha;
    }

    public double getSlackXi() {
        return slackXi;
    }

    public double getSlackG1() {
        return slackG1;
    }

    public double getSlackB1() {
        return slackB1;
    }

    public double getSlackG2() {
        return slackG2;
    }

    public double getSlackB2() {
        return slackB2;
    }

    public double getNewRho() {
        return newRho;
    }

    public double getNewY() {
        return newY;
    }

    public double getNewAlpha() {
        return newAlpha;
    }

    public double getNewXi() {
        return newXi;
    }

    public double getNewG1() {
        return newG1;
    }

    public double getB1() {
        return newB1;
    }

    public double getNewG2() {
        return newG2;
    }

    public double getNewB2() {
        return newB2;
    }
}

package com.powsybl.divergenceanalyser.parameters.output.modifications;

public class BranchPenalization {

    String branchId;

    boolean isRhoPenalised;
    boolean isYPenalised;
    boolean isAlphaPenalised;
    boolean isXiPenalised;
    boolean isG1Penalised;
    boolean isB1Penalised;
    boolean isG2Penalised;
    boolean isB2Penalised;

    double slackRho;
    double slackY;
    double slackAlpha;
    double slackXi;
    double slackG1;
    double slackB1;
    double slackG2;
    double slackB2;

    double rho;
    double y;
    double alpha;
    double xi;
    double g1;
    double b1;
    double g2;
    double b2;

    public BranchPenalization(String branchId, boolean isRhoPenalised, boolean isYPenalised, boolean isAlphaPenalised, boolean isXiPenalised, boolean isG1Penalised, boolean isB1Penalised, boolean isG2Penalised, boolean isB2Penalised,
                              double slackRho, double slackY, double slackAlpha, double slackXi, double slackG1, double slackB1, double slackG2, double slackB2,
                              double rho, double y, double alpha, double xi, double g1Value, double b1Value, double g2Value, double b2Value) {
        this.branchId = branchId;
        this.isRhoPenalised = isRhoPenalised;
        this.isYPenalised = isYPenalised;
        this.isAlphaPenalised = isAlphaPenalised;
        this.isXiPenalised = isXiPenalised;
        this.isG1Penalised = isG1Penalised;
        this.isB1Penalised = isB1Penalised;
        this.isG2Penalised = isG2Penalised;
        this.isB2Penalised = isB2Penalised;
        this.slackRho = slackRho;
        this.slackY = slackY;
        this.slackAlpha = slackAlpha;
        this.slackXi = slackXi;
        this.slackG1 = slackG1;
        this.slackB1 = slackB1;
        this.slackG2 = slackG2;
        this.slackB2 = slackB2;
        this.rho = rho;
        this.y = y;
        this.alpha = alpha;
        this.xi = xi;
        this.g1 = g1Value;
        this.b1 = b1Value;
        this.g2 = g2Value;
        this.b2 = b2Value;
    }

    public void print() {
        System.out.println("For branch " + getBranchId() + " : ");

        if (isRhoPenalised()) {
            System.out.println("New rho value = " + getRho()
                                + " (slack = " + getSlackRho() + ")");
        }

        if (isAlphaPenalised()) {
            System.out.println("New alpha value = " + getAlpha()
                    + " (slack = " + getSlackAlpha() + ")");
        }

        if (isYPenalised()) {
            System.out.println("New Y value = " + getY()
                    + " (slack = " + getSlackY() + ")");
        }

        if (isXiPenalised()) {
            System.out.println("New Xi value = " + getXi()
                    + " (slack = " + getSlackXi() + ")");
        }

        if (isG1Penalised()) {
            System.out.println("New G1 value = " + getG1()
                    + " (slack = " + getSlackG1() + ")");
        }

        if (isB1Penalised()) {
            System.out.println("New B1 value = " + getB1()
                    + " (slack = " + getSlackB1() + ")");
        }

        if (isG2Penalised()) {
            System.out.println("New G2 value = " + getG2()
                    + " (slack = " + getSlackG2() + ")");
        }

        if (isB2Penalised()) {
            System.out.println("New B2 value = " + getB2()
                    + " (slack = " + getSlackB2() + ")");
        }
    }

    public String getBranchId() {
        return branchId;
    }

    public boolean isRhoPenalised() {
        return isRhoPenalised;
    }

    public boolean isYPenalised() {
        return isYPenalised;
    }

    public boolean isAlphaPenalised() {
        return isAlphaPenalised;
    }

    public boolean isXiPenalised() {
        return isXiPenalised;
    }

    public boolean isG1Penalised() {
        return isG1Penalised;
    }

    public boolean isB1Penalised() {
        return isB1Penalised;
    }

    public boolean isG2Penalised() {
        return isG2Penalised;
    }

    public boolean isB2Penalised() {
        return isB2Penalised;
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

    public double getRho() {
        return rho;
    }

    public double getY() {
        return y;
    }

    public double getAlpha() {
        return alpha;
    }

    public double getXi() {
        return xi;
    }

    public double getG1() {
        return g1;
    }

    public double getB1() {
        return b1;
    }

    public double getG2() {
        return g2;
    }

    public double getB2() {
        return b2;
    }
}

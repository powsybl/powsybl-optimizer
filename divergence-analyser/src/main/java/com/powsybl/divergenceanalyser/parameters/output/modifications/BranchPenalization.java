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

    double rho;
    double y;
    double alpha;
    double xi;
    double g1Value;
    double b1Value;
    double g2Value;
    double b2Value;

    public BranchPenalization(String id, boolean isRhoPenalised, boolean isYPenalised, boolean isAlphaPenalised,
                              boolean isXiPenalised, boolean isG1Penalised, boolean isB1Penalised, boolean isG2Penalised,
                              boolean isB2Penalised, double rho, double y, double alpha, double xi, double g1Value,
                              double b1Value, double g2Value, double b2Value) {
        this.branchId = id;

        this.isRhoPenalised = isRhoPenalised;
        this.isAlphaPenalised = isAlphaPenalised;
        this.isYPenalised = isYPenalised;
        this.isXiPenalised = isXiPenalised;
        this.isG1Penalised = isG1Penalised;
        this.isB1Penalised = isB1Penalised;
        this.isG2Penalised = isG2Penalised;
        this.isB2Penalised = isB2Penalised;

        this.rho = rho;
        this.y = y;
        this.alpha = alpha;
        this.xi = xi;
        this.g1Value = g1Value;
        this.b1Value = b1Value;
        this.g2Value = g2Value;
        this.b2Value = b2Value;
    }

    public void print(){
        System.out.println("For branch " + getBranchId() + " : ");

        if (isRhoPenalised()) {
            System.out.println("New rho value = " + getRho());
        }

        if (isAlphaPenalised()) {
            System.out.println("New alpha value = " + getAlpha());
        }

        if (isYPenalised()) {
            System.out.println("New Y value = " + getY());
        }

        if (isXiPenalised()) {
            System.out.println("New Xi value = " + getXi());
        }

        if (isG1Penalised()) {
            System.out.println("New G1 value = " + getG1());
        }

        if (isB1Penalised()) {
            System.out.println("New B1 value = " + getB1());
        }

        if (isG2Penalised()) {
            System.out.println("New G2 value = " + getG2());
        }

        if (isB2Penalised()) {
            System.out.println("New B2 value = " + getB2());
        }
    }

    public String getBranchId() {
        return branchId;
    }

    public boolean isRhoPenalised() {
        return isRhoPenalised;
    }

    public double getRho() {
        return rho;
    }

    public boolean isAlphaPenalised() {
        return isAlphaPenalised;
    }

    public double getAlpha() {
        return alpha;
    }

    public boolean isYPenalised() {
        return isYPenalised;
    }

    public double getY() {
        return y;
    }

    public boolean isXiPenalised() {
        return isXiPenalised;
    }

    public double getXi() {
        return xi;
    }

    public boolean isG1Penalised() {
        return isG1Penalised;
    }

    public double getG1() {
        return g1Value;
    }

    public boolean isB1Penalised() {
        return isB1Penalised;
    }

    public double getB1() {
        return b1Value;
    }

    public boolean isG2Penalised() {
        return isG2Penalised;
    }

    public double getG2() {
        return g2Value;
    }

    public boolean isB2Penalised() {
        return isB2Penalised;
    }

    public double getB2() {
        return b2Value;
    }
}

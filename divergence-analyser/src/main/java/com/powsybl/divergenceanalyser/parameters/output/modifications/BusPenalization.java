package com.powsybl.divergenceanalyser.parameters.output.modifications;

public class BusPenalization {

    String busId;
    boolean isTargetVPenalized;
    double newTargetV;

    public BusPenalization(String busId, boolean isTargetVPenalized, double newTargetV) {
        this.busId = busId;
        this.isTargetVPenalized = isTargetVPenalized;
        this.newTargetV = newTargetV;
    }

    public void print() {
        System.out.println("For bus " + getBusId() + " : ");

        if (isTargetVPenalized()) {
            System.out.println("New target V = " + getNewTargetV());
        }
    }

    public String getBusId() {
        return busId;
    }

    public boolean isTargetVPenalized() {
        return isTargetVPenalized;
    }

    public double getNewTargetV() {
        return newTargetV;
    }
}

package com.powsybl.divergenceanalyser.parameters.output.modifications;

public class BusPenalization {

    String busId;
    boolean isTargetVPenalized;
    double slackTargetV;
    double newTargetV;

    public BusPenalization(String busId, boolean isTargetVPenalized, double slackTargetV, double newTargetV) {
        this.busId = busId;
        this.isTargetVPenalized = isTargetVPenalized;
        this.slackTargetV = slackTargetV;
        this.newTargetV = newTargetV;
    }

    public void print() {
        System.out.println("For bus " + getBusId() + " : ");

        if (isTargetVPenalized()) {
            System.out.println("New target V = " + getNewTargetV()
                    + " (slack = " + getSlackTargetV() + ")");
        }
    }

    public String getBusId() {
        return busId;
    }

    public boolean isTargetVPenalized() {
        return isTargetVPenalized;
    }

    public double getSlackTargetV() {
        return slackTargetV;
    }

    public double getNewTargetV() {
        return newTargetV;
    }
}

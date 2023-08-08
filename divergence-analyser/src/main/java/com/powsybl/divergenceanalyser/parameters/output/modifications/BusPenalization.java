/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser.parameters.output.modifications;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
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

/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser.parameters.output.modifications;

import com.powsybl.iidm.network.Network;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class BusPenalization {

    String busId;
    double slackTargetV;
    double newTargetV;

    public BusPenalization(String busId, double slackTargetV, double newTargetV) {
        this.busId = busId;
        this.slackTargetV = slackTargetV;
        this.newTargetV = newTargetV;
    }

    public void printPu() {
        System.out.println("For bus " + getBusId() + " : ");
        if (Math.abs(getSlackTargetV()) > 0) {
            System.out.println("\tTarget V modification : ");
            System.out.println("\t\tNew Value = " + getNewTargetV() + " p.u. / Old value = "
                    + (getNewTargetV() + getSlackTargetV()) + " p.u. (difference = " + getSlackTargetV() + " p.u.)");
        }
    }

    public void printSi(Network network) {
        double nomV = network.getBusView().getBus(getBusId()).getVoltageLevel().getNominalV();

        System.out.println("For bus " + getBusId() + " : ");
        if (Math.abs(getSlackTargetV()) > 0) {
            System.out.println("\tTarget V modification : ");
            double targetV = getNewTargetV() * nomV;
            System.out.println("\t\tNew value = " + targetV + " kV / Old value = "
                    + (targetV + getSlackTargetV() * nomV) + " kV (difference = " + targetV + " kV)");
        }
    }

    public String getBusId() {
        return busId;
    }

    public double getSlackTargetV() {
        return slackTargetV;
    }

    public double getNewTargetV() {
        return newTargetV;
    }
}

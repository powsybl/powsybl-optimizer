/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.output.estimates;

import com.powsybl.iidm.network.Network;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class BusStateEstimate {

    String busId;
    double V; // in p.u.
    double theta; // in radians

    public BusStateEstimate(String busId, double V, double theta) {
        this.busId = busId;
        this.V = V;
        this.theta = theta;
    }

    public void printPu() {
        System.out.println("\nState estimation for bus " + getBusId() + " : ");
        System.out.println("V = " + getV() + " p.u. , theta = " + getTheta() + " rad");
    }

    public void printSi(Network network) {
        double  nomV = network.getBusView().getBus(this.getBusId()).getVoltageLevel().getNominalV();
        System.out.println("\nState estimation for bus " + this.getBusId() + " : ");
        System.out.println("V = " + this.getV() * nomV + " kV , theta = " + this.getTheta() + " rad");
    }

    public String getBusId() {
        return busId;
    }

    public double getV() {
        return V;
    }

    public double getTheta() {
        return theta;
    }
}

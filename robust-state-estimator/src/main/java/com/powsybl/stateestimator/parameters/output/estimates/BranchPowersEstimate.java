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
public class BranchPowersEstimate {

    String branchID;
    String firstBusID;
    String secondBusID;
    double activePowerEnd1;
    double activePowerEnd2;
    double reactivePowerEnd1;
    double reactivePowerEnd2;

    public BranchPowersEstimate(String branchID, String firstBusID, String secondBusID,
                                double activePowerEnd1, double activePowerEnd2, double reactivePowerEnd1, double reactivePowerEnd2) {
        this.branchID = branchID;
        this.firstBusID = firstBusID;
        this.secondBusID = secondBusID;
        this.activePowerEnd1 = activePowerEnd1;
        this.activePowerEnd2 = activePowerEnd2;
        this.reactivePowerEnd1 = reactivePowerEnd1;
        this.reactivePowerEnd2 = reactivePowerEnd2;
    }

    public String getBranchID() {
        return branchID;
    }

    public String getFirstBusID() {
        return firstBusID;
    }

    public String getSecondBusID() {
        return secondBusID;
    }

    public double getActivePowerEnd1() {
        return activePowerEnd1;
    }

    public double getActivePowerEnd2() {
        return activePowerEnd2;
    }

    public double getReactivePowerEnd1() {
        return reactivePowerEnd1;
    }

    public double getReactivePowerEnd2() {
        return reactivePowerEnd2;
    }

}

/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.output.estimates;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class BranchStatusEstimate {

    String branchId;
    String isSuspected;
    String presumedStatus;
    String estimatedStatus;

    public BranchStatusEstimate(String branchId, String isSuspected, String presumedStatus, String estimatedStatus) {
        this.branchId = branchId;
        this.isSuspected = isSuspected;
        this.presumedStatus = presumedStatus;
        this.estimatedStatus = estimatedStatus;
    }

    public void print() {
        System.out.println("\nEstimated state of branch " + getBranchId() + " : " + getEstimatedStatus());
    }


    public String getBranchId() {
        return branchId;
    }

    public String getIsSuspected() {
        return isSuspected;
    }

    public String getPresumedStatus() {
        return presumedStatus;
    }

    public String getEstimatedStatus() {
        return estimatedStatus;
    }
}

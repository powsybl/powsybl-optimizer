/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.output.estimates;

import java.util.ArrayList;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class BranchStatusEstimate {

    String branchId;
    String status;

    public BranchStatusEstimate(String branchId, String status) {
        this.branchId = branchId;
        this.status = status;
    }

    public void print() {
        System.out.println("\nEstimated state of branch " + getBranchId() + " : " + getStatus());
    }


    public String getBranchId() {
        return branchId;
    }

    public String getStatus() {
        return status;
    }
}

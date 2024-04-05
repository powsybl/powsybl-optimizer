/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.input.options;

import java.util.HashMap;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class StateEstimatorOptions {

    // About MINLP options
    public static final int DEFAULT_SOLVING_MODE = 2; //0;
    public static final int DEFAULT_MAX_TIME_SOLVING = 120;
    public static final int DEFAULT_MAX_TOPOLOGY_CHANGES = 5;
    HashMap<String, Integer> solvingOptions = new HashMap<>();

    public StateEstimatorOptions() {
        setSolvingMode(DEFAULT_SOLVING_MODE);
        setMaxTimeSolving(DEFAULT_MAX_TIME_SOLVING);
        setMaxNbTopologyErrors(DEFAULT_MAX_TOPOLOGY_CHANGES);
    }

    public HashMap<String, Integer> getSolvingOptions() {
        return solvingOptions;
    }

    /**
     * @param solvingMode The solving mode of the solver used for the state estimation (corresponds to Knitro "mip_intvar_strategy")
     * @return The object on which the method is applied.
     */
    public StateEstimatorOptions setSolvingMode(int solvingMode) {
        if (solvingMode < 0 || solvingMode > 2) {
            throw new IllegalArgumentException("Solving mode must be in [0, 1, 2].");
        }
        solvingOptions.put("solving_mode", solvingMode);
        return this;
    }

    /**
     * Put solving mode to 2
     * @return The object on which the method is applied.
     */
    public StateEstimatorOptions setResolutionMPEC() {
        return setSolvingMode(2);
    }

    /**
     * Put solving mode to 1.
     * @return The object on which the method is applied.
     */
    public StateEstimatorOptions setResolutionNlp() {
        return setSolvingMode(1);
    }

    /**
     * Put solving mode to 0.
     * @return The object on which the method is applied.
     */
    public StateEstimatorOptions setResolutionMinlp() {
        return setSolvingMode(0);
    }

    /**
     * @param maxTimeSolving The maximum time the solver is allowed to run.
     * @return The object on which the method is applied.
     */
    public StateEstimatorOptions setMaxTimeSolving(int maxTimeSolving) {
        if (maxTimeSolving <= 0) {
            throw new IllegalArgumentException("Max time solving must be > 0.");
        }
        solvingOptions.put("max_time_solving", maxTimeSolving);
        return this;
    }

    /**
     * @param maxNbTopologyErrors The maximum number of branches status the solver is allowed to switch
     * @return The object on which the method is applied.
     */
    public StateEstimatorOptions setMaxNbTopologyErrors(int maxNbTopologyErrors) {
        if (maxNbTopologyErrors < 0) {
            throw new IllegalArgumentException("Maximum number of topology errors must be >= 0.");
        }
        solvingOptions.put("max_nb_topology_errors", maxNbTopologyErrors);
        return this;
    }

}

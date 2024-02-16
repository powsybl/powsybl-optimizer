/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser.parameters.input;

import java.util.HashMap;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class DivergenceAnalyserParameters {

    // About penalization
    public static final boolean DEFAULT_PENAL_ACTIVATION = false;
    HashMap<String, Integer> penalizationOptions = new HashMap<>();

    // About MINLP options
    public static final int DEFAULT_SOLVING_MODE = 0;
    public static final int DEFAULT_MAX_TIME_SOLVING = 120;
    HashMap<String, Integer> solvingOptions = new HashMap<>();

    public DivergenceAnalyserParameters() {
        setAllPenalization(DEFAULT_PENAL_ACTIVATION);
        setSolvingMode(DEFAULT_SOLVING_MODE);
        setMaxTimeSolving(DEFAULT_MAX_TIME_SOLVING);
    }

    public void setAllPenalization(boolean isActivated) {
        setTargetVUnitsPenal(isActivated);
        setTargetVSvcPenal(isActivated);
        setYPenal(isActivated);
        setXiPenal(isActivated);
        setRhoTransformerPenal(isActivated);
        setAlphaPSTPenal(isActivated);
        setG1Penal(isActivated);
        setG2Penal(isActivated);
        setB1Penal(isActivated);
        setB2Penal(isActivated);
    }

    public HashMap<String, Integer> getPenalizationOptions() {
        return penalizationOptions;
    }

    public HashMap<String, Integer> getSolvingOptions() {
        return solvingOptions;
    }

    /**
     * @param isActivated boolean to activate/inactive penalization of units target V.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setTargetVUnitsPenal(boolean isActivated) {
        penalizationOptions.put("target_v_units", isActivated ? 1 : 0);
        return this;
    }

    /**
     * @param isActivated boolean to activate/inactive of penalization svc target V.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setTargetVSvcPenal(boolean isActivated) {
        penalizationOptions.put("target_v_svc", isActivated ? 1 : 0);
        return this;
    }

    /**
     * @param isActivated boolean to activate/inactive penalization of transformer ratio.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setRhoTransformerPenal(boolean isActivated) {
        penalizationOptions.put("rho_transformer", isActivated ? 1 : 0);
        return this;
    }

    /**
     * @param isActivated boolean to activate/inactive penalization of phase shifts.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setAlphaPSTPenal(boolean isActivated) {
        penalizationOptions.put("phase_shift", isActivated ? 1 : 0);
        return this;
    }

    /**
     * @param isActivated boolean to activate/inactive penalization of admittance.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setYPenal(boolean isActivated) {
        penalizationOptions.put("admittance", isActivated ? 1 : 0);
        return this;
    }

    /**
     * @param isActivated boolean to activate/inactive penalization of xi.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setXiPenal(boolean isActivated) {
        penalizationOptions.put("xi", isActivated ? 1 : 0);
        return this;
    }

    /**
     * @param isActivated boolean to activate/inactive penalization of shunt 1 conductance.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setG1Penal(boolean isActivated) {
        penalizationOptions.put("g_shunt_1", isActivated ? 1 : 0);
        return this;
    }

    /**
     * @param isActivated boolean to activate/inactive penalization of shunt 2 conductance.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setG2Penal(boolean isActivated) {
        penalizationOptions.put("g_shunt_2", isActivated ? 1 : 0);
        return this;
    }

    /**
     * @param isActivated boolean to activate/inactive penalization of shunt 1 susceptance.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setB1Penal(boolean isActivated) {
        penalizationOptions.put("b_shunt_1", isActivated ? 1 : 0);
        return this;
    }

    /**
     * @param isActivated boolean to activate/inactive penalization of shunt 2 susceptance.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setB2Penal(boolean isActivated) {
        penalizationOptions.put("b_shunt_2", isActivated ? 1 : 0);
        return this;
    }

    /**
     * @param solvingMode the solving mode of the solver used for the divergence analysis.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setSolvingMode(int solvingMode) {
        if (solvingMode < 0 || solvingMode > 2) {
            throw new IllegalArgumentException("Solving mode must be in [0, 1, 2].");
        }
        solvingOptions.put("solving_mode", solvingMode);
        return this;
    }

    /**
     * Put solving mode to 2
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setResolutionMPEC() {
        return setSolvingMode(2);
    }

    /**
     * Put solving mode to 1.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setResolutionNlp() {
        return setSolvingMode(1);
    }

    /**
     * Put solving mode to 0.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setResolutionMinlp() {
        return setSolvingMode(0);
    }

    /**
     * @param maxTimeSolving the maximum time the solver will run.
     * @return the object on which the method is applied.
     */
    public DivergenceAnalyserParameters setMaxTimeSolving(int maxTimeSolving) {
        if (maxTimeSolving <= 0) {
            throw new IllegalArgumentException("Max time solving must be > 0.");
        }
        solvingOptions.put("max_time_solving", maxTimeSolving);
        return this;
    }

}

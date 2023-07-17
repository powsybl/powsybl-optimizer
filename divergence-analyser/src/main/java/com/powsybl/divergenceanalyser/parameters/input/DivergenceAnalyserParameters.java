package com.powsybl.divergenceanalyser.parameters.input;

import java.util.HashMap;

public class DivergenceAnalyserParameters {

    // About penalization
    private boolean targetVUnitsPenal = false;
    private boolean targetVSVCPenal = false;
    private boolean rhoTransformerPenal = false;
    private boolean alphaPSTPenal = false;
    private boolean yPenal = false;
    private boolean xiPenal = false;
    private boolean g1Penal = false;
    private boolean b1Penal = false;
    private boolean g2penal = false;
    private boolean b2Penal = false;

    // About MINLP options
    private boolean isContinuous = false;
    private int maxTimeSolving = 120;

    public DivergenceAnalyserParameters(){
    }

    public DivergenceAnalyserParameters(boolean penalizeEverything){
        if (penalizeEverything) {
            this.targetVUnitsPenal = true;
            this.targetVSVCPenal = true;
            this.rhoTransformerPenal = true;
            this.alphaPSTPenal = true;
            this.yPenal = true;
            this.xiPenal = true;
            this.g1Penal = true;
            this.g2penal = true;
            this.b1Penal = true;
            this.b2Penal = true;
        }
    }

    public HashMap<String, Integer> getPenalization(){
        HashMap<String, Integer> penal = new HashMap<>();
        if (targetVUnitsPenal) penal.put("is_target_v_units", 1);
        if (targetVSVCPenal) penal.put("is_target_v_svc", 1);
        if (rhoTransformerPenal) penal.put("is_rho_control", 1);
        if (alphaPSTPenal) penal.put("is_phase_shift_control", 1);
        if (yPenal) penal.put("is_admittance_control", 1);
        if (xiPenal) penal.put("is_xi_control", 1);
        if (g1Penal) penal.put("is_g_shunt_1_control", 1);
        if (b1Penal) penal.put("is_b_shunt_1_control", 1);
        if (g2penal) penal.put("is_g_shunt_2_control", 1);
        if (b2Penal) penal.put("is_b_shunt_2_control", 1);
        return penal;
    }

    public HashMap<String, Integer> getSolvingOptions(){
        HashMap<String, Integer> options = new HashMap<>();
        if (isContinuous) options.put("is_continuous", 1);
        options.put("max_time_solving", maxTimeSolving);
        return options;
    }

    public DivergenceAnalyserParameters setTargetVUnitsPenal(boolean targetVUnitsPenal) {
        this.targetVUnitsPenal = targetVUnitsPenal;
        return this;
    }

    public DivergenceAnalyserParameters setTargetVSVCPenal(boolean targetVSVCPenal) {
        this.targetVSVCPenal = targetVSVCPenal;
        return this;
    }

    public DivergenceAnalyserParameters setRhoTransformerPenal(boolean rhoTransformerPenal){
        this.rhoTransformerPenal = rhoTransformerPenal;
        return this;
    }

    public DivergenceAnalyserParameters setAlphaPSTPenal(boolean alphaPSTPenal) {
        this.alphaPSTPenal = alphaPSTPenal;
        return this;
    }

    public DivergenceAnalyserParameters setYPenal(boolean yPenal) {
        this.yPenal = yPenal;
        return this;
    }

    public DivergenceAnalyserParameters setXiPenal(boolean xiPenal) {
        this.xiPenal = xiPenal;
        return this;
    }

    public DivergenceAnalyserParameters setG1Penal(boolean g1Penal) {
        this.g1Penal = g1Penal;
        return this;
    }

    public DivergenceAnalyserParameters setG2penal(boolean g2penal) {
        this.g2penal = g2penal;
        return this;
    }

    public DivergenceAnalyserParameters setB1Penal(boolean b1Penal) {
        this.b1Penal = b1Penal;
        return this;
    }

    public DivergenceAnalyserParameters setB2Penal(boolean b2Penal) {
        this.b2Penal = b2Penal;
        return this;
    }

    public DivergenceAnalyserParameters setIsRelaxed(boolean isRelaxed) {
        this.isContinuous = isRelaxed;
        return this;
    }

    public DivergenceAnalyserParameters setMaxTimeSolving(int maxTimeSolving) {
        this.maxTimeSolving = maxTimeSolving;
        return this;
    }


}

package com.powsybl.divergenceanalyser;

import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class DivergenceAnalysisParametersTest {

    @Test
    void testSolvingOptions() {
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        HashMap<String, Integer> solvingOptions = parameters.getSolvingOptions();

        // Verify default values of the parameters
        assert(solvingOptions.get("max_time_solving") == 120);
        assert(solvingOptions.get("solving_mode") == 0);

        // Verify the update of solving mode
        parameters.setRelaxResolution();
        assert(solvingOptions.get("solving_mode") == 1);

        parameters.setMPECResolution();
        assert(solvingOptions.get("solving_mode") == 2);

        parameters.setMINLPResolution();
        assert(solvingOptions.get("solving_mode") == 0);

        assertThrows(IllegalArgumentException.class, () -> parameters.setSolvingMode(4));
        assertThrows(IllegalArgumentException.class, () -> parameters.setSolvingMode(-1));

        // Verify the update of max time solving
        parameters.setMaxTimeSolving(68);
        assert(solvingOptions.get("max_time_solving") == 68);

        assertThrows(IllegalArgumentException.class, () -> parameters.setMaxTimeSolving(0));
        assertThrows(IllegalArgumentException.class, () -> parameters.setMaxTimeSolving(-5));

    }

    /**
     * Verify a penalization is removed if user asks it.
     */
    @Test
    void removePenalizationToParameters() {

        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        HashMap<String, Integer> penalization = parameters.getPenalizationOptions();

        parameters.setAllPenalization(true);
        assert(penalization.size() == 10 && !penalization.containsValue(0));

        parameters.setTargetVUnitsPenal(false);
        assert(penalization.get("target_v_units") == 0);

        parameters.setTargetVSvcPenal(false);
        assert(penalization.get("target_v_svc") == 0);

        parameters.setYPenal(false);
        assert(penalization.get("admittance") == 0);

        parameters.setXiPenal(false);
        assert(penalization.get("xi") == 0);

        parameters.setRhoTransformerPenal(false);
        assert(penalization.get("rho_transformer") == 0);

        parameters.setAlphaPSTPenal(false);
        assert(penalization.get("phase_shift") == 0);

        parameters.setG1Penal(false);
        assert(penalization.get("g_shunt_1") == 0);

        parameters.setB1Penal(false);
        assert(penalization.get("b_shunt_1") == 0);

        parameters.setG2Penal(false);
        assert(penalization.get("g_shunt_2") == 0);

        parameters.setB2Penal(false);
        assert(penalization.get("b_shunt_2") == 0);

        assert(penalization.size() == 10 && !penalization.containsValue(1));
    }

    /**
     * Verify a penalization is added if user asks it.
     */
    @Test
    void addPenalizationToParameters() {

        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        HashMap<String, Integer> penalization = parameters.getPenalizationOptions();

        assert(penalization.size() == 10 && !penalization.containsValue(1));

        parameters.setTargetVUnitsPenal(true);
        assert(penalization.get("target_v_units") == 1);

        parameters.setTargetVSvcPenal(true);
        assert(penalization.get("target_v_svc") == 1);

        parameters.setYPenal(true);
        assert(penalization.get("admittance") == 1);

        parameters.setXiPenal(true);
        assert(penalization.get("xi") == 1);

        parameters.setRhoTransformerPenal(true);
        assert(penalization.get("rho_transformer") == 1);

        parameters.setAlphaPSTPenal(true);
        assert(penalization.get("phase_shift") == 1);

        parameters.setG1Penal(true);
        assert(penalization.get("g_shunt_1") == 1);

        parameters.setB1Penal(true);
        assert(penalization.get("b_shunt_1") == 1);

        parameters.setG2Penal(true);
        assert(penalization.get("g_shunt_2") == 1);

        parameters.setB2Penal(true);
        assert(penalization.get("b_shunt_2") == 1);

        assert(penalization.size() == 10 && !penalization.containsValue(0));
    }

}

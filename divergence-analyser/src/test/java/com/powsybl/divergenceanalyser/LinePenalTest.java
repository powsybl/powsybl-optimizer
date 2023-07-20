package com.powsybl.divergenceanalyser;

import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import com.powsybl.divergenceanalyser.parameters.output.DivergenceAnalyserResults;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.network.SlackBusSelectionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LinePenalTest {

    Network network;

    LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

    @BeforeEach
    void setUp() {
        network = IeeeCdfNetworkFactory.create14();

        loadFlowParameters.setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_REMAINING_MARGIN)
                .setDistributedSlack(false)
                .setUseReactiveLimits(false)
                .setReadSlackBus(false);

        // Parameters for OLF
        OpenLoadFlowParameters parametersExt = OpenLoadFlowParameters.create(loadFlowParameters);
        parametersExt.setVoltageRemoteControl(false)
                .setAlwaysUpdateNetwork(true)
                .setSlackBusSelectionMode(SlackBusSelectionMode.MOST_MESHED);

        LoadFlowResult loadFlowResult = LoadFlow.run(network, loadFlowParameters);
        assertTrue(loadFlowResult.isOk());
    }


//    @Test
//    void testRPenal() throws IOException {
//        Line l = network.getLine("l1");
//        l.setR(277.9584);
//
//        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
//        parameters.setYPenal(true)
//                .setXiPenal(true);
//
//        verifyDAResults(parameters);
//    }

//    @Test
//    void testXPenal() throws IOException {
//        Line l = network.getLine("L2-3-1");
//        l.setX(-2.654671725);
//
//        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
//        parameters.setYPenal(true)
//                .setXiPenal(true);
//
//        verifyDAResults(parameters);
//    }

    @Test
    void testG1Penal() throws IOException {
        Line l = network.getLine("L7-9-1");
        l.setG1(17.0903472222);

        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        parameters.setG1Penal(true);

        verifyDAResults(parameters);
    }

    @Test
    void testG2Penal() throws IOException {
        Line l = network.getLine("L4-5-1");
        l.setG2(39.2750068587);

        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        parameters.setG2penal(true);

        verifyDAResults(parameters);
    }

    @Test
    void testB1Penal() throws IOException {
        Line l = network.getLine("L10-11-1");
        l.setB1(-267.06875);

        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        parameters.setB1Penal(true);

        verifyDAResults(parameters);
    }

    @Test
    void testB2Penal() throws IOException {
        Line l = network.getLine("L6-11-1");
        l.setB2(2.9594375);

        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        parameters.setB2Penal(true);

        verifyDAResults(parameters);
    }

    void verifyDAResults(DivergenceAnalyserParameters parameters) throws IOException {
        // Verify LF diverges without DA
        LoadFlowResult loadFlowResult = LoadFlow.run(network, loadFlowParameters);
        assertFalse(loadFlowResult.isOk());

        // Run DA and apply results
        DivergenceAnalyserResults result = DivergenceAnalyser.getDivergenceAnalysisResults(network, parameters);
        result.applyDivergenceAnalysisPenalisation(network);

        // Verify LF converges with DA results
        loadFlowResult = LoadFlow.run(network, loadFlowParameters);
        assertTrue(loadFlowResult.isOk());
    }

}

package com.powsybl.openreac.parameters.output;

import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoltagePlanOutputTest {

    @Test
    void readTest() throws IOException {
        VoltagePlanOutput output = new VoltagePlanOutput();
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_voltages.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {

            output.read(reader, null);
            assertEquals(3, output.getVoltagePlan().size());
            Pair<Double, Double> busVoltage1 = output.getVoltagePlan().get("bus1");
            Pair<Double, Double> busVoltage2 = output.getVoltagePlan().get("bus2");
            Pair<Double, Double> busVoltage3 = output.getVoltagePlan().get("bus3");
            Assertions.assertAll(
                    () -> assertEquals(0.8, busVoltage1.getFirst()),
                    () -> assertEquals(1.1, busVoltage1.getSecond()),
                    () -> assertEquals(1.2, busVoltage2.getFirst()),
                    () -> assertEquals(Double.NaN, busVoltage2.getSecond()),
                    () -> assertEquals(Double.NaN, busVoltage3.getFirst()),
                    () -> assertEquals(0.11, busVoltage3.getSecond())
            );
        }
    }
}

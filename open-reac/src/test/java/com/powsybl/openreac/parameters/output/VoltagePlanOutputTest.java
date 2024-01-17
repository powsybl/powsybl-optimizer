package com.powsybl.openreac.parameters.output;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoltagePlanOutputTest {

    @Test
    void busResultTest() {
        VoltagePlanOutput.BusResult busResult = new VoltagePlanOutput.BusResult("id", 0.98, 0.23);
        assertEquals("id", busResult.getBusId());
        assertEquals(0.98, busResult.getV());
        assertEquals(0.23, busResult.getAngle());
    }

    @Test
    void readTest() throws IOException {
        VoltagePlanOutput output = new VoltagePlanOutput();
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_voltages.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {

            output.read(reader, null);
            assertEquals(3, output.getVoltagePlan().size());
            VoltagePlanOutput.BusResult busResult1 = output.getVoltagePlan().get(0);
            VoltagePlanOutput.BusResult busResult2 = output.getVoltagePlan().get(1);
            VoltagePlanOutput.BusResult busResult3 = output.getVoltagePlan().get(2);
            Assertions.assertAll(
                    () -> assertEquals("bus1", busResult1.getBusId()),
                    () -> assertEquals(0.8, busResult1.getV()),
                    () -> assertEquals(1.1, busResult1.getAngle()),
                    () -> assertEquals("bus2", busResult2.getBusId()),
                    () -> assertEquals(1.2, busResult2.getV()),
                    () -> assertEquals(Double.NaN, busResult2.getAngle()),
                    () -> assertEquals("bus3", busResult3.getBusId()),
                    () -> assertEquals(Double.NaN, busResult3.getV()),
                    () -> assertEquals(0.11, busResult3.getAngle())
            );
        }
    }
}

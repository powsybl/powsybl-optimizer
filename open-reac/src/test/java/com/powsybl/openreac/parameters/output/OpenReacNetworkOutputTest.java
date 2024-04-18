package com.powsybl.openreac.parameters.output;

import com.powsybl.openreac.exceptions.IncompatibleModelException;
import com.powsybl.openreac.parameters.output.network.GeneratorNetworkOutput;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Pierre Arvy {@literal <pierre.arvy at artelys.com>}
 */
class OpenReacNetworkOutputTest {
    @Test
    void wrongNumberOfColumnsTest() throws IOException {
        GeneratorNetworkOutput output = new GeneratorNetworkOutput(null);
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_shunts.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            IncompatibleModelException e = assertThrows(IncompatibleModelException.class, () -> output.read(reader, null));
            assertEquals("Error of compatibility between the ampl model and the interface, this is a OpenReac issue.\n" +
                    "Error reading reactiveopf_results_generators.csv, wrong number of columns. Expected: 9, found:6", e.getMessage());
        }
    }
}

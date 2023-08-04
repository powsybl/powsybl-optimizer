package com.powsybl.divergenceanalyser.parameters.output;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.divergenceanalyser.parameters.output.modifications.BusPenalization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BusPenalizationOutput implements AmplOutputFile {

    public static final int NEW_TARGET_V_COL = 2;
    public static final int BIN_TARGET_V_COL = 4;
    public static final int EXPECTED_COLS = 5; // Contains ID, new values of parameters, values of slacks/binary var
    private static final String SEP = ";";

    private final List<BusPenalization> penalisation = new ArrayList<>();

    public List<BusPenalization> getPenalisation() {
        return penalisation;
    }

    @Override
    public String getFileName() {
        return "da_bus_penal.csv";
    }

    @Override
    public void read(Path path, StringToIntMapper<AmplSubset> amplMapper) {
        List<String> busModificationsLines;

        if (Files.isRegularFile(path)) {
            try {
                busModificationsLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            } catch (IOException e) {
                // File reading went wrong
                return;
            }

            String headers = busModificationsLines.get(0);
            int readCols = headers.split(SEP).length;
            if (readCols != EXPECTED_COLS) {
                try {
                    throw new Exception("Error reading " + getFileName() + ", wrong number of columns. Expected: " + EXPECTED_COLS + ", found:" + readCols);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                for (String line : busModificationsLines.subList(1, busModificationsLines.size())) {
                    readLine(line.split(SEP), amplMapper);
                }
            }
        }
    }

    private void readLine(String[] tokens, StringToIntMapper<AmplSubset> amplMapper) {

        String id = amplMapper.getId(AmplSubset.BUS, Integer.parseInt(tokens[0]));
        double newTargetV = readDouble(tokens[NEW_TARGET_V_COL - 1]);
        boolean isTargetVPenalized = readDouble(tokens[BIN_TARGET_V_COL - 1]) > 0;

        penalisation.add(new BusPenalization(id, isTargetVPenalized, newTargetV));
    }

    private double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }
}

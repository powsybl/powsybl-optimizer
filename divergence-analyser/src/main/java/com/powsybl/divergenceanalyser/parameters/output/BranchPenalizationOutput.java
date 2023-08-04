package com.powsybl.divergenceanalyser.parameters.output;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.divergenceanalyser.parameters.output.modifications.BranchPenalization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BranchPenalizationOutput implements AmplOutputFile {
    public static final int NEW_RHO_COL = 4;
    public static final int NEW_Y_COL = 5;
    public static final int NEW_ALPHA_COL = 6;
    public static final int NEW_XI_COL = 7;
    public static final int NEW_G1_COL = 8;
    public static final int NEW_B1_COL = 9;
    public static final int NEW_G2_COL = 10;
    public static final int NEW_B2_COL = 11;

    public static final int BIN_RHO_COL = 20;
    public static final int BIN_Y_COL = 21;
    public static final int BIN_ALPHA_COL = 22;
    public static final int BIN_XI_COL = 23;
    public static final int BIN_G1_COL = 24;
    public static final int BIN_B1_COL = 25;
    public static final int BIN_G2_COL = 26;
    public static final int BIN_B2_COL = 27;

    public static final int EXPECTED_COLS = 28; // Contains ID, new values of parameters, values of slacks/binary var
    private static final String SEP = ";";

    private final List<BranchPenalization> penalization = new ArrayList<>();

    public List<BranchPenalization> getPenalization() {
        return penalization;
    }

    @Override
    public String getFileName() {
        return "da_branch_penal.csv";
    }

    @Override
    public void read(Path path, StringToIntMapper<AmplSubset> amplMapper) {
        List<String> branchModifLines;

        if (Files.isRegularFile(path)) {
            try {
                branchModifLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            } catch (IOException e) {
                // File reading went wrong
                return;
            }

            String headers = branchModifLines.get(0);
            int readCols = headers.split(SEP).length;
            if (readCols != EXPECTED_COLS) {
                try {
                    throw new Exception("Error reading " + getFileName() + ", wrong number of columns. Expected: " + EXPECTED_COLS + ", found:" + readCols);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                for (String line : branchModifLines.subList(1, branchModifLines.size())) {
                    readLine(line.split(SEP), amplMapper);
                }
            }
        }
    }

    private void readLine(String[] tokens, StringToIntMapper<AmplSubset> amplMapper) {

        String id = amplMapper.getId(AmplSubset.BRANCH, Integer.parseInt(tokens[0]));

        double rho = readDouble(tokens[NEW_RHO_COL - 1]);
        double y = readDouble(tokens[NEW_Y_COL - 1]);
        double alpha = readDouble(tokens[NEW_ALPHA_COL - 1]);
        double xi = readDouble(tokens[NEW_XI_COL - 1]);
        double g1 = readDouble(tokens[NEW_G1_COL - 1]);
        double b1 = readDouble(tokens[NEW_B1_COL - 1]);
        double g2 = readDouble(tokens[NEW_G2_COL - 1]);
        double b2 = readDouble(tokens[NEW_B2_COL - 1]);

        boolean isRhoPenalised = readDouble(tokens[BIN_RHO_COL - 1]) > 0;
        boolean isYPenalised = readDouble(tokens[BIN_Y_COL - 1]) > 0;
        boolean isAlphaPenalised = readDouble(tokens[BIN_ALPHA_COL - 1]) > 0;
        boolean isXiPenalised = readDouble(tokens[BIN_XI_COL - 1]) > 0;
        boolean isG1Penalised = readDouble(tokens[BIN_G1_COL - 1]) > 0;
        boolean isB1Penalised = readDouble(tokens[BIN_B1_COL - 1]) > 0;
        boolean isG2Penalised = readDouble(tokens[BIN_G2_COL - 1]) > 0;
        boolean isB2Penalised = readDouble(tokens[BIN_B2_COL - 1]) > 0;

        penalization.add(new BranchPenalization(id, isRhoPenalised, isYPenalised, isAlphaPenalised, isXiPenalised, isG1Penalised,
                isB1Penalised, isG2Penalised, isB2Penalised, rho, y, alpha, xi, g1, b1, g2, b2));

    }

    private double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }

}

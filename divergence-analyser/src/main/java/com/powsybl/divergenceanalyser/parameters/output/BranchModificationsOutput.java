package com.powsybl.divergenceanalyser.parameters.output;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BranchModificationsOutput implements AmplOutputFile {
    public static final int NEW_RHO_VALUE_COL = 4;
    public static final int NEW_Y_VALUE_COL = 5;
    public static final int NEW_ALPHA_VALUE_COL = 6;
    public static final int NEW_XI_VALUE_COL = 7;
    public static final int NEW_G1_VALUE_COL = 8;
    public static final int NEW_B1_VALUE_COL = 9;
    public static final int NEW_G2_VALUE_COL = 10;
    public static final int NEW_B2_VALUE_COL = 11;

    public static final int EXPECTED_COLS = 28;
    private static final String SEP = ";";

    public static class BranchModification {
        private final String branchId;
        private final double newRho;
        private final double newY;
        private final double newAlpha;
        private final double newXi;
        private final double newG1;
        private final double newB1;
        private final double newG2;
        private final double newB2;

        public BranchModification(String branchId, double newRho, double newY, double newAlpha, double newXi, double newG1, double newB1, double newG2, double newB2) {
            this.branchId = branchId;
            this.newRho = newRho;
            this.newY = newY;
            this.newAlpha = newAlpha;
            this.newXi = newXi;
            this.newG1 = newG1;
            this.newB1 = newB1;
            this.newG2 = newG2;
            this.newB2 = newB2;
        }

        public String getBranchId() {
            return branchId;
        }

        public double getNewRho() {
            return newRho;
        }

        public double getNewY() {
            return newY;
        }

        public double getNewAlpha() {
            return newAlpha;
        }

        public double getNewXi() {
            return newXi;
        }

        public double getNewG1() {
            return newG1;
        }

        public double getNewB1() {
            return newB1;
        }

        public double getNewG2() {
            return newG2;
        }

        public double getNewB2() {
            return newB2;
        }
    }

    private final List<BranchModification> modifications = new ArrayList<>();

    public List<BranchModification> getModificiations() {
        return modifications;
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

        double newRho = readDouble(tokens[NEW_RHO_VALUE_COL - 1]);
        double newY = readDouble(tokens[NEW_Y_VALUE_COL - 1]);
        double newAlpha = readDouble(tokens[NEW_ALPHA_VALUE_COL - 1]);
        double newXi = readDouble(tokens[NEW_XI_VALUE_COL - 1]);
        double newG1 = readDouble(tokens[NEW_G1_VALUE_COL - 1]);
        double newB1 = readDouble(tokens[NEW_B1_VALUE_COL - 1]);
        double newG2 = readDouble(tokens[NEW_G2_VALUE_COL - 1]);
        double newB2 = readDouble(tokens[NEW_B2_VALUE_COL - 1]);

        modifications.add(new BranchModification(id, newRho, newY, newAlpha, newXi, newG1, newB1, newG2, newB2));

    }

    private double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }

}

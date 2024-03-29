/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.preprocessing;

import org.apache.commons.math3.linear.*;

import static org.apache.commons.math3.linear.MatrixUtils.createRealMatrix;
import static org.apache.commons.math3.linear.MatrixUtils.inverse;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */


public class GeneralizedStateEstimationModel {

    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        // Network parameters
        int N = 4; // number of nodes
        int E = 5; // number of lines
        // List of all possible lines in the network : defines the order of the B[m,n] in currentStateEstimate !
        List<String> lines = List.of("1_2", "1_4", "2_3", "2_4", "3_4");
        boolean topology = false; // indicate whether topology given to the GSE is true or false
        int[] errorOnLine = {2,4}; // if topology = True, indicate the line whose status is wrong
        int slackBus = 2; // slack (not the Java index ! slackJavaIndex = slackBus - 1)

        // Network parameters
        // TODO : method to directly build conductanceMap and susceptanceMap
        double[][] conductanceMatrix = {
                {0., 0.09999, 0., 0.049995},
                {0.09999, 0., 0.011111, 0.124922},
                {0., 0.011111, 0., 0.09999},
                {0.049995, 0.124922, 0.09999, 0.}
        };
        double[][] susceptanceMatrix = {
                {0., -9.999, 0., -4.9995},
                {-9.999, 0., -3.333296, -4.996877},
                {0., -3.333296, 0., -9.999},
                {-4.9995, -4.996877, -9.999, 0.}
        };
        Map<String, Double> conductanceMap = new HashMap<>();
        Map<String, Double> susceptanceMap = new HashMap<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                String lineID = Integer.toString(i + 1) + "_" + Integer.toString(j + 1);
                conductanceMap.put(lineID, conductanceMatrix[i][j]);
                susceptanceMap.put(lineID, susceptanceMatrix[i][j]);
            }
        }
        // "A priori" lines states
        double[][] linesStatusAssumption = {
                {0, 1, 0, 1},
                {1, 0, 1, 1},
                {0, 1, 0, 1},
                {1, 1, 1, 0}
        };
        if (!topology) {
            linesStatusAssumption[errorOnLine[0] - 1][errorOnLine[1] - 1] = 1 - linesStatusAssumption[errorOnLine[0] - 1][errorOnLine[1] - 1];
        }
        // Slack voltage (enforced in operational constraints)
        double VReSlackTrue = 1.3;
        double VImSlackTrue = 0;

        // Measurements vector
        Map<String, Map<String, Object>> zMap = new HashMap<>();
        zMap.put("a", Map.of("type", "P", "loc", 1, "value", 3.0, "variance", 0.05));
        zMap.put("b", Map.of("type", "P", "loc", 2, "value", 2.0, "variance", 0.05));
        zMap.put("c", Map.of("type", "P", "loc", 3, "value", -1.5, "variance", 0.05));
        zMap.put("d", Map.of("type", "P", "loc", 4, "value", -3.0, "variance", 0.05));
        zMap.put("e", Map.of("type", "Q", "loc", 3, "value", -1.0, "variance", 0.05));
        zMap.put("f", Map.of("type", "Q", "loc", 4, "value", -1.0, "variance", 0.05));
        zMap.put("g", Map.of("type", "V2", "loc", 1, "value", 1.1, "variance", 0.05));
        zMap.put("h", Map.of("type", "V2", "loc", 4, "value", 1.03298, "variance", 0.05));
        zMap.put("i", Map.of("type", "Pf", "loc", new int[]{2, 3}, "value", 1.153624, "variance", 0.05));
        zMap.put("j", Map.of("type", "Pf", "loc", new int[]{2, 4}, "value", 1.588139, "variance", 0.05));
        zMap.put("k", Map.of("type", "Pf", "loc", new int[]{3, 4}, "value", -0.348346, "variance", 0.05));
        zMap.put("l", Map.of("type", "Qf", "loc", new int[]{1, 2}, "value", -2.158516, "variance", 0.05));

        // Number of measurements
        int m = zMap.size();

        // Initialize an array to only store the values of the measurements
        double[] zVec = new double[m];
        // Initialize the measurement covariance matrix
        double[][] R = new double[m + 2 + E][m + 2 + E];
        // Iterate over the entries of zMap and store the "value" and "variance" of each measurement in zVec and R
        int index = 0;
        for (Map<String, Object> entry : zMap.values()) {
            zVec[index] = ((Number) entry.get("value")).doubleValue();
            R[index][index] = ((Number) entry.get("variance")).doubleValue();
            index++;
        }
        //

        // Hachtel's procedure

        // Parameters

        int nbMaxIter = 20;
        double tolerance = 1e-7;

        // Initial guess for extended state vector ("flat start")
        // TODO : build automatically "flat start"
        double[] currentStateEstimate = {1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1};
        // Adjust currentStateEstimation to topology assumption
        if (!topology) {
            String errorLineID = Integer.toString(errorOnLine[0]) + "_" + Integer.toString(errorOnLine[1]);
            int errorLineIdx = 2 * N + lines.indexOf(errorLineID);
            currentStateEstimate[errorLineIdx] = 1 - currentStateEstimate[errorLineIdx];
        }

        System.out.println("\nInitial state estimate :");
        System.out.println(Arrays.toString(currentStateEstimate));

        // Declare the vector of Lagrange multipliers
        RealVector lagrangeMultipliers = new ArrayRealVector();

        // Declare the final matrix A = [[0 H.T],[H R]] found at convergence
        RealMatrix finalA = null;

        // Start iterative process

        for (int iter = 0; iter < nbMaxIter; iter++) {

            // Build HashMaps containing current estimates of real (VRe) and imaginary (VIm) parts of the voltage at each node
            Map<Integer, Double> VRe = new HashMap<>();
            Map<Integer, Double> VIm = new HashMap<>();
            for (int node = 1; node <= N; node++) {
                VRe.put(node, currentStateEstimate[node - 1]);
                VIm.put(node, currentStateEstimate[node - 1 + N]);
            }

            // Build HashMap containing current estimates of lines status
            Map<String, Double> linesStatusEstimate = new HashMap<>();
            for (int i = 0; i < lines.size(); i++) {
                String lineID = lines.get(i);
                linesStatusEstimate.put(lineID, currentStateEstimate[2 * N + i]);
            }

            // Compute mismatch vector at current state estimate
            double[] mismatchVector = Mismatch.computeMismatchVector(VRe, VIm, linesStatusEstimate, zMap, zVec, VReSlackTrue,
                    VImSlackTrue, slackBus, conductanceMap, susceptanceMap, linesStatusAssumption);

            // Compute Jacobian matrix at current state estimate (computeJacobian() must return a double[][]
            RealMatrix H = createRealMatrix(Jacobian.computeJacobian(VRe, VIm, linesStatusEstimate, zMap, slackBus,
                    conductanceMap, susceptanceMap));

            // Build matrix A=[[0 H.T],[H R]]
            // Define the matrix blocks
            double[][] block2 = H.transpose().getData();
            double[][] block3 = H.getData();
            double[][] block4 = R;
            double[][] block1 = new double[block2.length][block3[0].length];
            for (int i = 0; i < block2.length; i++) {
                for (int j = 0; j < block3[0].length; j++) {
                    block1[i][j] = 0;
                }
            }

            // Calculate the dimensions of the final matrix A
            int nbRows = block2.length + block4.length;
            int nbCols = block3[0].length + block4[0].length;

            RealMatrix A = new BlockRealMatrix(nbRows, nbCols);

            // Copy blocks into the matrix A
            A.setSubMatrix(block1, 0, 0);
            A.setSubMatrix(block2, 0, block1[0].length);
            A.setSubMatrix(block3, block1.length, 0);
            A.setSubMatrix(block4, block1.length, block1[0].length);

            // Build vector b = [0 mismatchVector].T
            double[] bTmp = new double[nbRows];
            // Fill b with 0
            for (int i = 0; i < nbRows; i++) {
                bTmp[i] = 0;
            }
            // Add mismatchVector at the end
            for (int i = 0; i < mismatchVector.length; i++) {
                bTmp[nbRows - i - 1] = mismatchVector[mismatchVector.length - i - 1];
            }
            RealVector b = new ArrayRealVector(bTmp);

            // Get LU decomposition of A
            DecompositionSolver solver = new LUDecomposition(A).getSolver();
            // Solve the linear system Ax = b
            RealVector solution = solver.solve(b);

            // Retrieve change vector for currentStateEstimate
            RealVector deltaState = solution.getSubVector(0, 2 * N + E);

            // Update currentStateEstimate
            RealVector currentStateEstimateTmp = new ArrayRealVector(currentStateEstimate);
            currentStateEstimate = currentStateEstimateTmp.add(deltaState).toArray();

            // Update vector of Lagrange multipliers
            int nbLagrangeMultipliers = m + 2 + E;
            lagrangeMultipliers = solution.getSubVector(2 * N + E, nbLagrangeMultipliers);

            // Check convergence
            if (iter > 0) {
                // Calculate total change in currentStateEstimate
                double change = deltaState.getNorm(); // Calculate change in solution vector

                // Check if change is below tolerance
                if (change < tolerance) {
                    System.out.printf("\nConvergence achieved after %d iterations.\n", iter);
                    // Store the final matrix A
                    finalA = A;
                    // Exit the iterative process
                    break;
                }

            }
        }

        System.out.println("\nFinal state estimate [VRe1,VRe2,VRe3,VRe4,VIm1,VIm2,VIm3,VIm4,B12,B14,B23,B24,B34]: ");
        System.out.println(Arrays.toString(currentStateEstimate));

        System.out.println("\nLagrange multipliers associated to [B12,B14,B23,B24,B34] : ");
        System.out.println(lagrangeMultipliers.getSubVector(m+2, E));

        // Compute normalized Lagrange multipliers (only if convergence achieved)
        if (finalA != null) {
            // Invert matrix A
            RealMatrix invertedA = inverse(finalA);
            // Select V block (V has same dimensions as R : (m+2+E)x(m+2+E))
            RealMatrix V = invertedA.getSubMatrix(invertedA.getRowDimension() - (m+2+E),
                    invertedA.getRowDimension() - 1,
                    invertedA.getColumnDimension() - (m+2+E),
                    invertedA.getColumnDimension() - 1
                    );
            // Normalize Lagrange multipliers (absolute value)
            RealVector normalizedLagrangeMultipliers = new ArrayRealVector(lagrangeMultipliers.getDimension());
            for (int i = 0; i < lagrangeMultipliers.getDimension(); i++) {
                normalizedLagrangeMultipliers.setEntry(i, Math.abs(lagrangeMultipliers.getEntry(i) / Math.sqrt(V.getEntry(i,i))));
            }

            System.out.println("\nNormalized Lagrange multipliers (absolute value) associated to [B12,B14,B23,B24,B34] : ");
            System.out.println(normalizedLagrangeMultipliers.getSubVector(m+2, E));
            System.out.println();
        }
    }
}



/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.preprocessing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */


public class Mismatch {

    /**
     * @return an array containing all mismatches between expected values (for measurements, structural and operational constraints)
     * and estimated values as inferred from the current state estimate
     */
    public static double[] computeMismatchVector(Map<Integer, Double> VRe, Map<Integer, Double> VIm,
                                                 Map<String, Double> linesStatusEstimate, Map<String, Map<String, Object>> zMap,
                                                 double[] zVec, double VReSlackTrue, double VImSlackTrue,
                                                 int slackBus, Map<String, Double> conductanceMap,
                                                 Map<String, Double> susceptanceMap,
                                                 double[][] linesStatusAssumption)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        // Number of nodes
        int N = VRe.size();
        // Number of lines
        int E = linesStatusEstimate.size();

        // Compute mismatch related to measurements
        double[] measurementsEstimates = estimateMeasurements(zMap, VRe, VIm, linesStatusEstimate, conductanceMap, susceptanceMap);

        double[] deltaZM = new double[zVec.length];
        for (int i = 0; i < zVec.length; i++) {
            deltaZM[i] = zVec[i] - measurementsEstimates[i];
        }

        // Compute mismatch related to structural constraints
        double[] deltaZS = {VReSlackTrue - VRe.get(slackBus), VImSlackTrue - VIm.get(slackBus)};

        // Compute mismatch related to operational constraints
        double[] deltaZO = new double[linesStatusEstimate.size()];
        // For each line
        int index = 0;
        for (Map.Entry<String, Double> entry : linesStatusEstimate.entrySet()) {
            String lineId = entry.getKey();
            String[] parts = lineId.split("_");
            int lineIdxI = Integer.parseInt(parts[0]) - 1;
            int lineIdxJ = Integer.parseInt(parts[1]) - 1;
            deltaZO[index++] = linesStatusAssumption[lineIdxI][lineIdxJ] - entry.getValue();
        }

        // Gather all mismatches in one array
        double[] deltaZ = new double[zVec.length + 2 + E];
        System.arraycopy(deltaZM, 0, deltaZ, 0, zVec.length);
        System.arraycopy(deltaZS, 0, deltaZ, zVec.length, 2);
        System.arraycopy(deltaZO, 0, deltaZ, zVec.length + 2, E);

        return deltaZ;
    }

    /**
     * @return an array containing all measurements estimates as inferred from the current state estimate
     */
    private static double[] estimateMeasurements(Map<String, Map<String, Object>> zMap, Map<Integer, Double> VRe,
                                                 Map<Integer, Double> VIm, Map<String, Double> linesStatusEstimate,
                                                 Map<String, Double> conductanceMap, Map<String, Double> susceptanceMap) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // Number of measurements
        int m = zMap.size();

        // Array of estimated measurements
        double[] measurementsEstimates = new double[m];

        // Duplicate each line in linesStatusEstimate ("1_2" -> "1_2" & "2_1") to ease calculus in MeasurementFunctions methods
        Map<String, Double> linesStatusEstimateDuplicate = new HashMap<>();
        for (Map.Entry<String, Double> entry : linesStatusEstimate.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split("_");
            String reversedKey = parts[1] + "_" + parts[0]; // Reverse the key
            linesStatusEstimateDuplicate.put(key, entry.getValue());
            linesStatusEstimateDuplicate.put(reversedKey, entry.getValue());
        }

        // For each measurement
        int i = 0;
        for (Map.Entry<String, Map<String, Object>> entry : zMap.entrySet()) {
            Map<String, Object> measurement = entry.getValue();
            // Get measurement's type
            String measurementType = (String) measurement.get("type");

            double estimateTmp;
            // Find which measurement function to use in order to compute measurement's estimate
            MeasurementFunctions obj = new MeasurementFunctions();

            if (measurementType.equals("Pf") || measurementType.equals("Qf")) {
                // If measurement corresponds to a power flow, two nodes are involved
                int[] nodesInvolved = (int[]) measurement.get("loc");
                // Find which measurement function to use in order to compute measurement's estimate
                Method measurementFunction = obj.getClass().getMethod(measurementType, int.class, int.class,
                        HashMap.class, HashMap.class, HashMap.class, HashMap.class, HashMap.class);
                // Compute measurement estimate
                estimateTmp = (double) measurementFunction.invoke(obj, nodesInvolved[0], nodesInvolved[1], VRe, VIm,
                        linesStatusEstimateDuplicate, conductanceMap, susceptanceMap);
            } else {
                // Else, only one node is involved
                int nodeInvolved = (int) measurement.get("loc");
                // Find which measurement function to use in order to compute measurement's estimate
                Method measurementFunction = obj.getClass().getMethod(measurementType, int.class,
                        HashMap.class, HashMap.class, HashMap.class, HashMap.class, HashMap.class);
                // Compute measurement estimate
                estimateTmp = (double) measurementFunction.invoke(obj, nodeInvolved, VRe, VIm,
                        linesStatusEstimateDuplicate, conductanceMap, susceptanceMap);
            }
            measurementsEstimates[i++] = estimateTmp;
        }
        return measurementsEstimates;
    }
}


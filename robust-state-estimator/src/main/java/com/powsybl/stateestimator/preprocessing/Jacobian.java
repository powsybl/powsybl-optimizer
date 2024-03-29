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

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */


public class Jacobian {

    /**
     * @return an array containing all mismatches between expected values (for measurements, structural and operational constraints)
     * and estimated values as inferred from the current state estimate
     */
    public static double[][] computeJacobian(Map<Integer, Double> VRe, Map<Integer, Double> VIm,
                                             Map<String, Double> linesStatusEstimate, Map<String, Map<String, Object>> zMap,
                                             int slackBus, Map<String, Double> conductanceMap, Map<String, Double> susceptanceMap)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // Number of lines
        int E = linesStatusEstimate.size();
        // Number of nodes
        int N = VRe.size();
        // Number of measurements
        int m = zMap.size();

        // Initialize Jacobian matrix
        double[][] H = new double[m+2+E][2*N+E];

        // Duplicate each line in linesStatusEstimate ("1_2" -> "1_2" & "2_1") to ease calculus in MeasurementFunctions methods
        Map<String, Double> linesStatusEstimateDuplicate = new HashMap<>();
        for (Map.Entry<String, Double> entry : linesStatusEstimate.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split("_");
            String reversedKey = parts[1] + "_" + parts[0]; // Reverse the key
            linesStatusEstimateDuplicate.put(key, entry.getValue());
            linesStatusEstimateDuplicate.put(reversedKey, entry.getValue());
        }

        // Compute rows of the Jacobian related to measurements

        // For each measurement
        int rowIdx = 0;
        for (Map.Entry<String, Map<String, Object>> entry : zMap.entrySet()) {
            Map<String, Object> measurement = entry.getValue();
            // Get measurement type
            String measurementType = (String) measurement.get("type");

            // Get localisation of the measurement and compute derivatives w.r.t VRe[m] and VIm[m] at each node m
            if (measurementType.equals("Pf") || measurementType.equals("Qf")) {

                // If measurement corresponds to a power flow, two nodes are involved
                int[] nodesInvolved = (int[]) measurement.get("loc");

                // Get derivative functions related to the measurement type...
                JacobianFunctions obj = new JacobianFunctions();
                // ... with respect to VRe
                Method dMeas_dVRe = obj.getClass().getMethod("d" + measurementType + "_dVRe",
                        int.class, int.class, int.class, HashMap.class, HashMap.class,
                        HashMap.class, HashMap.class,HashMap.class);
                // ... with respect to VIm
                Method dMeas_dVIm = obj.getClass().getMethod("d" + measurementType + "_dVIm",
                        int.class, int.class, int.class, HashMap.class, HashMap.class,
                        HashMap.class, HashMap.class,HashMap.class);

                // Then for each node in the network
                for (int nodeM = 1; nodeM <= N; nodeM++) {

                    // Compute derivatives of the measurement function at nodesInvolved w.r.t VRe[m] and VIm[m]
                    double derivativeVReTmp = (double) dMeas_dVRe.invoke(obj, nodesInvolved[0], nodesInvolved[1], nodeM, VRe, VIm,
                            linesStatusEstimateDuplicate, conductanceMap, susceptanceMap);
                    double derivativeVImTmp = (double) dMeas_dVIm.invoke(obj, nodesInvolved[0], nodesInvolved[1], nodeM, VRe, VIm,
                            linesStatusEstimateDuplicate, conductanceMap, susceptanceMap);
                    H[rowIdx][nodeM-1] = derivativeVReTmp;
                    H[rowIdx][nodeM+N-1] = derivativeVImTmp;
                }
            } else {

                // Else, only one node is involved
                int nodeInvolved = (int) measurement.get("loc");

                // Get derivative functions related to the measurement type...
                JacobianFunctions obj = new JacobianFunctions();
                // ... with respect to VRe
                Method dMeas_dVRe = obj.getClass().getMethod("d" + measurementType + "_dVRe",
                        int.class, int.class, HashMap.class, HashMap.class,
                        HashMap.class, HashMap.class,HashMap.class);
                // ... with respect to VIm
                Method dMeas_dVIm = obj.getClass().getMethod("d" + measurementType + "_dVIm",
                        int.class, int.class, HashMap.class, HashMap.class,
                        HashMap.class, HashMap.class,HashMap.class);

                for (int nodeM = 1; nodeM <= N; nodeM++) {

                    // Compute derivative of the measurement function at nodesInvolved with respect to VRe[nodeM]
                    double derivativeVReTmp = (double) dMeas_dVRe.invoke(obj, nodeInvolved, nodeM, VRe, VIm,
                            linesStatusEstimateDuplicate, conductanceMap, susceptanceMap);
                    double derivativeVImTmp = (double) dMeas_dVIm.invoke(obj, nodeInvolved, nodeM, VRe, VIm,
                            linesStatusEstimateDuplicate, conductanceMap, susceptanceMap);
                    H[rowIdx][nodeM-1] = derivativeVReTmp;
                    H[rowIdx][nodeM+N-1] = derivativeVImTmp;
                }
            }

            // Get localisation of the measurement and compute derivatives w.r.t B[m,n] at each line (m,n)
            if (measurementType.equals("Pf") || measurementType.equals("Qf")) {

                // If measurement corresponds to a power flow, two nodes are involved in the measurement
                int[] nodesInvolved = (int[]) measurement.get("loc");

                // Get derivative function related to measurement type with respect to B
                JacobianFunctions obj = new JacobianFunctions();
                Method dMeas_dB = obj.getClass().getMethod("d" + measurementType + "_dB",
                        int.class, int.class, int.class, int.class, HashMap.class, HashMap.class,
                        HashMap.class, HashMap.class,HashMap.class);

                int lineRelativeIdx = 0;
                // For each line (m,n)
                // TODO : vérifier que seules des lignes existantes sont passées en argument
                for (String lineID : linesStatusEstimate.keySet()) {
                    String[] parts = lineID.split("_");
                    int nodeM = Integer.parseInt(parts[0]);
                    int nodeN = Integer.parseInt(parts[1]);
                    // Compute derivative of the measurement function at nodesInvolved w.r.t B[m,n]
                    double derivativeBTmp = (double) dMeas_dB.invoke(obj, nodesInvolved[0], nodesInvolved[1], nodeM, nodeN, VRe, VIm,
                            linesStatusEstimateDuplicate, conductanceMap, susceptanceMap);
                    H[rowIdx][lineRelativeIdx+2*N] = derivativeBTmp;
                    lineRelativeIdx++;
                }
            } else {

                // Else, only one node is involved in the measurement
                int nodeInvolved = (int) measurement.get("loc");

                // Get derivative function related to measurement type with respect to B
                JacobianFunctions obj = new JacobianFunctions();
                Method dMeas_dB = obj.getClass().getMethod("d" + measurementType + "_dB",
                        int.class, int.class, int.class, HashMap.class, HashMap.class,
                        HashMap.class, HashMap.class,HashMap.class);

                int lineRelativeIdx = 0;
                // For each line (m,n)
                for (String lineID : linesStatusEstimate.keySet()) {
                    String[] parts = lineID.split("_");
                    int nodeM = Integer.parseInt(parts[0]);
                    int nodeN = Integer.parseInt(parts[1]);
                    // Compute derivative of the measurement function at nodesInvolved w.r.t B[m,n]
                    double derivativeBTmp = (double) dMeas_dB.invoke(obj, nodeInvolved, nodeM, nodeN, VRe, VIm,
                            linesStatusEstimateDuplicate, conductanceMap, susceptanceMap);
                    H[rowIdx][lineRelativeIdx+2*N] = derivativeBTmp;
                    lineRelativeIdx++;
                }
            }
            rowIdx++;
        }

        // Compute rows of the Jacobian related to structural constraints
        // First row : VRe[slackBus] = VReSlackTrue
        for (int i = 0; i < 2*N+E; i++){
            if (i == slackBus-1){
                H[m][i] = 1;
            }
            else {
                H[m][i] = 0;
            }
        }
        // Second row : VIm[slackBus] = VImSlackTrue
        for (int i = 0; i < 2*N+E; i++){
            if (i == slackBus-1+N){
                H[m+1][i] = 1;
            }
            else {
                H[m+1][i] = 0;
            }
        }

        // Compute rows of the Jacobian related to operation constraints
        // Each row corresponds to a line (m,n) related to constraint : linesStatusEstimate[m,n] = linesStatusAssumption[m,n]
        for (int rowRelativeIdx = 0; rowRelativeIdx < E; rowRelativeIdx++) {
            for (int i = 0; i < 2*N+E; i++) {
                if (i == 2*N + rowRelativeIdx) {
                    H[m+2+rowRelativeIdx][i] = 1;
                }
                else {
                    H[m+2+rowRelativeIdx][i] = 0;
                }
            }
        }

        return H;
    }
}


/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.preprocessing;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */

public class MeasurementFunctions {

    /**
     * @return active injected power at bus l for a given state vector x = (VRe, VIm, B)
     */
    public static double P(int l, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                           HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double P = 0;
        int N = VRe.size(); // number of nodes
        for (int k = 1; k <= N; k++) {
            if (k != l) {
                String lk = l + "_" + k;
                if (B.containsKey(lk)) {
                    P += B.get(lk) * (g.get(lk) * (Math.pow(VRe.get(l), 2) + Math.pow(VIm.get(k), 2))
                            - g.get(lk) * (VRe.get(l) * VRe.get(k) + VIm.get(l) * VIm.get(k))
                            + b.get(lk) * (VRe.get(l) * VIm.get(k) - VIm.get(l) * VRe.get(k)));
                }
            }
        }
        return P;
    }

    /**
     * @return reactive injected power at bus l for a given state vector x = (VRe, VIm, B)
     */
    public static double Q(int l, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                           HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double Q = 0;
        int N = VRe.size(); // number of nodes
        for (int k = 1; k <= N; k++) {
            if (k != l) {
                String lk = l + "_" + k;
                if (B.containsKey(lk)) {
                    Q += B.get(lk) * (-b.get(lk) * (Math.pow(VRe.get(l), 2) + Math.pow(VIm.get(k), 2))
                            + b.get(lk) * (VRe.get(l) * VRe.get(k) + VIm.get(l) * VIm.get(k))
                            + g.get(lk) * (VRe.get(l) * VIm.get(k) - VIm.get(l) * VRe.get(k)));
                }
            }
        }
        return Q;
    }

    /**
     * @return squared voltage magnitude at bus l for a given state vector x = (VRe, VIm, B)
     */
    public static double V2(int l, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                            HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        return Math.pow(VRe.get(l), 2) + Math.pow(VIm.get(l), 2);
    }

    /**
     * @return active power flow for line (l,k) on side l for a given state vector x = (VRe, VIm, B)
     */
    public static double Pf(int l, int k, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                            HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        String lk = l + "_" + k;
        if (B.containsKey(lk)) {
            return B.get(lk) * (g.get(lk) * (Math.pow(VRe.get(l), 2) + Math.pow(VIm.get(k), 2))
                    - g.get(lk) * (VRe.get(l) * VRe.get(k) + VIm.get(l) * VIm.get(k))
                    + b.get(lk) * (VRe.get(l) * VIm.get(k) - VIm.get(l) * VRe.get(k)));
        }
        return 0;
    }

    /**
     * @return reactive power flow for line (l,k) on side l for a given state vector x = (VRe, VIm, B)
     */
    public static double Qf(int l, int k, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                            HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        String lk = l + "_" + k;
        if (B.containsKey(lk)) {
            return B.get(lk) * (-b.get(lk) * (Math.pow(VRe.get(l), 2) + Math.pow(VIm.get(k), 2))
                    + g.get(lk) * (VRe.get(l) * VIm.get(k) - VIm.get(l) * VRe.get(k))
                    + b.get(lk) * (VRe.get(l) * VRe.get(k) + VIm.get(l) * VIm.get(k)));
        }
        return 0;
    }
}
/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.preprocessing;

import java.util.HashMap;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */


public class JacobianFunctions {

    /**
     * @return the derivative of P[l] w.r.t VRe[m] for a given state vector x = (VRe, VIm, B)
     */
    public static double dP_dVRe(int l, int m, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                                 HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double dP = 0;
        int N = VRe.size();
        if (l == m) {
            for (int k = 1; k <= N; k++) {
                if (k != l) {
                    String lk = l + "_" + k;
                    if (B.containsKey(lk)) {
                        dP += B.get(lk) * (2 * g.get(lk) * VRe.get(l) - g.get(lk) * VRe.get(k) + b.get(lk) * VIm.get(k));
                    }
                }
            }
        } else {
            String lm = l + "_" + m;
            if (B.containsKey(lm)) {
                dP = B.get(lm) * (-g.get(lm) * VRe.get(l) - b.get(lm) * VIm.get(l));
            }
        }
        return dP;
    }

    /**
     * @return the derivative of P[l] w.r.t VIm[m] for a given state vector x = (VRe, VIm, B)
     */
    public static double dP_dVIm(int l, int m, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                                 HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double dP = 0;
        int N = VRe.size();
        if (l == m) {
            for (int k = 1; k <= N; k++) {
                if (k != l) {
                    String lk = l + "_" + k;
                    if (B.containsKey(lk)) {
                        dP += B.get(lk) * (-g.get(lk) * VIm.get(k) - b.get(lk) * VRe.get(k));
                    }
                }
            }
        } else {
            String lm = l + "_" + m;
            if (B.containsKey(lm)) {
                dP = B.get(lm) * (2 * g.get(lm) * VIm.get(m) - g.get(lm) * VIm.get(l) + b.get(lm) * VRe.get(l));
            }
        }
        return dP;
    }

    /**
     * @return the derivative of P[l] w.r.t B[m,n] for a given state vector x = (VRe, VIm, B)
     */
    public static double dP_dB(int l, int m, int n, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                               HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        if (l == m) {
            String ln = l + "_" + n;
            return g.get(ln) * (Math.pow(VRe.get(l), 2) + Math.pow(VIm.get(n), 2)) - g.get(ln) * (VRe.get(l) * VRe.get(n) + VIm.get(l) * VIm.get(n))
                    + b.get(ln) * (VRe.get(l) * VIm.get(n) - VIm.get(l) * VRe.get(n));
        } else if (l == n) {
            String lm = l + "_" + m;
            return g.get(lm) * (Math.pow(VRe.get(l), 2) + Math.pow(VIm.get(m), 2)) - g.get(lm) * (VRe.get(l) * VRe.get(m) + VIm.get(l) * VIm.get(m))
                    + b.get(lm) * (VRe.get(l) * VIm.get(m) - VIm.get(l) * VRe.get(m));
        } else {
            return 0;
        }
    }

    /**
     * @return the derivative of Q[l] w.r.t VRe[m] for a given state vector x = (VRe, VIm, B)
     */
    public static double dQ_dVRe(int l, int m, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                                 HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double dQ = 0;
        int N = VRe.size(); // get number of nodes
        if (l == m) {
            for (int k = 1; k <= N; k++) {
                if (k != l) {
                    String lk = l + "_" + k;
                    if (B.containsKey(lk)) {
                        dQ += B.get(lk) * (-2 * b.get(lk) * VRe.get(l) + g.get(lk) * VIm.get(k) + b.get(lk) * VRe.get(k));
                    }
                }
            }
        } else {
            String lm = l + "_" + m;
            if (B.containsKey(lm)) {
                dQ = B.get(lm) * (-g.get(lm) * VIm.get(l) + b.get(lm) * VRe.get(l));
            }
        }
        return dQ;
    }

    /**
     * @return the derivative of Q[l] w.r.t VIm[m] for a given state vector x = (VRe, VIm, B)
     */
    public static double dQ_dVIm(int l, int m, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                                 HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double dQ = 0;
        int N = VRe.size(); // get number of nodes
        if (l == m) {
            for (int k = 1; k <= N; k++) {
                if (k != l) {
                    String lk = l + "_" + k;
                    if (B.containsKey(lk)) {
                        dQ += B.get(lk) * (-g.get(lk) * VRe.get(k) + b.get(lk) * VIm.get(k));
                    }
                }
            }
        } else {
            String lm = l + "_" + m;
            if (B.containsKey(lm)) {
                dQ = B.get(lm) * (-2 * b.get(lm) * VIm.get(m) + g.get(lm) * VRe.get(l) + b.get(lm) * VIm.get(l));
            }
        }
        return dQ;
    }

    /**
     * @return the derivative of Q[l] w.r.t B[m,n] for a given state vector x = (VRe, VIm, B)
     */
    public static double dQ_dB(int l, int m, int n, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                               HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double dQ;
        if (l == m) {
            String ln = l + "_" + n;
            dQ = -b.get(ln) * (Math.pow(VRe.get(l), 2) + Math.pow(VIm.get(n), 2))
                    + b.get(ln) * (VRe.get(l) * VRe.get(n) + VIm.get(l) * VIm.get(n))
                    + g.get(ln) * (VRe.get(l) * VIm.get(n) - VIm.get(l) * VRe.get(n));
        } else if (l == n) {
            String lm = l + "_" + m;
            dQ = VRe.get(l) * VIm.get(m) - VIm.get(l) * VRe.get(m);
        } else {
            dQ = 0;
        }
        return dQ;
    }

    /**
     * @return the derivative of |V[l]|^2 w.r.t VRe[m] for a given state vector x = (VRe, VIm, B)
     */
    public static double dV2_dVRe(int l, int m, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                                  HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double dV2;
        if (l == m) {
            dV2 = 2 * VRe.get(l);
        } else {
            dV2 = 0;
        }
        return dV2;
    }

    /**
     * @return the derivative of |V[l]|^2 w.r.t VIm[m] for a given state vector x = (VRe, VIm, B)
     */
    public static double dV2_dVIm(int l, int m, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                                  HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double dV2;
        if (l == m) {
            dV2 = 2 * VIm.get(l);
        } else {
            dV2 = 0;
        }
        return dV2;
    }

    /**
     * @return the derivative of |V[l]|^2 w.r.t B[m,n] for a given state vector x = (VRe, VIm, B)
     */
    public static double dV2_dB(int l, int m, int n, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                                HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        return 0;
    }

    /**
     * @return the derivative of Pf[l,k] w.r.t VRe[m] for a given state vector x = (VRe, VIm, B)
     */
    public static double dPf_dVRe(int l, int k, int m, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                                  HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double dPf = 0;
        if (m == l) {
            String lk = l + "_" + k;
            dPf = B.get(lk) * (2 * g.get(lk) * VRe.get(l) - g.get(lk) * VRe.get(k) + b.get(lk) * VIm.get(k));
        } else if (m == k) {
            String lk = l + "_" + k;
            dPf = B.get(lk) * (-g.get(lk) * VRe.get(l) - b.get(lk) * VIm.get(l));
        }
        return dPf;
    }

    /**
     * @return the derivative of Pf[l,k] w.r.t VIm[m] for a given state vector x = (VRe, VIm, B)
     */
    public static double dPf_dVIm(int l, int k, int m, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                                  HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double dPf = 0;
        if (m == l) {
            String lk = l + "_" + k;
            dPf = B.get(lk) * (-g.get(lk) * VIm.get(k) - b.get(lk) * VRe.get(k));
        } else if (m == k) {
            String lk = l + "_" + k;
            dPf = B.get(lk) * (2 * g.get(lk) * VIm.get(k) - g.get(lk) * VIm.get(l) + b.get(lk) * VRe.get(l));
        }
        return dPf;
    }

    /**
     * @return the derivative of Pf[l,k] w.r.t B[m,n] for a given state vector x = (VRe, VIm, B)
     */
    public static double dPf_dB(int l, int k, int m, int n, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                                HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double dPf = 0;
        if ((m == l && n == k) || (m == k && n == l)) {
            String lk = l + "_" + k;
            dPf = g.get(lk) * (Math.pow(VRe.get(l), 2) + Math.pow(VIm.get(k), 2))
                    - g.get(lk) * (VRe.get(l) * VRe.get(k) + VIm.get(l) * VIm.get(k))
                    + b.get(lk) * (VRe.get(l) * VIm.get(k) - VIm.get(l) * VRe.get(k));
        }
        return dPf;
    }

    /**
     * @return the derivative of Qf[l,k] w.r.t VRe[m] for a given state vector x = (VRe, VIm, B)
     */
    public static double dQf_dVRe(int l, int k, int m, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                                  HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double dQf = 0;
        if (m == l) {
            String lk = l + "_" + k;
            dQf = B.get(lk) * (-2 * b.get(lk) * VRe.get(l) + g.get(lk) * VIm.get(k) + b.get(lk) * VRe.get(k));
        } else if (m == k) {
            String lk = l + "_" + k;
            dQf = B.get(lk) * (-g.get(lk) * VIm.get(l) + b.get(lk) * VRe.get(l));
        }
        return dQf;
    }

    /**
     * @return the derivative of Qf[l,k] w.r.t VIm[m] for a given state vector x = (VRe, VIm, B)
     */
    public static double dQf_dVIm(int l, int k, int m, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                                  HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double dQf = 0;
        if (m == l) {
            String lk = l + "_" + k;
            dQf = B.get(lk) * (-g.get(lk) * VRe.get(k) + b.get(lk) * VIm.get(k));
        } else if (m == k) {
            String lk = l + "_" + k;
            dQf = B.get(lk) * (-2 * b.get(lk) * VIm.get(k) + g.get(lk) * VRe.get(l) + b.get(lk) * VIm.get(l));
        }
        return dQf;
    }

    /**
     * @return the derivative of Qf[l,k] w.r.t B[m,n] for a given state vector x = (VRe, VIm, B)
     */
    public static double dQf_dB(int l, int k, int m, int n, HashMap<Integer, Double> VRe, HashMap<Integer, Double> VIm,
                                HashMap<String, Double> B, HashMap<String, Double> g, HashMap<String, Double> b) {
        double dQf = 0;
        if ((m == l && n == k) || (m == k && n == l)) {
            String lk = l + "_" + k;
            dQf = -b.get(lk) * (Math.pow(VRe.get(l), 2) + Math.pow(VIm.get(k), 2))
                    + g.get(lk) * (VRe.get(l) * VIm.get(k) - VIm.get(l) * VRe.get(k))
                    + b.get(lk) * (VRe.get(l) * VRe.get(k) + VIm.get(l) * VIm.get(k));
        }
        return dQf;
    }
}


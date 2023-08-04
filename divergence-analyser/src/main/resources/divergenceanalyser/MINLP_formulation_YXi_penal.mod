###############################################################################
#
# Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
###############################################################################

###############################################################################
# Divergence analysis
# Author : Jean Maeght 2022 2023
# Author : Pierre Arvy 2023
###############################################################################

# Problem of optimization. Corresponds to detection of inconsistencies with data penalization
set PROBLEM_DETECTION_PARAM default { };

# Activation/desactivation of binary penal variables
subject to activ_penal_for_units_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PV_UNITS}:          if (is_target_v_units == 0)     then b_s1[n] = 0;
subject to activ_penal_for_svc_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PV_SVC}:              if (is_target_v_svc == 0)       then b_s1[n] = 0;
subject to ctr_b_sigma1_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_TRANSFORMER}: if (is_rho_control == 0)        then b_sigma1[qq,m,n] = 0;
subject to ctr_b_sigma2_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   if (is_admittance_control == 0) then b_sigma2[qq,m,n] = 0;
subject to ctr_b_sigma3_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_DEPH}:        if (is_phase_shif_control == 0) then b_sigma3[qq,m,n] = 0;
subject to ctr_b_sigma4_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   if (is_xi_control == 0)         then b_sigma4[qq,m,n] = 0;
subject to ctr_b_sigma5_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   if (is_g_shunt_1_control == 0)  then b_sigma5[qq,m,n] = 0;
subject to ctr_b_sigma6_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   if (is_b_shunt_1_control == 0)  then b_sigma6[qq,m,n] = 0;
subject to ctr_b_sigma7_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   if (is_g_shunt_2_control == 0)  then b_sigma7[qq,m,n] = 0;
subject to ctr_b_sigma8_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   if (is_b_shunt_2_control == 0)  then b_sigma8[qq,m,n] = 0;


# Lower bounds of penalized data
subject to ctr_s1_units_min_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PV_UNITS}:              targetV_busPV[n] + s1[n]                 >= targetV_p_inf;
subject to ctr_s1_svc_min_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PV_SVC}:                  targetV_busPV[n] + s1[n]                 >= targetV_p_inf;
subject to ctr_sigma1_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_TRANSFORMER}:   branch_Ror[qq,m,n] + sigma1[qq,m,n]      >= rho_p_inf[qq,m,n];
subject to ctr_sigma2_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:     branch_admi[qq,m,n] + sigma2[qq,m,n]     >= Y_p_inf[qq,m,n];
subject to ctr_sigma3_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_DEPH}:          branch_dephor[qq,m,n] + sigma3[qq,m,n]   >= alpha_p_inf[qq,m,n];
subject to ctr_sigma4_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:     branch_angper[qq,m,n] + sigma4[qq,m,n]   >= xi_p_inf[qq,m,n];
subject to ctr_sigma5_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:     branch_Gor[1,qq,m,n] + sigma5[qq,m,n]    >= G1_p_inf[qq,m,n];
subject to ctr_sigma6_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:     branch_Bor[1,qq,m,n] + sigma6[qq,m,n]    >= B1_p_inf[qq,m,n];
subject to ctr_sigma7_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:     branch_Gex[1,qq,m,n] + sigma7[qq,m,n]    >= G2_p_inf[qq,m,n];
subject to ctr_sigma8_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:     branch_Bex[1,qq,m,n] + sigma8[qq,m,n]    >= B2_p_inf[qq,m,n];

# Upper bounds of penalized data
subject to ctr_s1_units_max_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PV_UNITS}:              targetV_busPV[n] + s1[n]                <= targetV_p_sup;
subject to ctr_s1_svc_max_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PV_SVC}:                  targetV_busPV[n] + s1[n]                <= targetV_p_sup;
subject to ctr_sigma1_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_TRANSFORMER}:   branch_Ror[qq,m,n] + sigma1[qq,m,n]     <= rho_p_sup[qq,m,n];
subject to ctr_sigma2_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:     branch_admi[qq,m,n] + sigma2[qq,m,n]    <= Y_p_sup[qq,m,n];
subject to ctr_sigma3_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_DEPH}:          branch_dephor[qq,m,n] + sigma3[qq,m,n]  <= alpha_p_sup[qq,m,n];
subject to ctr_sigma4_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:     branch_angper[qq,m,n] + sigma4[qq,m,n]  <= xi_p_sup[qq,m,n];
subject to ctr_sigma5_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:     branch_Gor[1,qq,m,n] + sigma5[qq,m,n]   <= G1_p_sup[qq,m,n];
subject to ctr_sigma6_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:     branch_Bor[1,qq,m,n] + sigma6[qq,m,n]   <= B1_p_sup[qq,m,n];
subject to ctr_sigma7_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:     branch_Gex[1,qq,m,n] + sigma7[qq,m,n]   <= G2_p_sup[qq,m,n];
subject to ctr_sigma8_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:     branch_Bex[1,qq,m,n] + sigma8[qq,m,n]   <= B2_p_sup[qq,m,n];

        # Activation of binary variables if penal var > 0
        subject to ctr_b_s1_pos_dbp{PROBLEM_DETECTION_PARAM, k in BUSCC_PV}:                                s1[k]  <= (targetV_p_sup - targetV_busPV[k] + 0.001) * b_s1[k];
        subject to ctr_b_sigma1_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_TRANSFORMER}: sigma1[qq,m,n] <= (rho_p_sup[qq,m,n] - branch_Ror[qq,m,n] + 0.001) * b_sigma1[qq,m,n];
        subject to ctr_b_sigma2_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   sigma2[qq,m,n] <= (Y_p_sup[qq,m,n] - branch_admi[qq,m,n] + 0.001) * b_sigma2[qq,m,n];
        subject to ctr_b_sigma3_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_DEPH}:        sigma3[qq,m,n] <= (alpha_p_sup[qq,m,n] - branch_dephor[qq,m,n] + 0.001) * b_sigma3[qq,m,n];
        subject to ctr_b_sigma4_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   sigma4[qq,m,n] <= (xi_p_sup[qq,m,n] - branch_angper[qq,m,n] + 0.001) * b_sigma4[qq,m,n];
        subject to ctr_b_sigma5_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   sigma5[qq,m,n] <= (G1_p_sup[qq,m,n] - branch_Gor[1,qq,m,n] + 0.001) * b_sigma5[qq,m,n];
        subject to ctr_b_sigma6_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   sigma6[qq,m,n] <= (B1_p_sup[qq,m,n] - branch_Bor[1,qq,m,n] + 0.001) * b_sigma6[qq,m,n];
        subject to ctr_b_sigma7_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   sigma7[qq,m,n] <= (G2_p_sup[qq,m,n] - branch_Gex[1,qq,m,n] + 0.001) * b_sigma7[qq,m,n];
        subject to ctr_b_sigma8_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   sigma8[qq,m,n] <= (B2_p_sup[qq,m,n] - branch_Bex[1,qq,m,n] + 0.001) * b_sigma8[qq,m,n];

        # Activation of binary variables if penal var < 0
        subject to ctr_b_s1_neg_dbp{PROBLEM_DETECTION_PARAM, k in BUSCC_PV}:                                 s1[k] >= (targetV_p_inf - 0.001 - targetV_busPV[k]) * b_s1[k];
        subject to ctr_b_sigma1_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_TRANSFORMER}: sigma1[qq,m,n] >= (rho_p_inf[qq,m,n] - branch_Ror[qq,m,n] - 0.001) * b_sigma1[qq,m,n];
        subject to ctr_b_sigma2_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   sigma2[qq,m,n] >= (Y_p_inf[qq,m,n] - branch_admi[qq,m,n]- 0.001) * b_sigma2[qq,m,n];
        subject to ctr_b_sigma3_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_DEPH}:        sigma3[qq,m,n] >= (alpha_p_inf[qq,m,n] - branch_dephor[qq,m,n]- 0.001) * b_sigma3[qq,m,n];
        subject to ctr_b_sigma4_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   sigma4[qq,m,n] >= (xi_p_inf[qq,m,n] - branch_angper[qq,m,n]- 0.001) * b_sigma4[qq,m,n];
        subject to ctr_b_sigma5_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   sigma5[qq,m,n] >= (G1_p_inf[qq,m,n] - branch_Gor[1,qq,m,n]- 0.001) * b_sigma5[qq,m,n];
        subject to ctr_b_sigma6_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   sigma6[qq,m,n] >= (B1_p_inf[qq,m,n] - branch_Bor[1,qq,m,n]- 0.001) * b_sigma6[qq,m,n];
        subject to ctr_b_sigma7_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   sigma7[qq,m,n] >= (G2_p_inf[qq,m,n] - branch_Gex[1,qq,m,n]- 0.001) * b_sigma7[qq,m,n];
        subject to ctr_b_sigma8_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}:   sigma8[qq,m,n] >= (B2_p_inf[qq,m,n] - branch_Bex[1,qq,m,n]- 0.001) * b_sigma8[qq,m,n];

#####################################
#                                   #
#     Load flow var/constraints     #
#                                   #
#####################################

# Consistency for voltage magnitude of PQ-buses
subject to ctr_voltage_values_min_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PQ}: V[n] <= 1.25;
subject to ctr_voltage_values_max_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PQ}: V[n] >= 0.75;

# Consistency for voltage angle of buses
subject to ctr_voltage_angle_min{PROBLEM_DETECTION_PARAM, n in BUSCC}: teta[n] <= 3.141592;
subject to ctr_voltage_angle_max{PROBLEM_DETECTION_PARAM, n in BUSCC}: teta[n] >= -3.141592;

# Slack bus constraint
subject to ctr_null_phase_bus_dbp{PROBLEM_DETECTION_PARAM}: teta[null_phase_bus] = 0;

# Voltage regulation constraint
subject to ctr_voltage_PV_buses_dbp{PROBLEM_DETECTION_PARAM,k in BUSCC_PV}: V[k] - target_v_penal[k] = 0;

############################################################
#     Active and reactive powers variables/constraints     #
############################################################

# Active Balance for all buses except slack bus
subject to ctr_balance_P_dbp{PROBLEM_DETECTION_PARAM,k in BUSCC diff {null_phase_bus}}:
  # Flows on branches
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * act_power_dir_YXi_penal[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * act_power_inv_YXi_penal[qq,m,k]
  # Flows on branches with one side opened
  + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED} base100MVA * V[k] * act_power_bus2_opened_YXi_penal[qq,k,n]
  + sum{(qq,m,k) in BRANCH_WITH_SIDE_1_OPENED} base100MVA * V[k] * act_power_bus1_opened_YXi_penal[qq,m,k]
  # Generating units
  - sum{(g,k) in UNITCC} unit_Pc[1,g,k] # Fixed value
  # Loads
  + sum{(c,k) in LOADCC} load_PFix[1,c,k]     # Fixed value
  # VSC converters
  - sum{(v,k) in VSCCONVON} vscconv_P0[1,v,k] # Fixed value
  # LCC converters
  + sum{(l,k) in LCCCONVON} lccconv_P0[1,l,k] # Fixed value
  = 0;

# Reactive Balance for PQ buses
subject to ctr_balance_Q_dbp{PROBLEM_DETECTION_PARAM,k in BUSCC_PQ}:
  # Flows
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * rea_power_dir_YXi_penal[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * rea_power_inv_YXi_penal[qq,m,k]
  # Flows on branches with one side opened
  + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED} base100MVA * V[k] * rea_power_bus2_opened_YXi_penal[qq,k,n]
  + sum{(qq,m,k) in BRANCH_WITH_SIDE_1_OPENED} base100MVA * V[k] * rea_power_bus1_opened_YXi_penal[qq,m,k]
  # Generating units
  - sum{(g,k) in UNITCC} unit_Qc[1,g,k] # Fixed value
  # Load
  + sum{(c,k) in LOADCC} load_QFix[1,c,k] # Fixed value
  # Shunts
  - sum{(shunt,k) in SHUNTCC} base100MVA * shunt_valnom[1,shunt,k] * V[k]^2
  # SVC that does not regulate voltage
  - sum{(svc,k) in SVCCC_PQ_1 : -1000 <= svc_Q0[1,svc,k] and svc_Q0[1,svc,k] <= 1000} svc_Q0[1,svc,k] # Fixed value
  - sum{(svc,k) in SVCCC_PQ_2} if bus_V0[1,k] > svc_targetV[1,svc,k]
                              then base100MVA * svc_bmin[1,svc,k] * V[k]^2
                              else base100MVA * svc_bmax[1,svc,k] * V[k]^2
  # VSC converters
  - sum{(v,k) in VSCCONVON} vscconv_Q0[1,v,k] # Fixed values
  # LCC converters
  + sum{(l,k) in LCCCONVON} lccconv_Q0[1,l,k] # Fixed values
  = 0;



#subject to ctr_thetas_diff_np{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_DIFF_THETA_CONST}:
#      abs(teta[m] + branch_dephor[qq,m,n] - teta[n]) <=
#      if (qq,m,n) in BRANCHCC_TRANSFORMER and abs(branch_Xdeph[qq,m,n]) > 0.05 then
#      min(1.570796 / 2, abs(1.732 * max(substation_Vnomi[1,bus_substation[1,m]]*abs(branch_patl1[1,qq,m,n]),substation_Vnomi[1,bus_substation[1,n]]*abs(branch_patl2[1,qq,m,n])) * abs(branch_Xdeph[qq,m,n])
#      / 100 / 100 / 0.9))
#      else if abs(branch_X_mod[qq,m,n]) > 0.05 then
#      min(1.570796 / 2, abs(1.732 * max(substation_Vnomi[1,bus_substation[1,m]]*abs(branch_patl1[1,qq,m,n]),substation_Vnomi[1,bus_substation[1,n]]*abs(branch_patl2[1,qq,m,n])) * abs(branch_X_mod[qq,m,n])
#       / 100 / 100 / 0.9))
#       else 1.570796 / 2
#      ;
#subject to ctr_on_v{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}:
#    abs(V[m] - V[n]) <= 0.1;


###########################################
#                                         #
#             Objective function          #
#                                         #
###########################################


minimize problem_dbp:
  0
  + 1 * sum{n in BUSCC_PV}                    b_s1[n]
  + 1 * sum{(qq,m,n) in BRANCHCC_TRANSFORMER} b_sigma1[qq,m,n]
  + 1.5 * sum{(qq,m,n) in BRANCHCC_PENALIZED}   b_sigma2[qq,m,n]
  + 1 * sum{(qq,m,n) in BRANCHCC_DEPH}        b_sigma3[qq,m,n]
  + 1.5 * sum{(qq,m,n) in BRANCHCC_PENALIZED}   b_sigma4[qq,m,n]
  + 1 * sum{(qq,m,n) in BRANCHCC_PENALIZED}   b_sigma5[qq,m,n]
  + 1 * sum{(qq,m,n) in BRANCHCC_PENALIZED}   b_sigma6[qq,m,n]
  + 1 * sum{(qq,m,n) in BRANCHCC_PENALIZED}   b_sigma7[qq,m,n]
  + 1 * sum{(qq,m,n) in BRANCHCC_PENALIZED}   b_sigma8[qq,m,n]
  ;
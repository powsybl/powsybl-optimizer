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

###############################################################################
#                                                                             #
#                 OPT PROBLEM FOR ERROR DETECTION BY BRANCHES                 #
#                                                                             #
###############################################################################

set PROBLEM_DETECTION_BRANCHES default { };

# Activation/desactivation of penal variables
subject to ctr_s1_null_dbb{PROBLEM_DETECTION_BRANCHES, n in BUSCC_PV}: if(is_voltage_rho_control == 0) then s1[n] = 0;
subject to ctr_sigma1_null_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_REGL}: if(is_voltage_rho_control == 0) then sigma1[qq,m,n] = 0;
subject to ctr_sigma2_null_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_admi_xi_control == 0) then sigma2[qq,m,n] = 0;
subject to ctr_sigma3_null_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_DEPH}: if(is_angle_deph_control == 0) then sigma3[qq,m,n] = 0;
subject to ctr_sigma4_null_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_admi_xi_control == 0) then sigma4[qq,m,n] = 0;
subject to ctr_sigma5_null_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_G_B_control == 0) then sigma5[qq,m,n] = 0;
subject to ctr_sigma6_null_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_G_B_control == 0) then sigma6[qq,m,n] = 0;
subject to ctr_sigma7_null_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_G_B_control == 0) then sigma7[qq,m,n] = 0;
subject to ctr_sigma8_null_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_G_B_control == 0) then sigma8[qq,m,n] = 0;

#
# Bounds for penal variables. Are based on job knowledge
#

# Lower bounds of penal var
subject to ctr_s1_min_dbb{PROBLEM_DETECTION_BRANCHES, n in BUSCC_PV}: s1[n] + targetV_busPV[n] >= s1_inf;
subject to ctr_sigma1_min_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_REGL}: sigma1[qq,m,n] + branch_Ror[qq,m,n] >= sigma1_inf[qq,m,n];
subject to ctr_sigma2_min_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma2[qq,m,n] + branch_G[qq,m,n] >= sigma2_inf;
subject to ctr_sigma3_min_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_DEPH}: sigma3[qq,m,n] + branch_dephor[qq,m,n] >= sigma3_inf[qq,m,n];
subject to ctr_sigma4_min_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma4[qq,m,n] + branch_B[qq,m,n] >= sigma4_inf;
subject to ctr_sigma5_min_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma5[qq,m,n] + branch_Gor_corrected[1,qq,m,n] >= sigma5_inf;
subject to ctr_sigma6_min_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma6[qq,m,n] + branch_Bor_corrected[1,qq,m,n] >= sigma6_inf;
subject to ctr_sigma7_min_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma7[qq,m,n] + branch_Gex_corrected[1,qq,m,n] >= sigma7_inf;
subject to ctr_sigma8_min_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma8[qq,m,n] + branch_Bex_corrected[1,qq,m,n] >= sigma8_inf;

# Upper bounds of penal var
subject to ctr_s1_max_dbb{PROBLEM_DETECTION_BRANCHES, n in BUSCC_PV}: s1[n] + targetV_busPV[n] <= s1_sup;
subject to ctr_sigma1_max_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_REGL}: sigma1[qq,m,n] + branch_Ror[qq,m,n] <= sigma1_sup[qq,m,n];
subject to ctr_sigma2_max_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma2[qq,m,n] + branch_G[qq,m,n] <= sigma2_sup;
subject to ctr_sigma3_max_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_DEPH}: sigma3[qq,m,n] + branch_dephor[qq,m,n] <= sigma3_sup[qq,m,n];
subject to ctr_sigma4_max_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma4[qq,m,n] + branch_B[qq,m,n] <= sigma4_sup;
subject to ctr_sigma5_max_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma5[qq,m,n] + branch_Gor_corrected[1,qq,m,n] <= sigma5_sup[qq,m,n];
subject to ctr_sigma6_max_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma6[qq,m,n] + branch_Bor_corrected[1,qq,m,n] <= sigma6_sup[qq,m,n];
subject to ctr_sigma7_max_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma7[qq,m,n] + branch_Gex_corrected[1,qq,m,n] <= sigma7_sup[qq,m,n];
subject to ctr_sigma8_max_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma8[qq,m,n] + branch_Bex_corrected[1,qq,m,n] <= sigma8_sup[qq,m,n];

#
# Activation of b variables if s/sigma var != 0
#

# if s/sigma variable > 0
subject to ctr_b_s1_pos_dbb{PROBLEM_DETECTION_BRANCHES, k in BUSCC_PV}: s1[k] <= (s1_sup - targetV_busPV[k] + 0.001) * b_s1[k];
subject to ctr_b_sigma1_pos_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_REGL}: sigma1[qq,m,n] <= (sigma1_sup[qq,m,n] - branch_Ror[qq,m,n] + 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma2_pos_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma2[qq,m,n] <= (sigma2_sup - branch_G[qq,m,n] + 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma3_pos_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_DEPH}: sigma3[qq,m,n] <= (sigma3_sup[qq,m,n] - branch_dephor[qq,m,n] + 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma4_pos_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma4[qq,m,n] <= (sigma4_sup - branch_B[qq,m,n] + 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma5_pos_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma5[qq,m,n] <= (sigma5_sup[qq,m,n] - branch_Gor_corrected[1,qq,m,n] + 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma6_pos_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma6[qq,m,n] <= (sigma6_sup[qq,m,n] - branch_Bor_corrected[1,qq,m,n] + 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma7_pos_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma7[qq,m,n] <= (sigma7_sup[qq,m,n] - branch_Gex_corrected[1,qq,m,n] + 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma8_pos_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma8[qq,m,n] <= (sigma8_sup[qq,m,n] - branch_Bex_corrected[1,qq,m,n] + 0.001) * b_branch[qq,m,n];

# if s/sigma variable < 0
subject to ctr_b_s1_neg_dbb{PROBLEM_DETECTION_BRANCHES, k in BUSCC_PV}: s1[k] >= (s1_inf - targetV_busPV[k] - 0.001) * b_s1[k];
subject to ctr_b_sigma1_neg_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_REGL}: sigma1[qq,m,n] >= (sigma1_inf[qq,m,n] - branch_Ror[qq,m,n] - 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma2_neg_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma2[qq,m,n] >= (sigma2_inf - branch_G[qq,m,n] - 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma3_neg_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_DEPH}: sigma3[qq,m,n] >= (sigma3_inf[qq,m,n] - branch_dephor[qq,m,n] - 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma4_neg_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma4[qq,m,n] >= (sigma4_inf - branch_B[qq,m,n] - 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma5_neg_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma5[qq,m,n] >= (sigma5_inf - branch_Gor_corrected[1,qq,m,n] - 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma6_neg_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma6[qq,m,n] >= (sigma6_inf - branch_Bor_corrected[1,qq,m,n] - 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma7_neg_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma7[qq,m,n] >= (sigma7_inf - branch_Gex_corrected[1,qq,m,n] - 0.001) * b_branch[qq,m,n];
subject to ctr_b_sigma8_neg_dbb{PROBLEM_DETECTION_BRANCHES, (qq,m,n) in BRANCHCC_PENALIZED}: sigma8[qq,m,n] >= (sigma8_inf - branch_Bex_corrected[1,qq,m,n] - 0.001) * b_branch[qq,m,n];

#####################################
#                                   #
#     Load flow var/constraints     #
#                                   #  
#####################################

# Consistency of voltage values for PQ-bus
subject to ctr_voltage_values_min_dbb{PROBLEM_DETECTION_BRANCHES, n in BUSCC_PQ}: V[n] <= 1.25;
subject to ctr_voltage_values_max_dbb{PROBLEM_DETECTION_BRANCHES, n in BUSCC_PQ}: V[n] >= 0.75;


subject to ctr_null_phase_bus_dbb{PROBLEM_DETECTION_BRANCHES}: teta[null_phase_bus] = 0;
subject to ctr_voltage_PV_buses_dbb{PROBLEM_DETECTION_BRANCHES,k in BUSCC_PV}: V[k] - targetV_busPV[k] + s1[k] = 0;

#
# Active Balance for all buses except slack bus
#

subject to ctr_balance_P_dbb{PROBLEM_DETECTION_BRANCHES,k in BUSCC diff {null_phase_bus}}:
  # Flows
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Dir_GB[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Inv_GB[qq,m,k]
  # Flows on branches with one side opened
  + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED} base100MVA * V[k] * Act_branch_bus_2_opened_GB[qq,k,n]
  + sum{(qq,m,k) in BRANCH_WITH_SIDE_1_OPENED} base100MVA * V[k] * Act_branch_bus_1_opened_GB[qq,m,k]
  # Generating units
  - sum{(g,k) in UNITCC} unit_Pc[1,g,k] # Fixed value
  # Loads
  + sum{(c,k) in LOADCC} load_PFix[1,c,k]     # Fixed value
  # VSC converters
  + sum{(v,k) in VSCCONVON} vscconv_P0[1,v,k] # Fixed value
  # LCC converters
  + sum{(l,k) in LCCCONVON} lccconv_P0[1,l,k] # Fixed value
  = 0;

#
# Reactive Balance for PQ buses
#

subject to ctr_balance_Q_dbb{PROBLEM_DETECTION_BRANCHES,k in BUSCC_PQ}: 
  # Flows
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Dir_GB[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Inv_GB[qq,m,k]
    # Flows on branches with one side opened
  + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED} base100MVA * V[k] * Rea_branch_bus_2_opened_GB[qq,k,n]
  + sum{(qq,m,k) in BRANCH_WITH_SIDE_1_OPENED} base100MVA * V[k] * Rea_branch_bus_1_opened_GB[qq,m,k]
  # Senerating units
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


###########################################
#                                         #
#             Objective function          #
#                                         #
###########################################

# penal of the gap between initial value and variable


minimize problem_dbb:
  0
  + sum{n in BUSCC_PV} b_s1[n]
  + sum{(qq,m,n) in BRANCHCC_PENALIZED} b_branch[qq,m,n]
  ;
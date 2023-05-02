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

set PROBLEM_DETECTION_PARAM default { };

# Activation/desactivation of penal variables
subject to ctr_s1_null_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PV}: if(is_voltage_rho_control == 0) then s1[n] = 0;
subject to ctr_sigma1_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_voltage_rho_control == 0) then sigma1[qq,m,n] = 0;
subject to ctr_sigma2_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_admi_xi_control == 0) then sigma2[qq,m,n] = 0;
subject to ctr_sigma3_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_angle_deph_control == 0) then sigma3[qq,m,n] = 0;
subject to ctr_sigma4_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_admi_xi_control == 0) then sigma4[qq,m,n] = 0;
subject to ctr_sigma5_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_G_B_control == 0) then sigma5[qq,m,n] = 0;
subject to ctr_sigma6_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_G_B_control == 0) then sigma6[qq,m,n] = 0;
subject to ctr_sigma7_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_G_B_control == 0) then sigma7[qq,m,n] = 0;
subject to ctr_sigma8_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_G_B_control == 0) then sigma8[qq,m,n] = 0;

# Penalization is possible only on branches with a transfo
subject to ctr_sigma1_regl_branches_only_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED diff BRANCHCC_REGL}: sigma1[qq,m,n] = 0;
subject to ctr_sigma3_deph_branches_only_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED diff BRANCHCC_DEPH}: sigma3[qq,m,n] = 0;
subject to ctr_b_sigma1_regl_branches_only_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED diff BRANCHCC_REGL}: b_sigma1[qq,m,n] = 0;
subject to ctr_b_sigma3_deph_branches_only_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED diff BRANCHCC_DEPH}: b_sigma3[qq,m,n] = 0;

# Activation/desactivation of binary penal variables
subject to ctr_b_s1_null_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PV}: if(is_voltage_rho_control == 0) then b_s1[n] = 0;
subject to ctr_b_sigma1_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_voltage_rho_control == 0) then b_sigma1[qq,m,n] = 0;
subject to ctr_b_sigma2_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_admi_xi_control == 0) then b_sigma2[qq,m,n] = 0;
subject to ctr_b_sigma3_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_angle_deph_control == 0) then b_sigma3[qq,m,n] = 0;
subject to ctr_b_sigma4_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_admi_xi_control == 0) then b_sigma4[qq,m,n] = 0;
subject to ctr_b_sigma5_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_G_B_control == 0) then b_sigma5[qq,m,n] = 0;
subject to ctr_b_sigma6_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_G_B_control == 0) then b_sigma6[qq,m,n] = 0;
subject to ctr_b_sigma7_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_G_B_control == 0) then b_sigma7[qq,m,n] = 0;
subject to ctr_b_sigma8_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: if(is_G_B_control == 0) then b_sigma8[qq,m,n] = 0;

#
# Bounds for penal variables. Are based on job knowledge
#

# Lower bounds of penal var
subject to ctr_s1_min_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PV}: s1[n] + targetV_busPV[n] >= s1_inf;
subject to ctr_sigma1_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma1[qq,m,n] + branch_Ror[qq,m,n] >= sigma1_inf[qq,m,n];
subject to ctr_sigma2_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma2[qq,m,n] + branch_admi[qq,m,n] >= sigma2_inf;
subject to ctr_sigma3_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma3[qq,m,n] + branch_dephor[qq,m,n] >= sigma3_inf[qq,m,n];
subject to ctr_sigma4_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma4[qq,m,n] + branch_angper[qq,m,n] >= sigma4_inf;
subject to ctr_sigma5_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma5[qq,m,n] + branch_Gor[1,qq,m,n] >= sigma5_inf;
subject to ctr_sigma6_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma6[qq,m,n] + branch_Bor[1,qq,m,n] >= sigma6_inf;
subject to ctr_sigma7_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma7[qq,m,n] + branch_Gex[1,qq,m,n] >= sigma7_inf;
subject to ctr_sigma8_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma8[qq,m,n] + branch_Bex[1,qq,m,n] >= sigma8_inf;

# Upper bounds of penal var
subject to ctr_s1_max_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PV}: s1[n] + targetV_busPV[n] <= s1_sup;
subject to ctr_sigma1_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma1[qq,m,n] + branch_Ror[qq,m,n] <= sigma1_sup[qq,m,n];
subject to ctr_sigma2_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma2[qq,m,n] + branch_admi[qq,m,n] <= sigma2_sup;
subject to ctr_sigma3_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma3[qq,m,n] + branch_dephor[qq,m,n] <= sigma3_sup[qq,m,n];
subject to ctr_sigma4_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma4[qq,m,n] + branch_angper[qq,m,n] <= sigma4_sup;
subject to ctr_sigma5_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma5[qq,m,n] + branch_Gor[1,qq,m,n] <= sigma5_sup;
subject to ctr_sigma6_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma6[qq,m,n] + branch_Bor[1,qq,m,n] <= sigma6_sup;
subject to ctr_sigma7_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma7[qq,m,n] + branch_Gex[1,qq,m,n] <= sigma7_sup;
subject to ctr_sigma8_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma8[qq,m,n] + branch_Bex[1,qq,m,n] <= sigma8_sup;

# Rho+sigma1 entre 0.5 et 2. Y entre admi_min. Enlever 1 pour tout ce qui n'est pas min/max. Mettre 0.001 pour les autres.
#



#
# Activation of b variables if s/sigma var != 0
#

# if s/sigma variable > 0
subject to ctr_b_s1_pos_dbp{PROBLEM_DETECTION_PARAM, k in BUSCC_PV}: s1[k] <= (s1_sup - targetV_busPV[k] + 0.001) * b_s1[k];
subject to ctr_b_sigma1_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma1[qq,m,n] <= (sigma1_sup[qq,m,n] - branch_Ror[qq,m,n] + 0.001) * b_sigma1[qq,m,n];
subject to ctr_b_sigma2_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma2[qq,m,n] <= (sigma2_sup - branch_admi[qq,m,n] + 0.001) * b_sigma2[qq,m,n];
subject to ctr_b_sigma3_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma3[qq,m,n] <= (sigma3_sup[qq,m,n] - branch_dephor[qq,m,n] + 0.001) * b_sigma3[qq,m,n];
subject to ctr_b_sigma4_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma4[qq,m,n] <= (sigma4_sup - branch_angper[qq,m,n] + 0.001) * b_sigma4[qq,m,n];
subject to ctr_b_sigma5_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma5[qq,m,n] <= (sigma5_sup - branch_Gor[1,qq,m,n] + 0.001) * b_sigma5[qq,m,n];
subject to ctr_b_sigma6_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma6[qq,m,n] <= (sigma6_sup - branch_Bor[1,qq,m,n] + 0.001) * b_sigma6[qq,m,n];
subject to ctr_b_sigma7_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma7[qq,m,n] <= (sigma7_sup - branch_Gex[1,qq,m,n] + 0.001) * b_sigma7[qq,m,n];
subject to ctr_b_sigma8_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma8[qq,m,n] <= (sigma8_sup - branch_Bex[1,qq,m,n] + 0.001) * b_sigma8[qq,m,n];

# if s/sigma variable < 0
subject to ctr_b_s1_neg_dbp{PROBLEM_DETECTION_PARAM, k in BUSCC_PV}: s1[k] >= (s1_inf - 0.001 - targetV_busPV[k]) * b_s1[k];
subject to ctr_b_sigma1_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma1[qq,m,n] >= (sigma1_inf[qq,m,n] - branch_Ror[qq,m,n] - 0.001) * b_sigma1[qq,m,n];
subject to ctr_b_sigma2_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma2[qq,m,n] >= (sigma2_inf - branch_admi[qq,m,n]- 0.001) * b_sigma2[qq,m,n];
subject to ctr_b_sigma3_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma3[qq,m,n] >= (sigma3_inf[qq,m,n] - branch_dephor[qq,m,n]- 0.001) * b_sigma3[qq,m,n];
subject to ctr_b_sigma4_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma4[qq,m,n] >= (sigma4_inf - branch_angper[qq,m,n]- 0.001) * b_sigma4[qq,m,n];
subject to ctr_b_sigma5_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma5[qq,m,n] >= (sigma5_inf - branch_Gor[1,qq,m,n]- 0.001) * b_sigma5[qq,m,n];
subject to ctr_b_sigma6_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma6[qq,m,n] >= (sigma6_inf - branch_Bor[1,qq,m,n]- 0.001) * b_sigma6[qq,m,n];
subject to ctr_b_sigma7_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma7[qq,m,n] >= (sigma7_inf - branch_Gex[1,qq,m,n]- 0.001) * b_sigma7[qq,m,n];
subject to ctr_b_sigma8_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC_PENALIZED}: sigma8[qq,m,n] >= (sigma8_inf - branch_Bex[1,qq,m,n]- 0.001) * b_sigma8[qq,m,n];

#####################################
#                                   #
#     Load flow var/constraints     #
#                                   #  
#####################################


#############################################
#     Slack bus and voltage regulation      #
#############################################

subject to ctr_null_phase_bus_dbp{PROBLEM_DETECTION_PARAM}: teta[null_phase_bus] = 0;
subject to ctr_voltage_PV_buses_dbp{PROBLEM_DETECTION_PARAM,k in BUSCC_PV}: V[k] - targetV_busPV[k] + s1[k] = 0;

############################################################
#     Active and reactive powers variables/constraints     #  
############################################################

#
# Active Balance for all buses except slack bus
#

subject to ctr_balance_P_dbp{PROBLEM_DETECTION_PARAM,k in BUSCC diff {null_phase_bus}}:
  # Flows on branches
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Dir_YKsi[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Inv_YKsi[qq,m,k]
  # Flows on branches with a shunt on earth # TODO : CHANGE THE COMMENT HERE
  + sum{(qq,k,n) in BRANCH_WITH_SHUNT_1} base100MVA * V[k] * Act_branch_bus_2_opened_YKsi[qq,k,n]
  + sum{(qq,m,k) in BRANCH_WITH_SHUNT_2} base100MVA * V[k] * Act_branch_bus_1_opened_YKsi[qq,m,k]
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

subject to ctr_balance_Q_dbp{PROBLEM_DETECTION_PARAM,k in BUSCC_PQ}: 
  # Flows
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Dir_YKsi[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Inv_YKsi[qq,m,k]
  # Flows on branches with a shunt on earth # TODO : CHANGE THE COMMENT HERE
  + sum{(qq,k,n) in BRANCH_WITH_SHUNT_1} base100MVA * V[k] * Rea_branch_bus_2_opened_YKsi[qq,k,n]
  + sum{(qq,m,k) in BRANCH_WITH_SHUNT_2} base100MVA * V[k] * Rea_branch_bus_1_opened_YKsi[qq,m,k]
  # Senerating units
  - sum{(g,k) in UNITCC} unit_Qc[1,g,k] # Fixed value
  # Load
  + sum{(c,k) in LOADCC} load_QFix[1,c,k] # Fixed value
  # Shunts
  - sum{(shunt,k) in SHUNTCC} base100MVA * shunt_valnom[1,shunt,k] * V[k]^2
  # SVC that does not regulate voltage
  - sum{(svc,k) in SVCCC_PQ_1} svc_Q0[1,svc,k] # Fixed value
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


minimize problem_dbp:
  0
  + sum{n in BUSCC_PV} b_s1[n]
  + sum{(qq,m,n) in BRANCHCC_PENALIZED} b_sigma1[qq,m,n]
  + 8 * sum{(qq,m,n) in BRANCHCC_PENALIZED} b_sigma2[qq,m,n] # We do not want to move admittance too easily.
  + sum{(qq,m,n) in BRANCHCC_PENALIZED} b_sigma3[qq,m,n]
  + sum{(qq,m,n) in BRANCHCC_PENALIZED} b_sigma4[qq,m,n]
  + sum{(qq,m,n) in BRANCHCC_PENALIZED} b_sigma5[qq,m,n]
  + sum{(qq,m,n) in BRANCHCC_PENALIZED} b_sigma6[qq,m,n]
  + sum{(qq,m,n) in BRANCHCC_PENALIZED} b_sigma7[qq,m,n]
  + sum{(qq,m,n) in BRANCHCC_PENALIZED} b_sigma8[qq,m,n]
  ;
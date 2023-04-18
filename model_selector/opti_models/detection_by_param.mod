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
subject to ctr_sigma1_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_voltage_rho_control == 0) then sigma1[qq,m,n] = 0;
subject to ctr_sigma2_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_admi_xi_control == 0) then sigma2[qq,m,n] = 0;
subject to ctr_sigma3_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_angle_deph_control == 0) then sigma3[qq,m,n] = 0;
subject to ctr_sigma4_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_admi_xi_control == 0) then sigma4[qq,m,n] = 0;
subject to ctr_sigma5_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_G_B_control == 0) then sigma5[qq,m,n] = 0;
subject to ctr_sigma6_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_G_B_control == 0) then sigma6[qq,m,n] = 0;
subject to ctr_sigma7_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_G_B_control == 0) then sigma7[qq,m,n] = 0;
subject to ctr_sigma8_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_G_B_control == 0) then sigma8[qq,m,n] = 0;

# Penalization is possible only on branches with a transfo
subject to ctr_sigma1_regl_branches_only_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC diff BRANCHCC_REGL}: sigma1[qq,m,n] = 0;
subject to ctr_sigma3_deph_branches_only_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC diff BRANCHCC_DEPH}: sigma3[qq,m,n] = 0;
subject to ctr_b_sigma1_regl_branches_only_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC diff BRANCHCC_REGL}: b_sigma1[qq,m,n] = 0;
subject to ctr_b_sigma3_deph_branches_only_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC diff BRANCHCC_DEPH}: b_sigma3[qq,m,n] = 0;

# Activation/desactivation of binary penal variables
subject to ctr_b_s1_null_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PV}: if(is_voltage_rho_control == 0) then b_s1[n] = 0;
subject to ctr_b_sigma1_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_voltage_rho_control == 0) then b_sigma1[qq,m,n] = 0;
subject to ctr_b_sigma2_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_admi_xi_control == 0) then b_sigma2[qq,m,n] = 0;
subject to ctr_b_sigma3_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_angle_deph_control == 0) then b_sigma3[qq,m,n] = 0;
subject to ctr_b_sigma4_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_admi_xi_control == 0) then b_sigma4[qq,m,n] = 0;
subject to ctr_b_sigma5_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_G_B_control == 0) then b_sigma5[qq,m,n] = 0;
subject to ctr_b_sigma6_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_G_B_control == 0) then b_sigma6[qq,m,n] = 0;
subject to ctr_b_sigma7_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_G_B_control == 0) then b_sigma7[qq,m,n] = 0;
subject to ctr_b_sigma8_null_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: if(is_G_B_control == 0) then b_sigma8[qq,m,n] = 0;

#
# Bounds for penal variables. Are based on job knowledge
#

# Lower bounds of penal var
subject to ctr_s1_min_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PV}: s1[n] + targetV_busPV[n] >= 0.2;
subject to ctr_sigma1_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma1[qq,m,n] + branch_Ror[qq,m,n] >= 0;
subject to ctr_sigma2_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma2[qq,m,n] + branch_admi[qq,m,n] >= 0;
subject to ctr_sigma3_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma3[qq,m,n] + branch_dephor[qq,m,n] >= - 3.141592;
subject to ctr_sigma4_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma4[qq,m,n] + branch_angper[qq,m,n] >= - 3.141592;
subject to ctr_sigma5_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma5[qq,m,n] + branch_Gor[1,qq,m,n] >= -max_branch_Gor;
subject to ctr_sigma6_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma6[qq,m,n] + branch_Bor[1,qq,m,n] >= -max_branch_Bor;
subject to ctr_sigma7_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma7[qq,m,n] + branch_Gex[1,qq,m,n] >= -max_branch_Gex;
subject to ctr_sigma8_min_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma8[qq,m,n] + branch_Bex[1,qq,m,n] >= -max_branch_Bex;

# Upper bounds of penal var
subject to ctr_s1_max_dbp{PROBLEM_DETECTION_PARAM, n in BUSCC_PV}: s1[n] + targetV_busPV[n] <= 1.8;
subject to ctr_sigma1_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma1[qq,m,n] + branch_Ror[qq,m,n] <= 10;
subject to ctr_sigma2_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma2[qq,m,n] + branch_admi[qq,m,n] <= max_branch_admi;
subject to ctr_sigma3_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma3[qq,m,n] + branch_dephor[qq,m,n] <= 3.141592;
subject to ctr_sigma4_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma4[qq,m,n] + branch_angper[qq,m,n] <= 3.141592;
subject to ctr_sigma5_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma5[qq,m,n] + branch_Gor[1,qq,m,n] <= max_branch_Gor;
subject to ctr_sigma6_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma6[qq,m,n] + branch_Bor[1,qq,m,n] <= max_branch_Bor;
subject to ctr_sigma7_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma7[qq,m,n] + branch_Gex[1,qq,m,n] <= max_branch_Gex;
subject to ctr_sigma8_max_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma8[qq,m,n] + branch_Bex[1,qq,m,n] <= max_branch_Bex;

#
# Activation of b variables if s/sigma var != 0
#

# if s/sigma variable > 0
subject to ctr_b_s1_pos_dbp{PROBLEM_DETECTION_PARAM, k in BUSCC_PV}: s1[k] <= (1.8 - targetV_busPV[k]) * b_s1[k];
subject to ctr_b_sigma1_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma1[qq,m,n] <= (10 - branch_Ror[qq,m,n] + 1) * b_sigma1[qq,m,n];
subject to ctr_b_sigma2_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma2[qq,m,n] <= (max_branch_admi - branch_admi[qq,m,n] + 1) * b_sigma2[qq,m,n];
subject to ctr_b_sigma3_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma3[qq,m,n] <= (3.141592 - branch_dephor[qq,m,n] + 1) * b_sigma3[qq,m,n];
subject to ctr_b_sigma4_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma4[qq,m,n] <= (3.141592 - branch_angper[qq,m,n] + 1) * b_sigma4[qq,m,n];
subject to ctr_b_sigma5_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma5[qq,m,n] <= (max_branch_Gor - branch_Gor[1,qq,m,n] + 1) * b_sigma5[qq,m,n];
subject to ctr_b_sigma6_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma6[qq,m,n] <= (max_branch_Bor - branch_Bor[1,qq,m,n] + 1) * b_sigma6[qq,m,n];
subject to ctr_b_sigma7_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma7[qq,m,n] <= (max_branch_Gex - branch_Gex[1,qq,m,n] + 1) * b_sigma7[qq,m,n];
subject to ctr_b_sigma8_pos_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma8[qq,m,n] <= (max_branch_Bex - branch_Bex[1,qq,m,n] + 1) * b_sigma8[qq,m,n];

# if s/sigma variable < 0
subject to ctr_b_s1_neg_dbp{PROBLEM_DETECTION_PARAM, k in BUSCC_PV}: s1[k] >= (0.2 - 1 - targetV_busPV[k]) * b_s1[k];
subject to ctr_b_sigma1_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma1[qq,m,n] >= (0 - branch_Ror[qq,m,n] - 1) * b_sigma1[qq,m,n];
subject to ctr_b_sigma2_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma2[qq,m,n] >= (0 - branch_admi[qq,m,n]- 1) * b_sigma2[qq,m,n];
subject to ctr_b_sigma3_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma3[qq,m,n] >= (-3.141592 - branch_dephor[qq,m,n]- 1) * b_sigma3[qq,m,n];
subject to ctr_b_sigma4_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma4[qq,m,n] >= (-3.141592 - branch_angper[qq,m,n]- 1) * b_sigma4[qq,m,n];
subject to ctr_b_sigma5_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma5[qq,m,n] >= (-max_branch_Gor - branch_Gor[1,qq,m,n]- 1) * b_sigma5[qq,m,n];
subject to ctr_b_sigma6_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma6[qq,m,n] >= (-max_branch_Bor - branch_Bor[1,qq,m,n]- 1) * b_sigma6[qq,m,n];
subject to ctr_b_sigma7_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma7[qq,m,n] >= (-max_branch_Gex - branch_Gex[1,qq,m,n]- 1) * b_sigma7[qq,m,n];
subject to ctr_b_sigma8_neg_dbp{PROBLEM_DETECTION_PARAM, (qq,m,n) in BRANCHCC}: sigma8[qq,m,n] >= (-max_branch_Bex - branch_Bex[1,qq,m,n]- 1) * b_sigma8[qq,m,n];

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
# Flows in one direction, then the inverse
#

var Red_Tran_Act_Dir_dbp{(qq,m,n) in BRANCHCC} =
  (branch_Ror[qq,m,n]+sigma1[qq,m,n]) * V[n] * (branch_admi[qq,m,n]+sigma2[qq,m,n]) * sin(teta[m]-teta[n]+(branch_dephor[qq,m,n]+sigma3[qq,m,n])-(branch_angper[qq,m,n]+sigma4[qq,m,n]))
  + (branch_Ror[qq,m,n]+sigma1[qq,m,n])**2 * V[m] * ((branch_admi[qq,m,n]+sigma2[qq,m,n])*sin(branch_angper[qq,m,n]+sigma4[qq,m,n])+(branch_Gor[1,qq,m,n]+sigma5[qq,m,n]));

var Red_Tran_Rea_Dir_dbp{(qq,m,n) in BRANCHCC} = 
  - (branch_Ror[qq,m,n]+sigma1[qq,m,n]) * V[n] * (branch_admi[qq,m,n]+sigma2[qq,m,n]) * cos(teta[m]-teta[n]+(branch_dephor[qq,m,n]+sigma3[qq,m,n])-(branch_angper[qq,m,n]+sigma4[qq,m,n]))
  + (branch_Ror[qq,m,n]+sigma1[qq,m,n])**2 * V[m] * ((branch_admi[qq,m,n]+sigma2[qq,m,n])*cos(branch_angper[qq,m,n]+sigma4[qq,m,n])-(branch_Bor[1,qq,m,n]+sigma6[qq,m,n]));

var Red_Tran_Act_Inv_dbp{(qq,m,n) in BRANCHCC} = 
  (branch_Ror[qq,m,n]+sigma1[qq,m,n]) * V[m] * (branch_admi[qq,m,n]+sigma2[qq,m,n]) * sin(teta[n]-teta[m]-(branch_dephor[qq,m,n]+sigma3[qq,m,n])-(branch_angper[qq,m,n]+sigma4[qq,m,n]))
  + V[n] * ((branch_admi[qq,m,n]+sigma2[qq,m,n])*sin(branch_angper[qq,m,n]+sigma4[qq,m,n])+(branch_Gex[1,qq,m,n]+sigma7[qq,m,n]));

var Red_Tran_Rea_Inv_dbp{(qq,m,n) in BRANCHCC} =
  - (branch_Ror[qq,m,n]+sigma1[qq,m,n]) * V[m] * (branch_admi[qq,m,n]+sigma2[qq,m,n]) * cos(teta[n]-teta[m]-(branch_dephor[qq,m,n]+sigma3[qq,m,n])-(branch_angper[qq,m,n]+sigma4[qq,m,n]))
  + V[n] * ((branch_admi[qq,m,n]+sigma2[qq,m,n])*cos(branch_angper[qq,m,n]+sigma4[qq,m,n])-(branch_Bex[1,qq,m,n]+sigma8[qq,m,n]));


#
# Active Balance for all buses except slack bus
#

subject to ctr_balance_P_dbp{PROBLEM_DETECTION_PARAM,k in BUSCC diff {null_phase_bus}}:
  # Flows
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Dir_dbp[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Inv_dbp[qq,m,k]
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
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Dir_dbp[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Inv_dbp[qq,m,k]
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
  + sum{(qq,m,n) in BRANCHCC} b_sigma1[qq,m,n]
  + 20 * sum{(qq,m,n) in BRANCHCC} b_sigma2[qq,m,n] # We do not want to move admittance too easily.
  + sum{(qq,m,n) in BRANCHCC} b_sigma3[qq,m,n]
  + sum{(qq,m,n) in BRANCHCC} b_sigma4[qq,m,n]
  + sum{(qq,m,n) in BRANCHCC} b_sigma5[qq,m,n]
  + sum{(qq,m,n) in BRANCHCC} b_sigma6[qq,m,n]
  + sum{(qq,m,n) in BRANCHCC} b_sigma7[qq,m,n]
  + sum{(qq,m,n) in BRANCHCC} b_sigma8[qq,m,n]
  ;
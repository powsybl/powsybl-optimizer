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

set PROBLEM_NO_PENAL default { };

#####################################
#                                   #
#     Load flow var/constraints     #
#                                   #  
#####################################

subject to ctr_voltage_values_PQ_sup{PROBLEM_NO_PENAL, n in BUSCC_PQ diff BUSCC_3WT}: V[n] <= 1.25;
subject to ctr_voltage_values_PQ_inf{PROBLEM_NO_PENAL, n in BUSCC_PQ diff BUSCC_3WT}: V[n] >= 0.75;

subject to ctr_voltage_values_3wt_sup{PROBLEM_NO_PENAL, n in BUSCC_3WT}: V[n] <= 10;
subject to ctr_voltage_values_3wt_inf{PROBLEM_NO_PENAL, n in BUSCC_3WT}: V[n] >= 0;



#############################################
#     Slack bus and voltage regulation      #
#############################################

subject to ctr_null_phase_bus_no_penal{PROBLEM_NO_PENAL}: teta[null_phase_bus] = 0;
# TODO : CHange here, i put bus_V0 but it should be targetV !
#subject to ctr_voltage_PV_buses_no_penal{PROBLEM_NO_PENAL, k in BUSCC_PV}: V[k] - targetV_busPV[k] = 0;
subject to ctr_voltage_PV_buses_no_penal{PROBLEM_NO_PENAL, k in BUSCC_PV}: V[k] - bus_V0[1,k] = 0;

############################################################
#     Active and reactive powers variables/constraints     #  
############################################################

#
# Active Balance for all buses except slack bus
#

subject to ctr_balance_P_no_penal{PROBLEM_NO_PENAL,k in BUSCC diff {null_phase_bus}}:
  # Flows on branches
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Dir_no_penal[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Inv_no_penal[qq,m,k]
  # Flows on branches with one side opened
  + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED} base100MVA * V[k] * Act_branch_bus_2_opened_no_penal[qq,k,n]
  + sum{(qq,m,k) in BRANCH_WITH_SIDE_1_OPENED} base100MVA * V[k] * Act_branch_bus_1_opened_no_penal[qq,m,k]
  # Generating units
  - sum{(g,k) in UNITCC} unit_Pc[1,g,k] # Fixed value
  # Loads
  + sum{(c,k) in LOADCC} load_PFix[1,c,k]     # Fixed value
  # VSC converters
  - sum{(v,k) in VSCCONVON} vscconv_P0[1,v,k] # Fixed value
  # LCC converters
  + sum{(l,k) in LCCCONVON} lccconv_P0[1,l,k] # Fixed values
  = 0;

#
# Reactive Balance for PQ buses
#

subject to ctr_balance_Q_no_penal{PROBLEM_NO_PENAL,k in BUSCC_PQ}: 
  # Flows
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Dir_no_penal[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Inv_no_penal[qq,m,k]
  # Flows on branches with one side opened
  + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED} base100MVA * V[k] * Rea_branch_bus_2_opened_no_penal[qq,k,n]
  + sum{(qq,m,k) in BRANCH_WITH_SIDE_1_OPENED} base100MVA * V[k] * Rea_branch_bus_1_opened_no_penal[qq,m,k]
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


###########################################
#                                         #
#             Objective function          #
#                                         #
###########################################


minimize problem_no_penal:
  0
  ;
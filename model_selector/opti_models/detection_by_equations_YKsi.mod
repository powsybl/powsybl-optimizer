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
#                 OPT PROBLEM FOR ERROR DETECTION BY EQUATIONS                #
#                                                                             #
###############################################################################

set PROBLEM_DETECTION_EQUATIONS default { };

#############################################
#             Model Constraints             #
#############################################

subject to ctr_s1_min_dbe{PROBLEM_DETECTION_EQUATIONS, n in BUSCC_PV}: s1[n] + targetV_busPV[n] >= 0.2;
subject to ctr_s1_max_dbe{PROBLEM_DETECTION_EQUATIONS, n in BUSCC_PV}: s1[n] + targetV_busPV[n] <= 1.8;

param abs_borne_s2{k in BUSCC diff {null_phase_bus}} =
  sum{(qq,k,n) in BRANCHCC} base100MVA * 1.2 * 1.2 * branch_Ror[qq,k,n] * (branch_admi[qq,k,n] + branch_Ror[qq,k,n] * (branch_admi[qq,k,n] + abs(branch_Gor_corrected[1,qq,k,n])))
  + sum{(qq,m,k) in BRANCHCC} + base100MVA * 1.2 * 1.2 * (branch_Ror[qq,m,k] * branch_admi[qq,m,k] + branch_admi[qq,m,k] + abs(branch_Gex_corrected[1,qq,m,k]))

+ abs(- sum{(g,k) in UNITCC} unit_Pc[1,g,k]
  + sum{(c,k) in LOADCC} load_PFix[1,c,k]
  + sum{(v,k) in VSCCONVON} vscconv_P0[1,v,k]
  + sum{(l,k) in LCCCONVON} lccconv_P0[1,l,k]);

param abs_borne_s3{k in BUSCC_PQ} =

    sum{(qq,k,n) in BRANCHCC} abs(base100MVA * 1.2 * 1.2 * branch_Ror[qq,k,n] * (branch_admi[qq,k,n] + branch_Ror[qq,k,n] * (branch_admi[qq,k,n] + abs(branch_Bor_corrected[1,qq,k,n]))))
  + sum{(qq,m,k) in BRANCHCC} abs(base100MVA * 1.2 * 1.2 * (branch_Ror[qq,m,k] * branch_admi[qq,m,k] + branch_admi[qq,m,k] + abs(branch_Bex_corrected[1,qq,m,k])))

  + abs(- sum{(g,k) in UNITCC} unit_Qc[1,g,k]
  + sum{(c,k) in LOADCC} load_QFix[1,c,k]
  - sum{(shunt,k) in SHUNTCC} base100MVA * shunt_valnom[1,shunt,k] * 1
  - sum{(svc,k) in SVCCC_PQ_1} svc_Q0[1,svc,k])

  + abs(sum{(svc,k) in SVCCC_PQ_2} if bus_V0[1,k] > svc_targetV[1,svc,k] 
                              then base100MVA * svc_bmin[1,svc,k] * 1.2^2
                              else base100MVA * svc_bmax[1,svc,k] * 1.2^2)
                              
  + abs(- sum{(v,k) in VSCCONVON} vscconv_Q0[1,v,k]
  + sum{(l,k) in LCCCONVON} lccconv_Q0[1,l,k]);

# Activation of b_s if s var is > 0
subject to ctr_b_s1_pos_dbe{PROBLEM_DETECTION_EQUATIONS, k in BUSCC_PV}: s1[k] <= (1.8 - targetV_busPV[k]) * b_s1[k];
subject to ctr_b_s2_pos_dbe{PROBLEM_DETECTION_EQUATIONS, k in BUSCC diff {null_phase_bus}}: s2[k] <= abs_borne_s2[k] * b_s2[k];
subject to ctr_b_s3_pos_dbe{PROBLEM_DETECTION_EQUATIONS, k in BUSCC_PQ}: s3[k] <= abs_borne_s3[k] * b_s3[k];

# Activation of b_s if s var is < 0
subject to ctr_b_s1_neg_dbe{PROBLEM_DETECTION_EQUATIONS, k in BUSCC_PV}: s1[k] >= (0.2 - 1 - targetV_busPV[k]) * b_s1[k];
subject to ctr_b_s2_neg_dbe{PROBLEM_DETECTION_EQUATIONS, k in BUSCC diff {null_phase_bus}}: s2[k] >= - abs_borne_s2[k] * b_s2[k];
subject to ctr_b_s3_neg_dbe{PROBLEM_DETECTION_EQUATIONS, k in BUSCC_PQ}: s3[k] >= - abs_borne_s3[k] * b_s3[k];

#############################################
#             LoadFlow Constraints          #
#############################################

# Consistency of voltage values for PQ-bus
subject to ctr_voltage_values_min_dbe{PROBLEM_DETECTION_EQUATIONS, n in BUSCC_PQ}: V[n] <= 1.25;
subject to ctr_voltage_values_max_dbe{PROBLEM_DETECTION_EQUATIONS, n in BUSCC_PQ}: V[n] >= 0.75;


subject to ctr_null_phase_bus_dbe{PROBLEM_DETECTION_EQUATIONS}: teta[null_phase_bus] = 0; # Slack bus

subject to ctr_voltage_PV_buses_dbe{PROBLEM_DETECTION_EQUATIONS, k in BUSCC_PV}: V[k] - targetV_busPV[k] + s1[k] = 0; # PV node voltage regulation

subject to ctr_balance_P_dbe{PROBLEM_DETECTION_EQUATIONS, k in BUSCC diff {null_phase_bus}}:
  # Flows
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Dir_YKsi[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Inv_YKsi[qq,m,k]
  # Generating units
  - sum{(g,k) in UNITCC} unit_Pc[1,g,k] # Fixed value
  # Loads
  + sum{(c,k) in LOADCC} load_PFix[1,c,k]     # Fixed value
  # VSC converters
  + sum{(v,k) in VSCCONVON} vscconv_P0[1,v,k] # Fixed value
  # LCC converters
  + sum{(l,k) in LCCCONVON} lccconv_P0[1,l,k] # Fixed value
  - s2[k]
  = 0;

subject to ctr_balance_Q_dbe{PROBLEM_DETECTION_EQUATIONS, k in BUSCC_PQ}: 
  # Flows
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Dir_YKsi[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Inv_YKsi[qq,m,k]
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
  - s3[k]
  = 0;


###########################################
#       Objective function ACDA_LOC       #
###########################################

minimize problem_dbe:
  0
  + sum{n in BUSCC_PV} b_s1[n]
  + sum{n in BUSCC diff {null_phase_bus}} b_s2[n]
  + sum{n in BUSCC_PQ} b_s3[n]
  ;
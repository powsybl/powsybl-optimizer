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

###########################################################
#                                                         #
#     All the variables of the optimization problems      #
#                                                         #
###########################################################

# Magnitude and Angle for the voltage of each bus
var V{n in BUSCC};
var teta{n in BUSCC};


# Slack/Binary variables for data penalization ...

# ... of target V
var s1{n in BUSCC_PV}; 
var b_s1{n in BUSCC_PV} binary;
# ... of rho
var sigma1{(qq,m,n) in BRANCHCC_TRANSFORMER}; 
var b_sigma1{(qq,m,n) in BRANCHCC_TRANSFORMER} binary;
# ... of Y
var sigma2{(qq,m,n) in BRANCHCC_PENALIZED}; 
var b_sigma2{(qq,m,n) in BRANCHCC_PENALIZED} binary;
# ... of alpha
var sigma3{(qq,m,n) in BRANCHCC_DEPH}; 
var b_sigma3{(qq,m,n) in BRANCHCC_DEPH} binary;
# ... of Xi
var sigma4{(qq,m,n) in BRANCHCC_PENALIZED}; 
var b_sigma4{(qq,m,n) in BRANCHCC_PENALIZED} binary;
# ... of G1
var sigma5{(qq,m,n) in BRANCHCC_PENALIZED}; 
var b_sigma5{(qq,m,n) in BRANCHCC_PENALIZED} binary;
# ... of B1
var sigma6{(qq,m,n) in BRANCHCC_PENALIZED};
var b_sigma6{(qq,m,n) in BRANCHCC_PENALIZED} binary;
# ... of G2
var sigma7{(qq,m,n) in BRANCHCC_PENALIZED};
var b_sigma7{(qq,m,n) in BRANCHCC_PENALIZED} binary;

# ... of B2
var sigma8{(qq,m,n) in BRANCHCC_PENALIZED};
var b_sigma8{(qq,m,n) in BRANCHCC_PENALIZED} binary;


# Variables to simplify active/reactive power formulations
var target_v_penal{n in BUSCC_PV}               = if (n in BUSCC_PV_UNITS and is_target_v_units == 1) or (n in BUSCC_PV_SVC and is_target_v_svc == 1)
                                                  then targetV_busPV[n] + s1[n]
                                                  else targetV_busPV[n];
var rho_penal{(qq,m,n) in BRANCHCC_PENALIZED}   = if is_rho_control == 1 and (qq,m,n) in BRANCHCC_TRANSFORMER 
                                                  then branch_Ror[qq,m,n] + sigma1[qq,m,n] 
                                                  else branch_Ror[qq,m,n];
var y_penal{(qq,m,n) in BRANCHCC_PENALIZED}     = if is_admittance_control == 1 
                                                  then branch_admi[qq,m,n] + sigma2[qq,m,n]
                                                  else branch_admi[qq,m,n];
var alpha_penal{(qq,m,n) in BRANCHCC_PENALIZED} = if is_phase_shif_control == 1 and (qq,m,n) in BRANCHCC_DEPH 
                                                  then branch_dephor[qq,m,n] + sigma3[qq,m,n] 
                                                  else branch_dephor[qq,m,n];
var Xi_penal{(qq,m,n) in BRANCHCC_PENALIZED}    = if is_xi_control == 1 then branch_angper[qq,m,n] + sigma4[qq,m,n]
                                                  else branch_angper[qq,m,n];
var G1_penal{(qq,m,n) in BRANCHCC_PENALIZED}    = if is_g_shunt_1_control == 1 
                                                  then branch_Gor[1,qq,m,n] + sigma5[qq,m,n]
                                                  else branch_Gor[1,qq,m,n];
var B1_penal{(qq,m,n) in BRANCHCC_PENALIZED}    = if is_b_shunt_1_control == 1
                                                  then branch_Bor[1,qq,m,n] + sigma6[qq,m,n]
                                                  else branch_Bor[1,qq,m,n];
var G2_penal{(qq,m,n) in BRANCHCC_PENALIZED}    = if is_g_shunt_2_control == 1 and (qq,m,n) not in BRANCHCC_TRANSFORMER
                                                  then branch_Gex[1,qq,m,n] + sigma7[qq,m,n]
                                                  else branch_Gex[1,qq,m,n];
var B2_penal{(qq,m,n) in BRANCHCC_PENALIZED}    = if is_b_shunt_2_control == 1 and (qq,m,n) not in BRANCHCC_TRANSFORMER
                                                  then branch_Bex[1,qq,m,n] + sigma8[qq,m,n]
                                                  else branch_Bex[1,qq,m,n];


# Penalized active/reactive power going from m to n
var act_power_dir_YXi_penal{(qq,m,n) in BRANCHCC} =
  rho_penal[qq,m,n] * V[n] * y_penal
[qq,m,n] * sin(teta[m] - teta[n] + alpha_penal[qq,m,n] - Xi_penal[qq,m,n])
  + (rho_penal[qq,m,n])**2 * V[m] * (y_penal
[qq,m,n] * sin(Xi_penal[qq,m,n]) + G1_penal[qq,m,n]);

var rea_power_dir_YXi_penal{(qq,m,n) in BRANCHCC} = 
  - rho_penal[qq,m,n] * V[n] * y_penal
[qq,m,n] * cos(teta[m] - teta[n] + alpha_penal[qq,m,n] - Xi_penal[qq,m,n])
  + (rho_penal[qq,m,n])**2 * V[m] * (y_penal
[qq,m,n] * cos(Xi_penal[qq,m,n]) - B1_penal[qq,m,n]);

var act_power_inv_YXi_penal{(qq,m,n) in BRANCHCC} = 
  rho_penal[qq,m,n] * V[m] * y_penal
[qq,m,n] * sin(teta[n] - teta[m] - alpha_penal[qq,m,n] - Xi_penal[qq,m,n])
  + V[n] * (y_penal
[qq,m,n] * sin(Xi_penal[qq,m,n]) + G2_penal[qq,m,n]);

var rea_power_inv_YXi_penal{(qq,m,n) in BRANCHCC} =
  - rho_penal[qq,m,n] * V[m] * y_penal
[qq,m,n] * cos(teta[n] - teta[m] - alpha_penal[qq,m,n] - Xi_penal[qq,m,n])
  + V[n] * (y_penal
[qq,m,n] * cos(Xi_penal[qq,m,n]) - B2_penal[qq,m,n]);

# Penalized active/reactive power on branches with one side opened
var act_power_bus2_opened_YXi_penal{(qq,m,n) in BRANCH_WITH_SIDE_2_OPENED} =
  (rho_penal[qq,m,n])**2 * V[m] * 
  (G1_penal[qq,m,n] + (y_penal[qq,m,n])**2 * G2_penal[qq,m,n] 
  / ( (G2_penal[qq,m,n] + y_penal
[qq,m,n] * sin(Xi_penal[qq,m,n]))**2 
  + (- B2_penal[qq,m,n] + y_penal
[qq,m,n] * cos(Xi_penal[qq,m,n]))**2 ) # Shunt
  + ((B2_penal[qq,m,n])**2 + (G2_penal[qq,m,n])**2) * y_penal
[qq,m,n] * sin(Xi_penal[qq,m,n])
  / ( (G2_penal[qq,m,n] + y_penal
[qq,m,n] * sin(Xi_penal[qq,m,n]))**2 
  + (- B2_penal[qq,m,n] + y_penal
[qq,m,n] * cos(Xi_penal[qq,m,n]))**2 ) # Shunt
  );

var rea_power_bus2_opened_YXi_penal{(qq,m,n) in BRANCH_WITH_SIDE_2_OPENED} = 
  - (rho_penal[qq,m,n])**2 * V[m] *
  (B1_penal[qq,m,n] + (y_penal
[qq,m,n])**2 * B2_penal[qq,m,n]
  / ( (G2_penal[qq,m,n] + y_penal
[qq,m,n] * sin(Xi_penal[qq,m,n]))**2 
  + (- B2_penal[qq,m,n] + y_penal
[qq,m,n] * cos(Xi_penal[qq,m,n]))**2 ) # Shunt
  - ((B2_penal[qq,m,n])**2 + (G2_penal[qq,m,n])**2) * y_penal
[qq,m,n] * cos(Xi_penal[qq,m,n])
  / ( (G2_penal[qq,m,n] + y_penal
[qq,m,n] * sin(Xi_penal[qq,m,n]))**2 
  + (-B2_penal[qq,m,n] + y_penal
[qq,m,n] * cos(Xi_penal[qq,m,n]))**2 ) # Shunt
  );

var act_power_bus1_opened_YXi_penal{(qq,m,n) in BRANCH_WITH_SIDE_1_OPENED} =
  V[n] * 
  (G2_penal[qq,m,n] 
  + (y_penal
[qq,m,n])**2 * G1_penal[qq,m,n] 
  / ( (G1_penal[qq,m,n] + y_penal
[qq,m,n] * sin(Xi_penal[qq,m,n]))**2 
  + (- B1_penal[qq,m,n] + y_penal
[qq,m,n] * cos(Xi_penal[qq,m,n]))**2 ) # Shunt
  + ((B1_penal[qq,m,n])**2 + (G1_penal[qq,m,n])**2) * y_penal
[qq,m,n] * sin(Xi_penal[qq,m,n])
  / ( (G1_penal[qq,m,n] + y_penal
[qq,m,n] * sin(Xi_penal[qq,m,n]))**2 
  + (- B1_penal[qq,m,n] + y_penal
[qq,m,n] * cos(Xi_penal[qq,m,n]))**2 ) # Shunt
  );

var rea_power_bus1_opened_YXi_penal{(qq,m,n) in BRANCH_WITH_SIDE_1_OPENED} = 
  - V[n] *
  (B2_penal[qq,m,n] 
  + (y_penal
[qq,m,n])**2 * B1_penal[qq,m,n]
  / ( (G1_penal[qq,m,n] + y_penal
[qq,m,n] * sin(Xi_penal[qq,m,n]))**2 
  + (- B1_penal[qq,m,n] + y_penal
[qq,m,n] * cos(Xi_penal[qq,m,n]))**2 ) # Shunt
  - ((B1_penal[qq,m,n])**2 + (G1_penal[qq,m,n])**2) * y_penal
[qq,m,n] * cos(Xi_penal[qq,m,n])
  / ( (G1_penal[qq,m,n] + y_penal
[qq,m,n] * sin(Xi_penal[qq,m,n]))**2 
  + (- B1_penal[qq,m,n] + y_penal
[qq,m,n] * cos(Xi_penal[qq,m,n]))**2 ) # Shunt
  );
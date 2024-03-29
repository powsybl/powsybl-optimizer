###############################################################################
#
# Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services 
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
###############################################################################

###############################################################################
# State Estimator
# Author : Jean Maeght 2022 2023
# Author : Pierre Arvy 2023
# Author : Lucas Riou 2024
###############################################################################

###########################################################
#                                                         #
#     All the variables of the SE optimization problem    #
#                                                         #
###########################################################

# Magnitude and Angle for the voltage of each bus
var V{n in BUSCC};
var theta{n in BUSCC};

# Status (opened/closed) of each branch
var y{(qq,m,n) in BRANCHCC_FULL} binary;

# Residuals : r = z - h(x)
var resid_Pf{l in MEASURECC_Pf};
var resid_Qf{l in MEASURECC_Qf};
var resid_P{l in MEASURECC_P};
var resid_Q{l in MEASURECC_Q};
var resid_V{l in MEASURECC_V};

# Intermediate variables for active/reactive power flows going from bus m to bus n (expressions lack multiplication by V)
# Each time, two formulations (dir/inv), as line (m,n) is only imported once (same line but indexed as (n,m) does not exist)

# Full lines
var act_power_dir{(qq,m,n) in BRANCHCC} =
  branch_Ror[qq,m,n] * branch_Rex[qq,m,n] * V[n] * branch_admi[qq,m,n] 
  * sin(theta[m] - theta[n] + branch_dephor[qq,m,n] - branch_angper[qq,m,n])
  + (branch_Ror[qq,m,n])**2 * V[m] 
  * (branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]) + branch_Gor[1,qq,m,n]);

var act_power_inv{(qq,m,n) in BRANCHCC} =
  branch_Ror[qq,m,n] * branch_Rex[qq,m,n] * V[m] * branch_admi[qq,m,n] 
  * sin(theta[n] - theta[m] - branch_dephor[qq,m,n] - branch_angper[qq,m,n])
  + (branch_Rex[qq,m,n])**2 * V[n] 
  * (branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]) + branch_Gex[1,qq,m,n]);

var rea_power_dir{(qq,m,n) in BRANCHCC} = 
  - branch_Ror[qq,m,n] * branch_Rex[qq,m,n] * V[n] * branch_admi[qq,m,n] 
  * cos(theta[m] - theta[n] + branch_dephor[qq,m,n] - branch_angper[qq,m,n])
  + (branch_Ror[qq,m,n])**2 * V[m] 
  * (branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]) - branch_Bor[1,qq,m,n]);

var rea_power_inv{(qq,m,n) in BRANCHCC} =
  - branch_Ror[qq,m,n] * branch_Rex[qq,m,n] * V[m] * branch_admi[qq,m,n] 
  * cos(theta[n] - theta[m] - branch_dephor[qq,m,n] - branch_angper[qq,m,n])
  + (branch_Rex[qq,m,n])**2 * V[n] 
  * (branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]) - branch_Bex[1,qq,m,n]);

# Lines with one side opened
var act_power_bus1_opened{(qq,m,n) in BRANCH_WITH_SIDE_1_OPENED} =
  V[n] 
  * (
    branch_Gex[1,qq,m,n] 
    + (branch_admi[qq,m,n])**2 
    * branch_Gor[1,qq,m,n] 
    / (
      (branch_Gor[1,qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2 
      + (- branch_Bor[1,qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2 
    ) # Shunt
    + (
      (branch_Bor[1,qq,m,n])**2 
      + (branch_Gor[1,qq,m,n])**2
    ) 
    * branch_admi[qq,m,n] * sin(branch_angper[qq,m,n])
    / (
      (branch_Gor[1,qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2 
      + (- branch_Bor[1,qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2 
    ) # Shunt
  );

var act_power_bus2_opened{(qq,m,n) in BRANCH_WITH_SIDE_2_OPENED} =
  (branch_Ror[qq,m,n])**2 * V[m] 
  * (
    branch_Gor[1,qq,m,n] 
    + (branch_admi[qq,m,n])**2 * branch_Gex[1,qq,m,n] 
    / (
      (branch_Gex[1,qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2 
      + (- branch_Bex[1,qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2 
    ) # Shunt
    + (
      (branch_Bex[1,qq,m,n])**2 
      + (branch_Gex[1,qq,m,n])**2
    ) * branch_admi[qq,m,n] * sin(branch_angper[qq,m,n])
    / (
      (branch_Gex[1,qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2 
      + (- branch_Bex[1,qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2 
    ) # Shunt
  );

var rea_power_bus1_opened{(qq,m,n) in BRANCH_WITH_SIDE_1_OPENED} = 
  - V[n] 
  * (
    branch_Bex[1,qq,m,n] 
    + (branch_admi[qq,m,n])**2 * branch_Bor[1,qq,m,n]
    / (
      (branch_Gor[1,qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2 
      + (- branch_Bor[1,qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2
    ) # Shunt
    - (
      (branch_Bor[1,qq,m,n])**2 
      + (branch_Gor[1,qq,m,n])**2
    ) 
    * branch_admi[qq,m,n] * cos(branch_angper[qq,m,n])
    / (
      (branch_Gor[1,qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2 
      + (- branch_Bor[1,qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2
    ) # Shunt
  );

var rea_power_bus2_opened{(qq,m,n) in BRANCH_WITH_SIDE_2_OPENED} = 
  - (branch_Ror[qq,m,n])**2 * V[m] 
  * (
    branch_Bor[1,qq,m,n] 
    + (branch_admi[qq,m,n])**2 * branch_Bex[1,qq,m,n]
    / (
      (branch_Gex[1,qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2 
      + (- branch_Bex[1,qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2 
    ) # Shunt
    - (
      (branch_Bex[1,qq,m,n])**2 + (branch_Gex[1,qq,m,n])**2
    ) 
    * branch_admi[qq,m,n] * cos(branch_angper[qq,m,n])
    / (
      (branch_Gex[1,qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2 
      + (-branch_Bex[1,qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2
    ) # Shunt
  );

# END OF MY VERSION

/* # Constants for normalization of residuals (in SI)
param max_Pf_value := if card(MEASURECC_Pf) > 0 then max({l in MEASURECC_Pf} Pf_value[l]) else 1; # MW
param max_Qf_value := if card(MEASURECC_Qf) > 0 then max({l in MEASURECC_Qf} Qf_value[l]) else 1; # MVar
param max_P_value := if card(MEASURECC_P) > 0 then max({l in MEASURECC_P} P_value[l]) else 1; # MW
param max_Q_value := if card(MEASURECC_Q) > 0 then max({l in MEASURECC_Q} Q_value[l]) else 1; # MVar
param max_V_value := if card(MEASURECC_V) > 0 
                     then max({l in MEASURECC_V} V_value[l] * substation_Vnomi[1,bus_substation[1,V_bus[l]]]) 
                     else 1; # kV */
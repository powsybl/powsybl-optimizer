###############################################################################
#
# Copyright (c) 2022 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
###############################################################################

###############################################################################
# Reactive OPF
# Author:  Jean Maeght 2022 2023
###############################################################################

# Definition of optimization problem
set PROBLEM_DCOPF default { };


###############################################################################
# Voltage variables and constraints 
###############################################################################

# Phase of voltage
var teta_dc{n in BUSCC} 
  <= teta_max, 
  >= teta_min;

subject to ctr_null_phase_bus_dc{PROBLEM_DCOPF}: teta_dc[null_phase_bus] = 0;


###############################################################################
# Active power variables and constraints 
###############################################################################

# Variable flow is the flow from bus 1 to bus 2
var activeflow{BRANCHCC};
subject to ctr_activeflow{PROBLEM_DCOPF, (qq,m,n) in BRANCHCC}:
  activeflow[qq,m,n] = base100MVA * (teta_dc[m]-teta_dc[n]) / branch_X_mod[qq,m,n];#* branch_X_mod[qq,m,n] / (branch_X_mod[qq,m,n]**2+branch_R[1,qq,m,n]**2);

# Generation for DCOPF
var P_dcopf{(g,n) in UNITON}; # >= unit_Pmin[1,g,n], <= unit_Pmax[1,g,n];

# Slack variable for each bus
# >=0 if too much generation in bus, <=0 if missing generation
var balance_pos{BUSCC} >= 0;
var balance_neg{BUSCC} >= 0;

# Active power balance at each bus
subject to ctr_balance{PROBLEM_DCOPF, n in BUSCC}:
  - sum{(g,n) in UNITON} P_dcopf[g,n]
  - sum{(b,n) in BATTERYCC} battery_p0[1,b,n]
  + sum{(c,n) in LOADCC} load_PFix[1,c,n]
  + sum{(qq,n,m) in BRANCHCC} activeflow[qq,n,m] # active power flow outgoing on branch qq at bus n
  - sum{(qq,m,n) in BRANCHCC} activeflow[qq,m,n] # active power flow entering in bus n on branch qq
  + sum{(vscconv,n) in VSCCONVON} vscconv_P0[1,vscconv,n]
  + sum{(l,n) in LCCCONVON} lccconv_P0[1,l,n]
  =
  balance_pos[n] - balance_neg[n];


###############################################################################
# Objective function and penalties
###############################################################################

param penalty_gen     := 1;
param penalty_balance := 1000;

minimize problem_dcopf_objective:
    penalty_gen     * sum{(g,n) in UNITON} ((P_dcopf[g,n]-unit_Pc[1,g,n])/max(0.01*abs(unit_Pc[1,g,n]),1))**2
  + penalty_balance * sum{n in BUSCC} ( balance_pos[n] + balance_neg[n] )
  ;

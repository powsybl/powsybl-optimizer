###############################################################################
#
# Copyright (c) 2022 2023 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
###############################################################################

###############################################################################
# Reactive OPF
# Author:  Jean Maeght 2022 2023
# Author:  Manuel Ruiz 2023 2024
###############################################################################


set PROBLEM_ACOPF default { };

###############################################################################
#
# Variables and contraints for ACOPF
#
###############################################################################
# Notice that some variables and constraints for DCOPF are also used for ACOPF

#
# Phase and modulus of voltage
#
# Complex voltage = V*exp(i*teta). (with i**2=-1)

# Phase of voltage
var teta{BUSCC} <= teta_max, >= teta_min;
subject to ctr_null_phase_bus{PROBLEM_ACOPF}: teta[null_phase_bus] = 0;

# Modulus of voltage
var V{n in BUSCC}
  <=
  if substation_Vnomi[1,bus_substation[1,n]] <= ignore_voltage_bounds then max_plausible_high_voltage_limit else
  voltage_upper_bound[1,bus_substation[1,n]],
  >=
  if substation_Vnomi[1,bus_substation[1,n]] <= ignore_voltage_bounds then min_plausible_low_voltage_limit else
  voltage_lower_bound[1,bus_substation[1,n]];


#
# Generation
#
# General idea: generation is an input data, but as voltage may vary, generation may vary a little.
# Variations of generation is totally controlled by unique scalar variable alpha
# Before and after optimization, there is no waranty that P is within
# its "bounds" [corrected_unit_Pmin;corrected_unit_Pmax]
#

# Active generation
var alpha <=1, >=-1; # If alpha==1 then all units are at Pmax
var P_bounded{(g,n) in UNITON} <= max(unit_Pc[1,g,n],corrected_unit_Pmax[g,n]), >= min(unit_Pc[1,g,n],corrected_unit_Pmin[g,n]);
# If coeff_alpha == 1 then all P are defined by the single variable alpha
# If coeff_alpha == 0 then all P are free within their respective bounds
# todo faire des tests avec les valeurs de coeff_alpha
var P{(g,n) in UNITON} =
  if   ( unit_Pc[1,g,n] < (corrected_unit_Pmax[g,n] - Pnull) and unit_Pc[1,g,n] > Pnull )
  then (     coeff_alpha   * ( unit_Pc[1,g,n] + alpha*(corrected_unit_Pmax[g,n]- unit_Pc[1,g,n]) )
         + (1-coeff_alpha) *   P_bounded[g,n] )
  else unit_Pc[1,g,n] ;


#
# Reactive generation
# 
# todo: add trapeze or hexagone constraints for reactive power
var Q{(g,n) in UNITON} <= corrected_unit_Qmax[g,n], >= corrected_unit_Qmin[g,n];


#
# Variable shunts
#
var shunt_var{(shunt,n) in SHUNT_VAR}
  >= min{(1,shunt,k) in SHUNT} shunt_valmin[1,shunt,k],
  <= max{(1,shunt,k) in SHUNT} shunt_valmax[1,shunt,k];


#
# SVC reactive generation
#
var svc_qvar{(svc,n) in SVCON} >= svc_bmin[1,svc,n], <= svc_bmax[1,svc,n];


#
# VSCCONV reactive generation
#
var vscconv_qvar{(v,n) in VSCCONVON}
  >= min(vscconv_qP[1,v,n],vscconv_qp0[1,v,n],vscconv_qp[1,v,n]),
  <= max(vscconv_QP[1,v,n],vscconv_Qp0[1,v,n],vscconv_Qp[1,v,n]);
# todo: add trapeze or hexagone constraints for reactive power


#
# Ratios of transformers
#
var branch_Ror_var{(qq,m,n) in BRANCHCC_REGL_VAR}
  >= regl_ratio_min[1,branch_ptrRegl[1,qq,m,n]],
  <= regl_ratio_max[1,branch_ptrRegl[1,qq,m,n]];

#
# Flows
#

var Red_Tran_Act_Dir{(qq,m,n) in BRANCHCC } =
    V[n] * branch_admi[qq,m,n] * sin(teta[m]-teta[n]+branch_dephor[qq,m,n]-branch_angper[qq,m,n])
    * (if (qq,m,n) in BRANCHCC_REGL_VAR then branch_Ror_var[qq,m,n]*branch_cstratio[1,qq,m,n] else branch_Ror[qq,m,n])
  + V[m] * (branch_admi[qq,m,n]*sin(branch_angper[qq,m,n])+branch_Gor_mod[qq,m,n])
    * (if (qq,m,n) in BRANCHCC_REGL_VAR then branch_Ror_var[qq,m,n]*branch_cstratio[1,qq,m,n] else branch_Ror[qq,m,n])**2
  ;

var Red_Tran_Rea_Dir{(qq,m,n) in BRANCHCC } =
  - V[n] * branch_admi[qq,m,n] * cos(teta[m]-teta[n]+branch_dephor[qq,m,n]-branch_angper[qq,m,n])
    * (if (qq,m,n) in BRANCHCC_REGL_VAR then branch_Ror_var[qq,m,n]*branch_cstratio[1,qq,m,n] else branch_Ror[qq,m,n])
  + V[m] * (branch_admi[qq,m,n]*cos(branch_angper[qq,m,n])-branch_Bor_mod[qq,m,n])
    * (if (qq,m,n) in BRANCHCC_REGL_VAR then branch_Ror_var[qq,m,n]*branch_cstratio[1,qq,m,n] else branch_Ror[qq,m,n])^2
  ;

var Red_Tran_Act_Inv{(qq,m,n) in BRANCHCC } =
    V[m] * branch_admi[qq,m,n] * sin(teta[n]-teta[m]-branch_dephor[qq,m,n]-branch_angper[qq,m,n])
    * (if (qq,m,n) in BRANCHCC_REGL_VAR then branch_Ror_var[qq,m,n]*branch_cstratio[1,qq,m,n] else branch_Ror[qq,m,n])
  + V[n] * (branch_admi[qq,m,n]*sin(branch_angper[qq,m,n])+branch_Gex_mod[qq,m,n])
  ;

var Red_Tran_Rea_Inv{(qq,m,n) in BRANCHCC } =
  - V[m] * branch_admi[qq,m,n] * cos(teta[n]-teta[m]-branch_dephor[qq,m,n]-branch_angper[qq,m,n])
    * (if (qq,m,n) in BRANCHCC_REGL_VAR then branch_Ror_var[qq,m,n]*branch_cstratio[1,qq,m,n] else branch_Ror[qq,m,n])
  + V[n] * (branch_admi[qq,m,n]*cos(branch_angper[qq,m,n])-branch_Bex_mod[qq,m,n])
  ;

var Red_Tran_Act_Dir_Side_2_Opened{(qq,m,n) in BRANCHCC_WITH_SIDE_2_OPENED} =
    (branch_Ror[qq,m,n])**2 * V[m] * (branch_Gor_mod[qq,m,n] + (branch_admi[qq,m,n])**2 * branch_Gex_mod[qq,m,n] / ( (branch_Gex_mod[qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2
    + (- branch_Bex_mod[qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2 ) 
    + ((branch_Bex_mod[qq,m,n])**2 + (branch_Gex_mod[qq,m,n])**2) * branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]) / ( (branch_Gex_mod[qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2
    + (- branch_Bex_mod[qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2 ))
  ;

var Red_Tran_Rea_Dir_Side_2_Opened{(qq,m,n) in BRANCHCC_WITH_SIDE_2_OPENED} =
    - (branch_Ror[qq,m,n])**2 * V[m] * (branch_Bor_mod[qq,m,n] + (branch_admi[qq,m,n])**2 * branch_Bex_mod[qq,m,n] / ( (branch_Gex_mod[qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2
    + (- branch_Bex_mod[qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2 )
    - ((branch_Bex_mod[qq,m,n])**2 + (branch_Gex_mod[qq,m,n])**2) * branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]) / ( (branch_Gex_mod[qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2
    + (-branch_Bex_mod[qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2 ))
  ;

var Red_Tran_Act_Inv_Side_1_Opened{(qq,m,n) in BRANCHCC_WITH_SIDE_1_OPENED} =
    V[n] * (branch_Gex_mod[qq,m,n] + (branch_admi[qq,m,n])**2 * branch_Gor_mod[qq,m,n] / ( (branch_Gor_mod[qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2
    + (- branch_Bor_mod[qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2 )
    + ((branch_Bor_mod[qq,m,n])**2 + (branch_Gor_mod[qq,m,n])**2) * branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]) / ( (branch_Gor_mod[qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2
    + (- branch_Bor_mod[qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2 ))
  ;

var Red_Tran_Rea_Inv_Side_1_Opened{(qq,m,n) in BRANCHCC_WITH_SIDE_1_OPENED} =
    - V[n] * (branch_Bex_mod[qq,m,n]
    + (branch_admi[qq,m,n])**2 * branch_Bor_mod[qq,m,n] / ( (branch_Gor_mod[qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2
    + (- branch_Bor_mod[qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2 )
    - ((branch_Bor_mod[qq,m,n])**2 + (branch_Gor_mod[qq,m,n])**2) * branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]) / ( (branch_Gor_mod[qq,m,n] + branch_admi[qq,m,n] * sin(branch_angper[qq,m,n]))**2
    + (- branch_Bor_mod[qq,m,n] + branch_admi[qq,m,n] * cos(branch_angper[qq,m,n]))**2 ))
  ;


#
# Active Balance
#

subject to ctr_balance_P{PROBLEM_ACOPF,k in BUSCC}:
# Flows
sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Dir[qq,k,n]
+ sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Inv[qq,m,k]
# Flows on branches with one side opened
+ sum{(qq,k,n) in BRANCHCC_WITH_SIDE_2_OPENED} base100MVA * V[k] * Red_Tran_Act_Dir_Side_2_Opened[qq,k,n]
+ sum{(qq,m,k) in BRANCHCC_WITH_SIDE_1_OPENED} base100MVA * V[k] * Red_Tran_Act_Inv_Side_1_Opened[qq,m,k]
# Generating units
- sum{(g,k) in UNITON} P[g,k]
# Batteries
- sum{(b,k) in BATTERYCC} battery_p0[1,b,k]
# Loads
+ sum{(c,k) in LOADCC} load_PFix[1,c,k]     # Fixed value
# VSC converters
+ sum{(v,k) in VSCCONVON} vscconv_P0[1,v,k] # Fixed value
# LCC converters
+ sum{(l,k) in LCCCONVON} lccconv_P0[1,l,k] # Fixed value
= 0; # No slack variables for active power. If data are really too bad, may not converge.


#
# Reactive Balance
#

# Reactive balance slack variables at configured nodes
set BUSCC_SLACK := if buses_with_reactive_slacks == "ALL" then BUSCC
                    else if buses_with_reactive_slacks == "NO_GENERATION" then {n in BUSCC: (card{(g,n) in UNITON: (g,n) not in UNIT_FIXQ}==0 and card{(svc,n) in SVCON}==0 and card{(vscconv,n) in VSCCONVON}==0)}
                    else BUSCC inter PARAM_BUSES_WITH_REACTIVE_SLACK; # if = "CONFIGURED", buses given as parameter but in connex component
var slack1_shunt_B{BUSCC_SLACK} >= 0;
var slack2_shunt_B{BUSCC_SLACK} >= 0;
#subject to ctr_compl_slack_Q{PROBLEM_ACOPF,k in BUSCC_SLACK}: slack1_balance_Q[k] >= 0 complements slack2_balance_Q[k] >= 0;

subject to ctr_balance_Q{PROBLEM_ACOPF,k in BUSCC}:
# Flows
sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Dir[qq,k,n]
+ sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Inv[qq,m,k]
# Flows on branches with one side opened
+ sum{(qq,k,n) in BRANCHCC_WITH_SIDE_2_OPENED} base100MVA * V[k] * Red_Tran_Rea_Dir_Side_2_Opened[qq,k,n]
+ sum{(qq,m,k) in BRANCHCC_WITH_SIDE_1_OPENED} base100MVA * V[k] * Red_Tran_Rea_Inv_Side_1_Opened[qq,m,k]
# Generating units
- sum{(g,k) in UNITON: (g,k) not in UNIT_FIXQ } Q[g,k]
- sum{(g,k) in UNIT_FIXQ} unit_Qc[1,g,k]
# Batteries
- sum{(b,k) in BATTERYCC} battery_q0[1,b,k]
# Loads
+ sum{(c,k) in LOADCC} load_QFix[1,c,k]
# Shunts
- sum{(shunt,k) in SHUNT_FIX} base100MVA * shunt_valnom[1,shunt,k] * V[k]^2
- sum{(shunt,k) in SHUNT_VAR} base100MVA * shunt_var[shunt,k] * V[k]^2
# SVC
- sum{(svc,k) in SVCON} base100MVA * svc_qvar[svc,k] * V[k]^2
# VSC converters
- sum{(v,k) in VSCCONVON} vscconv_qvar[v,k]
# LCC converters
+ sum{(l,k) in LCCCONVON} lccconv_Q0[1,l,k] # Fixed value
# Slack variables
+ if k in BUSCC_SLACK then
(- base100MVA * V[k]^2 * slack1_shunt_B[k]  # Homogeneous to a generation of reactive power (condensator)
+ base100MVA * V[k]^2 * slack2_shunt_B[k]) # homogeneous to a reactive load (self)
= 0;


#
# Definitions for objective function
#

# Voltage target : ratio between Vmin and Vmax
var target_voltage_ratio = sum{n in BUSCC: substation_Vnomi[1,bus_substation[1,n]] > ignore_voltage_bounds}
  ( V[n] - (1-ratio_voltage_target)*voltage_lower_bound[1,bus_substation[1,n]] + ratio_voltage_target*voltage_upper_bound[1,bus_substation[1,n]] )**2;

# Voltage target : value V0 in input data
var target_voltage_data = sum{n in BUSVV} (V[n] - bus_V0[1,n])**2;


#
# Objective function and penalties
#
param penalty_invest_rea_pos := 10;
param penalty_invest_rea_neg := 10;
param penalty_units_reactive := 0.1;
param penalty_transfo_ratio  := 0.1;

param penalty_active_power_high := 1;
param penalty_active_power_low  := 0.01;

param penalty_voltage_target_high := 1;
param penalty_voltage_target_low  := 0.01;

minimize problem_acopf_objective:
  sum{n in BUSCC_SLACK} (
        penalty_invest_rea_pos * base100MVA * slack1_shunt_B[n]
        + penalty_invest_rea_neg * base100MVA * slack2_shunt_B[n]
    )

  # coeff_alpha == 1 : minimize sum of generation, all generating units vary with 1 unique variable alpha
  # coeff_alpha == 0 : minimize sum of squared difference between target and value
  + (if objective_choice==1 or objective_choice==2 then penalty_active_power_low else penalty_active_power_high)
  * sum{(g,n) in UNITON} (coeff_alpha * P[g,n] + (1-coeff_alpha)*( (P[g,n]-unit_Pc[1,g,n])/max(1,abs(unit_Pc[1,g,n])) )**2 )

  # Voltage for busses, ratio between Vmin and Vmax
  + (if objective_choice==1 then penalty_voltage_target_high else penalty_voltage_target_low)
  * target_voltage_ratio

  # Voltage target : value V0 in input data
  + (if objective_choice==2 then penalty_voltage_target_high else penalty_voltage_target_low)
  * target_voltage_data

  # Reactive power of units
  + penalty_units_reactive * sum{(g,n) in UNITON} (Q[g,n]/max(1,abs(corrected_unit_Qmin[g,n]),abs(corrected_unit_Qmax[g,n])))**2

  # Ratio of transformers
  + penalty_transfo_ratio * sum{(qq,m,n) in BRANCHCC_REGL_VAR} (branch_Ror[qq,m,n]-branch_Ror_var[qq,m,n])**2
  ;


# 
param solve_result_num_limit := 200;
param output_results binary default 0;
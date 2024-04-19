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

set PROBLEM_SE default { };

###########################################################
#                                                         #
#   All the constraints of the optimization problem       #
#                                                         #
###########################################################

###########################################################
#                                                         #
#              Consistency constraints                    #
#                                                         #
###########################################################

# Constraint on the slack bus (angle reference)
subject to slack_null_phase{PROBLEM_SE}:
  theta[null_phase_bus] == 0;

# Consistency for voltage magnitudes
subject to ctrl_voltage_mag_min{PROBLEM_SE, n in BUSCC}:
  V[n] >= 0.75;
subject to ctrl_voltage_mag_max{PROBLEM_SE, n in BUSCC}:
  V[n] <= 1.25;

# Consistency for voltage angles
subject to ctrl_voltage_ang_max{PROBLEM_SE, n in BUSCC}:
  theta[n] <= 3.141592;
subject to ctrl_voltage_ang_min{PROBLEM_SE, n in BUSCC}:
  theta[n] >= -3.141592;

###########################################################
#                                                         #
#         Number of topology changes allowed              #
#                                                         #
###########################################################

subject to ctrl_nb_topology_changes{PROBLEM_SE}:
  sum{(qq,k,n,l) in BRANCHCC_FULL cross BRANCH_SUSP: branch_id[1,qq,k,n] == branch_susp_id[l]} 
    abs(y[qq,k,n] - y_prior[l]) <= max_nb_topology_errors; 

###########################################################
#                                                         #
#         Hard constraints on zero-injection buses        #
#                                                         #
###########################################################

subject to ctrl_zero_injection_buses_act_power{PROBLEM_SE,
  (a,b) in BUSCC cross BUS_ZERO_INJECTION : bus_id[1,a] == bus_zero_injection_id[b]}:
      abs(
      # Flows on branches
      sum{(qq,k,n) in BRANCHCC : k == a}
      y[qq,k,n] * act_power_dir[qq,k,n]
      + sum{(qq,m,k) in BRANCHCC : k == a}
      y[qq,m,k] * act_power_inv[qq,m,k]
      # Flows on branches with one side opened
      + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED : k == a}
      y[qq,k,n] * act_power_bus2_opened[qq,k,n]
      + sum{(qq,m,k) in BRANCH_WITH_SIDE_1_OPENED : k == a}
      y[qq,m,k] * act_power_bus1_opened[qq,m,k]
      )
      = 0; # <= epsilon_max_power_balance;

        subject to ctrl_zero_injection_buses_rea_power{PROBLEM_SE,
  (a,b) in BUSCC cross BUS_ZERO_INJECTION : bus_id[1,a] == bus_zero_injection_id[b]}:
      abs(
      # Flows on branches
      sum{(qq,k,n) in BRANCHCC : k == a}
      y[qq,k,n] * rea_power_dir[qq,k,n]
      + sum{(qq,m,k) in BRANCHCC : k == a}
      y[qq,m,k] * rea_power_inv[qq,m,k]
      # Flows on branches with one side opened
      + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED : k == a}
      y[qq,k,n] * rea_power_bus2_opened[qq,k,n]
      + sum{(qq,m,k) in BRANCH_WITH_SIDE_1_OPENED : k == a}
      y[qq,m,k] * rea_power_bus1_opened[qq,m,k]
      )
      = 0; # <= epsilon_max_power_balance;

###########################################################
#                                                         #
#             Measurement residuals (in SI)               #
#                                                         #
###########################################################

# Constraints on residuals for "active power flow" measurements (MW)
subject to residual_computation_Pf{PROBLEM_SE, l in MEASURECC_Pf}:
  resid_Pf[l] = Pf_value[l] 
                - (
                  # Flows on branches (note : only one term is not null over the two sums below)
                  sum{(qq,k,n) in BRANCHCC : qq == Pf_branch[l] and k == Pf_firstbus[l] and n == Pf_secondbus[l]}
                  y[qq,k,n] * act_power_dir[qq,k,n]
                  + sum{(qq,m,k) in BRANCHCC : qq == Pf_branch[l] and k == Pf_firstbus[l] and m == Pf_secondbus[l]}
                  y[qq,m,k] * act_power_inv[qq,m,k]
                  # Flows on branches with one side opened
                  + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED : qq == Pf_branch[l] and k == Pf_firstbus[l] and n == Pf_secondbus[l]}
                  y[qq,k,n] * act_power_bus2_opened[qq,k,n]
                  + sum{(qq,m,k) in BRANCH_WITH_SIDE_1_OPENED : qq == Pf_branch[l] and k == Pf_firstbus[l] and m == Pf_secondbus[l]}
                  y[qq,m,k] * act_power_bus1_opened[qq,m,k]
                );

# Constraints on residuals for "reactive power flow" measurements (MVar)
subject to residual_computation_Qf{PROBLEM_SE, l in MEASURECC_Qf}:
  resid_Qf[l] = Qf_value[l] 
                - (
                  # Flows on branches (note : only ONE term is not null over the two sums below)
                  sum{(qq,k,n) in BRANCHCC : qq == Qf_branch[l] and k == Qf_firstbus[l] and n == Qf_secondbus[l]}
                  y[qq,k,n] * rea_power_dir[qq,k,n]
                  + sum{(qq,m,k) in BRANCHCC : qq == Qf_branch[l] and k == Qf_firstbus[l] and m == Qf_secondbus[l]}
                  y[qq,m,k] * rea_power_inv[qq,m,k]
                  # Flows on branches with one side opened
                  + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED : qq == Qf_branch[l] and k == Qf_firstbus[l] and n == Qf_secondbus[l]}
                  y[qq,k,n] * rea_power_bus2_opened[qq,k,n]
                  + sum{(qq,m,k) in BRANCH_WITH_SIDE_1_OPENED : qq == Qf_branch[l] and k == Qf_firstbus[l] and m == Qf_secondbus[l]}
                  y[qq,m,k] * rea_power_bus1_opened[qq,m,k]
                );

# Constraints on residuals for "injected active power" measurements (MW)
subject to residual_computation_P{PROBLEM_SE, l in MEASURECC_P}:
  resid_P[l] = P_value[l] 
                - (
                  # Flows on branches
                  sum{(qq,k,n) in BRANCHCC : k == P_bus[l]}
                  y[qq,k,n] * act_power_dir[qq,k,n]
                  + sum{(qq,m,k) in BRANCHCC : k == P_bus[l]}
                  y[qq,m,k] * act_power_inv[qq,m,k]
                  # Flows on branches with one side opened
                  + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED : k == P_bus[l]}
                  y[qq,k,n] * act_power_bus2_opened[qq,k,n]
                  + sum{(qq,m,k) in BRANCH_WITH_SIDE_1_OPENED : k == P_bus[l]}
                  y[qq,m,k] * act_power_bus1_opened[qq,m,k]
                );

# Constraints on residuals for "injected reactive power" measurements (MVar)
subject to residual_computation_Q{PROBLEM_SE, l in MEASURECC_Q}:
  resid_Q[l] = Q_value[l] 
                - (
                  # Flows on branches
                  sum{(qq,k,n) in BRANCHCC : k == Q_bus[l]}
                  y[qq,k,n] * rea_power_dir[qq,k,n]
                  + sum{(qq,m,k) in BRANCHCC : k == Q_bus[l]}
                  y[qq,m,k] * rea_power_inv[qq,m,k]
                  # Flows on branches with one side opened
                  + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED : k == Q_bus[l]}
                  y[qq,k,n] * rea_power_bus2_opened[qq,k,n]
                  + sum{(qq,m,k) in BRANCH_WITH_SIDE_1_OPENED : k == Q_bus[l]}
                  y[qq,m,k] * rea_power_bus1_opened[qq,m,k]
                );

# Constraints on residuals for "voltage magnitude" measurements (kV)
subject to residual_computation_V{PROBLEM_SE, l in MEASURECC_V}:
  resid_V[l] = V_value[l] - V[V_bus[l]] * substation_Vnomi[1,bus_substation[1,V_bus[l]]];

# (constraints) New type of measurement
# .... 

###########################################
#                                         #
#             Objective function          #
#                                         #
###########################################

# Sum of squares of residuals, each being divided by measurement variance
# as well as maximum value observed over a set of similar measurements.
# The latter enables to "normalize" residuals (originally computed in SI)
# and compare residuals of different types (how to compare resid_P in MW and resid_V in kV otherwise ?)
# This also keeps intact the multiplicity of measurements (e.g. we have 10 "V" measures but only 1 "Pf" measures).
# Normalizing each measurement type by "sum{l in measurement_type} measurement_value[l]" would not.

minimize problem_sum_of_squares_of_residuals:
  0
  + sum{l in MEASURECC_Pf} (resid_Pf[l])**2 / Pf_variance[l]
  + sum{l in MEASURECC_Qf} (resid_Qf[l])**2 / Qf_variance[l]
  + sum{l in MEASURECC_P} (resid_P[l])**2 / P_variance[l]
  + sum{l in MEASURECC_Q} (resid_Q[l])**2 / Q_variance[l]
  + sum{l in MEASURECC_V} (resid_V[l])**2 / V_variance[l]
  ;
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
    V[n] >= 0.5;
subject to ctrl_voltage_mag_max{PROBLEM_SE, n in BUSCC}:
    V[n] <= 1.5;

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
    sum{(qq,k,n,l) in BRANCHCC_FULL cross BRANCH_SUSP: branch_id[1,qq,k,n] == branch_susp_id[l] and y_prior[l] == 1}
        (y_prior[l] - y[qq,k,n])
    + sum{(qq,k,n,l) in BRANCHCC_FULL cross BRANCH_SUSP: branch_id[1,qq,k,n] == branch_susp_id[l] and y_prior[l] == 0}
        (y[qq,k,n] - y_prior[l])
    <= max_nb_topology_changes;

###########################################################
#                                                         #
#         Hard constraints on zero-injection buses        #
#                                                         #
###########################################################

subject to ctrl_zero_injection_buses_act_power{PROBLEM_SE,
  (a,b) in BUSCC cross BUS_ZERO_INJECTION : bus_id[1,a] == bus_zero_injection_id[b]}:
      # Flows on branches

      sum{(qq,k,n) in BRANCHCC : k == a}
      # act_power_dir[qq,k,n]
      y[qq,k,n] * (
        branch_Ror_SI[qq,k,n]^2 * branch_Gor_SI[qq,k,n] * V[k]^2 
          * substation_Vnomi[1,bus_substation[1,k]]^2
        + branch_Ror_SI[qq,k,n]^2 * branch_admi_SI[qq,k,n] * V[k]^2 * sin(branch_angper[qq,k,n])
          * substation_Vnomi[1,bus_substation[1,k]]^2
        - branch_Ror_SI[qq,k,n] * branch_Rex_SI[qq,k,n] * branch_admi_SI[qq,k,n] * V[k] * V[n] 
          * sin(branch_angper[qq,k,n] - branch_dephor[qq,k,n] + theta[n] - theta[k])
          * substation_Vnomi[1,bus_substation[1,k]] * substation_Vnomi[1,bus_substation[1,n]]
      )

      + sum{(qq,n,k) in BRANCHCC : k == a}
      # act_power_inv[qq,n,k]
      y[qq,n,k] * (
        branch_Rex_SI[qq,n,k]^2 * branch_Gex_SI[qq,n,k] * V[k]^2 
          * substation_Vnomi[1,bus_substation[1,k]]^2
        + branch_Rex_SI[qq,n,k]^2 * branch_admi_SI[qq,n,k] * V[k]^2 * sin(branch_angper[qq,n,k])
          * substation_Vnomi[1,bus_substation[1,k]]^2
        - branch_Rex_SI[qq,n,k] * branch_Ror_SI[qq,n,k] * branch_admi_SI[qq,n,k] * V[k] * V[n] 
          * sin(branch_angper[qq,n,k] + branch_dephor[qq,n,k] + theta[n] - theta[k])
          * substation_Vnomi[1,bus_substation[1,k]] * substation_Vnomi[1,bus_substation[1,n]]
      )

      # Flows on branches with one side opened

      + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED : k == a}
      # act_power_bus2_opened[qq,k,n]
      y[qq,k,n] * (
        branch_Ror_SI[qq,k,n]^2 * V[k]^2 * substation_Vnomi[1,bus_substation[1,k]]^2
        * (
          branch_Gor_SI[qq,k,n] 
          + branch_admi_SI[qq,k,n]^2 * branch_Gex_SI[qq,k,n] 
          / (
            (branch_Gex_SI[qq,k,n] + branch_admi_SI[qq,k,n] * sin(branch_angper[qq,k,n]))^2 
            + (- branch_Bex_SI[qq,k,n] + branch_admi_SI[qq,k,n] * cos(branch_angper[qq,k,n]))^2 
          ) # Shunt
          + (
            branch_Bex_SI[qq,k,n]^2 + branch_Gex_SI[qq,k,n]^2
          ) * branch_admi_SI[qq,k,n] * sin(branch_angper[qq,k,n])
          / (
            (branch_Gex_SI[qq,k,n] + branch_admi_SI[qq,k,n] * sin(branch_angper[qq,k,n]))^2 
            + (- branch_Bex_SI[qq,k,n] + branch_admi_SI[qq,k,n] * cos(branch_angper[qq,k,n]))^2 
          ) # Shunt
        )
      )

      + sum{(qq,n,k) in BRANCH_WITH_SIDE_1_OPENED : k == a}
      # act_power_bus1_opened[qq,n,k]
      y[qq,n,k] * (
        V[k]^2 * substation_Vnomi[1,bus_substation[1,k]]^2
        * (
          branch_Gex_SI[qq,n,k] 
          + branch_admi_SI[qq,n,k]^2 
          * branch_Gor_SI[qq,n,k] 
          / (
            (branch_Gor_SI[qq,n,k] + branch_admi_SI[qq,n,k] * sin(branch_angper[qq,n,k]))^2 
            + (- branch_Bor_SI[qq,n,k] + branch_admi_SI[qq,n,k] * cos(branch_angper[qq,n,k]))^2 
          ) # Shunt
          + (
            branch_Bor_SI[qq,n,k]^2 + branch_Gor_SI[qq,n,k]^2
          ) 
          * branch_admi_SI[qq,n,k] * sin(branch_angper[qq,n,k])
          / (
            (branch_Gor_SI[qq,n,k] + branch_admi_SI[qq,n,k] * sin(branch_angper[qq,n,k]))^2 
            + (- branch_Bor_SI[qq,n,k] + branch_admi_SI[qq,n,k] * cos(branch_angper[qq,n,k]))^2 
          ) # Shunt
        )
      )

      = 0; 

subject to ctrl_zero_injection_buses_rea_power{PROBLEM_SE,
  (a,b) in BUSCC cross BUS_ZERO_INJECTION : bus_id[1,a] == bus_zero_injection_id[b]}:
      # Flows on branches

      sum{(qq,k,n) in BRANCHCC : k == a}
      # rea_power_dir[qq,k,n]
      y[qq,k,n] * (
      - branch_Ror_SI[qq,k,n]^2 * branch_Bor_SI[qq,k,n] * V[k]^2
        * substation_Vnomi[1,bus_substation[1,k]]^2
      + branch_Ror_SI[qq,k,n]^2 * branch_admi_SI[qq,k,n] * V[k]^2 * cos(branch_angper[qq,k,n])
        * substation_Vnomi[1,bus_substation[1,k]]^2
      - branch_Ror_SI[qq,k,n] * branch_Rex_SI[qq,k,n] * branch_admi_SI[qq,k,n] * V[k] * V[n]
        * cos(theta[k] - theta[n] + branch_dephor[qq,k,n] - branch_angper[qq,k,n])
        * substation_Vnomi[1,bus_substation[1,k]] * substation_Vnomi[1,bus_substation[1,n]]
      )

      + sum{(qq,n,k) in BRANCHCC : k == a}
      # rea_power_inv[qq,n,k]
      y[qq,n,k] * (
        - branch_Rex_SI[qq,n,k]^2 * branch_Bex_SI[qq,n,k] * V[k]^2
          * substation_Vnomi[1,bus_substation[1,k]]^2
        + branch_Rex_SI[qq,n,k]^2 * branch_admi_SI[qq,n,k] * V[k]^2 * cos(branch_angper[qq,n,k])
          * substation_Vnomi[1,bus_substation[1,k]]^2
        - branch_Ror_SI[qq,n,k] * branch_Rex_SI[qq,n,k] * branch_admi_SI[qq,n,k] * V[k] * V[n]
          * cos(theta[k] - theta[n] - branch_dephor[qq,n,k] - branch_angper[qq,n,k])
          * substation_Vnomi[1,bus_substation[1,k]] * substation_Vnomi[1,bus_substation[1,n]]
      )

      # Flows on branches with one side opened

      + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED : k == a}
      # rea_power_bus2_opened[qq,k,n]
      y[qq,k,n] * (
        - branch_Ror_SI[qq,k,n]^2 * V[k]^2 * substation_Vnomi[1,bus_substation[1,k]]^2
        * (
          branch_Bor_SI[qq,k,n] 
          + branch_admi_SI[qq,k,n]^2 * branch_Bex_SI[qq,k,n]
          / (
            (branch_Gex_SI[qq,k,n] + branch_admi_SI[qq,k,n] * sin(branch_angper[qq,k,n]))^2 
            + (- branch_Bex_SI[qq,k,n] + branch_admi_SI[qq,k,n] * cos(branch_angper[qq,k,n]))^2 
          ) # Shunt
          - (
            branch_Bex_SI[qq,k,n]^2 + branch_Gex_SI[qq,k,n]^2
          ) 
          * branch_admi_SI[qq,k,n] * cos(branch_angper[qq,k,n])
          / (
            (branch_Gex_SI[qq,k,n] + branch_admi_SI[qq,k,n] * sin(branch_angper[qq,k,n]))^2 
            + (-branch_Bex_SI[qq,k,n] + branch_admi_SI[qq,k,n] * cos(branch_angper[qq,k,n]))^2
          ) # Shunt
        )
      )

      + sum{(qq,n,k) in BRANCH_WITH_SIDE_1_OPENED : k == a}
      # rea_power_bus1_opened[qq,n,k]
      y[qq,n,k] * (
        - V[k]^2 * substation_Vnomi[1,bus_substation[1,k]]^2
        * (
          branch_Bex_SI[qq,n,k] 
          + branch_admi_SI[qq,n,k]^2 * branch_Bor_SI[qq,n,k]
          / (
            (branch_Gor_SI[qq,n,k] + branch_admi_SI[qq,n,k] * sin(branch_angper[qq,n,k]))^2 
            + (- branch_Bor_SI[qq,n,k] + branch_admi_SI[qq,n,k] * cos(branch_angper[qq,n,k]))^2
          ) # Shunt
          - (
            branch_Bor_SI[qq,n,k]^2 + branch_Gor_SI[qq,n,k]^2
          ) 
          * branch_admi_SI[qq,n,k] * cos(branch_angper[qq,n,k])
          / (
            (branch_Gor_SI[qq,n,k] + branch_admi_SI[qq,n,k] * sin(branch_angper[qq,n,k]))^2 
            + (- branch_Bor_SI[qq,n,k] + branch_admi_SI[qq,n,k] * cos(branch_angper[qq,n,k]))^2
          ) # Shunt
        )
      )

      = 0;

###########################################################
#                                                         #
# Constraints to linearize absolute values of residuals   #
#                                                         #
###########################################################

subject to model_abs_value_1_Pf{PROBLEM_SE, l in MEASURECC_Pf}:
  epi_resid_Pf[l] >= resid_Pf[l] / sqrt(Pf_variance[l]);

subject to model_abs_value_2_Pf{PROBLEM_SE, l in MEASURECC_Pf}:
  epi_resid_Pf[l] >= - resid_Pf[l] / sqrt(Pf_variance[l]);

subject to model_abs_value_1_Qf{PROBLEM_SE, l in MEASURECC_Qf}:
  epi_resid_Qf[l] >= resid_Qf[l] / sqrt(Qf_variance[l]);

subject to model_abs_value_2_Qf{PROBLEM_SE, l in MEASURECC_Qf}:
  epi_resid_Qf[l] >= - resid_Qf[l] / sqrt(Qf_variance[l]);

subject to model_abs_value_1_P{PROBLEM_SE, l in MEASURECC_P}:
  epi_resid_P[l] >= resid_P[l] / sqrt(P_variance[l]);

subject to model_abs_value_2_P{PROBLEM_SE, l in MEASURECC_P}:
  epi_resid_P[l] >= - resid_P[l] / sqrt(P_variance[l]);

subject to model_abs_value_1_Q{PROBLEM_SE, l in MEASURECC_Q}:
  epi_resid_Q[l] >= resid_Q[l] / sqrt(Q_variance[l]);

subject to model_abs_value_2_Q{PROBLEM_SE, l in MEASURECC_Q}:
  epi_resid_Q[l] >= - resid_Q[l] / sqrt(Q_variance[l]);

subject to model_abs_value_1_V{PROBLEM_SE, l in MEASURECC_V}:
  epi_resid_V[l] >= resid_V[l] / sqrt(V_variance[l]);

subject to model_abs_value_2_V{PROBLEM_SE, l in MEASURECC_V}:
  epi_resid_V[l] >= - resid_V[l] / sqrt(V_variance[l]);

###########################################
#                                         #
#             Objective function          #
#                                         #
###########################################

# Sum of squares of residuals, each being divided by measurement variance

minimize problem_sum_of_absolute_values_of_residuals:
  0
  + sum{l in MEASURECC_Pf} epi_resid_Pf[l]
  + sum{l in MEASURECC_Qf} epi_resid_Qf[l]
  + sum{l in MEASURECC_P} epi_resid_P[l]
  + sum{l in MEASURECC_Q} epi_resid_Q[l]
  + sum{l in MEASURECC_V} epi_resid_V[l]
  + sum{(qq,k,n,l) in BRANCHCC_FULL cross BRANCH_SUSP: branch_id[1,qq,k,n] == branch_susp_id[l] and y_prior[l] == 1}
    (y_prior[l] - y[qq,k,n])
  + sum{(qq,k,n,l) in BRANCHCC_FULL cross BRANCH_SUSP: branch_id[1,qq,k,n] == branch_susp_id[l] and y_prior[l] == 0}
    (y[qq,k,n] - y_prior[l])
  ;
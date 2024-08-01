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

# Magnitude (p.u.) and Angle (rad) for the voltage of each bus
var V{n in BUSCC};
var theta{n in BUSCC};

# Status (opened/closed) of each branch
var y{(qq,k,n) in BRANCHCC_FULL} binary;

# Residuals : r = z - h(x) (in SI)

# Variables used to model absolute values in objective function
var binary_resid_Pf{l in MEASURECC_Pf} binary default 0;
var binary_resid_Qf{l in MEASURECC_Qf} binary default 0;
var binary_resid_P{l in MEASURECC_P} binary default 0;
var binary_resid_Q{l in MEASURECC_Q} binary default 0;
var binary_resid_V{l in MEASURECC_V} binary default 0;

###########################################################
#                                                         #
#        Residuals for active power flows Pf (MW)         #
#                                                         #
###########################################################

var resid_Pf{l in MEASURECC_Pf} = 
  Pf_value[l] 
  - (
    # Flows on branches (note : only one term is not null over the two sums (dir/inv) below)

    sum{(qq,k,n) in BRANCHCC : qq == Pf_branch[l] and k == Pf_firstbus[l] and n == Pf_secondbus[l]}
    y[qq,k,n] * (
      #act_power_dir[qq,k,n]
      branch_Ror_SI[qq,k,n]^2 * branch_Gor_SI[qq,k,n] * V[k]^2 
        * substation_Vnomi[1,bus_substation[1,k]]^2
      + branch_Ror_SI[qq,k,n]^2 * branch_admi_SI[qq,k,n] * V[k]^2 * sin(branch_angper[qq,k,n])
        * substation_Vnomi[1,bus_substation[1,k]]^2
      - branch_Ror_SI[qq,k,n] * branch_Rex_SI[qq,k,n] * branch_admi_SI[qq,k,n] * V[k] * V[n] 
        * sin(branch_angper[qq,k,n] - branch_dephor[qq,k,n] + theta[n] - theta[k])
        * substation_Vnomi[1,bus_substation[1,k]] * substation_Vnomi[1,bus_substation[1,n]]
    )

    + sum{(qq,n,k) in BRANCHCC : qq == Pf_branch[l] and k == Pf_firstbus[l] and n == Pf_secondbus[l]}
    y[qq,n,k] * (
      #act_power_inv[qq,n,k]
      branch_Rex_SI[qq,n,k]^2 * branch_Gex_SI[qq,n,k] * V[k]^2 
        * substation_Vnomi[1,bus_substation[1,k]]^2
      + branch_Rex_SI[qq,n,k]^2 * branch_admi_SI[qq,n,k] * V[k]^2 * sin(branch_angper[qq,n,k])
        * substation_Vnomi[1,bus_substation[1,k]]^2
      - branch_Rex_SI[qq,n,k] * branch_Ror_SI[qq,n,k] * branch_admi_SI[qq,n,k] * V[k] * V[n] 
        * sin(branch_angper[qq,n,k] + branch_dephor[qq,n,k] + theta[n] - theta[k])
        * substation_Vnomi[1,bus_substation[1,k]] * substation_Vnomi[1,bus_substation[1,n]]
    )

    # Flows on branches with one side opened

    + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED : qq == Pf_branch[l] and k == Pf_firstbus[l] and n == Pf_secondbus[l]}
    y[qq,k,n] * (
      #act_power_bus2_opened[qq,k,n]
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

    + sum{(qq,n,k) in BRANCH_WITH_SIDE_1_OPENED : qq == Pf_branch[l] and k == Pf_firstbus[l] and n == Pf_secondbus[l]}
    y[qq,n,k] * (
      #act_power_bus1_opened[qq,n,k]
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
  );

###########################################################
#                                                         #
#      Residuals for reactive power flows Qf (MVar)       #
#                                                         #
###########################################################

# Residuals for reactive power flows Qf
var resid_Qf{l in MEASURECC_Qf} = 
  Qf_value[l] 
  - (
    # Flows on branches (note : only ONE term is not null over the two sums (dir/inv) below)

    sum{(qq,k,n) in BRANCHCC : qq == Qf_branch[l] and k == Qf_firstbus[l] and n == Qf_secondbus[l]}
    y[qq,k,n] * (
      # rea_power_dir[qq,k,n]
      - branch_Ror_SI[qq,k,n]^2 * branch_Bor_SI[qq,k,n] * V[k]^2
        * substation_Vnomi[1,bus_substation[1,k]]^2
      + branch_Ror_SI[qq,k,n]^2 * branch_admi_SI[qq,k,n] * V[k]^2 * cos(branch_angper[qq,k,n])
        * substation_Vnomi[1,bus_substation[1,k]]^2
      - branch_Ror_SI[qq,k,n] * branch_Rex_SI[qq,k,n] * branch_admi_SI[qq,k,n] * V[k] * V[n]
        * cos(theta[k] - theta[n] + branch_dephor[qq,k,n] - branch_angper[qq,k,n])
        * substation_Vnomi[1,bus_substation[1,k]] * substation_Vnomi[1,bus_substation[1,n]]
    )

    + sum{(qq,n,k) in BRANCHCC : qq == Qf_branch[l] and k == Qf_firstbus[l] and n == Qf_secondbus[l]}
    y[qq,n,k] * (
      # rea_power_inv[qq,n,k]
      - branch_Rex_SI[qq,n,k]^2 * branch_Bex_SI[qq,n,k] * V[k]^2
        * substation_Vnomi[1,bus_substation[1,k]]^2
      + branch_Rex_SI[qq,n,k]^2 * branch_admi_SI[qq,n,k] * V[k]^2 * cos(branch_angper[qq,n,k])
        * substation_Vnomi[1,bus_substation[1,k]]^2
      - branch_Ror_SI[qq,n,k] * branch_Rex_SI[qq,n,k] * branch_admi_SI[qq,n,k] * V[k] * V[n]
        * cos(theta[k] - theta[n] - branch_dephor[qq,n,k] - branch_angper[qq,n,k])
        * substation_Vnomi[1,bus_substation[1,k]] * substation_Vnomi[1,bus_substation[1,n]]
    )

    # Flows on branches with one side opened

    + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED : qq == Qf_branch[l] and k == Qf_firstbus[l] and n == Qf_secondbus[l]}
    y[qq,k,n] * (
      # rea_power_bus2_opened[qq,k,n]
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

    + sum{(qq,n,k) in BRANCH_WITH_SIDE_1_OPENED : qq == Qf_branch[l] and k == Qf_firstbus[l] and n == Qf_secondbus[l]}
    y[qq,n,k] * (
      # rea_power_bus1_opened[qq,n,k]
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
  );

###########################################################
#                                                         #
#        Residuals for active power balances P (MW)       #
#                                                         #
###########################################################

var resid_P{l in MEASURECC_P} = 
  P_value[l] 
  - (
    # Flows on branches

    sum{(qq,k,n) in BRANCHCC : k == P_bus[l]}
    y[qq,k,n] * (
      # act_power_dir[qq,k,n]
      branch_Ror_SI[qq,k,n]^2 * branch_Gor_SI[qq,k,n] * V[k]^2 
        * substation_Vnomi[1,bus_substation[1,k]]^2
      + branch_Ror_SI[qq,k,n]^2 * branch_admi_SI[qq,k,n] * V[k]^2 * sin(branch_angper[qq,k,n])
        * substation_Vnomi[1,bus_substation[1,k]]^2
      - branch_Ror_SI[qq,k,n] * branch_Rex_SI[qq,k,n] * branch_admi_SI[qq,k,n] * V[k] * V[n] 
        * sin(branch_angper[qq,k,n] - branch_dephor[qq,k,n] + theta[n] - theta[k])
        * substation_Vnomi[1,bus_substation[1,k]] * substation_Vnomi[1,bus_substation[1,n]]
    )

    + sum{(qq,n,k) in BRANCHCC : k == P_bus[l]}
    y[qq,n,k] * (
      # act_power_inv[qq,n,k]
      branch_Rex_SI[qq,n,k]^2 * branch_Gex_SI[qq,n,k] * V[k]^2 
        * substation_Vnomi[1,bus_substation[1,k]]^2
      + branch_Rex_SI[qq,n,k]^2 * branch_admi_SI[qq,n,k] * V[k]^2 * sin(branch_angper[qq,n,k])
        * substation_Vnomi[1,bus_substation[1,k]]^2
      - branch_Rex_SI[qq,n,k] * branch_Ror_SI[qq,n,k] * branch_admi_SI[qq,n,k] * V[k] * V[n] 
        * sin(branch_angper[qq,n,k] + branch_dephor[qq,n,k] + theta[n] - theta[k])
        * substation_Vnomi[1,bus_substation[1,k]] * substation_Vnomi[1,bus_substation[1,n]]
    )

    # Flows on branches with one side opened

    + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED : k == P_bus[l]}
    y[qq,k,n] * (
      # act_power_bus2_opened[qq,k,n]
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

    + sum{(qq,n,k) in BRANCH_WITH_SIDE_1_OPENED : k == P_bus[l]}
    y[qq,n,k] * (
      # act_power_bus1_opened[qq,n,k]
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
  );

###########################################################
#                                                         #
#     Residuals for reactive power balances Q (MVar)       #
#                                                         #
###########################################################

var resid_Q{l in MEASURECC_Q} = 
  Q_value[l] 
  - (
    # Flows on branches

    sum{(qq,k,n) in BRANCHCC : k == Q_bus[l]}
    y[qq,k,n] * (
      # rea_power_dir[qq,k,n]
      - branch_Ror_SI[qq,k,n]^2 * branch_Bor_SI[qq,k,n] * V[k]^2
        * substation_Vnomi[1,bus_substation[1,k]]^2
      + branch_Ror_SI[qq,k,n]^2 * branch_admi_SI[qq,k,n] * V[k]^2 * cos(branch_angper[qq,k,n])
        * substation_Vnomi[1,bus_substation[1,k]]^2
      - branch_Ror_SI[qq,k,n] * branch_Rex_SI[qq,k,n] * branch_admi_SI[qq,k,n] * V[k] * V[n]
        * cos(theta[k] - theta[n] + branch_dephor[qq,k,n] - branch_angper[qq,k,n])
        * substation_Vnomi[1,bus_substation[1,k]] * substation_Vnomi[1,bus_substation[1,n]]
    )

    + sum{(qq,n,k) in BRANCHCC : k == Q_bus[l]}
    y[qq,n,k] * (
      # rea_power_inv[qq,n,k]
      - branch_Rex_SI[qq,n,k]^2 * branch_Bex_SI[qq,n,k] * V[k]^2
        * substation_Vnomi[1,bus_substation[1,k]]^2
      + branch_Rex_SI[qq,n,k]^2 * branch_admi_SI[qq,n,k] * V[k]^2 * cos(branch_angper[qq,n,k])
        * substation_Vnomi[1,bus_substation[1,k]]^2
      - branch_Ror_SI[qq,n,k] * branch_Rex_SI[qq,n,k] * branch_admi_SI[qq,n,k] * V[k] * V[n]
        * cos(theta[k] - theta[n] - branch_dephor[qq,n,k] - branch_angper[qq,n,k])
        * substation_Vnomi[1,bus_substation[1,k]] * substation_Vnomi[1,bus_substation[1,n]]
    )

    # Flows on branches with one side opened

    + sum{(qq,k,n) in BRANCH_WITH_SIDE_2_OPENED : k == Q_bus[l]}
    y[qq,k,n] * (
      # rea_power_bus2_opened[qq,k,n]
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

    + sum{(qq,n,k) in BRANCH_WITH_SIDE_1_OPENED : k == Q_bus[l]}
    y[qq,n,k] * (
      # rea_power_bus1_opened[qq,n,k]
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
  );

###########################################################
#                                                         #
#            Residuals for tensions (kV)                  #
#                                                         #
###########################################################

var resid_V{l in MEASURECC_V} = 
  V_value[l] - V[V_bus[l]] * substation_Vnomi[1,bus_substation[1,V_bus[l]]];

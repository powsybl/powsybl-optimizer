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

# Bound for target V parameter
param targetV_p_inf := 0.5;
param targetV_p_sup := 1.5;

# Bound for rho/alpha parameters

param rho_inf_tct {(qq,m,n) in BRANCHCC_TRANSFORMER} := min_branch_regl[qq,m,n];
param rho_sup_tct {(qq,m,n) in BRANCHCC_TRANSFORMER} := max_branch_regl[qq,m,n];
param alpha_inf_tct {(qq,m,n) in BRANCHCC_DEPH}        := min_branch_deph[qq,m,n];
param alpha_sup_tct {(qq,m,n) in BRANCHCC_DEPH}        := max_branch_deph[qq,m,n];

param rho_inf_default := 0.5;
param rho_sup_default := 2;
param alpha_inf_default := - 3.141592 / 2;
param alpha_sup_default := 3.141592 / 2;

param rho_p_inf {(qq,m,n) in BRANCHCC_TRANSFORMER} := 0.5 * branch_cstratio[1,qq,m,n];
param rho_p_sup {(qq,m,n) in BRANCHCC_TRANSFORMER} := 2 * branch_cstratio[1,qq,m,n];

param alpha_p_inf {(qq,m,n) in BRANCHCC_DEPH} := if alpha_inf_tct[qq,m,n] < 0 then max(alpha_inf_tct[qq,m,n] * 1.3, alpha_inf_default) else alpha_inf_tct[qq,m,n] * 0.7;
param alpha_p_sup {(qq,m,n) in BRANCHCC_DEPH} := if alpha_inf_tct[qq,m,n] < 0 then alpha_sup_tct[qq,m,n] * 0.7 else min(alpha_sup_tct[qq,m,n] * 1.3, alpha_sup_default);
# TODO : Here there is an error, but it works better... Change sigma3_inf_tct by sigma3_sup_tct

# Bound inf for Y/Xi or R/X parameters (depends on model used)
param Y_p_inf {(qq,m,n) in BRANCHCC_PENALIZED} :=  if is_admittance_control == 1 then 0               else branch_admi[qq,m,n];
param Y_p_sup {(qq,m,n) in BRANCHCC_PENALIZED} :=  if is_admittance_control == 1 then max_branch_admi else branch_admi[qq,m,n];
param xi_p_inf {(qq,m,n) in BRANCHCC_PENALIZED} := if is_xi_control         == 1 then -3.141592       else branch_angper[qq,m,n];
param xi_p_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if is_xi_control         == 1 then 3.141592        else branch_angper[qq,m,n];

# Lower/upper knowledge-based bounds for G/B of shunts
param G_inf_default := 0;
param G_sup_default := 0.2;
param B_inf_default := -2;
param B_sup_default := 2;

# Lower bound for G/B penalizated values
param G1_p_inf {(qq,m,n) in BRANCHCC_PENALIZED} := if is_g_shunt_1_control == 1 then G_inf_default else branch_Gor[1,qq,m,n];
param B1_p_inf {(qq,m,n) in BRANCHCC_PENALIZED} := if is_b_shunt_1_control == 1 then B_inf_default else branch_Bor[1,qq,m,n];
param G2_p_inf {(qq,m,n) in BRANCHCC_PENALIZED} := if is_g_shunt_2_control == 1 then G_inf_default else branch_Gex[1,qq,m,n];
param B2_p_inf {(qq,m,n) in BRANCHCC_PENALIZED} := if is_b_shunt_2_control == 1 then B_inf_default else branch_Bex[1,qq,m,n];
# TODO : Improve X/B bound by looking if there are X negative values in the network.

# Upper bound for G/B penalizated values
param G1_p_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if is_g_shunt_1_control == 1 then G_sup_default else branch_Gor[1,qq,m,n];
param B1_p_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if is_b_shunt_1_control == 1 then B_sup_default else branch_Bor[1,qq,m,n];
param G2_p_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if is_g_shunt_2_control == 1 then G_sup_default else branch_Gex[1,qq,m,n];
param B2_p_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if is_b_shunt_2_control == 1 then B_sup_default else branch_Bex[1,qq,m,n];


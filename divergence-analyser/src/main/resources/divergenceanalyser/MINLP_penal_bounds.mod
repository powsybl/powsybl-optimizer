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
param Y_p_inf := 0;
param Xi_p_inf := - 3.141592;
param Y_p_sup {(qq,m,n) in BRANCHCC_PENALIZED} := max_branch_admi;
param Xi_p_sup := 3.141592;

# Bound inf for G/B parameters
param G1_p_inf := 0;
param B1_p_inf := -0.8; # TODO : Improve X bound by looking if there are X negative values in the network. Most of the time, negative values are on the 3wt legs.
param G2_p_inf := 0;
param B2_p_inf := -0.8; # TODO : Improve X bound by looking if there are X negative values in the network. Most of the time, negative values are on the 3wt legs.

# Upper bound in practice for usual network
param G1_sup_default := 0.1;
param B1_sup_default := 1.3;
param G2_sup_default := 0.1;
param B2_sup_default := 1.3;

param G1_p_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if branch_Gor[1,qq,m,n] == 0 then G1_sup_default else min(G1_sup_default, 2 * abs(branch_Gor[1,qq,m,n]));
param B1_p_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if branch_Bor[1,qq,m,n] == 0 then B1_sup_default else min(B1_sup_default, 2 * abs(branch_Bor[1,qq,m,n]));
param G2_p_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if branch_Gex[1,qq,m,n] == 0 then G2_sup_default else min(G2_sup_default, 2 * abs(branch_Gex[1,qq,m,n]));
param B2_p_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if branch_Bex[1,qq,m,n] == 0 then B2_sup_default else min(B2_sup_default, 2 * abs(branch_Bex[1,qq,m,n]));

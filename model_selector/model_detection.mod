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

param no_penal default 1;
param branch_detection default 0;
param param_detection default 0;
param equations_detection default 0;

param is_penal_on_YKsi := 1;
param is_penal_on_GB := 1 - is_penal_on_YKsi;

param is_voltage_rho_control := 1; # For voltages and rho
param is_admi_xi_control := 1; # For admittance and Xi
param is_angle_deph_control := 1; # For angle dephasage A_i
param is_G_B_control := 1; # For G_or, G_ex, B_or and B_ex

# Selection of data importer
include "opti_models/data_importer.mod";

# Selection of teta_ccomputation model
include "opti_models/teta_ccomputation.mod";

# Common variables of optimization problems
include "opti_models/all_variables.mod";

# Bound for target V parameter
param s1_inf := 0.5;
param s1_sup := 1.5;

# Bound for rho/alpha parameters

#param sigma1_inf_tct {(qq,m,n) in BRANCHCC_TRANSFORMER} := if branch_ptrRegl[1,qq,m,n] != -1 then min_branch_Ror_tct[1,branch_ptrRegl[1,qq,m,n]] else min_branch_Ror_tct[1,branch_ptrDeph[1,qq,m,n]];
#param sigma1_sup_tct {(qq,m,n) in BRANCHCC_TRANSFORMER} := if branch_ptrRegl[1,qq,m,n] != -1 then max_branch_Ror_tct[1,branch_ptrRegl[1,qq,m,n]] else max_branch_Ror_tct[1,branch_ptrDeph[1,qq,m,n]];
#param sigma3_inf_tct {(qq,m,n) in BRANCHCC_TRANSFORMER} := if branch_ptrDeph[1,qq,m,n] != -1 then min_branch_alpha_tct[1,branch_ptrDeph[1,qq,m,n]] else min_branch_alpha_tct[1,branch_ptrRegl[1,qq,m,n]];
#param sigma3_sup_tct {(qq,m,n) in BRANCHCC_TRANSFORMER} := if branch_ptrDeph[1,qq,m,n] != -1 then max_branch_alpha_tct[1,branch_ptrDeph[1,qq,m,n]] else max_branch_alpha_tct[1,branch_ptrRegl[1,qq,m,n]];

param sigma1_inf_tct {(qq,m,n) in BRANCHCC_TRANSFORMER} := min_branch_regl[qq,m,n];
param sigma1_sup_tct {(qq,m,n) in BRANCHCC_TRANSFORMER} := max_branch_regl[qq,m,n];
param sigma3_inf_tct {(qq,m,n) in BRANCHCC_DEPH} := min_branch_deph[qq,m,n];
param sigma3_sup_tct {(qq,m,n) in BRANCHCC_DEPH} := max_branch_deph[qq,m,n];

param sigma1_inf_default := 0.5;
param sigma1_sup_default := 2;
param sigma3_inf_default := - 3.141592 / 2;
param sigma3_sup_default := 3.141592 / 2;

param sigma1_inf {(qq,m,n) in BRANCHCC_TRANSFORMER} := 0.5 * branch_cstratio_corrected[1,qq,m,n];
param sigma1_sup {(qq,m,n) in BRANCHCC_TRANSFORMER} := 2 * branch_cstratio_corrected[1,qq,m,n];

param sigma3_inf {(qq,m,n) in BRANCHCC_DEPH} := if sigma3_inf_tct[qq,m,n] < 0 then max(sigma3_inf_tct[qq,m,n] * 1.3, sigma3_inf_default) else sigma3_inf_tct[qq,m,n] * 0.7;
param sigma3_sup {(qq,m,n) in BRANCHCC_DEPH} := if sigma3_inf_tct[qq,m,n] < 0 then sigma3_sup_tct[qq,m,n] * 0.7 else min(sigma3_sup_tct[qq,m,n] * 1.3, sigma3_sup_default);
# TODO : Here there is an error, but it works better... Change sigma3_inf_tct by sigma3_sup_tct

# Bound inf for Y/Xi or R/X parameters (depends on model used)
param sigma2_inf := if is_penal_on_YKsi == 1 then 0 else min_branch_G;
param sigma4_inf := if is_penal_on_YKsi == 1 then - 3.141592 else min_branch_B;
param sigma2_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if is_penal_on_YKsi == 1 then max_branch_admi else max_branch_G;
param sigma4_sup := if is_penal_on_YKsi == 1 then 3.141592 else max_branch_B;

# Bound inf for G/B parameters
param sigma5_inf := 0;
param sigma6_inf := -0.8; # TODO : Check if 0 is not better
param sigma7_inf := 0;
param sigma8_inf := -0.8; # TODO : Check if 0 is not better

# Upper bound in practice for usual network
param sigma5_sup_default := 0.1;
param sigma6_sup_default := 1.3;
param sigma7_sup_default := 0.1;
param sigma8_sup_default := 1.3;

param sigma5_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if branch_Gor_corrected[1,qq,m,n] == 0 then sigma5_sup_default else min(sigma5_sup_default, 2 * abs(branch_Gor_corrected[1,qq,m,n]));
param sigma6_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if branch_Bor_corrected[1,qq,m,n] == 0 then sigma6_sup_default else min(sigma6_sup_default, 2 * abs(branch_Bor_corrected[1,qq,m,n]));
param sigma7_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if branch_Gex_corrected[1,qq,m,n] == 0 then sigma7_sup_default else min(sigma7_sup_default, 2 * abs(branch_Gex_corrected[1,qq,m,n]));
param sigma8_sup {(qq,m,n) in BRANCHCC_PENALIZED} := if branch_Bex_corrected[1,qq,m,n] == 0 then sigma8_sup_default else min(sigma8_sup_default, 2 * abs(branch_Bex_corrected[1,qq,m,n]));

# Inclusion of all optimization problems
include "opti_models/no_penal.mod";
include "opti_models/detection_by_branches_YKsi.mod";
include "opti_models/detection_by_param_YKsi.mod";
include "opti_models/detection_by_equations_YKsi.mod";
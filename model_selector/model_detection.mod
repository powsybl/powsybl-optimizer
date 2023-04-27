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

# Introduire boolean ici pour savoir le traitement que je devrais faire derriere.
param branch_detection default 0;
param param_detection default 1;
param equations_detection default 0;

param is_penal_on_YKsi := 0;
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

param s1_inf := 0.2;
param sigma1_inf := 0.5;
param sigma3_inf := - 3.141592 / 2;
param sigma5_inf := -max_branch_Gor;
param sigma6_inf := -max_branch_Bor;
param sigma7_inf := -max_branch_Gex;
param sigma8_inf := -max_branch_Bex;
param sigma2_inf := if is_penal_on_YKsi == 1 then 0 else min_branch_G;
param sigma4_inf := if is_penal_on_YKsi == 1 then - 3.141592 else min_branch_B;

param s1_sup := 1.8;
param sigma1_sup := 2;
param sigma3_sup := 3.141592 / 2;
param sigma5_sup := max_branch_Gor;
param sigma6_sup := max_branch_Bor;
param sigma7_sup := max_branch_Gex;
param sigma8_sup := max_branch_Bex;
param sigma2_sup := if is_penal_on_YKsi == 1 then max_branch_admi else max_branch_G;
param sigma4_sup := if is_penal_on_YKsi == 1 then 3.141592 else max_branch_B;


# Inclusion of all optimization problems
include "opti_models/detection_by_branches_YKsi.mod";
include "opti_models/detection_by_param_GB.mod";
include "opti_models/detection_by_equations_YKsi.mod";
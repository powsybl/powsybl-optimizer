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
param branch_detection default 1;
param param_detection default 0;
param equations_detection default 0;

# Selection of data importer
include "opti_models/data_importer.mod";

# Selection of teta_ccomputation model
include "opti_models/teta_ccomputation.mod";

# Common variables of optimization problems
include "opti_models/all_variables.mod";

param is_voltage_rho_control := 1; # For voltages and rho
param is_admi_xi_control := 1; # For admittance and Xi
param is_angle_deph_control := 1; # For angle dephasage A_i
param is_G_B_control := 1; # For G_or, G_ex, B_or and B_ex

# Inclusion of all optimization problems
include "opti_models/detection_by_branches.mod";
include "opti_models/detection_by_param.mod";
include "opti_models/detection_by_equations.mod";
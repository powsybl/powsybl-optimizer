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
# Author:  Jean Maeght   2022 2023
# Author:  Manuel Ruiz   2023 2024
# Author:  Oscar Lamolet 2025 2026
###############################################################################


###############################################################################
# Overrides for voltage bounds of substations
###############################################################################
#ampl_network_substations_override.txt
set BOUND_OVERRIDES dimen 1 default {};
param substation_new_Vmin    {BOUND_OVERRIDES};
param substation_new_Vmax    {BOUND_OVERRIDES};
param substation_new_checkId {BOUND_OVERRIDES} symbolic;

# Consistency checks
check {(t,s) in SUBSTATIONS: s in BOUND_OVERRIDES}: substation_id[t,s] == substation_new_checkId[s];
check {(t,s) in SUBSTATIONS: s in BOUND_OVERRIDES}: substation_new_Vmin[s] >= 0;
check {(t,s) in SUBSTATIONS: s in BOUND_OVERRIDES}: substation_new_Vmax[s] >= 0;
check {(t,s) in SUBSTATIONS: s in BOUND_OVERRIDES}: substation_new_Vmin[s] < substation_new_Vmax[s];


###############################################################################
# Controls for shunts decision
###############################################################################
# param_shunts.txt
# All shunts are considered fixed to their value in ampl_network_shunts.txt (set and parameters based on SHUNT above)
# Only shunts listed here will be changed by this reactive opf
set PARAM_SHUNT  dimen 1 default {};
param param_shunt_id{PARAM_SHUNT} symbolic;
check {(t,s,n) in SHUNT: s in PARAM_SHUNT}: shunt_id[t,s,n] == param_shunt_id[s];


###############################################################################
# Controls for reactive power of generating units
###############################################################################
# param_generators_reactive.txt
# All units are considered with variable Q, within bounds.
# Only units listed in this file will be considered with fixed reactive power value
#"num" "id"
set PARAM_UNIT_FIXQ  dimen 1 default {};
param param_unit_fixq_id{PARAM_UNIT_FIXQ} symbolic;
check {(t,g,n) in UNIT: g in PARAM_UNIT_FIXQ}: unit_id[t,g,n] == param_unit_fixq_id[g];


###############################################################################
# Controls for transformers
###############################################################################
# param_transformers.txt
# All transformers are considered with fixed ratio
# Only transformers listed in this file will be considered with variable ratio value
#"num" "id"
set PARAM_TRANSFORMERS_RATIO_VARIABLE  dimen 1 default {};
param param_transformers_ratio_variable_id{PARAM_TRANSFORMERS_RATIO_VARIABLE} symbolic;
check {(t,qq,m,n) in BRANCH: qq in PARAM_TRANSFORMERS_RATIO_VARIABLE}: branch_id[t,qq,m,n] == param_transformers_ratio_variable_id[qq];


###############################################################################
# Controls for parallel transformer groups
###############################################################################
# param_parallel_transformers.txt
# Members of a LARGE parallel group: all branches sharing a num_group are tied to a
# single shared ratio variable, bounded by [group_rho_min, group_rho_max] (the rho
# intersection, repeated on every row). Indexed by (num_group, num_branch).
#"num_group" "num_branch" "group_rho_min" "group_rho_max" "id"
set PARAM_PARALLEL_TRANSFORMERS  dimen 2 default {};
param param_parallel_transformers_rho_min{PARAM_PARALLEL_TRANSFORMERS};
param param_parallel_transformers_rho_max{PARAM_PARALLEL_TRANSFORMERS};
param param_parallel_transformers_id     {PARAM_PARALLEL_TRANSFORMERS} symbolic;
# No id check here: LARGE-group members are all variable-ratio, hence already covered
# by the PARAM_TRANSFORMERS_RATIO_VARIABLE id check above.

# param_fixed_ratio_transformers.txt
# Members of POINT/EMPTY parallel groups: each variable member is fixed to fixed_rho.
#"num_branch" "fixed_rho" "id"
set PARAM_FIXED_RATIO_TRANSFORMERS  dimen 1 default {};
param param_fixed_ratio_transformers_rho{PARAM_FIXED_RATIO_TRANSFORMERS};
param param_fixed_ratio_transformers_id {PARAM_FIXED_RATIO_TRANSFORMERS} symbolic;
check {(t,qq,m,n) in BRANCH: qq in PARAM_FIXED_RATIO_TRANSFORMERS}: branch_id[t,qq,m,n] == param_fixed_ratio_transformers_id[qq];


###############################################################################
# Buses with reactive slacks
###############################################################################
# param_buses_with_reactive_slack.txt
# If buses_with_reactive_slacks == "CONFIGURED" then only buses listed in this file will have reactive slacks attached in ACOPF
#"num" "id"
set PARAM_BUSES_WITH_REACTIVE_SLACK  dimen 1 default {};
param param_buses_with_reactive_slack_id{PARAM_BUSES_WITH_REACTIVE_SLACK} symbolic;
check {(t,n) in BUS: n in PARAM_BUSES_WITH_REACTIVE_SLACK}: bus_id[t,n] == param_buses_with_reactive_slack_id[n];

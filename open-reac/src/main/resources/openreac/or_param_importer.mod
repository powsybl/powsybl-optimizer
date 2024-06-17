###############################################################################
#
# Copyright (c) 2022 2023 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
###############################################################################

###############################################################################
# Reactive OPF
# Author:  Jean Maeght 2022 2023
# Author:  Manuel Ruiz 2023 2024
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
# Buses with reactive slacks
###############################################################################
# param_buses_with_reactive_slack.txt
# If buses_with_reactive_slacks == "CONFIGURED" then only buses listed in this file will have reactive slacks attached in ACOPF
#"num" "id"
set PARAM_BUSES_WITH_REACTIVE_SLACK  dimen 1 default {};
param param_buses_with_reactive_slack_id{PARAM_BUSES_WITH_REACTIVE_SLACK} symbolic;
check {(t,n) in BUS: n in PARAM_BUSES_WITH_REACTIVE_SLACK}: bus_id[t,n] == param_buses_with_reactive_slack_id[n];
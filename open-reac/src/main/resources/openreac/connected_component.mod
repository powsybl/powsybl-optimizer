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

set PROBLEM_CCOMP default { };

###############################################################################
#
# Variables and contraints for connexity computation
#
###############################################################################
# Even if set BUS2 is only with busses in connex component (CC) number '0', for an OPF we
# have to do connexity computation using only AC branches, ie in only one single synchronous area.
# Indeed, HVDC branches may connect 2 synchronous areas; one might consider in that case that de grid is connex
# In our case we have to consider only buses which are connected to reference bus with AC branches
var teta_ccomputation{BUS2} >=0, <=1;
subject to ctr_null_phase_bus_cccomputation{PROBLEM_CCOMP}: teta_ccomputation[null_phase_bus] = 0;
subject to ctr_flow_cccomputation{PROBLEM_CCOMP, (qq,m,n) in BRANCH2}: teta_ccomputation[m]-teta_ccomputation[n]=0;
maximize cccomputation_objective: sum{n in BUS2} teta_ccomputation[n];
# All busses AC-connected to null_phase_bus will have '0' as optimal value, other will have '1' 
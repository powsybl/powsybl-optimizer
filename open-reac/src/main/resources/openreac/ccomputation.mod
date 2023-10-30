###############################################################################
#
# Copyright (c) 2022 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
###############################################################################

###############################################################################
# Reactive OPF
# Author:  Jean Maeght 2022 2023
###############################################################################

# Definition of optimization problem
set PROBLEM_CCOMP default { };


###############################################################################
# Variables
###############################################################################

var teta_ccomputation{BUS2} >=0, <=1;


###############################################################################
# Constraints
###############################################################################

subject to ctr_null_phase_bus_cccomputation{PROBLEM_CCOMP}: teta_ccomputation[null_phase_bus] = 0;

subject to ctr_flow_cccomputation{PROBLEM_CCOMP, (qq,m,n) in BRANCH2}: teta_ccomputation[m]-teta_ccomputation[n]=0;
# All busses AC-connected to null_phase_bus will have '0' as optimal value, other will have '1'


###############################################################################
# Objective function 
###############################################################################

maximize cccomputation_objective: sum{n in BUS2} teta_ccomputation[n];
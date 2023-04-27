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

###########################################################
#                                                         #
#     All the variables of the optimization problems      #
#                                                         #
###########################################################


# V and teta parameters for all the buses
var V{n in BUSCC} >= 0.8, <= 1.2; 
var teta{n in BUSCC} <= 3.141592, >= -3.141592;

# Slack variables for equations PV, P balance and Q balance
var s1{n in BUSCC_PV};
var s2{n in BUSCC diff {null_phase_bus}};
var s3{n in BUSCC_PQ};

# Slack variables for parameters on branches
var sigma1{(qq,m,n) in BRANCHCC_PENALIZED};
var sigma2{(qq,m,n) in BRANCHCC_PENALIZED};
var sigma3{(qq,m,n) in BRANCHCC_PENALIZED};
var sigma4{(qq,m,n) in BRANCHCC_PENALIZED};
var sigma5{(qq,m,n) in BRANCHCC_PENALIZED};
var sigma6{(qq,m,n) in BRANCHCC_PENALIZED};
var sigma7{(qq,m,n) in BRANCHCC_PENALIZED};
var sigma8{(qq,m,n) in BRANCHCC_PENALIZED};

#
# Binary variables for activation of previous slack variables
#

# Detection by equations
var b_s1{n in BUSCC_PV} binary;
var b_s2{n in BUSCC diff {null_phase_bus}} binary;
var b_s3{n in BUSCC_PQ} binary;

# Detection by branches
var b_branch{(qq,m,n) in BRANCHCC_PENALIZED} binary;

# Detection by params
var b_sigma1{(qq,m,n) in BRANCHCC_PENALIZED} binary;
var b_sigma2{(qq,m,n) in BRANCHCC_PENALIZED} binary;
var b_sigma3{(qq,m,n) in BRANCHCC_PENALIZED} binary;
var b_sigma4{(qq,m,n) in BRANCHCC_PENALIZED} binary;
var b_sigma5{(qq,m,n) in BRANCHCC_PENALIZED} binary;
var b_sigma6{(qq,m,n) in BRANCHCC_PENALIZED} binary;
var b_sigma7{(qq,m,n) in BRANCHCC_PENALIZED} binary;
var b_sigma8{(qq,m,n) in BRANCHCC_PENALIZED} binary;

include "opti_models/lf_YKsi_variables.mod";
include "opti_models/lf_GB_variables.mod";
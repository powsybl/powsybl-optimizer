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

# Data import and create sets/params
include "data_importer.mod";

# teta_ccomputation model for CC computation
include "cc_computation.mod";

# Variables of optimization problem
include "MINLP_variables_penal.mod";

# Bounds for each penalization
include "MINLP_penal_bounds.mod";

# Inclusion of all optimization problems
include "MINLP_formulation_YXi_penal.mod";
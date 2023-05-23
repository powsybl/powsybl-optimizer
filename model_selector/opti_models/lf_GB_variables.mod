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

#
# Flows in one direction, then the inverse
#

var Red_Tran_Act_Dir_GB{(qq,m,n) in BRANCHCC} =
  - (rho_penalized[qq,m,n]) * V[n] 
  * ((branch_G[qq,m,n] + sigma2[qq,m,n]) * cos(teta[m] - teta[n] + (alpha_penalized[qq,m,n]))
  + (branch_B[qq,m,n] + sigma4[qq,m,n]) * sin(teta[m] - teta[n] + (alpha_penalized[qq,m,n])))
  + (rho_penalized[qq,m,n])**2 * V[m] * ((branch_G[qq,m,n] + sigma2[qq,m,n]) + (branch_Gor_corrected[1,qq,m,n] + sigma5[qq,m,n]));

var Red_Tran_Rea_Dir_GB{(qq,m,n) in BRANCHCC} = 
  - (rho_penalized[qq,m,n]) * V[n] 
  * ((branch_G[qq,m,n] + sigma2[qq,m,n]) * sin(teta[m] - teta[n] + (alpha_penalized[qq,m,n]))
  - (branch_B[qq,m,n] + sigma4[qq,m,n]) * cos(teta[m] - teta[n] + (alpha_penalized[qq,m,n])))
  - (rho_penalized[qq,m,n])**2 * V[m] * ((branch_B[qq,m,n] + sigma4[qq,m,n]) + (branch_Bor_corrected[1,qq,m,n] + sigma6[qq,m,n]));

var Red_Tran_Act_Inv_GB{(qq,m,n) in BRANCHCC} = 
  - (rho_penalized[qq,m,n]) * V[m]
  * ((branch_G[qq,m,n] + sigma2[qq,m,n]) * cos(teta[n] - teta[m] - (alpha_penalized[qq,m,n]))
  + (branch_B[qq,m,n] + sigma4[qq,m,n]) * sin(teta[n] - teta[m] - (alpha_penalized[qq,m,n])))
  + V[n] * ((branch_G[qq,m,n] + sigma2[qq,m,n]) + (branch_Gex_corrected[1,qq,m,n]+sigma7[qq,m,n]));

var Red_Tran_Rea_Inv_GB{(qq,m,n) in BRANCHCC} =
  - (rho_penalized[qq,m,n]) * V[m] 
  * ((branch_G[qq,m,n] + sigma2[qq,m,n]) * sin(teta[n] - teta[m] - (alpha_penalized[qq,m,n]))
  - (branch_B[qq,m,n] + sigma4[qq,m,n]) * cos(teta[n] - teta[m] - (alpha_penalized[qq,m,n])))
  - V[n] * ((branch_B[qq,m,n] + sigma4[qq,m,n]) + (branch_Bex_corrected[1,qq,m,n] + sigma8[qq,m,n]));

#
# Flows on branches with one side opened
#

var Act_branch_bus_2_opened_GB{(qq,m,n) in BRANCH_WITH_SIDE_2_OPENED} =
  (rho_penalized[qq,m,n])**2 * V[m] * 
  ((branch_Gor_corrected[1,qq,m,n] + sigma5[qq,m,n]) 
  + ((branch_G[qq,m,n] + sigma2[qq,m,n])**2 + (branch_B[qq,m,n] + sigma4[qq,m,n])**2) * (branch_Gex_corrected[1,qq,m,n] + sigma7[qq,m,n]) 
  / ( ((branch_Gex_corrected[1,qq,m,n] + sigma7[qq,m,n]) + (branch_G[qq,m,n] + sigma2[qq,m,n]))**2 
  + (-(branch_Bex_corrected[1,qq,m,n] + sigma8[qq,m,n]) -(branch_B[qq,m,n] + sigma4[qq,m,n]))**2 ) # Shunt
  + ((branch_Bex_corrected[1,qq,m,n] + sigma8[qq,m,n])**2 + (branch_Gex_corrected[1,qq,m,n] + sigma7[qq,m,n])**2) * (branch_G[qq,m,n] + sigma2[qq,m,n])
  / ( ((branch_Gex_corrected[1,qq,m,n] + sigma7[qq,m,n]) + (branch_G[qq,m,n] + sigma2[qq,m,n]))**2 
  + (-(branch_Bex_corrected[1,qq,m,n] + sigma8[qq,m,n]) + -(branch_B[qq,m,n] + sigma4[qq,m,n]))**2 ) # Shunt
  );

var Rea_branch_bus_2_opened_GB{(qq,m,n) in BRANCH_WITH_SIDE_2_OPENED} = 
  - (rho_penalized[qq,m,n])**2 * V[m] *
  ((branch_Bor_corrected[1,qq,m,n] + sigma6[qq,m,n]) 
  + ((branch_G[qq,m,n] + sigma2[qq,m,n])**2 + (branch_B[qq,m,n] + sigma4[qq,m,n])**2) * (branch_Bex_corrected[1,qq,m,n] + sigma8[qq,m,n])
  / ( ((branch_Gex_corrected[1,qq,m,n] + sigma7[qq,m,n]) + (branch_G[qq,m,n] + sigma2[qq,m,n]))**2 
  + (-(branch_Bex_corrected[1,qq,m,n] + sigma8[qq,m,n]) -(branch_B[qq,m,n] + sigma4[qq,m,n]))**2 ) # Shunt
  - ((branch_Bex_corrected[1,qq,m,n] + sigma8[qq,m,n])**2 + (branch_Gex_corrected[1,qq,m,n] + sigma7[qq,m,n])**2) * -(branch_B[qq,m,n] + sigma4[qq,m,n])
  / ( ((branch_Gex_corrected[1,qq,m,n] + sigma7[qq,m,n]) + (branch_G[qq,m,n] + sigma2[qq,m,n]))**2 
  + (-(branch_Bex_corrected[1,qq,m,n] + sigma8[qq,m,n]) -(branch_B[qq,m,n] + sigma4[qq,m,n]))**2 ) # Shunt
  );

var Act_branch_bus_1_opened_GB{(qq,m,n) in BRANCH_WITH_SIDE_1_OPENED} =
  V[n] * 
  ((branch_Gex_corrected[1,qq,m,n] + sigma7[qq,m,n]) 
  + ((branch_G[qq,m,n] + sigma2[qq,m,n])**2 + (branch_B[qq,m,n] + sigma4[qq,m,n])**2) * (branch_Gor_corrected[1,qq,m,n] + sigma5[qq,m,n]) 
  / ( ((branch_Gor_corrected[1,qq,m,n] + sigma5[qq,m,n]) + (branch_G[qq,m,n] + sigma2[qq,m,n]))**2 
  + (-(branch_Bor_corrected[1,qq,m,n] + sigma6[qq,m,n]) -(branch_B[qq,m,n] + sigma4[qq,m,n]))**2 ) # Shunt
  + ((branch_Bor_corrected[1,qq,m,n] + sigma6[qq,m,n])**2 + (branch_Gor_corrected[1,qq,m,n] + sigma5[qq,m,n])**2) * (branch_G[qq,m,n] + sigma2[qq,m,n])
  / ( ((branch_Gor_corrected[1,qq,m,n] + sigma5[qq,m,n]) + (branch_G[qq,m,n] + sigma2[qq,m,n]))**2 
  + (-(branch_Bor_corrected[1,qq,m,n] + sigma6[qq,m,n]) -(branch_B[qq,m,n] + sigma4[qq,m,n]))**2 ) # Shunt
  );

var Rea_branch_bus_1_opened_GB{(qq,m,n) in BRANCH_WITH_SIDE_1_OPENED} = 
  - V[n] *
  ((branch_Bex_corrected[1,qq,m,n] + sigma8[qq,m,n]) 
  + ((branch_G[qq,m,n] + sigma2[qq,m,n])**2 + (branch_B[qq,m,n] + sigma4[qq,m,n])**2) * (branch_Bor_corrected[1,qq,m,n] + sigma6[qq,m,n])
  / ( ((branch_Gor_corrected[1,qq,m,n] + sigma5[qq,m,n]) + (branch_G[qq,m,n] + sigma2[qq,m,n]))**2 
  + (-(branch_Bor_corrected[1,qq,m,n] + sigma6[qq,m,n]) -(branch_B[qq,m,n] + sigma4[qq,m,n]))**2 ) # Shunt
  - ((branch_Bor_corrected[1,qq,m,n] + sigma6[qq,m,n])**2 + (branch_Gor_corrected[1,qq,m,n] + sigma5[qq,m,n])**2) * -(branch_B[qq,m,n] + sigma4[qq,m,n])
  / ( ((branch_Gor_corrected[1,qq,m,n] + sigma5[qq,m,n]) + (branch_G[qq,m,n] + sigma2[qq,m,n]))**2 
  + (-(branch_Bor_corrected[1,qq,m,n] + sigma6[qq,m,n]) -(branch_B[qq,m,n] + sigma4[qq,m,n]))**2 ) # Shunt
  );
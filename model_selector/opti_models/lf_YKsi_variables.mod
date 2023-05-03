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


var Red_Tran_Act_Dir_YKsi{(qq,m,n) in BRANCHCC} =
  (rho_penalized[qq,m,n]) * V[n] * (branch_admi[qq,m,n]+sigma2[qq,m,n]) * sin(teta[m]-teta[n]+(alpha_penalized[qq,m,n])-(branch_angper[qq,m,n]+sigma4[qq,m,n]))
  + (rho_penalized[qq,m,n])**2 * V[m] * ((branch_admi[qq,m,n]+sigma2[qq,m,n])*sin(branch_angper[qq,m,n]+sigma4[qq,m,n])+(branch_Gor[1,qq,m,n]+sigma5[qq,m,n]));

var Red_Tran_Rea_Dir_YKsi{(qq,m,n) in BRANCHCC} = 
  - (rho_penalized[qq,m,n]) * V[n] * (branch_admi[qq,m,n]+sigma2[qq,m,n]) * cos(teta[m]-teta[n]+(alpha_penalized[qq,m,n])-(branch_angper[qq,m,n]+sigma4[qq,m,n]))
  + (rho_penalized[qq,m,n])**2 * V[m] * ((branch_admi[qq,m,n]+sigma2[qq,m,n])*cos(branch_angper[qq,m,n]+sigma4[qq,m,n])-(branch_Bor[1,qq,m,n]+sigma6[qq,m,n]));

var Red_Tran_Act_Inv_YKsi{(qq,m,n) in BRANCHCC} = 
  (rho_penalized[qq,m,n]) * V[m] * (branch_admi[qq,m,n]+sigma2[qq,m,n]) * sin(teta[n]-teta[m]-(alpha_penalized[qq,m,n])-(branch_angper[qq,m,n]+sigma4[qq,m,n]))
  + V[n] * ((branch_admi[qq,m,n]+sigma2[qq,m,n])*sin(branch_angper[qq,m,n]+sigma4[qq,m,n])+(branch_Gex[1,qq,m,n]+sigma7[qq,m,n]));

var Red_Tran_Rea_Inv_YKsi{(qq,m,n) in BRANCHCC} =
  - (rho_penalized[qq,m,n]) * V[m] * (branch_admi[qq,m,n]+sigma2[qq,m,n]) * cos(teta[n]-teta[m]-(alpha_penalized[qq,m,n])-(branch_angper[qq,m,n]+sigma4[qq,m,n]))
  + V[n] * ((branch_admi[qq,m,n]+sigma2[qq,m,n])*cos(branch_angper[qq,m,n]+sigma4[qq,m,n])-(branch_Bex[1,qq,m,n]+sigma8[qq,m,n]));

#
# Flows on shunts
#

var Act_branch_bus_2_opened_YKsi{(qq,m,n) in BRANCH_WITH_SHUNT_1} =
  (rho_penalized[qq,m,n])**2 * V[m] * 
  ((branch_Gor[1,qq,m,n] + sigma5[qq,m,n]) 
  + (branch_admi[qq,m,n] + sigma2[qq,m,n])**2 * (branch_Gex[1,qq,m,n] + sigma7[qq,m,n]) 
  / ( ((branch_Gex[1,qq,m,n] + sigma7[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * sin(branch_angper[qq,m,n] + sigma4[qq,m,n]))**2 
  + (-(branch_Bex[1,qq,m,n] + sigma8[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * cos(branch_angper[qq,m,n] + sigma4[qq,m,n]))**2 ) # Shunt
  + ((branch_Bex[1,qq,m,n] + sigma8[qq,m,n])**2 + (branch_Gex[1,qq,m,n] + sigma7[qq,m,n])**2) * (branch_admi[qq,m,n]+sigma2[qq,m,n]) * sin(branch_angper[qq,m,n]+sigma4[qq,m,n])
  / ( ((branch_Gex[1,qq,m,n] + sigma7[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * sin(branch_angper[qq,m,n] + sigma4[qq,m,n]))**2 
  + (-(branch_Bex[1,qq,m,n] + sigma8[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * cos(branch_angper[qq,m,n] + sigma4[qq,m,n]))**2 ) # Shunt
  );

var Rea_branch_bus_2_opened_YKsi{(qq,m,n) in BRANCH_WITH_SHUNT_1} = 
  - (rho_penalized[qq,m,n])**2 * V[m] *
  ((branch_Bor[1,qq,m,n] + sigma6[qq,m,n]) 
  + (branch_admi[qq,m,n] + sigma2[qq,m,n])**2 * (branch_Bex[1,qq,m,n] + sigma8[qq,m,n])
  / ( ((branch_Gex[1,qq,m,n] + sigma7[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * sin((branch_angper[qq,m,n] + sigma4[qq,m,n])))**2 
  + (-(branch_Bex[1,qq,m,n] + sigma8[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * cos((branch_angper[qq,m,n] + sigma4[qq,m,n])))**2 ) # Shunt
  - ((branch_Bex[1,qq,m,n] + sigma8[qq,m,n])**2 + (branch_Gex[1,qq,m,n] + sigma7[qq,m,n])**2) * (branch_admi[qq,m,n] + sigma2[qq,m,n]) * cos((branch_angper[qq,m,n] + sigma4[qq,m,n]))
  / ( ((branch_Gex[1,qq,m,n] + sigma7[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * sin((branch_angper[qq,m,n] + sigma4[qq,m,n])))**2 
  + (-(branch_Bex[1,qq,m,n] + sigma8[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * cos((branch_angper[qq,m,n] + sigma4[qq,m,n])))**2 ) # Shunt
  );

var Act_branch_bus_1_opened_YKsi{(qq,m,n) in BRANCH_WITH_SHUNT_2} =
  V[n] * 
  ((branch_Gex[1,qq,m,n] + sigma7[qq,m,n]) 
  + (branch_admi[qq,m,n] + sigma2[qq,m,n])**2 * (branch_Gor[1,qq,m,n] + sigma5[qq,m,n]) 
  / ( ((branch_Gor[1,qq,m,n] + sigma5[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * sin((branch_angper[qq,m,n] + sigma4[qq,m,n])))**2 
  + (-(branch_Bor[1,qq,m,n] + sigma6[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * cos((branch_angper[qq,m,n] + sigma4[qq,m,n])))**2 ) # Shunt
  + ((branch_Bor[1,qq,m,n] + sigma6[qq,m,n])**2 + (branch_Gor[1,qq,m,n] + sigma5[qq,m,n])**2) * (branch_admi[qq,m,n] + sigma2[qq,m,n]) * sin((branch_angper[qq,m,n] + sigma4[qq,m,n]))
  / ( ((branch_Gor[1,qq,m,n] + sigma5[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * sin((branch_angper[qq,m,n] + sigma4[qq,m,n])))**2 
  + (-(branch_Bor[1,qq,m,n] + sigma6[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * cos((branch_angper[qq,m,n] + sigma4[qq,m,n])))**2 ) # Shunt
  );

var Rea_branch_bus_1_opened_YKsi{(qq,m,n) in BRANCH_WITH_SHUNT_2} = 
  - V[n] *
  ((branch_Bex[1,qq,m,n] + sigma8[qq,m,n]) 
  + (branch_admi[qq,m,n] + sigma2[qq,m,n])**2 * (branch_Bor[1,qq,m,n] + sigma6[qq,m,n])
  / ( ((branch_Gor[1,qq,m,n] + sigma5[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * sin((branch_angper[qq,m,n] + sigma4[qq,m,n])))**2 
  + (-(branch_Bor[1,qq,m,n] + sigma6[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * cos((branch_angper[qq,m,n] + sigma4[qq,m,n])))**2 ) # Shunt
  - ((branch_Bor[1,qq,m,n] + sigma6[qq,m,n])**2 + (branch_Gor[1,qq,m,n] + sigma5[qq,m,n])**2) * (branch_admi[qq,m,n] + sigma2[qq,m,n]) * cos((branch_angper[qq,m,n] + sigma4[qq,m,n]))
  / ( ((branch_Gor[1,qq,m,n] + sigma5[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * sin((branch_angper[qq,m,n] + sigma4[qq,m,n])))**2 
  + (-(branch_Bor[1,qq,m,n] + sigma6[qq,m,n]) + (branch_admi[qq,m,n] + sigma2[qq,m,n]) * cos((branch_angper[qq,m,n] + sigma4[qq,m,n])))**2 ) # Shunt
  );
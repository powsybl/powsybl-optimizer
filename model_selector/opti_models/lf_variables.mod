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

var Red_Tran_Act_Dir_no_penal{(qq,m,n) in BRANCHCC} =
  (branch_Ror[qq,m,n]) * V[n] * (branch_admi[qq,m,n]) * sin(teta[m]-teta[n]+(branch_dephor[qq,m,n])-(branch_angper[qq,m,n]))
  + (branch_Ror[qq,m,n])**2 * V[m] * ((branch_admi[qq,m,n])*sin(branch_angper[qq,m,n])+(branch_Gor[1,qq,m,n]));

var Red_Tran_Rea_Dir_no_penal{(qq,m,n) in BRANCHCC} = 
  - (branch_Ror[qq,m,n]) * V[n] * (branch_admi[qq,m,n]) * cos(teta[m]-teta[n]+(branch_dephor[qq,m,n])-(branch_angper[qq,m,n]))
  + (branch_Ror[qq,m,n])**2 * V[m] * ((branch_admi[qq,m,n])*cos(branch_angper[qq,m,n])-(branch_Bor[1,qq,m,n]));

var Red_Tran_Act_Inv_no_penal{(qq,m,n) in BRANCHCC} = 
  (branch_Ror[qq,m,n]) * V[m] * (branch_admi[qq,m,n]) * sin(teta[n]-teta[m]-(branch_dephor[qq,m,n])-(branch_angper[qq,m,n]))
  + V[n] * ((branch_admi[qq,m,n])*sin(branch_angper[qq,m,n])+(branch_Gex[1,qq,m,n]));

var Red_Tran_Rea_Inv_no_penal{(qq,m,n) in BRANCHCC} =
  - (branch_Ror[qq,m,n]) * V[m] * (branch_admi[qq,m,n]) * cos(teta[n]-teta[m]-(branch_dephor[qq,m,n])-(branch_angper[qq,m,n]))
  + V[n] * ((branch_admi[qq,m,n])*cos(branch_angper[qq,m,n])-(branch_Bex[1,qq,m,n]));

#
# Flows on branches with one side opened
#

var Act_branch_bus_2_opened_no_penal{(qq,m,n) in BRANCH_WITH_SIDE_2_OPENED} =
  (branch_Ror[qq,m,n])**2 * V[m] * 
  ((branch_Gor[1,qq,m,n]) 
  + (branch_admi[qq,m,n])**2 * (branch_Gex[1,qq,m,n]) 
  / ( ((branch_Gex[1,qq,m,n]) + (branch_admi[qq,m,n]) * sin(branch_angper[qq,m,n]))**2 
  + (-(branch_Bex[1,qq,m,n]) + (branch_admi[qq,m,n]) * cos(branch_angper[qq,m,n]))**2 ) # Shunt
  + ((branch_Bex[1,qq,m,n])**2 + (branch_Gex[1,qq,m,n])**2) * (branch_admi[qq,m,n]) * sin(branch_angper[qq,m,n])
  / ( ((branch_Gex[1,qq,m,n]) + (branch_admi[qq,m,n]) * sin(branch_angper[qq,m,n]))**2 
  + (-(branch_Bex[1,qq,m,n]) + (branch_admi[qq,m,n]) * cos(branch_angper[qq,m,n]))**2 ) # Shunt
  );

var Rea_branch_bus_2_opened_no_penal{(qq,m,n) in BRANCH_WITH_SIDE_2_OPENED} = 
  - (branch_Ror[qq,m,n])**2 * V[m] *
  ((branch_Bor[1,qq,m,n]) 
  + (branch_admi[qq,m,n])**2 * (branch_Bex[1,qq,m,n])
  / ( ((branch_Gex[1,qq,m,n]) + (branch_admi[qq,m,n]) * sin((branch_angper[qq,m,n])))**2 
  + (-(branch_Bex[1,qq,m,n]) + (branch_admi[qq,m,n]) * cos((branch_angper[qq,m,n])))**2 ) # Shunt
  - ((branch_Bex[1,qq,m,n])**2 + (branch_Gex[1,qq,m,n])**2) * (branch_admi[qq,m,n]) * cos((branch_angper[qq,m,n]))
  / ( ((branch_Gex[1,qq,m,n]) + (branch_admi[qq,m,n]) * sin((branch_angper[qq,m,n])))**2 
  + (-(branch_Bex[1,qq,m,n]) + (branch_admi[qq,m,n]) * cos((branch_angper[qq,m,n])))**2 ) # Shunt
  );

var Act_branch_bus_1_opened_no_penal{(qq,m,n) in BRANCH_WITH_SIDE_1_OPENED} =
  V[n] * 
  ((branch_Gex[1,qq,m,n]) 
  + (branch_admi[qq,m,n])**2 * (branch_Gor[1,qq,m,n]) 
  / ( ((branch_Gor[1,qq,m,n]) + (branch_admi[qq,m,n]) * sin((branch_angper[qq,m,n])))**2 
  + (-(branch_Bor[1,qq,m,n]) + (branch_admi[qq,m,n]) * cos((branch_angper[qq,m,n])))**2 ) # Shunt
  + ((branch_Bor[1,qq,m,n])**2 + (branch_Gor[1,qq,m,n])**2) * (branch_admi[qq,m,n]) * sin((branch_angper[qq,m,n]))
  / ( ((branch_Gor[1,qq,m,n]) + (branch_admi[qq,m,n]) * sin((branch_angper[qq,m,n])))**2 
  + (-(branch_Bor[1,qq,m,n]) + (branch_admi[qq,m,n]) * cos((branch_angper[qq,m,n])))**2 ) # Shunt
  );

var Rea_branch_bus_1_opened_no_penal{(qq,m,n) in BRANCH_WITH_SIDE_1_OPENED} = 
  - V[n] *
  ((branch_Bex[1,qq,m,n]) 
  + (branch_admi[qq,m,n])**2 * (branch_Bor[1,qq,m,n])
  / ( ((branch_Gor[1,qq,m,n]) + (branch_admi[qq,m,n]) * sin((branch_angper[qq,m,n])))**2 
  + (-(branch_Bor[1,qq,m,n]) + (branch_admi[qq,m,n]) * cos((branch_angper[qq,m,n])))**2 ) # Shunt
  - ((branch_Bor[1,qq,m,n])**2 + (branch_Gor[1,qq,m,n])**2) * (branch_admi[qq,m,n]) * cos((branch_angper[qq,m,n]))
  / ( ((branch_Gor[1,qq,m,n]) + (branch_admi[qq,m,n]) * sin((branch_angper[qq,m,n])))**2 
  + (-(branch_Bor[1,qq,m,n]) + (branch_admi[qq,m,n]) * cos((branch_angper[qq,m,n])))**2 ) # Shunt
  );
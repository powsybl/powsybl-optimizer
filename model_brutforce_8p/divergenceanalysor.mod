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

###############################################################################
#               Substations (ampl_network_substations.txt)                    #
###############################################################################
#ampl_network_substations.txt

# [variant, substation]
# The 1st column "variant" may also be used to define time step, in case this
# PowSyBl format is used for multi-timestep OPF. This is why the letter for
# the variant is mostly 't' and not 'v' (in power system, v is for voltage).

set SUBSTATIONS dimen 2; #See this in error message? Use "ampl reactiveopf.run" instead of .mod 
param substation_horizon     {SUBSTATIONS} symbolic;
param substation_fodist      {SUBSTATIONS};
param substation_Vnomi       {SUBSTATIONS}; # kV
param substation_Vmin        {SUBSTATIONS}; # pu
param substation_Vmax        {SUBSTATIONS}; # pu
param substation_fault       {SUBSTATIONS};
param substation_curative    {SUBSTATIONS};
param substation_country     {SUBSTATIONS} symbolic;
param substation_id          {SUBSTATIONS} symbolic;
param substation_description {SUBSTATIONS} symbolic;

# Consistency checks

# Check only time stamp 1 is in files
set TIME := setof{(t,s) in SUBSTATIONS}t;
check card(TIME) == 1;
check 1 in TIME;
check card({(t,s) in SUBSTATIONS: substation_Vnomi[t,s] >= epsilon_nominal_voltage}) > 1;

# Voltage bounds
check{(t,s) in SUBSTATIONS: substation_Vmin[t,s] >= epsilon_min_voltage and substation_Vmax[t,s] >= epsilon_min_voltage}:
  substation_Vmin[t,s] < substation_Vmax[t,s];
# Parameter below will be used to force voltage to be in interval [epsilon;2-epsilon]. Typical value is 0.5 although academics would use 0.9 or 0.95
check epsilon_min_voltage > 0 and epsilon_min_voltage < 1;
# Bounds below will be used for substations without bounds or with bad bounds (eg 0.01pu or 20pu are bad values)
param minimal_voltage_lower_bound := 
  if card({(t,s) in SUBSTATIONS: substation_Vmin[t,s] > 0}) > 0
  then max(  epsilon_min_voltage,min{(t,s) in SUBSTATIONS: substation_Vmin[t,s] > 0} substation_Vmin[t,s])
  else epsilon_min_voltage;
param maximal_voltage_upper_bound := 
  if card({(t,s) in SUBSTATIONS: substation_Vmin[t,s] > 0}) > 0
  then min(2-epsilon_min_voltage,max{(t,s) in SUBSTATIONS: substation_Vmax[t,s] > 0} substation_Vmax[t,s])
  else 2-epsilon_min_voltage;
check minimal_voltage_lower_bound > 0;
check maximal_voltage_upper_bound > minimal_voltage_lower_bound;

# Voltage bounds that will really been used
# Negative value for substation_Vmin or substation_Vmac means that the value is undefined
# In that case, minimal_voltage_lower_bound or maximal_voltage_upper_bound is used instead
param voltage_lower_bound{(t,s) in SUBSTATIONS} := 
  max(minimal_voltage_lower_bound,substation_Vmin[t,s]);
param voltage_upper_bound{(t,s) in SUBSTATIONS} :=
  if substation_Vmax[t,s] <= voltage_lower_bound[t,s]
  then maximal_voltage_upper_bound
  else min(maximal_voltage_upper_bound,substation_Vmax[t,s]);
check {(t,s) in SUBSTATIONS}: voltage_lower_bound[t,s] < voltage_upper_bound[t,s];



###############################################################################
#                       Buses (ampl_network_buses.txt)                        #
###############################################################################

set BUS dimen 2 ; # [variant, bus]
param bus_substation{BUS} integer;
param bus_CC        {BUS} integer; # num of connex component. Computation only in CC number 0 (=main connex component)
param bus_V0        {BUS};
param bus_angl0     {BUS};
param bus_injA      {BUS};
param bus_injR      {BUS};
param bus_fault     {BUS};
param bus_curative  {BUS};
param bus_id        {BUS} symbolic;

# Consistency checks
check{(t,n) in BUS}: t in TIME;
check{(t,n) in BUS}: n >= -1;
check{(t,n) in BUS}: (t,bus_substation[t,n]) in SUBSTATIONS;

param null_phase_bus;



###############################################################################
#              Generating units (ampl_network_generators.txt)                 #  
###############################################################################

set UNIT dimen 3; # [variant, unit, bus]
param unit_potentialbus{UNIT} integer;
param unit_substation  {UNIT} integer;
param unit_Pmin    {UNIT};
param unit_Pmax    {UNIT};
param unit_qP      {UNIT};
param unit_qp0     {UNIT};
param unit_qp      {UNIT};
param unit_QP      {UNIT};
param unit_Qp0     {UNIT};
param unit_Qp      {UNIT};
param unit_vregul  {UNIT} symbolic; # Does unit do voltage regulation, or PQ bus? # if true, is PV
param unit_Vc      {UNIT}; # Voltage set point (in case of voltage regulation)
param unit_Pc      {UNIT}; # Active  power set point
param unit_Qc      {UNIT}; # Rective power set point (in case no voltage regulation)
param unit_fault   {UNIT};
param unit_curative{UNIT};
param unit_id      {UNIT} symbolic;
param unit_name    {UNIT} symbolic; # description
param unit_P0      {UNIT}; # Initial value of P (if relevant)
param unit_Q0      {UNIT}; # Initial value of Q (if relevant)

# Consistency

check {(t,g,n) in UNIT}: t in TIME;
check {(t,g,n) in UNIT}: (t,n) in BUS or n==-1;
check {(t,g,n) in UNIT}: (t,unit_substation[t,g,n]) in SUBSTATIONS;
check {(t,g,n) in UNIT}: unit_Pmax[t,g,n] >= -Pnull;
check {(t,g,n) in UNIT}: unit_Pmax[t,g,n] >= unit_Pmin[t,g,n];
# Checks below are useless since values will be corrected for units in UNITON
#check {(t,g,n) in UNIT}: unit_Qp[t,g,n] >= unit_qp[t,g,n];
#check {(t,g,n) in UNIT}: unit_QP[t,g,n] >= unit_qP[t,g,n] ;

# Global inital losses_ratio: value of (P-C-H)/(C+H) in data files
# Value is 0 if no losses
# Value 0.02 means % of losses 
param global_initial_losses_ratio default 0.02; # Typical value value for transmission



###############################################################################
#                       Loads (ampl_network_loads.txt)                        #
###############################################################################

set LOAD dimen 3; # [variant, load, bus]
param load_substation{LOAD} integer;
param load_PFix      {LOAD};
param load_QFix      {LOAD};
param load_fault     {LOAD};
param load_curative  {LOAD};
param load_id        {LOAD} symbolic;
param load_name      {LOAD} symbolic;
param load_p         {LOAD};
param load_q         {LOAD};

# Consistency checks

check {(t,c,n) in LOAD}: t in TIME;
check {(t,c,n) in LOAD}: (t,n) in BUS or n==-1;
check {(t,c,n) in LOAD}: (t,load_substation[t,c,n]) in SUBSTATIONS;



###############################################################################
#                       Shunts (ampl_network_shunts.txt)                      #
###############################################################################

# TODO : raccorder les shunts
set SHUNT dimen 3; # [variant, shunt, bus]
param shunt_possiblebus   {SHUNT} integer;
param shunt_substation    {SHUNT} integer;
param shunt_valmin        {SHUNT}; # Susceptance B in p.u.: compute B*100*V^2 to get MVAr
param shunt_valmax        {SHUNT}; # Susceptance B in p.u.: compute B*100*V^2 to get MVAr
param shunt_interPoints   {SHUNT}; # Intermediate points: if there are 0 interPoint, it means that either min or max are possible
param shunt_valnom        {SHUNT}; # Susceptance B in p.u.: compute B*100*V^2 to get MVAr. If value >= 0, this means reactive power generation
param shunt_fault         {SHUNT};
param shunt_curative      {SHUNT};
param shunt_id            {SHUNT} symbolic;
param shunt_nom           {SHUNT} symbolic;
param shunt_P0            {SHUNT};
param shunt_Q0            {SHUNT}; # Reactive power load: valnom >= 0 means Q0 <= 0
param shunt_sections_count{SHUNT} integer;

# Consistency checks
check {(t,s,n)  in SHUNT}: (t,n) in BUS or n==-1;
check {(t,s,n)  in SHUNT}: n==-1 or shunt_possiblebus[t,s,n]==n;
check {(t,s,-1) in SHUNT}: (t,shunt_possiblebus[t,s,-1]) in BUS or shunt_possiblebus[t,s,-1]==-1;
check {(t,s,n)  in SHUNT}: (t,shunt_substation[t,s,n]) in SUBSTATIONS;
check {(t,s,n)  in SHUNT}: shunt_valmin[1,s,n] < shunt_valmax[1,s,n];

# Case of a reactance : check valmin < 0 and valmax=0
check {(t,s,n) in SHUNT}: shunt_valmin[1,s,n] <= 0;
check {(t,s,n) in SHUNT : shunt_valmin[1,s,n] <= -Pnull / base100MVA}: shunt_valmax[1,s,n] <=  Pnull / base100MVA;
# Case of a condo : check valmin = 0 and valmax>0
check {(t,s,n) in SHUNT}: shunt_valmax[1,s,n] >= 0;
check {(t,s,n) in SHUNT : shunt_valmax[1,s,n] >=  Pnull / base100MVA}: shunt_valmin[1,s,n] >= -Pnull / base100MVA;



###############################################################################
#     Static Var Compensator (ampl_network_static_var_compensators.txt)       #
###############################################################################

set SVC dimen 3; # [variant, svc, bus]
param svc_possiblebus {SVC} integer;
param svc_substation  {SVC} integer;
param svc_bmin        {SVC}; # Susceptance B in p.u.: compute B*100*V^2 to get MVAr
param svc_bmax        {SVC}; # Susceptance B in p.u.: compute B*100*V^2 to get MVAr
param svc_vregul      {SVC} symbolic; # true if SVC is in voltage regulation mode
param svc_targetV     {SVC}; # Voltage target for voltage regulation mode
param svc_targetQ     {SVC};
param svc_fault       {SVC};
param svc_curative    {SVC};
param svc_id          {SVC} symbolic;
param svc_description {SVC} symbolic;
param svc_P0          {SVC};
param svc_Q0          {SVC}; # Fixed value to be used if SVC is not in voltage regulation mode. If value >= 0, this means reactive power generation

# Consistency checks
check {(t,svc,n) in SVC}: (t,n) in BUS or n==-1;
check {(t,svc,n) in SVC}: (t,svc_substation[t,svc,n]) in SUBSTATIONS;



###############################################################################
#                   Batteries (ampl_network_batteries.txt)                    #
###############################################################################

set BATTERY dimen 3; # [variant, battery, bus]
param battery_possiblebus{BATTERY} integer;
param battery_substation {BATTERY} integer;
param battery_p0      {BATTERY}; # current P of the battery, P0 <= 0 if batterie is charging
param battery_q0      {BATTERY};
param battery_Pmin    {BATTERY};
param battery_Pmax    {BATTERY};
param battery_qP      {BATTERY};
param battery_qp0     {BATTERY};
param battery_qp      {BATTERY};
param battery_QP      {BATTERY};
param battery_Qp0     {BATTERY};
param battery_Qp      {BATTERY};
param battery_fault   {BATTERY};
param battery_curative{BATTERY};
param battery_id      {BATTERY} symbolic;
param battery_name    {BATTERY} symbolic;
param battery_P0      {BATTERY};
param battery_Q0      {BATTERY};

# Consistency checks
check {(t,b,n) in BATTERY} : (t,n) in BUS union {(t,-1)};
check {(t,b,n) in BATTERY} : (t,battery_substation[t,b,n]) in SUBSTATIONS;
check {(t,b,n) in BATTERY: (t,n) in BUS} : battery_substation[t,b,n] == bus_substation[t,n];
check {(t,b,n) in BATTERY} : battery_Pmin[t,b,n] <= battery_Pmax[t,b,n] ;


# Tap seems to be transformers

###############################################################################
#                   Tables of taps (ampl_network_tct.txt)                     #
###############################################################################

# Data in these tables are used for both ratio tap changers and phase taps changers

set TAPS dimen 3;  # [variant, num, tap]
param tap_ratio    {TAPS};
param tap_x        {TAPS};
param tap_angle    {TAPS};
param tap_fault    {TAPS};
param tap_curative {TAPS};

# Created data
set TAPTABLES := setof {(t,tab,tap) in TAPS} tab;

# Consistency checks
check {(t,tab,tap) in TAPS}: tab > 0 && tap >= 0;



###############################################################################
#                 Ratio tap changers (ampl_network_rtc.txt)                   #
###############################################################################

param regl_V_missing := -99999.0;
set REGL dimen 2;  # [variant, num]
param regl_tap0     {REGL} integer;
param regl_table    {REGL} integer;
param regl_onLoad   {REGL} symbolic;
param regl_V        {REGL} ;
param regl_fault    {REGL};
param regl_curative {REGL};
param regl_id       {REGL} symbolic;

# Consistency checks
check {(t,r) in REGL}: regl_table[t,r] in TAPTABLES;
check {(t,r) in REGL}: (t,regl_table[t,r], regl_tap0[t,r]) in TAPS;

param regl_ratio_min{(t,r) in REGL} := min{(t,regl_table[t,r],tap) in TAPS} tap_ratio[t,regl_table[t,r],tap];
param regl_ratio_max{(t,r) in REGL} := max{(t,regl_table[t,r],tap) in TAPS} tap_ratio[t,regl_table[t,r],tap];



###############################################################################
#                 Phase tap changers (ampl_network_ptc.txt)                   #
###############################################################################

set DEPH dimen 2; # [variant, num]
param deph_tap0     {DEPH} integer;
param deph_table    {DEPH} integer;
param deph_fault    {DEPH};
param deph_curative {DEPH};
param deph_id       {DEPH} symbolic;

# Consistency checks
check {(t,d) in DEPH}: deph_table[t,d] in TAPTABLES;
check {(t,d) in DEPH}: (t,deph_table[t,d], deph_tap0[t,d]) in TAPS;



###############################################################################
#                     Branches (ampl_network_branches.txt)                    #
###############################################################################

set BRANCH dimen 4; # [variant, branch, bus1, bus2]
param branch_subor        {BRANCH} integer;
param branch_subex        {BRANCH} integer;
param branch_3wt          {BRANCH};
param branch_R            {BRANCH};
param branch_X            {BRANCH};
param branch_Gor          {BRANCH};
param branch_Gex          {BRANCH};
param branch_Bor          {BRANCH};
param branch_Bex          {BRANCH};
param branch_cstratio     {BRANCH}; # fixed ratio
param branch_ptrRegl      {BRANCH} integer; # Number of ratio tap changer
param branch_ptrDeph      {BRANCH} integer; # Number of phase tap changer
param branch_Por          {BRANCH};
param branch_Pex          {BRANCH};
param branch_Qor          {BRANCH};
param branch_Qex          {BRANCH};
param branch_patl1        {BRANCH};
param branch_patl2        {BRANCH};
param branch_merged       {BRANCH} symbolic;
param branch_fault        {BRANCH};
param branch_curative     {BRANCH};
param branch_id           {BRANCH} symbolic;
param branch_name         {BRANCH} symbolic;


# Consistency checks
check {(t,qq,m,n) in BRANCH}: t in TIME;
check {(t,qq,m,n) in BRANCH}:
     ( (t,m) in BUS or m==-1 )
  && ( (t,n) in BUS or n==-1 )
  && ( m != n || m == -1 ) # no problem if m==n==-1
  && qq > 0
  && (t,branch_subor[t,qq,m,n]) in SUBSTATIONS
  && (t,branch_subex[t,qq,m,n]) in SUBSTATIONS;
check {(t,qq,m,n) in BRANCH}: (t,branch_ptrRegl[t,qq,m,n]) in REGL union {(1,-1)};
check {(t,qq,m,n) in BRANCH}: (t,branch_ptrDeph[t,qq,m,n]) in DEPH union {(1,-1)};

# Admittances
#param branch_G {(t,qq,m,n) in BRANCH} = +branch_R[t,qq,m,n]/(branch_R[t,qq,m,n]^2+branch_X[t,qq,m,n]^2);
#param branch_B {(t,qq,m,n) in BRANCH} = -branch_X[t,qq,m,n]/(branch_R[t,qq,m,n]^2+branch_X[t,qq,m,n]^2);



###############################################################################
#   VSC converter station data (ampl_network_vsc_converter_stations.txt)      #
###############################################################################

set VSCCONV dimen 3; # [variant, num, bus]
param vscconv_possiblebus {VSCCONV} integer;
param vscconv_substation  {VSCCONV} integer;
param vscconv_Pmin        {VSCCONV};
param vscconv_Pmax        {VSCCONV};
param vscconv_qP          {VSCCONV};
param vscconv_qp0         {VSCCONV};
param vscconv_qp          {VSCCONV};
param vscconv_QP          {VSCCONV};
param vscconv_Qp0         {VSCCONV};
param vscconv_Qp          {VSCCONV};
param vscconv_vregul      {VSCCONV} symbolic;
param vscconv_targetV     {VSCCONV};
param vscconv_targetQ     {VSCCONV};
param vscconv_lossFactor  {VSCCONV};
param vscconv_fault       {VSCCONV};
param vscconv_curative    {VSCCONV};
param vscconv_id          {VSCCONV} symbolic;
param vscconv_description {VSCCONV} symbolic;
param vscconv_P0          {VSCCONV}; # P0 >= 0 means active power going from AC grid to DC line (homogeneous to a load)
param vscconv_Q0          {VSCCONV};

# Consistency checks
check {(t,cs,n) in VSCCONV}: (t,n)  in BUS union {(1,-1)};
check {(t,cs,n) in VSCCONV}: (t,vscconv_substation[t,cs,n]) in SUBSTATIONS;
check {(t,cs,n) in VSCCONV}: vscconv_Pmin[t,cs,n] <= vscconv_Pmax[t,cs,n];
check {(t,cs,n) in VSCCONV}: vscconv_qp[t,cs,n]   <= vscconv_Qp[t,cs,n];
check {(t,cs,n) in VSCCONV}: vscconv_qp0[t,cs,n]  <= vscconv_Qp0[t,cs,n];
check {(t,cs,n) in VSCCONV}: vscconv_qP[t,cs,n]   <= vscconv_QP[t,cs,n];



###############################################################################
#     LCC converter station data (ampl_network_lcc_converter_stations.txt)    #
###############################################################################

#"variant" "num" "bus" "con. bus" "substation" "lossFactor (%PDC)" "powerFactor" "fault" "curative" "id" "description" "P (MW)" "Q (MVar)"

set LCCCONV dimen 3; # [variant, num, bus]
param lccconv_possiblebus {LCCCONV} integer;
param lccconv_substation  {LCCCONV} integer;
param lccconv_loss_factor {LCCCONV};
param lccconv_power_factor{LCCCONV};
param lccconv_fault       {LCCCONV};
param lccconv_curative    {LCCCONV};
param lccconv_id          {LCCCONV} symbolic;
param lccconv_description {LCCCONV} symbolic;
param lccconv_P0          {LCCCONV};
param lccconv_Q0          {LCCCONV};



###############################################################################
#                          HVDC (ampl_network_hvdc.txt)                       #
###############################################################################

set HVDC dimen 2; # [variant, num]
param hvdc_type           {HVDC} integer; # 1->vscConverterStation, 2->lccConverterStation
param hvdc_conv1          {HVDC} integer;
param hvdc_conv2          {HVDC} integer;
param hvdc_r              {HVDC};
param hvdc_Vnom           {HVDC};
param hvdc_convertersMode {HVDC} symbolic;
param hvdc_targetP        {HVDC};
param hvdc_Pmax           {HVDC};
param hvdc_fault          {HVDC};
param hvdc_curative       {HVDC};
param hvdc_id             {HVDC} symbolic;
param hvdc_description    {HVDC} symbolic;

# Consistency checks
check {(t,h) in HVDC}: hvdc_type[t,h] == 1 or hvdc_type[t,h] == 2;  
check {(t,h) in HVDC}: hvdc_conv1[t,h] != hvdc_conv2[t,h];
check {(t,h) in HVDC:  hvdc_type[t,h] == 1}: hvdc_conv1[t,h] in setof{(t,n,bus) in VSCCONV}n;
check {(t,h) in HVDC:  hvdc_type[t,h] == 1}: hvdc_conv2[t,h] in setof{(t,n,bus) in VSCCONV}n;
check {(t,h) in HVDC:  hvdc_type[t,h] == 2}: hvdc_conv1[t,h] in setof{(t,n,bus) in LCCCONV}n;
check {(t,h) in HVDC:  hvdc_type[t,h] == 2}: hvdc_conv2[t,h] in setof{(t,n,bus) in LCCCONV}n;
check {(t,h) in HVDC}: hvdc_Vnom[t,h] >= epsilon_nominal_voltage;
check {(t,h) in HVDC}: hvdc_convertersMode[t,h] == "SIDE_1_RECTIFIER_SIDE_2_INVERTER" or hvdc_convertersMode[t,h] == "SIDE_1_INVERTER_SIDE_2_RECTIFIER";
check {(t,h) in HVDC}: hvdc_targetP[t,h] >= 0.0;
check {(t,h) in HVDC}: hvdc_targetP[t,h] <= hvdc_Pmax[t,h];



###############################################################################
#   Additional sets for equipments which are really working (in the main CC)  #
###############################################################################

#
# Elements in main connex component
#
param index_main_connex_component = 0;
set BUS2:= setof{(1,n) in BUS: bus_CC[1,n] == index_main_connex_component and n >= 0} n;
check {n in BUS2}: substation_Vnomi[1,bus_substation[1,n]] >= epsilon_nominal_voltage;
set BRANCH2:= setof {(1,qq,m,n) in BRANCH: m in BUS2 and n in BUS2} (qq,m,n);

#
# Elements in the final component (after checking of theta = 0)
#
set BUSCC dimen 1 default {}; # Def in reacriveopf.run
#check {n in BUSCC}: bus_V0[1,n] >= epsilon_min_voltage; # Check buses have valid voltage value
# Un check sur la cardinalite de BUSCC et BUS2 ?
param bus_V0_corrected{n in BUSCC};
param bus_angl0_corrected{n in BUSCC};

set BRANCHCC:= setof {(1,qq,m,n) in BRANCH: m in BUSCC and n in BUSCC} (qq,m,n); 
set LOADCC  := setof {(1,c,n)    in LOAD  : n in BUSCC               } (c,n);
set UNITCC  := setof {(1,g,n)    in UNIT  : n in BUSCC} (g,n);

param eps_PQ := 0.1;
set UNITCC_PQ_1 := setof {(g,n) in UNITCC : unit_vregul[1,g,n] == "false"} (g,n);
set UNITCC_PQ_2 := setof {(g,n) in UNITCC : unit_vregul[1,g,n] == "true"
                                          and  unit_Q0[1,g,n] <= unit_Qp0[1,g,n] + eps_PQ
                                          and  unit_Q0[1,g,n] >= unit_Qp0[1,g,n] - eps_PQ
                                          and  unit_Q0[1,g,n] <= unit_qp0[1,g,n] + eps_PQ
                                          and  unit_Q0[1,g,n] >= unit_qp0[1,g,n] - eps_PQ} (g,n);
set UNITCC_PQ := UNITCC_PQ_1 union UNITCC_PQ_2;
set UNITCC_PV := UNITCC diff UNITCC_PQ; # We include the slack bus in PV nodes

# Buses that are PQ and PV
set BUSCC_PV := setof {(g,n) in UNITCC_PV} n;
param targetV_busPV{n in BUSCC_PV}; # def in divergenceanalysor.run
set BUSCC_PQ := BUSCC diff BUSCC_PV;
# TODO: add checking consistency of BUSCC_PQ, BUSCC_PV, UNITCC_PQ and UNITCC_PV

#
# Shunts, regleurs et dephaseurs
#
set BRANCHZNULL := {(qq,m,n) in BRANCHCC: branch_R[1,qq,m,n]^2+branch_X[1,qq,m,n]^2 <= Znull^2};

# NB : SHUNTCC are the shunts that are fixed. Could be variable but we do not want for now
set SHUNTCC := {(1,s,n) in SHUNT: n in BUSCC or shunt_possiblebus[1,s,n] in BUSCC}; # We want to be able to reconnect shunts
set BRANCHCC_REGL := {(qq,m,n) in BRANCHCC diff BRANCHZNULL: branch_ptrRegl[1,qq,m,n] != -1 };
set BRANCHCC_DEPH := {(qq,m,n) in BRANCHCC diff BRANCHZNULL: branch_ptrDeph[1,qq,m,n] != -1 };
set SVCCC   := {(1,svc,n) in SVC: n in BUSCC};

#
# VSC converter stations
#
check {(t,v,n) in VSCCONV}: n in BUSCC and abs(vscconv_P0[t,v,n]  ) <= PQmax
  and abs(vscconv_Pmin[t,v,n]) <= PQmax and abs(vscconv_Pmax[t,v,n]) <= PQmax
  and vscconv_P0[t,v,n] >= vscconv_Pmin[t,v,n] and vscconv_P0[t,v,n] <= vscconv_Pmax[t,v,n];
set VSCCONVON := setof{(t,v,n) in VSCCONV} (v,n);

#
# LCC converter stations
#
check {(t,l,n) in LCCCONV}: n in BUSCC and abs(lccconv_P0[1,l,n]) <= PQmax 
                            and abs(lccconv_Q0[1,l,n]) <= PQmax;
set LCCCONVON := setof{(t,l,n) in LCCCONV} (l,n);


###############################################################################
#                         Corrected values for reactances                     #
###############################################################################

# Here, what does X_mod mean ?
# If in BRANCHZNULL, then set X to ZNULL
param branch_X_mod{(qq,m,n) in BRANCHCC} :=
  if (qq,m,n) in BRANCHZNULL then Znull
  else branch_X[1,qq,m,n];
check {(qq,m,n) in BRANCHCC}: abs(branch_X_mod[qq,m,n]) > 0;

###############################################################################
#           Transformers and Phase shifter transformers parameters            #
###############################################################################

# Variable reactance, depanding on tap
param branch_Xdeph{(qq,m,n) in BRANCHCC_DEPH} = tap_x[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]];

# Resistance variable selon la prise
# Comme on ne dispose pas de valeurs variables de R dans les tables des lois des TDs, on fait varier R proportionellement a X
param branch_Rdeph{(qq,m,n) in BRANCHCC_DEPH} =
  if abs(branch_X_mod[qq,m,n]) >= Znull 
  then branch_R[1,qq,m,n]*branch_Xdeph[qq,m,n]/branch_X_mod[qq,m,n]
  else branch_R[1,qq,m,n]
  ;

param branch_angper{(qq,m,n) in BRANCHCC} =
  if (qq,m,n) in BRANCHCC_DEPH
  then atan2(branch_Rdeph[qq,m,n], branch_Xdeph[qq,m,n])
  else atan2(branch_R[1,qq,m,n]  , branch_X_mod[qq,m,n]);

param branch_admi {(qq,m,n) in BRANCHCC} = 
  if (qq,m,n) in BRANCHCC_DEPH
  then 1./sqrt(branch_Rdeph[qq,m,n]^2 + branch_Xdeph[qq,m,n]^2 )
  else 1./sqrt(branch_R[1,qq,m,n]^2   + branch_X_mod[qq,m,n]^2   );

param branch_Ror {(qq,m,n) in BRANCHCC} =
    ( if ((qq,m,n) in BRANCHCC_REGL)
      then tap_ratio[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]]
      else 1.0
    )
  * ( if ((qq,m,n) in BRANCHCC_DEPH)
      then tap_ratio[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]]
      else 1.0
    )
  * (branch_cstratio[1,qq,m,n]);
param branch_Rex {(q,m,n) in BRANCHCC} = 1; # In IIDM, everything is in bus1 so ratio at bus2 is always 1

param branch_dephor {(qq,m,n) in BRANCHCC} =
  if ((qq,m,n) in BRANCHCC_DEPH)
  then tap_angle [1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]]
  else 0;
param branch_dephex {(qq,m,n) in BRANCHCC} = 0; # In IIDM, everything is in bus1 so dephase at bus2 is always 0 -->




###############################################################################
#                                                                             #
#     List of all optimization problems which wil be solved successively      #
#                                                                             #
###############################################################################
# Variables are defined without reference to a particular optimization problem
# Constraints are to be defined with
set PROBLEM_CCOMP default {1};
set PROBLEM_ACDA default { };

###############################################################################
#                                                                             #
#             Variables and contraints for connexity computation              #
#                                                                             #
###############################################################################
# Even if set BUS2 is only with busses in connex component (CC) number '0', for an OPF we
# have to do connexity computation using only AC branches, ie in only one single synchronous area.
# Indeed, HVDC branches may connect 2 synchronous areas; one might consider in that case that the grid is connex
# In our case we have to consider only buses which are connected to reference bus with AC branches
var teta_ccomputation{BUS2} >=0, <=1;
subject to ctr_null_phase_bus_cccomputation{PROBLEM_CCOMP}: teta_ccomputation[null_phase_bus] = 0;
subject to ctr_flow_cccomputation{PROBLEM_CCOMP, (qq,m,n) in BRANCH2}: teta_ccomputation[m]-teta_ccomputation[n]=0;
maximize cccomputation_objective: sum{n in BUS2} teta_ccomputation[n];
# All busses AC-connected to null_phase_bus will have '0' as optimal value, other will have '1'


###############################################################################
#                                                                             #
#       Variables and contraints for AC Divergence Analysis                   #
#    We use the complexe notation of voltage : V*exp(i*teta). (with i**2=-1)  #
#                                                                             #
###############################################################################

#
# Phase of voltage and constraint of slack bus
#

var teta{n in BUSCC};
var V{n in BUSCC}; 

# Slack variable for voltage of PV-nodes
var z2{n in BUSCC_PV};

# Constraint for slack bus and PV-nodes
subject to ctr_null_phase_bus{PROBLEM_ACDA}: teta[null_phase_bus] = 0; # Phase of slack bus = 0
subject to ctr_voltage_PV_buses{PROBLEM_ACDA,k in BUSCC_PV}: V[k] - targetV_busPV[k] + z2[k] = 0;

# Slack variables for elements on the branches
var y1{(qq,m,n) in BRANCHCC};
var y2{(qq,m,n) in BRANCHCC};
var y3{(qq,m,n) in BRANCHCC};
var y4{(qq,m,n) in BRANCHCC};
var y5{(qq,m,n) in BRANCHCC};
var y6{(qq,m,n) in BRANCHCC};
var y7{(qq,m,n) in BRANCHCC};
var y8{(qq,m,n) in BRANCHCC};

# TODO : Ameliorer les bornes des y_i
param max_branch_Ror := (max {(qq,m,n) in BRANCHCC} branch_Ror[qq,m,n]);
param max_branch_admi := (max {(qq,m,n) in BRANCHCC} branch_admi[qq,m,n]);
param max_branch_dephor := (max {(qq,m,n) in BRANCHCC} branch_dephor[qq,m,n]);
param max_branch_angper := (max {(qq,m,n) in BRANCHCC} branch_angper[qq,m,n]);
param max_branch_Gor := (max {(qq,m,n) in BRANCHCC} branch_Gor[1,qq,m,n]);
param max_branch_Bor := (max {(qq,m,n) in BRANCHCC} branch_Bor[1,qq,m,n]);
param max_branch_Gex := (max {(qq,m,n) in BRANCHCC} branch_Gex[1,qq,m,n]);
param max_branch_Bex := (max {(qq,m,n) in BRANCHCC} branch_Bex[1,qq,m,n]);

# Bornes for y1
subject to ctr_y1_min{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y1[qq,m,n] + branch_Ror[qq,m,n] >= 0;
subject to ctr_y1_max{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y1[qq,m,n] + branch_Ror[qq,m,n] <= max_branch_Ror;

# Bornes for y2
subject to ctr_y2_min{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y2[qq,m,n] + branch_admi[qq,m,n] >= 0;
subject to ctr_y2_max{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y2[qq,m,n] + branch_admi[qq,m,n] <= max_branch_admi;

# Bornes for y3
subject to ctr_y3_min{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y3[qq,m,n] + branch_dephor[qq,m,n] >= 0;
subject to ctr_y3_max{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y3[qq,m,n] + branch_dephor[qq,m,n] <= max_branch_dephor;

# Bornes for y4
subject to ctr_y4_min{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y4[qq,m,n] + branch_angper[qq,m,n] >= 0;
subject to ctr_y4_max{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y4[qq,m,n] + branch_angper[qq,m,n] <= max_branch_angper;

# Bornes for y4
subject to ctr_y5_min{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y5[qq,m,n] + branch_Gor[1,qq,m,n] >= 0;
subject to ctr_y5_max{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y5[qq,m,n] + branch_Gor[1,qq,m,n] <= max_branch_Gor;

# Bornes for y4
subject to ctr_y6_min{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y6[qq,m,n] + branch_Bor[1,qq,m,n] >= 0;
subject to ctr_y6_max{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y6[qq,m,n] + branch_Bor[1,qq,m,n] <= max_branch_Bor;

# Bornes for y4
subject to ctr_y7_min{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y7[qq,m,n] + branch_Gex[1,qq,m,n] >= 0;
subject to ctr_y7_max{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y7[qq,m,n] + branch_Gex[1,qq,m,n] <= max_branch_Gex;

# Bornes for y4
subject to ctr_y8_min{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y8[qq,m,n] + branch_Bex[1,qq,m,n] >= 0;
subject to ctr_y8_max{PROBLEM_ACDA, (qq,m,n) in BRANCHCC}: y8[qq,m,n] + branch_Bex[1,qq,m,n] <= max_branch_Bex;

#
# Active and reactive powers
#
param P{(g,n) in UNITCC} = unit_Pc[1,g,n];
param Q{(g,n) in UNITCC} = unit_Qc[1,g,n];
var svc_b_var{(svc,n) in SVCCC} >= svc_bmin[1,svc,n], <= svc_bmax[1,svc,n];
# param vscconv_qvar{(v,n) in VSCCONVON} := if abs(vscconv_targetQ[1,v,n]) < PQmax then vscconv_targetQ[1,v,n] else 0;

#
# Flows in one direction, then the inverse
#
var Red_Tran_Act_Dir{(qq,m,n) in BRANCHCC} =
  (branch_Ror[qq,m,n]+y1[qq,m,n]) * V[n] * (branch_admi[qq,m,n]+y2[qq,m,n]) * sin(teta[m]-teta[n]+(branch_dephor[qq,m,n]+y3[qq,m,n])-(branch_angper[qq,m,n]+y4[qq,m,n]))
  + (branch_Ror[qq,m,n]+y1[qq,m,n])**2 * V[m] * ((branch_admi[qq,m,n]+y2[qq,m,n])*sin(branch_angper[qq,m,n]+y4[qq,m,n])+(branch_Gor[1,qq,m,n]+y5[qq,m,n]));

var Red_Tran_Rea_Dir{(qq,m,n) in BRANCHCC} = 
  - (branch_Ror[qq,m,n]+y1[qq,m,n]) * V[n] * (branch_admi[qq,m,n]+y2[qq,m,n]) * cos(teta[m]-teta[n]+(branch_dephor[qq,m,n]+y3[qq,m,n])-(branch_angper[qq,m,n]+y4[qq,m,n]))
  + (branch_Ror[qq,m,n]+y1[qq,m,n])**2 * V[m] * ((branch_admi[qq,m,n]+y2[qq,m,n])*cos(branch_angper[qq,m,n]+y4[qq,m,n])-(branch_Bor[1,qq,m,n]+y6[qq,m,n]));

var Red_Tran_Act_Inv{(qq,m,n) in BRANCHCC} = 
  (branch_Ror[qq,m,n]+y1[qq,m,n]) * V[m] * (branch_admi[qq,m,n]+y2[qq,m,n]) * sin(teta[n]-teta[m]-(branch_dephor[qq,m,n]+y3[qq,m,n])-(branch_angper[qq,m,n]+y4[qq,m,n]))
  + V[n] * ((branch_admi[qq,m,n]+y2[qq,m,n])*sin(branch_angper[qq,m,n]+y4[qq,m,n])+(branch_Gex[1,qq,m,n]+y7[qq,m,n]));

var Red_Tran_Rea_Inv{(qq,m,n) in BRANCHCC} =
  - (branch_Ror[qq,m,n]+y1[qq,m,n]) * V[m] * (branch_admi[qq,m,n]+y2[qq,m,n]) * cos(teta[n]-teta[m]-(branch_dephor[qq,m,n]+y3[qq,m,n])-(branch_angper[qq,m,n]+y4[qq,m,n]))
  + V[n] * ((branch_admi[qq,m,n]+y2[qq,m,n])*cos(branch_angper[qq,m,n]+y4[qq,m,n])-(branch_Bex[1,qq,m,n]+y8[qq,m,n]));


#
# Active Balance for all buses except slack bus
#

subject to ctr_balance_P{PROBLEM_ACDA,k in BUSCC diff {null_phase_bus}}:
  # Flows
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Dir[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Inv[qq,m,k]
  # Generating units
  - sum{(g,k) in UNITCC} P[g,k] # Fixed valus
  # Loads
  + sum{(c,k) in LOADCC} load_PFix[1,c,k]     # Fixed value
  # VSC converters
  + sum{(v,k) in VSCCONVON} vscconv_P0[1,v,k] # Fixed value
  # LCC converters
  + sum{(l,k) in LCCCONVON} lccconv_P0[1,l,k] # Fixed value
  = 0;

#
# Reactive Balance for PQ buses
#

subject to ctr_balance_Q{PROBLEM_ACDA,k in BUSCC_PQ}: 
  # Flows
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Dir[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Inv[qq,m,k]
  # Senerating units
  - sum{(g,k) in UNITCC} Q[g,k]
  # Load
  + sum{(c,k) in LOADCC} load_QFix[1,c,k]
  # Shunts
  - sum{(shunt,k) in SHUNTCC} base100MVA * shunt_valnom[1,shunt,k] * V[k]^2
  # SVC
  - sum{(svc,k) in SVCCC} base100MVA * svc_b_var[svc,k] * V[k]^2
  # VSC converters
  - sum{(v,k) in VSCCONVON} vscconv_Q0[1,v,k] # Fixed values
  # LCC converters
  + sum{(l,k) in LCCCONVON} lccconv_Q0[1,l,k] # Fixed values
  = 0;


###########################################
#             Objective function          #
###########################################

# Parameters for normalization of slack variables
param alpha_default := 1000;
param alpha_z2 = 1; 
param alpha_y1 = if max_branch_Ror != 0 then 1 / max_branch_Ror else alpha_default;
param alpha_y2 = if max_branch_admi != 0 then 1 / max_branch_admi else alpha_default;
param alpha_y3 = if max_branch_dephor != 0 then 1 / max_branch_dephor else alpha_default;
param alpha_y4 = if max_branch_angper != 0 then 1 / max_branch_angper else alpha_default;
param alpha_y5 = if max_branch_Gor != 0 then 1 / max_branch_Gor else alpha_default;
param alpha_y6 = if max_branch_Bor != 0 then 1 / max_branch_Bor else alpha_default;
param alpha_y7 = if max_branch_Gex != 0 then 1 / max_branch_Gex else alpha_default;
param alpha_y8 = if max_branch_Bex != 0 then 1 / max_branch_Bex else alpha_default;

# penal of the gap between initial value and variable
param beta_take_into_account_slackness_voltage = 10;
param beta_take_into_account_slackness_angles = 1;


minimize problem_divergence_analysis:
  0
  + sum{n in BUSCC_PV} (alpha_z2 * z2[n])**2

  # Sum log of y with penal
  #+ sum{(qq,m,n) in BRANCHCC} (alpha_y1 * abs(y1[qq,m,n]))**2
  #+ sum{(qq,m,n) in BRANCHCC} (alpha_y2 * abs(y2[qq,m,n]))**2
  #+ sum{(qq,m,n) in BRANCHCC} (alpha_y3 * abs(y3[qq,m,n]))**2
  #+ sum{(qq,m,n) in BRANCHCC} (alpha_y4 * abs(y4[qq,m,n]))**2
  #+ sum{(qq,m,n) in BRANCHCC} (alpha_y5 * abs(y5[qq,m,n]))**2
  #+ sum{(qq,m,n) in BRANCHCC} (alpha_y6 * abs(y6[qq,m,n]))**2
  #+ sum{(qq,m,n) in BRANCHCC} (alpha_y7 * abs(y7[qq,m,n]))**2
  #+ sum{(qq,m,n) in BRANCHCC} (alpha_y8 * abs(y8[qq,m,n]))**2

  # Sum log of y with penal
  + sum{(qq,m,n) in BRANCHCC} (log(1 + alpha_y1 * abs(y1[qq,m,n])))**2
  + sum{(qq,m,n) in BRANCHCC} (log(1 + alpha_y2 * abs(y2[qq,m,n])))**2
  + sum{(qq,m,n) in BRANCHCC} (log(1 + alpha_y3 * abs(y3[qq,m,n])))**2
  + sum{(qq,m,n) in BRANCHCC} (log(1 + alpha_y4 * abs(y4[qq,m,n])))**2
  + sum{(qq,m,n) in BRANCHCC} (log(1 + alpha_y5 * abs(y5[qq,m,n])))**2
  + sum{(qq,m,n) in BRANCHCC} (log(1 + alpha_y6 * abs(y6[qq,m,n])))**2
  + sum{(qq,m,n) in BRANCHCC} (log(1 + alpha_y7 * abs(y7[qq,m,n])))**2
  + sum{(qq,m,n) in BRANCHCC} (log(1 + alpha_y8 * abs(y8[qq,m,n])))**2

  # Sum log of y without penal
  #+ sum{(qq,m,n) in BRANCHCC} (log(1 + abs(y1[qq,m,n])))**2
  #+ sum{(qq,m,n) in BRANCHCC} (log(1 + abs(y2[qq,m,n])))**2
  #+ sum{(qq,m,n) in BRANCHCC} (log(1 + abs(y3[qq,m,n])))**2
  #+ sum{(qq,m,n) in BRANCHCC} (log(1 + abs(y4[qq,m,n])))**2
  #+ sum{(qq,m,n) in BRANCHCC} (log(1 + abs(y5[qq,m,n])))**2
  #+ sum{(qq,m,n) in BRANCHCC} (log(1 + abs(y6[qq,m,n])))**2
  #+ sum{(qq,m,n) in BRANCHCC} (log(1 + abs(y7[qq,m,n])))**2
  #+ sum{(qq,m,n) in BRANCHCC} (log(1 + abs(y8[qq,m,n])))**2

  # slack between initial value and final value
  + beta_take_into_account_slackness_voltage * sum{n in BUSCC} (V[n] - bus_V0_corrected[n]) ** 2 #~0.5
  #+ sum{n in BUSCC} (log(abs(V[n]))) ** 2 #~0.5
  + beta_take_into_account_slackness_angles * sum{n in BUSCC} (teta[n] - bus_angl0_corrected[n]) ** 2 #~2pi
  ;
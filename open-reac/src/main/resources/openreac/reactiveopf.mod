###############################################################################
#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
###############################################################################

###############################################################################
# Reactive OPF
# Author :  Jean Maeght 2022 2023
###############################################################################



###############################################################################
# Substations
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
# Buses
###############################################################################
# ampl_network_buses.txt
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
# Generating units
###############################################################################
# ampl_network_generators.txt

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
param unit_vregul  {UNIT} symbolic; # Does unit do voltage regulation, or PQ bus?
param unit_Vc      {UNIT}; # Voltage set point (in case of voltage regulation)
param unit_Pc      {UNIT}; # Active  power set point
param unit_Qc      {UNIT}; # Rective power set point (in case no voltage regulation)
param unit_fault   {UNIT};
param unit_curative{UNIT};
param unit_id      {UNIT} symbolic;
param unit_name    {UNIT} symbolic; # description
param unit_P0      {UNIT}; # Initial value of P (if relevant)
param unit_Q0      {UNIT}; # Initial value of Q (if relevant)

#
# Consistency
#
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
# Loads
###############################################################################
# ampl_network_loads.txt

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
# Shunts
###############################################################################
# ampl_network_shunts.txt

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
# Static Var Compensator
###############################################################################
# ampl_network_static_var_compensators.txt

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
# Batteries
###############################################################################
# ampl_network_batteries.txt
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



###############################################################################
# Tables of taps
###############################################################################
# ampl_network_tct.txt
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
# Ratio tap changers
###############################################################################
# ampl_network_rtc.txt

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
# Phase tap changers
###############################################################################
# ampl_network_ptc.txt

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
# Branches
###############################################################################
# ampl_network_branches.txt

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
# VSC converter station data
###############################################################################
# ampl_network_vsc_converter_stations.txt

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
# LCC converter station data
###############################################################################
# ampl_network_lcc_converter_stations.txt
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
#  HVDC
###############################################################################
# ampl_network_hvdc.txt

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
# Controls for shunts decision
###############################################################################
# param_shunts.txt
# All shunts are considered fixed to their value in ampl_network_shunts.txt (set and parameters based on SHUNT above)
# Only shunts listed here will be changed by this reactive opf
set PARAM_SHUNT  dimen 3 default {}; # [variant, shunt, bus]
param param_shunt_id{PARAM_SHUNT} symbolic;
check {(t,s,n) in PARAM_SHUNT inter SHUNT}: shunt_id[t,s,n] == param_shunt_id[t,s,n];



###############################################################################
# Controls for reactive power of generating units
###############################################################################
# param_generators_reactive.txt
# All units are considered with variable Q, within bounds.
# Only units listed in this file will be considered with fixed reactive power value
#"variant" "num" "bus" "id"
set PARAM_UNIT_FIXQ  dimen 3 default {}; # [variant, num, bus]
param param_unit_fixq_id{PARAM_UNIT_FIXQ} symbolic;
check {(t,g,n) in PARAM_UNIT_FIXQ inter UNIT}: unit_id[t,g,n] == param_unit_fixq_id[t,g,n];



###############################################################################
# Controls for transformers
###############################################################################
# param_transformers.txt
# All transformers are considered with fixed ratio
# Only transformers listed in this file will be considered with variable ratio value
#"variant" "num" "bus" "id"
set PARAM_TRANSFORMERS_RATIO_VARIABLE  dimen 4 default {}; # [variant, num, bus1, bus2]
param param_transformers_ratio_variable_id{PARAM_TRANSFORMERS_RATIO_VARIABLE} symbolic;
check {(t,qq,m,n) in PARAM_TRANSFORMERS_RATIO_VARIABLE inter BRANCH}: branch_id[t,qq,m,n] == param_transformers_ratio_variable_id[t,qq,m,n];



###############################################################################
# Additional sets for equipments which are really working
###############################################################################

# Elements in main connex component
set BUS2:= setof {(1,n) in BUS: 
  bus_CC[1,n] == 0 
  and n >= 0 
  and substation_Vnomi[1,bus_substation[1,n]] >= epsilon_nominal_voltage
  } n;
set BRANCH2:= setof {(1,qq,m,n) in BRANCH: m in BUS2 and n in BUS2} (qq,m,n);

set BUSCC dimen 1 default {};
set BRANCHCC:= {(qq,m,n) in BRANCH2: m in BUSCC and n in BUSCC};
set LOADCC  := setof {(1,c,n)    in LOAD  : n in BUSCC               } (c,n);
set UNITCC  := setof {(1,g,n)    in UNIT  : n in BUSCC               } (g,n);

# Busses with valid voltage value
set BUSVV := {n in BUSCC : bus_V0[1,n] >= epsilon_min_voltage}; 

# Warning: units with zero target are considered as out of order
# Units up and generating:
set UNITON := {(g,n) in UNITCC : abs(unit_Pc[1,g,n]) >= Pnull};

# Branches with zero or near zero impedances
# Notice: module of Z is equal to square root of (R^2+X^2)
set BRANCHZNULL := {(qq,m,n) in BRANCHCC: branch_R[1,qq,m,n]^2+branch_X[1,qq,m,n]^2 <= Znull^2};

# Reactive
set SHUNTCC := {(1,s,n) in SHUNT: n in BUSCC or shunt_possiblebus[1,s,n] in BUSCC}; # We want to be able to reconnect shunts
set BRANCHCC_REGL := {(qq,m,n) in BRANCHCC diff BRANCHZNULL: branch_ptrRegl[1,qq,m,n] != -1 };
set BRANCHCC_DEPH := {(qq,m,n) in BRANCHCC diff BRANCHZNULL: branch_ptrDeph[1,qq,m,n] != -1 };
set SVCCC   := {(1,svc,n) in SVC: n in BUSCC};

#
# Control parameters for shunts
# If no shunt in PARAM_SHUNT, it means that hey are all fixed
#
# Variable shunts
# Shunts wich are not connected (n=-1) but which are in PARAM_SHUNT are considered as connected to their possible bus, with variable reactance
set SHUNT_VAR := setof {(1,s,n) in SHUNT inter PARAM_SHUNT: 
  (s,n) in SHUNTCC # Remember n might be -1 if shunt_possiblebus[1,s,n] is in BUSCC
  and abs(shunt_valmin[1,s,n])+abs(shunt_valmax[1,s,n]) >= Pnull / base100MVA # Useless to allow change if values are too small
  } (s,shunt_possiblebus[1,s,n]);
# Shunts with fixed values
set SHUNT_FIX := setof {(1,s,n) in SHUNT diff PARAM_SHUNT: n in BUSCC} (s,n);
# If a shunt is not connected (n=-1) and it is not in PARAM_SHUNT, then it will not be 
# reconnected by reactive opf. These shunts are not in SHUNT_VAR nor in SHUNT_FIX; they
# are simply ignored

#
# Control parameters for SVC
#
# Simple: if regul==true then SVC is on, else it is off
set SVCON := {(svc,n) in SVCCC: svc_vregul[1,svc,n]=="true" and svc_bmin[1,svc,n]<=svc_bmax[1,svc,n]-Pnull/base100MVA};

#
# Control parameters for reactive power of units
#
# If unit_Qc is not consistent, then reactive power will be a variable
set UNIT_FIXQ := setof{(t,g,n) in PARAM_UNIT_FIXQ: (g,n) in UNITON and abs(unit_Qc[1,g,n])<PQmax } (g,n);

#
# Control parameters for ratios of transformers
#
set BRANCHCC_REGL_VAR := 
  { (qq,m,n) in BRANCHCC_REGL: 
    (1,qq,m,n) in PARAM_TRANSFORMERS_RATIO_VARIABLE
    and regl_ratio_min[1,branch_ptrRegl[1,qq,m,n]] < regl_ratio_max[1,branch_ptrRegl[1,qq,m,n]]
  };
set BRANCHCC_REGL_FIX :=BRANCHCC_REGL diff BRANCHCC_REGL_VAR;


#
# VSC converter stations
#
set VSCCONVON := setof{(t,v,n) in VSCCONV: 
  n in BUSCC
  and abs(vscconv_P0[t,v,n]  ) <= PQmax
  and abs(vscconv_Pmin[t,v,n]) <= PQmax
  and abs(vscconv_Pmax[t,v,n]) <= PQmax
  and vscconv_P0[t,v,n] >= vscconv_Pmin[t,v,n] 
  and vscconv_P0[t,v,n] <= vscconv_Pmax[t,v,n]
  } (v,n);

#
# LCC converter stations
#
set LCCCONVON := setof{(t,l,n) in LCCCONV: 
  n in BUSCC
  and abs(lccconv_P0[1,l,n]) <= PQmax
  and abs(lccconv_Q0[1,l,n]) <= PQmax
  } (l,n);




###############################################################################
# Corrected values for reactances
###############################################################################
# If in BRANCHZNULL, then set X to ZNULL
param branch_X_mod{(qq,m,n) in BRANCHCC} :=
  if (qq,m,n) in BRANCHZNULL then Znull
  else branch_X[1,qq,m,n];
check {(qq,m,n) in BRANCHCC}: abs(branch_X_mod[qq,m,n]) > 0; 


###############################################################################
# Corrected values for units
###############################################################################
param corrected_unit_Pmin{UNITON} default defaultPmin;
param corrected_unit_Pmax{UNITON} default defaultPmax;
param corrected_unit_qP  {UNITON} default defaultQmin;
param corrected_unit_qp  {UNITON} default defaultQmin;
param corrected_unit_QP  {UNITON} default defaultQmax;
param corrected_unit_Qp  {UNITON} default defaultQmax;
param corrected_unit_Qmin{UNITON} default defaultQmin;
param corrected_unit_Qmax{UNITON} default defaultQmax;



###############################################################################
# Maximum flows on branches
###############################################################################
param Fmax{(qq,m,n) in BRANCHCC} :=
  1.732 * 0.001
  * max(substation_Vnomi[1,bus_substation[1,m]]*abs(branch_patl1[1,qq,m,n]),substation_Vnomi[1,bus_substation[1,n]]*abs(branch_patl2[1,qq,m,n])); 



###############################################################################
# Transformers and Phase shifter transformers parameters
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
  else atan2(branch_R[1,qq,m,n]  , branch_X_mod[qq,m,n]  );

param branch_admi {(qq,m,n) in BRANCHCC} = 
  if (qq,m,n) in BRANCHCC_DEPH
  then 1./sqrt(branch_Rdeph[qq,m,n]^2 + branch_Xdeph[qq,m,n]^2 )
  else 1./sqrt(branch_R[1,qq,m,n]^2   + branch_X_mod[qq,m,n]^2   );

# TODO : passer la partie regleur en variable
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
param branch_dephex {(qq,m,n) in BRANCHCC} = 0; # In IIDM, everything is in bus1 so dephase at bus2 is always 0




###############################################################################
#
# List of all optimization problems which wil be solved successively
#
###############################################################################
# Variables are defined without reference to a particular optimization problem
# Constraints are to be defined with
set PROBLEM_CCOMP default {1};
set PROBLEM_DCOPF default { };
set PROBLEM_ACOPF default { };
# After solving DCOPF, switch to ACOPF this way:
#let PROBLEM_CCOMP := { }; # Desactivation of CCOMP
#let PROBLEM_DCOPF := { }; # Desactivation of DCOPF
#let PROBLEM_ACOPF := {1}; # Activation of ACOPF



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



###############################################################################
#
# Variables and contraints for DCOPF
#
###############################################################################
# Why doing a DCOPF before ACOPF?
# 1/ if DCOPF fails, how to hope that ACOPF is feasible? -> DCOPF may be seen as an unformal consistency check on data
# 2/ phases computed in DCOPF will be used as initial point for ACOPF
# Some of the variables and constraints defined for DCOPF will be used also for ACOPF

# Phase of voltage
param teta_min default -10; # roughly 3*pi
param teta_max default  10; # roughly 3*pi
var teta_dc{n in BUSCC} <= teta_max, >= teta_min;
subject to ctr_null_phase_bus_dc{PROBLEM_DCOPF}: teta_dc[null_phase_bus] = 0;

# Variable flow is the flow from bus 1 to bus 2
var activeflow{BRANCHCC};
subject to ctr_activeflow{PROBLEM_DCOPF, (qq,m,n) in BRANCHCC}:
  activeflow[qq,m,n] = base100MVA * (teta_dc[m]-teta_dc[n]) / branch_X_mod[qq,m,n];#* branch_X_mod[qq,m,n] / (branch_X_mod[qq,m,n]**2+branch_R[1,qq,m,n]**2);

# Generation for DCOPF
var P_dcopf{(g,n) in UNITON}; # >= unit_Pmin[1,g,n], <= unit_Pmax[1,g,n];

# Slack variable for each bus
# >=0 if too much generation in bus, <=0 if missing generation
var balance_pos{BUSCC} >= 0;
var balance_neg{BUSCC} >= 0;

# Balance at each bus
subject to ctr_balance{PROBLEM_DCOPF, n in BUSCC}:
  - sum{(g,n) in UNITON} P_dcopf[g,n]
  + sum{(c,n) in LOADCC} load_PFix[1,c,n]
  + sum{(qq,n,m) in BRANCHCC} activeflow[qq,n,m] # active power flow outgoing on branch qq at bus n
  - sum{(qq,m,n) in BRANCHCC} activeflow[qq,m,n] # active power flow entering in bus n on branch qq
  + sum{(vscconv,n) in VSCCONVON} vscconv_P0[1,vscconv,n]
  + sum{(l,n) in LCCCONVON} lccconv_P0[1,l,n]
  =
  balance_pos[n] - balance_neg[n];


#
# objective function and penalties
#
param penalty_gen     := 1;
param penalty_balance := 1000;

minimize problem_dcopf_objective:
    penalty_gen     * sum{(g,n) in UNITON} ((P_dcopf[g,n]-unit_Pc[1,g,n])/max(0.01*abs(unit_Pc[1,g,n]),1))**2 
  + penalty_balance * sum{n in BUSCC} ( balance_pos[n] + balance_neg[n] )
  ;



###############################################################################
#
# Variables and contraints for ACOPF
#
###############################################################################
# Notice that some variables and constraints for DCOPF are also used for ACOPF


#
# Phase and modulus of voltage
#
# Complex voltage = V*exp(i*teta). (with i**2=-1)

# Phase of voltage
var teta{BUSCC} <= teta_max, >= teta_min;
subject to ctr_null_phase_bus{PROBLEM_ACOPF}: teta[null_phase_bus] = 0;

# Modulus of voltage
var V{n in BUSCC} 
  <= 
  if substation_Vnomi[1,bus_substation[1,n]] <= ignore_voltage_bounds then 2-epsilon_min_voltage else
  voltage_upper_bound[1,bus_substation[1,n]],
  >= 
  if substation_Vnomi[1,bus_substation[1,n]] <= ignore_voltage_bounds then epsilon_min_voltage else
  voltage_lower_bound[1,bus_substation[1,n]];


#
# Generation
#
# General idea: generation is an input data, but as voltage may vary, generation may vary a little.
# Variations of generation is totally controlled by unique scalar variable alpha
# Before and after optimization, there is no waranty that P is within 
# its "bounds" [corrected_unit_Pmin;corrected_unit_Pmax]
#

# Active generation
var alpha <=1, >=1; # If alpha==1 then all units are at Pmax
var P{(g,n) in UNITON} 
# todo mieux gerer la production lorsqu'on aure progresse pour la realisabilite
/*
 = unit_Pc[1,g,n] + 
  if ( unit_Pc[1,g,n] < corrected_unit_Pmax[g,n] - Pnull and unit_Pc[1,g,n] > Pnull) 
  then alpha*(corrected_unit_Pmax[g,n]- unit_Pc[1,g,n]);
  */
  <= max(unit_Pc[1,g,n],corrected_unit_Pmax[g,n]), >= min(unit_Pc[1,g,n],corrected_unit_Pmin[g,n]);


#
# Reactive generation
#
# todo: add trapeze of hexagone constraints for reactive power
var Q{(g,n) in UNITON} <= corrected_unit_Qmax[g,n], >= corrected_unit_Qmin[g,n];


#
# Variable shunts
#
var shunt_var{(shunt,n) in SHUNT_VAR}
  >= min{(1,shunt,k) in SHUNT} shunt_valmin[1,shunt,k], 
  <= max{(1,shunt,k) in SHUNT} shunt_valmax[1,shunt,k];
 

#
# SVC reactive generation
#
var svc_qvar{(svc,n) in SVCON} >= svc_bmin[1,svc,n], <= svc_bmax[1,svc,n];


#
# VSCCONV reactive generation
#
var vscconv_qvar{(v,n) in VSCCONVON}
  >= min(vscconv_qP[1,v,n],vscconv_qp0[1,v,n],vscconv_qp[1,v,n]),
  <= max(vscconv_QP[1,v,n],vscconv_Qp0[1,v,n],vscconv_Qp[1,v,n]);
# todo: add trapeze of hexagone constraints for reactive power


#
# Ratios of transformers
#
var branch_Ror_var{(qq,m,n) in BRANCHCC_REGL_VAR} 
  >= regl_ratio_min[1,branch_ptrRegl[1,qq,m,n]],
  <= regl_ratio_max[1,branch_ptrRegl[1,qq,m,n]];

#
# Flows
#

var Red_Tran_Act_Dir{(qq,m,n) in BRANCHCC } =
    V[n] * branch_admi[qq,m,n] * sin(teta[m]-teta[n]+branch_dephor[qq,m,n]-branch_angper[qq,m,n])
    * (if (qq,m,n) in BRANCHCC_REGL_VAR then branch_Ror_var[qq,m,n]*branch_cstratio[1,qq,m,n] else branch_Ror[qq,m,n]) 
  + V[m] * (branch_admi[qq,m,n]*sin(branch_angper[qq,m,n])+branch_Gor[1,qq,m,n])
    * (if (qq,m,n) in BRANCHCC_REGL_VAR then branch_Ror_var[qq,m,n]*branch_cstratio[1,qq,m,n] else branch_Ror[qq,m,n])**2
  ;

var Red_Tran_Rea_Dir{(qq,m,n) in BRANCHCC } = 
  - V[n] * branch_admi[qq,m,n] * cos(teta[m]-teta[n]+branch_dephor[qq,m,n]-branch_angper[qq,m,n])
    * (if (qq,m,n) in BRANCHCC_REGL_VAR then branch_Ror_var[qq,m,n]*branch_cstratio[1,qq,m,n] else branch_Ror[qq,m,n])
  + V[m] * (branch_admi[qq,m,n]*cos(branch_angper[qq,m,n])-branch_Bor[1,qq,m,n])
    * (if (qq,m,n) in BRANCHCC_REGL_VAR then branch_Ror_var[qq,m,n]*branch_cstratio[1,qq,m,n] else branch_Ror[qq,m,n])^2 
  ;

var Red_Tran_Act_Inv{(qq,m,n) in BRANCHCC } = 
    V[m] * branch_admi[qq,m,n] * sin(teta[n]-teta[m]-branch_dephor[qq,m,n]-branch_angper[qq,m,n])
    * (if (qq,m,n) in BRANCHCC_REGL_VAR then branch_Ror_var[qq,m,n]*branch_cstratio[1,qq,m,n] else branch_Ror[qq,m,n])
  + V[n] * (branch_admi[qq,m,n]*sin(branch_angper[qq,m,n])+branch_Gex[1,qq,m,n])
  ;

var Red_Tran_Rea_Inv{(qq,m,n) in BRANCHCC } =
  - V[m] * branch_admi[qq,m,n] * cos(teta[n]-teta[m]-branch_dephor[qq,m,n]-branch_angper[qq,m,n])
    * (if (qq,m,n) in BRANCHCC_REGL_VAR then branch_Ror_var[qq,m,n]*branch_cstratio[1,qq,m,n] else branch_Ror[qq,m,n])
  + V[n] * (branch_admi[qq,m,n]*cos(branch_angper[qq,m,n])-branch_Bex[1,qq,m,n])
  ;


#
# Active Balance
#

subject to ctr_balance_P{PROBLEM_ACOPF,k in BUSCC}:
  # Flows
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Dir[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Act_Inv[qq,m,k]
  # Generating units
  - sum{(g,k) in UNITON} P[g,k]
  # Loads
  + sum{(c,k) in LOADCC} load_PFix[1,c,k]     # Fixed value
  # VSC converters
  + sum{(v,k) in VSCCONVON} vscconv_P0[1,v,k] # Fixed value
  # LCC converters
  + sum{(l,k) in LCCCONVON} lccconv_P0[1,l,k] # Fixed value
  = 0; # No slack variables for active power. If data are really too bad, may not converge.


#
# Reactive Balance
#

# Reactive balance slack variables only if there is a load or a shunt connected
# If there is a unit, or SVC, or VSC, they already have reactive power generation, so no need to add slack variables
set BUSCC_SLACK :=  {n in BUSCC: 
  (
  #(card({(c,n) in LOADCC})>0 or card({(t,s,n) in SHUNT})>0) and
  card{(g,n) in UNITON: (g,n) not in UNIT_FIXQ}==0
  and card{(svc,n) in SVCON}==0
  and card{(vscconv,n) in VSCCONVON}==0
  )
  #or (card(setof{(qq,m,n) in BRANCHCC}m)+card(setof{(qq,n,m) in BRANCHCC}m))==1 # Bus connected with only one other bus
  } ;
var slack1_balance_Q{BUSCC_SLACK} >=0, <= 500; # 500 Mvar is already HUGE
var slack2_balance_Q{BUSCC_SLACK} >=0, <= 500;
#subject to ctr_compl_slack_Q{PROBLEM_ACOPF,k in BUSCC_SLACK}: slack1_balance_Q[k] >= 0 complements slack2_balance_Q[k] >= 0;

subject to ctr_balance_Q{PROBLEM_ACOPF,k in BUSCC}: 
  # Flows
    sum{(qq,k,n) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Dir[qq,k,n]
  + sum{(qq,m,k) in BRANCHCC} base100MVA * V[k] * Red_Tran_Rea_Inv[qq,m,k]
  # Senerating units
  - sum{(g,k) in UNITON: (g,k) not in UNIT_FIXQ } Q[g,k]
  - sum{(g,k) in UNIT_FIXQ} unit_Qc[1,g,k]
  # Soad
  + sum{(c,k) in LOADCC} load_QFix[1,c,k]
  # Shunts
  - sum{(shunt,k) in SHUNT_FIX} base100MVA * shunt_valnom[1,shunt,k] * V[k]^2
  - sum{(shunt,k) in SHUNT_VAR} base100MVA * shunt_var[shunt,k] * V[k]^2
  # SVC
  - sum{(svc,k) in SVCON} base100MVA * svc_qvar[svc,k] * V[k]^2
  # VSC converters
  - sum{(v,k) in VSCCONVON} vscconv_qvar[v,k]
  # LCC converters
  + sum{(l,k) in LCCCONVON} lccconv_Q0[1,l,k] # Fixed value
  # Slack variables
  + if k in BUSCC_SLACK then 
  (- slack1_balance_Q[k]  # Homogeneous to a generation of reactive power (condensator)
   + slack2_balance_Q[k]) # homogeneous to a reactive load (self)
  = 0;



#
# Objective function and penalties
#
param penalty_invest_rea_pos := 10;
param penalty_invest_rea_neg := 10;
param penalty_units_reactive := 0.1;
param penalty_transfo_ratio  := 0.1;

minimize problem_acopf_objective:
  sum{n in BUSCC_SLACK} (
      penalty_invest_rea_pos * slack1_balance_Q[n]
    + penalty_invest_rea_neg * slack2_balance_Q[n]
    )
  
  #+(sum{n in BUSCC_SLACK} slack1_balance_Q[n] * slack2_balance_Q[n] ) * (penalite_invest_rea_pos+penalite_invest_rea_neg)
  
  # todo revenir a une variation homogene de la prod
  # L'idee serait de demarrer avec des prods proportionnelle (utiliser alpha), mais si on n'y arrive 
  # pas alors on minimise l'ecart quadratique ou la somme
  #+ alpha
  + sum{(g,n) in UNITON} ( (P[g,n]-unit_Pc[1,g,n])/max(1,abs(unit_Pc[1,g,n])) )**2 
  #+ sum{(g,n) in UNITON} P[g,n]
  
  # Voltage for busses
  + sum{n in BUSCC} (V[n]-0.5*(voltage_lower_bound[1,bus_substation[1,n]]+voltage_upper_bound[1,bus_substation[1,n]]))**2
  
  # Reactive power of units
  + penalty_units_reactive * sum{(g,n) in UNITON} (Q[g,n]/max(1,abs(corrected_unit_Qmin[g,n]),abs(corrected_unit_Qmax[g,n])))**2
  
  # Ratio of transformers
  + penalty_transfo_ratio * sum{(qq,m,n) in BRANCHCC_REGL_VAR} (branch_Ror[qq,m,n]-branch_Ror_var[qq,m,n])**2
  ;
  #





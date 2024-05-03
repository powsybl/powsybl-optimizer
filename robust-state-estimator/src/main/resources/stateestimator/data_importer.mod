###############################################################################
#
# Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services 
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
###############################################################################

###############################################################################
# State Estimator
# Author : Jean Maeght 2022 2023
# Author : Pierre Arvy 2023
# Author : Lucas Riou 2024
###############################################################################



###############################################################################
#                             MEASUREMENTS                                    #
###############################################################################

# Any piece of information about physical variables will only be considered if
# provided through a measurement file. Most information about generators, loads, 
# bus voltages provided through network files is loaded but will not be
# considered by the State Estimator.
# Measurements are given in SI.

###############################################################################
#               Active power flows (ampl_measures_Pf.txt)                     #
###############################################################################
#ampl_measures_Pf.txt

set MEASURES_Pf dimen 1;
param Pf_type             {MEASURES_Pf} symbolic;
param Pf_branch_id        {MEASURES_Pf} symbolic; # branch (m,n) number for which Pf is measured
param Pf_firstbus_id      {MEASURES_Pf} symbolic; # first bus m (important ! tells on which side power flow is measured)
param Pf_secondbus_id     {MEASURES_Pf} symbolic; # second bus n
param Pf_value            {MEASURES_Pf}; 
param Pf_variance         {MEASURES_Pf};

# "Pf_branch","Pf_firstbus","Pf_secondbus" are not loaded from data.
# They are assigned the bus/branch number (see ampl_network_buses.txt, ampl_network_branches.txt) 
# that relates to "Pf_branch_id"/"Pf_firstbus_id"/"Pf_secondbus_id" (see state_estimator.run)
param Pf_branch    {l in MEASURES_Pf} symbolic;
param Pf_firstbus  {l in MEASURES_Pf} symbolic;
param Pf_secondbus {l in MEASURES_Pf} symbolic;

###############################################################################
#               Reactive power flows (ampl_measures_Qf.txt)                   #
###############################################################################
#ampl_measures_Qf.txt

set MEASURES_Qf dimen 1;
param Qf_type             {MEASURES_Qf} symbolic;
param Qf_branch_id        {MEASURES_Qf} symbolic; # branch (m,n) ID for which Qf is measured
param Qf_firstbus_id      {MEASURES_Qf} symbolic; # first bus m
param Qf_secondbus_id     {MEASURES_Qf} symbolic; # second bus n
param Qf_value            {MEASURES_Qf}; 
param Qf_variance         {MEASURES_Qf};

# "Qf_branch","Qf_firstbus","Qf_secondbus" are not loaded from data.
# They are assigned the bus/branch number (see ampl_network_buses.txt, ampl_network_branches.txt) 
# that relates to "Qf_branch_id"/"Qf_firstbus_id"/"Qf_secondbus_id" (see state_estimator.run)
param Qf_branch    {l in MEASURES_Qf} symbolic;
param Qf_firstbus  {l in MEASURES_Qf} symbolic;
param Qf_secondbus {l in MEASURES_Qf} symbolic;

###############################################################################
#               Injected active powers (ampl_measures_P.txt)                  #
###############################################################################
#ampl_measures_P.txt

set MEASURES_P dimen 1;
param P_type              {MEASURES_P} symbolic;
param P_bus_id            {MEASURES_P} symbolic; 
param P_value             {MEASURES_P}; 
param P_variance          {MEASURES_P};

# "P_bus" is not loaded from data, but is assigned the bus number (see ampl_network_buses.txt)
# that relates to "P_bus_id" (see state_estimator.run)
param P_bus {l in MEASURES_P} symbolic; 

###############################################################################
#               Injected reactive powers (ampl_measures_Q.txt)                #
###############################################################################
#ampl_measures_Q.txt

set MEASURES_Q dimen 1;
param Q_type             {MEASURES_Q} symbolic;
param Q_bus_id           {MEASURES_Q} symbolic;
param Q_value            {MEASURES_Q}; 
param Q_variance         {MEASURES_Q};

# "Q_bus" is not loaded from data, but is assigned the bus number (see ampl_network_buses.txt)
# that relates to "Q_bus_id" (see state_estimator.run)
param Q_bus {l in MEASURES_Q} symbolic; 

###############################################################################
#                   Voltage magnitude (ampl_measures_V.txt)                   #
###############################################################################
#ampl_measures_V.txt

set MEASURES_V dimen 1;
param V_type             {MEASURES_V} symbolic;
param V_bus_id           {MEASURES_V} symbolic;
param V_value            {MEASURES_V}; 
param V_variance         {MEASURES_V};

# "V_bus" is not loaded from data, but is assigned the bus number (see ampl_network_buses.txt)
# that relates to "V_bus_id" (see state_estimator.run)
param V_bus {l in MEASURES_V} symbolic;


###############################################################################
#                              NETWORK                                        #
###############################################################################

# In 'ampl_network' files, parameters are given in per unit
# Great care must be taken when choosing base voltages to revert to SI units(see Ampl Exporter in powsybl-core)

###############################################################################
#               Substations (ampl_network_substations.txt)                    #
###############################################################################
#ampl_network_substations.txt

# [variant, substation]
# The 1st column "variant" may also be used to define time step, in case this
# PowSyBl format is used for multi-timestep OPF. The letter mostly used to
# refer to the variant is 't' (unused feature in the state estimator)

set SUBSTATIONS dimen 2; #See this in error message? Use "ampl reactiveopf.run" instead of .mod 
param substation_horizon     {SUBSTATIONS} symbolic;
param substation_fodist      {SUBSTATIONS};
param substation_Vnomi       {SUBSTATIONS}; # kV (useful)
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

###############################################################################
#                       Buses (ampl_network_buses.txt)                        #
###############################################################################

set BUS dimen 2 ; # [variant, bus]
param bus_substation{BUS} integer;
param bus_CC        {BUS} integer; # num of connex component. Computation only in CC number 0 (=main connex component)
param bus_V0        {BUS}; # (unused in SE)
param bus_angl0     {BUS}; # (unused in SE)
param bus_injA      {BUS};
param bus_injR      {BUS};
param bus_fault     {BUS};
param bus_curative  {BUS};
param bus_id        {BUS} symbolic;

# Consistency checks
check{(t,n) in BUS}: t in TIME;
check{(t,n) in BUS}: n >= -1;
check{(t,n) in BUS}: (t,bus_substation[t,n]) in SUBSTATIONS;
# Check uniqueness of buses IDs
set TEST_UNIQUENESS_BUS := setof{(1,n) in BUS} bus_id[1,n];
check card(BUS) == card(TEST_UNIQUENESS_BUS);


###############################################################################
#                           Slack (slack_bus.txt)                             #
###############################################################################

set SLACK dimen 1;
param slack_bus_id{SLACK} symbolic;
param null_phase_bus;

###############################################################################
#              Generating units (ampl_network_generators.txt)                 #  useless ?
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
param unit_Vc      {UNIT}; # Voltage set point (in case of voltage regulation) (unused in SE)
param unit_Pc      {UNIT}; # Active  power set point (unused in SE)
param unit_Qc      {UNIT}; # Rective power set point (in case no voltage regulation) (unused in SE)
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
#check {(t,g,n) in UNIT}: unit_Pmax[t,g,n] >= -Pnull;
#check {(t,g,n) in UNIT}: unit_Pmax[t,g,n] >= unit_Pmin[t,g,n];
# Checks below are useless since values will be corrected for units in UNITON
#check {(t,g,n) in UNIT}: unit_Qp[t,g,n] >= unit_qp[t,g,n];
#check {(t,g,n) in UNIT}: unit_QP[t,g,n] >= unit_qP[t,g,n] ;

# Global inital losses_ratio: value of (P-C-H)/(C+H) in data files
# Value is 0 if no losses
# Value 0.02 means % of losses 
param global_initial_losses_ratio default 0.02; # Typical value for transmission

###############################################################################
#                       Loads (ampl_network_loads.txt)                        # useless ?
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
#check {(t,s,n)  in SHUNT}: shunt_valmin[1,s,n] <= shunt_valmax[1,s,n];

# Case of a reactance : check valmin < 0 and valmax=0
#check {(t,s,n) in SHUNT}: shunt_valmin[1,s,n] <= 0;
#check {(t,s,n) in SHUNT : shunt_valmin[1,s,n] <= -Pnull / base100MVA}: shunt_valmax[1,s,n] <=  Pnull / base100MVA;
# Case of a condo : check valmin = 0 and valmax>0
#check {(t,s,n) in SHUNT}: shunt_valmax[1,s,n] >= 0;
#check {(t,s,n) in SHUNT : shunt_valmax[1,s,n] >=  Pnull / base100MVA}: shunt_valmin[1,s,n] >= -Pnull / base100MVA;

###############################################################################
#     Static Var Compensator (ampl_network_static_var_compensators.txt)       # useless ?
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
#                   Batteries (ampl_network_batteries.txt)                    # useless ?
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
#check {(t,b,n) in BATTERY} : battery_Pmin[t,b,n] <= battery_Pmax[t,b,n] ;

# Tap seems to be transformers

###############################################################################
#                   Tables of taps (ampl_network_tct.txt)                     # useless (we have all we need in branches.txt)
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
param regl_V        {REGL};
param regl_fault    {REGL};
param regl_curative {REGL};
param regl_id       {REGL} symbolic;

# Consistency checks
check {(t,r) in REGL}: regl_table[t,r] in TAPTABLES;
check {(t,r) in REGL}: (t,regl_table[t,r], regl_tap0[t,r]) in TAPS;

# Useless
#param regl_ratio_min{(t,r) in REGL} := if card(REGL) > 0 then min{(t,regl_table[t,r],tap) in TAPS} tap_ratio[t,regl_table[t,r],tap] else 0;
#param regl_ratio_max{(t,r) in REGL} := if card(REGL) > 0 then max{(t,regl_table[t,r],tap) in TAPS} tap_ratio[t,regl_table[t,r],tap] else 0;

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
#   VSC converter station data (ampl_network_vsc_converter_stations.txt)      # useless 
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
#check {(t,cs,n) in VSCCONV}: vscconv_Pmin[t,cs,n] <= vscconv_Pmax[t,cs,n];
#check {(t,cs,n) in VSCCONV}: vscconv_qp[t,cs,n]   <= vscconv_Qp[t,cs,n];
#check {(t,cs,n) in VSCCONV}: vscconv_qp0[t,cs,n]  <= vscconv_Qp0[t,cs,n];
#check {(t,cs,n) in VSCCONV}: vscconv_qP[t,cs,n]   <= vscconv_QP[t,cs,n];

###############################################################################
#     LCC converter station data (ampl_network_lcc_converter_stations.txt)    # useless
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
#                          HVDC (ampl_network_hvdc.txt)                       # useless
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
#check {(t,h) in HVDC}: hvdc_Vnom[t,h] >= epsilon_nominal_voltage;
check {(t,h) in HVDC}: hvdc_convertersMode[t,h] == "SIDE_1_RECTIFIER_SIDE_2_INVERTER" or hvdc_convertersMode[t,h] == "SIDE_1_INVERTER_SIDE_2_RECTIFIER";
check {(t,h) in HVDC}: hvdc_targetP[t,h] >= 0.0;
check {(t,h) in HVDC}: hvdc_targetP[t,h] <= hvdc_Pmax[t,h];

###############################################################################
#                     Branches (ampl_network_branches.txt)                    #
###############################################################################

set BRANCH dimen 4; # [variant, branch, bus1, bus2]
param branch_3wt          {BRANCH};
param branch_subor        {BRANCH} integer;
param branch_subex        {BRANCH} integer;
param branch_R            {BRANCH};
param branch_X            {BRANCH};
param branch_Gor          {BRANCH};
param branch_Gex          {BRANCH};
param branch_Bor          {BRANCH};
param branch_Bex          {BRANCH};
param branch_cstratio     {BRANCH}; # fixed ratio
param branch_ptrRegl      {BRANCH} integer; # Number of the ratio tap changer if any (-1 otherwise)
param branch_ptrDeph      {BRANCH} integer; # Number of the phase tap changer (-1 otherwise)
param branch_Por          {BRANCH};
param branch_Pex          {BRANCH};
param branch_Qor          {BRANCH};
param branch_Qex          {BRANCH};
param branch_patl1        {BRANCH}; # current limit 1 (in A)
param branch_patl2        {BRANCH}; # current limit 2 (in A)
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

# Check uniqueness of branches IDs
set TEST_UNIQUENESS_BRANCH := setof{(1,qq,m,n) in BRANCH} branch_id[1,qq,m,n];
check card(BRANCH) == card(TEST_UNIQUENESS_BRANCH);

# 

###############################################################################
#                        ADDITIONAL KNOWLEDGE                                 #
###############################################################################

###############################################################################
#       Set of suspect branches (ampl_suspect_branches.txt)                   #
###############################################################################

set BRANCH_SUSP dimen 1; # [num]
param branch_susp_id {BRANCH_SUSP} symbolic; # All branches are present in ampl_suspect_branches.txt
param is_suspected {BRANCH_SUSP} binary; # If equal to 1, then branch status is suspected to be false. Change of status allowed.
param y_prior {BRANCH_SUSP} binary; # "A priori" status of the branch

# Note : checking that suspect branches IDs are valid is done in Java
# Same goes with buses/branches IDs related to measurements

###############################################################################
#       Set of zero-injection buses (ampl_zero_injection_buses.txt)           #
###############################################################################

set BUS_ZERO_INJECTION dimen 1; # [num]
param bus_zero_injection_id {BUS_ZERO_INJECTION} symbolic;



#####################################################################################################
#                     Build the sets of equipments present in the main CC                           #
#####################################################################################################

# Elements (buses and branches) in the main connex component
param index_main_connex_component = 0;
set BUS2:= setof{(1,n) in BUS: bus_CC[1,n] == index_main_connex_component and n >= 0} n;
#check {n in BUS2}: substation_Vnomi[1,bus_substation[1,n]] >= epsilon_nominal_voltage;
set BRANCH2:= setof {(1,qq,m,n) in BRANCH: m in BUS2 and n in BUS2} (qq,m,n);

# Elements in the final component used for the optimization problem (after checking that theta = 0)
set BUSCC dimen 1 default {}; # defined in state_estimator.run
#check {n in BUSCC}: bus_V0[1,n] >= epsilon_min_voltage; # Check buses have valid voltage value

#param bus_V0_corrected{n in BUSCC};
#param bus_angl0_corrected{n in BUSCC};

# Set of branches in main CC
set BRANCHCC:= setof {(1,qq,m,n) in BRANCH: m in BUSCC and n in BUSCC } (qq,m,n); 
set BRANCH_WITH_SIDE_2_OPENED := setof {(1,qq,m,n) in BRANCH: m in BUSCC and n == -1 and m != n} (qq,m,n);
set BRANCH_WITH_SIDE_1_OPENED := setof {(1,qq,m,n) in BRANCH: m == -1 and n in BUSCC and m != n} (qq,m,n);

set BRANCHCC_FULL:= BRANCHCC union BRANCH_WITH_SIDE_2_OPENED union BRANCH_WITH_SIDE_1_OPENED;

# Sets of measurements strictly linked to the main CC
set MEASURECC_V := setof {l in MEASURES_V: V_bus[l] in BUSCC} l;
set MEASURECC_P := setof{l in MEASURES_P: P_bus[l] in BUSCC} l;
set MEASURECC_Q := setof{l in MEASURES_Q: Q_bus[l] in BUSCC} l;
set MEASURECC_Pf := setof{l in MEASURES_Pf: Pf_firstbus[l] in BUSCC and Pf_secondbus[l] in BUSCC} l;
set MEASURECC_Qf := setof{l in MEASURES_Qf: Qf_firstbus[l] in BUSCC and Qf_secondbus[l] in BUSCC} l;

# Define the set of truly suspect branches (used only for information)
set BRANCH_TRULY_SUSP := setof{l in BRANCH_SUSP: is_suspected[l] == 1} l;
# Define the set of truly suspect branches included in BRANCHCC (used only for information)
set BRANCHCC_TRULY_SUSP := setof{(qq,m,n,l) in BRANCHCC_FULL cross BRANCH_SUSP: 
    branch_susp_id[l] == branch_id[1,qq,m,n] and is_suspected[l] == 1} (qq,m,n);

#set LOADCC  := setof {(1,c,n)    in LOAD  : n in BUSCC} (c,n);
#set UNITCC  := setof {(1,g,n)    in UNIT  : n in BUSCC} (g,n);

###############################################################################
#        Deal with "zero"-impedance branches (first step)                     #
###############################################################################

# Compute module of Z in SI (use only base voltage of terminal 1 !)
param branch_Z{(qq,m,n) in BRANCHCC_FULL} := 
  sqrt((branch_R[1,qq,m,n] * substation_Vnomi[1,branch_subor[1,qq,m,n]]^2 / base100MVA)^2
  + (branch_X[1,qq,m,n] * substation_Vnomi[1,branch_subor[1,qq,m,n]]^2 / base100MVA)^2);
check {(qq,m,n) in BRANCHCC_FULL}: branch_Z[qq,m,n] >= 0;

# Define set BRANCHZNULL (used later)
set BRANCHZNULL := {(qq,m,n) in BRANCHCC_FULL: branch_Z[qq,m,n] <= Znull};

#####################################################################################################
#                                Additional sets                                                    #
#####################################################################################################

#
# Shunts, regleurs et dephaseurs
#
# NB : SHUNTCC are the shunts that are fixed
set SHUNTCC := {(1,s,n) in SHUNT: n in BUSCC or shunt_possiblebus[1,s,n] in BUSCC}; # We want to be able to reconnect shunts
set BRANCHCC_REGL := {(qq,m,n) in BRANCHCC_FULL diff BRANCHZNULL: branch_ptrRegl[1,qq,m,n] != -1 }; 
set BRANCHCC_DEPH := {(qq,m,n) in BRANCHCC_FULL diff BRANCHZNULL: branch_ptrDeph[1,qq,m,n] != -1 };
set BRANCHCC_TRANSFORMER := BRANCHCC_REGL union BRANCHCC_DEPH;
set BRANCHCC_3WT := {(qq,m,n) in BRANCHCC_FULL : branch_3wt[1,qq,m,n] != -1};
set SVCCC   := setof {(1,svc,n) in SVC: n in BUSCC} (svc,n);
set BUSCC_3WT := setof {(qq,m,n) in BRANCHCC : branch_3wt[1,qq,m,n] != -1} n;

# Define the set of transformers that are not indicated as such in 'ampl_network_branches.txt'
# Will only be used for transformer not appearing as such
set BRANCHCC_TRANSFO_AS_LINES := {(qq,m,n) in BRANCHCC_FULL diff BRANCHCC_TRANSFORMER: 
                                substr(branch_id[1,qq,m,n], 1, 1) == "T"};

#param targetV_busPV{n in BUSCC_PV};

# VSC converter stations
/* check {(t,v,n) in VSCCONV}: n in BUSCC and abs(vscconv_P0[t,v,n]  ) <= PQmax
  and abs(vscconv_Pmin[t,v,n]) <= PQmax and abs(vscconv_Pmax[t,v,n]) <= PQmax
  and vscconv_P0[t,v,n] >= vscconv_Pmin[t,v,n] and vscconv_P0[t,v,n] <= vscconv_Pmax[t,v,n]; */
set VSCCONVON := setof{(t,v,n) in VSCCONV} (v,n);

# LCC converter stations
/* check {(t,l,n) in LCCCONV}: n in BUSCC and abs(lccconv_P0[1,l,n]) <= PQmax 
                            and abs(lccconv_Q0[1,l,n]) <= PQmax; */
set LCCCONVON := setof{(t,l,n) in LCCCONV} (l,n); 

###############################################################################
#   Convert R and X to SI for lines (and transformers not indicated as such)  #
###############################################################################

# In view of AMPLExporter methods, different cases must be considered when converting back from p.u to SI

# Compute R in SI and correct its value if the branch has "zero impedance"

param branch_R_SI{(qq,m,n) in BRANCHCC_FULL} :=

  if (branch_Z[qq,m,n] == 0) 
  then Znull / sqrt(2)

  else if (qq,m,n) in BRANCHCC_TRANSFO_AS_LINES and (branch_Z[qq,m,n] <= Znull)
  then branch_R[1,qq,m,n] * Znull / branch_Z[qq,m,n] * substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA

  else if (qq,m,n) in BRANCHCC_TRANSFO_AS_LINES
  then branch_R[1,qq,m,n] * substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA

  else if (branch_Z[qq,m,n] <= Znull) 
  then branch_R[1,qq,m,n] * Znull / branch_Z[qq,m,n] * substation_Vnomi[1,branch_subor[1,qq,m,n]]^2 / base100MVA

  else branch_R[1,qq,m,n] * substation_Vnomi[1,branch_subor[1,qq,m,n]]^2 / base100MVA;

check {(qq,m,n) in BRANCHCC_FULL}: branch_R_SI[qq,m,n] >= 0;

# Compute X in SI and correct its value if the branch has "zero impedance"

param branch_X_SI{(qq,m,n) in BRANCHCC_FULL} :=

  if (branch_Z[qq,m,n] == 0) 
  then Znull / sqrt(2)

  else if (qq,m,n) in BRANCHCC_TRANSFO_AS_LINES and (branch_Z[qq,m,n] <= Znull)
  then branch_X[1,qq,m,n] * Znull / branch_Z[qq,m,n] * substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA

  else if (qq,m,n) in BRANCHCC_TRANSFO_AS_LINES
  then branch_X[1,qq,m,n] * substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA

  else if (branch_Z[qq,m,n] <= Znull) 
  then branch_X[1,qq,m,n] * Znull / branch_Z[qq,m,n] * substation_Vnomi[1,branch_subor[1,qq,m,n]]^2 / base100MVA

  else branch_X[1,qq,m,n] * substation_Vnomi[1,branch_subor[1,qq,m,n]]^2 / base100MVA;

check {(qq,m,n) in BRANCHCC_FULL}: abs(branch_X_SI[qq,m,n]) >= 0;

###############################################################################
#           Transformers and Phase shifting transformers parameters           #
###############################################################################

# TODO : check this !!! ==> OK

# Variable reactance, depending on tap (in SI)
param branch_Xdeph{(qq,m,n) in BRANCHCC_TRANSFORMER} =
  if (qq,m,n) in BRANCHCC_DEPH and (qq,m,n) in BRANCHCC_REGL
  and abs(tap_x[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]] 
      * tap_x[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]] / branch_X[1,qq,m,n]) > Znull
  then tap_x[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]] 
      * tap_x[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]] / (branch_X[1,qq,m,n])
      * substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA

  else if (qq,m,n) in BRANCHCC_DEPH and (qq,m,n) in BRANCHCC_REGL and
  tap_x[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]] 
      * tap_x[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]] / branch_X[1,qq,m,n] > 0
  then Znull

  else if (qq,m,n) in BRANCHCC_DEPH and (qq,m,n) in BRANCHCC_REGL and
  tap_x[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]] 
      * tap_x[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]] / branch_X[1,qq,m,n] < 0
  then -Znull

  else if (qq,m,n) in BRANCHCC_DEPH and abs(tap_x[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]]) > Znull
  then tap_x[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]]
      * substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA

  else if (qq,m,n) in BRANCHCC_DEPH and tap_x[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]] > 0
  then Znull

  else if (qq,m,n) in BRANCHCC_DEPH and tap_x[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]] < 0
  then -Znull

  else if (qq,m,n) in BRANCHCC_REGL and abs(tap_x[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]]) > Znull
  then tap_x[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]]
      * substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA

  else if (qq,m,n) in BRANCHCC_REGL and tap_x[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]] > 0
  then Znull

  else if (qq,m,n) in BRANCHCC_REGL and tap_x[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]] < 0
  then -Znull
  
  else Znull;


# TODO : check if this still works with branch_R_SI ==> OK

param branch_Rdeph{(qq,m,n) in BRANCHCC_TRANSFORMER} =
    branch_R[1,qq,m,n] * substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA;

# Variable resistance, depending on tap (in SI)
# As we do not have access to true values of R in law tables of transformers, we choose to vary R proportionnaly to X
#param branch_Rdeph{(qq,m,n) in BRANCHCC_TRANSFORMER} =
    #if abs(branch_X_SI[qq,m,n]) >= Znull
    #then branch_R[1,qq,m,n] * branch_Xdeph[qq,m,n] / branch_X_SI[qq,m,n]
    #      * substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA
    #else
    # branch_R[1,qq,m,n] * substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA
    #;

check {(qq,m,n) in BRANCHCC_TRANSFORMER}: branch_Rdeph[qq,m,n] >= 0;

###############################################################################
#     Additional information on impedances and admittances of the lines       #
###############################################################################

# All values in SI

param branch_angper{(qq,m,n) in BRANCHCC_FULL} =
  if (qq,m,n) in BRANCHCC_TRANSFORMER 
  then atan2(branch_Rdeph[qq,m,n], branch_Xdeph[qq,m,n])
  else atan2(branch_R_SI[qq,m,n], branch_X_SI[qq,m,n]);

param branch_admi_SI{(qq,m,n) in BRANCHCC_FULL} =  
  if (qq,m,n) in BRANCHCC_TRANSFORMER 
  then 1./sqrt(branch_Rdeph[qq,m,n]^2 + branch_Xdeph[qq,m,n]^2 )
  else 1./sqrt(branch_R_SI[qq,m,n]^2 + branch_X_SI[qq,m,n]^2);

/* param branch_G{(qq,m,n) in BRANCHCC_FULL} = 
  if (qq,m,n) in BRANCHCC_TRANSFORMER 
  then branch_Rdeph[qq,m,n] / (branch_Rdeph[qq,m,n]^2 + branch_Xdeph[qq,m,n]^2)
  else branch_R_SI[qq,m,n] / (branch_R_SI[qq,m,n]^2 + branch_X_SI[qq,m,n]^2);

param branch_B{(qq,m,n) in BRANCHCC_FULL} =  
  if (qq,m,n) in BRANCHCC_TRANSFORMER 
  then -branch_Xdeph[qq,m,n] / (branch_Rdeph[qq,m,n]^2 + branch_Xdeph[qq,m,n]^2)
  else -branch_X_SI[qq,m,n] / (branch_R_SI[qq,m,n]^2 + branch_X_SI[qq,m,n]^2); */

param branch_Gor_SI{(qq,m,n) in BRANCHCC_FULL} = 
  if (qq,m,n) in BRANCHCC_TRANSFORMER or (qq,m,n) in BRANCHCC_3WT or (qq,m,n) in BRANCHCC_TRANSFO_AS_LINES
  then branch_Gor[1,qq,m,n] / (substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA)
  else branch_Gor[1,qq,m,n] / (substation_Vnomi[1,branch_subor[1,qq,m,n]]^2 / base100MVA);

param branch_Gex_SI{(qq,m,n) in BRANCHCC_FULL} = 
  if (qq,m,n) in BRANCHCC_TRANSFORMER or (qq,m,n) in BRANCHCC_3WT or (qq,m,n) in BRANCHCC_TRANSFO_AS_LINES
  then branch_Gex[1,qq,m,n] / (substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA)
  else branch_Gex[1,qq,m,n] / (substation_Vnomi[1,branch_subor[1,qq,m,n]]^2 / base100MVA);

param branch_Bor_SI{(qq,m,n) in BRANCHCC_FULL} = 
  if (qq,m,n) in BRANCHCC_TRANSFORMER or (qq,m,n) in BRANCHCC_3WT or (qq,m,n) in BRANCHCC_TRANSFO_AS_LINES
  then branch_Bor[1,qq,m,n] / (substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA)
  else branch_Bor[1,qq,m,n] / (substation_Vnomi[1,branch_subor[1,qq,m,n]]^2 / base100MVA);

param branch_Bex_SI{(qq,m,n) in BRANCHCC_FULL} = 
  if (qq,m,n) in BRANCHCC_TRANSFORMER or (qq,m,n) in BRANCHCC_3WT or (qq,m,n) in BRANCHCC_TRANSFO_AS_LINES
  then branch_Bex[1,qq,m,n] / (substation_Vnomi[1,branch_subex[1,qq,m,n]]^2 / base100MVA)
  else branch_Bex[1,qq,m,n] / (substation_Vnomi[1,branch_subor[1,qq,m,n]]^2 / base100MVA);

###############################################################################
#         Additional information on rho and alpha of transformers             #
###############################################################################

# Note 1 : in IIDM network, a transformer on line (i,j) is always indicated on side i
# If in reality, transformer is on side j, then an equivalent transformer is computed so that it appears on side i
# Consequence : branch_Rex is always equal to 1 (in p.u.)

# Note 2 : rho is calculated using per-unit voltages in AMPL exporter (rho = (Vrated2/Vb2) / (Vrated1/Vb1)). 
# Therefore we must multiply by Vb2 and Vb1 to get rho as if calculated using SI voltages
# This must be done for transformers only : when dealing with a line (even if linking two different voltage levels), rho=1

# Note 3 : values "in SI" obtained are not exactly equal to those obtained in PowSyBl doing rho = Vrated2 / Vrated1

param branch_Ror_SI {(qq,m,n) in BRANCHCC_FULL} =
    ( if ((qq,m,n) in BRANCHCC_REGL)
      then tap_ratio[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]]
            * substation_Vnomi[1,branch_subex[1,qq,m,n]] / substation_Vnomi[1,branch_subor[1,qq,m,n]]
      else 1.0
    )
  * ( if ((qq,m,n) in BRANCHCC_DEPH) # TODO : Shouldn't be diff BRANCHCC_REGL ??
      then tap_ratio[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]]
            * substation_Vnomi[1,branch_subex[1,qq,m,n]] / substation_Vnomi[1,branch_subor[1,qq,m,n]]
      else 1.0
    )
  * ( if ((qq,m,n) in BRANCHCC_TRANSFO_AS_LINES) # Recall : BRANCHCC_TRANSFO_AS_LINES INTERSECT BRANCHCC_REGL = empty (same goes with BRANCHCC_DEPH)
      then branch_cstratio[1,qq,m,n] 
            * substation_Vnomi[1,branch_subex[1,qq,m,n]] / substation_Vnomi[1,branch_subor[1,qq,m,n]]
      else 1.0
    );

param branch_Rex_SI {(qq,m,n) in BRANCHCC_FULL} = 1;

param branch_dephor {(qq,m,n) in BRANCHCC_FULL} =
  if ((qq,m,n) in BRANCHCC_DEPH)
  then tap_angle[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]]
  else 0;

param branch_dephex {(qq,m,n) in BRANCHCC_FULL} = 0;
###############################################################################
#
# Copyright (c) 2022 2023 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
###############################################################################

###############################################################################
# Reactive OPF
# Author:  Jean Maeght 2022 2023
# Author:  Manuel Ruiz 2023 2024
###############################################################################


###############################################################################
# Voltage bounds that will really been used
###############################################################################

# Negative value for substation_Vmin or substation_Vmax means that the value is undefined
# In that case, minimal_voltage_lower_bound or maximal_voltage_upper_bound is used instead

# Note that low and high override are taken into account only if
# substation_new_Vmin > minimal_voltage_lower_bound and substation_new_Vmax < maximal_voltage_upper_bound

param voltage_lower_bound{(t,s) in SUBSTATIONS} :=
  max( minimal_voltage_lower_bound,
       if s in BOUND_OVERRIDES then substation_new_Vmin[s] else substation_Vmin[t,s]
      );

param voltage_upper_bound{(t,s) in SUBSTATIONS} :=
  if s in BOUND_OVERRIDES and substation_new_Vmax[s] <= voltage_lower_bound[t,s] then maximal_voltage_upper_bound
  else if s in BOUND_OVERRIDES then min(maximal_voltage_upper_bound,substation_new_Vmax[s])
  else if substation_Vmax[t,s] <= voltage_lower_bound[t,s] then maximal_voltage_upper_bound
  else min(maximal_voltage_upper_bound,substation_Vmax[t,s]);

check {(t,s) in SUBSTATIONS}: voltage_lower_bound[t,s] < voltage_upper_bound[t,s];

# Elements in main connex component
set BUS2:= setof {(1,n) in BUS:
  bus_CC[1,n] == 0
  and n >= 0
  and substation_Vnomi[1,bus_substation[1,n]] >= epsilon_nominal_voltage
  } n;
set BRANCH2:= setof {(1,qq,m,n) in BRANCH: m in BUS2 and n in BUS2} (qq,m,n);

set BUSCC dimen 1 default {};
# Branches with bus on side 1 and 2 in CC
set BRANCHCC  := {(qq,m,n) in BRANCH2: m in BUSCC and n in BUSCC};
# Branches with disconnected bus on side 2
set BRANCHCC_WITH_SIDE_2_OPENED := setof {(1,qq,m,n) in BRANCH: m in BUSCC and n == -1 and m != n} (qq,m,n);
# Branches with disconnected bus on side 1
set BRANCHCC_WITH_SIDE_1_OPENED := setof {(1,qq,m,n) in BRANCH: m == -1 and n in BUSCC and m != n} (qq,m,n);
set ALL_BRANCHCC := BRANCHCC union BRANCHCC_WITH_SIDE_2_OPENED union BRANCHCC_WITH_SIDE_1_OPENED;


###############################################################################
# Maximum flows on branches
###############################################################################
param Fmax{(qq,m,n) in BRANCHCC} :=
  1.732 * 0.001
  * max(substation_Vnomi[1,bus_substation[1,m]]*abs(branch_patl1[1,qq,m,n]),substation_Vnomi[1,bus_substation[1,n]]*abs(branch_patl2[1,qq,m,n]));

set LOADCC    := setof {(1,c,n) in LOAD    : n in BUSCC} (c,n);
set UNITCC    := setof {(1,g,n) in UNIT    : n in BUSCC} (g,n);
set BATTERYCC := setof {(1,b,n) in BATTERY : n in BUSCC} (b,n);


# Units up and generating:
# Warning: units with Ptarget=0 are considered as out of order
set UNITON := {(g,n) in UNITCC : abs(unit_Pc[1,g,n]) >= Pnull};

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

# Branches with zero or near zero impedances
# Notice: module of Z is equal to square root of (R^2+X^2)
set BRANCHZNULL := {(qq,m,n) in ALL_BRANCHCC: branch_R[1,qq,m,n]^2+branch_X[1,qq,m,n]^2 <= Znull^2};


# If in BRANCHZNULL, then set X to ZNULL
param branch_X_mod{(qq,m,n) in ALL_BRANCHCC} :=
  if (qq,m,n) in BRANCHZNULL then Znull
  else branch_X[1,qq,m,n];
check {(qq,m,n) in ALL_BRANCHCC}: abs(branch_X_mod[qq,m,n]) > 0;

# If in BRANCHZNULL, then set Gor/Gex/Bor/Bex to 0
param branch_Gor_mod{(qq,m,n) in ALL_BRANCHCC} :=
    if (qq,m,n) in BRANCHCC and (qq,m,n) in BRANCHZNULL then 0
    else branch_Gor[1,qq,m,n];

param branch_Gex_mod{(qq,m,n) in ALL_BRANCHCC} :=
    if (qq,m,n) in BRANCHCC and (qq,m,n) in BRANCHZNULL then 0
    else branch_Gex[1,qq,m,n];

param branch_Bor_mod{(qq,m,n) in ALL_BRANCHCC} :=
    if (qq,m,n) in BRANCHCC and (qq,m,n) in BRANCHZNULL then 0
    else branch_Bor[1,qq,m,n];

param branch_Bex_mod{(qq,m,n) in ALL_BRANCHCC} :=
    if (qq,m,n) in BRANCHCC and (qq,m,n) in BRANCHZNULL then 0
    else branch_Bex[1,qq,m,n];

# Busses with valid voltage value
set BUSVV := {n in BUSCC : bus_V0[1,n] >= min_plausible_low_voltage_limit};

# Reactive
set SHUNTCC := {(1,s,n) in SHUNT: n in BUSCC or shunt_possiblebus[1,s,n] in BUSCC}; # We want to be able to reconnect shunts
set BRANCHCC_REGL := {(qq,m,n) in BRANCHCC union BRANCHCC_WITH_SIDE_2_OPENED diff BRANCHZNULL: branch_ptrRegl[1,qq,m,n] != -1 }; # ratio tap changers have impact on lines with side 1 opened
set BRANCHCC_DEPH := {(qq,m,n) in BRANCHCC diff BRANCHZNULL: branch_ptrDeph[1,qq,m,n] != -1 };
set SVCCC   := {(1,svc,n) in SVC: n in BUSCC};

#
# Control parameters for SVC
#
# Simple: if regul==true then SVC is on, else it is off
set SVCON := {(svc,n) in SVCCC: svc_vregul[1,svc,n]=="true" and svc_bmin[1,svc,n]<=svc_bmax[1,svc,n]-Pnull/base100MVA};

#
# Control parameters for shunts
# If no shunt in PARAM_SHUNT, it means that hey are all fixed
#
# Variable shunts
# Shunts wich are not connected (n=-1) but which are in PARAM_SHUNT are considered as connected to their possible bus, with variable reactance
set SHUNT_VAR := setof {(1,s,n) in SHUNT :
  s in PARAM_SHUNT
  and (s,n) in SHUNTCC # Remember n might be -1 if shunt_possiblebus[1,s,n] is in BUSCC
  and abs(shunt_valmin[1,s,n])+abs(shunt_valmax[1,s,n]) >= Pnull / base100MVA # Useless to allow change if values are too small
  } (s,shunt_possiblebus[1,s,n]);
# Shunts with fixed values
set SHUNT_FIX := setof {(1,s,n) in SHUNT: s not in PARAM_SHUNT and n in BUSCC} (s,n);
# If a shunt is not connected (n=-1) and it is not in PARAM_SHUNT, then it will not be
# reconnected by reactive opf. These shunts are not in SHUNT_VAR nor in SHUNT_FIX; they
# are simply ignored

#
# Control parameters for reactive power of units
#
# If unit_Qc is not consistent, then reactive power will be a variable
set UNIT_FIXQ := {(g,n) in UNITON: g in PARAM_UNIT_FIXQ and abs(unit_Qc[1,g,n])<PQmax };

#
# Control parameters for ratios of transformers
#
set BRANCHCC_REGL_VAR :=
  { (qq,m,n) in BRANCHCC_REGL:
    qq in PARAM_TRANSFORMERS_RATIO_VARIABLE
    and (qq,m,n) not in BRANCHCC_WITH_SIDE_2_OPENED # ratio tap changers on branch with side 2 opened are not optimized
    and regl_ratio_min[1,branch_ptrRegl[1,qq,m,n]] < regl_ratio_max[1,branch_ptrRegl[1,qq,m,n]]
  };
set BRANCHCC_REGL_FIX := BRANCHCC_REGL diff BRANCHCC_REGL_VAR;


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

param branch_angper{(qq,m,n) in ALL_BRANCHCC} =
  if (qq,m,n) in BRANCHCC_DEPH
  then atan2(branch_Rdeph[qq,m,n], branch_Xdeph[qq,m,n])
  else atan2(branch_R[1,qq,m,n]  , branch_X_mod[qq,m,n]  );

param branch_admi {(qq,m,n) in ALL_BRANCHCC} =
  if (qq,m,n) in BRANCHCC_DEPH
  then 1./sqrt(branch_Rdeph[qq,m,n]^2 + branch_Xdeph[qq,m,n]^2 )
  else 1./sqrt(branch_R[1,qq,m,n]^2   + branch_X_mod[qq,m,n]^2   );

# Later in this file, a variable branch_Ror_var will be created, to replace branch_Ror when it is not variable
param branch_Ror {(qq,m,n) in ALL_BRANCHCC} =
    ( if ((qq,m,n) in BRANCHCC_REGL)
      then tap_ratio[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]]
      else 1.0
    )
  * ( if ((qq,m,n) in BRANCHCC_DEPH)
      then tap_ratio[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]]
      else 1.0
    )
  * (branch_cstratio[1,qq,m,n]);
param branch_Rex {(q,m,n) in ALL_BRANCHCC} = 1; # In IIDM, everything is in bus1 so ratio at bus2 is always 1

param branch_dephor {(qq,m,n) in ALL_BRANCHCC} =
  if ((qq,m,n) in BRANCHCC_DEPH)
  then tap_angle [1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]]
  else 0;
param branch_dephex {(qq,m,n) in ALL_BRANCHCC} = 0; # In IIDM, everything is in bus1 so dephase at bus2 is always 0


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
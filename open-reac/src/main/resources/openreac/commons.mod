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
# Author:  Jean Maeght   2022 2023
# Author:  Manuel Ruiz   2023 2024
# Author:  Oscar Lamolet 2025 2026
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
# Branches with bus on side 1 in CC, and disconnected bus on side 2
set BRANCHCC_WITH_SIDE_2_OPENED := setof {(1,qq,m,n) in BRANCH: m in BUSCC and n == -1 and m != n} (qq,m,n);
# Branches with bus on side 2 in CC, and disconnected bus on side 1
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

# Active and reactive targets of converter stations
# Warning: the losses are ignored
set LCCCONV_NUM := setof{(t,lcc,bus) in LCCCONV}lcc;
set VSCCONV_NUM := setof{(t,vsc,bus) in VSCCONV}vsc;
param lccconv_targetP {LCCCONV_NUM};
param vscconv_targetP {VSCCONV_NUM};
for {(1,h) in HVDC} {
  # case of VSC converter stations
  if (hvdc_type[1,h] == 1) then {
    if (hvdc_convertersMode[1,h] == "SIDE_1_RECTIFIER_SIDE_2_INVERTER") then {
      let vscconv_targetP[hvdc_conv1[1,h]] := hvdc_targetP[1,h];
      let vscconv_targetP[hvdc_conv2[1,h]] := -hvdc_targetP[1,h];
    } else {
      let vscconv_targetP[hvdc_conv1[1,h]] := -hvdc_targetP[1,h];
      let vscconv_targetP[hvdc_conv2[1,h]] := hvdc_targetP[1,h];
    }
  }
  # case of LCC converter stations
  if (hvdc_type[1,h] == 2) then {
    if (hvdc_convertersMode[1,h] == "SIDE_1_RECTIFIER_SIDE_2_INVERTER") then {
      let lccconv_targetP[hvdc_conv1[1,h]] := hvdc_targetP[1,h];
      let lccconv_targetP[hvdc_conv2[1,h]] := -hvdc_targetP[1,h];
    } else {
      let lccconv_targetP[hvdc_conv1[1,h]] := -hvdc_targetP[1,h];
      let lccconv_targetP[hvdc_conv2[1,h]] := hvdc_targetP[1,h];
    }
  }
}
check {lcc in LCCCONV_NUM}: lccconv_targetP[lcc] != NaN;
check {vsc in VSCCONV_NUM}: vscconv_targetP[vsc] != NaN;

#
# VSC converter stations
#
set VSCCONVON := setof{(t,v,n) in VSCCONV:
  n in BUSCC
  and abs(vscconv_targetP[v])  <= PQmax
  and abs(vscconv_Pmin[t,v,n]) <= PQmax
  and abs(vscconv_Pmax[t,v,n]) <= PQmax
  and vscconv_targetP[v] >= vscconv_Pmin[t,v,n]
  and vscconv_targetP[v] <= vscconv_Pmax[t,v,n]
  } (v,n);

#
# LCC converter stations
#
set LCCCONVON := setof{(t,l,n) in LCCCONV:
  n in BUSCC
  and abs(lccconv_targetP[l]) <= PQmax
  and abs(lccconv_q0[1,l,n])  <= PQmax
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

# If in BRANCHZNULL and Vnom1 != Vnom2, then set Gor/Gex/Bor/Bex to 0
param branch_Gor_mod{(qq,m,n) in ALL_BRANCHCC} :=
    if (qq,m,n) in BRANCHCC and (qq,m,n) in BRANCHZNULL and substation_Vnomi[1,bus_substation[1,m]] != substation_Vnomi[1,bus_substation[1,n]] then 0
    else branch_Gor[1,qq,m,n];

param branch_Gex_mod{(qq,m,n) in ALL_BRANCHCC} :=
    if (qq,m,n) in BRANCHCC and (qq,m,n) in BRANCHZNULL and substation_Vnomi[1,bus_substation[1,m]] != substation_Vnomi[1,bus_substation[1,n]] then 0
    else branch_Gex[1,qq,m,n];

param branch_Bor_mod{(qq,m,n) in ALL_BRANCHCC} :=
    if (qq,m,n) in BRANCHCC and (qq,m,n) in BRANCHZNULL and substation_Vnomi[1,bus_substation[1,m]] != substation_Vnomi[1,bus_substation[1,n]] then 0
    else branch_Bor[1,qq,m,n];

param branch_Bex_mod{(qq,m,n) in ALL_BRANCHCC} :=
    if (qq,m,n) in BRANCHCC and (qq,m,n) in BRANCHZNULL and substation_Vnomi[1,bus_substation[1,m]] != substation_Vnomi[1,bus_substation[1,n]] then 0
    else branch_Bex[1,qq,m,n];

# Busses with valid voltage value
set BUSVV := {n in BUSCC : bus_V0[1,n] >= min_plausible_low_voltage_limit};

# Reactive
set SHUNTCC := {(1,s,n) in SHUNT: n in BUSCC or shunt_possiblebus[1,s,n] in BUSCC}; # We want to be able to reconnect shunts
set BRANCHCC_REGL := {(qq,m,n) in BRANCHCC union BRANCHCC_WITH_SIDE_2_OPENED diff BRANCHZNULL: branch_ptrRegl[1,qq,m,n] != -1 }; # ratio tap changers also have impact on lines with side 2 open
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
    and (qq,m,n) not in BRANCHCC_WITH_SIDE_2_OPENED # ratio tap changers on open branches are not optimized
    and regl_ratio_min[1,branch_ptrRegl[1,qq,m,n]] < regl_ratio_max[1,branch_ptrRegl[1,qq,m,n]]
  };
set BRANCHCC_REGL_FIX := BRANCHCC_REGL diff BRANCHCC_REGL_VAR;


###############################################################################
# Parallel transformer bundles (shared ratio)
###############################################################################
# Bundles of parallel transformers are detected topologically on the Java side and passed
# as a membership + orientation relation (num_bundle, num_branch, orientation): +1 for a
# member declared in the bundle's canonical direction (that of its first member in id
# order), -1 for a member declared in the opposite direction. EVERYTHING numeric is derived
# here, once, from AMPL's own data: the per-member effective rho bounds, the bundle
# intersection, the LARGE / POINT / EMPTY qualification, the shared-ratio variable bounds
# and the fixed targets. Having a single authority means the qualification and the
# constraints can never disagree (no Java<->AMPL text round-trip on cstratio and tap ratios,
# which used to trip the solver presolve by a few 1e-6).
#
# Everything lives in EFFECTIVE-ratio space: the ratio entering the flow equations is
# branch_Ror_var * branch_cstratio, and circulating flows between parallel branches are
# driven by mismatches of this effective quantity. Bundle-level quantities (bounds, centre,
# shared variable) are expressed in the bundle's CANONICAL direction; a reversed member
# maps to that space by inversion (its declared range [lo, hi] becomes [1/hi, 1/lo]).

# Branch nums the optimization can actually move this run.
set BRANCHCC_REGL_VAR_NUM := setof {(qq,m,n) in BRANCHCC_REGL_VAR} qq;

set PARALLEL_BUNDLES_ALL := setof {(g,qq) in PARAM_PARALLEL_TRANSFORMERS} g;

# Width below which an effective intersection is treated as degenerate (a single point, or
# empty). Single source of truth, formerly the Java RHO_INTERSECTION_EPSILON.
param parallel_rho_intersection_epsilon := 1e-4;

# Every member num of every bundle (scopes the per-member bounds below).
set PARALLEL_MEMBER_NUMS := setof {(g,qq) in PARAM_PARALLEL_TRANSFORMERS} qq;

# Bundle a member belongs to, and its orientation relative to that bundle's canonical
# direction (+1 direct, -1 reversed), straight from the detection file. Membership is a
# function (bundles are disjoint), so the max over the singleton simply selects it. Defined
# once for every member and reused by both the tied and the fixed constraints.
param parallel_bundle_of_member{qq in PARALLEL_MEMBER_NUMS} :=
  max {(g,qqq) in PARAM_PARALLEL_TRANSFORMERS: qqq == qq} g;
param parallel_member_orientation{qq in PARALLEL_MEMBER_NUMS} :=
  param_parallel_transformers_orientation[parallel_bundle_of_member[qq],qq];

# Effective rho bounds of a member in its DECLARED direction, over the connected-component
# RTC branches (where branch_cstratio and the tap tables are defined):
#  - a user-variable member (its num is in PARAM_TRANSFORMERS_RATIO_VARIABLE) spans its full
#    tap range [cstratio * regl_ratio_min, cstratio * regl_ratio_max];
#  - a user-fixed member cannot be moved and is pinned to its current tap, i.e. the single
#    point cstratio * tap_ratio(current tap) -- exactly the value AMPL otherwise freezes it at.
param parallel_declared_rho_lo{(qq,m,n) in BRANCHCC_REGL: qq in PARALLEL_MEMBER_NUMS} :=
  branch_cstratio[1,qq,m,n] *
  (if qq in PARAM_TRANSFORMERS_RATIO_VARIABLE
     then regl_ratio_min[1,branch_ptrRegl[1,qq,m,n]]
     else tap_ratio[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]]);
param parallel_declared_rho_hi{(qq,m,n) in BRANCHCC_REGL: qq in PARALLEL_MEMBER_NUMS} :=
  branch_cstratio[1,qq,m,n] *
  (if qq in PARAM_TRANSFORMERS_RATIO_VARIABLE
     then regl_ratio_max[1,branch_ptrRegl[1,qq,m,n]]
     else tap_ratio[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]]);

# Effective rho bounds of a bundle = intersection of its members' effective ranges, expressed
# in the bundle's CANONICAL direction: a direct member (orientation +1) contributes its
# declared range as-is; a reversed member (orientation -1) transforms in the opposite
# declared direction, so it contributes the INVERSE of its declared range, bounds swapped
# ([lo, hi] -> [1/hi, 1/lo]). This reproduces the former Java analysis member by member.
# A member outside BRANCHCC_REGL (near-zero impedance, out of the main component) contributes
# nothing here; such a member necessarily fails the tie guard below, so its bundle is
# released regardless of these bounds.
param parallel_bundle_rho_min{g in PARALLEL_BUNDLES_ALL} :=
  max {(gg,qq) in PARAM_PARALLEL_TRANSFORMERS, (qq2,m,n) in BRANCHCC_REGL: gg == g and qq2 == qq}
    (if param_parallel_transformers_orientation[gg,qq] == 1
       then parallel_declared_rho_lo[qq2,m,n]
       else 1 / parallel_declared_rho_hi[qq2,m,n]);
param parallel_bundle_rho_max{g in PARALLEL_BUNDLES_ALL} :=
  min {(gg,qq) in PARAM_PARALLEL_TRANSFORMERS, (qq2,m,n) in BRANCHCC_REGL: gg == g and qq2 == qq}
    (if param_parallel_transformers_orientation[gg,qq] == 1
       then parallel_declared_rho_hi[qq2,m,n]
       else 1 / parallel_declared_rho_lo[qq2,m,n]);

# LARGE: the effective intersection is wider than epsilon (a usable shared interval).
# Otherwise the bundle is degenerate (POINT/EMPTY) and its members are fixed (see below).
set PARALLEL_BUNDLES_LARGE := {g in PARALLEL_BUNDLES_ALL:
  parallel_bundle_rho_max[g] - parallel_bundle_rho_min[g] > parallel_rho_intersection_epsilon};

# A bundle is tie-able only if EVERY member is a variable-ratio branch this run. A member can
# be silently demoted to fixed by the model (zero impedance, out of the main connected
# component, single-tap table, side opened, ...), which the topological detection cannot
# foresee; in that case the bundle is not tied (defensive guard against a silent partial tie).
set PARALLEL_BUNDLES_GUARDED := {g in PARALLEL_BUNDLES_ALL:
  card({(gg,qq) in PARAM_PARALLEL_TRANSFORMERS: gg == g and qq not in BRANCHCC_REGL_VAR_NUM}) == 0};

# Tied bundles: a wide intersection and every member movable this run.
set PARALLEL_BUNDLES := PARALLEL_BUNDLES_LARGE inter PARALLEL_BUNDLES_GUARDED;
# Released bundles: a wide intersection would let us tie them, but a member cannot be moved
# this run, so nothing is tied and the run optimizes them independently (logged in acopf.run).
set PARALLEL_BUNDLES_DROPPED := PARALLEL_BUNDLES_LARGE diff PARALLEL_BUNDLES_GUARDED;
# Degenerate bundles (POINT/EMPTY): their movable members are fixed, member by member, below.
set PARALLEL_BUNDLES_FIXED := PARALLEL_BUNDLES_ALL diff PARALLEL_BUNDLES_LARGE;

set PARALLEL_BRANCHES := setof {(g,qq) in PARAM_PARALLEL_TRANSFORMERS: g in PARALLEL_BUNDLES} qq;

# Members of a degenerate bundle that are still movable this run: each is fixed to the gap
# centre (mapped back to the member's declared direction, i.e. inverted for a reversed
# member) clamped into its own declared effective domain (see ctr_fixed_ratio in acopf.mod).
# User-fixed members are already frozen at their current tap by the model and need no
# constraint.
set PARALLEL_FIXED_BRANCHES := setof
  {(g,qq) in PARAM_PARALLEL_TRANSFORMERS, (qq2,m,n) in BRANCHCC_REGL_VAR: g in PARALLEL_BUNDLES_FIXED and qq2 == qq} qq;
# Common target effective ratio of a degenerate bundle, in the CANONICAL direction: the
# centre of its (point or empty) intersection. ctr_fixed_ratio maps it to each member's
# declared direction and clamps it into the member's own declared effective range.
param parallel_bundle_center{g in PARALLEL_BUNDLES_FIXED} :=
  (parallel_bundle_rho_min[g] + parallel_bundle_rho_max[g]) / 2;


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
    ( if (branch_ptrRegl[1,qq,m,n] != -1)
      then tap_ratio[1,regl_table[1,branch_ptrRegl[1,qq,m,n]],regl_tap0[1,branch_ptrRegl[1,qq,m,n]]]
      else 1.0
    )
  * ( if (branch_ptrDeph[1,qq,m,n] != -1)
      then tap_ratio[1,deph_table[1,branch_ptrDeph[1,qq,m,n]],deph_tap0[1,branch_ptrDeph[1,qq,m,n]]]
      else 1.0
    )
  * (branch_cstratio[1,qq,m,n]);
param branch_Rex {(q,m,n) in ALL_BRANCHCC} = 1; # In IIDM, everything is in bus1 so ratio at bus2 is always 1

param branch_dephor {(qq,m,n) in ALL_BRANCHCC} =
  if (branch_ptrDeph[1,qq,m,n] != -1)
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
param corrected_unit_qp0  {UNITON} default defaultQmin;
param corrected_unit_QP  {UNITON} default defaultQmax;
param corrected_unit_Qp  {UNITON} default defaultQmax;
param corrected_unit_Qp0  {UNITON} default defaultQmax;
param corrected_unit_Qmin{UNITON} default defaultQmin;
param corrected_unit_Qmax{UNITON} default defaultQmax;

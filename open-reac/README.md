# OpenReac

OpenReac is a reactive optimal power flow that gives a set of hypotheses
for voltage and reactive controls by network equipments such as
generators, shunt compensators and transformers. OpenReac can be used
for network planning or in operation as well.

---

## Getting started

### Knitro

To run this model, in addition of AMPL you'll need Knitro. Knitro is a
proprietary non-linear solver.

Artelys is the company developping Knitro. It is distributing keys
[here](https://www.artelys.com/solvers/knitro/).

After the installation is done and that you got a valid licence, you
must have `knitroampl` in your path.

To check, start a bash and run :

```bash
knitroampl stub
```

---

## Reactive Optimal Power Flow

### 1 Overview

The reactive Optimal Power Flow (OPF) is implemented with AMPL. Its goal is to propose values
for all voltage and reactive equipment and controllers of the grid
(voltage set point of generating units, shunts, transformers ratios...).

In a grid development study, you decide new equipments, new generating units,
new substations, new loads, you set values for active and reactive loads,
you set values for active power generation and HVDC flows.
Then if you wish to do AC powerflow simulations with N-1 analysis, you need
all voltage and reactive set points and this reactive OPF is your solution.

Please notice that this reactive OPF:
- does **not** decide active power of generating units and HVDC branches,
- does **not** take into account current nor power limits on branches,
- **use** upper and lower limits for voltage, so be careful with them.


### 2 Division of the code

The code of the reactive OPF is divided into several files, 
each serving a specific function:
- `reactiveopf.dat` defines the network data files imported (with
  prefix *ampl_*), and the files used to configure the run (with prefix *param_*).
Refer to section 3 for more information.
- `reactiveopf.mod` defines the sets, parameters and optimization problems (CC, DCOPF, ACOPF). 
Refer to section 4,5, and 6 for more information.
- `reactiveopfoutput.mod` exports result files if
  the solving of optimization problems converges. 
Refer to section 7.1 for more information.
- `reactiveopfexit.run` contains the code executed when the AMPL run fails. 
Refer to section 7.2 for more information.
- `reactiveopf.run` executes the AMPL process of OpenReac, calling the previous scripts.


### 3 Input

#### 3.1 Network data


Text files with the prefix `ampl_` contain the 
data/parameters of the network on which the OPF is executed. 
These files can be obtained by using the [AMPL export](https://github.com/powsybl/powsybl-core/tree/main/ampl-converter)
of [powsybl-core](https://github.com/powsybl/powsybl-core/).

TODO : more details on the content of the files ?

#### 3.2 Fixed VS Variable values

TODO : which elements are fixed, which are variables

TODO : what control has the user (which elements can be fixed or put as variables), with which files

#### 3.3 Checks and special handling

TODO : add comments on transformation ratios/voltage level checks

TODO : add comments on voltage level limits handling

TODO : add comments on correction of P/Q units' domain

---
### 4 Reference bus and main connex component


A _reference bus_ (`null_phase_bus` parameter) is determined to enforce the zero-phase constraint of the OPFs. 
This reference bus corresponds to the bus in the network with the most AC branches connected,
among those belonging to the main connected component (`bus_CC = 0`). 
If multiple buses have the same maximum cardinality, the one with the highest `num` is selected.
If no bus is found meeting these criteria, the bus with the lowest `num` in the network is chosen.

The DCOPF and ACOPF are executed on buses connected to the reference bus by AC branches.
Then, buses connected to the reference bus by HVDC lines are excluded in OPF computation.
These buses are determined by solving the `PROBLEM_CCOMP` optimization problem.
After the optimization, buses connected by AC branches are determined by verifying
that the associated variable `teta_ccomputation` is set to 0.

### 5 Direct Current Optimal Power Flow

Before solving the reactive ACOPF, a DCOPF is solved for two main reasons:
- If the DCOPF resolution fails, it provides a strong indication that the ACOPF resolution will also fail. 
Therefore, the DCOPF serves as a formal consistency check on the data.
- The phases computed during the DCOPF resolution 
will be used as initial points for the ACOPF resolution.

The DCOPF involves the following constraints:
- `ctr_null_phase_bus_dc`, which sets the phase of the reference bus to 0.
- `ctr_activeflow`, which defines the active power flowing through the network's branches.
- `ctr_balance`, which enforces the active power balance at each network node.
This balance takes into account the active powers generated/consumed 
by various devices connected to the nodes. 
Within this balance, the following elements are considered as variables:
    - The active power generated by the generating units (`UNITON` set).
    - The slack variables `balance_pos` and `balance_neg`, which represent 
  the excess or shortfall of active power produced at each node.

And the objective function `problem_dcopf_objective`, which minimizes the following summations:
- The sum of squared deviations between the calculated 
active power generation for each generator and its target active power (`unit_Pc` parameter).
This sum is normalized by the target active power, 
which helps homogenize the deviations among different generators.
- The sum of the variables `balance_pos` and `balance_neg`, penalized by a high coefficient.
The goal is to drive these variables towards 0, ensuring an active power balance at each node.

### 6 Alternative Current Optimal Power Flow

After solving the DCOPF, the calculated phases are used 
to initialize the phases in the reactive ACOPF.

This OPF depends on specific user-selected parameters, 
including the OPF objective function (see Section 1.2)
and the equipment with variable or fixed values (see Section 1.2).

The reactive ACOPF involves the following constraints :
- `ctr_null_phase_bus`, which sets the phase of the reference bus to 0.
- `ctr_balance_P`, which enforces the active power balance at each node of the network. 
It takes into account the active powers generated/consumed by various devices connected
to the nodes.
- `ctr_balance_Q` enforces the reactive power balance at each node of the network. 
It considers the reactive powers generated/consumed by various devices connected to the nodes. 
Within this balance, the following elements are considered as variables:
  - The transformation ratios of transformers defined as variables by the user (`BRANCHCC_REGL_VAR` set).
  - The reactive power generated by the generating units defined as variables by the user 
(`UNITON diff UNIT_FIXQ` set).
  - The susceptance of shunts defined as variables by the user (`SHUNT_VAR` set).
  - The reactive power generated by SVCs (`SVCON` set, containing SVCs with `svc_vregul = true`).
  - The reactive power generated by VSCs (`VSCCONVON` set).
  - The slack variables `slack1_balance_Q` and `slack2_balance_Q`,
which represent the excess or shortfall of active power produced at each node. 
This applies to nodes with a load or a shunt but no unit, 
SVC, or VSC (where the reactive power is already defined).

And the objective function `problem_acopf_objective` which minimizes the following sums 
(with certain coefficients that determine their relative importance):
  - The sum of `slack1_balance_Q` and `slack2_balance_Q` variables, penalized by a high coefficient. 
The objective is to drive these variables towards 0, ensuring a balance in reactive power at each node.
  - The sum of squared barycenter between the active power generated by each generator and 
the difference between this active power and the generator's target P.
This sum is penalized with a significant coefficient only when `objective_choice = 0`.
This barycenter depends on the `coeff_alpha` weight, which can be chosen by the user. 
The closer this coefficient is to 1, the more important the first term of the barycenter, 
thus emphasizing the minimization of generated active power. 
A coefficient closer to 1 increases the deviation between this active power and the generator's target P 
(`unit_Pc` parameter).
  - The sum of squared deviations between the calculated voltage values at each node and a 
barycenter between the lower and upper voltage limits of the associated voltage level.
This sum is penalized with a significant coefficient only when `objective_choice = 1`.
This barycenter depends on the `ratio_voltage_target` weight,
which can be chosen by the user.
  - The sum of squared deviations between the calculated voltage values and their initial values at each node.
    This sum is penalized with a significant coefficient only when `objective_choice = 2`.
  - The sum of squared deviations of variable transformation ratios from their initial values. 
This sum is penalized by a small coefficient. The goal is to limit this deviation without overly restricting it.
  - The sum of squared ratios of reactive powers generated by generating units at 
their maximal reactive power bounds.
This sum is penalized by a small coefficient. The goal is to limit this deviation without overly restricting it.

TODO : add comments on results treatment by Knitro (what kind of solutions are considered as good...)
    
### 7 Output

#### 7.1 In case of convergence

If the solving of DCOPF and reactive ACOPF is successful,
the following files are exported (containing variables calculated values):

- `reactiveopf_results_indic.txt`, which contains various indicators to provide an overview of the
  run. It includes:
  - General information (system OS, computation time, etc.).
  - The thresholds/parameters used (minimum impedance of lines
    considered as impedances, chosen objective function, etc.).
  - The cardinality of various sets (number of non-impedance branches,
    number of shunts with fixed values, etc.).
  - Information about calculated angles (maximum/minimum theta, maximum
    difference between neighboring buses, etc.).


- `reactiveopf_results_static_var_compensators.csv`, which contains calculated 
voltage and reactive power values for the SVC that regulate voltage :

  Format : 6 columns #"variant" "num" "bus" "vRegul" "V(pu)" "Q(Mvar)"


- `reactiveopf_results_shunts.csv`, which contains calculated reactive power (and susceptance) values 
for shunts that were either connected or modified after the optimization problems were resolved.

  Format : 6 columns #"variant" "num" "bus" "b(pu)" "Q(Mvar)" "section"


- `reactiveopf_results_generators.csv`, which contains 
calculated active and reactive power values for generating units.

  Format : 9 columns #"variant" "num" "bus" "vRegul" "V(pu)"
  "targetP(MW)" "targetQ(Mvar)" "P(MW)" "Q(MW)"


- `reactiveopf_results_vsc_converter_stations.csv`, which contains 
calculated reactive power values for VSC converter stations.

  Format : 8 columns #"variant" "num" "bus" "vRegul" "targetV(pu)"
  "targetQ(Mvar)" "P(MW)" "Q(Mvar)"


- `reactiveopf_results_rtc.csv`, which contains the RTCs and their associated taps,
with the transformation ratio closest to the one calculated after 
the optimization.

  Format : 3 columns #"variant" "num" "tap"


- `reactiveopf_results_reactive_slacks.csv`, which contains the calculated
reactive slack variables `slack1_balance_Q` and `slack2_balance_Q`.

  Format : 6 columns #"variant" "bus" "slack_condensator(Mvar)" "slack_self(Mvar)" "id" "substation"

#### 7.2 In case of error

If for any reason the execution of the file *reactiveopf.run* fails, 
the file *reactiveopfexit.run* is executed. Then, the following
file is exported:

- `reactiveopf_results_indic.txt`, which contains various indicators to 
provide an overview of the run. It includes:
  - The error message(s) returned by AMPL.
  - General information (system OS, computation time, etc.).
  - The thresholds/parameters used (minimum impedance of lines
    considered as impedances, chosen objective function, etc.).


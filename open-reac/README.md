# OpenReac

OpenReac is a reactive optimal power flow that gives a set of hypotheses
for voltage and reactive controls by network equipments such as
generators, shunt compensators and transformers. OpenReac can be used
for network planning or in operation as well.


## Getting started

---

### AMPL
For this project, you must have [AMPL](https://ampl.com/) installed.
AMPL is a proprietary tool that works as an optimization modelling language. It can be interfaced with many solvers.

AMPL is sold by many companies including Artelys, you can find keys [here](https://www.artelys.com/solvers/ampl/).

You must add in your `~/.itools/config.yml` an ampl section like this:
```yaml
ampl:
  # Change to the ampl folder path that contains the ampl executable
  homeDir: /home/user/ampl
```

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


## Reactive Optimal Power Flow

---

### 1 Overview

The reactive optimal power flow (OPF) is implemented with AMPL. Its goal is to propose values
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
- `reactiveopf.dat` defines the network data files imported (files with
  *ampl_* prefix), and the files used to configure the run (files with *param_* prefix).
  Refer to section [3](#3-input).
- `reactiveopf.mod` defines the sets, parameters and optimization problems (CC, DCOPF, ACOPF) solved in `reactiveopf.run`. 
  Refer to sections [5](#5-reference-bus--main-connex-component), [6](#6-direct-current-optimal-power-flow) and [7](#7-alternative-current-optimal-power-flow).
- `reactiveopfoutput.mod` exports result files if the execution of `reactiveopf.run` is successful. 
  Refer to section [8.1](#81-in-case-of-convergence).
- `reactiveopfexit.run` contains the code executed when the problem is inconsistent. 
  Refer to section [8.2](#82-in-case-of-inconsistency).
- `reactiveopf.run` executes the AMPL process of OpenReac, calling the previous scripts.

### 3 Input

#### 3.1 Network data

Files with the prefix `ampl_` contain the
data and the parameters of the network on which the reactive OPF is executed.
These files are obtained by using the
[AMPL export of PowSyBl](https://github.com/powsybl/powsybl-core/blob/main/ampl-converter/src/main/java/com/powsybl/ampl/converter/AmplNetworkWriter.java).

#### 3.2 Configuration of the run

The user can configure the run with the dedicated Java interface 
(see [OpenReacParameters](src/main/java/com/powsybl/openreac/parameters/input/OpenReacParameters.java)).
Specifically, the user can set various parameters and thresholds used in the preprocessing and modeling of the reactive OPF. 
These are specified in the file `param_algo.txt`:

| Parameter                        | Description                                                                                                                                                                       | Default value     | Possible value                              |
|----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|---------------------------------------------|
| log_level_ampl                   | Level of display for AMPL prints                                                                                                                                                  | INFO              | {DEBUG, INFO, WARNING, ERROR}               |
| log_level_knitro                 | Level of display for solver prints (see [AMPL documentation](https://dev.ampl.com/ampl/options.html)                                                                              | $1$               | ${0, 1, 2}$                                 |  
| objective_choice                 | Choice of the objective function for the ACOPF (see [7](#7-alternative-current-optimal-power-flow))                                                                               | $0$               | ${0, 1, 2}$                                 |
| ratio_voltage_target             | Ratio to calculate target V of buses when objective_choice = 1                                                                                                                    | $0.5$             | $\[0; 1\]$                                  |
| coeff_alpha                      | Weight to favor more/less minimization of active power produced by generators or deviation between them and target values (see [6.2](#62-alternative-current-optimal-power-flow)) | $1$               | $\[0; 1\]$                                  |
| Pnull                            | Threshold of active and reactive powers considered as null                                                                                                                        | $0.01$ (MW)       | $\[0; 1\]$                                  |
| Znull                            | Threshold of impedance considered as null (see [4.2](#42-zero-impedance-lines))                                                                                                   | $10^{-5}$ (p.u.)  | $\[0; 0.1\]$                                |                                                                                                                                                                  
 | epsilon_nominal_voltage          | Threshold to ignore voltage levels with nominal voltage lower than it                                                                                                             | $1$ (kV)          | $\mathbb{R}^{+}$                            | 
| min_plausible_low_voltage_limit  | Consistency bound for low voltage limit of voltage levels (see [4.1](#41-voltage-level-limits-computation))                                                                       | $0.5$ (p.u.)      | $\mathbb{R}^{+}$                            |
| max_plausible_high_voltage_limit | Consistency bound for high voltage limit of voltage levels (see [4.1](#41-voltage-level-limits-computation))                                                                      | $1.5$ (p.u.)      | [min_plausible_low_voltage_limit; $\infty$] |
| ignore_voltage_bounds            | Threshold to replace voltage limits of voltage levels with nominal voltage lower than it, by  [min_plausible_low_voltage_limit; max_plausible_high_voltage_limit]                 | $0$ (p.u.)        | $\mathbb{R}^{+}$                            |
| buses_with_reactive_slacks       | Choice of which buses will have reactive slacks attached in ACOPF solving (see [7](#7-alternative-current-optimal-power-flow))                                                    | NO_GENERATION     | {CONFIGURED, NO_GENERATION, ALL}            |
| PQmax                            | Threshold for maximum active and reactive power considered in correction of generator limits  (see [4.5](#45-pq-units-domain))                                                    | $9000$ (MW, MVAr) |                                             |
| defaultPmax                      | Threshold for correction of high active power limit produced by generators (see [4.5](#45-pq-units-domain))                                                                       | $1000$ (MW)       |                                             |
| defaultPmin                      | Threshold for correction of low active power limit produced by generators (see [4.5](#45-pq-units-domain))                                                                        | $0$ (MW)          |                                             |
| defaultQmaxPmaxRatio             | Ratio used to calculate threshold for corrections of high/low reactive power limits (see [4.5](#45-pq-units-domain))                                                              | $0.3$ (MVAr/MW)   |                                             |
| minimalQPrange                   | Threshold to fix active (resp. reactive) power of generators with active (resp. reactive) power limits that are closer than it (see [4.5](#45-pq-units-domain))                   | $1$ (MW, MVAr)    |                                             |


In addition to the previous parameters, the user can specify which 
parameters will be variable or fixed in the ACOPF solving (see [7](#7-alternative-current-optimal-power-flow)).
This is done using the following files:

| File                                | Description                                                                                                                                            | Default behavior of modified values                                               |
|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| param_transformers.txt              | Ratio tap changers with a variable transformation ratio (real variable)                                                                                | Transformation ratios are fixed                                                   |
| param_shunt.txt                     | Shunts with a continuous variable susceptance and which can be modified and/or connected (only if possible bus is defined in `ampl_network_shunts.txt` | Shunt susceptances are fixed                                                      |
| param_generators_reactive.txt       | Generators with a constant reactive power production. If this value is not consistent (> PQmax), the reactive power production stays variable          | Coherent reactive power productions (see [4.5](#45-pq-units-domain)) are variable |
| param_buses_with_reactive_slack.txt | Buses with attached reactive slacks if configurable parameter buses_with_reactive_slacks = "CONFIGURED"                                                | Only buses with no reactive power production have reactive slacks attached        |    

All of these files share the same format: 2 columns #"num" "id".

#### 3.3 New voltage limits

In addition to the elements specified in section [3.2](#32-configuration-of-the-run), the user may choose to override
the voltage limits of specified voltage levels. These values are defined in `ampl_network_substations_override.txt` and
are employed to establish the new voltage limits as specified in section
[4.1](#41-voltage-level-limits-overrides). 
Format : 4 columns #"num" "minV (pu)" "maxV (pu)" "id"

### 4 Pre-processing

Before solving the reactive OPF described in [7](#7-alternative-current-optimal-power-flow), 
the following pre-processing blocks are executed to ensure the consistency of the values. 

#### 4.1 Voltage level limits overrides

In order to ensure consistent voltage level limits for the voltage levels,
the consistency thresholds  *minimal_voltage_lower_bound* and *maximal_voltage_upper_bound* are employed.
They are initialized as follows:
- $\text{minimal_voltage_lower_bound} = \max(\min\limits_{s\in \text{SUBSTATIONS}}(V_{min}^{s}), \text{min_plausible_low_voltage_limit})$
- $\text{maximal_voltage_upper_bound} = \min(\max\limits_{s\in \text{SUBSTATIONS}}(V_{max}^{s}), \text{max_plausible_high_voltage_limit})$

where $V_{min}$ (resp. $V_{max}$) is the low (resp. high) voltage limit of voltage level $s\in \text{SUBSTATIONS}$.



As a result, the lower voltage bound chosen is equal to the maximum value between
`minimal_voltage_lower_bound`
and the specified `minV (pu)` value in `ampl_network_substations.txt`. 
If an override value is specified by the user (see [3.3](#33-voltage-limits-overrides)), it replaces `minV (pu)`.

The upper voltage bound chosen is equal to the minimum value between `maximal_voltage_upper_bound`
and the specified `maxV (pu)` value in `ampl_network_substations.txt`.
If an override value is specified by the user (see [3.3](#33-voltage-limits-overrides)) and it is higher than
the previously calculated lower voltage bound, then the override value replaces `maxV (pu)`.

#### 4.2 Zero-impedance branches

Branches with an impedance magnitude (calculated in p.u.)
below the configurable threshold `Znull` (see section [3.2](#32-configuration-of-runtime))
are considered as non-impedant. 
These branches will have their reactance replaced by the threshold `Znull` (in p.u.).

#### 4.3 Impedance of transformers

In the calculations of the ACOPF (see [7](#7-alternative-current-optimal-power-flow)), 
the transformers with an impedance (the one specified in `ampl_network_branches.txt`)
considered as null (see [4.2](#42-zero-impedance-branches)) 
are treated as lines. Then, the transformation ratios/phase shifts are ignored, as well as the impedance
specified in the tap changer table `ampl_network_tct.txt`.

For phase shifters transformers considered as impedant, the reactance values from the tap changer table (in `ampl_network_tct.txt`)
replace the reactance specified in `ampl_network_branches.txt`. The resistance is then calculated proportionnaly to this reactance. 
For the ratio tap changers, the impedance remain as specified in `ampl_network_branches.txt`. Please notice there is no
specified handling for cases where resistances/reactances are negative or if there is both a ratio tap changer and a 
phase shift transformer on the same branch.

#### 4.5 P/Q units' domain

TODO : add
- `defaultQmaxPmaxRatio`: Parameter used to calculate  `defaultQmin` and `defaultQmax`,
  the thresholds used to correct the minimum and maximum reactive powers produced by generators
  (see section [4.5](#45-pq-units-domain)).
  The default value for this parameter is 0.3 (MVAr/MW), and the thresholds are calculated as follows:
  - `defaultQmin` = - `defaultPmin` x `defaultQmaxPmaxRatio`
  - `defaultQmax` =   `defaultPmax` x `defaultQmaxPmaxRatio`

### 5 Reference bus & main connex component

A reference bus (`null_phase_bus` AMPL parameter) enforces the zero-phase constraint of the OPFs. 
This reference bus corresponds to the bus in the network with the most AC branches connected,
among those belonging to the main connected component ($0$ in `ampl_network_buses.txt`). 
If multiple buses have the same maximum cardinality, the one with the highest `num` identifier is selected.
If no bus is found meeting these criteria, the first bus defined in the file `ampl_network_buses.txt` is selected.

The OPFs are executed on the main connex component (i.e. buses connected to the reference bus by AC branches).
Then, buses connected to the reference bus by HVDC lines are excluded.
The main connex component is determined by solving the `PROBLEM_CCOMP` optimization problem.

### 6 Direct current optimal power flow

Before to address the ACOPF (see [7](#7-alternative-current-optimal-power-flow)), a DCOPF is solved for two main reasons:
- If the DCOPF resolution fails, it provides a strong indication that the ACOPF resolution will also fail.
  The DCOPF serves as a formal consistency check on the data.
- The phases computed during the DCOPF resolution will be used as initial points for the ACOPF resolution.

The DCOPF involves the following constraints:
- `ctr_null_phase_bus_dc`, which sets the phase of the reference (refer to [5](#5-reference-bus--main-connex-component)) to 0.
- `ctr_activeflow`, which defines the active power flowing through the branches of the network.
- `ctr_balance`, which enforces the active power balance at each network node.
This balance takes into account the active power produced by generators and batteries, as well as the power consumed
by loads, VSC stations and LCC stations connected to each bus (in addition to what enters and exits the bus).
Within this balance, the following are variables:
    - The active power `P_dcopf` generated by the generating units.
    - The slack variables `balance_pos` and `balance_neg` (both positive), which represent 
  the excess or shortfall of active power produced at each node.

And the objective function `problem_dcopf_objective`, which minimizes the following summations:
- The sum of squared deviations between the calculated 
active power generation for each generator and its 
target active power (`targetP (MW)` defined in `ampl_network_generators.txt`).
This sum is normalized by the target active power, 
which helps homogenize the deviations among different generators.
- The sum of the variables `balance_pos` and `balance_neg`, penalized by a high coefficient (`penalty_balance`).
The goal is to drive these variables towards 0, ensuring an active power balance at each node.

The resolution of this DCOPF is considered as successful if the solver identifies a feasible solution without reaching
a default limit, and if the sum of all balance variables (`balance_pos` and `balance_neg`) does not exceed the configurable threshold `Pnull` 
(see [3.2](#32-configuration-of-the-run)). Otherwise, the solving is considered unsuccessful.

### 7 Alternative current optimal power flow

TODO : refactor

TODO :
add
- `buses_with_reactive_slacks`: The parameter determining which buses will have reactive slacks attached in the resolution of the ACOPF
(see [6.2](#62-alternative-current-optimal-power-flow)).
It can take the following values:
- "ALL": all buses have reactive slack variables attached.
- "NO_GENERATION": only buses not producing reactive power will have reactive slack variables attached.
- "CONFIGURED": only buses specified in `param_buses_with_reactive_slack.txt` will have reactive slack variables attached.

TODO : add
- `objective_choice` defining the choice of the objective function for the ACOPF (see [6.2](#62-alternative-current-optimal-power-flow)):
The minimization priority depends on the value of the parameter:
- If $0$, the active power produced by generators.
- If $1$, the deviation between the voltage value and the calculated target of the buses.
- If $2$, the deviation between the voltage value of the buses and the target V specified in `ampl_network_buses.txt`.

TODO : add
This target lies between the upper and lower voltage limits of the level voltages to which the buses are connected, and is calculated using
configurable parameter `ratio_voltage_target`.

This ACOPF relies on specific parameters selected by the user, 
as thresholds and equipment of the power network which will be treated as variable or fixed (refer to [3.2](#32-configuration-of-the-run)).
LThe voltage variables utilized in the ACOPF are initialized as follows:
- The phase angles are set equal to those calculated by solving the DCOPF.
- The voltages of buses without reactive slacks (and with nominal voltage greater than `min_plausible_low_voltage_limit`)
are set equal to the specified `v (pu)` values in `ampl_network_buses.txt`.
- The voltages of buses with reactive slacks are initialized to the midpoint of the voltage level limits to which they are connected.

The ACOPF involves the following constraints :
- `ctr_null_phase_bus`, which sets the phase of the reference bus (refer to [5](#5-reference-bus--main-connex-component)) to 0.
- `ctr_balance_P`, which enforces the active power balance at each node of the network.
  This balance takes into account the active power produced by generators and batteries, as well as the power consumed
  by loads, VSC stations and LCC stations connected to each bus (in addition to what enters and exits the bus).
Within this balance, the active power produced by units and the flows on the branches are considered as variables.
- `ctr_balance_Q` enforces the reactive power balance at each node of the network.
  This balance takes into account the reactive power produced by generators, batteries, shunts, static var compensators, VSC stations, 
as well as the power consumed by loads and LCC stations connected to each bus (in addition to what enters and exits the bus).
Within this balance, the following elements are considered as variables:
  - The flows on the branches (tensions, phases and transformation ratios of transformers defined as variables by the user).
  - The reactive power generated by the units defined as variables.
  - The susceptance of shunts defined as variables by the user.
  - The reactive power generated by SVCs (only the one with `svc_vregul` value equals to `true` in `ampl_network_static_var_compensators.txt`).
  - The reactive power generated by VSC stations (all consistent ones defined in `ampl_network_vsc_converter_stations.txt`).
  - The slack variables `slack1_balance_Q` and `slack2_balance_Q`,
which represent the excess or shortfall of active power produced at the buses chosen by the user.

And the objective function `problem_acopf_objective` which minimizes the following sums:
  - The sum of `slack1_balance_Q` and `slack2_balance_Q` variables, penalized by a very high coefficient (`penalty_invest_rea_pos`). 
The objective is to drive these variables towards 0, ensuring a balance in reactive power at each node.
  - The sum of squared barycenter between the two values.
The first is the active power produced by each generator. The second is 
the difference between this power and the unit's target P (`unit_Pc` parameter), divided by this target.
This sum is penalized with a significant coefficient only when `objective_choice = 0`.
This barycenter depends on the configurable parameter `coeff_alpha`. 
The closer this coefficient is to 1, the more important the first term of the barycenter, 
thus emphasizing the minimization of generated active power. 
A coefficient closer to 1 increases the deviation between this active power and the generator's target P 
(`targetP (MW)` defined in `ampl_network_generators.txt`).
  - The sum of squared deviations between the calculated voltage values at each node and a 
barycenter between the lower and upper voltage limits of the associated voltage level.
This sum is penalized with a significant coefficient only when `objective_choice = 1`.
This barycenter depends on the configurable parameter `ratio_voltage_target`.
  - The sum of squared deviations between the calculated voltage values and their initial values (`v (pu)` in `ampl_network_buses.txt`) at each node.
This sum is penalized with a significant coefficient only when `objective_choice = 2`.
  - The sum of squared deviations of variable transformation ratios from their initial values. 
This sum is penalized by a small coefficient. The goal is to limit this deviation without overly restricting it.
  - The sum of squared ratios of reactive powers generated by units over 
their maximal reactive power bounds.
This sum is penalized by a small coefficient. The goal is to limit this deviation without overly restricting it.

TODO : expliciter le fait qu'on fasse plusieurs optimisations à la suite en jouant sur alpha si besoin.

TODO : add comments on results treatment by Knitro (what kind of solutions are considered as good...)
    
### 8 Output

#### 8.1 In case of convergence

If the AMPL process defined in `reactiveopf.run` is successful, the script `reactiveopfoutput.run` is executed 
(even if the solving of ACOPF did not reached a feasible point) and the following files are exported:

| File                                            | Content                                                                                                                                                                                                                                                                                                                                                      | Format                                                                                          |
|-------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| reactiveopf_results_indic.txt                   | General information (system OS, computation time, etc.). The configurable thresholds/parameters (see section [3.2](#32-configuration-of-the-run)). The cardinality of the sets (number of non-impedance branches, number of slack variables, etc.). Information about calculated angles (maximum/minimum theta, maximum difference between neighbors, etc.). |                                                                                                 |
| reactiveopf_results_static_var_compensators.csv | Calculated voltage and reactive power values for the SVC that regulate voltage.                                                                                                                                                                                                                                                                              | 6 columns #"variant" "num" "bus" "vRegul" "V(pu)" "Q(MVAr)"                                     |
| reactiveopf_results_shunts.csv                  | Calculated reactive power (and susceptance) values for shunts that were either connected or modified after the optimization problems were resolved.                                                                                                                                                                                                          | 6 columns #"variant" "num" "bus" "b(pu)" "Q(MVAr)" "section"                                    |
| reactiveopf_results_generators.csv              | Calculated active and reactive power values for generating units.                                                                                                                                                                                                                                                                                            | 9 columns #"variant" "num" "bus" "vRegul" "V(pu)" "targetP(MW)" "targetQ(MVAr)" "P(MW)" "Q(MW)" |
| reactiveopf_results_vsc_converter_stations.csv  | Calculated reactive power values for VSC converter stations.                                                                                                                                                                                                                                                                                                 | 8 columns #"variant" "num" "bus" "vRegul" "targetV(pu)" "targetQ(MVAr)" "P(MW)" "Q(MVAr)"       |
| reactiveopf_results_rtc.csv                     | RTCs and associated taps, with transformer ratio closest to the one calculated after the optimization.                                                                                                                                                                                                                                                       | 3 columns #"variant" "num" "tap"                                                                |
| reactiveopf_results_reactive_slacks.csv         | Calculated reactive slack variables `slack1_balance_Q` and `slack2_balance_Q`.                                                                                                                                                                                                                                                                               | 6 columns #"variant" "bus" "slack_condensator(MVAr)" "slack_self(MVAr)" "id" "substation"       |

#### 8.2 In case of inconsistency

If the computation of the main connex component (see [5](#5-reference-bus--main-connex-component)) or of the DCOPF fails (see [6](#6-direct-current-optimal-power-flow)),
the problem is considered as inconsistent.
Then, the script `reactiveopfexit.run` is executed and the file reactiveopf_results_indic.txt described in previous section is exported, without the information on the calculated angles.

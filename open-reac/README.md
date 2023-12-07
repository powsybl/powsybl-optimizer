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

Files with the prefix `ampl_` contain the
data and the parameters of the network on which the reactive OPF is executed.
These files are obtained by using this [AMPL export](https://github.com/powsybl/powsybl-core/tree/main/ampl-converter).

#### 3.2 Configuration of the run

L'utilisateur peut également configurer certains éléments du run, à l'aide de l'interface
Java prévue à cet effet (see X). 

Il peut notamment configurer certains paramètres/seuils utilisés dans l'exécution, à 
l'aide du fichier `param_algo.txt`. On compte:
- `log_level_ampl`, le paramètre définissant le niveau d'affichage des prints AMPL. La valeur spécifiée 
par l'utilisateur doit être "DEBUG", "INFO", "WARNING" ou "ERROR". Ce paramètre vaut "INFO" par défaut.
- `log_level_knitro`, le paramètre définissant le niveau d'affichage des prints Knitro. La valeur spécifiée
par l'utilisateur doit être 0,1 ou 2, comme spécifié dans la documentation AMPL. Ce paramètre vaut 1 par défaut.
- `objective_choice`, le paramètre définissant le choix de la fonction objectif de l'ACOPF décrit section X:
  - Si la valeur spécifiée est 0, la minimization de la puissance active produite par les générateurs est privilégiée.
  - Si la valeur spécifiée est 1, la minimization de l'écart entre la valeur de tension et la target X des bus est privilégiée.
  - Si la valeur spécifiée est 2, la minimization de l'écart entre la valeur de tension et la target X des bus est privilégiée.
- `ratio_voltage_target`, le paramètre utilisé pour calculer la target des bus si le paramètre `objective_choice`
a une valeur de 1. Ce paramètre vaut 0.5 par défaut, et doit être compris entre 0 et 1.
- `coeff_alpha`, le paramètre utilisée dans la fonction objectif de l'ACOPF pour plus ou moins 
privilégier la minimization de la génération de puissance active des générateurs 
(`coeff_alpha`=1) ou des écarts entre les puissances actives calculées par l'ACOPF et 
la target value (`coeff_alpha`=0). Ce paramètre vaut 1 par défaut, et doit être compris 
entre 0 et 1.
- `Pnull` le paramètre définissant les puissances actives et réactives considérées comme nulles.
Ce paramètre vaut 0.01 (MW) par défaut, et doit être compris entre 0 et 1 strictement.
- `Znull` le paramètre utilisé pour déterminer quelles branches du réseau sont non-impédantes. 
Ce paramètre vaut 1e-4 (pu) par défaut, et doit être compris entre 0 et 0.1 strictement.
- `epsilon_nominal_voltage` le paramètre utilisée comme seuil de consistances pour la tension 
nominal des bus du réseau. Les bus ayant une tension nominale inférieure à ce paramètre seront ignorés.
Ce paramètre vaut 1 (kV) par défaut, et doit être supérieur à 0 strictement.
- `min_plausible_low_voltage_limit` le paramètre utilisé comme borne de consistence pour les tensions
minimales des différents voltage levels (voir section X). Ce paramètre vaut 0.5 par défaut, et doit
être supérieur à 0 strictement.
- `max_plausible_high_voltage_limit` le paramètre utilisé comme borne de consistence pour les tensions
maximales des différents voltage levels (voir section X). Ce paramètre vaut 1.5 par défaut, et doit
être supérieur à `min_plausible_low_voltage_limit` strictement.
- `ignore_voltage_bounds` le paramètre utilisé comme seuil de consistances pour prendre en considération
les bornes inférieures et supérieures en tension des bus. Les bus ayant une tension nominale inférieure à 
ce paramètre auront leurs bornes remplacées par [`min_plausible_low_voltage_limit`; `max_plausible_high_voltage_limit`].
- `buses_with_reactive_slacks` le paramètre déterminant quels bus auront des slacks réactifs attachés dans la résolution
de l'ACOPF. Ces paramètres peut prendre les valeurs suivantes:
  - "ALL", pour indiquer que tous les bus ont des reactive slack variables attached.
  - "NO_GENERATION" pour indiquer que tous les bus ne produisant pas de réactif auront des reactive slacks variables attached.
  - "CONFIGURED" pour indiquer 


Ces éléments sont dans les fichiers ayant comme prefix
`param_`, et on compte :

- `param_transformers.txt`, defining which ratio tap changers have a variable ratio.
  By default, no ratio is variable.

  Format : 2 columns #"num" "id"


- `param_shunts.txt`, defining which shunts have a variable susceptance value and which can be modified/connected.
  By default, all susceptance shunts are constant, fixed to the values defined in `ampl_network_shunts.txt`.
  Among the variable shunts, if one is not connected (`bus=-1`)
  but parameter `bus_possible` is well defined, then this shunt may be connected by the OPF.

  Format : 2 columns #"num" "id"


- `param_generators_reactive.txt`, defining which generators have a constant reactive power value
  (defined by `unit_Qc` in `ampl_network_generators.txt`) in the OPFs. This value is used even if it
  falls out of bounds (`Qmin`/`Qmax`). However, if it is
  not consistent (> `PQmax`), then reactive power becomes a variable. 
  By default, the reactive power of all generating units are variable.
  User is invited to note that it is also possible to fix reactive power by setting the minimum and maximum reactive
  power bounds to same value in `ampl_network_generators.txt`.

  Format : 2 columns #"num" "id"


- `param_buses_with_reactive_slack.txt`, defining which buses will have reactive slacks attached
  in the solving of the ACOPF, if `buses_with_reactive_slacks="CONFIGURED"`.
  
  Format : 2 columns #"num" "id"


### 4 Checks and special handling

#### 4.X Voltage level limits

#### 4.X Computation of reference bus

#### 4.X Transformer consistency

#### 4.X P/Q units' domain

### 4 Reference bus and main connex component


A _reference bus_ (`null_phase_bus` AMPL parameter) is determined to enforce the zero-phase constraint of the OPFs. 
This reference bus corresponds to the bus in the network with the most AC branches connected,
among those belonging to the main connected component (`bus_CC = 0`). 
If multiple buses have the same maximum cardinality, the one with the highest `num` is selected.
If no bus is found meeting these criteria, the first bus defined in the file `ampl_network_buses.txt` is chosen.

The DCOPF and ACOPF are executed on buses connected to the reference bus by AC branches.
Then, buses connected to the reference bus by HVDC lines are excluded in OPF computation.
These buses are determined by solving the `PROBLEM_CCOMP` optimization problem.
After the optimization, buses connected by AC branches are determined by verifying
that the associated variable `teta_ccomputation` is set to 0.

### 5 Optimal Power Flows

#### 5.1 Direct Current Optimal Power Flow

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

#### 5.2 Alternative Current Optimal Power Flow

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
    
### X Output

#### X.1 In case of inconsistency

If the computation of the main connex component or of the DCOPF fails (Knitro diverges
or the problem is solved with too much slack), the problem is considered as inconsistent.
Then, the script `reactiveopfexit.run` is executed and the following file is exported:

- `reactiveopf_results_indic.txt`, which contains various indicators to
  provide an overview of the run. It includes:
  - The error message(s) returned by AMPL.
  - General information (system OS, computation time, etc.).
  - The configurable thresholds/parameters used in the run (see section X).

#### X.2 In case of convergence

If the AMPL process defined in `reactiveopf.run` is successful, the script `reactiveopfoutput.run` is executed 
(even if the solving of ACOPF failed) and the following files are exported:

- `reactiveopf_results_indic.txt`, which contains various indicators to provide an overview of the
  run. It includes:
  - General information (system OS, computation time, etc.).
  - The configurable thresholds/parameters used in the run (see section X).
  - The cardinality of the sets used in the optimization problems (number of non-impedance branches,
    number of buses with slack variables, etc.).
  - Information about calculated angles (maximum/minimum theta, maximum
    difference between neighboring buses, etc.).


- `reactiveopf_results_static_var_compensators.csv`, which contains calculated 
voltage and reactive power values for the SVC that regulate voltage.

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


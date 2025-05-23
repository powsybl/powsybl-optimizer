# Alternative current optimal power flow

## Generalities

The goal of the reactive ACOPF is to compute voltage values on each bus, as well as control values for reactive equipment and controllers of the grid. 
Then, the following values will be variable in the optimization:
- $\boldsymbol{V_i}$ and $\boldsymbol{\theta_i}$ the voltage magnitude and phase of bus $i$.
- $\boldsymbol{P_{i,g}}$ (resp. $\boldsymbol{Q_{i,g}}$) the active (resp. reactive) power produced by variable generator $g$ of bus $i$.
- $\boldsymbol{Q_{i,vsc}}$ the reactive power produced by voltage source converter stations $vsc$ of bus $i$.
- $\boldsymbol{b_{i,s}}$ (resp. $\boldsymbol{b_{i,svc}}$) the susceptance of shunt $s$ (resp. of static var compensator $svc$) of bus $i$.
- $\boldsymbol{\rho_{ij}}$ the transformer ratio of the ratio tap changer on branch $ij$, 
specified as variable by the user (see [Configuration of the run](inputs.md#configuration-of-the-run)).

Please note that:
- Units with active power specified in `ampl_network_generators.txt` that is less than the configurable parameter `Pnull` **are excluded from the optimization**,
  even if the user designates these generators as fixed in the parameter file `param_generators_reactive.txt` (see [Configuration of the run](inputs.md#configuration-of-the-run)).
  Therefore, when the optimization results are exported, **these generators are exported with a reactive power target of $0$**.
- **Neither current limits nor power limits** on branches are considered in the optimization.
- Branches with one side open are considered in optimization. 
- The voltage controls are not taken into account in the optimization model, as its purpose is to determine them (see [OpenReac](index.md#openreac)).
  However, the remote control of generators and static var compensators is taken into account in the export of equipment's voltage target (see [Outputs](outputs.md#in-case-of-convergence)).
- The transformation ratios $\boldsymbol{\rho_{ij}}$ and the shunt susceptances $\boldsymbol{b_{i,s}}$ are continuous in the optimization. 
At the end, these variables may differ from the values associated with the discrete taps of the equipment (see [Network data](inputs.md#network-data)), and rounding may be necessary. 
In the case of transformers, a second optimization can be carried out to adjust the voltage plan to the new transformation ratios after rounding (see [Solving](acOptimalPowerflow.md#solving)). 

## Constraints

The constraints of the optimization problem depend on parameters specified by the user (see [Configuration of the run](inputs.md#configuration-of-the-run)). 
In particular, the user can indicate which buses will have associated **reactive slacks** $\boldsymbol{\sigma_{i}^{Q,+}}$ and $\boldsymbol{\sigma_{i}^{Q,-}}$
, expressing the excess (resp. shortfall) of reactive power produced in bus $i$, and used to ensure reactive power balance. 
To do so, these buses must be specified in parameter file `param_buses_with_reactive_slack.txt`, and `buses_with_reactive_slacks` must be set to $\text{CONFIGURED}$.

The ACOPF involves the following constraints, in addition to the slack constraint $(1)$ introduced in the [Slack bus and main connex component](slackBusMainConnexComponent.md) part:

$$\sum\limits_{j\in v(i)} \boldsymbol{p_{ij}} = P_i^{in} - \sum\limits_{g}\boldsymbol{P_{i,g}}, \quad i\in\text{BUSCC} \quad (5)$$

$$\sum\limits_{j\in v(i)} \boldsymbol{q_{ij}} = Q_i^{in} - \boldsymbol{\sigma_{i}^{Q,+}} + \boldsymbol{\sigma_{Q_i}^{-}} - \sum\limits_{g}\boldsymbol{Q_{i,g}} - \sum\limits_{vsc}\boldsymbol{Q_{i,vsc}} - \sum\limits_{s}\boldsymbol{b_{i,s}}{V_i}^2 - \sum\limits_{svc}\boldsymbol{b_{i,svc}}{V_i}^2, \quad i\in\text{BUSCC} \quad (6)$$

where:
- $\boldsymbol{p_{ij}}$ (resp. $\boldsymbol{q_{ij}}$) is the active (resp. reactive) power leaving bus $i$ on branch $ij$,
  calculated as defined in the [PowSyBl documentation](https://powsybl.readthedocs.io/projects/powsybl-open-loadflow).
  Those are variables because they depend on $\boldsymbol{V_i}$, $\boldsymbol{V_j}$, $\boldsymbol{\theta_i}$, $\boldsymbol{\theta_j}$ and $\boldsymbol{\rho_{ij}}$.
- $P_i^{in}$ is the constant active power injected or consumed in bus $i$ by batteries, loads, VSC stations and LCC stations.
- $Q_i^{in}$ is the constant reactive power injected or consumed in bus $i$, by fixed generators and fixed shunts (see [Configuration of the run](inputs.md#configuration-of-the-run)), batteries, loads and LCC stations.

In order to bound the variables described in [Generalities](#generalities), the limits specified in the files of network data (see [Network data](inputs.md#network-data)) are used. We specify the following special treatments:
- The voltage magnitude $\boldsymbol{V_i}$ lies between the corrected voltage limits described in the [Voltage level limit consistency](preprocessing.md#voltage-level-limit-consistency) section.
- The reactive power $\boldsymbol{Q_{i,g}}$ produced by unit $g$ lies between the corrected limits described in the [P/Q unit domain](preprocessing.md#pq-unit-domain) section.
- The active power $\boldsymbol{P_{i,g}}$ also lies between the corrected limits described in the [P/Q unit domain](preprocessing.md#pq-unit-domain) section, but these bounds are only considered when the configurable parameter $\alpha$ is different than $1$ (default value).
Otherwise, all active powers evolve proportionally to their initial point $P_{i,g}^t$ (specified in `ampl_network_generators.txt`):
$\boldsymbol{P_{i,g}} = P_{i,g}^t + \boldsymbol{\gamma} (P_{g}^{max,c} - P_{i,g}^t)$, where $\boldsymbol{\gamma}$ is optimized and lies in $[-1;1]$.
- The reactive power $\boldsymbol{Q_{i,vsc}}$ produced by voltage source converter station $vsc$ is included in $[\min(qP_{vsc}, qp_{vsc}, qp_{vsc}^0)$; $\max(QP_{vsc}, Qp_{vsc}, Qp_{vsc}^0)]$.
**The bounds are therefore rectangular, not trapezoidal.**

## Objective function

The objective function also depends on parameters specified by the user (see [Configuration of the run](inputs.md#configuration-of-the-run)).
The `objective_choice` parameter modifies the values of penalties $\beta_1$, $\beta_2$, and $\beta_3$ in the objective function:
if `objective_choice` $= i$, then $\beta_i = 1$ and $\beta_j = 0.01$ for $j \neq i$.

Specifically, if `objective_choice` takes on:
- $0$, the minimization of active power production $\sum\limits_{i,g}\boldsymbol{P_{i,g}}$ is prioritized.
- $1$, the minimization of $\sum\limits_{i} \boldsymbol{V_i}-(\rho V_i^{c,max} - (1-\rho)V_i^{c,min})^2$ is prioritized ($\rho$ 
equals the configurable parameter `ratio_voltage_target`). 
- $2$, the minimization of $\sum\limits_{i} (\boldsymbol{V_i} - V_i^t)^2$ is prioritized.

The objective function of the ACOPF is:

$
\begin{aligned}
\text{minimize} \quad &
10 \sum\limits_{i} (\boldsymbol{\sigma_{i}^{Q,+}} + \boldsymbol{\sigma_{i}^{Q,-}}) \\
& + \beta_1 \sum\limits_{g} \left( \alpha \boldsymbol{P_{i,g}} + (1-\alpha)\left(\frac{\boldsymbol{P_{i,g}} - P_{i,g}^t}{\max(1, |P_{i,g}^t|)}\right)^2 \right) \\
& + \beta_2 \sum\limits_{i} \left( \boldsymbol{V_i} - (1-\rho)V_{i}^{\text{min,c}} + \rho V_{i}^{\text{max,c}} \right)^2 + \beta_3 \sum\limits_{i} (\boldsymbol{V_i} - V_i^t)^2 \\
& + 0.1 \sum\limits_{g} \left(\frac{\boldsymbol{Q_{i,g}}}{\max(1,Q_{g}^{\text{min,c}}, Q_{g}^{\text{max,c}})}\right)^2 + 0.1 \sum\limits_{ij} (\boldsymbol{\rho_{ij}} - \rho_{ij})^2
\end{aligned}
$


where: 
- $P_{i,g}^t$ (resp. $V_i^t$) is the active target (resp. voltage initial point) specified in `ampl_network_generators.txt` (resp. `ampl_network_buses.txt`).
- $\rho_{ij}$ is the transformer ratio of line $ij$, specified in `ampl_network_tct.txt`.

The sum of the reactive slack variables is penalized by a
high coefficient ($10$) to drive it towards $0$, ensuring reactive power balance at each bus of the network.

## Solving

Before solving the ACOPF, the voltage magnitudes $\boldsymbol{V_i}$ are warm-started with $V_i^t$
(specified in `ampl_network_buses.txt`), as well as the voltage phases $\boldsymbol{\theta_i}$ with the results of the DCOPF (see [DC optimal powerflow](dcOptimalPowerflow.md)).
Please also note that a scaling is applied with user-defined values before solving the ACOPF.

A solving is considered as successful if the non-linear solver employed (see [Non-linear optimization solver](../gettingStarted.md#non-linear-optimization-solver)) finds a feasible approximate solution (**even if the sum of slacks is important**).

At the user's request (see [Configuration of the run](inputs.md#configuration-of-the-run)), and if at least one transformer is optimized, 
a second ACOPF optimization can be performed after rounding the transformer ratios (which, as a reminder, are continuous in the solving) 
to the nearest tap in the input data (see [Network data](inputs.md#network-data)). 
This allows the voltage plan to be readjusted to the new fixed transformation ratios in the second optimization. 
Without this optimization, note that power flows can vary significantly before and after rounding the taps, particularly for transformers with low impedance.

If the ACOPF resolution(s) are successfully completed, the script `reactiveopfoutput.run` is executed (see [In case of convergence](outputs.md#in-case-of-convergence)). 
Otherwise, the script `reactiveopfexit.run` is executed (see [In case of inconsistency](outputs.md#in-case-of-inconsistency)).

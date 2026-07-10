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
- Transformers connected in parallel are grouped and constrained to a common transformation ratio (see [Parallel transformers](#parallel-transformers)). 

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

## Parallel transformers

Transformers connected in parallel (sharing the same pair of buses, or forming a closed loop of transformers inside a single substation) should keep the same transformation ratio: letting them diverge would create circulating reactive flows between the parallel branches. The optimizer therefore detects such groups (called *bundles*) automatically, and constrains each bundle to a single shared ratio.

The quantity that must be equalized is the *effective* per-unit ratio $c_{ij} \boldsymbol{\rho_{ij}}$, where $c_{ij}$ is the constant (off-tap) per-unit ratio of the transformer (the "cst ratio (pu)" column of the network data): this is the ratio entering the flow equations, whose mismatch drives circulating flows. When all members of a bundle are identical units ($c_{ij}$ equal), this is equivalent to equalizing the tap ratios themselves.

The effective ratio applies from terminal 1 to terminal 2 *as declared* in the network, and nothing in the data model forces two physically parallel transformers to be declared in the same direction. Each member is therefore qualified against a canonical direction of its bundle (that of its first member in id order): a member declared in the same direction is *direct* (orientation +1), a member declared in the opposite direction is *reversed* (orientation -1). The comparison uses the nominal-voltage side of terminal 1, falling back to the terminal-1 bus for equal-nominal-voltage bundles sharing a single bus pair; an equal-nominal-voltage bundle coming from a loop cannot be oriented and is released with a warning instead of being tied blindly.

For a bundle $B$ whose members are all optimized variable-ratio transformers, all the effective ratios are tied to one shared variable $\boldsymbol{\rho_B}$, expressed in the canonical direction:

$$c_{ij} \boldsymbol{\rho_{ij}} = \boldsymbol{\rho_B} \;\; (ij \text{ direct}), \qquad c_{ij} \boldsymbol{\rho_{ij}} \, \boldsymbol{\rho_B} = 1 \;\; (ij \text{ reversed}), \quad ij \in B \quad (7)$$

A reversed member transforms in the opposite declared direction, so equalizing raw effective ratios would enforce a physically wrong condition; with $(7)$, the product of the declared transformations around a loop of transformers is identically $1$, whatever $\boldsymbol{\rho_B}$. The shared variable is bounded by the intersection of the members' effective ranges mapped to the canonical direction — a direct member contributes $[c_{ij} \rho_{ij}^{min}, c_{ij} \rho_{ij}^{max}]$, a reversed member the inverse interval $[1 / (c_{ij} \rho_{ij}^{max}), 1 / (c_{ij} \rho_{ij}^{min})]$ — i.e. $\boldsymbol{\rho_B} \in [\rho_B^{min}, \rho_B^{max}]$ with $\rho_B^{min}$ the largest of the mapped lower bounds and $\rho_B^{max}$ the smallest of the mapped upper bounds.

Depending on this intersection, a bundle is handled in one of three ways:
- **non-empty interval** ($\rho_B^{min} < \rho_B^{max}$): the ratios are tied through constraint $(7)$.
- **single point** ($\rho_B^{min} \approx \rho_B^{max}$): there is essentially one feasible common effective ratio. Each member $ij$ is fixed at $\frac{\rho_B^{min} + \rho_B^{max}}{2}$ (its inverse for a reversed member), clamped to its own declared effective range $[c_{ij} \rho_{ij}^{min}, c_{ij} \rho_{ij}^{max}]$ — the same rule as the empty case below. Since both bounds nearly coincide, this value is essentially the shared ratio $\rho_B^{min} \approx \rho_B^{max}$.
- **empty interval** ($\rho_B^{min} > \rho_B^{max}$): the members' effective ranges are disjoint, which signals inconsistent input data (e.g. disjoint tap ranges, or rated voltages making a common effective ratio unreachable). The optimizer falls back to a best-effort behavior, fixing each member as close as possible to the center of the gap $\frac{\rho_B^{min} + \rho_B^{max}}{2}$ (its inverse for a reversed member), clamped to its own declared effective range $[c_{ij} \rho_{ij}^{min}, c_{ij} \rho_{ij}^{max}]$.

A bundle is tied through $(7)$ only if **all** its members are optimized variable-ratio transformers (specified in `param_transformers.txt`, see [Configuration of the run](inputs.md#configuration-of-the-run)). A member that is not optimized is frozen at its current tap, which pins the shared ratio to that value and collapses the bundle to the single-point or empty case above. As a consequence, declaring only a subset of a parallel group as variable does not optimize that subset freely: those transformers are fixed at the common ratio, again to avoid circulating flows.

This grouping interacts with the second optimization after tap rounding (see [Solving](#solving)): constraint $(7)$ is released before rounding, so each transformer is rounded to the nearest tap of its own table independently. Members of a bundle may therefore end up on different discrete taps if their tap tables differ; the shared ratio is guaranteed only for the continuous solving.

Finally, if at solve time a member of a tied bundle turns out not to be an optimized variable-ratio branch — for instance because it lies outside the main connex component, or has a near-zero impedance — the whole bundle is released: its members are optimized independently for that run, and the event is logged.

## Objective function

The objective function is a weighted sum of penalty terms. Each of the seven weights is configurable by the user (see [Configuration of the run](inputs.md#configuration-of-the-run)), which allows arbitrating between the different terms of the objective (reactive slack activation, active power generation, voltage targeting, reactive power of units and transformer ratio).

The objective function of the ACOPF is:

$
\begin{aligned}
\text{minimize} \quad &
\sum\limits_{i} \left( w_{\sigma}^{+}\,\boldsymbol{\sigma_{i}^{Q,+}} + w_{\sigma}^{-}\,\boldsymbol{\sigma_{i}^{Q,-}} \right) \\
& + w_{P} \sum\limits_{g} \left( \alpha \boldsymbol{P_{i,g}} + (1-\alpha)\left(\frac{\boldsymbol{P_{i,g}} - P_{i,g}^t}{\max(1, |P_{i,g}^t|)}\right)^2 \right) \\
& + w_{V}^{\rho} \sum\limits_{i} \left( \boldsymbol{V_i} - (1-\rho)V_{i}^{\text{min,c}} + \rho V_{i}^{\text{max,c}} \right)^2 + w_{V}^{0} \sum\limits_{i} (\boldsymbol{V_i} - V_i^t)^2 \\
& + w_{Q} \sum\limits_{g} \left(\frac{\boldsymbol{Q_{i,g}}}{\max(1,Q_{g}^{\text{min,c}}, Q_{g}^{\text{max,c}})}\right)^2 + w_{\rho} \sum\limits_{ij} (\boldsymbol{\rho_{ij}} - \rho_{ij})^2
\end{aligned}
$


where: 
- $P_{i,g}^t$ (resp. $V_i^t$) is the active target (resp. voltage initial point) specified in `ampl_network_generators.txt` (resp. `ampl_network_buses.txt`).
- $\rho_{ij}$ is the transformer ratio of line $ij$, specified in `ampl_network_tct.txt`.
- the weights $w_{\sigma}^{+}$, $w_{\sigma}^{-}$, $w_{P}$, $w_{V}^{\rho}$, $w_{V}^{0}$, $w_{Q}$ and $w_{\rho}$ correspond respectively to the parameters `penalty_invest_rea_pos`, `penalty_invest_rea_neg`, `penalty_active_power`, `penalty_voltage_target_ratio`, `penalty_voltage_target_data`, `penalty_units_reactive` and `penalty_transfo_ratio`.

Four of these weights have a fixed default value: $w_{\sigma}^{+} = 10$, $w_{\sigma}^{-} = 10$, $w_{Q} = 0.1$ and $w_{\rho} = 0.1$.
The high default weight on the reactive slacks drives their sum towards $0$, ensuring reactive power balance at each bus of the network.

The three remaining weights — $w_{P}$, $w_{V}^{\rho}$ and $w_{V}^{0}$ — have a default value that depends on the `objective_choice` parameter when they are left unset: the term matching the selected objective receives a weight of $1$, while the other two receive $0.01$. Specifically, if `objective_choice` takes on:
- $0$ (`MIN_GENERATION`), the minimization of active power production $\sum\limits_{i,g}\boldsymbol{P_{i,g}}$ is prioritized ($w_{P} = 1$).
- $1$ (`BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT`), the minimization of $\sum\limits_{i} \boldsymbol{V_i}-(\rho V_i^{c,max} - (1-\rho)V_i^{c,min})^2$ is prioritized ($w_{V}^{\rho} = 1$), where $\rho$ equals the configurable parameter `ratio_voltage_target`.
- $2$ (`SPECIFIC_VOLTAGE_PROFILE`), the minimization of $\sum\limits_{i} (\boldsymbol{V_i} - V_i^t)^2$ is prioritized ($w_{V}^{0} = 1$).

Setting an explicit value on any of these three weights overrides this objective-dependent default, regardless of the selected objective. As all weights accept any value $\geq 0$, a term can be fully neutralized by setting its weight to $0$.

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

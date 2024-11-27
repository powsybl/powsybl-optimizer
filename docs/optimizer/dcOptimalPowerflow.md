# Direct current optimal power flow

## Generalities

Before addressing the ACOPF (see [AC optimal powerflow](acOptimalPowerflow.md)), a DCOPF is solved for two main reasons:
- If the DCOPF resolution fails, it provides a strong indication that the ACOPF resolution will also fail.
  Thus, it serves as a formal consistency check on the data.
- The phases computed by DCOPF resolution will be used as initial points for the solving of the ACOPF.

## Optimization problem

The DCOPF model involves the following constraints, in addition to the slack constraint $(1)$ introduced in [Slack bus and main connex component](slackBusMainConnexComponent.md):

$$\sum\limits_{j\in v(i)} \boldsymbol{p_{ij}} = P_i^{in} + \boldsymbol{\sigma_{P,i}^{+}} - \boldsymbol{\sigma_{P,i}^{-}} - \sum\limits_{g}\boldsymbol{P_{i,g}}, \quad i\in\text{BUSCC} \quad (4)$$

where:
- $\boldsymbol{p_{ij}}$ is the active power leaving bus $i$ on branch $ij$, defined as $\boldsymbol{p_{ij}} = \frac{\boldsymbol{\theta_i} - \boldsymbol{\theta_j}}{X_{ij}}$, where $X_{ij}$ is the reactance of line $ij$ (specified in `ampl_network_branches.txt`).
- $P_i^{in}$ the constant active power injected or consumed in bus $i$ (by batteries, loads, VSC stations and LCC stations).
- $\boldsymbol{P_{i,g}}$ is the variable active power produced by generators of bus $i$.
- $\boldsymbol{\sigma_{P,i}^{+}}$ (resp. $\boldsymbol{\sigma_{P,i}^{-}}$) is a positive slack variable expressing the excess (resp. shortfall) of active power produced in bus $i$.

And the following objective function:

$$\text{minimize} \left(1000 \sum\limits_{i} (\boldsymbol{\sigma_{i}^{P,+}} + \boldsymbol{\sigma_{i}^{P,-}}) + \sum\limits_{g} \left(\frac{\boldsymbol{P_{i,g}} - P_{i,g}^{t}}{\max(1, \frac{P_{i,g}^t}{100})}\right)^2\right)$$

where $P_{i,g}^{t}$ is the target of the generator $g$ on bus $i$. 

The sum of the active slack variables ($\boldsymbol{\sigma_{i}^{P,+}}$ and $\boldsymbol{\sigma_{i}^{P,-}}$) is penalized by a high coefficient ($1000$) to drive it towards $0$, ensuring active power balance at each bus of the network.
The solving of the DCOPF is considered as successful if this sum **does not exceed** the configurable threshold `Pnull` (see [Configuration of the run](inputs.md#configuration-of-the-run)), and if the non-linear solver employed (see [Non-linear optimization solver](../gettingStarted.md#non-linear-optimization-solver)) finds a feasible solution without reaching one of its default limit. Otherwise, the solving is considered unsuccessful and the script `reactiveopfexit.run` is executed (see [In case of inconsistency](outputs.md#in-case-of-inconsistency)).

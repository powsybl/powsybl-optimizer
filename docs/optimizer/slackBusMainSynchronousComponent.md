# Slack bus and main synchronous component

## Main synchronous component

The OPFs are executed on the **main synchronous component** of the network: the buses whose synchronous component number (`sc` in `ampl_network_buses.txt`) is $0$, restricted to buses whose nominal voltage is greater than or equal to `epsilon_nominal_voltage` (see [Configuration of the run](inputs.md#configuration-of-the-run)).

The `sc` value is computed by PowSyBl on the IIDM network and exported by the AMPL exporter: OpenReac does not recompute it.
Synchronous components are computed on AC branches only, so **buses connected to the rest of the network only by HVDC lines are excluded**.
Components are numbered by decreasing size, hence $0$ for the main one.
Note that these numbers are global to the network, whereas the AMPL export can be restricted to the main connex component (through the `AmplExportConfig` export scope; the default scope used by OpenReac exports all the components).
With such a restricted export, no bus with `sc` $= 0$ may remain when the largest synchronous component lies in another connex one, and the run then stops on an empty main synchronous component.

The connex component (`cc`) is deliberately not used as a filter.
Connex components are computed across HVDC links, so they may merge several synchronous areas, which an ACOPF cannot solve together.
Since a synchronous component is always contained in a single connex component, intersecting with the main connex component would either change nothing, or discard the whole main synchronous component when it happens to lie in another connex one.

The sets of buses and branches belonging to the main synchronous component are denoted $BUSCC$ and $BRANCHCC$, respectively.
If $BUSCC$ is empty, no bus is left to optimize: the script `reactiveopfexit.run` is executed (see [In case of inconsistency](outputs.md#in-case-of-inconsistency)) and the execution is stopped.

Note that buses whose nominal voltage is below `epsilon_nominal_voltage` are discarded even when they belong to the main synchronous component.
If such a bus is the only link between two parts of the component, $BUSCC$ is split into islands that are no longer connected in $BRANCHCC$.
The angle reference $(1)$ then only applies to the island containing the slack bus, which is harmless in itself: the angles of the other islands are simply free within their bounds.
The consequence is on the active power balance, and it differs between the two problems:

- the DCOPF has one slack variable per bus and unbounded generation, so it balances each island independently. It fails only if an island cannot be balanced at all, typically because it contains no generating unit up and running (see [DC optimal power flow](dcOptimalPowerflow.md));
- the ACOPF has no slack variable on its active power balance and, when `coeff_alpha` is $1$, its default value (see [Constraints](acOptimalPowerflow.md#constraints)), the generation of every unit is an affine function of a single global variable $\alpha$. One value of $\alpha$ must then balance every island at once, which only happens if all islands require the same $\alpha$. The ACOPF is therefore infeasible in the general case, **even when each island could be balanced on its own**.

Lowering `epsilon_nominal_voltage` restores the discarded links.
The number of discarded buses is exported as the indicator `nb_bus_dropped_in_main_SC`, and is recalled in the error message when the DCOPF turns out to be infeasible.

## Slack bus

The slack bus $s$ is used only to fix the voltage angle reference of the DCOPF and the ACOPF:

$$\boldsymbol{\theta_s} = 0 \quad (1)$$

It is not a slack bus in the load flow sense: this reactive OPF changes the generation values proportionally, in order to ensure the global balance generation = losses + load.

It is selected as follows:

1. If at least one bus of the main synchronous component is flagged in the `slack bus` column of `ampl_network_buses.txt`, the one with the smallest `num` is used.
   In IIDM, this flag comes from the `SlackTerminal` extension, which is typically written by a load flow previously executed on the network.
   The indicator `slack_bus_origin` is then set to `DATA`.
2. Otherwise, a fallback bus is computed.
   Among the buses of the main synchronous component whose nominal voltage is at least 90% of $\min(300 \text{ kV}, V_{nom}^{max})$, where $V_{nom}^{max}$ is the highest nominal voltage of the component, the bus with the **highest number of AC branches connected** is selected.
   If multiple buses have such cardinality, the one with the highest identifier (`num` parameter) is chosen.
   In the event that no bus satisfies these conditions, the first bus of the component is selected.
   The indicator `slack_bus_origin` is then set to `FALLBACK`.

Buses flagged as slack outside $BUSCC$ are ignored: this includes buses of another synchronous component, of which a load flow typically flags one per component, as well as buses whose nominal voltage is below `epsilon_nominal_voltage`. The fallback applies only when none of the flagged buses belongs to $BUSCC$.

The identifier of the bus finally used is exported as the indicator `slack_bus` in `reactiveopf_results_indic.txt` (see [Outputs](outputs.md#in-case-of-convergence)).

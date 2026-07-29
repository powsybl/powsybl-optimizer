# Slack bus and main synchronous component

## Main synchronous component

The OPFs are executed on the **main synchronous component** of the network: the buses whose synchronous component number (`sc` in `ampl_network_buses.txt`) is $0$, restricted to the main connex component (`cc` set to $0$) and to buses whose nominal voltage is greater than or equal to `epsilon_nominal_voltage` (see [Configuration of the run](inputs.md#configuration-of-the-run)).

Both `cc` and `sc` are computed by PowSyBl on the IIDM network and exported by the AMPL exporter: OpenReac does not recompute them.
As synchronous components ignore HVDC links, **buses connected to the rest of the network only by HVDC lines are excluded**.

Connex and synchronous components are numbered independently, both by decreasing size.
The main synchronous component is therefore not necessarily included in the main connex component.
When this happens, no bus is left to optimize: the script `reactiveopfexit.run` is executed (see [In case of inconsistency](outputs.md#in-case-of-inconsistency)) and the execution is stopped.

The sets of buses and branches belonging to the main synchronous component are denoted $BUSCC$ and $BRANCHCC$, respectively.

Note that buses whose nominal voltage is below `epsilon_nominal_voltage` are discarded even when they belong to the main synchronous component.
If such a bus is the only link between two parts of the component, $BUSCC$ is not connected in $BRANCHCC$, and the voltage angles are then determined up to a constant on each island.
A warning is issued when at least one bus of the main synchronous component is discarded this way.

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

A bus flagged as slack but located outside the main synchronous component is ignored, and the fallback applies.

The identifier of the bus finally used is exported as the indicator `slack_bus` in `reactiveopf_results_indic.txt` (see [Outputs](outputs.md#in-case-of-convergence)).

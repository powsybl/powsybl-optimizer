# ACOPF initialization

## Generalities

Before the AMPL export, the starting point of the ACOPF (see [AC optimal powerflow](acOptimalPowerflow.md)) is written on the network by the Java interface, on a copy of the requested variant that is removed once the run is over.
The network handed by the user is thus left untouched, and the values found in the network data files (see [Network data](inputs.md#network-data)) are those of this starting point.

It is built in two steps: the controls optimized by the ACOPF are set to the requested reference state, then a DC load flow gives the voltage phases.

## Reference state

The reference state is the state the ACOPF starts from and, for the controls carrying a penalty in its objective function (see [Objective function](acOptimalPowerflow.md#objective-function)), is pulled back to.
It is chosen through the Java API (`OpenReacParameters`, parameter `referenceState`):

- `NETWORK` (default): the state of the network, which may be the result of a previous optimization.
- `NEUTRAL`: a state independent of the input, so that two successive runs, between which the results have been applied, give the same solution.
  On the main synchronous component, the voltage magnitudes are set to $1$ p.u., the ratio tap changers optimized by the ACOPF to their neutral position (ratio $1$, or the closest one), the variable shunts with a linear model to $0$ section, and the reactive power set point of VSC converter stations to the middle of their reactive power range, as generators and batteries are in the ACOPF (see [Solving](acOptimalPowerflow.md#solving)).
  Variable shunts with a non linear model are left as they are: the susceptance bounds of these shunts in `ampl_network_shunts.txt` depend on their current section.

With `NEUTRAL`, the voltage target $V_i^t$ of the objective function is $1$ p.u. on every bus.

## DC load flow

A DC load flow is then solved with [OpenLoadFlow](https://github.com/powsybl/powsybl-open-loadflow) on the main synchronous component, in place of the DCOPF formerly solved in AMPL.
The active power mismatch is distributed on the generators proportionally to their active power target, regardless of their active power limits as in the former DCOPF, and what cannot be distributed is left on the slack bus: the ACOPF has its own handling of the active power balance (see [Constraints](acOptimalPowerflow.md#constraints)).
As in the ACOPF, the AC emulation of HVDC lines is not considered and the phase shifters are not regulating. The transformation ratios are ignored.

The slack bus of this load flow is the most meshed bus among those of the highest nominal voltages.
It is the angle reference of the ACOPF, given to AMPL through `param_algo.txt` (see [Slack bus](slackBusMainSynchronousComponent.md#slack-bus)).
The `SlackTerminal` extension of the network is neither read nor written.

The load flow writes the voltage phases $\boldsymbol{\theta_i}$ and the active power of the generators, which are read from `ampl_network_buses.txt` and `ampl_network_generators.txt` to warm-start the ACOPF (see [Solving](acOptimalPowerflow.md#solving)).
The voltage magnitudes are not written.

If the main synchronous component has no generator or if the load flow fails, no AMPL run is performed and an exception is raised.

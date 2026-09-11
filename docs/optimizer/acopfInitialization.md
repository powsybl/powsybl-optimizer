# ACOPF initialization

## Generalities

Before the AMPL export, the starting point of the ACOPF (see [AC optimal powerflow](acOptimalPowerflow.md)) is written on the network by the Java interface, on a copy of the requested variant that is removed once the run is over.
The network handed by the user is thus left untouched, and the values found in the network data files (see [Network data](inputs.md#network-data)) are those of this starting point.

## DC load flow

A DC load flow is solved with [OpenLoadFlow](https://github.com/powsybl/powsybl-open-loadflow) on the main synchronous component, in place of the DCOPF formerly solved in AMPL.
The active power mismatch is distributed on the generators proportionally to their active power target, regardless of their active power limits as in the former DCOPF, and what cannot be distributed is left on the slack bus: the ACOPF has its own handling of the active power balance (see [Constraints](acOptimalPowerflow.md#constraints)).
As in the ACOPF, the AC emulation of HVDC lines is not considered and the phase shifters are not regulating. The transformation ratios are ignored.

The slack bus of this load flow is the most meshed bus among those of the highest nominal voltages.
It is the angle reference of the ACOPF, given to AMPL through `param_algo.txt` (see [Slack bus](slackBusMainSynchronousComponent.md#slack-bus)).
The `SlackTerminal` extension of the network is neither read nor written.

The load flow writes the voltage phases $\boldsymbol{\theta_i}$ and the active power of the generators, which are read from `ampl_network_buses.txt` and `ampl_network_generators.txt` to warm-start the ACOPF (see [Solving](acOptimalPowerflow.md#solving)).
The voltage magnitudes are not written.

If the main synchronous component has no generator or if the load flow fails, no AMPL run is performed and an exception is raised.

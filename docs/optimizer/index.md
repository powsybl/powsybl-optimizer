# OpenReac

The reactive optimal power flow (OPF) is implemented with AMPL.
Its goal is to compute voltage values on each point of the network as well as control values for reactive equipment and controllers of the grid (voltage set point of generating units, shunts, transformer ratios...).

In a grid development study, you decide new equipment, new generating units, new substations, new loads, you set values for active and reactive loads, you set values for active power generation and HVDC flows.
Then if you wish to do AC powerflow simulations with N-1 analysis, you need all voltage and reactive set points and this reactive OPF is your solution.

Please notice that this reactive OPF does **not** decide active power of generating units and HVDC branches.


```{toctree}
---
maxdepth: 2
hidden: true
---
divisionOfCode.md
inputs.md
preprocessing.md
slackBusMainConnexComponent.md
dcOptimalPowerflow.md
acOptimalPowerflow.md
outputs.md

```


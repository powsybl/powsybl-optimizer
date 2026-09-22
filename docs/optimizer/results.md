# Results

The results of a run are returned to the user through the Java class
[OpenReacResult](https://github.com/powsybl/powsybl-optimizer/blob/main/open-reac/src/main/java/com/powsybl/openreac/parameters/output/OpenReacResult.java),
built from the files described in [Outputs](outputs.md).

TODO: describe the status, the indicators, the network modifications, the voltage profile, the reactive slacks and the fixed parallel transformers.

## Shunt compensator discretization

The susceptances of the variable shunts are continuous in the optimization (see [AC optimal power flow](acOptimalPowerflow.md#generalities)),
whereas a shunt compensator of the network can only be set on one of its sections.
For each shunt of `reactiveopf_results_shunts.csv`, the continuous susceptance $b$ (in S, converted from the per-unit value on the $S_b = 100$ MVA base)
is therefore rounded onto the section of the shunt whose susceptance $b_{section}$ is the closest to $b$, and the resulting `ShuntCompensatorModification` sets this section.

The following results quantify this rounding:

| Method                                | Description                                                                                                                                                                                                                                                   |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `getContinuousSusceptanceByShunt()`   | For each shunt, the continuous susceptance $b$, in S, before rounding.                                                                                                                                                                                        |
| `getReactiveDeviationByShunt()`       | For each shunt, the reactive power deviation $\Delta Q = (b_{section} - b) \, V_{nom}^2$, in MVar, with $V_{nom}$ the nominal voltage of the shunt in kV. It is positive when the retained section generates more reactive power than the continuous optimum. |
| `getTotalAbsoluteReactiveDeviation()` | Over all the shunts, the sum $\sum \lvert \Delta Q \rvert$, in MVar. It is NaN as soon as the AMPL output contains an invalid value for one shunt, in which case the two maps above keep NaN for this shunt.                                                  |

The per-shunt results are keyed by shunt id, and only the shunts present in `reactiveopf_results_shunts.csv` and found in the network are covered.

Independently of these results, the shunts for which the difference between $\lvert b_{section} \rvert \, V_{nom}^2$ and $\lvert b \rvert \, V_{nom}^2$ exceeds
the `shuntCompensatorActivationAlertThreshold` of [OpenReacParameters](https://github.com/powsybl/powsybl-optimizer/blob/main/open-reac/src/main/java/com/powsybl/openreac/parameters/input/OpenReacParameters.java)
are listed in the `ReportNode` of the run.

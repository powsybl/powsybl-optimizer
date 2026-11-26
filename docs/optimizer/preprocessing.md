# Pre-processing

Before solving the reactive ACOPF described in [AC optimal powerflow](acOptimalPowerflow.md), the following pre-processing blocks are executed to ensure the consistency of the values used in the optimization. 

## Voltage level limit consistency

To ensure consistent voltage level limits for the buses, the configurable domain [`min_plausible_low_voltage_limit`; `max_plausible_high_voltage_limit`] is used (see [Configuration of the run](inputs.md#configuration-of-the-run)). 

Let $V_{s}^{min}$ (resp. $V_{s}^{max}$) be the low (resp. high) voltage limit of substation $s$ specified in `ampl_network_substations.txt` (or in `ampl_network_substations_override.txt` if an override is given for $s$) and $V_{s}^{min,c}$ (resp. $V_{s}^{max,c}$) its associated corrected low (resp. high) limit. Then, the limits are calculated as follows:
- $V_{s}^{min,c} = \max(V_{s}^{min},$ min_plausible_low_voltage_limit)
- $V_{s}^{max,c} = \min(V_{s}^{max},$ max_plausible_low_voltage_limit)

## Zero-impedance threshold

Branches with an impedance magnitude, **calculated in per unit**, lower than the configurable threshold `Znull` (see section [Configuration of the run](inputs.md#configuration-of-the-run)) are considered as non-impedant. 
These branches will have their reactance replaced by the threshold `Znull` (in p.u.), **even if the reactance specified in `ampl_network_branches.txt` is negative**.
Note that for lines considered as non-impedant and connecting two different nominal voltage levels, the conductances and susceptances are cancelled in order to avoid numerical instabilities.

## Impedance of transformers

In the calculations of the ACOPF (see [AC optimal powerflow](acOptimalPowerflow.md)), the impedance specified in tap changer table `ampl_network_tct.txt` is ignored for transformers with an impedance (specified in `ampl_network_branches.txt`) considered as null (see [Zero-impedance threshold](#zero-impedance-threshold)). Note that the transformer ratios and phase shiftings are considered anyway.

For phase shifters transformers considered as impedant, the reactance values from the tap changer table (in `ampl_network_tct.txt`) replace the reactance specified in `ampl_network_branches.txt`. The resistance is then calculated proportionally. 
For the ratio tap changers, the impedance stays as specified in `ampl_network_branches.txt`. **Please notice there is no specified handling for cases where resistance and/or reactance is negative or if there is both a ratio tap changer and a  phase shift transformer on the same branch.**

## P/Q unit domain

The following corrections apply successively to determine consistent domains for the active power and reactive power produced by generators.

To determine the consistent domain of produced active power, the bounds of the domains $P_g^{min}$ and $P_g^{max}$, as well as the target $P_g^{t}$ of generator $g$ (all specified in `ampl_network_generators.txt`) are used.

Let $P_{g}^{min,c}$ and $P_{g}^{max,c}$ be the corrected active bounds. Note that $P_{g}^{max,c}$ is used in the correction of the generator's reactive bounds, and these bounds can be used in the optimization if the user specifies a value for
`coeff_alpha` different from 1 (see [Configuration of the run](inputs.md#configuration-of-the-run)).

$P_{g}^{min,c}$ and $P_{g}^{max,c}$ are computed using the following:

- By default, $P_{g}^{min,c} = \text{defaultPmin}$ and $P_{g}^{max,c} = \text{defaultPmax}$ (see [Configuration of the run](inputs.md#configuration-of-the-run))
- If $|P_g^{max}| \geq \text{PQmax}$, then $P_{g}^{max,c} = \max(\text{defaultPmax}, P_g^t)$
- If $|P_g^{min}| \geq \text{PQmax}$, then $P_{g}^{min,c} = \min(\text{defaultPmin}, P_g^t)$
- If $|P_{g}^{max,c} - P_{g}^{min,c}| \leq \text{minimalQPrange}$, then $P_{g}^{max,c} = P_{g}^{min,c} = P_{g}^t$ (active power is fixed).

To determine the consistent domain of produced reactive power, the reactive power limits $qPc_g$ and $QPc_g$ of generator $g$ at its active power target $P_g^t$ (specified in `ampl_network_generators.txt`) are used.
They are computed by the AMPL export from the reactive capability curve of the generator, and extrapolated when $P_g^t$ is outside the range of the curve's points.
Let $Q_{g}^{min,c}$ and $Q_{g}^{max,c}$ be the corrected reactive bounds, used in the optimization:

- If $|qPc_{g}| \geq \text{PQmax}$, then $Q_{g}^{min,c} = -\text{defaultQmaxPmaxRatio} \times P_{g}^{max,c}$, otherwise $Q_{g}^{min,c} = qPc_{g}$.
- If $|QPc_{g}| \geq \text{PQmax}$, then $Q_{g}^{max,c} = \text{defaultQmaxPmaxRatio} \times P_{g}^{max,c}$, otherwise $Q_{g}^{max,c} = QPc_{g}$.
- If $Q_{g}^{min,c} > Q_{g}^{max,c}$, the values are swapped.
- If $Q_{g}^{max,c} - Q_{g}^{min,c} \leq \text{minimalQPrange}$, then $Q_{g}^{min,c} = Q_{g}^{max,c} = \frac{Q_{g}^{min,c} + Q_{g}^{max,c}}{2}$ (reactive power is fixed).

Please note that in the end, the corrected bounds are used in the reactive OPF (see [AC optimal powerflow](acOptimalPowerflow.md)).

The same reactive-domain correction applies to batteries that regulate voltage, with the reactive power limits taken at their fixed active power $P_b^0$.

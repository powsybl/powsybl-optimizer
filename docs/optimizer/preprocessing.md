# Pre-processing

Before solving the reactive ACOPF described in [AC optimal powerflow](acOptimalPowerflow.md), the following pre-processing blocks are executed to ensure the consistency of the values used in the optimization. 

##Voltage level limit consistency

To ensure consistent voltage level limits for the buses, the configurable domain [`min_plausible_low_voltage_limit`; `max_plausible_high_voltage_limit`] is used (see [Configuration of the run](inputs.md#configuration-of-the-run)). 

Let $V_{s}^{min}$ (resp. $V_{s}^{max}$) be the low (resp. high) voltage limit of substation $s$ specified in `ampl_network_substations.txt` (or in `ampl_network_substations_override.txt` if an override is given for $s$) and $V_{s}^{min,c}$ (resp. $V_{s}^{max,c}$) its associated corrected low (resp. high) limit. Then, the limits are calculated as follows:
- $V_{s}^{min,c} = \max(V_{s}^{min},$ min_plausible_low_voltage_limit)
- $V_{s}^{max,c} = \min(V_{s}^{max},$ max_plausible_low_voltage_limit)

## Zero-impedance branches

Branches with an impedance magnitude, **calculated in per unit**, lower than the configurable threshold `Znull` (see section [Configuration of the run](inputs.md#configuration-of-the-run)) are considered as non-impedant. 
These branches will have their reactance replaced by the threshold `Znull` (in p.u.), **even if the reactance specified in `ampl_network_branches.txt` is negative**.

## Impedance of transformers

In the calculations of the ACOPF (see [AC optimal powerflow](acOptimalPowerflow.md)), the transformers with an impedance (specified in `ampl_network_branches.txt`) considered as null (see [Zero-impedance branches](preprocessing.md#zero-impedance-branches))  **are treated as lines**. Then, the transformation ratios/phase shifts are ignored, as well as the impedance specified in the tap changer table `ampl_network_tct.txt`.

For phase shifters transformers considered as impedant, the reactance values from the tap changer table (in `ampl_network_tct.txt`) replace the reactance specified in `ampl_network_branches.txt`. The resistance is then calculated proportionally. 
For the ratio tap changers, the impedance stays as specified in `ampl_network_branches.txt`. **Please notice there is no specified handling for cases where resistance and/or reactance is negative or if there is both a ratio tap changer and a  phase shift transformer on the same branch.**

## P/Q unit domain

The following corrections apply successively to determine consistent domains for the active power and reactive power produced by generators.

To determine the consistent domain of produced active power, the bounds of the domains $P_g^{min}$ and $P_g^{max}$, as well as the target $P_g^{t}$ of generator $g$ (all specified in `ampl_network_generators.txt`) are used.

Let $P_{g}^{min,c}$ and $P_{g}^{max,c}$ be the corrected active bounds:

- By default, $P_{g}^{min,c} = \text{defaultPmin}$ and $P_{g}^{max,c} = \text{defaultPmax}$ (see [Configuration of the run](inputs.md#configuration-of-the-run))
- If $|P_g^{max}| \geq \text{PQmax}$, then $P_{g}^{max,c} = \max(\text{defaultPmax}, P_g^t)$
- If $|P_g^{min}| \geq \text{PQmax}$, then $P_{g}^{min,c} = \min(\text{defaultPmin}, P_g^t)$
- If $|P_{g}^{max,c} - P_{g}^{min,c}| \leq \text{minimalQPrange}$, then $P_{g}^{max,c} = P_{g}^{min,c} = P_{g}^t$ (active power is fixed).

To determine the consistent domain of produced reactive power, the reactive power diagram (specified in `ampl_network_generators.txt`) of generator $g$ is used: $qp_g$ (resp. $qP_g$) and $Qp_g$ ($QP_g$) when $P_{g}^{min,c}$ (resp. $P_{g}^{max,c}$) is reached.
Let $qp_g^c$ (resp. $qP_g^c$) and $Qp_g^c$ (resp. $QP_g^c$) be the bounds of the corrected reactive diagram, and $Q_{g}^{min,c}$ and $Q_{g}^{max,c}$ be the corrected reactive bounds:

- By default, $qp_g^{c} = qP_{g}^{c} = - \text{defaultPmin} \times \text{defaultQmaxPmaxRatio}$ and $Qp_{g}^{c} = QP_{g}^{c} = \text{defaultPmax} \times \text{defaultQmaxPmaxRatio}$ (see [Configuration of the run](inputs.md#configuration-of-the-run))
- If $|qp_{g}| \geq \text{PQmax}$, then $qp_{g}^{c} = -\text{defaultQmaxPmaxRatio} \times P_{max}^{g,c}$.  
  Same with $qP_{g}^{c}$.  
- If $|Qp_{g}| \geq \text{PQmax}$, then $Qp_{g}^{c} = \text{defaultQmaxPmaxRatio} \times P_{max}^{g,c}$.  
  Same with $QP_{g}^{c}$.  
- If $qp_{g}^{c} > Qp_{g}^{c}$, the values are swapped.  
  Same with $qP_{g}^{c}$ and $QP_{g}^{c}$.  
- If the corrected reactive diagram is too small (the distances between the vertices of the reactive diagram are lower than $\text{minimalQPrange}$), then $qp_{g}^{c} = Qp_{g}^{c} = qP_{g}^{c} = QP_{g}^{c} = \frac{qp_{g}^{c} + Qp_{g}^{c} + qP_{g}^{c} + QP_{g}^{c}}{4}$ (reactive power is fixed).  
- $Q_{g}^{min,c} = \min(qp_{g}^{c}, qP_{g}^{c})$ and $Q_{g}^{max,c} = \min(Qp_{g}^{c}, QP_{g}^{c})$

Please note that in the end, **the corrected bounds are rectangular**, not trapezoidal, and they are used only in the reactive OPF (see [AC optimal powerflow](acOptimalPowerflow.md). The trapezoidal diagram should be added shortly. 
In addition, bounds $qP_{g}^0$ and $Qp_{g}^0$ are not used, as generators with zero active power will be excluded from the optimisation (see [AC optimal powerflow](acOptimalPowerflow.md#generalities)).

The general correction of the generator's reactive power diagram $g$ is illustrated in the following figure:
![Reactive diagram correction](_static/img/reactive-diagram.png){width="30%" align=center}


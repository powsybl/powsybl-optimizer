# Inputs

## Network data

Files with the prefix `ampl_` contain the data and the parameters of the network on which the reactive OPF is executed.

These files are obtained by using the [V2 of the extended version of PowSyBl AMPL export](https://github.com/powsybl/powsybl-core/blob/main/ampl-converter/src/main/java/com/powsybl/ampl/converter/version/ExtendedAmplExporterV2.java), which is the default version.  

Note that from release `0.10.0` of OpenReac, the active target of VSC and LCC converter stations is calculated using HVDC line active set point 
and the converter mode, both specified in `ampl_network_hvdc.txt`. The losses related to rectifier/inverter conversion, and HVDC line **are ignored**.


## Configuration of the run

The user can configure the run with the dedicated Java interface 
(see [OpenReacParameters](https://github.com/powsybl/powsybl-optimizer/blob/main/open-reac/src/main/java/com/powsybl/openreac/parameters/input/OpenReacParameters.java)).
Specifically, the user can set various parameters and thresholds used in the preprocessing and modeling of the reactive OPF. 
These are specified in the file `param_algo.txt`:

| Parameter                                   | Description                                                                                                                                                                       | Java default value | Domain                                        |
|---------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------|-----------------------------------------------|
| `log_level_ampl`                            | Level of display for AMPL prints                                                                                                                                                  | INFO               | {DEBUG, INFO, WARNING, ERROR}                 |
| `log_level_knitro`                          | Level of display for solver prints (see [AMPL documentation](https://dev.ampl.com/ampl/options.html))                                                                             | $1$                | {0, 1, 2}                                     |  
| `objective_choice`                          | Choice of the objective function for the ACOPF (see [AC optimal powerflow](acOptimalPowerflow.md))                                                                               | $0$                | {0, 1, 2}                                     |
| `ratio_voltage_target`                      | Ratio to calculate target V of buses when `objective_choice` is set to $1$ (see [AC optimal powerflow](acOptimalPowerflow.md))                                                   | $0.5$              | $[0; 1]$                                    |
| `coeff_alpha`                               | Weight to favor more/less minimization of active power produced by generators or deviation between them and target values (see [AC optimal powerflow](acOptimalPowerflow.md)) | $1$                | $[0; 1]$                                    |
| `Pnull`                                     | Threshold of active and reactive powers considered as null                                                                                                                        | $0.01$ (MW)        | $[0; 1]$                                    |
| `Znull`                                     | Threshold of impedance considered as null (see [Zero impedance threshold](preprocessing.md#zero-impedance-threshold))                                                                                                                          | $10^{-5}$ (p.u.)   | $[0; 0.1]$                                  |                                                                                                                                                                  
 | `epsilon_nominal_voltage`                   | Threshold to ignore voltage levels with nominal voltage lower than it                                                                                                             | $1$ (kV)           | $\mathbb{R}^{+}$                              | 
| `min_plausible_low_voltage_limit`           | Consistency bound for low voltage limit of voltage levels (see [Voltage level limit consistency](preprocessing.md#voltage-level-limit-consistency))                                                                       | $0.5$ (p.u.)       | $\mathbb{R}^{+}$                              |
| `max_plausible_high_voltage_limit`          | Consistency bound for high voltage limit of voltage levels (see [Voltage level limit consistency](preprocessing.md#voltage-level-limit-consistency))                                                                      | $1.5$ (p.u.)       | [`min_plausible_low_voltage_limit`; $\infty$] |
| `ignore_voltage_bounds`                     | Threshold to replace voltage limits of voltage levels with nominal voltage lower than it, by  [min_plausible_low_voltage_limit; max_plausible_high_voltage_limit]                 | $0$ (p.u.)         | $\mathbb{R}^{+}$                              |
| `buses_with_reactive_slacks`                | Choice of which buses will have reactive slacks attached in ACOPF solving (see [AC optimal powerflow](acOptimalPowerflow.md))                                                    | ALL                | {CONFIGURED, NO_GENERATION, ALL}              |
| `PQmax`                                     | Threshold for maximum active and reactive power considered in correction of generator limits  (see [PQ unit domain](preprocessing.md#pq-unit-domain))                                                    | $9000$ (MW, MVAr)  | $\mathbb{R}$                                  |
| `defaultPmax`                               | Threshold for correction of high active power limit produced by generators (see [PQ unit domain](preprocessing.md#pq-unit-domain))                                                                       | $1000$ (MW)        | $\mathbb{R}$                                  |
| `defaultPmin`                               | Threshold for correction of low active power limit produced by generators (see [PQ unit domain](preprocessing.md#pq-unit-domain))                                                                        | $0$ (MW)           | $\mathbb{R}$                                  |
| `defaultQmaxPmaxRatio`                      | Ratio used to calculate threshold for corrections of high/low reactive power limits (see [PQ unit domain](preprocessing.md#pq-unit-domain))                                                              | $0.3$ (MVAr/MW)    | $\mathbb{R}$                                  |
| `minimalQPrange`                            | Threshold to fix active (resp. reactive) power of generators with active (resp. reactive) power limits that are closer than it (see [PQ unit domain](preprocessing.md#pq-unit-domain))                   | $1$ (MW, MVAr)     | $\mathbb{R}$                                  |
| `default_variable_scaling_factor`           | Default scaling factor applied to all the variables (except reactive slacks and transformer ratios) before ACOPF solving                                                          | $1$                | $\mathbb{R}^{*,+}$                            |
| `default_constraint_scaling_factor`         | Default scaling factor applied to all the constraints before ACOPF solving                                                                                                        | $1$                | $\mathbb{R}^{+}$                              |
| `reactive_slack_variable_scaling_factor`    | Scaling factor applied to all reactive slacks variables before ACOPF solving (see [AC optimal powerflow](acOptimalPowerflow.md))                                                 | $0.1$              | $\mathbb{R}^{*,+}$                            |
| `transformer_ratio_variable_scaling_factor` | Scaling factor applied to all transformer ratio variables before ACOPF solving (see [AC optimal powerflow](acOptimalPowerflow.md))                                               | $0.001$            | $\mathbb{R}^{*,+}$                            |
| `shunt_variable_scaling_factor`             | Scaling factor applied to all shunt variables before ACOPF solving (see [AC optimal powerflow](acOptimalPowerflow.md))                                                           | $0.1$              | $\mathbb{R}^{*,+}$                            |

Please note that for these parameters, the AMPL code defines default values which may be different from those in Java (for example, for the scaling values). This allows a user to use the AMPL code without going through the Java interface, and without providing the file `param_algo.txt`.

In addition to the previous parameters, the user can specify which parameters will be variable or fixed in the ACOPF solving (see [AC optimal powerflow](acOptimalPowerflow.md)).
This is done using the following files:

| File                                  | Description                                                                                                                                             | Default behavior of modified values                                                                         |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `param_transformers.txt`              | Ratio tap changers with a variable transformation ratio (real variable). Note that ratio tap changers on branches with one side open are not optimized. | Transformation ratios are fixed                                                                             |
| `param_shunt.txt`                     | Shunts with a continuous variable susceptance and which can be modified and/or connected (only if possible bus is defined in `ampl_network_shunts.txt`) | Shunt susceptances are fixed                                                                                |
| `param_generators_reactive.txt`       | Generators with a constant reactive power production. If this value is not consistent (> PQmax), the reactive power production stays variable           | Coherent reactive power productions (see [P/Q unit domain](preprocessing.md#pq-unit-domain)) are variable   |
| `param_buses_with_reactive_slack.txt` | Buses with attached reactive slacks if configurable parameter buses_with_reactive_slacks = "CONFIGURED"                                                 | Only buses with no reactive power production have reactive slacks attached                                  |    

All of these files share the same format: 2 columns #"num" "id".

Once again, the user can directly execute the AMPL code without passing these parameters files as input. 
If so, empty files will be created during execution.

## New voltage limits

In addition to the elements specified in section [Configuration of the run](#configuration-of-the-run), the user may choose to override the voltage limits of specified voltage levels. These values must be defined in `ampl_network_substations_override.txt` and are employed to establish the new voltage limits as specified in section [Voltage level limit consistency](preprocessing.md#voltage-level-limit-consistency). 

Format of `ampl_network_substations_override.txt`: 4 columns  
\#"num" "minV (pu)" "maxV (pu)" "id"

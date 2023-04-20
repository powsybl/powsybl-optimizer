# OpenReac
OpenReac is a reactive optimal power flow that gives a set of hypotheses for voltage and reactive controls by network equipments such as generators, shunt compensators and transformers. OpenReac can be used for network planning or in operation as well.

## Getting started
### Knitro
To run this model, in addition of AMPL you'll need Knitro. Knitro is a proprietary non-linear solver.

Artelys is the company developping Knitro. It is distributing keys [here](https://www.artelys.com/solvers/knitro/).

After the installation is done and that you got a valid licence, you must have `knitroampl` in your path.

To check, start a bash and run :
```bash
knitroampl stub
```
## Itools
This project also provides an utilty to run OpenReac with Itools.

1. Run OpenReac on the provided network.
2. Run LoadFlow on the result.
3. Run another OpenReac on the loadflow result.

You will have the running OpenReac folders next to your working directory.

You can also customize the algorithm parameters directly with the option `--open-reac-params params.txt`.
Note on the objective parameters, you must provide one of these lines:
``` properties
# Minimum power generation (default)
obj_min_gen
# Target low_voltage_limit + (high_voltage_limit - low_voltage_limit) * RATIO for each equipement
obj_target_ratio RATIO
# Use the target voltage provided in the network file
obj_provided_target_v
```
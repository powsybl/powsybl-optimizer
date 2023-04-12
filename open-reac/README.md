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

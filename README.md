# PowSyBl Optimizer

[![Actions Status](https://github.com/powsybl/powsybl-optimizer/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/powsybl/powsybl-optimizer/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=com.powsybl%3Apowsybl-optimizer&metric=coverage)](https://sonarcloud.io/component_measures?id=com.powsybl%3Apowsybl-optimizer&metric=coverage)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.powsybl%3Apowsybl-optimizer&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.powsybl%3Apowsybl-optimizer)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Slack](https://img.shields.io/badge/slack-powsybl-blueviolet.svg?logo=slack)](https://join.slack.com/t/powsybl/shared_invite/zt-36jvd725u-cnquPgZb6kpjH8SKh~FWHQ)

PowSyBl (**Pow**er **Sy**stem **Bl**ocks) is an open source framework written in Java, that makes it easy to write complex
software for power systems’ simulations and analysis. Its modular approach allows developers to extend or customize its
features.

PowSyBl is part of the LF Energy Foundation, a project of The Linux Foundation that supports open source innovation projects
within the energy and electricity sectors.

<p align="center">
<img src="https://raw.githubusercontent.com/powsybl/powsybl-gse/main/gse-spi/src/main/resources/images/logo_lfe_powsybl.svg?sanitize=true" alt="PowSyBl Logo" width="50%"/>
</p>

Read more at https://www.powsybl.org !

This project and everyone participating in it is under the [Linux Foundation Energy governance principles](https://www.powsybl.org/pages/project/governance.html) and must respect the [PowSyBl Code of Conduct](https://github.com/powsybl/.github/blob/main/CODE_OF_CONDUCT.md).
By participating, you are expected to uphold this code. Please report unacceptable behavior to [powsybl-tsc@lists.lfenergy.org](mailto:powsybl-tsc@lists.lfenergy.org).


## PowSyBl vs PowSyBl Optimizer
PowSyBl Optimizer provides production-ready optimal power flow optimizers:

- OpenReac is a reactive optimal power flow that gives a set of hypotheses for voltage and reactive controls by network equipment such as generators, shunt compensators and transformers. OpenReac can be used for network planning or in operation as well.


## Environment requirements

### Java / Maven
PowSyBl-optimizer project is partly written in Java, so you need few requirements:
- JDK *(21 or greater)*
- Maven *(3.8.1 or greater, 3.9.x recommended)*

### AMPL
For this project, you must also have [AMPL](https://ampl.com/) installed.
AMPL is a proprietary tool that works as an optimization modelling language. It can be interfaced with many solvers.

AMPL is sold by many companies including Artelys, you can find keys [here](https://www.artelys.com/solvers/ampl/).

You must add in your `~/.itools/config.yml` an ampl section like this:
```yaml
ampl:
  # Change to the ampl folder path that contains the ampl executable
  homeDir: /home/user/ampl
```

### Non-linear optimization solver

To run the model implemented in AMPL, you’ll need a non-linear optimization solver. By default, the AMPL code is configured to run Knitro, which is a proprietary non-linear solver, but you are free to configure a different one.

If you chose to run Knitro, you must have `knitroampl` in your path, after the installation of the solver is done and that you got a valid licence.


## Building / running tests

To run all the tests, simply launch the following command from the root of the repository:
```
$> mvn package
```

## Documentation

Latest version of the documentation is available [here](https://powsybl.readthedocs.io/projects/powsybl-optimizer/en/stable/).

To contribute to the documentation follow the instructions in the [documentation README](https://github.com/powsybl/powsybl-optimizer/blob/main/docs/README.md) page.

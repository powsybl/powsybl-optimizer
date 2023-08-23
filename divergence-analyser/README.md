# Divergence Analyser

## Overview

The divergence-analyzer module includes a tool intended to assist users in identifying inconsistent values, within power network parameters.
It should be used in cases where load flow calculations 
from [powsybl-open-loadflow](https://github.com/powsybl/powsybl-open-loadflow) fail to converge 
due to such inconsistencies.

The tool relies on an AMPL-implemented Mixed Integer Non-Linear Program (MINLP), which is solved using the Knitro solver.
This MINLP applies penalties to power network parameters by solving corresponding load flow equations.
It provides users with the calculated penalties and indicators, facilitating the identification of the inconsistencies in power network parameters.

The capabilities and limitations of the tool are outlined in the document [INCOMING].


## Getting started

### AMPL
For this project, you must have [AMPL](https://ampl.com/) installed.
AMPL is a proprietary tool that works as an optimization modelling language, and it can be interfaced with many solvers.
AMPL is sold by many companies, including Artelys, and you can find keys [here](https://www.artelys.com/solvers/ampl/).

Then, you must add in your `~/.itools/config.yml`, where `~` is your local home directory (where your personal files and settings are stored), an ampl section like this:
```yaml
ampl:
  # Change to the ampl folder path that contains the ampl executable
  homeDir: /home/user/ampl
```

For Windows users, be careful not to include any spaces in the specified path.
### Knitro

From the AMPL code of the divergence analysis, the Knitro solver is called. Knitro is an optimization solver specialized in solving nonlinear problems.

It is developed and distributed by Artelys, and you can obtain it [here](https://www.artelys.com/solvers/knitro/). After obtaining Knitro, please ensure that the Knitro executable is included in your system's path.

## How to use it

An illustrative usage example of the divergence analyzer can be found in the test [```UseCaseTest```](https://github.com/powsybl/powsybl-optimizer/blob/divergence-analyser/divergence-analyser/src/test/java/com/powsybl/divergenceanalyser/UseCaseTest.java).

In general, the tool must be apply to power networks for which the load flow calculations
of [powsybl-open-loadflow](https://github.com/powsybl/powsybl-open-loadflow) fail to converge. Then,
the user can run the divergence analyzer using the following code:

 ```java
// ... load the network 
// ... lf calculations fail to converge 

DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
parameters.setAllPenalization(true);
DivergenceAnalyserResults results = DivergenceAnalyser.runDivergenceAnalysis(network, parameters);
 ```

The initial two lines establish the launch options 
for the tool. 
These options determine which parameters of 
the inconsistent power network can
be subjected to penalties, thereby guiding the search for inconsistencies. 
Refer to the next section for further details. **The default setting has no penalization active**, 
and invoking the method ```setAllPenalization(true)``` enables the penalization of all network parameters.

Once the results are obtained, the user can print the computed penalties (expressed in per unit 
or international units) and run indicators, using the following code:

 ```java
results.printPenalizationPu();
results.printIndicators();
```

This information should guide the user towards inconsistencies in parameters of the network.


### Parametrization

The user can opt to target specific parameters, 
introducing a controlled bias during the divergence analysis.
**It is highly recommended to apply such a bias when it is possible, especially when the user has an idea of which parameter could be inconsistent.**
In fact, this approach directly influences the structure of the solved MINLP, thereby simplifying its resolution.

Based on the [load flow equations used in powsybl-open-loadflow](https://www.powsybl.org/pages/documentation/simulation/powerflow/openlf.html), the following parameters can be penalized (or modified):
- The target V of generators.
- The target V of static var compensators.
- The admittance Y and the angle Xi (involved in admittance calculation) of branches.
- The conductance G1 and susceptance B1 on side 1 of the branches.
- The conductance G2 and susceptance B2 on side 2 of the branches.
- The voltage ratio rho, and the angle shifting alpha for transformers.

All of this is explained and detailed in the report [INCOMING].

It is also possible to adjust MINLP resolution parameters. These include:
- The MINLP solving mode (integer, nonlinear relaxation, or MPEC). By default, the problem is solved in integer mode.
- The maximum solving time. By default, this time is set to 120 seconds.

For instance, a user aiming to penalize the impedances of all the branches and target V of generators, 
while solving with nonlinear relaxation and a maximum solving time of 45 seconds 
can use the following code:

 ```java
DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
parameters.setYPenal(true)
        .setXiPenal(true)
        .setTargetVUnitsPenal(true)
        .setResolutionNlp()
        .setMaxTimeSolving(45);
 ```
# State Estimator

## Table of content

- [Overview](#overview)
- [Getting started](#getting-started)
- [How to use the State Estimator](#how-to-use-the-state-estimator)
- [Description of the module and classes](#description-of-the-module-and-classes)

## Overview

The `robust-state-estimator` module is a tool to perform the state estimation of a network imported on PowSyBl. Given a certain set of measurements, it returns estimates for the values of voltage magnitude and voltage angle at every node of the network, as well as for the active and reactive power flows on all branches (the latter being derived from voltages). It is designed to be robust against topology errors (e.g. a branch with erroneous status) as well as gross measurement errors (e.g. a measurement presenting a significant deviation from standard noise expected from the measurement device).

The tool relies on an implementation in AMPL of a Mixed Integer Non-Linear Program (MINLP), which is solved using the solver Artelys Knitro. Depending on the modalities of the state estimation the user whishes to perform, the problem sometimes reduces to a Non-Linear Program (NLP).

The State Estimation (SE) problem is formulated in AMPL as a Weighted Least Squares (WLS) problem: its goal is to minimize the sum over all measurements of the squares of weighted residuals (a weighted residual being defined as the difference between the measurement estimate and the measurement value, divided by the measurement standard deviation). Note that a formulation of the SE problem as a Weighted Least Absolute Values (WLAV) estimator is also available.

Additionnaly, the SE formulation includes equality constraints to model zero-injection buses and reasonable bounds on the possible values of the conventional state variables (nodal voltages). Above all, it models with binary variables the branch statuses ("closed"/"open"), therefore making the network topology a direct output of the state estimator.

---
## Getting started

### AMPL
For this project, you must have [AMPL](https://ampl.com/) installed.
AMPL is a proprietary tool that works as an optimization modelling language and can be interfaced with many solvers.
AMPL is sold by many companies, including Artelys. Keys can be found [here](https://www.artelys.com/solvers/ampl/).

Then in `~/.itools/config.yml` (where `~` corresponds to your local home directory, i.e. where your personal files and settings are stored), you must add the following AMPL section:
```yaml
ampl:
  # Change to the AMPL folder path that contains the AMPL executable
  homeDir: /home/user/ampl
```

**For Windows users, be careful not to include any spaces in the specified path.**

### Knitro

The AMPL code is run from the Java code of the state estimator, which in turn calls the solver Knitro to solve the problem at hand. Knitro is an optimization solver specialized in solving nonlinear problems.

It is developed and distributed by Artelys, and can be obtained [here](https://www.artelys.com/solvers/knitro/). After obtaining Knitro, **please ensure that the Knitro executable is included in your system's path.**

---
## How to use the State Estimator

An illustrative usage example of the state estimator can be found in the test [```UseExample```](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/test/java/com/powsybl/stateestimator/UseExample.java). Let's explain how to use the state estimation module.

First, a network must be loaded.
 ```java
Network network = network.read(...);
 ```

Then an instance of the class [```StateEstimatorKnowledge```](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/parameters/input/knowledge/StateEstimatorKnowledge.java) must be created. This object gathers all the data (measurements, reference bus for voltage angles, zero injection buses, set of suspect branches) that will be provided to the state estimator as inputs. 
 ```java
// Create StateEstimatorKnowledge object
StateEstimatorKnowledge knowledge = new StateEstimatorKnowledge(network);
// Specify the reference bus
knowledge.setSlack("Bus-1", network);
// Specify the presumed status (open/closed) of a branch and whether it should be suspect or not
knowledge.setSuspectBranch("Line-1-2", true, "PRESUMED OPENED");
// Create a measurement
Map<String, String> measurement = Map.of("BranchID","Line-1-2","FirstBusID","Bus-1","SecondBusID","Bus-2","Value","32.6","Variance","0.1306","Type","Pf");
// Add it to the set of measurements
knowledge.addMeasure(measurement);
 ```

 When the user does not have a real set of measurements to provide and only wishes to build test cases, he can use the [```RandomMeasuresGenerator```](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/parameters/input/knowledge/RandomMeasuresGenerator.java) class. This class provides methods to randomly generate measurements out of the Load Flow results. Obviously, the user first has to solve the Load Flow problem, so that results are stored in the network object. 
 
 Note: to emulate a topology error, the user must disconnect the erroneous branch before launching the LF resolution, and reconnect it right after.
  ```java
// Solve the Load Flow problem (results are stored in "network")
LoadFlowParameters parametersLf = new LoadFlowParameters();
LoadFlowResult loadFlowResult = LoadFlow.run(network, parametersLf);
// Randomly generate measurements (useful for test cases) out of Load Flow results
var parameters = new RandomMeasuresGenerator.RandomMeasuresGeneratorParameters();
parameters.withSeed(seedNumber).withRatioMeasuresToBuses(ratioZtoN).withAddNoise(true).withEnsureObservability(true);
RandomMeasuresGenerator.generateRandomMeasurements(knowledge, network, parameters);
 ```

 The user must also define the solving options for the state estimator in a [StateEstimationOptions](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/parameters/input/options/StateEstimatorOptions.java) object. When the WLS SE problem is a MINLP (binary variables activated for branch statuses), he can choose the solving mode employed by Knitro (integer, non-linear relaxation or MPEC. See [Knitro documentation](https://www.artelys.com/app/docs/knitro/3_referenceManual.html)). He can also choose the maximum resolution time (in seconds), the maximum number of branch statuses the solver is allowed to make, and decide whether Knitro is allowed to use its "multistart" option.
```java
// Define options for the state estimation
StateEstimatorOptions options = new StateEstimatorOptions();
options.setSolvingMode(0).setMaxTimeSolving(30).setMaxNbTopologyChanges(5).setMipMultistart(0);
```

Once the user has provided all the desired inputs, the state estimator can be run. The `runStateEstimation` method includes the possibility to choose whether to use a WLS or the WLAV estimator. Results are stored in a [StateEstimatorResults](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/StateEstimatorResults.java) object.
```java
// Run the state estimator
StateEstimatorResults results = StateEstimator.runStateEstimation(network, network.getVariantManager().getWorkingVariantId(), knowledge, options, new StateEstimatorConfig(true), new LocalComputationManager(), "WLAV");      
```

From there, several ways of displaying the results exist:
```java
// Print all estimates (nodal voltages and network topology), using engineering (SI) units where needed
results.printAllResultsSi(network);
// Print all estimates (nodal voltages and network topology), using per-unit values where needed
results.printAllResultsPu();
// Print only state vector estimate
results.printStateVectorSi(network);
results.printStateVectorPu();
// Print only network topology estimate
results.printNetworkTopology();
// Print/export all measurements along with their estimated values and residuals
results.printAllMeasurementEstimatesAndResidualsSi();
results.exportAllMeasurementEstimatesAndResidualsSi();
// Print some indicators on the Knitro resolution and the network studied
results.printIndicators();
// Estimated values for specific variables can also be obtained
results.getBranchPowersEstimate("Line-1-2"); // power flows
results.getBusStateEstimate("Bus-1"); // nodal voltage
results.getBranchStatusEstimate("Line-1-2"); // branch status estimate
```

Instead of using the straightforward version of the state estimator with the ``runStateEstimation`` method from the [StateEstimator](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/StateEstimator.java) class (meaning the WLS SE problem is solved only once), the user is advised to use the run method ``runHeuristic`` from the [StateEstimatorHeuristic](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/StateEstimatorHeuristic.java) class. This method employs a heuristic algorithm that solves the WLS SE problem multiple times, analyzing the results obtained at each resolution in order to detect and correct any error in the data provided. **In particular, if the data may contain a gross measurement error or a topology error, the user is strongly advised to use this heuristic algorithm.** The object returned contains three components:
1. The final results/estimates obtained, in the form of a ``StateEstimatorResults`` object.
2. The final set of inputs used by the algorithm at its last iteration (= WLS SE resolution), in the form of a ``StateEstimatorKnowledge`` object. For instance, if the algorithm has detected and removed a gross measurement error, the set of measurements in the final ''knowledge'' will not contain it, unlike the initial ``StateEstimatorKnowledge`` instance provided by the user at the beginning.
3. The number of iterations (=WLS SE resolutions) performed by the algorithm.
```java
// Run SE heuristic algorithm with "initialKnowledge"
Map<String, Object> heuristicResults = StateEstimatorHeuristic.runHeuristic(initialKnowledge, network);
StateEstimatorResults finalResults = (StateEstimatorResults) heuristicResults.get("Results");
StateEstimatorKnowledge finalKnowledge = (StateEstimatorKnowledge) heuristicResults.get("Knowledge");
int nbIter = (int) heuristicResults.get("NbIter");
```

Finally, note that when the user has generated randomly the set of measurements for testing purposes, he can use the methods of the [StateEstimatorEvaluator](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/StateEstimatorEvaluator.java) class to compute various statistics based on the errors between estimated values (as returned by the state estimator) and true values (as returned by the Load Flow resolution):
```java
StateEstimatorEvaluator evaluator = new StateEstimatorEvaluator(network, knowledge, results);
// Compute some indicators on the accuracy of the state estimation w.r.t Load Flow solution
List<Double> voltageErrorStats = evaluator.computeVoltageRelativeErrorStats();
List<Double> angleErrorStats = evaluator.computeAngleDegreeErrorStats();
List<Double> activePowerFlowErrorStats = evaluator.computeActivePowerFlowsRelativeErrorsStats();
List<Double> reactivePowerFlowErrorStats = evaluator.computeReactivePowerFlowsRelativeErrorsStats();
double performanceIndex = evaluator.computePerformanceIndex(); 
```

---

## Description of the module and classes

This section provides a quick overview of the main classes of the `robust-state-estimator` module.

The tree structure of `robust-state-estimator/src` is as follows:

```
├── main
│   ├── java
│   │   ├── parameters
│   │   │   ├── input
│   │   │   │   ├── knowledge
│   │   │   │   │   ├── StateEstimatorKnowledge
│   │   │   │   │   ├── "..."Measures (ActivePowerFlow/VoltageMagnitude/etc)
│   │   │   │   │   ├── SuspectBranches
│   │   │   │   │   ├── StateVectorStartingPoint
│   │   │   │   │   ├── ZeroInjectionBuses
│   │   │   │   │   ├── SlackBus
│   │   │   │   ├── options
│   │   │   │   │   ├── StateEstimatorOptions
│   │   │   │   │   ├── SolvingOptions
│   │   │   │   ├── measuregeneration
│   │   │   │   │   ├── RandomMeasuresGenerator
│   │   │   ├── output
│   │   │   │   ├── estimates
│   │   │   │   │   ├── BranchPowersEstimate
│   │   │   │   │   ├── BranchStatusEstimate
│   │   │   │   │   ├── BusStateEstimate
│   │   │   │   ├── "..."EstimateOutput (StateVector/NetworkTopology/etc)
│   │   │   │   ├── "..."IndicatorsOutput (Run/Network)
│   │   │   ├── StateEstimatorAmplIOFiles
│   │   ├── StateEstimator"..." (Model/Runner/Results/Evaluator/Heuristic/etc)
│   ├── resources
│   │   ├── AMPL files (.mod/.run/.dat)
├── test
│   ├── java
│   │   ├── UseExample
│   ├── resources
│   │   ├── config.yml
│   │   ├── filelist.txt
```

`knowledge` folder contains all classes related to the pieces of knowledge given as inputs to the state estimator: measurements, presumed branch statuses, initial point for the state vector, zero-injection buses and slack (= reference) bus. Each type of input has a class of its own, which must implement the `AmplInputFile` class so that data can be written in a CSV file and exported from Java towards AMPL.

`measuresgeneration` folder contains the [RandomMeasuresGenerator](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/parameters/input/knowledge/RandomMeasuresGenerator.java), which can be used for generating sets of measurements for testing purposes, as explained in [How to use the State Estimator](#how-to-use-the-state-estimator).

Classes used to defined the solving options of the state estimation problem, gathered in a [StateEstimatorOptions](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/parameters/input/options/StateEstimatorOptions.java), are present in the folder `options`.

Once all prior knowledge to the state estimation and solving options have been provided by the user, the state estimator can be run. It makes use of the following main functions: [StateEstimator](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/StateEstimator.java), [StateEstimatorRunner](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/StateEstimatorRunner.java) and [StateEstimatorModel](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/StateEstimatorModel.java).

`resources` folder contains all AMPL files (.mod/.dat/.run) at the core of the state estimator. In particular, it contains the mathematical formulation of the state estimator as an optimization problem to be solved. When running a state estimation, all the necessary files (network data, measurement files, solving options, etc) are exported by the [StateEstimatorRunner](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/StateEstimatorRunner.java) class as CSV and text files. Once the (MI)NLP is solved, an AMPL script exports the results and resolution indicators as output files. Note that the two formulations provided for the estimator (WLS and WLAV) are in `resources`.

The class [StateEstimatorAmplIOFiles](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/parameters/StateEstimatorAmplIOFiles.java) represents the interface between Java and AMPL. It handles both input files provided to AMPL (related to `knowledge` folder) and output files exported by AMPL.

These output files are written in CSV format. They are retrieved by the [StateEstimatorAmplIOFiles](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/parameters/StateEstimatorAmplIOFiles.java) class and read into Java-formatted objects. Classes related to these objects are gathered in the `output` folder. Again, there is one class for each type of results/indicators: [StateVectorEstimateOutput](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/parameters/output/StateVectorEstimateOutput.java) (estimates on nodal voltages), [NetworkTopologyEstimateOutput](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/parameters/output/NetworkTopologyEstimateOutput.java) (estimates on branch statuses), [NetworkPowersEstimateOutput](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/parameters/output/NetworkPowersEstimateOutput.java) (estimates on branch power flows), etc. Certain classes makes use of sub-classes gathered in `estimates` folder. For example, an instance of the [StateVectorEstimateOutput](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/parameters/output/StateVectorEstimateOutput.java) class is actually composed of multiple instances of the [BusStateEstimate](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/parameters/output/estimates/BusStateEstimate.java) class (one for each bus in the network.)

Once read, results are then stored in a easy-to-handle [StateEstimatorResults](https://github.com/powsybl/powsybl-optimizer/blob/robust-state-estimator/robust-state-estimator/src/main/java/com/powsybl/stateestimator/StateEstimatorResults.java) object, as explained in [How to use the State Estimator](#how-to-use-the-state-estimator). Multiple methods are provided to display the results.

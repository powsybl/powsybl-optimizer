# Division of the code

The code of the reactive OPF is divided into several files, each serving a specific function:

- `reactiveopf.dat` defines the network data files imported (files with *ampl_* prefix), and the files used to configure the run (files with *param_* prefix).  
  See [Inputs](inputs.md).
  
- `iidm_importer.mod`, `or_param_importer.mod` and `commons.mod` define the sets and parameters of the optimization.

- `connected_component.mod`, `dcopf.mod` and `acopf.mod` define the optimization problems solved in `reactiveopf.run`.  
  See [Slack bus and main connex components](slackBusMainConnexComponent.md), [DC optimal power flow](dcOptimalPowerflow.md) and [AC optimal power flow](acOptimalPowerflow.md), respectively.
  
- `connected_component.run`, `dcopf.run`, `acopf_preprocessing.run` and `acopf.run` orchestrate the optimization and its post-processing.

- `reactiveopfoutput.mod` exports result files if the execution of `reactiveopf.run` is successful.  
  See [Outputs](outputs.md#in-case-of-convergence).
  
- `reactiveopfexit.run` contains the code executed when the process fails.  
  Refer to section [8.2](outputs.md#in-case-of-inconsistency).
  
- `reactiveopf.run` executes the AMPL process of OpenReac, calling the previous scripts.



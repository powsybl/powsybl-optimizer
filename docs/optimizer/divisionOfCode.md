# Division of the code

The code of the reactive OPF is divided into several files, each serving a specific function:

- `reactiveopf.dat` defines the network data files imported (files with *ampl_* prefix), and the files used to configure the run (files with *param_* prefix).  
  See [Inputs](inputs.md).
  
- `iidm_importer.mod`, `or_param_importer.mod` and `commons.mod` define the sets and parameters of the optimization.

- `network_info.run` checks that the main synchronous component is not empty and that the slack bus belongs to it, and prints general information about the network.  
  See [Slack bus and main synchronous component](slackBusMainSynchronousComponent.md).
  
- `acopf.mod` defines the optimization problem solved in `reactiveopf.run`.  
  See [AC optimal power flow](acOptimalPowerflow.md).
  
- `acopf_preprocessing.run` and `acopf.run` orchestrate the optimization and its post-processing.

- `reactiveopfoutput.run` exports result files if the execution of `reactiveopf.run` is successful.  
  See [Outputs](outputs.md#in-case-of-convergence).
  
- `reactiveopfexit.run` contains the code executed when the process fails.  
  Refer to section [8.2](outputs.md#in-case-of-inconsistency).
  
- `reactiveopf.run` executes the AMPL process of OpenReac, calling the previous scripts.

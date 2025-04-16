# Getting started

## AMPL
For this project, you must have [AMPL](https://ampl.com/) installed on your machine.
AMPL is a proprietary tool that works as an optimization modelling language, 
and it can be interfaced with many solvers.

To run OpenReac, you must add in your `~/.itools/config.yml` an ampl section like this:
```yaml
ampl:
  # Change to the ampl folder path that contains the ampl executable
  homeDir: /home/user/ampl
```

For Windows users, make sure there are no spaces in the provided path.

## Non-linear optimization solver

To run the model implemented in AMPL, you'll need a non-linear optimization solver.
By default, the AMPL code is configured to run Knitro, which is a proprietary non-linear solver, but you
are free to configure a different one.

If you chose to run Knitro, you must have `knitroampl` in your path, after the installation
of the solver is done and that you got a valid licence.

## First Run

Using PowSyBl-OpenReac to run an OPF is simple: 
just load the network you want and launch the optimization with the configuration of your choice.

You first need to add a few Maven dependencies to gain access to the network model, customization of the platform configuration 
and simple logging capabilities:
```
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-iidm-impl</artifactId>
    <version>6.7.0</version>
</dependency>
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-config-classic</artifactId>
    <version>6.7.0</version>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.13</version>
</dependency>
```

You are now able to load a network:
```Java
Network network = MetrixTutorialSixBusesFactory.create();
```

After adding a last Maven dependency on OpenReac implementation:
```
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-open-reac</artifactId>
    <version>0.12.0</version>
</dependency>
```

You can run the optimizer on the network, and apply the optimization results (see [Outputs](./optimizer/outputs.md#in-case-of-convergence)) on it:
```
OpenReacParameters parameters = new OpenReacParameters();
OpenReacResult results = run(network, network.getVariantManager().getWorkingVariantId(), parameters);
results.applyAllModifications(network);
```

To verify the quality of the solution computed by OpenReac, the load flow implementation of [PowSyBl Open Load Flow](https://github.com/powsybl/powsybl-open-loadflow) can be used.
Refer to the [dedicated documentation](https://powsybl.readthedocs.io/projects/powsybl-open-loadflow/en/latest/index.html) for more details.

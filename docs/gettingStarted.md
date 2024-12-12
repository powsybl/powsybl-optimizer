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

## Non-linear optimization solver

To run the model implemented in AMPL, you'll need a non-linear optimization solver.
By default, the AMPL code is configured to run Knitro, which is a proprietary non-linear solver, but you
are free to configure a different one.

If you chose to run Knitro, you must have `knitroampl` in your path, after the installation
of the solver is done and that you got a valid licence.


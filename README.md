# Powsybl-optimizer
A repository to store production-ready optimal powerflow optimizers

## Getting started

### AMPL
For this project, you must have [AMPL](https://ampl.com/) installed.
AMPL is a proprietary tool that works as an optimization modelling language. It can be interfaced with many solvers.

AMPL is sold by many companies including Artelys, you can find keys [here](https://www.artelys.com/solvers/ampl/).

You must add in your `~/.itools/config.yml` an ampl section like this:
```yaml
ampl:
  # Change to the ampl folder path that contains the ampl executable
  homeDir: /home/user/ampl
```

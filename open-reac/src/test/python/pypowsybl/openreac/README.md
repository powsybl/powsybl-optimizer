# LoadFlowTest

LoadFlowTest is a Python script for calculating the load flow in an openreac network.

## Installation

Use the package manager [pip](https://pip.pypa.io/en/stable/) to install pypowsybl first.

```bash
pip install pypowsybl
```

## Usage

```python
# import class
from test_genral_cases import LoadFlowTest

# xiidm file
path = f"file.xiidm"

# call LoadFlowTest with the path, and optionally high voltage bound and update voltage bound for pst
# lf = LoadFlowTest(path)
lf = LoadFlowTest(path, 1, 1)

# calculate
lf.calculate()

```

## Contributing

Pull requests are welcome. For major changes, please open an issue first
to discuss what you would like to change.

Please make sure to update tests as appropriate.
# Slack bus and main connex component
 
The slack bus $s$ is determined by identifying the bus with the **highest number of AC branches connected**, within the main component (`cc` set to $0$ in `ampl_network_buses.txt`). 
If multiple buses have such cardinality, the one with the highest identifier (`num` parameter) is chosen.
In the event that no bus satisfies these conditions, the first bus defined in `ampl_network_buses.txt` is selected.

The OPFs are executed on the **main connex component** (i.e. buses connected to slack bus by AC branches) of the network.
Consequently, **buses connected to the slack only by HVDC lines are excluded**.

This component is determined by solving the following optimization problem (the variables are bolded):

$$\text{maximize} \left(\sum\limits_{i} \boldsymbol{\theta_i^{cc}}\right)$$

where $\boldsymbol{\theta_i^{cc}}$ is the voltage angle of bus $i$, and with the following constraints:

$$\boldsymbol{\theta_s^{cc}} = 0 \quad (1)$$

$$\boldsymbol{\theta_i^{cc}} - \boldsymbol{\theta_j^{cc}} = 0, \quad ij \in BRANCH \quad (2)$$

$$0 \leq \boldsymbol{\theta_i^{cc}} \leq 1, \quad i \in BUS \quad (3)$$ 

If the solving is unsuccessful,  the script `reactiveopfexit.run` is executed (see [In case of inconsistency](outputs.md#in-case-of-inconsistency)) and the execution is stopped.
The sets of buses and branches belonging to the main connex component are now denoted $BUSCC$ and $BRANCHCC$, respectively.

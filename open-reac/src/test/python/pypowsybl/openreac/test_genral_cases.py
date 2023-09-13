from pypowsybl import network, voltage_initializer, loadflow
from pypowsybl._pypowsybl import VoltageInitializerObjective, VoltageInitMode
import os

class LoadFlowTest:
    
    def __init__(self, path, def_high_voltage_bound = 1, update_voltage_bound_pst_const = 1):
        self.path, self.casepath = os.path.split(path)
        self.params = voltage_initializer.VoltageInitializerParameters()
        self.n = network.load(path)
        
        # override high voltage 
        self.def_high_voltage_bound = def_high_voltage_bound
        # override voltage bounds for transformers check
        self.update_voltage_bound_pst_const = update_voltage_bound_pst_const
    

        # list of bus_id with transformer check fail
        self.id_list = ['BRAEKP1','DAMBRP1', 'DOMLOP1', 'COCHEP1', 'EGUZOP1', 'MARMAP1', 'QUINTP1', 'VALDIP1', 'MIMI5P1', 'MARTYP1']
        self.nominal_v_list = [19.0 for _ in self.id_list]
        self.low_voltage_limit_list = [18.0 for _ in self.id_list]
        self.high_voltage_limit_list = [21.0 for _ in self.id_list]

    def _update_voltage_pst(self,id_data,nominal_v_data,low_voltage_limit_data,high_voltage_limit_data):
        self.n.update_voltage_levels(id= id_data, nominal_v= nominal_v_data, 
                                low_voltage_limit = low_voltage_limit_data,
                                high_voltage_limit= high_voltage_limit_data )

    def calculate(self):
        if self.def_high_voltage_bound == 1:
            # update powsyble voltage bound error -> Voltage level '4REVIP7' has only one voltage limit defined (min:100.0, max:NaN). Please define none or both.
            self.n.update_voltage_levels(id='4REVIP7', low_voltage_limit = 360.0, high_voltage_limit= 440.0)
            
        if self.update_voltage_bound_pst_const == 1: 
            if (self.casepath == 'EdZ_Est_Jan09.xiidm') or (self.casepath == 'EdZ_Est_Jun4.xiidm') or (self.casepath == 'EdZ_Est_Jun10.xiidm'):
                # "ERROR INFEASIBLE transformer BRAEKY611 BRAEKP1(20kV)->BRAEKP6(225kV) cstratio=1.000 ratio_min=1.165 ratio_max=1.165 : 
                # Vmin1=0.950 * ratio_min > Vmax2=1.089"'  
                self.n.update_voltage_levels(id='BRAEKP1', nominal_v= 19.0 , low_voltage_limit = 18.0, high_voltage_limit= 21.0)
                
            if self.casepath == 'INT_Dopler.xiidm':
                self._update_voltage_pst(self.id_list,self.nominal_v_list,self.low_voltage_limit_list,self.high_voltage_limit_list)


        self.params.set_objective_distance(VoltageInitializerObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT)
        results = voltage_initializer.run(self.n, self.params, True)
        results.apply_all_modification(self.n)
        print(results.status())
        print(results.indicators())

        # initialize dc values for loadflow
        p = loadflow.Parameters(voltage_init_mode = VoltageInitMode.DC_VALUES)
        # run AC loadflow
        results_ac = loadflow.run_ac(self.n , p)
        print(f"results_ac \n {results_ac}")
        lf_values_ac = self.n.get_lines()[['p1', 'p2', 'q1', 'q2']].round(2)
        print(f"lf_values_ac \n {lf_values_ac}")

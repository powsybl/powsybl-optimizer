package com.powsybl.openreac.network;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;

public class OneSideOpenNetworkFactory extends AbstractLoadFlowNetworkFactory {

    public static Network createWithOpenedOnSide1() {
        Network network = create();
        Bus b0 = createBus(network, "b0");
        Bus b1 = network.getBusView().getBus("b1");
        createLine(network, b0, b1, "l01", 0, 0.1f, 0.1f, 0.1f);
        network.getLine("l01").getTerminal1().disconnect();
        return network;
    }


    /**
     *      0 MW, 0 MVar
     *   1 ===== 1pu
     *       |
     *       |
     *       | 0.1j
     *       |
     *       |
     *   2 ===== 1pu
     *      200 MW, 100 MVar
     */
    public static Network create() {
        Network network = Network.create("2-bus", "code");
        Bus b1 = createBus(network, "b1");
        Bus b2 = createBus(network, "b2");
        createGenerator(b1, "g1", 2, 1);
        createLoad(b2, "l1", 2, 1);
        createLine(network, b1, b2, "l12", 0.1f);
        return network;
    }

}

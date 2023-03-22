package com.powsybl.openreac.parameters;

import com.powsybl.commons.PowsyblException;

public class InvalidParametersException extends PowsyblException {
    public InvalidParametersException(String message){
        super(message);
    }
}

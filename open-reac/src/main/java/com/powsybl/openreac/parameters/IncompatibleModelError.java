package com.powsybl.openreac.parameters;

/**
 * Throw this error when the interface between ampl and java is not correct.
 * This is an internal OpenReac error. It is not the user fault.
 */
public class IncompatibleModelError extends Error {
    public IncompatibleModelError(String message){
        super("Error of compatibility between the ampl model and the interface, this is a OpenReac issue.\n" + message);
    }

}

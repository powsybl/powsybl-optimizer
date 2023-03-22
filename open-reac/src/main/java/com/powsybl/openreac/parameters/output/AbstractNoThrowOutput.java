package com.powsybl.openreac.parameters.output;

public abstract class AbstractNoThrowOutput implements NoThrowAmplOutput{
    private boolean errorState;
    @Override
    public boolean isErrorState() {
        return errorState;
    }

    protected void triggerErrorState() {
        errorState = true;
    }

}

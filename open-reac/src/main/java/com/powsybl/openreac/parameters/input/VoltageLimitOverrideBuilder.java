package com.powsybl.openreac.parameters.input;

public class VoltageLimitOverrideBuilder {

    private VoltageLimitOverride.OverrideKind lowLimitKind;
    private VoltageLimitOverride.OverrideKind highLimitKind;

    private double lowLimitOverride;
    private double highLimitOverride;

    public VoltageLimitOverride build() {
        if (lowLimitKind == null && highLimitKind == null) {
            throw new IllegalStateException("For a valid voltage limit override, the kind of one side must be provided");
        }
        if (lowLimitOverride == 0 && highLimitOverride == 0) {
            throw new IllegalStateException("For a valid voltage limit override, an override value must be provided");
        }
        return new VoltageLimitOverride(lowLimitKind, highLimitKind, lowLimitOverride, highLimitOverride);
    }

    public VoltageLimitOverrideBuilder withLowLimitKind(VoltageLimitOverride.OverrideKind lowVoltageLimitKind) {
        this.lowLimitKind = lowVoltageLimitKind;
        return this;
    }

    public VoltageLimitOverrideBuilder withHighLimitKind(VoltageLimitOverride.OverrideKind highVoltageLimitKind) {
        this.highLimitKind = highVoltageLimitKind;
        return this;
    }

    public VoltageLimitOverrideBuilder withLowLimitOverride(double lowVoltageLimitOverride) {
        this.lowLimitOverride = lowVoltageLimitOverride;
        return this;
    }

    public VoltageLimitOverrideBuilder withHighLimitOverride(double highVoltageLimitOverride) {
        this.highLimitOverride = highVoltageLimitOverride;
        return this;
    }

}

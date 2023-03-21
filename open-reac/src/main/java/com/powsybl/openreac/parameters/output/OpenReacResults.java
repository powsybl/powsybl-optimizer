package com.powsybl.openreac.parameters.output;

import com.powsybl.openreac.parameters.output.ReactiveInvestmentOutput.ReactiveInvestment;

import java.util.List;
import java.util.Map;

public class OpenReacResults {
    private final OpenReacStatus status;
    private final List<ReactiveInvestment> reactiveInvestments;
    private final Map<String, String> indicators;

    public OpenReacResults(OpenReacStatus status, List<ReactiveInvestment> reactiveInvestments,
                           Map<String, String> indicators) {
        this.status = status;
        this.reactiveInvestments = reactiveInvestments;
        this.indicators = indicators;
    }

    public OpenReacStatus getStatus() {
        return status;
    }

    public List<ReactiveInvestment> getReactiveInvestments() {
        return reactiveInvestments;
    }

    public Map<String, String> getIndicators() {
        return indicators;
    }
}

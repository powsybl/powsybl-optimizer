package com.powsybl.divergenceanalyser;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

public class DivergenceAnalyserConfig {
    /**
     * Default parameters
     */

    // For debug
    private static final boolean DEFAULT_DEBUG = true;

    private final boolean debug;

    public DivergenceAnalyserConfig(boolean debug){
        this.debug = debug;
    }

    public static DivergenceAnalyserConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static DivergenceAnalyserConfig load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig("divergenceanalyser")
                .map(config -> new DivergenceAnalyserConfig(config.getBooleanProperty("debug", DEFAULT_DEBUG)))
                .orElse(new DivergenceAnalyserConfig(false));
    }

    public boolean isDebug(){
        return debug;
    }


}

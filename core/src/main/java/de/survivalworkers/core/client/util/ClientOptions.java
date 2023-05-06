package de.survivalworkers.core.client.util;

import de.survivalworkers.core.common.option.Option;
import de.survivalworkers.core.common.option.OptionGroup;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientOptions extends OptionGroup {

    @Option(defaultValue = "true")
    private boolean enableDebugLayers;
    @Option(defaultValue = "2")
    private int presentMode;

    public ClientOptions() {
        super("client");
    }
}

package de.thecoolcraft11.hideAndSeek.nms;

import de.thecoolcraft11.hideAndSeek.nms.meta.NmsAdapterMeta;

public class NoopNmsMeta implements NmsAdapterMeta {
    @Override
    public boolean supports(String version) {
        return true;
    }

    @Override
    public String name() {
        return "NOOP";
    }

    @Override
    public Class<? extends NmsAdapter> implementation() {
        return NoopNmsAdapter.class;
    }
}

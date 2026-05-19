package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11;

import de.thecoolcraft11.hideAndSeek.nms.NmsAdapter;
import de.thecoolcraft11.hideAndSeek.nms.meta.NmsAdapterMeta;

public class NmsMeta implements NmsAdapterMeta {
    @Override
    public boolean supports(String version) {
        return version.equals("1.21.11");
    }

    @Override
    public String name() {
        return "v1_21_11";
    }

    @Override
    public Class<? extends NmsAdapter> implementation() {
        return NmsAdapterImpl.class;
    }
}

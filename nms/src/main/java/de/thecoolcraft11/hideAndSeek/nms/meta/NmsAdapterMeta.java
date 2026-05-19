package de.thecoolcraft11.hideAndSeek.nms.meta;

import de.thecoolcraft11.hideAndSeek.nms.NmsAdapter;

public interface NmsAdapterMeta {

    boolean supports(String version);

    String name();

    Class<? extends NmsAdapter> implementation();
}
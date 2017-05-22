package voss.nms.inventory.diff.network.builder;

import voss.core.server.builder.CommandBuilder;

public interface BuilderFactory {

    CommandBuilder getBuilder();
}
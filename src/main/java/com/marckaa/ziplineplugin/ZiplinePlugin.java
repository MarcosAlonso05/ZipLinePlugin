package com.marckaa.ziplineplugin;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class ZiplinePlugin extends JavaPlugin {

    private static ZiplinePlugin instance;

    private ComponentType<ChunkStore, ZiplineComponent> ziplineComponentType;

    public static ZiplinePlugin getInstance() {
        return instance;
    }

    public ZiplinePlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;

        this.ziplineComponentType = this.getChunkStoreRegistry().registerComponent(ZiplineComponent.class, "ZiplineComponent", ZiplineComponent.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ConnectZipline", ConnectZiplineInteraction.class, ConnectZiplineInteraction.CODEC);
        this.getChunkStoreRegistry().registerSystem(new ZiplineBreakSystem());
    }

    public ComponentType<ChunkStore, ZiplineComponent> getZiplineComponentType() {
        return ziplineComponentType;
    }
}
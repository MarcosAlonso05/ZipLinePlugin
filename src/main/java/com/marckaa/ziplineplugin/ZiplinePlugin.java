package com.marckaa.ziplineplugin;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class ZiplinePlugin extends JavaPlugin {

    // 1. Creamos una instancia estática para acceder desde cualquier lado
    private static ZiplinePlugin instance;

    // 2. Guardamos el TIPO del componente
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

        // 3. Registramos y GUARDAMOS el tipo
        this.ziplineComponentType = this.getChunkStoreRegistry().registerComponent(ZiplineComponent.class, "ZiplineComponent", ZiplineComponent.CODEC);

        this.getCodecRegistry(Interaction.CODEC).register("ConnectZipline", ConnectZiplineInteraction.class, ConnectZiplineInteraction.CODEC);

        System.out.println("Zipline Mod cargado.");
    }

    // 4. Getter público para el tipo
    public ComponentType<ChunkStore, ZiplineComponent> getZiplineComponentType() {
        return ziplineComponentType;
    }
}
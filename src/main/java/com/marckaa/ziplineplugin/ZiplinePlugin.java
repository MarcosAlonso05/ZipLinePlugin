package com.marckaa.ziplineplugin;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.marckaa.ziplineplugin.components.RideComponent;
import com.marckaa.ziplineplugin.components.ZiplineComponent;
import com.marckaa.ziplineplugin.interactions.ConnectZiplineInteraction;
import com.marckaa.ziplineplugin.interactions.RideZipLineInteraction;
import com.marckaa.ziplineplugin.systems.RideSystem;
import com.marckaa.ziplineplugin.systems.ZiplineBreakBlockSystem; // Importar el nuevo sistema

public class ZiplinePlugin extends JavaPlugin {

    private static ZiplinePlugin instance;
    private ComponentType<ChunkStore, ZiplineComponent> ziplineComponentType;
    private ComponentType<EntityStore, RideComponent> rideComponentType;

    public static ZiplinePlugin getInstance() { return instance; }

    public ZiplinePlugin(JavaPluginInit init) { super(init); }

    @Override
    protected void setup() {
        instance = this;

        // Registros de componentes
        this.ziplineComponentType = this.getChunkStoreRegistry().registerComponent(ZiplineComponent.class, "ZiplineComponent", ZiplineComponent.CODEC);
        this.rideComponentType = this.getEntityStoreRegistry().registerComponent(RideComponent.class, "RideComponent", RideComponent.CODEC);

        // Registros de interacciones
        this.getCodecRegistry(Interaction.CODEC).register("ConnectZipline", ConnectZiplineInteraction.class, ConnectZiplineInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ZipLineRide", RideZipLineInteraction.class, RideZipLineInteraction.CODEC);

        // Registros de sistemas
        this.getChunkStoreRegistry().registerSystem(new ZiplineBreakSystem());
        this.getEntityStoreRegistry().registerSystem(new RideSystem());

        // --- AQUÍ EL CAMBIO ---
        // Registramos el sistema de evento de entidad en lugar del listener
        this.getEntityStoreRegistry().registerSystem(new ZiplineBreakBlockSystem());
    }

    public ComponentType<ChunkStore, ZiplineComponent> getZiplineComponentType() { return ziplineComponentType; }
    public ComponentType<EntityStore, RideComponent> getRideComponentType() { return rideComponentType; }
}
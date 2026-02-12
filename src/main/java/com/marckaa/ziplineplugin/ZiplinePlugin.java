package com.marckaa.ziplineplugin;

import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.AssetUpdateQuery;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.marckaa.ziplineplugin.components.RideComponent;
import com.marckaa.ziplineplugin.components.ZiplineComponent;
import com.marckaa.ziplineplugin.interactions.ConnectZiplineInteraction;
import com.marckaa.ziplineplugin.interactions.RideZipLineInteraction;
import com.marckaa.ziplineplugin.interactions.TensorInteraction;
import com.marckaa.ziplineplugin.systems.RideSystem;
import com.marckaa.ziplineplugin.systems.ZiplineBreakBlockSystem;
import com.marckaa.ziplineplugin.systems.ZiplineBreakSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

public class ZiplinePlugin extends JavaPlugin {

    private static ZiplinePlugin instance;
    private ComponentType<ChunkStore, ZiplineComponent> ziplineComponentType;
    private ComponentType<EntityStore, RideComponent> rideComponentType;

    public static ZiplinePlugin getInstance() { return instance; }

    private static final Field ANIMATION_SET_MAP_FIELD;
    private static final Method RELOAD_CHILDREN_CONTAINER_ASSETS_METHOD;
    private static final Method GET_EVENT_BUS_METHOD;

    static {
        try {
            Field animationSetMapField = ModelAsset.class.getDeclaredField("animationSetMap");
            animationSetMapField.setAccessible(true);

            Method reloadMethod = AssetStore.class.getDeclaredMethod("reloadChildrenContainerAssets", String.class, Map.class);
            reloadMethod.setAccessible(true);

            Method getEventBusMethod = AssetStore.class.getDeclaredMethod("getEventBus");
            getEventBusMethod.setAccessible(true);

            ANIMATION_SET_MAP_FIELD = animationSetMapField;
            RELOAD_CHILDREN_CONTAINER_ASSETS_METHOD = reloadMethod;
            GET_EVENT_BUS_METHOD = getEventBusMethod;
        } catch (Exception e) {
            throw new RuntimeException("Critical error in ZiplinePlugin's reflection", e);
        }
    }

    public ZiplinePlugin(JavaPluginInit init) { super(init); }

    @Override
    protected void setup() {
        instance = this;
        AssetRegistry.register(
                HytaleAssetStore.builder(ZiplineAnimation.class, new DefaultAssetMap<>())
                        .setPath("PlayerAnimations")
                        .setCodec(ZiplineAnimation.CODEC)
                        .setKeyFunction(ZiplineAnimation::getId)
                        .loadsBefore(ModelAsset.class)
                        .build()
        );

        this.getEventRegistry().register(LoadedAssetsEvent.class, ZiplineAnimation.class, this::onZiplineAnimationsLoaded);
        this.getEventRegistry().register(LoadedAssetsEvent.class, ModelAsset.class, this::onModelsLoaded);

        this.ziplineComponentType = this.getChunkStoreRegistry().registerComponent(ZiplineComponent.class, "ZiplineComponent", ZiplineComponent.CODEC);
        this.rideComponentType = this.getEntityStoreRegistry().registerComponent(RideComponent.class, "RideComponent", RideComponent.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ConnectZipline", ConnectZiplineInteraction.class, ConnectZiplineInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ZipLineRide", RideZipLineInteraction.class, RideZipLineInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("TensorInteract", TensorInteraction.class, TensorInteraction.CODEC);
        this.getChunkStoreRegistry().registerSystem(new ZiplineBreakSystem());
        this.getEntityStoreRegistry().registerSystem(new RideSystem());
        this.getEntityStoreRegistry().registerSystem(new ZiplineBreakBlockSystem());
    }

    public ComponentType<ChunkStore, ZiplineComponent> getZiplineComponentType() { return ziplineComponentType; }
    public ComponentType<EntityStore, RideComponent> getRideComponentType() { return rideComponentType; }

    private void onZiplineAnimationsLoaded(LoadedAssetsEvent<String, ZiplineAnimation, DefaultAssetMap<String, ZiplineAnimation>> event) {
        updateAssetNoChange(ModelAsset.class, "Hytale:Hytale", "Player");
    }

    private void onModelsLoaded(LoadedAssetsEvent<String, ModelAsset, DefaultAssetMap<String, ModelAsset>> event) {
        ModelAsset playerAsset = event.getLoadedAssets().get("Player");

        if (playerAsset != null) {
            applyAnimationsToModel(playerAsset);
        }
    }

    private void applyAnimationsToModel(ModelAsset modelAsset) {
        try {
            Object2ObjectOpenHashMap<String, ModelAsset.AnimationSet> animationSetMap = new Object2ObjectOpenHashMap<>(modelAsset.getAnimationSetMap());

            AssetStore<String, ZiplineAnimation, DefaultAssetMap<String, ZiplineAnimation>> store = AssetRegistry.getAssetStore(ZiplineAnimation.class);

            if (store == null || store.getAssetMap().getAssetMap().isEmpty()) {
                return;
            }

            for (Map.Entry<String, ZiplineAnimation> entry : store.getAssetMap().getAssetMap().entrySet()) {
                ZiplineAnimation anim = entry.getValue();
                if (anim.getAnimationSet() != null) {
                    animationSetMap.put(anim.getId(), anim.getAnimationSet());
                }
            }

            ANIMATION_SET_MAP_FIELD.set(modelAsset, Collections.unmodifiableMap(animationSetMap));

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static <K, T extends JsonAssetWithMap<K, M>, M extends AssetMap<K, T>> void updateAssetNoChange(Class<T> assetClass, String packKey, K assetKey) {
        AssetStore<K, T, M> assetStore = AssetRegistry.getAssetStore(assetClass);
        if (assetStore == null) return;

        T asset = assetStore.getAssetMap().getAsset(assetKey);
        if (asset == null) return;

        Map<K, T> loadedAssets = Collections.singletonMap(assetKey, asset);

        try {
            RELOAD_CHILDREN_CONTAINER_ASSETS_METHOD.invoke(assetStore, packKey, loadedAssets);
            IEventBus eventBus = (IEventBus) GET_EVENT_BUS_METHOD.invoke(assetStore);
            IEventDispatcher dispatcher = eventBus.dispatchFor(LoadedAssetsEvent.class, assetClass);
            if (dispatcher.hasListener()) {
                dispatcher.dispatch(new LoadedAssetsEvent(assetClass, assetStore.getAssetMap(), loadedAssets, false, AssetUpdateQuery.DEFAULT));
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
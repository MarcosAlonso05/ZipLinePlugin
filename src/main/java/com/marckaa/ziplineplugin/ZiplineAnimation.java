package com.marckaa.ziplineplugin;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;

public class ZiplineAnimation implements JsonAssetWithMap<String, DefaultAssetMap<String, ZiplineAnimation>> {

    public static final AssetBuilderCodec<String, ZiplineAnimation> CODEC;

    protected String id;
    protected ModelAsset.AnimationSet animationSet;
    protected AssetExtraInfo.Data extraData;

    protected ZiplineAnimation() {}

    public String getId() {
        return this.id;
    }

    public ModelAsset.AnimationSet getAnimationSet() {
        return this.animationSet;
    }

    static {
        CODEC = AssetBuilderCodec.builder(ZiplineAnimation.class, ZiplineAnimation::new, Codec.STRING,
                        (anim, id) -> anim.id = id,
                        ZiplineAnimation::getId,
                        (anim, data) -> anim.extraData = data,
                        (anim) -> anim.extraData
                )
                .appendInherited(new KeyedCodec<>("AnimationSet", ModelAsset.AnimationSet.CODEC),
                        (anim, set) -> anim.animationSet = set,
                        (anim) -> anim.animationSet,
                        (anim, parent) -> anim.animationSet = parent.animationSet
                )
                .add()
                .build();
    }
}
package com.marckaa.ziplineplugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class GuideLineData {
    public int x, y, z;
    public boolean hasStartPoint;

    public GuideLineData() {}

    public static final BuilderCodec<GuideLineData> CODEC = BuilderCodec.builder(GuideLineData.class, GuideLineData::new)
            // CORRECCIÓN: Las Keys ("X") deben ir en Mayúscula
            .append(new KeyedCodec<>("X", Codec.INTEGER), (d, v) -> d.x = v, d -> d.x)
            .add()

            .append(new KeyedCodec<>("Y", Codec.INTEGER), (d, v) -> d.y = v, d -> d.y)
            .add()

            .append(new KeyedCodec<>("Z", Codec.INTEGER), (d, v) -> d.z = v, d -> d.z)
            .add()

            // CORRECCIÓN: "HasStartPoint" en PascalCase
            .append(new KeyedCodec<>("HasStartPoint", Codec.BOOLEAN), (d, v) -> d.hasStartPoint = v, d -> d.hasStartPoint)
            .add()

            .build();
}
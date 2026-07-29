package gg.alexandre.camera;

import com.hypixel.hytale.builtin.triggervolumes.TriggerVolumesPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class TriggerCameraPlugin extends JavaPlugin {

    public TriggerCameraPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        TriggerVolumesPlugin.get().registerEffectType(
                "Camera",
                CameraEffect.class,
                CameraEffect.CODEC
        );

        TriggerVolumesPlugin.get().registerEffectType(
                "CameraAlongPath",
                CameraAlongPathEffect.class,
                CameraAlongPathEffect.CODEC
        );
        TriggerVolumesPlugin.get().registerEffectType(
                "CameraAlongPathWorldLocked",
                CameraAlongPathWorldLockedEffect.class,
                CameraAlongPathWorldLockedEffect.CODEC
        );
        TriggerVolumesPlugin.get().registerEffectType(
                "CameraAlongPathTrackPlayer",
                CameraAlongPathTrackPlayerEffect.class,
                CameraAlongPathTrackPlayerEffect.CODEC
        );
        TriggerVolumesPlugin.get().registerEffectType(
                "CameraAlongPathPullBack",
                CameraAlongPathPullBackEffect.class,
                CameraAlongPathPullBackEffect.CODEC
        );
        TriggerVolumesPlugin.get().registerEffectType(
                "CameraAlongPathSimplified",
                CameraAlongPathSimplifiedEffect.class,
                CameraAlongPathSimplifiedEffect.CODEC
        );
    }

}

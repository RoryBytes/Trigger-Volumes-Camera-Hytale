package gg.alexandre.camera;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PositionUtil;
import org.joml.Vector3d;

import javax.annotation.Nonnull;

public class CameraEffect extends TriggerEffect {

    private static final float DEFAULT_LERP_SPEED = 0.15F;

    @Nonnull
    public static final BuilderCodec<CameraEffect> CODEC = BuilderCodec.builder(
                    CameraEffect.class, CameraEffect::new, BASE_CODEC
            )
            .append(
                    new KeyedCodec<>("Custom", Codec.BOOLEAN, false),
                    (e, v) -> e.custom = v,
                    (e) -> e.custom
            ).add()
            .append(
                    new KeyedCodec<>("Absolute", Codec.BOOLEAN, false),
                    (e, v) -> e.absolute = v,
                    (e) -> e.absolute
            ).add()
            .append(
                    new KeyedCodec<>("LookAtPlayer", Codec.BOOLEAN, false),
                    (e, v) -> e.lookPlayer = v,
                    (e) -> e.lookPlayer
            ).add()
            .append(
                    new KeyedCodec<>("Position", Vector3dUtil.CODEC, false),
                    (e, v) -> e.positionData = v,
                    (e) -> e.positionData
            ).add()
            .append(
                    new KeyedCodec<>("Rotation", Vector3dUtil.CODEC, false),
                    (e, v) -> e.rotationData = v,
                    (e) -> e.rotationData
            ).add()
            .append(
                    new KeyedCodec<>("PositionLerpSpeed", Codec.FLOAT, false),
                    (e, v) -> e.positionLerpSpeed =
                            v != null ? v : DEFAULT_LERP_SPEED,
                    e -> e.positionLerpSpeed
            )
            .add()
            .append(
                    new KeyedCodec<>("RotationLerpSpeed", Codec.FLOAT, false),
                    (e, v) -> e.rotationLerpSpeed =
                            v != null ? v : DEFAULT_LERP_SPEED,
                    e -> e.rotationLerpSpeed
            )
            .add()
            .build();

    private boolean custom = true;
    private boolean absolute = false;
    private boolean lookPlayer = false;

    private Vector3d positionData = new Vector3d();
    private Vector3d rotationData = new Vector3d();

    private Float positionLerpSpeed = DEFAULT_LERP_SPEED;
    private Float rotationLerpSpeed = DEFAULT_LERP_SPEED;

    public void execute(@Nonnull TriggerContext context) {
        Ref<EntityStore> entityRef = context.getEntityRef();
        Store<EntityStore> store = context.getStore();

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef != null) {
            if (custom) {
                Position position = PositionUtil.toPositionPacket(new Vector3d(0, 1.6, 0).add(positionData));
                Direction rotation = new Direction(
                        (float) Math.toRadians(rotationData.y),
                        (float) Math.toRadians(rotationData.x),
                        (float) Math.toRadians(rotationData.z)
                );

                if (lookPlayer) {
                    TransformComponent transformComponent = store.getComponent(
                            entityRef, TransformComponent.getComponentType()
                    );

                    if (transformComponent != null) {
                        Vector3d pos = transformComponent.getPosition();

                        Vector3d directionToPlayer = new Vector3d(pos).sub(positionData).normalize();

                        float yaw = (float) (Math.atan2(directionToPlayer.x, directionToPlayer.z) + Math.PI);
                        float pitch = (float) Math.asin(directionToPlayer.y);

                        rotation = new Direction(yaw, pitch, rotation.roll);
                    }
                }

                ServerCameraSettings settings = new ServerCameraSettings();

                if (absolute) {
                    settings.position = position;
                    settings.rotation = rotation;
                    settings.positionType = PositionType.Custom;
                    settings.rotationType = RotationType.Custom;
                } else {
                    settings.positionOffset = position;
                    settings.rotationOffset = rotation;
                }

                settings.isFirstPerson = false;
                settings.allowPitchControls = false;
                settings.sendMouseMotion = false;

                settings.positionLerpSpeed =
                        positionLerpSpeed != null ? positionLerpSpeed : DEFAULT_LERP_SPEED;

                settings.rotationLerpSpeed =
                        rotationLerpSpeed != null ? rotationLerpSpeed : DEFAULT_LERP_SPEED;

                playerRef.getPacketHandler().writeNoCache(
                        new SetServerCamera(ClientCameraView.Custom, true, settings)
                );
            }

            if (!custom) {
                playerRef.getPacketHandler().writeNoCache(
                        new SetServerCamera(ClientCameraView.FirstPerson, false, null)
                );
            }
        }
    }

}

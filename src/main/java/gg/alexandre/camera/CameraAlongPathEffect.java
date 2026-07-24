package gg.alexandre.camera;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PositionUtil;
import org.joml.Vector3d;

import javax.annotation.Nonnull;

public class CameraAlongPathEffect extends TriggerEffect {

    private static final float DEFAULT_LERP_SPEED = 0.15F;
    private static final double MIN_PATH_LENGTH_SQUARED = 1.0E-8;

    @Nonnull
    public static final BuilderCodec<CameraAlongPathEffect> CODEC =
            BuilderCodec.builder(
                            CameraAlongPathEffect.class,
                            CameraAlongPathEffect::new,
                            BASE_CODEC
                    )
                    .append(
                            new KeyedCodec<>("PathStart", Vector3dUtil.CODEC, false),
                            (effect, value) -> effect.pathStart =
                                    value != null ? value : new Vector3d(),
                            effect -> effect.pathStart
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("PathEnd", Vector3dUtil.CODEC, false),
                            (effect, value) -> effect.pathEnd =
                                    value != null ? value : new Vector3d(),
                            effect -> effect.pathEnd
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("CameraStartOffset", Vector3dUtil.CODEC, false),
                            (effect, value) -> effect.cameraStartOffset =
                                    value != null ? value : new Vector3d(),
                            effect -> effect.cameraStartOffset
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("CameraEndOffset", Vector3dUtil.CODEC, false),
                            (effect, value) -> effect.cameraEndOffset =
                                    value != null ? value : new Vector3d(),
                            effect -> effect.cameraEndOffset
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("CameraStartRotation", Vector3dUtil.CODEC, false),
                            (effect, value) -> effect.cameraStartRotation =
                                    value != null ? value : new Vector3d(),
                            effect -> effect.cameraStartRotation
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("CameraEndRotation", Vector3dUtil.CODEC, false),
                            (effect, value) -> effect.cameraEndRotation =
                                    value != null ? value : new Vector3d(),
                            effect -> effect.cameraEndRotation
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("PositionLerpSpeed", Codec.FLOAT, false),
                            (effect, value) -> effect.positionLerpSpeed =
                                    value != null ? value : DEFAULT_LERP_SPEED,
                            effect -> effect.positionLerpSpeed
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("RotationLerpSpeed", Codec.FLOAT, false),
                            (effect, value) -> effect.rotationLerpSpeed =
                                    value != null ? value : DEFAULT_LERP_SPEED,
                            effect -> effect.rotationLerpSpeed
                    )
                    .add()
                    .build();

    private Vector3d pathStart = new Vector3d();
    private Vector3d pathEnd = new Vector3d();

    private Vector3d cameraStartOffset = new Vector3d();
    private Vector3d cameraEndOffset = new Vector3d();

    private Vector3d cameraStartRotation = new Vector3d();
    private Vector3d cameraEndRotation = new Vector3d();

    private Float positionLerpSpeed = DEFAULT_LERP_SPEED;
    private Float rotationLerpSpeed = DEFAULT_LERP_SPEED;

    @Override
    public void execute(@Nonnull TriggerContext context) {
        Ref<EntityStore> entityRef = context.getEntityRef();
        Store<EntityStore> store = context.getStore();

        PlayerRef playerRef = store.getComponent(
                entityRef,
                PlayerRef.getComponentType()
        );

        TransformComponent transformComponent = store.getComponent(
                entityRef,
                TransformComponent.getComponentType()
        );

        if (playerRef == null || transformComponent == null) {
            return;
        }

        Vector3d playerPosition = transformComponent.getPosition();
        double progress = calculateProgress(playerPosition);

        Vector3d calculatedOffset =
                new Vector3d(cameraStartOffset)
                        .lerp(cameraEndOffset, progress);

        Vector3d calculatedRotation =
                new Vector3d(cameraStartRotation)
                        .lerp(cameraEndRotation, progress);

        Position positionOffset = PositionUtil.toPositionPacket(
                new Vector3d(0, 1.6, 0).add(calculatedOffset)
        );

        Direction rotationOffset = new Direction(
                (float) Math.toRadians(calculatedRotation.y),
                (float) Math.toRadians(calculatedRotation.x),
                (float) Math.toRadians(calculatedRotation.z)
        );

        ServerCameraSettings settings = new ServerCameraSettings();

        settings.positionOffset = positionOffset;
        settings.rotationOffset = rotationOffset;

        settings.isFirstPerson = false;
        settings.allowPitchControls = false;
        settings.sendMouseMotion = false;

        settings.positionLerpSpeed =
                positionLerpSpeed != null
                        ? positionLerpSpeed
                        : DEFAULT_LERP_SPEED;

        settings.rotationLerpSpeed =
                rotationLerpSpeed != null
                        ? rotationLerpSpeed
                        : DEFAULT_LERP_SPEED;

        playerRef.getPacketHandler().writeNoCache(
                new SetServerCamera(
                        ClientCameraView.Custom,
                        true,
                        settings
                )
        );
    }

    private double calculateProgress(@Nonnull Vector3d playerPosition) {
        Vector3d pathDirection =
                new Vector3d(pathEnd).sub(pathStart);

        double pathLengthSquared = pathDirection.lengthSquared();

        if (pathLengthSquared < MIN_PATH_LENGTH_SQUARED) {
            return 0.0;
        }

        Vector3d playerFromStart =
                new Vector3d(playerPosition).sub(pathStart);

        double progress =
                playerFromStart.dot(pathDirection)
                        / pathLengthSquared;

        return Math.clamp(progress, 0.0, 1.0);
    }
}
package gg.alexandre.camera;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PositionUtil;
import org.joml.Vector3d;

import javax.annotation.Nonnull;

public class CameraAlongPathPullBackEffect extends TriggerEffect {

    private static final float DEFAULT_LERP_SPEED = 0.15F;
    private static final double MIN_PATH_LENGTH_SQUARED = 1.0E-8;
    private static final float DEFAULT_LOOK_AT_HEIGHT = 1.0F;
    private static final double MIN_LOOK_DIRECTION_LENGTH_SQUARED = 1.0E-8;
    private static final float DEFAULT_EYE_HEIGHT = 1.6F;
    private static final float DEFAULT_MAX_PULLBACK_DISTANCE = 16.0F;
    private static final float DEFAULT_END_HEIGHT = 5.0F;
    private static final float DEFAULT_END_SIDE_OFFSET = 4.0F;
    private static final float DEFAULT_SIDE_MOVEMENT_START = 0.35F;
    private static final float DEFAULT_DOORWAY_CLEARANCE = 0.5F;
    private static final float DEFAULT_TRACKING_BLEND_END = 0.25F;

    @Nonnull
    public static final BuilderCodec<CameraAlongPathPullBackEffect> CODEC =
            BuilderCodec.builder(
                            CameraAlongPathPullBackEffect.class,
                            CameraAlongPathPullBackEffect::new,
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
                            new KeyedCodec<>("EyeHeight", Codec.FLOAT, false),
                            (effect, value) -> effect.eyeHeight =
                                    value != null ? value : DEFAULT_EYE_HEIGHT,
                            effect -> effect.eyeHeight
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("MaxPullbackDistance", Codec.FLOAT, false),
                            (effect, value) -> effect.maxPullbackDistance =
                                    value != null ? value : DEFAULT_MAX_PULLBACK_DISTANCE,
                            effect -> effect.maxPullbackDistance
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("EndHeight", Codec.FLOAT, false),
                            (effect, value) -> effect.endHeight =
                                    value != null ? value : DEFAULT_END_HEIGHT,
                            effect -> effect.endHeight
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("EndSideOffset", Codec.FLOAT, false),
                            (effect, value) -> effect.endSideOffset =
                                    value != null ? value : DEFAULT_END_SIDE_OFFSET,
                            effect -> effect.endSideOffset
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("SideMovementStart", Codec.FLOAT, false),
                            (effect, value) -> effect.sideMovementStart =
                                    value != null ? value : DEFAULT_SIDE_MOVEMENT_START,
                            effect -> effect.sideMovementStart
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("DoorwayClearance", Codec.FLOAT, false),
                            (effect, value) -> effect.doorwayClearance =
                                    value != null ? value : DEFAULT_DOORWAY_CLEARANCE,
                            effect -> effect.doorwayClearance
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("TrackingBlendEnd", Codec.FLOAT, false),
                            (effect, value) -> effect.trackingBlendEnd =
                                    value != null ? value : DEFAULT_TRACKING_BLEND_END,
                            effect -> effect.trackingBlendEnd
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("LookAtHeight", Codec.FLOAT, false),
                            (effect, value) -> effect.lookAtHeight =
                                    value != null ? value : DEFAULT_LOOK_AT_HEIGHT,
                            effect -> effect.lookAtHeight
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

    private Float eyeHeight = DEFAULT_EYE_HEIGHT;
    private Float maxPullbackDistance = DEFAULT_MAX_PULLBACK_DISTANCE;
    private Float endHeight = DEFAULT_END_HEIGHT;
    private Float endSideOffset = DEFAULT_END_SIDE_OFFSET;
    private Float sideMovementStart = DEFAULT_SIDE_MOVEMENT_START;
    private Float doorwayClearance = DEFAULT_DOORWAY_CLEARANCE;
    private Float trackingBlendEnd = DEFAULT_TRACKING_BLEND_END;

    private Float positionLerpSpeed = DEFAULT_LERP_SPEED;
    private Float rotationLerpSpeed = DEFAULT_LERP_SPEED;
    private Float lookAtHeight = DEFAULT_LOOK_AT_HEIGHT;

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

        /*
         * Build a horizontal forward direction from Path Start toward Path End.
         *
         * We intentionally ignore Y here. The path's full 3D coordinates still
         * determine progress, but the camera pulls backward horizontally rather
         * than being driven into the floor or ceiling on a sloped path.
         */
        Vector3d forward = new Vector3d(
                pathEnd.x - pathStart.x,
                0.0,
                pathEnd.z - pathStart.z
        );

        if (forward.lengthSquared() < MIN_PATH_LENGTH_SQUARED) {
            return;
        }

        forward.normalize();

        /*
         * A horizontal right-facing vector perpendicular to the path.
         *
         * A positive EndSideOffset moves in this direction.
         * A negative value moves to the opposite side.
         */
        Vector3d right = new Vector3d(
                -forward.z,
                0.0,
                forward.x
        );

        float eyeHeightValue =
                eyeHeight != null
                        ? eyeHeight
                        : DEFAULT_EYE_HEIGHT;

        float maxPullbackValue =
                maxPullbackDistance != null
                        ? maxPullbackDistance
                        : DEFAULT_MAX_PULLBACK_DISTANCE;

        float endHeightValue =
                endHeight != null
                        ? endHeight
                        : DEFAULT_END_HEIGHT;

        float endSideOffsetValue =
                endSideOffset != null
                        ? endSideOffset
                        : DEFAULT_END_SIDE_OFFSET;

        float sideMovementStartValue =
                sideMovementStart != null
                        ? sideMovementStart
                        : DEFAULT_SIDE_MOVEMENT_START;

        float doorwayClearanceValue =
                doorwayClearance != null
                        ? doorwayClearance
                        : DEFAULT_DOORWAY_CLEARANCE;

        float trackingBlendEndValue =
                trackingBlendEnd != null
                        ? trackingBlendEnd
                        : DEFAULT_TRACKING_BLEND_END;

        float lookAtHeightValue =
                lookAtHeight != null
                        ? lookAtHeight
                        : DEFAULT_LOOK_AT_HEIGHT;

        /*
         * Measure how far the player has physically entered the hall.
         *
         * At Path Start this is approximately 0.
         * It increases while moving toward Path End.
         */
        double distanceEntered =
                new Vector3d(playerPosition)
                        .sub(pathStart)
                        .dot(forward);

        distanceEntered = Math.max(0.0, distanceEntered);

        /*
         * Desired pullback grows smoothly from zero to the configured maximum.
         */
        double pullbackProgress = smoothstep(progress);

        double desiredPullback =
                Math.max(0.0, maxPullbackValue)
                        * pullbackProgress;

        /*
         * Prevent the camera from moving behind the entrance wall.
         *
         * The camera cannot pull back farther than the available distance
         * between the player and Path Start, minus Doorway Clearance.
         */
        double availablePullback =
                Math.max(
                        0.0,
                        distanceEntered - Math.max(0.0, doorwayClearanceValue)
                );

        double safePullback =
                Math.min(desiredPullback, availablePullback);

        /*
         * Height starts at zero additional height because the base position
         * is already the player's eye position.
         */
        double additionalHeight =
                endHeightValue * smoothstep(progress);

        /*
         * Side motion begins later than pullback and height.
         *
         * Example:
         * SideMovementStart = 0.35
         * means no sideways movement during the first 35% of the path.
         */
        double clampedSideStart =
                Math.clamp(sideMovementStartValue, 0.0, 0.9999);

        double sideProgress =
                progress <= clampedSideStart
                        ? 0.0
                        : (progress - clampedSideStart)
                        / (1.0 - clampedSideStart);

        sideProgress = smoothstep(sideProgress);

        double currentSideOffset =
                endSideOffsetValue * sideProgress;

        /*
         * Begin at the player's eye position.
         */
        Vector3d eyePosition =
                new Vector3d(playerPosition)
                        .add(0.0, eyeHeightValue, 0.0);

        /*
         * Move:
         * - backward along the hall
         * - upward
         * - sideways later in the path
         */
        Vector3d cameraWorldPosition =
                new Vector3d(eyePosition)
                        .add(
                                new Vector3d(forward)
                                        .mul(-safePullback)
                        )
                        .add(0.0, additionalHeight, 0.0)
                        .add(
                                new Vector3d(right)
                                        .mul(currentSideOffset)
                        );

        /*
         * Read the player's current look direction.
         *
         * Use lookOrientation when Hytale has sent one. Fall back to the
         * entity's rotation when it is unavailable.
         */
        Direction playerLook =
                transformComponent.getSentTransform().lookOrientation;

        if (playerLook == null) {
            Rotation3f playerRotation =
                    transformComponent.getRotation();

            playerLook = new Direction(
                    playerRotation.yaw(),
                    playerRotation.pitch(),
                    playerRotation.roll()
            );
        }

        /*
         * Calculate a rotation that aims from the camera toward the player.
         */
        Vector3d lookTarget =
                new Vector3d(playerPosition)
                        .add(0.0, lookAtHeightValue, 0.0);

        Vector3d lookDirection =
                new Vector3d(lookTarget)
                        .sub(cameraWorldPosition);

        Direction trackingRotation;

        if (lookDirection.lengthSquared()
                < MIN_LOOK_DIRECTION_LENGTH_SQUARED) {
            trackingRotation = new Direction(playerLook);
        } else {
            lookDirection.normalize();

            float trackingYaw =
                    (float) (
                            Math.atan2(
                                    lookDirection.x,
                                    lookDirection.z
                            )
                                    + Math.PI
                    );

            float trackingPitch =
                    (float) Math.asin(
                            lookDirection.y
                    );

            trackingRotation =
                    new Direction(
                            trackingYaw,
                            trackingPitch,
                            0.0F
                    );
        }

        /*
         * At the doorway, use the player's view.
         *
         * By TrackingBlendEnd, fully rotate toward tracking the player.
         */
        double safeTrackingBlendEnd =
                Math.clamp(
                        trackingBlendEndValue,
                        0.0001,
                        1.0
                );

        double trackingBlend =
                smoothstep(
                        Math.clamp(
                                progress / safeTrackingBlendEnd,
                                0.0,
                                1.0
                        )
                );

        Direction cameraRotation =
                new Direction(
                        lerpAngle(
                                playerLook.yaw,
                                trackingRotation.yaw,
                                trackingBlend
                        ),
                        lerpAngle(
                                playerLook.pitch,
                                trackingRotation.pitch,
                                trackingBlend
                        ),
                        lerpAngle(
                                playerLook.roll,
                                trackingRotation.roll,
                                trackingBlend
                        )
                );

        Position cameraPosition =
                PositionUtil.toPositionPacket(cameraWorldPosition);

        ServerCameraSettings settings =
                new ServerCameraSettings();

        settings.position = cameraPosition;
        settings.rotation = cameraRotation;

        settings.positionType = PositionType.Custom;
        settings.rotationType = RotationType.Custom;

        settings.isFirstPerson =
                safePullback < 1.0;
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

    private double calculateProgress(
            @Nonnull Vector3d playerPosition
    ) {
        Vector3d pathDirection =
                new Vector3d(pathEnd)
                        .sub(pathStart);

        double pathLengthSquared =
                pathDirection.lengthSquared();

        if (pathLengthSquared < MIN_PATH_LENGTH_SQUARED) {
            return 0.0;
        }

        Vector3d playerFromStart =
                new Vector3d(playerPosition)
                        .sub(pathStart);

        double progress =
                playerFromStart.dot(pathDirection)
                        / pathLengthSquared;

        return Math.clamp(progress, 0.0, 1.0);
    }

    private static double smoothstep(double value) {
        double clamped =
                Math.clamp(value, 0.0, 1.0);

        return clamped
                * clamped
                * (3.0 - 2.0 * clamped);
    }

    private static float lerpAngle(
            float start,
            float end,
            double amount
    ) {
        /*
         * Find the shortest angular distance, including across the
         * -180/180-degree boundary expressed here in radians.
         */
        double difference =
                Math.atan2(
                        Math.sin(end - start),
                        Math.cos(end - start)
                );

        return (float) (
                start + difference * amount
        );
    }
}
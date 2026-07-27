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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CameraAlongPathPullBackEffect extends TriggerEffect {

    private static final float DEFAULT_LERP_SPEED = 0.15F;
    private static final double MIN_PATH_LENGTH_SQUARED = 1.0E-8;
    private static final double MIN_LOOK_DIRECTION_LENGTH_SQUARED = 1.0E-8;
    private static final double MIN_DISTANCE_SETTING = 1.0E-4;
    private static final float MIN_RETURN_DURATION_SECONDS = 0.05F;

    private static final float DEFAULT_EYE_HEIGHT = 1.6F;
    private static final float DEFAULT_TRANSITION_START_DISTANCE = 1.0F;
    private static final float DEFAULT_ENTRY_ALIGNMENT_DISTANCE = 2.0F;
    private static final float DEFAULT_MAX_PULLBACK_DISTANCE = 16.0F;
    private static final float DEFAULT_END_HEIGHT = 5.0F;
    private static final float DEFAULT_TRACKING_START_PULLBACK = 2.5F;
    private static final float DEFAULT_TRACKING_BLEND_DISTANCE = 3.0F;
    private static final float DEFAULT_END_ROLL_DEGREES = 2.5F;
    private static final float DEFAULT_ROLL_MOVEMENT_START = 0.35F;
    private static final float DEFAULT_DOORWAY_CLEARANCE = 0.5F;
    private static final float DEFAULT_BACKTRACK_CANCEL_DISTANCE = 1.0F;
    private static final float DEFAULT_RETURN_DURATION_SECONDS = 0.45F;
    private static final float DEFAULT_LOOK_AT_HEIGHT = 1.0F;

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
                            new KeyedCodec<>("TransitionStartDistance", Codec.FLOAT, false),
                            (effect, value) -> effect.transitionStartDistance =
                                    value != null ? value : DEFAULT_TRANSITION_START_DISTANCE,
                            effect -> effect.transitionStartDistance
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("EntryAlignmentDistance", Codec.FLOAT, false),
                            (effect, value) -> effect.entryAlignmentDistance =
                                    value != null ? value : DEFAULT_ENTRY_ALIGNMENT_DISTANCE,
                            effect -> effect.entryAlignmentDistance
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
                            new KeyedCodec<>("TrackingStartPullback", Codec.FLOAT, false),
                            (effect, value) -> effect.trackingStartPullback =
                                    value != null ? value : DEFAULT_TRACKING_START_PULLBACK,
                            effect -> effect.trackingStartPullback
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("TrackingBlendDistance", Codec.FLOAT, false),
                            (effect, value) -> effect.trackingBlendDistance =
                                    value != null ? value : DEFAULT_TRACKING_BLEND_DISTANCE,
                            effect -> effect.trackingBlendDistance
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("EndRollDegrees", Codec.FLOAT, false),
                            (effect, value) -> effect.endRollDegrees =
                                    value != null ? value : DEFAULT_END_ROLL_DEGREES,
                            effect -> effect.endRollDegrees
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("RollMovementStart", Codec.FLOAT, false),
                            (effect, value) -> effect.rollMovementStart =
                                    value != null ? value : DEFAULT_ROLL_MOVEMENT_START,
                            effect -> effect.rollMovementStart
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
                            new KeyedCodec<>("BacktrackCancelDistance", Codec.FLOAT, false),
                            (effect, value) -> effect.backtrackCancelDistance =
                                    value != null ? value : DEFAULT_BACKTRACK_CANCEL_DISTANCE,
                            effect -> effect.backtrackCancelDistance
                    )
                    .add()
                    .append(
                            new KeyedCodec<>("ReturnDurationSeconds", Codec.FLOAT, false),
                            (effect, value) -> effect.returnDurationSeconds =
                                    value != null ? value : DEFAULT_RETURN_DURATION_SECONDS,
                            effect -> effect.returnDurationSeconds
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
    private Float transitionStartDistance = DEFAULT_TRANSITION_START_DISTANCE;
    private Float entryAlignmentDistance = DEFAULT_ENTRY_ALIGNMENT_DISTANCE;
    private Float maxPullbackDistance = DEFAULT_MAX_PULLBACK_DISTANCE;
    private Float endHeight = DEFAULT_END_HEIGHT;
    private Float trackingStartPullback = DEFAULT_TRACKING_START_PULLBACK;
    private Float trackingBlendDistance = DEFAULT_TRACKING_BLEND_DISTANCE;
    private Float endRollDegrees = DEFAULT_END_ROLL_DEGREES;
    private Float rollMovementStart = DEFAULT_ROLL_MOVEMENT_START;
    private Float doorwayClearance = DEFAULT_DOORWAY_CLEARANCE;
    private Float backtrackCancelDistance = DEFAULT_BACKTRACK_CANCEL_DISTANCE;
    private Float returnDurationSeconds = DEFAULT_RETURN_DURATION_SECONDS;
    private Float lookAtHeight = DEFAULT_LOOK_AT_HEIGHT;
    private Float positionLerpSpeed = DEFAULT_LERP_SPEED;
    private Float rotationLerpSpeed = DEFAULT_LERP_SPEED;

    private final Map<UUID, TransitionState> transitionStates =
            new ConcurrentHashMap<>();

    private enum TransitionPhase {
        ACTIVE,
        RETURNING,
        CANCELLED
    }

    private static final class TransitionState {

        private final double startProgress;
        private final double startDistanceEntered;
        private final float startYaw;
        private final float startPitch;
        private final float pathYaw;

        private double furthestDistanceEntered;
        private TransitionPhase phase = TransitionPhase.ACTIVE;

        private Vector3d lastCameraPosition;
        private Direction lastCameraRotation;

        private long returnStartNanos;
        private Vector3d returnStartPosition;
        private Direction returnStartRotation;
        private boolean returnReadyToRelease;

        private TransitionState(
                double startProgress,
                double startDistanceEntered,
                Rotation3f startingRotation,
                float pathYaw
        ) {
            this.startProgress = startProgress;
            this.startDistanceEntered = startDistanceEntered;
            this.startYaw = startingRotation.yaw();
            this.startPitch = startingRotation.pitch();
            this.pathYaw = pathYaw;
            this.furthestDistanceEntered = startDistanceEntered;
        }
    }

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

        Vector3d forward = new Vector3d(
                pathEnd.x - pathStart.x,
                0.0,
                pathEnd.z - pathStart.z
        );

        if (forward.lengthSquared() < MIN_PATH_LENGTH_SQUARED) {
            return;
        }

        forward.normalize();

        float eyeHeightValue = valueOrDefault(
                eyeHeight,
                DEFAULT_EYE_HEIGHT
        );

        float transitionStartDistanceValue = valueOrDefault(
                transitionStartDistance,
                DEFAULT_TRANSITION_START_DISTANCE
        );

        float entryAlignmentDistanceValue = valueOrDefault(
                entryAlignmentDistance,
                DEFAULT_ENTRY_ALIGNMENT_DISTANCE
        );

        float maxPullbackValue = valueOrDefault(
                maxPullbackDistance,
                DEFAULT_MAX_PULLBACK_DISTANCE
        );

        float endHeightValue = valueOrDefault(
                endHeight,
                DEFAULT_END_HEIGHT
        );

        float trackingStartPullbackValue = valueOrDefault(
                trackingStartPullback,
                DEFAULT_TRACKING_START_PULLBACK
        );

        float trackingBlendDistanceValue = valueOrDefault(
                trackingBlendDistance,
                DEFAULT_TRACKING_BLEND_DISTANCE
        );

        float endRollDegreesValue = valueOrDefault(
                endRollDegrees,
                DEFAULT_END_ROLL_DEGREES
        );

        float rollMovementStartValue = valueOrDefault(
                rollMovementStart,
                DEFAULT_ROLL_MOVEMENT_START
        );

        float doorwayClearanceValue = valueOrDefault(
                doorwayClearance,
                DEFAULT_DOORWAY_CLEARANCE
        );

        float backtrackCancelDistanceValue = valueOrDefault(
                backtrackCancelDistance,
                DEFAULT_BACKTRACK_CANCEL_DISTANCE
        );

        float returnDurationSecondsValue = valueOrDefault(
                returnDurationSeconds,
                DEFAULT_RETURN_DURATION_SECONDS
        );

        float lookAtHeightValue = valueOrDefault(
                lookAtHeight,
                DEFAULT_LOOK_AT_HEIGHT
        );

        double distanceEntered =
                new Vector3d(playerPosition)
                        .sub(pathStart)
                        .dot(forward);

        distanceEntered = Math.max(0.0, distanceEntered);

        UUID playerUuid = playerRef.getUuid();
        TransitionState transitionState = transitionStates.get(playerUuid);

        if (transitionState == null) {
            double requiredDistance = Math.max(
                    0.0,
                    transitionStartDistanceValue
            );

            if (distanceEntered < requiredDistance) {
                return;
            }

            Rotation3f startingRotation = playerRef.getHeadRotation();

            float pathYaw =
                    (float) (
                            Math.atan2(
                                    forward.x,
                                    forward.z
                            )
                                    + Math.PI
                    );

            transitionState =
                    new TransitionState(
                            progress,
                            distanceEntered,
                            startingRotation,
                            pathYaw
                    );

            transitionStates.put(
                    playerUuid,
                    transitionState
            );
        }

        if (transitionState.phase == TransitionPhase.CANCELLED) {
            return;
        }

        if (transitionState.phase == TransitionPhase.RETURNING) {
            executeReturn(
                    playerRef,
                    playerPosition,
                    transitionState,
                    eyeHeightValue,
                    returnDurationSecondsValue
            );
            return;
        }

        transitionState.furthestDistanceEntered =
                Math.max(
                        transitionState.furthestDistanceEntered,
                        distanceEntered
                );

        double backtrackedDistance =
                transitionState.furthestDistanceEntered
                        - distanceEntered;

        if (transitionState.lastCameraPosition != null
                && backtrackedDistance
                >= Math.max(0.0, backtrackCancelDistanceValue)) {
            beginReturn(transitionState);

            executeReturn(
                    playerRef,
                    playerPosition,
                    transitionState,
                    eyeHeightValue,
                    returnDurationSecondsValue
            );
            return;
        }

        double remainingPathProgress =
                1.0 - transitionState.startProgress;

        double cinematicProgress;

        if (remainingPathProgress < MIN_PATH_LENGTH_SQUARED) {
            cinematicProgress = 1.0;
        } else {
            cinematicProgress =
                    Math.clamp(
                            (progress - transitionState.startProgress)
                                    / remainingPathProgress,
                            0.0,
                            1.0
                    );
        }

        double pullbackProgress =
                smoothstep(cinematicProgress);

        double desiredPullback =
                Math.max(0.0, maxPullbackValue)
                        * pullbackProgress;

        double availablePullback =
                Math.max(
                        0.0,
                        distanceEntered
                                - Math.max(0.0, doorwayClearanceValue)
                );

        double safePullback =
                Math.min(
                        desiredPullback,
                        availablePullback
                );

        double additionalHeight =
                endHeightValue
                        * smoothstep(cinematicProgress);

        double clampedRollStart =
                Math.clamp(
                        rollMovementStartValue,
                        0.0,
                        0.9999
                );

        double rollProgress =
                cinematicProgress <= clampedRollStart
                        ? 0.0
                        : (cinematicProgress - clampedRollStart)
                        / (1.0 - clampedRollStart);

        rollProgress = smoothstep(rollProgress);

        float endingRoll =
                (float) Math.toRadians(
                        endRollDegreesValue
                );

        float currentRoll =
                lerpAngle(
                        0.0F,
                        endingRoll,
                        rollProgress
                );

        Vector3d eyePosition =
                new Vector3d(playerPosition)
                        .add(0.0, eyeHeightValue, 0.0);

        Vector3d cameraWorldPosition =
                new Vector3d(eyePosition)
                        .add(
                                new Vector3d(forward)
                                        .mul(-safePullback)
                        )
                        .add(0.0, additionalHeight, 0.0);

        double distanceSinceActivation =
                Math.max(
                        0.0,
                        distanceEntered
                                - transitionState.startDistanceEntered
                );

        double entryAlignment =
                smoothstep(
                        Math.clamp(
                                distanceSinceActivation
                                        / Math.max(
                                        MIN_DISTANCE_SETTING,
                                        entryAlignmentDistanceValue
                                ),
                                0.0,
                                1.0
                        )
                );

        Direction alignedEntryView =
                new Direction(
                        lerpAngle(
                                transitionState.startYaw,
                                transitionState.pathYaw,
                                entryAlignment
                        ),
                        lerpAngle(
                                transitionState.startPitch,
                                0.0F,
                                entryAlignment
                        ),
                        0.0F
                );

        Vector3d lookTarget =
                new Vector3d(playerPosition)
                        .add(0.0, lookAtHeightValue, 0.0);

        Vector3d lookDirection =
                new Vector3d(lookTarget)
                        .sub(cameraWorldPosition);

        Direction trackingRotation;

        if (lookDirection.lengthSquared()
                < MIN_LOOK_DIRECTION_LENGTH_SQUARED) {
            trackingRotation =
                    new Direction(alignedEntryView);
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

        double trackingBlend =
                smoothstep(
                        Math.clamp(
                                (
                                        safePullback
                                                - Math.max(
                                                0.0,
                                                trackingStartPullbackValue
                                        )
                                )
                                        / Math.max(
                                        MIN_DISTANCE_SETTING,
                                        trackingBlendDistanceValue
                                ),
                                0.0,
                                1.0
                        )
                );

        Direction cameraRotation =
                new Direction(
                        lerpAngle(
                                alignedEntryView.yaw,
                                trackingRotation.yaw,
                                trackingBlend
                        ),
                        lerpAngle(
                                alignedEntryView.pitch,
                                trackingRotation.pitch,
                                trackingBlend
                        ),
                        currentRoll
                );

        sendCustomCamera(
                playerRef,
                cameraWorldPosition,
                cameraRotation,
                safePullback < 1.0,
                false,
                false,
                valueOrDefault(
                        positionLerpSpeed,
                        DEFAULT_LERP_SPEED
                ),
                valueOrDefault(
                        rotationLerpSpeed,
                        DEFAULT_LERP_SPEED
                )
        );

        transitionState.lastCameraPosition =
                new Vector3d(cameraWorldPosition);

        transitionState.lastCameraRotation =
                new Direction(cameraRotation);
    }

    private void beginReturn(
            @Nonnull TransitionState transitionState
    ) {
        transitionState.phase = TransitionPhase.RETURNING;
        transitionState.returnStartNanos = System.nanoTime();
        transitionState.returnStartPosition =
                new Vector3d(
                        transitionState.lastCameraPosition
                );
        transitionState.returnStartRotation =
                new Direction(
                        transitionState.lastCameraRotation
                );
        transitionState.returnReadyToRelease = false;
    }

    private void executeReturn(
            @Nonnull PlayerRef playerRef,
            @Nonnull Vector3d playerPosition,
            @Nonnull TransitionState transitionState,
            float eyeHeightValue,
            float returnDurationSecondsValue
    ) {
        if (transitionState.returnReadyToRelease) {
            restoreFirstPerson(playerRef);
            transitionState.phase = TransitionPhase.CANCELLED;
            return;
        }

        if (transitionState.returnStartPosition == null
                || transitionState.returnStartRotation == null) {
            restoreFirstPerson(playerRef);
            transitionState.phase = TransitionPhase.CANCELLED;
            return;
        }

        double durationSeconds =
                Math.max(
                        MIN_RETURN_DURATION_SECONDS,
                        returnDurationSecondsValue
                );

        double elapsedSeconds =
                (
                        System.nanoTime()
                                - transitionState.returnStartNanos
                )
                        / 1_000_000_000.0;

        double returnProgress =
                Math.clamp(
                        elapsedSeconds / durationSeconds,
                        0.0,
                        1.0
                );

        double easedReturnProgress =
                smoothstep(returnProgress);

        Vector3d eyePosition =
                new Vector3d(playerPosition)
                        .add(0.0, eyeHeightValue, 0.0);

        Rotation3f liveHeadRotation =
                playerRef.getHeadRotation();

        Vector3d returningPosition =
                new Vector3d(
                        transitionState.returnStartPosition
                )
                        .lerp(
                                eyePosition,
                                easedReturnProgress
                        );

        Direction returningRotation =
                new Direction(
                        lerpAngle(
                                transitionState.returnStartRotation.yaw,
                                liveHeadRotation.yaw(),
                                easedReturnProgress
                        ),
                        lerpAngle(
                                transitionState.returnStartRotation.pitch,
                                liveHeadRotation.pitch(),
                                easedReturnProgress
                        ),
                        lerpAngle(
                                transitionState.returnStartRotation.roll,
                                liveHeadRotation.roll(),
                                easedReturnProgress
                        )
                );

        boolean nearPlayerEyes =
                returningPosition.distanceSquared(
                        eyePosition
                )
                        < 1.0;

        sendCustomCamera(
                playerRef,
                returningPosition,
                returningRotation,
                nearPlayerEyes,
                true,
                true,
                1.0F,
                1.0F
        );

        if (returnProgress >= 1.0) {
            transitionState.returnReadyToRelease = true;
        }
    }

    private void sendCustomCamera(
            @Nonnull PlayerRef playerRef,
            @Nonnull Vector3d cameraWorldPosition,
            @Nonnull Direction cameraRotation,
            boolean isFirstPerson,
            boolean allowPitchControls,
            boolean sendMouseMotion,
            float positionSpeed,
            float rotationSpeed
    ) {
        Position cameraPosition =
                PositionUtil.toPositionPacket(
                        cameraWorldPosition
                );

        ServerCameraSettings settings =
                new ServerCameraSettings();

        settings.position = cameraPosition;
        settings.rotation = cameraRotation;
        settings.positionType = PositionType.Custom;
        settings.rotationType = RotationType.Custom;
        settings.isFirstPerson = isFirstPerson;
        settings.allowPitchControls = allowPitchControls;
        settings.sendMouseMotion = sendMouseMotion;
        settings.positionLerpSpeed = positionSpeed;
        settings.rotationLerpSpeed = rotationSpeed;

        playerRef.getPacketHandler().writeNoCache(
                new SetServerCamera(
                        ClientCameraView.Custom,
                        true,
                        settings
                )
        );
    }

    private void restoreFirstPerson(
            @Nonnull PlayerRef playerRef
    ) {
        playerRef.getPacketHandler().writeNoCache(
                new SetServerCamera(
                        ClientCameraView.FirstPerson,
                        false,
                        null
                )
        );
    }

    @Override
    public void onEntityExit(
            @Nonnull UUID entityUuid
    ) {
        transitionStates.remove(entityUuid);
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

        return Math.clamp(
                progress,
                0.0,
                1.0
        );
    }

    private static double smoothstep(double value) {
        double clamped =
                Math.clamp(
                        value,
                        0.0,
                        1.0
                );

        return clamped
                * clamped
                * (3.0 - 2.0 * clamped);
    }

    private static float lerpAngle(
            float start,
            float end,
            double amount
    ) {
        double difference =
                Math.atan2(
                        Math.sin(end - start),
                        Math.cos(end - start)
                );

        return (float) (
                start + difference * amount
        );
    }

    private static float valueOrDefault(
            Float value,
            float defaultValue
    ) {
        return value != null
                ? value
                : defaultValue;
    }
}
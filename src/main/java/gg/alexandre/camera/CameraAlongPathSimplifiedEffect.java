package gg.alexandre.camera;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.builtin.triggervolumes.shape.TriggerVolumeShape;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CameraAlongPathSimplifiedEffect extends TriggerEffect {

    private static final float DEFAULT_PULLBACK_DISTANCE = 16.0F;
    private static final float DEFAULT_VERTICAL_MOVEMENT = 5.0F;
    private static final float DEFAULT_HORIZONTAL_MOVEMENT = 0.0F;
    private static final float DEFAULT_CAMERA_ROLL_DEGREES = 2.5F;
    private static final float DEFAULT_TRANSITION_SPEED = 0.15F;
    private static final boolean DEFAULT_CANCEL_ON_BACKTRACKING = true;

    @Nonnull
    public static final BuilderCodec<CameraAlongPathSimplifiedEffect> CODEC =
            BuilderCodec.builder(
                            CameraAlongPathSimplifiedEffect.class,
                            CameraAlongPathSimplifiedEffect::new,
                            BASE_CODEC
                    )
                    .append(
                            new KeyedCodec<>(
                                    "PullbackDistance",
                                    Codec.FLOAT,
                                    false
                            ),
                            (effect, value) -> effect.pullbackDistance =
                                    value != null
                                            ? value
                                            : DEFAULT_PULLBACK_DISTANCE,
                            effect -> effect.pullbackDistance
                    )
                    .add()
                    .append(
                            new KeyedCodec<>(
                                    "VerticalMovement",
                                    Codec.FLOAT,
                                    false
                            ),
                            (effect, value) -> effect.verticalMovement =
                                    value != null
                                            ? value
                                            : DEFAULT_VERTICAL_MOVEMENT,
                            effect -> effect.verticalMovement
                    )
                    .add()
                    .append(
                            new KeyedCodec<>(
                                    "HorizontalMovement",
                                    Codec.FLOAT,
                                    false
                            ),
                            (effect, value) -> effect.horizontalMovement =
                                    value != null
                                            ? value
                                            : DEFAULT_HORIZONTAL_MOVEMENT,
                            effect -> effect.horizontalMovement
                    )
                    .add()
                    .append(
                            new KeyedCodec<>(
                                    "CameraRollDegrees",
                                    Codec.FLOAT,
                                    false
                            ),
                            (effect, value) -> effect.cameraRollDegrees =
                                    value != null
                                            ? value
                                            : DEFAULT_CAMERA_ROLL_DEGREES,
                            effect -> effect.cameraRollDegrees
                    )
                    .add()
                    .append(
                            new KeyedCodec<>(
                                    "TransitionSpeed",
                                    Codec.FLOAT,
                                    false
                            ),
                            (effect, value) -> effect.transitionSpeed =
                                    value != null
                                            ? value
                                            : DEFAULT_TRANSITION_SPEED,
                            effect -> effect.transitionSpeed
                    )
                    .add()
                    .append(
                            new KeyedCodec<>(
                                    "CancelOnBacktracking",
                                    Codec.BOOLEAN,
                                    false
                            ),
                            (effect, value) -> effect.cancelOnBacktracking =
                                    value != null
                                            ? value
                                            : DEFAULT_CANCEL_ON_BACKTRACKING,
                            effect -> effect.cancelOnBacktracking
                    )
                    .add()
                    .build();

    private Float pullbackDistance =
            DEFAULT_PULLBACK_DISTANCE;

    private Float verticalMovement =
            DEFAULT_VERTICAL_MOVEMENT;

    private Float horizontalMovement =
            DEFAULT_HORIZONTAL_MOVEMENT;

    private Float cameraRollDegrees =
            DEFAULT_CAMERA_ROLL_DEGREES;

    private Float transitionSpeed =
            DEFAULT_TRANSITION_SPEED;

    private Boolean cancelOnBacktracking =
            DEFAULT_CANCEL_ON_BACKTRACKING;

    private static final double MIN_PATH_LENGTH = 0.001;

    private static final double BACKTRACK_CANCEL_THRESHOLD = 0.05;

    private final Map<UUID, PathState> pathStates =
            new ConcurrentHashMap<>();

    private static final class PathState {

        private final boolean usesXAxis;
        private final boolean enteredFromMinimumEnd;
        private final boolean enteredThroughEnd;

        private int lastReportedBucket = -1;
        private double farthestProgress = 0.0;
        private boolean cancelled = false;

        private PathState(
                boolean usesXAxis,
                boolean enteredFromMinimumEnd,
                boolean enteredThroughEnd
        ) {
            this.usesXAxis = usesXAxis;
            this.enteredFromMinimumEnd =
                    enteredFromMinimumEnd;
            this.enteredThroughEnd =
                    enteredThroughEnd;
        }
    }

    @Override
    public void execute(@Nonnull TriggerContext context) {
        /*
         * The context identifies the entity that triggered the effect
         * and the entity-component store containing its data.
         */
        Ref<EntityStore> entityRef =
                context.getEntityRef();

        Store<EntityStore> store =
                context.getStore();

        /*
         * Request two components belonging to that entity:
         *
         * PlayerRef identifies and communicates with the player.
         * TransformComponent contains the entity's position and rotation.
         */
        PlayerRef playerRef =
                store.getComponent(
                        entityRef,
                        PlayerRef.getComponentType()
                );

        TransformComponent transformComponent =
                store.getComponent(
                        entityRef,
                        TransformComponent.getComponentType()
                );

        /*
         * A trigger effect might theoretically be activated by something
         * that is not a valid player or lacks position information.
         *
         * "return" stops this execution safely.
         */
        if (playerRef == null || transformComponent == null) {
            return;
        }

        /*
         * Obtain the trigger volume that is currently running this effect.
         */
        VolumeEntry volume =
                context.getVolume();

        TriggerVolumeShape shape =
                volume.getShape();

        /*
         * These start as empty vectors:
         *
         * volumeMin will become the smallest X, Y and Z coordinates.
         * volumeMax will become the largest X, Y and Z coordinates.
         */
        Vector3d volumeMin =
                new Vector3d();

        Vector3d volumeMax =
                new Vector3d();

        /*
         * The volume shape is stored relative to the volume's origin.
         * This method converts it into world-space minimum and maximum
         * coordinates and writes the results into our two vectors.
         */
        shape.getWorldAABB(
                volume.getPosition(),
                volumeMin,
                volumeMax
        );

        /*
         * Calculate the volume's horizontal size.
         *
         * Example:
         * minimum X = -60
         * maximum X = -11
         * size X    = 49 blocks
         */
        double sizeX =
                volumeMax.x - volumeMin.x;

        double sizeZ =
                volumeMax.z - volumeMin.z;

        /*
         * Whichever horizontal dimension is longer becomes the path.
         *
         * A long east-west hall will use X.
         * A long north-south hall will use Z.
         */
        boolean usesXAxis =
                sizeX >= sizeZ;

        double pathLength =
                usesXAxis
                        ? sizeX
                        : sizeZ;

        /*
         * Avoid dividing by zero if the volume has no meaningful
         * horizontal length.
         */
        if (pathLength < MIN_PATH_LENGTH) {
            return;
        }

        /*
         * Read the player's current world position.
         */
        Vector3d playerPosition =
                transformComponent.getPosition();

        /*
         * Select only the coordinate belonging to our path axis.
         */
        double playerCoordinate =
                usesXAxis
                        ? playerPosition.x
                        : playerPosition.z;

        double minimumCoordinate =
                usesXAxis
                        ? volumeMin.x
                        : volumeMin.z;

        double maximumCoordinate =
                usesXAxis
                        ? volumeMax.x
                        : volumeMax.z;

        UUID playerUuid =
                playerRef.getUuid();

        /*
         * Look for this player's existing note card.
         *
         * computeIfAbsent means:
         *
         * - return the existing PathState when one is present;
         * - otherwise create, store and return a new one.
         */
        PathState pathState =
                pathStates.computeIfAbsent(
                        playerUuid,
                        ignored -> {
                            /*
                             * Measure the player's distance from all four
                             * horizontal faces of the trigger volume.
                             */
                            double distanceToMinimumX =
                                    Math.abs(
                                            playerPosition.x
                                                    - volumeMin.x
                                    );

                            double distanceToMaximumX =
                                    Math.abs(
                                            volumeMax.x
                                                    - playerPosition.x
                                    );

                            double distanceToMinimumZ =
                                    Math.abs(
                                            playerPosition.z
                                                    - volumeMin.z
                                    );

                            double distanceToMaximumZ =
                                    Math.abs(
                                            volumeMax.z
                                                    - playerPosition.z
                                    );

                            double distanceToMinimumEnd;
                            double distanceToMaximumEnd;
                            double distanceToNearestSide;

                            /*
                             * If the hallway runs along X:
                             *
                             * minimum X and maximum X are the ends.
                             * minimum Z and maximum Z are the sides.
                             *
                             * If it runs along Z, those roles reverse.
                             */
                            if (usesXAxis) {
                                distanceToMinimumEnd =
                                        distanceToMinimumX;

                                distanceToMaximumEnd =
                                        distanceToMaximumX;

                                distanceToNearestSide =
                                        Math.min(
                                                distanceToMinimumZ,
                                                distanceToMaximumZ
                                        );
                            } else {
                                distanceToMinimumEnd =
                                        distanceToMinimumZ;

                                distanceToMaximumEnd =
                                        distanceToMaximumZ;

                                distanceToNearestSide =
                                        Math.min(
                                                distanceToMinimumX,
                                                distanceToMaximumX
                                        );
                            }

                            double distanceToNearestEnd =
                                    Math.min(
                                            distanceToMinimumEnd,
                                            distanceToMaximumEnd
                                    );

                            /*
                             * The closest boundary determines which face
                             * the player most likely crossed.
                             *
                             * A tie counts as an end entry so entering near
                             * an end corner still activates the effect.
                             */
                            boolean enteredThroughEnd =
                                    distanceToNearestEnd
                                            <= distanceToNearestSide;

                            boolean enteredFromMinimumEnd =
                                    distanceToMinimumEnd
                                            <= distanceToMaximumEnd;

                            PathState newState =
                                    new PathState(
                                            usesXAxis,
                                            enteredFromMinimumEnd,
                                            enteredThroughEnd
                                    );

                            /*
                             * Side entries are remembered but ignored until
                             * the player exits the volume again.
                             */
                            if (!enteredThroughEnd) {
                                String axisName =
                                        usesXAxis ? "X" : "Z";

                                System.out.printf(
                                        "[CameraAlongPathSimplified] "
                                                + "volume=%s axis=%s "
                                                + "entry=SIDE action=IGNORED%n",
                                        volume.getId(),
                                        axisName
                                );
                            }

                            return newState;
                        }
                );

        if (!pathState.enteredThroughEnd) {
            return;
        }

        /*
         * First calculate progress measured from the minimum-coordinate
         * end of the volume.
         *
         * At minimumCoordinate, rawProgress is 0.
         * At maximumCoordinate, rawProgress is 1.
         */
        double rawProgress =
                (playerCoordinate - minimumCoordinate)
                        / pathLength;

        /*
         * Clamp prevents the number from going below 0 or above 1.
         */
        rawProgress =
                Math.clamp(
                        rawProgress,
                        0.0,
                        1.0
                );

        /*
         * If the player entered from the maximum-coordinate end,
         * reverse the result.
         *
         * This ensures that the entrance is always progress 0 and
         * the opposite end is always progress 1.
         */
        double progress =
                pathState.enteredFromMinimumEnd
                        ? rawProgress
                        : 1.0 - rawProgress;

        /*
         * Compare the player's current progress with the farthest
         * point reached during this visit.
         */
        double previousFarthestProgress =
                pathState.farthestProgress;

        double backtrackedDistance =
                previousFarthestProgress - progress;

        /*
         * Cancel only when:
         *
         * 1. The option is enabled.
         * 2. This visit has not already been cancelled.
         * 3. The player retreated by at least 5% of the path.
         */
        if (!pathState.cancelled
                && Boolean.TRUE.equals(cancelOnBacktracking)
                && backtrackedDistance
                >= BACKTRACK_CANCEL_THRESHOLD) {

            pathState.cancelled = true;

            System.out.printf(
                    "[CameraAlongPathSimplified] "
                            + "volume=%s axis=%s "
                            + "action=CANCELLED_BACKTRACK "
                            + "farthest=%.0f%% current=%.0f%%%n",
                    volume.getId(),
                    pathState.usesXAxis ? "X" : "Z",
                    previousFarthestProgress * 100.0,
                    progress * 100.0
            );
        }

        /*
         * Preserve the greatest progress reached.
         *
         * Math.max chooses whichever number is larger.
         */
        pathState.farthestProgress =
                Math.max(
                        previousFarthestProgress,
                        progress
                );

        /*
         * Once cancelled, remain inactive until onEntityExit()
         * removes this player's PathState.
         */
        if (pathState.cancelled) {
            return;
        }

        /*
         * Create a horizontal forward direction.
         *
         * Only one of these coordinates will be nonzero:
         *
         * +X direction: forwardX = 1,  forwardZ = 0
         * -X direction: forwardX = -1, forwardZ = 0
         * +Z direction: forwardX = 0,  forwardZ = 1
         * -Z direction: forwardX = 0,  forwardZ = -1
         */
        double forwardX =
                0.0;

        double forwardZ =
                0.0;

        if (pathState.usesXAxis) {
            forwardX =
                    pathState.enteredFromMinimumEnd
                            ? 1.0
                            : -1.0;
        } else {
            forwardZ =
                    pathState.enteredFromMinimumEnd
                            ? 1.0
                            : -1.0;
        }

        /*
         * Turn the forward direction 90 degrees to create a
         * sideways direction.
         *
         * This direction stays relative to the player's direction
         * through the volume rather than being permanently tied
         * to one world direction.
         */
        double rightX =
                -forwardZ;

        double rightZ =
                forwardX;

        /*
         * Gradually increase each configured movement as progress
         * changes from 0 to 1.
         *
         * At 0% progress:
         * pullbackNow = 0
         *
         * At 50% progress with Pullback Distance 16:
         * pullbackNow = 8
         *
         * At 100%:
         * pullbackNow = 16
         */
        double pullbackNow =
                pullbackDistance * progress;

        double verticalNow =
                verticalMovement * progress;

        double horizontalNow =
                horizontalMovement * progress;

        double rollNow =
                cameraRollDegrees * progress;

        /*
         * Pullback moves opposite the forward direction.
         * Horizontal movement uses the 90-degree sideways direction.
         */
        double cameraOffsetX =
                (-forwardX * pullbackNow)
                        + (rightX * horizontalNow);

        double cameraOffsetY =
                verticalNow;

        double cameraOffsetZ =
                (-forwardZ * pullbackNow)
                        + (rightZ * horizontalNow);

        /*
         * Convert progress into ten reporting sections:
         *
         * 0  means 0-9%
         * 1  means 10-19%
         * ...
         * 10 means 100%
         */
        int progressBucket =
                (int) Math.floor(
                        progress * 10.0
                );

        /*
         * Print only when the player enters a different 10% section.
         * Otherwise TICK would flood the server console every 0.05 seconds.
         */
        if (progressBucket
                != pathState.lastReportedBucket) {
            pathState.lastReportedBucket =
                    progressBucket;

            String axisName =
                    pathState.usesXAxis
                            ? "X"
                            : "Z";

            String forwardDirection;

            if (pathState.usesXAxis) {
                forwardDirection =
                        pathState.enteredFromMinimumEnd
                                ? "+X"
                                : "-X";
            } else {
                forwardDirection =
                        pathState.enteredFromMinimumEnd
                                ? "+Z"
                                : "-Z";
            }

            System.out.printf(
                    "[CameraAlongPathSimplified] "
                            + "volume=%s axis=%s entry=END "
                            + "forward=%s progress=%.0f%% "
                            + "offset=(%.2f, %.2f, %.2f) "
                            + "roll=%.2f%n",
                    volume.getId(),
                    axisName,
                    forwardDirection,
                    progress * 100.0,
                    cameraOffsetX,
                    cameraOffsetY,
                    cameraOffsetZ,
                    rollNow
            );
        }
    }

    @Override
    public void onEntityExit(
            @Nonnull UUID entityUuid
    ) {
        pathStates.remove(entityUuid);
    }
}

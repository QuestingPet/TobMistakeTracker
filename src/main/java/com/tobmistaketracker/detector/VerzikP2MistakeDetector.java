package com.tobmistaketracker.detector;

import com.tobmistaketracker.TobBossNames;
import com.tobmistaketracker.TobMistake;
import com.tobmistaketracker.TobRaider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PlayerChanged;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
@Slf4j
@Singleton
public class VerzikP2MistakeDetector extends BaseTobMistakeDetector {

    private final int VERZIK_P2_POSE_ANIMATION_ID = 8113;
    private final int VERZIK_BOMB_GRAPHICS_OBJECT_ID = 1584;
    private final int VERZIK_BOUNCE_ANIMATION_ID = 8116;
    private final int PLAYER_BOUNCE_ANIMATION_ID = 1157;

    // TODO: acid hm

    private final Set<WorldPoint> activeBombTiles;

    // AFAIK you can only have one person bounced per tick, but just in case that ever changes...
    private final Set<String> playerNamesBounced;

    @Inject
    public VerzikP2MistakeDetector() {
        activeBombTiles = new HashSet<>();
        playerNamesBounced = new HashSet<>();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        activeBombTiles.clear();
        playerNamesBounced.clear();
    }

    @Override
    protected void computeDetectingMistakes() {
        if (!detectingMistakes && isAlreadySpawned()) {
            detectingMistakes = true;
        }
    }

    @Override
    public List<TobMistake> detectMistakes(@NonNull TobRaider raider) {
        List<TobMistake> mistakes = new ArrayList<>();

        if (raider.isDead()) {
            return mistakes;
        }

        if (activeBombTiles.contains(raider.getPreviousWorldLocation())) {
            mistakes.add(TobMistake.VERZIK_P2_BOMB);
        }

        // TODO: Acid

        // Currently, there doesn't seem to be a way to be both bombed *and* bounced on the same tick, but let's
        // write it up this way anyway in case that ever changes, since it's not a problem to do so.
        if (playerNamesBounced.contains(raider.getName())) {
            mistakes.add(TobMistake.VERZIK_P2_BOUNCE);
        }

        return mistakes;
    }

    @Override
    public void afterDetect() {
        activeBombTiles.clear();
        playerNamesBounced.clear();
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event) {
        if (event.getGraphicsObject().getId() == VERZIK_BOMB_GRAPHICS_OBJECT_ID) {
            WorldPoint worldPoint = WorldPoint.fromLocal(client, event.getGraphicsObject().getLocation());
            activeBombTiles.add(worldPoint);
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        log.info("onAnimationChanged " + event.getActor().getName() + " - " + event.getActor().getAnimation());
        if (event.getActor() instanceof Player && event.getActor().getAnimation() == PLAYER_BOUNCE_ANIMATION_ID) {
            playerNamesBounced.add(event.getActor().getName());
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        log.info("onNpcSpawned " + event.getActor().getName() + " - " + event.getActor().getPoseAnimation());
        if (!detectingMistakes && isVerzikP2(event.getActor())) {
            detectingMistakes = true;
        }
    }

    @Subscribe
    public void onNpcChanged(NpcChanged event) {
        log.info("onNpcChanged " + event.getNpc().getName() + " - " + event.getNpc().getPoseAnimation());
        if (!detectingMistakes && isVerzikP2(event.getNpc())) {
            detectingMistakes = true;
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event) {
        if (event.getActor() instanceof NPC && isVerzikP2(event.getActor())) {
            shutdown();
        }
    }

    private boolean isAlreadySpawned() {
        return client.getNpcs().stream().anyMatch(this::isVerzikP2);
    }

    private boolean isVerzikP2(Actor actor) {
        return TobBossNames.VERZIK.equals(actor.getName()) && actor.getPoseAnimation() == VERZIK_P2_POSE_ANIMATION_ID;
    }
}

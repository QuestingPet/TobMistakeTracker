package com.tobmistaketracker.detector;

import com.google.common.collect.ImmutableSet;
import com.tobmistaketracker.TobBossNames;
import com.tobmistaketracker.TobMistake;
import com.tobmistaketracker.TobRaider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * TODO:
 */
@Slf4j
@Singleton
public class SotesegMistakeDetector extends BaseTobMistakeDetector {

    // 1606 is mage orb, 1607 is range orb
    private static final Set<Integer> ORB_PROJECTILE_IDS = ImmutableSet.of(1606, 1607);

    private static final int MELEE_ANIMATION = 8138;

    // This includes the DD big ball
    private static final int ORB_ATTACK_ANIMATION = 8139;

    // 1606 orb -> 131 player graphic
    private static final int ORB_PLAYER_GRAPHIC = 131;

    //.... Wait I can literally just detect the game message :o
    // No, its only for that player...

    @Inject
    public SotesegMistakeDetector() {

    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    protected void computeDetectingMistakes() {
        if (!detectingMistakes && isAlreadySpawned()) {
            detectingMistakes = true;
        }
    }

    @Override
    public List<TobMistake> detectMistakes(@NonNull TobRaider raider) {
        return Collections.emptyList();
    }

    @Override
    public void afterDetect() {

    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        if (!detectingMistakes && TobBossNames.SOTETSEG.equals(event.getActor().getName())) {
            detectingMistakes = true;
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event) {
        Actor actor = event.getActor();
        if (actor instanceof NPC) {
            if (TobBossNames.SOTETSEG.equals(event.getActor().getName())) {
                shutdown();
            }
        }
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event) {
        if (!ORB_PROJECTILE_IDS.contains(event.getProjectile().getId())) return;

        if (event.getProjectile().getRemainingCycles() >= 200) {
            logProjectileMoved(event);
        } else if (event.getProjectile().getRemainingCycles() <= 30) {
            logProjectileMoved(event);
        }
    }

    // TODO: REMOVE
    @Subscribe
    public void onGameTick(GameTick event) {
        if (!detectingMistakes) return;

        NPC sot = client.getNpcs().stream().filter(n -> TobBossNames.SOTETSEG.equals(n.getName())).findFirst().get();
        sot.setOverheadText("" + client.getTickCount());
    }

    // TODO: REMOVE
    private void logProjectileMoved(ProjectileMoved event) {
        log.info(String.format("%s - ProjectileMoved: %s - Position: %s Remaining Cycles: %s - X1: %s Y1: %s",
                client.getTickCount(), event.getProjectile().getId(), event.getPosition(),
                event.getProjectile().getRemainingCycles(),
                event.getProjectile().getX1(), event.getProjectile().getY1()));
    }

    private boolean isAlreadySpawned() {
        return client.getNpcs().stream().anyMatch(npc -> TobBossNames.SOTETSEG.equals(npc.getName()));
    }
}

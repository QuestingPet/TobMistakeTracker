package com.tobmistaketracker.detector;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.tobmistaketracker.TobBossNames;
import com.tobmistaketracker.TobMistake;
import com.tobmistaketracker.TobRaider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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

    private boolean isAlreadySpawned() {
        return client.getNpcs().stream().anyMatch(npc -> TobBossNames.SOTETSEG.equals(npc.getName()));
    }
}

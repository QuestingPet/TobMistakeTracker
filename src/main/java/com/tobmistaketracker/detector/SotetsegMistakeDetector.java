package com.tobmistaketracker.detector;

import com.google.common.collect.ImmutableSet;
import com.tobmistaketracker.TobBossNames;
import com.tobmistaketracker.TobMistake;
import com.tobmistaketracker.TobRaider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.HeadIcon;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.runelite.api.HeadIcon.MAGIC;
import static net.runelite.api.HeadIcon.MELEE;
import static net.runelite.api.HeadIcon.RANGED;

/**
 * TODO:
 */
@Slf4j
@Singleton
public class SotetsegMistakeDetector extends BaseTobMistakeDetector {

    // 1606 is mage orb, 1607 is range orb
    private static final Set<Integer> ORB_PROJECTILE_IDS = ImmutableSet.of(1606, 1607);

    private static final EnumSet<HeadIcon> PROTECTION_OVERHEADS = EnumSet.of(MELEE, RANGED, MAGIC);

    private static final int MELEE_ANIMATION = 8138;

    // This includes the DD big ball
    private static final int ORB_ATTACK_ANIMATION = 8139;

    // 1606 orb -> 131 player graphic
    private static final int ORB_PLAYER_GRAPHIC = 131;

    private static final int PLAYER_TELEPORT_ANIMATION = 1816;

    private static final String TASTE_VENGEANCE = "Taste vengeance!";

    //.... Wait I can literally just detect the game message :o
    // No, its only for that player...

    // 1606 from sot - 7 ticks for the hitsplat (in 6 to proc, which is when it spawns the other two orbs)
    // 1607          - 8 ticks for the hitsplat
    // 1607 not sot  - 9 ticks for the hitsplat

    //........... sot orbs from him are consistent -- from others are not? like wtf is going on at 1254/1255???
    // I think it gets delayed if they already have an orb, so they dont stack?

    // TODO: Things to fix -- needs to be previous location, range orb is on-tick with hitsplat but mage orb is 1 extra
    // Something is wrong with starting the detector on the sot room.

    private final Set<WorldPoint> projectilePositions;
    private final Set<String> playersWithDamageHitsplats;
    private final Set<String> playersWithPoppedVeng;

    @Inject
    public SotetsegMistakeDetector() {
        this.projectilePositions = new HashSet<>();
        this.playersWithDamageHitsplats = new HashSet<>();
        this.playersWithPoppedVeng = new HashSet<>();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.projectilePositions.clear();
        this.playersWithDamageHitsplats.clear();
        this.playersWithPoppedVeng.clear();
    }

    @Override
    protected void computeDetectingMistakes() {
        if (!detectingMistakes && isAlreadySpawned()) {
            detectingMistakes = true;
        }
    }

    @Override
    public List<TobMistake> detectMistakes(@NonNull TobRaider raider) {
        if (raider.isDead() || !playersWithDamageHitsplats.contains(raider.getName())) {
            return Collections.emptyList();
        }

        // TODO: Is this the right check?
        if (projectilePositions.contains(raider.getPreviousWorldLocation()) &&
                !playersWithPoppedVeng.contains(raider.getName()) &&
                !PROTECTION_OVERHEADS.contains(raider.getPlayer().getOverheadIcon())) {
            // This player took damage on the same tile as a popped projectile, without a veng popping and without
            // a protection prayer up. Based on all this we should be safe to assume they took an orb.
            return Collections.singletonList(TobMistake.SOTETSEG_ORB);
        }

        return Collections.emptyList();
    }

    @Override
    public void afterDetect() {
        projectilePositions.clear();
        playersWithDamageHitsplats.clear();
        playersWithPoppedVeng.clear();
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        if (!detectingMistakes && isRealSotetseg(event.getNpc())) {
            detectingMistakes = true;
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        // During maze, the "real" sotetseg despawns and respawns after maze
        if (detectingMistakes && isRealSotetseg(event.getNpc())) {
            detectingMistakes = false;
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event) {
        if (event.getActor() instanceof NPC && TobBossNames.SOTETSEG.equals(event.getActor().getName())) {
            shutdown();
        }
    }

    @Subscribe
    public void onOverheadTextChanged(OverheadTextChanged event) {
        // Yes this can be "gamed" but who's going to, and who cares?
        if (event.getActor() instanceof Player && TASTE_VENGEANCE.equals(event.getOverheadText())) {
            playersWithPoppedVeng.add(event.getActor().getName());
        }
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event) {
        if (!ORB_PROJECTILE_IDS.contains(event.getProjectile().getId())) return;

        if (event.getProjectile().getRemainingCycles() == 0) {
            logProjectileMoved(event);
            projectilePositions.add(WorldPoint.fromLocal(client, event.getPosition()));
        }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        if (event.getActor() instanceof Player &&
                event.getHitsplat().getHitsplatType() == Hitsplat.HitsplatType.DAMAGE_OTHER) {
            playersWithDamageHitsplats.add(event.getActor().getName());
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

    private boolean isRealSotetseg(NPC npc) {
        return TobBossNames.SOTETSEG.equals(npc.getName()) && "Attack".equals(npc.getComposition().getActions()[1]);
    }

    private boolean isAlreadySpawned() {
        return client.getNpcs().stream().anyMatch(this::isRealSotetseg);
    }
}

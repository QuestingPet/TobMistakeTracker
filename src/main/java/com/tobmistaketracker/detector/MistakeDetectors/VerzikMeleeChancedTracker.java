package com.tobmistaketracker.detector.MistakeDetectors;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.gameval.AnimationID;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class VerzikMeleeChancedTracker {
    @Getter
    @Setter
    private String playerThatChancedMelee;

    private String lastTickTarget;
    private WorldArea lastTickTargetArea;
    private WorldArea lastTickVerzikArea;

    private static final Set<Integer> VERZIK_ATTACK_ANIMATIONS = Set.of(
            AnimationID.VERZIK_PHASE3_ATTACK_MELEE,
            AnimationID.VERZIK_PHASE3_ATTACK_MAGIC,
            AnimationID.VERZIK_PHASE3_ATTACK_RANGED
    );

    public void setVerzikAttackInfo(NPC verzik){
        if (verzik == null)
        {
            emptyTickData();
        } else {
            Actor target = verzik.getInteracting();
            if (target instanceof Player)
            {
                lastTickTarget = target.getName();
                lastTickTargetArea = target.getWorldArea();
                lastTickVerzikArea = verzik.getWorldArea();
            }
            else
            {
                emptyTickData();
            }
        }
    }

    public void checkPlayerWronglyTanked(AnimationChanged event)
    {
        int animationId = event.getActor().getAnimation();

        if (VERZIK_ATTACK_ANIMATIONS.contains(animationId))
        {
            if (lastTickTarget != null && lastTickTargetArea != null && lastTickVerzikArea != null)
            {
                if (isWronglyTanked(lastTickVerzikArea, lastTickTargetArea))
                {
                    playerThatChancedMelee = lastTickTarget;
                }
            }
        }
    }

    private boolean isWronglyTanked(WorldArea verzikArea, WorldArea tankArea)
    {
        return !verzikArea.intersectsWith(tankArea) && verzikArea.distanceTo(tankArea) == 1;
    }

    private void emptyTickData()
    {
        lastTickTarget = null;
        lastTickTargetArea = null;
        lastTickVerzikArea = null;
    }
}

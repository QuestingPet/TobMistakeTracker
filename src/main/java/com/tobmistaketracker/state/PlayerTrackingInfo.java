package com.tobmistaketracker.state;

import com.tobmistaketracker.TobMistake;
import lombok.Value;

import java.util.Map;

/**
 * Encapsulating class for relevant tracking information for a player, including mistakes.
 */
@Value
public class PlayerTrackingInfo {

    String playerName;
    Map<TobMistake, Integer> mistakes;
    int raidCount;

}

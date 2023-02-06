package com.gmail.necnionch.myplugin.crafterepreview.bukkit.record;

import com.google.common.collect.Maps;
import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;

public class TickData {

    private final long tick;
    private final Map<Location, String> blocks = Maps.newHashMap();
    private final Map<UUID, Location> players = Maps.newHashMap();

    public TickData(long tick) {
        this.tick = tick;
    }

    public long getTick() {
        return tick;
    }

    public Map<Location, String> blocks() {
        return blocks;
    }

    public Map<UUID, Location> players() {
        return players;
    }

}

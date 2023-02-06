package com.gmail.necnionch.myplugin.crafterepreview.bukkit.record;

import com.gmail.necnionch.myplugin.crafterepreview.bukkit.CrafterePreviewPlugin;
import com.google.common.collect.Maps;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class RecordPlayer implements Listener {

    private static final Map<Player, RecordPlayer> viewers = Maps.newHashMap();
    private final CrafterePreviewPlugin plugin = JavaPlugin.getPlugin(CrafterePreviewPlugin.class);
    private final Player viewer;
    private final RecordingReader reader;
    private final TickEventReader events;
    private final @Nullable Clipboard schematic;
    private final World world;
    private Location viewPositionOffset;
    private @Nullable BukkitTask runner;
    private int playSpeed = 0;
    private int playedIndex;
    private long playedTick;

    public RecordPlayer(Player viewer, RecordingReader reader) {
        this.viewer = viewer;
        this.reader = reader;
        this.events = new TickEventReader(reader.getEvents());
        this.schematic = reader.getSchematic();
        this.world = viewer.getWorld();
        this.viewPositionOffset = viewer.getLocation();
    }

    public Player getViewer() {
        return viewer;
    }

    public RecordingReader getReader() {
        return reader;
    }

    public void setViewPositionOffset(Location offset) {
        this.viewPositionOffset = offset;
    }

    public Location getViewPositionOffset() {
        return viewPositionOffset;
    }

    public void initPlayer() {
        if (viewers.containsKey(viewer))
            viewers.get(viewer).closePlayer();
        viewers.put(viewer, this);

//        if (!GameMode.SPECTATOR.equals(viewer.getGameMode()))
//            viewer.setGameMode(GameMode.SPECTATOR);
        viewer.getInventory().setHeldItemSlot(4);  // center

        if (schematic != null) {
            Region region = schematic.getRegion();
            Location pos = viewPositionOffset.clone();
            pos.add(BukkitAdapter.adapt(viewer.getWorld(), region.getCenter().subtract(region.getMinimumPoint().toVector3())));
            pos.setYaw(90);
            viewer.teleport(pos.add(10, 0, 0));
            renderSchematic();
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void closePlayer() {
        viewers.remove(viewer);
        HandlerList.unregisterAll(this);

        if (schematic != null) {
            Location location;
            for (BlockVector3 pos : schematic.getRegion()) {
                location = viewPositionOffset.clone();
                location.add(BukkitAdapter.adapt(world, pos.subtract(schematic.getOrigin())));
                viewer.sendBlockChange(location, location.getBlock().getBlockData());
            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onCursor(PlayerItemHeldEvent event) {
        if (!viewer.equals(event.getPlayer()))
            return;

        SlotCommand command = SlotCommand.valueOf(event.getNewSlot());
        if (event.getNewSlot() != command.getSlot()) {
            event.setCancelled(true);
            command.setHeld(viewer);
        }

        switch (command) {
            case STOP: {
                stop();
                return;
            }
            case PLAY: {
                play(1);
                return;
            }
            case FAST_FORWARD: {
                play(4);
                return;
            }
            case REWIND: {
                play(-2);
            }
        }

    }

    public void stop() {
        playSpeed = 0;
        SlotCommand.STOP.setHeld(viewer);
        stopLoop();
    }

    public void play(int speed) {
        if (speed == 1) {
            SlotCommand.PLAY.setHeld(viewer);
        } else if (speed < 0) {
            SlotCommand.REWIND.setHeld(viewer);
        } else if (1 < speed) {
            SlotCommand.FAST_FORWARD.setHeld(viewer);
        } else {
            stop();
            return;
        }
        playSpeed = speed;
        startLoop();

    }

    private void startLoop() {
        if (playSpeed == 0)
            return;
        runner = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (0 < playSpeed) {
                forwardTick();
            } else if (playSpeed < 0) {
                backTick();
            } else {
                stopLoop();
            }
            String text = String.format("Playing %d seconds (%d ticks)", playedTick / 20, playedTick);
            viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));

        }, 0, 1);
    }

    private void stopLoop() {
        if (runner != null)
            runner.cancel();
        runner = null;
    }

    private void forwardTick() {
        int rate = playSpeed;
        long end = Math.max(playedTick, 0) + rate;
        System.out.println(playedTick + " " + playedIndex);
        while (playedTick < end) {
            TickData tick = events.ticks()[playedIndex];
            if (end < tick.getTick())
                break;
            playedTick = tick.getTick();
            renderTick(tick);
            if (playedIndex + 1 >= events.getEventCount()) {
                stop();
                return;
            }
            playedIndex++;
        }
        playedTick++;
    }

    private void backTick() {
        int rate = playSpeed;
        long end = playedTick + rate;

        while (playedTick > end) {
            TickData tick = events.ticks()[playedIndex];
            if (end > tick.getTick())
                break;
            playedTick = tick.getTick();
            renderTick(tick);
            if (0 >= playedIndex) {
                stop();
                return;
            }
            playedIndex--;
        }
        playedTick--;
    }

    protected void renderTick(TickData data) {
        data.blocks().forEach((pos, blockData) -> {
            Location location = viewPositionOffset.clone().add(pos.getX(), pos.getY(), pos.getZ());
            try {
                viewer.sendBlockChange(location, plugin.getServer().createBlockData(blockData));
            } catch (IllegalArgumentException ignored) {
            }
        });

    }

    protected void renderSchematic() {
        if (schematic == null)
            return;

        Location location;
        for (BlockVector3 pos : schematic.getRegion()) {
            BlockState blockState = schematic.getBlock(pos);
            BlockData blockData;
            try {
                blockData = plugin.getServer().createBlockData(blockState.getAsString());
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            pos = pos.subtract(schematic.getMinimumPoint());
            location = viewPositionOffset.clone().add(pos.getX(), pos.getY(), pos.getZ());
            viewer.sendBlockChange(location, blockData);
        }
    }


    public static void closeAll() {
        viewers.values().forEach(RecordPlayer::closePlayer);
        viewers.clear();
    }

    public enum SlotCommand {
        STOP(4),
        PLAY(5),
        FAST_FORWARD(6),
        REWIND(3);

        private final int slot;

        SlotCommand(int slot) {
            this.slot = slot;
        }

        public int getSlot() {
            return slot;
        }

        public void setHeld(Player player) {
            player.getInventory().setHeldItemSlot(slot);
        }

        public static SlotCommand valueOf(int slot) {
            switch (slot) {
                case 5:
                    return PLAY;
                case 6:
                    return FAST_FORWARD;
                case 3:
                    return REWIND;
            }
            return STOP;
        }

    }
}

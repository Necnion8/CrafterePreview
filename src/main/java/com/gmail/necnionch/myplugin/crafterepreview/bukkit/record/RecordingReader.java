package com.gmail.necnionch.myplugin.crafterepreview.bukkit.record;

import com.gmail.necnionch.myplugin.crafterepreview.bukkit.Utils;
import com.google.common.collect.Maps;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import net.querz.nbt.io.NBTInputStream;
import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.Tag;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

public class RecordingReader {

    private final File file;
    protected final CompoundTag data;
    protected final Map<Integer, String> blockPalette = Maps.newHashMap();
    protected final Map<Integer, UUID> playerPalette = Maps.newHashMap();
    protected @Nullable TickData[] events;
    protected int blockPaletteMax;
    protected int playerPaletteMax;
    protected @Nullable Clipboard schematic;

    public RecordingReader(File file) throws IOException {
        this.file = file;
        try {
            this.data = open();
        } catch (ClassCastException e) {
            throw new IOException("Invalid data", e);
        }
        loadData();
    }

    CompoundTag open() throws IOException {
        try (NBTInputStream nbt = new NBTInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            return ((CompoundTag) nbt.readTag(Tag.DEFAULT_MAX_DEPTH).getTag());
        }
    }

    void loadData() throws IOException {
        CompoundTag eventsData = data.getCompoundTag("events");

        if (data.containsKey("schem")) {
            loadSchematic(data.get("schem"));
        }

        blockPaletteMax = eventsData.getInt("BlockPaletteMax");
        CompoundTag blockPaletteTag = eventsData.getCompoundTag("BlockPalette");
        blockPaletteTag.forEach((blockKey, value) -> {
            if (value instanceof IntTag) {
                blockPalette.put(((IntTag) value).asInt(), blockKey);
            }
        });

        playerPaletteMax = eventsData.getInt("PlayerPaletteMax");
        CompoundTag playerPaletteTag = eventsData.getCompoundTag("PlayerPalette");
        playerPaletteTag.forEach((playerIdRaw, value) -> {
            if (value instanceof IntTag) {
                UUID playerId;
                try {
                    playerId = UUID.fromString(playerIdRaw);
                } catch (IllegalArgumentException e) {
                    return;
                }
                playerPalette.put(((IntTag) value).asInt(), playerId);
            }
        });

        loadEventsData(eventsData);
    }

    void loadSchematic(Tag<?> data) throws IOException {
        schematic = null;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            NamedTag namedTag = new NamedTag(null, data);
            new NBTSerializer(true).toStream(namedTag, os);
            try (ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray())) {
                schematic = Utils.readSchematicFromStream(is);
            }
        }
    }

    void loadEventsData(CompoundTag data) throws IOException {
        events = data.keySet().stream()
                .filter(key -> key.matches("^\\d+$"))
                .map(key -> loadTickData(Long.parseLong(key), data.getCompoundTag(key)))
                .sorted(Comparator.comparingLong(TickData::getTick))
                .toArray(TickData[]::new);
    }

    TickData loadTickData(long tick, CompoundTag tag) {
        TickData tickData = new TickData(tick);

        if (tag.containsKey("BlockChange")) {
            for (Tag<?> blockChange : tag.getListTag("BlockChange")) {
                if (blockChange instanceof CompoundTag) {
                    CompoundTag bTag = (CompoundTag) blockChange;

                    int blockInternalId = bTag.getInt("BlockId");
                    Location location = Utils.doubleTagListToLocation(bTag.getListTag("Pos"));

                    if (blockPalette.containsKey(blockInternalId)) {
                        tickData.blocks().put(location, blockPalette.get(blockInternalId));
                    }
                }
            }
        }

        if (tag.containsKey("PlayerMove")) {
            for (Tag<?> playerMove : tag.getListTag("PlayerMove")) {
                if (playerMove instanceof CompoundTag) {
                    CompoundTag pTag = (CompoundTag) playerMove;

                    int playerInternalId = pTag.getInt("Id");
                    Location location = Utils.doubleTagListToLocation(pTag.getListTag("Pos"));
                    float yaw = pTag.getFloat("Yaw");

                    if (playerPalette.containsKey(playerInternalId)) {
                        location.setYaw(yaw);
                        tickData.players().put(playerPalette.get(playerInternalId), location);
                    }
                }
            }
        }

        return tickData;
    }

    public int getBlockPaletteMax() {
        return blockPaletteMax;
    }

    public int getPlayerPaletteMax() {
        return playerPaletteMax;
    }

    public Map<Integer, String> getBlockPalette() {
        return Collections.unmodifiableMap(blockPalette);
    }

    public Map<Integer, UUID> getPlayerPalette() {
        return Collections.unmodifiableMap(playerPalette);
    }

    public TickData[] getEvents() {
        return events;
    }

    public @Nullable Clipboard getSchematic() {
        return schematic;
    }

}

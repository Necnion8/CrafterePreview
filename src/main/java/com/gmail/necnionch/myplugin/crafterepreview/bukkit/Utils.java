package com.gmail.necnionch.myplugin.crafterepreview.bukkit;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import net.querz.nbt.tag.DoubleTag;
import net.querz.nbt.tag.ListTag;
import org.bukkit.Location;

import java.io.IOException;
import java.io.InputStream;

public class Utils {

    public static Clipboard readSchematicFromStream(InputStream stream) throws IOException {
        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(stream)) {
            return reader.read();
        }
    }

    public static Location doubleTagListToLocation(ListTag<?> tag) {
        return new Location(
                null,
                ((DoubleTag) tag.get(0)).asDouble(),
                ((DoubleTag) tag.get(1)).asDouble(),
                ((DoubleTag) tag.get(2)).asDouble()
        );
    }

}

package com.gmail.necnionch.myplugin.crafterepreview.bukkit;

import com.github.nova_27.mcplugin.crafterepost.CrafterePost;
import com.gmail.necnionch.myplugin.crafterepreview.bukkit.record.RecordPlayer;
import com.gmail.necnionch.myplugin.crafterepreview.bukkit.record.RecordingReader;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public final class CrafterePreviewPlugin extends JavaPlugin {

    private WorldEdit worldEdit;

    @Override
    public void onEnable() {
        if (!setupCrafterePost()) {
            getLogger().severe("Failed to hook to CrafterePost");
            setEnabled(false);
            return;
        }

        if (!setupWorldEdit()) {
            getLogger().severe("Failed to hook to WorldEdit");
            setEnabled(false);
            return;
        }

        Optional.ofNullable(getCommand("crafterepostpreview")).ifPresent(cmd ->
                cmd.setExecutor(new CrafterePreviewCommand(this)));

        getLogger().info("Enabled " + this);
    }

    @Override
    public void onDisable() {
        RecordPlayer.closeAll();
    }

    private boolean setupCrafterePost() {
        Plugin tmp = getServer().getPluginManager().getPlugin("CrafterePost");
        return tmp != null && tmp.isEnabled();
    }

    private boolean setupWorldEdit() {
        Plugin tmp = getServer().getPluginManager().getPlugin("WorldEdit");
        WorldEdit worldEdit = WorldEdit.getInstance();
        if (worldEdit != null && tmp != null && tmp.isEnabled()) {
            this.worldEdit = worldEdit;
            return true;
        }
        return false;
    }



    public void printRecordInfo(File file) {
        try {
            RecordingReader rr = new RecordingReader(new File(CrafterePost.getInstance().getDataFolder(), "crapos.mcsr"));
            System.out.println("Total events: " + rr.getEvents().length);
            System.out.println("Block ids: " + rr.getBlockPaletteMax());
            System.out.println("Player ids: " + rr.getPlayerPaletteMax());
            Clipboard schematic = rr.getSchematic();
            if (schematic != null) {
                System.out.println("Region: " + schematic.getRegion().getMinimumPoint() + " : " + schematic.getRegion().getMaximumPoint());
                System.out.println("Size: " + schematic.getRegion().getWidth() + " x " + schematic.getRegion().getHeight());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void test(Player player) {
        File file = new File(CrafterePost.getInstance().getDataFolder(), "crapos.mcsr");
        RecordingReader rr;
        try {
            rr = new RecordingReader(file);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        new RecordPlayer(player, rr).initPlayer();

    }


}

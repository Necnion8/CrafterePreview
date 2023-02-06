package com.gmail.necnionch.myplugin.crafterepreview.bukkit;

import com.sk89q.worldedit.WorldEdit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

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

        getLogger().info("Enabled CrafterePreview");
    }

    @Override
    public void onDisable() {
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



    public void test(Player player) {

    }


}

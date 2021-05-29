package dev.mruniverse.guardianlib.nms.v1_8_R1;

import com.mojang.authlib.GameProfile;
import dev.mruniverse.guardianlib.core.GuardianLIB;
import dev.mruniverse.guardianlib.core.enums.BorderColor;
import dev.mruniverse.guardianlib.core.enums.InteractType;
import dev.mruniverse.guardianlib.core.events.HologramInteractEvent;
import dev.mruniverse.guardianlib.core.nms.NMS;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_8_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import io.netty.channel.Channel;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NMSHandler implements NMS {
    private final HashMap<Player,EntityWither> bossBar = new HashMap<>();
    private final HashMap<String,EntityArmorStand> hologramsID = new HashMap<>();
    public void sendTitle(Player player, int fadeIn, int stay, int fadeOut, String title, String subtitle) {
        PlayerConnection pConn = ((CraftPlayer) player).getHandle().playerConnection;
        PacketPlayOutTitle pTitleInfo = new PacketPlayOutTitle(EnumTitleAction.TIMES, null, fadeIn, stay, fadeOut);
        pConn.sendPacket(pTitleInfo);
        if (subtitle != null) {
            subtitle = subtitle.replaceAll("%player%", player.getName());
            subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);
            IChatBaseComponent iComp = ChatSerializer.a("{\"text\": \"" + subtitle + "\"}");
            PacketPlayOutTitle pSubtitle = new PacketPlayOutTitle(EnumTitleAction.SUBTITLE, iComp);
            pConn.sendPacket(pSubtitle);
        }
        if (title != null) {
            title = title.replaceAll("%player%", player.getName());
            title = ChatColor.translateAlternateColorCodes('&', title);
            IChatBaseComponent iComp = ChatSerializer.a("{\"text\": \"" + title + "\"}");
            PacketPlayOutTitle pTitle = new PacketPlayOutTitle(EnumTitleAction.TITLE, iComp);
            pConn.sendPacket(pTitle);
        }
    }
    @SuppressWarnings("deprecation")
    public void setBlockData(org.bukkit.block.Block block,byte data) {
        block.setData(data);
    }

    public void playerBorder(Player player, Location borderCenter, int borderSize, BorderColor borderColor) {
        try {
            WorldBorder worldBorder;
            worldBorder = new WorldBorder();
            worldBorder.world = ((CraftWorld)borderCenter.getWorld()).getHandle();
            worldBorder.a(borderSize, borderSize, 0L);
            worldBorder.c(borderCenter.getX(), borderCenter.getZ());
            switch (borderColor) {
                case GREEN:
                    worldBorder.a(worldBorder.h() - 0.1D, worldBorder.h(), Long.MAX_VALUE);
                    break;
                case RED:
                    worldBorder.a(worldBorder.h(), worldBorder.h() - 1.0D, Long.MAX_VALUE);
                    break;
            }
            PacketPlayOutWorldBorder packetPlayOutWorldBorder = new PacketPlayOutWorldBorder(worldBorder, EnumWorldBorderAction.INITIALIZE);
            (((CraftPlayer)player).getHandle()).playerConnection.sendPacket(packetPlayOutWorldBorder);
        } catch (Throwable ignored) {}
    }
    public void injectPlayer(Player player) {
        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext channelHandlerContext, Object packet) {
                if(packet instanceof PacketPlayInUseEntity) {
                    try {
                        PacketPlayInUseEntity pack = (PacketPlayInUseEntity) packet;
                        readPacket(pack,player);
                    }
                    catch (Throwable ignored) { }
                }
                try {
                    super.channelRead(channelHandlerContext, packet);
                } catch (Throwable ignored) { }
            }
        };
        try {
            PlayerConnection connection = ((CraftPlayer)player).getHandle().playerConnection;
            Channel channel = ((Channel)getValue(connection.networkManager,"channel"));
            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addBefore("packet_handler", player.getName(), channelDuplexHandler);
        }catch (Throwable ignored) { }
    }
    public void readPacket(Packet packet,Player player) {
        if(packet.getClass().getSimpleName().equalsIgnoreCase("PacketPlayInUseEntity")) {
            int id = (int) getValue(packet, "a");
            if(getValue(packet, "action").toString().equalsIgnoreCase("interact")) {
                for(Map.Entry<String,EntityArmorStand> entry : hologramsID.entrySet()) {
                    EntityArmorStand armor = entry.getValue();
                    if(armor.getId() == id) {
                        Bukkit.getPluginManager().callEvent(new HologramInteractEvent(entry.getKey(),id,player, InteractType.INTERACT));
                    }
                }
            }
        }
    }

    private Object getValue(Object instance, String name) {
        Object result = null;
        try {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            result = field.get(instance);
            field.setAccessible(false);
        } catch(Throwable ignored) { }
        return result;
    }

    public void spawnHologram(Player player,String holoPrivateID,String holoLineText,Location holoLocation) {
        EntityArmorStand armorStand = new EntityArmorStand(((CraftWorld)holoLocation.getWorld()).getHandle(), holoLocation.getX(), holoLocation.getY(), holoLocation.getZ());

        armorStand.setGravity(false);
        armorStand.setCustomName(holoLineText);
        armorStand.setCustomNameVisible(true);
        armorStand.setInvisible(true);
        armorStand.setSmall(true);
        armorStand.setBasePlate(false);

        PacketPlayOutSpawnEntityLiving spawnPacket = new PacketPlayOutSpawnEntityLiving(armorStand);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(spawnPacket);
        hologramsID.put(holoPrivateID,armorStand);
    }
    public void resetName(Player player,List<Player> players) {
        CraftPlayer craftPlayer = (CraftPlayer)player;
        if(craftPlayer == null) return;
        GameProfile gameProfile = craftPlayer.getProfile();
        if(gameProfile == null) return;
        for(Player currentPlayer : players) {
            CraftPlayer currentCraftPlayer = (CraftPlayer) currentPlayer;
            if (currentCraftPlayer != null) {
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.REMOVE_PLAYER, craftPlayer.getHandle()));
                try {
                    Field nameField = GameProfile.class.getDeclaredField("name");
                    nameField.setAccessible(true);

                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);

                    nameField.set(gameProfile, player.getDisplayName());
                } catch (Throwable throwable) {
                    GuardianLIB.getControl().getLogs().error("(NameTag System) Can't set nameTag for " + player.getName());
                    GuardianLIB.getControl().getLogs().error(throwable);
                    return;
                }
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.ADD_PLAYER, craftPlayer.getHandle()));
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(player.getEntityId()));
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(craftPlayer.getHandle()));
            }
        }
    }
    public void resetName(Player player,Player[] players) {
        CraftPlayer craftPlayer = (CraftPlayer)player;
        if(craftPlayer == null) return;
        GameProfile gameProfile = craftPlayer.getProfile();
        if(gameProfile == null) return;
        for(Player currentPlayer : players) {
            CraftPlayer currentCraftPlayer = (CraftPlayer) currentPlayer;
            if (currentCraftPlayer != null) {
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.REMOVE_PLAYER, craftPlayer.getHandle()));
                try {
                    Field nameField = GameProfile.class.getDeclaredField("name");
                    nameField.setAccessible(true);

                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);

                    nameField.set(gameProfile, player.getDisplayName());
                } catch (Throwable throwable) {
                    GuardianLIB.getControl().getLogs().error("(NameTag System) Can't set nameTag for " + player.getName());
                    GuardianLIB.getControl().getLogs().error(throwable);
                    return;
                }
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.ADD_PLAYER, craftPlayer.getHandle()));
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(player.getEntityId()));
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(craftPlayer.getHandle()));
            }
        }
    }
    public void changeName(Player player,List<Player> players, String name){
        CraftPlayer craftPlayer = (CraftPlayer)player;
        if(craftPlayer == null) return;
        GameProfile gameProfile = craftPlayer.getProfile();
        if(gameProfile == null) return;
        for(Player currentPlayer : players) {
            CraftPlayer currentCraftPlayer = (CraftPlayer) currentPlayer;
            if (currentCraftPlayer != null) {
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.REMOVE_PLAYER, craftPlayer.getHandle()));
                try {
                    Field nameField = GameProfile.class.getDeclaredField("name");
                    nameField.setAccessible(true);

                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);

                    nameField.set(gameProfile, name);
                } catch (Throwable throwable) {
                    GuardianLIB.getControl().getLogs().error("(NameTag System) Can't set nameTag for " + player.getName());
                    GuardianLIB.getControl().getLogs().error(throwable);
                    return;
                }
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.ADD_PLAYER, craftPlayer.getHandle()));
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(player.getEntityId()));
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(craftPlayer.getHandle()));
            }
        }
    }
    public void changeName(Player player,Player[] players, String name){
        CraftPlayer craftPlayer = (CraftPlayer)player;
        if(craftPlayer == null) return;
        GameProfile gameProfile = craftPlayer.getProfile();
        if(gameProfile == null) return;
        for(Player currentPlayer : players) {
            CraftPlayer currentCraftPlayer = (CraftPlayer) currentPlayer;
            if (currentCraftPlayer != null) {
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.REMOVE_PLAYER, craftPlayer.getHandle()));
                try {
                    Field nameField = GameProfile.class.getDeclaredField("name");
                    nameField.setAccessible(true);

                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);

                    nameField.set(gameProfile, name);
                } catch (Throwable throwable) {
                    GuardianLIB.getControl().getLogs().error("(NameTag System) Can't set nameTag for " + player.getName());
                    GuardianLIB.getControl().getLogs().error(throwable);
                    return;
                }
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.ADD_PLAYER, craftPlayer.getHandle()));
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(player.getEntityId()));
                currentCraftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(craftPlayer.getHandle()));
            }
        }
    }
    public void spawnHologram(List<Player> players, String holoPrivateID, String holoLineText,Location holoLocation) {
        EntityArmorStand armorStand = new EntityArmorStand(((CraftWorld)holoLocation.getWorld()).getHandle(), holoLocation.getX(), holoLocation.getY(), holoLocation.getZ());

        armorStand.setGravity(false);
        armorStand.setCustomName(holoLineText);
        armorStand.setCustomNameVisible(true);
        armorStand.setInvisible(true);
        armorStand.setSmall(true);
        armorStand.setBasePlate(false);

        PacketPlayOutSpawnEntityLiving spawnPacket = new PacketPlayOutSpawnEntityLiving(armorStand);
        for(Player player : players) {
            try {
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(spawnPacket);
            }catch (Throwable ignored) {}
        }
        hologramsID.put(holoPrivateID,armorStand);
    }

    public Location getHologramLocation(String holoPrivateID) {
        return hologramsID.get(holoPrivateID).getBukkitEntity().getLocation();
    }
    public void updateHologramText(List<Player> players,String holoPrivateID,String holoLineText) {
        if(!hologramsID.containsKey(holoPrivateID)) {
            GuardianLIB.getControl().getLogs().info("(Hologram System) HoloPrivateID: " + holoPrivateID + " doesn't exists.");
            return;
        }
        EntityArmorStand armorStand = hologramsID.get(holoPrivateID);
        armorStand.setCustomName(holoLineText);
        PacketPlayOutEntityMetadata metaPacket = new PacketPlayOutEntityMetadata(armorStand.getId(), armorStand.getDataWatcher(), true);
        for(Player player : players) {
            try {
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(metaPacket);
            }catch (Throwable ignored) {}
        }
    }
    public void updateHologramText(Player player,String holoPrivateID,String holoLineText) {
        if(!hologramsID.containsKey(holoPrivateID)) {
            GuardianLIB.getControl().getLogs().info("(GlobalHologram System) HoloPrivateID: " + holoPrivateID + " doesn't exists.");
            return;
        }
        EntityArmorStand armorStand = hologramsID.get(holoPrivateID);
        armorStand.setCustomName(holoLineText);
        PacketPlayOutEntityMetadata metaPacket = new PacketPlayOutEntityMetadata(armorStand.getId(), armorStand.getDataWatcher(), true);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(metaPacket);
    }
    public void deleteHologram(Player player,String holoPrivateID) {
        if(!hologramsID.containsKey(holoPrivateID)) return;
        EntityArmorStand armorStand = hologramsID.remove(holoPrivateID);
        PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(armorStand.getId());
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
    }
    public void deleteHologram(List<Player> players,String holoPrivateID) {
        if(!hologramsID.containsKey(holoPrivateID)) return;
        EntityArmorStand armorStand = hologramsID.remove(holoPrivateID);
        PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(armorStand.getId());
        for(Player player : players) {
            try {
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
            }catch (Throwable ignored) {}
        }
    }

    public void sendActionBar(Player player, String msg) {
        String toBC = ChatColor.translateAlternateColorCodes('&', msg);
        IChatBaseComponent icbc = ChatSerializer.a("{\"text\": \"" + toBC + "\"}");
        PacketPlayOutChat bar = new PacketPlayOutChat(icbc, (byte)2);
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(bar);
    }

    public void sendBossBar(Player player, String message) {
        if(!BossHasPlayer(player)) {
            bossBar.put(player,new EntityWither(((CraftWorld)player.getWorld()).getHandle()));
        }
        Location witherLocation = getWitherLocation(player.getLocation());
        getBossBar(player).setCustomName(message);
        float life = (100 * getBossBar(player).getMaxHealth());
        getBossBar(player).setHealth(life);
        getBossBar(player).setInvisible(true);
        getBossBar(player).setLocation(witherLocation.getX(), witherLocation.getY(), witherLocation.getZ(), 0, 0);
        PacketPlayOutSpawnEntityLiving packet = new PacketPlayOutSpawnEntityLiving(getBossBar(player));
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
    }


    public void sendBossBar(Player player, String message,float percentage) {
        if(!BossHasPlayer(player)) {
            bossBar.put(player,new EntityWither(((CraftWorld)player.getWorld()).getHandle()));
        }
        if (percentage <= 0) {
            percentage = (float) 0.001;
        }
        Location witherLocation = getWitherLocation(player.getLocation());
        getBossBar(player).setCustomName(message);
        float life = (percentage * getBossBar(player).getMaxHealth());
        getBossBar(player).setHealth(life);
        getBossBar(player).setInvisible(true);
        getBossBar(player).setLocation(witherLocation.getX(), witherLocation.getY(), witherLocation.getZ(), 0, 0);
        PacketPlayOutSpawnEntityLiving packet = new PacketPlayOutSpawnEntityLiving(getBossBar(player));
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
    }
    public void deleteBossBar(Player player) {
        if(!BossHasPlayer(player)) return;
        EntityWither wither = bossBar.remove(player);
        PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(wither.getId());
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
    }
    public boolean BossHasPlayer(Player player) {
        return bossBar.containsKey(player);
    }
    public ItemStack getItemStack(Material material, String itemName, List<String> lore) {
        ItemStack addItem = new ItemStack(material, 1);
        ItemMeta addItemMeta = addItem.getItemMeta();
        addItemMeta.setDisplayName(itemName);
        addItemMeta.setLore(lore);
        addItem.setItemMeta(addItemMeta);
        return addItem;
    }

    private EntityWither getBossBar(Player player) {
        return bossBar.get(player);
    }
    private Location getWitherLocation(Location playerLocation) {
        return playerLocation.add(playerLocation.getDirection().multiply(60));
    }
}
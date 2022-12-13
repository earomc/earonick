package net.earomc.earonick.nick;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.earomc.earonick.EaroNick;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/**
 * @author tiiita_
 * Created on Dezember 11, 2022 | 01:49:47
 * (●'◡'●)
 */
public class NickManager {

    private final EaroNick plugin;

    public NickManager(EaroNick plugin) {
        this.plugin = plugin;
    }

    private final HashSet<Player> nickedPlayers = new HashSet<>();
    private final HashMap<UUID, String> uuidToOldNameMap = new HashMap<>();
    private final HashMap<UUID, Property> uuidToPropertyMap = new HashMap<>();

    public void nickPlayer(Player player, String newName) {
        nickedPlayers.add(player);
        uuidToOldNameMap.put(player.getUniqueId(), player.getName());
        uuidToPropertyMap.put(player.getUniqueId(), ((CraftPlayer) player).getHandle().getProfile().getProperties().get("textures").stream().findFirst().orElse(null));
        FileConfiguration config = plugin.getConfig();
        String playerPrefix = config.getString("nicked-name-prefix-tab").replaceAll("&", "§");


        //works if offline player exists
        String finalNewName = playerPrefix + newName;
        SkinChanger skinChanger = new SkinChanger(player.getUniqueId());

        try {
            //If the player doesn't exist it will give an error. This will catch it gives player random skin.
            Bukkit.getOfflinePlayer(UUID.fromString(newName)).hasPlayedBefore();
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cPlayer is null, random skin coming soon");
            return;
        }


        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(UUID.fromString(newName));
        GameProfile targetGameProfile = ((CraftPlayer) targetPlayer.getPlayer()).getHandle().getProfile();
        Property targetProperty = targetGameProfile.getProperties().get("textures").stream().findFirst().orElse(null);


        skinChanger.change(targetProperty.getValue(), targetProperty.getSignature());
        skinChanger.update();
    }

    public void unnickPlayer(Player player) {
        nickedPlayers.remove(player);

        //unnick player
        String realName = uuidToOldNameMap.get(player.getUniqueId());
        player.setDisplayName(realName);
        player.setPlayerListName(realName);
        player.setDisplayName(realName);

        changeNameTag(player, realName);

        SkinChanger skinChanger = new SkinChanger(player.getUniqueId());

        skinChanger.change(uuidToPropertyMap.get(player.getUniqueId()).getValue(), uuidToPropertyMap.get(player.getUniqueId()).getSignature());
        skinChanger.update();


        uuidToOldNameMap.remove(player.getUniqueId());
        uuidToPropertyMap.remove(player.getUniqueId());

    }

    public boolean isNicked(Player player) {
        return nickedPlayers.contains(player);
    }


    public String getNickName(Player player) {
        return player.getDisplayName();
    }

    public void changeNameTag(Player player, String newName) {
        for (Player currentPlayer : Bukkit.getOnlinePlayers()) {
            if (currentPlayer == player) continue;
            //REMOVES THE PLAYER
            ((CraftPlayer) currentPlayer).getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, ((CraftPlayer) player).getHandle()));
            //CHANGES THE PLAYER'S GAME PROFILE
            GameProfile gameProfile = ((CraftPlayer) player).getProfile();
            try {
                Field nameField = GameProfile.class.getDeclaredField("name");
                nameField.setAccessible(true);

                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);

                nameField.set(gameProfile, newName);
            } catch (IllegalAccessException | NoSuchFieldException ex) {
                throw new IllegalStateException(ex);
            }
            //ADDS THE PLAYER
            ((CraftPlayer) currentPlayer).getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, ((CraftPlayer) player).getHandle()));
            ((CraftPlayer) currentPlayer).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(player.getEntityId()));
            ((CraftPlayer) currentPlayer).getHandle().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(((CraftPlayer) player).getHandle()));
        }
    }

    private Property getDefaultSkin() {
        return new Property("texture", plugin.getConfig().getString("default-skin"));
    }
}
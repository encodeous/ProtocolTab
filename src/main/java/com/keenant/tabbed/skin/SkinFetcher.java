package com.keenant.tabbed.skin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;

/**
 * Some Skin utils.
 */
public class SkinFetcher {

    private static final String profileUrl = "https://sessionserver.mojang.com/session/minecraft/profile/";
    
    private final Executor bukkitExecutor;
    
    public SkinFetcher(JavaPlugin plugin) {
    	bukkitExecutor = (cmd) -> Bukkit.getScheduler().runTaskAsynchronously(plugin, cmd);
    }

    /**
     * Get a skin from an online player.
     * 
     * @param player the player
     * @return the skin or <code>null</code> if the operation failed
     */
    public Skin getPlayer(Player player) {
    	return (player != null && player.isOnline()) ? getForOnlinePlayer(player) : null;
    }
    
    private static Skin getForOnlinePlayer(Player player) {
    	Collection<WrappedSignedProperty> properties = WrappedGameProfile.fromPlayer(player).getProperties().get(Skin.TEXTURE_KEY);
        return (properties != null && properties.size() > 0) ? new Skin(properties.iterator().next()) : null;
    }

    /**
     * Get a Minecraft user's skin by their UUID. <br>
     * The completable future will yield a result relating to the player's skins.
     * It may or may not be immutable. <br>
     * <br>
     * If the player is online, a singleton list containing the skin is the result. <br>
     * Otherwise, the Mojang API is queried and all skins are the result. <br>
     * <b>Mojang API has rate limits! You are responsible for caching!</b> <br>
     * <br>
     * If the operation fails <code>null</code> is the result.
     * 
     * @param uuid the player uuid
     * @return a nonnull completable future whose result will be either the player's skins or <code>null</code> if failed
     */
    public CompletableFuture<List<Skin>> getByUUID(UUID uuid) {
    	Player player = Bukkit.getPlayer(uuid);
		return (player != null && player.isOnline())
				? CompletableFuture.completedFuture(Collections.singletonList(getForOnlinePlayer(player)))
				: CompletableFuture.supplyAsync(() -> downloadSkin(uuid), bukkitExecutor);
    }

    private static List<Skin> downloadSkin(UUID uuid) {
    	
        try {
        	URL url = new URL(profileUrl + uuid.toString().replace("-", "") + "?unsigned=false");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        	InputStreamReader isr = new InputStreamReader(connection.getInputStream(), "UTF-8");

            Object parsed = (new JSONParser()).parse(isr);

            connection.disconnect();
            isr.close();

            return (parsed instanceof JSONObject) ? getFromJson((JSONObject) parsed) : null;
        } catch (ParseException | IOException ex) {
           throw new RuntimeException(ex);
        }

    }
    
    private static List<Skin> getFromJson(JSONObject json) {
    	List<Skin> result = new ArrayList<Skin>();
    	
        JSONArray properties = (JSONArray) json.get("properties");

        for (Object object : properties) {
            JSONObject jsonObject = (JSONObject) object;
            String name = (String) jsonObject.get("name");
            String value = (String) jsonObject.get("value");
            String signature = (String) jsonObject.get("signature");
            if (name.equals(Skin.TEXTURE_KEY))
                result.add(new Skin(new WrappedSignedProperty(name, value, signature)));
        }
        return result;
    }
}

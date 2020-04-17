/*
   Copyright 2017-2018 Kacper Duras

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package pl.kacperduras.protocoltab.manager;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.keenant.tabbed.skin.SkinFetcher;

import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TabManager {

	@Getter
	private final SkinFetcher skinFetcher;
    private final Map<UUID, PacketTablist> tabMap = new ConcurrentHashMap<>();
    private static volatile Logger logger;
    
    public TabManager(JavaPlugin plugin) {
    	skinFetcher = new SkinFetcher(plugin);
    	logger = plugin.getLogger();
    }
    
    public PacketTablist get(Player player) {
        return tabMap.computeIfAbsent(player.getUniqueId(), (u) -> new PacketTablist(player));
    }
    
    public void forEach(BiConsumer<UUID, PacketTablist> action) {
    	tabMap.forEach(action);
    }
    
    public PacketTablist remove(Player player) {
    	return tabMap.remove(player.getUniqueId());
    }
    
    static void log(String message) {
    	logger.log(Level.INFO, " ProtocolTab – " + message);
    }
    
    static void logError(Exception ex) {
    	logger.log(Level.WARNING, " ProtocolTab – Exception encountered: ", ex);
    }

}

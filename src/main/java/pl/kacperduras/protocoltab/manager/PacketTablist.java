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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

public class PacketTablist {

    public final static String BLANK_TEXT = " ";

    private volatile TabItem[] existingSlots = new TabItem[] {};
    private final ConcurrentMap<Integer, TabItem> slots = new ConcurrentHashMap<>();
    private final UUID uuid;

    private volatile String existingHeader;
    private volatile String existingFooter;
    private volatile String header;
    private volatile String footer;

    PacketTablist(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "UUID must not be null");
    }
    
    private static PacketContainer makeHeaderFooterContainer(String header, String footer) {
        PacketContainer packetHeaderFooter = new PacketContainer(Server.PLAYER_LIST_HEADER_FOOTER);
        packetHeaderFooter.getChatComponents().write(0, WrappedChatComponent.fromText(header == null ? "" : header));
        packetHeaderFooter.getChatComponents().write(1, WrappedChatComponent.fromText(footer == null ? "" : footer));
        return packetHeaderFooter;
    }
    
    private static PacketContainer makeInfoDataContainer(PlayerInfoAction action, List<PlayerInfoData> infoData) {
        PacketContainer packetPlayerInfo = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        packetPlayerInfo.getPlayerInfoAction().write(0, action);
        packetPlayerInfo.getPlayerInfoDataLists().write(0, infoData);
        return packetPlayerInfo;
    }
    
    private static List<PacketContainer> makeUpdateAlt(List<PacketContainer> result, List<PlayerInfoData> removePlayer, List<PlayerInfoData> addPlayer, List<PlayerInfoData> pingUpdated, List<PlayerInfoData> displayUpdated, TabItem[] existing, TabItem[] update) {

        int largerSize = Math.max(existing.length, update.length);
        for (int n = 0; n < largerSize; n++) {

        	TabItem previous = (n < existing.length) ? existing[n] : null;
        	TabItem current = (n < update.length)? update[n] : null; 

        	assert current != null || previous != null;
        	
        	if (current != null) {
        		if (previous == null) {
        			addPlayer.add(current.makeInfoData(n));

        		} else if (!current.equals(previous)) {

        			boolean changedSkin = !current.getSkin().equals(previous.getSkin());
        			boolean changedText = !current.getText().equals(previous.getText());
        			boolean changedPing = current.getPing() != previous.getPing();

        			assert changedSkin || changedText || changedPing;

        			if (changedSkin || changedText && changedPing) {
        				addPlayer.add(current.makeInfoData(n));
        				
        			} else if (changedPing) {
        				pingUpdated.add(current.makeInfoData(n));
        				
        			} else if (changedText) {
        				displayUpdated.add(current.makeInfoData(n));

        			}
        		}
        	} else if (previous != null) {
				removePlayer.add(previous.makeInfoData(n));
        	}
        }
        if (removePlayer.size() > 0) {
        	result.add(makeInfoDataContainer(PlayerInfoAction.REMOVE_PLAYER, removePlayer));
        }
        if (addPlayer.size() > 0) {
        	result.add(makeInfoDataContainer(PlayerInfoAction.ADD_PLAYER, addPlayer));
        }
        if (pingUpdated.size() > 0) {
        	result.add(makeInfoDataContainer(PlayerInfoAction.UPDATE_LATENCY, pingUpdated));
        }
        if (displayUpdated.size() > 0) {
        	result.add(makeInfoDataContainer(PlayerInfoAction.UPDATE_DISPLAY_NAME, displayUpdated));
        }
        return result;
    }
    
    private static List<PacketContainer> makeUpdate(List<PacketContainer> result, List<PlayerInfoData> removePlayer, List<PlayerInfoData> addPlayer, List<PlayerInfoData> pingUpdated, List<PlayerInfoData> displayUpdated, TabItem[] existing, TabItem[] update) {

        int largerSize = Math.max(existing.length, update.length);
        for (int n = 0; n < largerSize; n++) {

        	TabItem previous = (n < existing.length) ? existing[n] : null;
        	TabItem current = (n < update.length)? update[n] : null; 

        	assert current != null || previous != null;
        	
        	if (current != null) {
        		if (previous == null) {
        			addPlayer.add(current.makeInfoData(n));

        		} else if (!current.equals(previous)) {

        			boolean changedSkin = !current.getSkin().equals(previous.getSkin());
        			boolean changedText = !current.getText().equals(previous.getText());
        			boolean changedPing = current.getPing() != previous.getPing();

        			assert changedSkin || changedText || changedPing;

        			if (changedSkin || changedText && changedPing) {
        				removePlayer.add(previous.makeInfoData(n));
        				addPlayer.add(current.makeInfoData(n));
        				
        			} else if (changedPing) {
        				pingUpdated.add(current.makeInfoData(n));
        				
        			} else if (changedText) {
        				displayUpdated.add(current.makeInfoData(n));

        			}
        		}
        	} else if (previous != null) {
				removePlayer.add(previous.makeInfoData(n));
        	}
        }
        if (removePlayer.size() > 0) {
        	result.add(makeInfoDataContainer(PlayerInfoAction.REMOVE_PLAYER, removePlayer));
        }
        if (addPlayer.size() > 0) {
        	result.add(makeInfoDataContainer(PlayerInfoAction.ADD_PLAYER, addPlayer));
        }
        if (pingUpdated.size() > 0) {
        	result.add(makeInfoDataContainer(PlayerInfoAction.UPDATE_LATENCY, pingUpdated));
        }
        if (displayUpdated.size() > 0) {
        	result.add(makeInfoDataContainer(PlayerInfoAction.UPDATE_DISPLAY_NAME, displayUpdated));
        }
        return result;
    }

    private void executeUpdate(Player player, String header, String footer, TabItem[] slots) {
    	// pre-initialising lists before entering the lock
    	List<PacketContainer> packets = new ArrayList<>();
    	List<PlayerInfoData> removePlayer = new ArrayList<>();
    	List<PlayerInfoData> addPlayer = new ArrayList<>();
    	List<PlayerInfoData> pingUpdated = new ArrayList<>();
    	List<PlayerInfoData> displayUpdated = new ArrayList<>();
    	boolean experimental = player.getUniqueId().equals(UUID.fromString("ed5f12cd-6007-45d9-a4b9-940524ddaecf"));
    	try {
    		synchronized (this) {
    	    	if (!Arrays.equals(this.existingSlots, slots)) {
    	    		if (experimental) {
    	    			makeUpdateAlt(packets, removePlayer, addPlayer, pingUpdated, displayUpdated, this.existingSlots, slots);
    	    		} else {
    	    			makeUpdate(packets, removePlayer, addPlayer, pingUpdated, displayUpdated, this.existingSlots, slots);
    	    		}
    	    		
    	    	}
    	    	if (!Objects.equals(this.existingHeader, header) || !Objects.equals(this.existingFooter, footer)) {
    	    		packets.add(makeHeaderFooterContainer(header, footer));
    	    	}
    			this.existingHeader = header;
    			this.existingFooter = footer;
    			this.existingSlots = slots;
    			for (PacketContainer packet : packets) {
    				ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
    			}
    		}
    	} catch (InvocationTargetException ex) {
    		TabManager.logError(ex);
    	}
    }
    
    public void update() {
    	Player player = Bukkit.getPlayer(uuid);
    	if (player == null || !player.isOnline()) {
    		return;
    	}

    	// Converting Map<Integer, TabItem> to a TabItem[]
    	List<TabItem> updateSlotList = new ArrayList<TabItem>();
    	slots.forEach((index, slot) -> {

    		// fill with null elements to prevent IndexOutOfBoundsException
    		while (index >= updateSlotList.size()) {
    			updateSlotList.add(null);
    		}
    		updateSlotList.set(index, slot);

    	});
    	TabItem[] updated = updateSlotList.toArray(new TabItem[] {});

    	executeUpdate(player, header, footer, updated);
    }

    public ConcurrentMap<Integer, TabItem> getMutableSlots() {
    	return slots;
    }

    public String getHeader() {
        return header;
    }

    public String getFooter() {
        return footer;
    }

    public void setHeader(String header) {
        this.header = ChatColor.translateAlternateColorCodes('&', header);
    }

    public void setFooter(String footer) {
        this.footer = ChatColor.translateAlternateColorCodes('&', footer);
    }

}

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

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;

import com.keenant.tabbed.skin.Skin;

public class TabItem {

	private final Skin skin;
	private final int ping;
	private final String text;

	private volatile int currentIndex;
	private volatile WrappedGameProfile currentProfile;
	private static ConcurrentHashMap<Integer, UUID> cachedIds = new ConcurrentHashMap<>();

	public TabItem(int ping, String text) {
		this.skin = Skin.DEFAULT;
		this.ping = ping;
		this.text = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(text, "Text must not be null"));
	}
	public TabItem(int ping, String text, Skin skin) {
		this.skin = Objects.requireNonNull(skin, "Skin must not be null");
		this.ping = ping;
		this.text = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(text, "Text must not be null"));
	}

	public String getText() {
		return text;
	}
	
	public int getPing() {
		return ping;
	}
	
	public Skin getSkin() {
		return skin;
	}
	
	private WrappedGameProfile makeProfile(int index) {
		cachedIds.putIfAbsent(index, UUID.randomUUID());
		String name = String.format("%03d", index) + "|UpdateMC"; // Starts with 00 so they are sorted in
		// alphabetical order and appear in the right order.

		WrappedGameProfile profile = new WrappedGameProfile(cachedIds.get(index), name);
		profile.getProperties().put(Skin.TEXTURE_KEY, skin.getProperty());
		
		currentIndex = index;
		currentProfile = profile;
		
		return profile;
	}
	
	private synchronized WrappedGameProfile getProfile(int index) {
		if(currentProfile == null) return makeProfile(index);
		return (currentIndex == index) ? currentProfile : makeProfile(index);
	}
	
	PlayerInfoData makeInfoData(int index) {
		return new PlayerInfoData(getProfile(index), ping, EnumWrappers.NativeGameMode.SURVIVAL,
				WrappedChatComponent.fromText(text));
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ping;
		result = prime * result + ((skin == null) ? 0 : skin.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		} else if (object instanceof TabItem) {
			TabItem other = (TabItem) object;
			return ping == other.ping && skin.equals(other.skin) && text.equals(other.text);
		}
		return false;
	}
}

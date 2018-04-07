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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class ProtocolTabManager {

    private final Map<UUID, ProtocolTab> tabMap = new ConcurrentHashMap<>();
    private final Cache<UUID, ProtocolTab> tabCache = CacheBuilder.newBuilder()
                                                                .removalListener(new ProtocolTabRemovalListener(this))
                                                                .expireAfterAccess(5, TimeUnit.MINUTES)
                                                                .softValues()
                                                                .build();

    private final int ping;

    public ProtocolTabManager(int ping) {
        this.ping = ping;
    }

    public ProtocolTab getTablist(Player player) {
        Validate.isTrue(player != null, "Player can not be null!");

        return this.getTablist(player.getUniqueId());
    }

    public ProtocolTab getTablist(UUID uuid) {
        Validate.isTrue(uuid != null, "UUID can not be null!");

        ProtocolTab tablist = this.tabCache.getIfPresent(uuid);
        if (tablist == null) {
            tablist = this.tabMap.get(uuid);
            if (tablist != null) {
                tabCache.put(uuid, tablist);
                tabMap.remove(uuid);
            }

            tablist = new ProtocolTab(uuid, this.ping);
            tabCache.put(uuid, tablist);
        }

        return tablist;
    }

    private class ProtocolTabRemovalListener implements RemovalListener<UUID, ProtocolTab> {

        private final ProtocolTabManager manager;

        public ProtocolTabRemovalListener(ProtocolTabManager manager) {
            Validate.isTrue(manager != null, "Manager can not be null!");

            this.manager = manager;
        }

        @Override
        public void onRemoval(RemovalNotification<UUID, ProtocolTab> notification) {
            UUID key = notification.getKey();
            ProtocolTab value = notification.getValue();

            if (key == null || value == null) {
                return;
            }

            this.manager.tabMap.put(key, value);
        }

    }

}

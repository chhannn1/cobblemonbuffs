/*
 * This file is part of architectury.
 * Copyright (C) 2020, 2021, 2022 architectury
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package dev.architectury.networking;

import I;
import com.google.common.collect.Maps;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2540;
import net.minecraft.class_2596;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3222;
import net.minecraft.class_5455;
import net.minecraft.class_634;
import net.minecraft.class_9129;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Forge {@code SimpleChannel} like network wrapper of {@link NetworkManager}.
 */
@Deprecated(forRemoval = true)
public final class NetworkChannel {
    private final class_2960 id;
    private final Map<Class<?>, MessageInfo<?>> encoders = Maps.newHashMap();
    
    private NetworkChannel(class_2960 id) {
        this.id = id;
    }
    
    public static NetworkChannel create(class_2960 id) {
        return new NetworkChannel(id);
    }
    
    public <T> void register(Class<T> type, BiConsumer<T, class_2540> encoder, Function<class_2540, T> decoder, BiConsumer<T, Supplier<PacketContext>> messageConsumer) {
        // TODO: this is pretty wasteful; add a way to specify custom or numeric ids
        var s = UUID.nameUUIDFromBytes(type.getName().getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        var info = new MessageInfo<T>(class_2960.method_60654(id + "/" + s), encoder, decoder, messageConsumer);
        encoders.put(type, info);
        NetworkManager.NetworkReceiver<class_9129> receiver = (buf, context) -> {
            info.messageConsumer.accept(info.decoder.apply(buf), () -> context);
        };
        NetworkManager.registerReceiver(NetworkManager.c2s(), info.packetId, receiver);
        if (Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(NetworkManager.s2c(), info.packetId, receiver);
        }
    }
    
    public static long hashCodeString(String str) {
        long h = 0;
        var length = str.length();
        for (var i = 0; i < length; i++) {
            h = 31 * h + str.charAt(i);
        }
        return h;
    }
    
    public <T> class_2596<?> toPacket(NetworkManager.Side side, T message, class_5455 access) {
        var messageInfo = (MessageInfo<T>) Objects.requireNonNull(encoders.get(message.getClass()), "Unknown message type! " + message);
        var buf = new class_9129(Unpooled.buffer(), access);
        messageInfo.encoder.accept(message, buf);
        return NetworkManager.toPacket(side, messageInfo.packetId, buf);
    }
    
    public <T> void sendToPlayer(class_3222 player, T message) {
        Objects.requireNonNull(player, "Unable to send packet to a 'null' player!").field_13987.method_14364(toPacket(NetworkManager.s2c(), message, player.method_56673()));
    }
    
    public <T> void sendToPlayers(Iterable<class_3222> players, T message) {
        Iterator<class_3222> iterator = players.iterator();
        if (!iterator.hasNext()) return;
        var packet = toPacket(NetworkManager.s2c(), message, iterator.next().method_56673());
        for (var player : players) {
            Objects.requireNonNull(player, "Unable to send packet to a 'null' player!").field_13987.method_14364(packet);
        }
    }
    
    @Environment(EnvType.CLIENT)
    public <T> void sendToServer(T message) {
        class_634 connection = class_310.method_1551().method_1562();
        if (connection != null) {
            connection.method_52787(toPacket(NetworkManager.c2s(), message, connection.method_29091()));
        } else {
            throw new IllegalStateException("Unable to send packet to the server while not in game!");
        }
    }
    
    @Environment(EnvType.CLIENT)
    public <T> boolean canServerReceive(Class<T> type) {
        return NetworkManager.canServerReceive(encoders.get(type).packetId);
    }
    
    public <T> boolean canPlayerReceive(class_3222 player, Class<T> type) {
        return NetworkManager.canPlayerReceive(player, encoders.get(type).packetId);
    }
    
    private record MessageInfo<T>(
            class_2960 packetId,
            BiConsumer<T, class_2540> encoder,
            Function<class_2540, T> decoder,
            BiConsumer<T, Supplier<PacketContext>> messageConsumer
    ) {
    }
}

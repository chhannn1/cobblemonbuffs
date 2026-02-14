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

import dev.architectury.impl.NetworkAggregator;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.networking.transformers.PacketCollector;
import dev.architectury.networking.transformers.PacketSink;
import dev.architectury.networking.transformers.PacketTransformer;
import dev.architectury.networking.transformers.SinglePacketCollector;
import dev.architectury.utils.Env;
import dev.architectury.utils.GameInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_2596;
import net.minecraft.class_2602;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_3231;
import net.minecraft.class_5455;
import net.minecraft.class_634;
import net.minecraft.class_8710;
import net.minecraft.class_9129;
import net.minecraft.class_9139;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class NetworkManager {
    /**
     * For S2C types, {@link #registerReceiver} should be called on the client side,
     * while {@link #registerS2CPayloadType} should be called on the server side.
     */
    @Deprecated(forRemoval = true)
    public static void registerS2CPayloadType(class_2960 id) {
        NetworkAggregator.registerS2CType(id, List.of());
    }
    
    /**
     * For S2C types, {@link #registerReceiver} should be called on the client side,
     * while {@link #registerS2CPayloadType} should be called on the server side.
     */
    public static <T extends class_8710> void registerS2CPayloadType(class_8710.class_9154<T> type, class_9139<? super class_9129, T> codec) {
        NetworkAggregator.registerS2CType(type, codec, List.of());
    }
    
    /**
     * For S2C types, {@link #registerReceiver} should be called on the client side,
     * while {@link #registerS2CPayloadType} should be called on the server side.
     */
    @Deprecated(forRemoval = true)
    public static void registerS2CPayloadType(class_2960 id, List<PacketTransformer> packetTransformers) {
        NetworkAggregator.registerS2CType(id, packetTransformers);
    }
    
    /**
     * For S2C types, {@link #registerReceiver} should be called on the client side,
     * while {@link #registerS2CPayloadType} should be called on the server side.
     */
    public static <T extends class_8710> void registerS2CPayloadType(class_8710.class_9154<T> type, class_9139<? super class_9129, T> codec, List<PacketTransformer> packetTransformers) {
        NetworkAggregator.registerS2CType(type, codec, packetTransformers);
    }
    
    @Deprecated(forRemoval = true)
    public static void registerReceiver(Side side, class_2960 id, NetworkReceiver<class_9129> receiver) {
        registerReceiver(side, id, Collections.emptyList(), receiver);
    }
    
    @ApiStatus.Experimental
    @Deprecated(forRemoval = true)
    public static void registerReceiver(Side side, class_2960 id, List<PacketTransformer> packetTransformers, NetworkReceiver<class_9129> receiver) {
        NetworkAggregator.registerReceiver(side, id, packetTransformers, receiver);
    }
    
    public static <T extends class_8710> void registerReceiver(Side side, class_8710.class_9154<T> id, class_9139<? super class_9129, T> codec, NetworkReceiver<T> receiver) {
        registerReceiver(side, id, codec, Collections.emptyList(), receiver);
    }
    
    @ApiStatus.Experimental
    public static <T extends class_8710> void registerReceiver(Side side, class_8710.class_9154<T> id, class_9139<? super class_9129, T> codec, List<PacketTransformer> packetTransformers, NetworkReceiver<T> receiver) {
        NetworkAggregator.registerReceiver(side, id, codec, packetTransformers, receiver);
    }
    
    @Deprecated(forRemoval = true)
    public static class_2596<?> toPacket(Side side, class_2960 id, class_9129 buf) {
        SinglePacketCollector sink = new SinglePacketCollector(null);
        collectPackets(sink, side, id, buf);
        return sink.getPacket();
    }
    
    @Deprecated(forRemoval = true)
    public static List<class_2596<?>> toPackets(Side side, class_2960 id, class_9129 buf) {
        PacketCollector sink = new PacketCollector(null);
        collectPackets(sink, side, id, buf);
        return sink.collect();
    }
    
    public static <T extends class_8710> class_2596<?> toPacket(Side side, T payload, class_5455 access) {
        SinglePacketCollector sink = new SinglePacketCollector(null);
        collectPackets(sink, side, payload, access);
        return sink.getPacket();
    }
    
    public static <T extends class_8710> List<class_2596<?>> toPackets(Side side, T payload, class_5455 access) {
        PacketCollector sink = new PacketCollector(null);
        collectPackets(sink, side, payload, access);
        return sink.collect();
    }
    
    @Deprecated(forRemoval = true)
    public static void collectPackets(PacketSink sink, Side side, class_2960 id, class_9129 buf) {
        NetworkAggregator.collectPackets(sink, side, id, buf);
    }
    
    public static <T extends class_8710> void collectPackets(PacketSink sink, Side side, T payload, class_5455 access) {
        NetworkAggregator.collectPackets(sink, side, payload, access);
    }
    
    @Deprecated(forRemoval = true)
    public static void sendToPlayer(class_3222 player, class_2960 id, class_9129 buf) {
        collectPackets(PacketSink.ofPlayer(player), serverToClient(), id, buf);
    }
    
    @Deprecated(forRemoval = true)
    public static void sendToPlayers(Iterable<class_3222> players, class_2960 id, class_9129 buf) {
        collectPackets(PacketSink.ofPlayers(players), serverToClient(), id, buf);
    }
    
    @Environment(EnvType.CLIENT)
    @Deprecated(forRemoval = true)
    public static void sendToServer(class_2960 id, class_9129 buf) {
        collectPackets(PacketSink.client(), clientToServer(), id, buf);
    }
    
    public static <T extends class_8710> void sendToPlayer(class_3222 player, T payload) {
        collectPackets(PacketSink.ofPlayer(player), serverToClient(), payload, player.method_56673());
    }
    
    public static <T extends class_8710> void sendToPlayers(Iterable<class_3222> players, T payload) {
        Iterator<class_3222> iterator = players.iterator();
        if (!iterator.hasNext()) return;
        collectPackets(PacketSink.ofPlayers(players), serverToClient(), payload, iterator.next().method_56673());
    }
    
    @Environment(EnvType.CLIENT)
    public static <T extends class_8710> void sendToServer(T payload) {
        class_634 connection = GameInstance.getClient().method_1562();
        if (connection == null) return;
        collectPackets(PacketSink.client(), clientToServer(), payload, connection.method_29091());
    }
    
    @Environment(EnvType.CLIENT)
    @ExpectPlatform
    public static boolean canServerReceive(class_2960 id) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    public static boolean canPlayerReceive(class_3222 player, class_2960 id) {
        throw new AssertionError();
    }
    
    @Environment(EnvType.CLIENT)
    public static boolean canServerReceive(class_8710.class_9154<?> type) {
        return canServerReceive(type.comp_2242());
    }
    
    public static boolean canPlayerReceive(class_3222 player, class_8710.class_9154<?> type) {
        return canPlayerReceive(player, type.comp_2242());
    }
    
    /**
     * Easy to use utility method to create an entity spawn packet.
     * This packet is needed everytime any mod adds a non-living entity.
     * The entity should override {@link class_1297#method_18002()} to point to this method!
     * <p>
     * Additionally, entities may implement {@link dev.architectury.extensions.network.EntitySpawnExtension}
     * to load / save additional data to the client.
     *
     * @param entity The entity which should be spawned.
     * @return The ready to use packet to spawn the entity on the client.
     * @see class_1297#method_18002()
     */
    @ExpectPlatform
    public static class_2596<class_2602> createAddEntityPacket(class_1297 entity, class_3231 serverEntity) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    private static NetworkAggregator.Adaptor getAdaptor() {
        throw new AssertionError();
    }
    
    @FunctionalInterface
    public interface NetworkReceiver<T> {
        void receive(T value, PacketContext context);
    }
    
    public interface PacketContext {
        class_1657 getPlayer();
        
        void queue(Runnable runnable);
        
        Env getEnvironment();
        
        class_5455 registryAccess();
        
        default EnvType getEnv() {
            return getEnvironment().toPlatform();
        }
    }
    
    public static Side s2c() {
        return Side.S2C;
    }
    
    public static Side c2s() {
        return Side.C2S;
    }
    
    public static Side serverToClient() {
        return Side.S2C;
    }
    
    public static Side clientToServer() {
        return Side.C2S;
    }
    
    public enum Side {
        S2C,
        C2S
    }
}

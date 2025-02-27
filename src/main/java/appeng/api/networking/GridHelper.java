/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 AlgorithmX2
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.networking;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.networking.events.GridEvent;
import appeng.capabilities.Capabilities;
import appeng.hooks.ticking.TickHandler;
import appeng.me.GridConnection;
import appeng.me.GridEventBus;
import appeng.me.InWorldGridNode;
import appeng.me.ManagedGridNode;

/**
 * A helper responsible for creating new {@link IGridNode}, connecting existing nodes, and related features.
 */
public final class GridHelper {
    private GridHelper() {
    }

    /**
     * On the server side, schedule a call to the passed callback once the block entity is in a ticking chunk. Any
     * action which might cause other chunks to be initialized, such as {@linkplain IManagedGridNode#create creating a
     * grid node}, should not be done immediately when the block entity is added to the world, but should rather be
     * deferred using this function.
     * <p>
     * This function should usually be called from {@link BlockEntity#clearRemoved()}, for example:
     *
     * <pre>
     * {@code
     * atOverride
     * public void clearRemoved() {
     *     super.clearRemoved();
     *     GridHelper.onFirstTick(this, MyBlockEntity::onFirstTick);
     * }
     *
     * private void onFirstTick() {
     *     // First tick logic here, for example:
     *     this.managedGridNode.create(getLevel(), getBlockPos());
     * }
     * }
     * </pre>
     * <p>
     * Client side this can be safely called, it will do nothing.
     */
    public static <T extends BlockEntity> void onFirstTick(T blockEntity, Consumer<? super T> callback) {
        TickHandler.instance().addInit(blockEntity, callback);
    }

    /**
     * Listens to events that are emitted per {@link IGrid}.
     */
    public static <T extends GridEvent> void addEventHandler(Class<T> eventClass, BiConsumer<IGrid, T> handler) {
        GridEventBus.subscribe(eventClass, handler);
    }

    /**
     * Forwards grid-wide events to the {@link IGridService} attached to that particular {@link IGrid}.
     */
    public static <T extends GridEvent, C extends IGridService> void addGridServiceEventHandler(Class<T> eventClass,
            Class<C> cacheClass,
            BiConsumer<C, T> eventHandler) {
        addEventHandler(eventClass, (grid, event) -> {
            eventHandler.accept(grid.getService(cacheClass), event);
        });
    }

    /**
     * Forwards grid-wide events to any node owner of a given type currently connected to that particular {@link IGrid}.
     *
     * @param nodeOwnerClass The class of node owner to forward the event to. Please note that subclasses are not
     *                       included.
     */
    public static <T extends GridEvent, C> void addNodeOwnerEventHandler(Class<T> eventClass,
            Class<C> nodeOwnerClass,
            BiConsumer<C, T> eventHandler) {
        addEventHandler(eventClass, (grid, event) -> {
            for (C machine : grid.getMachines(nodeOwnerClass)) {
                eventHandler.accept(machine, event);
            }
        });
    }

    /**
     * Convenience variant of {@link #addNodeOwnerEventHandler(Class, Class, BiConsumer)} where the event handler
     * doesn't care about the actual event object.
     */
    public static <T extends GridEvent, C> void addNodeOwnerEventHandler(Class<T> eventClass,
            Class<C> nodeOwnerClass,
            Consumer<C> eventHandler) {
        addEventHandler(eventClass, (grid, event) -> {
            for (C machine : grid.getMachines(nodeOwnerClass)) {
                eventHandler.accept(machine);
            }
        });
    }

    /**
     * Finds an {@link IInWorldGridNodeHost} at the given world location, or returns null if there isn't one.
     */
    @Nullable
    public static IInWorldGridNodeHost getNodeHost(Level level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be instanceof IInWorldGridNodeHost host) {
            return host;
        }
        return be != null ? be.getCapability(Capabilities.IN_WORLD_GRID_NODE_HOST).orElse(null) : null;
    }

    /**
     * Given a known {@link IInWorldGridNodeHost}, find an adjacent grid node (i.e. for the purposes of making
     * connections) on another host in the world.
     * <p/>
     * Nodes that have been destroyed or have not completed initialization will not be returned.
     *
     * @see #getNodeHost(Level, BlockPos)
     */
    @Nullable
    public static IGridNode getExposedNode(Level level, BlockPos pos,
            Direction side) {
        var host = getNodeHost(level, pos);
        if (host == null) {
            return null;
        }

        var node = host.getGridNode(side);
        if (node instanceof InWorldGridNode inWorldNode && inWorldNode.isExposedOnSide(side)) {
            return node;
        }
        return null;

    }

    /**
     * Creates a managed grid node that makes managing the lifecycle of an {@link IGridNode} easier.
     * <p/>
     * This method can be called on both server and client.
     *
     * @param owner    The game object that owns the node, such as a block entity or {@link appeng.api.parts.IPart}.
     * @param listener A listener that will adapt events sent by the grid node to the owner.
     * @param <T>      The type of the owner.
     * @return The managed grid node.
     */

    public static <T> IManagedGridNode createManagedNode(T owner, IGridNodeListener<T> listener) {
        return new ManagedGridNode(owner, listener);
    }

    /**
     * Create a direct connection between two {@link IGridNode}.
     * <p>
     * This will be considered as having a distance of 1, regardless of the location of both nodes.
     *
     * @param a to be connected gridnode
     * @param b to be connected gridnode
     * @throws IllegalStateException If the nodes are already connected.
     */
    public static IGridConnection createConnection(IGridNode a, IGridNode b) {
        return GridConnection.create(a, b, null);
    }

}

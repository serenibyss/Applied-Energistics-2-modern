/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.init;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.LazyOptional;
import net.neoforged.neoforge.event.AttachCapabilitiesEvent;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.storage.MEStorage;
import appeng.capabilities.Capabilities;
import appeng.core.AppEng;
import appeng.helpers.externalstorage.GenericStackFluidStorage;
import appeng.helpers.externalstorage.GenericStackItemStorage;

public final class InitCapabilities {
    private InitCapabilities() {
    }

    /**
     * Register AE2 provided capabilities.
     */
    public static void init(RegisterCapabilitiesEvent evt) {
        evt.register(MEStorage.class);
        evt.register(ICraftingMachine.class);
        evt.register(GenericInternalInventory.class);
    }

    public static void registerGenericInvWrapper(AttachCapabilitiesEvent<BlockEntity> event) {
        event.addCapability(AppEng.makeId("generic_inv_wrapper"), new ICapabilityProvider() {
            final BlockEntity be = event.getObject();

            @NotNull
            @Override
            public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if (cap == net.neoforged.neoforge.common.capabilities.Capabilities.ITEM_HANDLER) {
                    return be.getCapability(Capabilities.GENERIC_INTERNAL_INV, side)
                            .lazyMap(GenericStackItemStorage::new).cast();
                } else if (cap == net.neoforged.neoforge.common.capabilities.Capabilities.FLUID_HANDLER) {
                    return be.getCapability(Capabilities.GENERIC_INTERNAL_INV, side)
                            .lazyMap(GenericStackFluidStorage::new).cast();
                }
                return LazyOptional.empty();
            }
        });
    }
}

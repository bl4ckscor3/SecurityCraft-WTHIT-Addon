package bl4ckscor3.mod.scwthitaddon;

import java.util.List;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.ICommonAccessor;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.event.WailaRenderEvent;
import mcp.mobius.waila.api.event.WailaTooltipEvent;
import net.geforcemods.securitycraft.ClientHandler;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.compat.IOverlayDisplay;
import net.geforcemods.securitycraft.compat.hudmods.HudModHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

public final class WTHITDataProvider extends HudModHandler implements IWailaPlugin, IBlockComponentProvider, IEntityComponentProvider {
	static {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			MinecraftForge.EVENT_BUS.addListener(WTHITDataProvider::onWailaRender);
			MinecraftForge.EVENT_BUS.addListener(WTHITDataProvider::onWailaTooltip);
		}
	}

	@Override
	public void register(IRegistrar registrar) {
		registrar.addSyncedConfig(SHOW_OWNER, true);
		registrar.addSyncedConfig(SHOW_MODULES, true);
		registrar.addSyncedConfig(SHOW_CUSTOM_NAME, true);
		registrar.addComponent((IBlockComponentProvider) this, TooltipPosition.HEAD, IOverlayDisplay.class);
		registrar.addComponent((IBlockComponentProvider) this, TooltipPosition.BODY, IOwnable.class);
		registrar.addDisplayItem((IBlockComponentProvider) this, IOverlayDisplay.class);
		registrar.addComponent((IEntityComponentProvider) this, TooltipPosition.BODY, IOwnable.class);
	}

	@Override
	public ItemStack getDisplayItem(IBlockAccessor data, IPluginConfig config) {
		ItemStack displayStack = ((IOverlayDisplay) data.getBlock()).getDisplayStack(data.getWorld(), data.getBlockState(), data.getPosition());

		if (displayStack != null)
			return displayStack;
		else
			return IBlockComponentProvider.super.getDisplayItem(data, config);
	}

	@Override
	public void appendHead(List<ITextComponent> head, IBlockAccessor data, IPluginConfig config) {
		ItemStack displayStack = ((IOverlayDisplay) data.getBlock()).getDisplayStack(data.getWorld(), data.getBlockState(), data.getPosition());

		if (displayStack != null)
			head.set(0, new TranslationTextComponent(displayStack.getDescriptionId()).setStyle(ITEM_NAME_STYLE));
	}

	@Override
	public void appendBody(List<ITextComponent> tooltip, IBlockAccessor data, IPluginConfig config) {
		World level = data.getWorld();
		BlockPos pos = data.getPosition();
		BlockState state = data.getBlockState();
		Block block = data.getBlock();

		addDisguisedOwnerModuleNameInfo(level, pos, state, block, data.getBlockEntity(), data.getPlayer(), tooltip::add, config::get);
	}

	@Override
	public void appendBody(List<ITextComponent> tooltip, IEntityAccessor data, IPluginConfig config) {
		addEntityInfo(data.getEntity(), data.getPlayer(), tooltip::add, config::get);
	}

	public static void onWailaRender(WailaRenderEvent.Pre event) {
		if (ClientHandler.isPlayerMountedOnCamera())
			event.setCanceled(true);
	}

	public static void onWailaTooltip(WailaTooltipEvent event) {
		ICommonAccessor accessor = event.getAccessor();
		Block block = accessor.getBlock();

		if (block instanceof IOverlayDisplay) {
			World world = accessor.getWorld();
			BlockPos pos = accessor.getPosition();
			ItemStack disguisedAs = ((IOverlayDisplay) block).getDisplayStack(world, world.getBlockState(pos), pos);

			if (disguisedAs != null) {
				List<ITextComponent> tip = event.getCurrentTip();

				for (int i = 0; i < tip.size(); i++) {
					ITextComponent line = tip.get(i);

					if (line.getString().equals("${waila:mod_name}")) {
						String spoofedModName = ModList.get().getModContainerById(disguisedAs.getItem().getRegistryName().getNamespace()).get().getModInfo().getDisplayName();

						tip.set(i, new StringTextComponent(spoofedModName).setStyle(MOD_NAME_STYLE));
						return;
					}
				}
			}
		}
	}
}

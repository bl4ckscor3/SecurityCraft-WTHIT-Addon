package bl4ckscor3.mod.scwthitaddon;

import java.util.List;
import java.util.Optional;

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
import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.blocks.DisguisableBlock;
import net.geforcemods.securitycraft.compat.IOverlayDisplay;
import net.geforcemods.securitycraft.entity.sentry.Sentry;
import net.geforcemods.securitycraft.entity.sentry.Sentry.SentryMode;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.INameable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

public final class WTHITDataProvider implements IWailaPlugin, IBlockComponentProvider, IEntityComponentProvider {
	public static final WTHITDataProvider INSTANCE = new WTHITDataProvider();
	public static final ResourceLocation SHOW_OWNER = new ResourceLocation(SecurityCraft.MODID, "showowner");
	public static final ResourceLocation SHOW_MODULES = new ResourceLocation(SecurityCraft.MODID, "showmodules");
	public static final ResourceLocation SHOW_CUSTOM_NAME = new ResourceLocation(SecurityCraft.MODID, "showcustomname");
	private static final Style MOD_NAME_STYLE = Style.EMPTY.withColor(TextFormatting.BLUE).withItalic(true);
	private static final Style ITEM_NAME_STYLE = Style.EMPTY.applyFormat(TextFormatting.WHITE);

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
		registrar.addComponent((IBlockComponentProvider) INSTANCE, TooltipPosition.HEAD, IOverlayDisplay.class);
		registrar.addComponent((IBlockComponentProvider) INSTANCE, TooltipPosition.BODY, IOwnable.class);
		registrar.addDisplayItem((IBlockComponentProvider) INSTANCE, IOverlayDisplay.class);
		registrar.addComponent((IEntityComponentProvider) INSTANCE, TooltipPosition.BODY, IOwnable.class);
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
	public void appendBody(List<ITextComponent> body, IBlockAccessor data, IPluginConfig config) {
		Block block = data.getBlock();
		boolean disguised = false;

		if (block instanceof DisguisableBlock) {
			Optional<BlockState> disguisedBlockState = DisguisableBlock.getDisguisedBlockState(data.getWorld(), data.getPosition());

			if (disguisedBlockState.isPresent()) {
				disguised = true;
				block = disguisedBlockState.get().getBlock();
			}
		}

		if (block instanceof IOverlayDisplay && !((IOverlayDisplay) block).shouldShowSCInfo(data.getWorld(), data.getBlockState(), data.getPosition()))
			return;

		TileEntity be = data.getBlockEntity();

		//last part is a little cheaty to prevent owner info from being displayed on non-sc blocks
		if (config.get(SHOW_OWNER) && be instanceof IOwnable && block.getRegistryName().getNamespace().equals(SecurityCraft.MODID))
			body.add(Utils.localize("waila.securitycraft:owner", PlayerUtils.getOwnerComponent(((IOwnable) be).getOwner())));

		if (disguised)
			return;

		//if the te is ownable, show modules only when it's owned, otherwise always show
		if (config.get(SHOW_MODULES) && be instanceof IModuleInventory && (!(be instanceof IOwnable) || ((IOwnable) be).isOwnedBy(data.getPlayer()))) {
			if (!((IModuleInventory) be).getInsertedModules().isEmpty())
				body.add(Utils.localize("waila.securitycraft:equipped"));

			for (ModuleType module : ((IModuleInventory) be).getInsertedModules()) {
				body.add(new StringTextComponent("- ").append(new TranslationTextComponent(module.getTranslationKey())));
			}
		}

		if (config.get(SHOW_CUSTOM_NAME) && be instanceof INameable && ((INameable) be).hasCustomName()) {
			ITextComponent text = ((INameable) be).getCustomName();
			ITextComponent name = text == null ? StringTextComponent.EMPTY : text;

			body.add(Utils.localize("waila.securitycraft:customName", name));
		}
	}

	@Override
	public void appendBody(List<ITextComponent> body, IEntityAccessor data, IPluginConfig config) {
		Entity entity = data.getEntity();

		if (entity instanceof IOwnable && config.get(SHOW_OWNER))
			body.add(Utils.localize("waila.securitycraft:owner", PlayerUtils.getOwnerComponent(((IOwnable) entity).getOwner())));

		if (entity instanceof Sentry) {
			Sentry sentry = (Sentry) entity;
			SentryMode mode = sentry.getMode();

			if (config.get(SHOW_MODULES) && sentry.isOwnedBy(data.getPlayer()) && (!sentry.getAllowlistModule().isEmpty() || !sentry.getDisguiseModule().isEmpty() || sentry.hasSpeedModule())) {
				body.add(Utils.localize("waila.securitycraft:equipped"));

				if (!sentry.getAllowlistModule().isEmpty())
					body.add(new StringTextComponent("- ").append(new TranslationTextComponent(ModuleType.ALLOWLIST.getTranslationKey())));

				if (!sentry.getDisguiseModule().isEmpty())
					body.add(new StringTextComponent("- ").append(new TranslationTextComponent(ModuleType.DISGUISE.getTranslationKey())));

				if (sentry.hasSpeedModule())
					body.add(new StringTextComponent("- ").append(new TranslationTextComponent(ModuleType.SPEED.getTranslationKey())));
			}

			IFormattableTextComponent modeDescription = Utils.localize(mode.getModeKey());

			if (mode != SentryMode.IDLE)
				modeDescription.append("- ").append(Utils.localize(mode.getTargetKey()));

			body.add(modeDescription);
		}
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

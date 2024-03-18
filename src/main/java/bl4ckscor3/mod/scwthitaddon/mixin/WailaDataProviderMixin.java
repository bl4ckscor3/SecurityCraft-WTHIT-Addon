package bl4ckscor3.mod.scwthitaddon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcp.mobius.waila.api.IRegistrar;
import net.geforcemods.securitycraft.compat.waila.WailaDataProvider;

/**
 * WTHIT loads SecurityCraft's waila plugin which does not entirely work with WTHIT. Since this mod adds proper support for
 * WTHIT, the plugin needs to be prevented from loading.
 */
@Mixin(WailaDataProvider.class)
public class WailaDataProviderMixin {
	@Inject(method = "register", at = @At("HEAD"), cancellable = true)
	private void scwthitaddon$preventSecurityCraftsWailaPluginFromLoading(IRegistrar registrar, CallbackInfo ci) {
		ci.cancel();
	}
}

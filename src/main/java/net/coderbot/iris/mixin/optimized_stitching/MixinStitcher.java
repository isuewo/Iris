package net.coderbot.iris.mixin.optimized_stitching;

import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.Stitcher.Holder;
import net.minecraft.client.renderer.texture.Stitcher.Region;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(Stitcher.class)
public class MixinStitcher {
	@Shadow
	@Final
	private List<Region> storage;
	@Shadow
	private int storageX;
	@Shadow
	private int storageY;
	@Shadow
	@Final
	private int maxWidth;
	@Shadow
	@Final
	private int maxHeight;

	/**
	 * This method is a copy of the vanilla method with the growWidth calculation rewritten.
	 *
	 * @author PepperCode1
	 * @reason Optimize region creation to allow for a smaller atlas
	 */
	@Overwrite
	private boolean expand(Holder holder) {
		int newEffectiveWidth = Mth.smallestEncompassingPowerOfTwo(storageX + holder.width);
		int newEffectiveHeight = Mth.smallestEncompassingPowerOfTwo(storageY + holder.height);
		boolean canFitWidth = newEffectiveWidth <= maxWidth;
		boolean canFitHeight = newEffectiveHeight <= maxHeight;

		if (!canFitWidth && !canFitHeight) {
			return false;
		}

		boolean growWidth;
		if (canFitWidth & canFitHeight) {
			// Effective size calculation moved from head to be inside if block
			int effectiveWidth = Mth.smallestEncompassingPowerOfTwo(storageX);
			int effectiveHeight = Mth.smallestEncompassingPowerOfTwo(storageY);
			boolean wouldGrowEffectiveWidth = effectiveWidth != newEffectiveWidth;
			boolean wouldGrowEffectiveHeight = effectiveHeight != newEffectiveHeight;

			if (wouldGrowEffectiveWidth) {
				if (wouldGrowEffectiveHeight && effectiveWidth <= effectiveHeight) {
					/*
					 * If both the width and height would be grown, definitively grow the
					 * width if it is less than or equal to the height.
					 */
					growWidth = true;
				} else {
					/*
					 * If the height is grown, the new region's width is equal to the total
					 * width. However, if the holder's width is larger than the total width,
					 * the new region will not be able to contain it. Therefore, the width
					 * should be grown instead.
					 *
					 * By extension, this check does not have to be performed if growWidth
					 * is already true.
					 *
					 * This check does not have to be performed when wouldGrowEffectiveWidth
					 * is false because this means that the total width would be less than
					 * doubled, meaning that the holder width is less than the total width.
					 *
					 * A similar check does not have to be performed for the heights due to
					 * the sprite sorting - taller sprites are always added before shorter
					 * ones.
					 */
					growWidth = holder.width > storageX;
				}
			} else {
				if (wouldGrowEffectiveHeight) {
					/*
					 * If the height would be grown but the width would not be, grow the
					 * width to fill up the unused space on the right.
					 */
					growWidth = true;
				} else {
					/*
					 * If neither the width nor height would be grown, grow the dimension
					 * that is smaller than the other. If both dimensions are already the
					 * same, grow the width.
					 */
					growWidth = effectiveWidth <= effectiveHeight;
				}
			}
		} else {
			growWidth = canFitWidth;
		}

		Region region;
		if (growWidth) {
			if (storageY == 0) {
				storageY = holder.height;
			}

			region = new Region(storageX, 0, holder.width, storageY);
			storageX += holder.width;
		} else {
			region = new Region(0, storageY, storageX, holder.height);
			storageY += holder.height;
		}

		region.add(holder);
		storage.add(region);
		return true;
	}
}

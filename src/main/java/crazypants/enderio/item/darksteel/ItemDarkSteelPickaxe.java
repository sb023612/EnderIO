package crazypants.enderio.item.darksteel;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import cofh.api.energy.IEnergyContainerItem;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.EnderIO;
import crazypants.enderio.EnderIOTab;
import crazypants.enderio.config.Config;
import crazypants.enderio.gui.IAdvancedTooltipProvider;
import crazypants.enderio.machine.power.PowerDisplayUtil;
import crazypants.enderio.teleport.IItemOfTravel;
import crazypants.enderio.teleport.TravelController;
import crazypants.enderio.teleport.TravelSource;
import crazypants.render.BoundingBox;
import crazypants.util.BlockCoord;
import crazypants.util.ItemUtil;
import crazypants.util.Lang;

public class ItemDarkSteelPickaxe extends ItemPickaxe implements IEnergyContainerItem, IAdvancedTooltipProvider, IDarkSteelItem, IItemOfTravel {

  private static final Set SHOVEL_BLOCKS = Sets.newHashSet(new Block[] {
      Blocks.grass, Blocks.dirt, Blocks.sand, Blocks.gravel, Blocks.snow_layer, Blocks.snow,
      Blocks.clay, Blocks.farmland, Blocks.soul_sand, Blocks.mycelium });

  public static boolean isEquipped(EntityPlayer player) {
    if(player == null) {
      return false;
    }
    ItemStack equipped = player.getCurrentEquippedItem();
    if(equipped == null) {
      return false;
    }
    return equipped.getItem() == EnderIO.itemDarkSteelPickaxe;
  }

  public static boolean isEquippedAndPowered(EntityPlayer player, int requiredPower) {
    if(!isEquipped(player)) {
      return false;
    }
    return EnderIO.itemDarkSteelPickaxe.getEnergyStored(player.getCurrentEquippedItem()) >= requiredPower;
  }

  public static ItemDarkSteelPickaxe create() {
    ItemDarkSteelPickaxe res = new ItemDarkSteelPickaxe();
    res.init();
    MinecraftForge.EVENT_BUS.register(res);
    return res;
  }

  private long lastBlickTick = -1;

  public ItemDarkSteelPickaxe() {
    super(ItemDarkSteelSword.MATERIAL);
    setCreativeTab(EnderIOTab.tabEnderIO);
    String str = "darkSteel_pickaxe";
    setUnlocalizedName(str);
    setTextureName("enderIO:" + str);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void getSubItems(Item item, CreativeTabs par2CreativeTabs, List par3List) {
    ItemStack is = new ItemStack(this);
    par3List.add(is);

    is = new ItemStack(this);
    EnergyUpgrade.EMPOWERED_FOUR.writeToItem(is);
    EnergyUpgrade.setPowerFull(is);
    TravelUpgrade.INSTANCE.writeToItem(is);
    SpoonUpgrade.INSTANCE.writeToItem(is);
    par3List.add(is);
  }

  @Override
  public int getIngotsRequiredForFullRepair() {
    return 3;
  }

  @Override
  public boolean isDamaged(ItemStack stack) {
    return false;
  }

  @Override
  public boolean hitEntity(ItemStack par1ItemStack, EntityLivingBase par2EntityLivingBase, EntityLivingBase par3EntityLivingBase) {
    applyDamage(par3EntityLivingBase, par1ItemStack, 2);
    return true;
  }

  @Override
  public boolean onBlockDestroyed(ItemStack item, World world, Block block, int x, int y, int z, EntityLivingBase entLiving) {
    if(block.getBlockHardness(world, x, y, z) != 0.0D) {
      if(block == Blocks.obsidian) {
        extractEnergy(item, Config.darkSteelPickPowerUseObsidian, false);
      }
      applyDamage(entLiving, item, 1);
    }
    return true;
  }

  @Override
  public boolean onItemUse(ItemStack item, EntityPlayer player, World world, int x, int y, int z, int side, float par8, float par9, float par10) {
    return doRightClickItemPlace(player, world, x, y, z, side, par8, par9, par10);
  }

  static boolean doRightClickItemPlace(EntityPlayer player, World world, int x, int y, int z, int side, float par8, float par9, float par10) {
    int current = player.inventory.currentItem;
    int slot = current == 0 && Config.slotZeroPlacesEight ? 8 : current + 1;
    if(slot < 9 && player.inventory.mainInventory[slot] != null && !(player.inventory.mainInventory[slot].getItem() instanceof IDarkSteelItem)) {

      if(!canPlaceBlockOnRightClick(player, world, x, y, z, side, slot)) {
        return false;
      }
      boolean ret = player.inventory.mainInventory[slot].getItem().onItemUse(player.inventory.mainInventory[slot], player, world, x, y, z, side, par8,
          par9, par10);
      if(player.inventory.mainInventory[slot].stackSize <= 0) {
        player.inventory.mainInventory[slot] = null;
      }
      return ret;
    }
    return false;
  }

  static boolean canPlaceBlockOnRightClick(EntityPlayer player, World world, int x, int y, int z, int side, int slot) {
    BlockCoord placeCoord = new BlockCoord(x, y, z).getLocation(ForgeDirection.getOrientation(side));
    ItemStack toUse = player.inventory.mainInventory[slot];
    AxisAlignedBB aabb;
    Block blk = Block.getBlockFromItem(toUse.getItem());
    if(blk != null) {
      aabb = blk.getCollisionBoundingBoxFromPool(world, placeCoord.x, placeCoord.y, placeCoord.z);
    } else {
      BoundingBox bb = new BoundingBox(placeCoord);
      aabb = bb.getAxisAlignedBB();
    }      
    if(aabb != null && aabb.intersectsWith(player.boundingBox)) {
      return false;
    }
    return true;
  }

  private void applyDamage(EntityLivingBase entity, ItemStack item, int damage) {

    EnergyUpgrade eu = EnergyUpgrade.loadFromItem(item);
    if(eu != null && eu.isAbsorbDamageWithPower() && eu.getEnergy() > 0) {
      eu.extractEnergy(damage * Config.darkSteelPickPowerUsePerDamagePoint, false);

    } else {
      damage = item.getItemDamage() + damage;
      if(damage >= getMaxDamage()) {
        item.stackSize = 0;
      }
      item.setItemDamage(damage);
    }
    if(eu != null) {
      eu.writeToItem(item);
    }

  }

  @Override
  public float func_150893_a(ItemStack item, Block block) {
    int energy = getEnergyStored(item);
    if(block == Blocks.obsidian && energy > 0) {
      return super.func_150893_a(item, block) + Config.darkSteelPickEffeciencyObsidian;
    }
    if(energy > 0 && hasSpoonUpgrade(item)) {
      if(SHOVEL_BLOCKS.contains(block)) {
        return efficiencyOnProperMaterial;
      }
    }
    return super.func_150893_a(item, block);
  }

  private boolean hasSpoonUpgrade(ItemStack item) {
    return SpoonUpgrade.loadFromItem(item) != null;
  }

  @Override
  public float getDigSpeed(ItemStack stack, Block block, int meta) {
    if(ForgeHooks.isToolEffective(stack, block, meta)) {
      if(Config.darkSteelPickPowerUsePerDamagePoint <= 0 || getEnergyStored(stack) > 0) {
        return ItemDarkSteelSword.MATERIAL.getEfficiencyOnProperMaterial() + Config.darkSteelPickEffeciencyBoostWhenPowered;
      }
      return ItemDarkSteelSword.MATERIAL.getEfficiencyOnProperMaterial();
    }
    return super.getDigSpeed(stack, block, meta);
  }

  protected void init() {
    GameRegistry.registerItem(this, getUnlocalizedName());
  }

  @Override
  public int receiveEnergy(ItemStack container, int maxReceive, boolean simulate) {
    return EnergyUpgrade.receiveEnergy(container, maxReceive, simulate);
  }

  @Override
  public int extractEnergy(ItemStack container, int maxExtract, boolean simulate) {
    return EnergyUpgrade.extractEnergy(container, maxExtract, simulate);
  }

  @Override
  public int getEnergyStored(ItemStack container) {
    return EnergyUpgrade.getEnergyStored(container);
  }

  @Override
  public int getMaxEnergyStored(ItemStack container) {
    return EnergyUpgrade.getMaxEnergyStored(container);
  }

  @Override
  public boolean getIsRepairable(ItemStack i1, ItemStack i2) {
    return false;
  }

  @Override
  public void addCommonEntries(ItemStack itemstack, EntityPlayer entityplayer, List list, boolean flag) {
    DarkSteelRecipeManager.instance.addCommonTooltipEntries(itemstack, entityplayer, list, flag);
  }

  @Override
  public void addBasicEntries(ItemStack itemstack, EntityPlayer entityplayer, List list, boolean flag) {
    DarkSteelRecipeManager.instance.addBasicTooltipEntries(itemstack, entityplayer, list, flag);
  }

  @Override
  public void addDetailedEntries(ItemStack itemstack, EntityPlayer entityplayer, List list, boolean flag) {
    if(!Config.addDurabilityTootip) {
      list.add(ItemUtil.getDurabilityString(itemstack));
    }
    String str = EnergyUpgrade.getStoredEnergyString(itemstack);
    if(str != null) {
      list.add(str);
    }
    if(EnergyUpgrade.itemHasAnyPowerUpgrade(itemstack)) {
      list.add(EnumChatFormatting.WHITE + "+" + Config.darkSteelPickEffeciencyBoostWhenPowered + " "
          + Lang.localize("item.darkSteel_pickaxe.tooltip.effPowered"));
      list.add(EnumChatFormatting.WHITE + "+" + Config.darkSteelPickEffeciencyObsidian + " "
          + Lang.localize("item.darkSteel_pickaxe.tooltip.effObs") + " ");
      list.add(EnumChatFormatting.WHITE + "     (cost "
          + PowerDisplayUtil.formatPower(Config.darkSteelPickPowerUseObsidian / 10) + " "
          + PowerDisplayUtil.abrevation() + ")");
    }
    DarkSteelRecipeManager.instance.addAdvancedTooltipEntries(itemstack, entityplayer, list, flag);
  }

  public ItemStack createItemStack() {
    return new ItemStack(this);
  }

  @Override
  public boolean isActive(EntityPlayer ep, ItemStack equipped) {
    return isTravelUpgradeActive(ep, equipped);
  }

  @Override
  public void extractInternal(ItemStack equipped, int power) {
    extractEnergy(equipped, power, false);
  }

  private boolean isTravelUpgradeActive(EntityPlayer ep, ItemStack equipped) {
    return isEquipped(ep) && ep.isSneaking() && TravelUpgrade.loadFromItem(equipped) != null;
  }

  @Override
  public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
    if(isTravelUpgradeActive(player, stack)) {
      if(world.isRemote) {
        if(TravelController.instance.activateTravelAccessable(stack, world, player, TravelSource.STAFF)) {
          player.swingItem();
          return stack;
        }
      }

      long ticksSinceBlink = player.worldObj.getTotalWorldTime() - lastBlickTick;
      if(ticksSinceBlink < 0) {
        lastBlickTick = -1;
      }
      if(Config.travelStaffBlinkEnabled && world.isRemote && ticksSinceBlink >= Config.travelStaffBlinkPauseTicks) {
        if(TravelController.instance.doBlink(stack, player)) {
          player.swingItem();
          lastBlickTick = player.worldObj.getTotalWorldTime();
        }
      }
      return stack;
    }

    return super.onItemRightClick(stack, world, player);
  }

}

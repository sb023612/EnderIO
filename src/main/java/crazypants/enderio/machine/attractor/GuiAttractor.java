package crazypants.enderio.machine.attractor;

import java.awt.Color;

import net.minecraft.entity.player.InventoryPlayer;

import org.lwjgl.opengl.GL11;

import crazypants.enderio.machine.GuiMachineBase;
import crazypants.enderio.machine.spawner.ContainerPoweredSpawner;
import crazypants.enderio.machine.spawner.TilePoweredSpawner;
import crazypants.render.ColorUtil;
import crazypants.render.RenderUtil;
import crazypants.util.Lang;

public class GuiAttractor extends GuiMachineBase {

  private TileAttractor ta;
  
  public GuiAttractor(InventoryPlayer par1InventoryPlayer, TileAttractor te) {
    super(te, new ContainerAttractor(par1InventoryPlayer, te));
    ta = te;
  }
  
  @Override
  protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    RenderUtil.bindTexture("enderio:textures/gui/attractor.png");
    int sx = (width - xSize) / 2;
    int sy = (height - ySize) / 2;

    drawTexturedModalRect(sx, sy, 0, 0, this.xSize, this.ySize);
    
    super.drawGuiContainerBackgroundLayer(par1, par2, par3);
    
    int range = ta.getRange();
    drawCenteredString(fontRendererObj, Lang.localize("gui.spawnGurad.range") + " " + range, getGuiLeft() + sx/2 + 9, getGuiTop() + 68, ColorUtil.getRGB(Color.white));
  }

  @Override
  protected boolean showRecipeButton() {    
    return false;
  }

}

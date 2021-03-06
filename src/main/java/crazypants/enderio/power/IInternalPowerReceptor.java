package crazypants.enderio.power;

import net.minecraftforge.common.util.ForgeDirection;
import cofh.api.energy.IEnergyHandler;

public interface IInternalPowerReceptor extends IEnergyHandler {

  int getMaxEnergyRecieved(ForgeDirection dir);
  
  int getEnergyStored();
  
  int getMaxEnergyStored();
  
  void setEnergyStored(int stored);
  
}

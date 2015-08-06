package de.agentlv.dynamicholograms.nms.v1_8_R3;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.EntityItem;
import net.minecraft.server.v1_8_R3.GameProfileSerializer;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.Items;
import net.minecraft.server.v1_8_R3.MathHelper;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.PacketPlayOutAttachEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntity.PacketPlayOutRelEntityMove;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;

import de.agentlv.dynamicholograms.nms.NMSHologram;
import de.agentlv.dynamicholograms.objects.HoloItem;
import de.agentlv.dynamicholograms.objects.Hologram;
import de.agentlv.dynamicholograms.objects.PlayerSkullData;

public class NmsHologramImpl implements NMSHologram {
	
	@Override
	public Object create(Hologram hologram) {
		
		Location loc = hologram.getLocation();
		EntityArmorStand as = (EntityArmorStand) addMessage(hologram, hologram.getMessage(0));
			
		if (hologram.hasHoloItem()) {
			
			HoloItem holoItem = hologram.getHoloItem();
			ItemStack nmsStack;
			
			if (holoItem.isPlayerSkull()) {
				
				PlayerSkullData skullData = holoItem.getPlayerSkullData();
				
				nmsStack = new ItemStack(Items.SKULL, 1, 3);
				nmsStack.setTag(new NBTTagCompound());
				NBTTagCompound skullOwnerTag = new NBTTagCompound();
				GameProfileSerializer.serialize(skullOwnerTag, new GameProfile(skullData.getUniqueId(), skullData.getPlayerName()));
				nmsStack.getTag().set("SkullOwner", skullOwnerTag);
			
			} else {
				nmsStack = new ItemStack(Item.d(holoItem.getItemName()));
			}
			
			EntityItem item = new EntityItem(((CraftWorld) loc.getWorld()).getHandle());
	        item.setItemStack(nmsStack);
	        item.setLocation(loc.getX(), loc.getY() + 1.5, loc.getZ(), loc.getYaw(), loc.getPitch());
				
			for (Player p : hologram.getPlayers()) {
				PlayerConnection playerConnection = ((CraftPlayer) p).getHandle().playerConnection;
				
				PacketPlayOutSpawnEntity itemPacket = new PacketPlayOutSpawnEntity(item, 2, 0);
				playerConnection.sendPacket(itemPacket);
				
				PacketPlayOutEntityMetadata itemMetaDataPacket = new PacketPlayOutEntityMetadata(item.getId(), item.getDataWatcher(), true);
				playerConnection.sendPacket(itemMetaDataPacket);
				
				PacketPlayOutAttachEntity attachEntityPacket = new PacketPlayOutAttachEntity(0, item, as);
				playerConnection.sendPacket(attachEntityPacket);
			}
			
			//Set internal stuff
			holoItem.setArmorStand(as);
			holoItem.setItem(item);
		}
		
		return as;
	}
	
	@Override
	public void showPlayer(Hologram hologram, Player player) {
		
		PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection;
		EntityItem item = hologram.getHoloItem() == null ? null : (EntityItem) hologram.getHoloItem().getItem();
		HoloItem holoItem = hologram.getHoloItem();
		
		for (Object as : hologram.getArmorStands()) {
			PacketPlayOutSpawnEntityLiving packet = new PacketPlayOutSpawnEntityLiving((EntityArmorStand) as);
			((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
		}
		
		if (hologram.hasHoloItem()) {
			
			PacketPlayOutSpawnEntity itemPacket = new PacketPlayOutSpawnEntity(item, 2, 0);
			playerConnection.sendPacket(itemPacket);
			
			PacketPlayOutEntityMetadata itemMetaDataPacket = new PacketPlayOutEntityMetadata(item.getId(), item.getDataWatcher(), true);
			playerConnection.sendPacket(itemMetaDataPacket);
			
			PacketPlayOutAttachEntity attachEntityPacket = new PacketPlayOutAttachEntity(0, item, (EntityArmorStand) holoItem.getArmorStand());
			playerConnection.sendPacket(attachEntityPacket);
		}
			
	}
	
	@Override
	public void hidePlayer(Hologram hologram, Player player) {
		
		for (Object as : hologram.getArmorStands()) {
			PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(((EntityArmorStand) as).getId());
			((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
		}
	}
	
	
	
	@Override
	public void move(Hologram hologram, Location newLocation) {
		
		List<EntityArmorStand> armorStands = new ArrayList<EntityArmorStand>();
		
		for (Object as : hologram.getArmorStands())
			armorStands.add((EntityArmorStand) as);

		Location oldLocation = hologram.getLocation();
		
		int oldX = MathHelper.floor(oldLocation.getX() * 32.0D);
        int oldY = MathHelper.floor(oldLocation.getY() * 32.0D);
        int oldZ = MathHelper.floor(oldLocation.getZ() * 32.0D);
		
		int newX = MathHelper.floor(newLocation.getX() * 32.0D);
        int newY = MathHelper.floor(newLocation.getY() * 32.0D);
        int newZ = MathHelper.floor(newLocation.getZ() * 32.0D);
        
        int dx = newX - oldX;
        int dy = newY - oldY;
        int dz = newZ - oldZ;
        
        if (dx >= -128 && dx < 128 && dy >= -128 && dy < 128 && dz >= -128 && dz < 128) {
           
        	for (Player p : hologram.getPlayers()) {
	        	for (EntityArmorStand as : armorStands) {
					PacketPlayOutRelEntityMove packet = new PacketPlayOutRelEntityMove(as.getId(), (byte) dx, (byte) dy, 
							(byte) dz, false);
					((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
	        	}
        	}
        	
        } else {
        	
        	int distance = MathHelper.floor(hologram.getDistance() * 32.0D);
        	
        	for (Player p : hologram.getPlayers()) {
	        	for (EntityArmorStand as : armorStands) {
		        	PacketPlayOutEntityTeleport packet = new PacketPlayOutEntityTeleport(as.getId(), newX, newY, newZ, (
		        			byte) newLocation.getYaw(), (byte) newLocation.getPitch(), false);
		        	((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		        	
		        	newY -= distance;
	        	}
        	}
        }
        
        hologram.setLocation(newLocation);
		
	}
	
	
	
	@Override
	public Object addMessage(Hologram hologram, String message) {
		
		EntityArmorStand lastAs = hologram.getArmorStands().size() == 0 ? null : (EntityArmorStand) hologram.getArmorStand(hologram.getArmorStands().size() - 1);
		EntityArmorStand as;
		
		if (lastAs != null) {
			as = new EntityArmorStand(lastAs.world);
			as.setInvisible(true);
			as.setCustomName(message);
			as.setCustomNameVisible(true);
			as.setSmall(true);
			as.setGravity(false);
			as.setLocation(lastAs.locX, lastAs.locY - hologram.getDistance(), lastAs.locZ, lastAs.yaw, lastAs.pitch);
			
		} else {
			Location loc = hologram.getLocation();
	
			as = new EntityArmorStand(((CraftWorld) loc.getWorld()).getHandle());
			as.setInvisible(true);
			as.setCustomName(message);
			as.setCustomNameVisible(true);
			as.setSmall(true);
			as.setGravity(false);
			as.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
			
		}
		
		for (Player p : hologram.getPlayers()) {
			PacketPlayOutSpawnEntityLiving packet = new PacketPlayOutSpawnEntityLiving(as);
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		}
		
		return as;
		
	}
	
	@Override
	public Object setMessage(Hologram hologram, int index, String message) {
		EntityArmorStand as = (EntityArmorStand) hologram.getArmorStands().get(index);
		
		as.setCustomName(message);
		
		for (Player p : hologram.getPlayers()) {
			PacketPlayOutEntityMetadata itemMetaDataPacket = new PacketPlayOutEntityMetadata(as.getId(), as.getDataWatcher(), true);
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(itemMetaDataPacket);
		}
		return as;
	}
	
	@Override
	public void removeMessage(Hologram hologram, int index) {
		EntityArmorStand removeArmorStand = (EntityArmorStand) hologram.getArmorStands().get(index);
		List<Double> pos = new ArrayList<Double>();
		
		for (Player p : hologram.getPlayers()) {
			PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(removeArmorStand.getId());
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		}

		for (int i = index + 1; i < hologram.getArmorStands().size(); ++i) {
			EntityArmorStand as = (EntityArmorStand) hologram.getArmorStands().get(i);
			EntityArmorStand oldAs = (EntityArmorStand) hologram.getArmorStands().get(i - 1);
			int newY = MathHelper.floor(oldAs.locY * 32.0D) - MathHelper.floor(as.locY * 32.0D);
			
			for (Player p : hologram.getPlayers()) {
	        	PacketPlayOutRelEntityMove packet = new PacketPlayOutRelEntityMove(as.getId(), (byte) 0, (byte) newY, 
						(byte) 0, false);
				((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
        	}
			pos.add(oldAs.locY);
		}
		
		//Restore the correct positions
		int j = 0;
		for (int i = index + 1; i < hologram.getArmorStands().size(); ++i) {
			EntityArmorStand as = (EntityArmorStand) hologram.getArmorStands().get(i);
			as.setLocation(as.locX, pos.get(j), as.locZ, as.yaw, as.pitch);
			++j;
		}
		
	}
	
	@Override
	public HoloItem setHoloItem(Hologram hologram, HoloItem holoItem) {
		
		EntityArmorStand as = (EntityArmorStand) holoItem.getArmorStand();
		Location loc = hologram.getLocation();
		ItemStack nmsStack;
		
		if (holoItem.isPlayerSkull()) {
			
			PlayerSkullData skullData = holoItem.getPlayerSkullData();
			
			nmsStack = new ItemStack(Items.SKULL, 1, 3);
			nmsStack.setTag(new NBTTagCompound());
			NBTTagCompound skullOwnerTag = new NBTTagCompound();
			GameProfileSerializer.serialize(skullOwnerTag, new GameProfile(skullData.getUniqueId(), skullData.getPlayerName()));
			nmsStack.getTag().set("SkullOwner", skullOwnerTag);
		
		} else {
			nmsStack = new ItemStack(Item.d(holoItem.getItemName()));
		}
		
		EntityItem item = new EntityItem(((CraftWorld) loc.getWorld()).getHandle());
        item.setItemStack(nmsStack);
        item.setLocation(loc.getX(), loc.getY() + 1.5, loc.getZ(), loc.getYaw(), loc.getPitch());
		
        if (hologram.hasHoloItem()) {
	    	EntityItem oldItem = (EntityItem) hologram.getHoloItem().getItem();
			
			for (Player p : hologram.getPlayers()) {
				PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(oldItem.getId());
				((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
			}
        }
	
    
		for (Player p : hologram.getPlayers()) {
			PlayerConnection playerConnection = ((CraftPlayer) p).getHandle().playerConnection;
			
			PacketPlayOutSpawnEntity itemPacket = new PacketPlayOutSpawnEntity(item, 2, 0);
			playerConnection.sendPacket(itemPacket);
			
			PacketPlayOutEntityMetadata itemMetaDataPacket = new PacketPlayOutEntityMetadata(item.getId(), item.getDataWatcher(), true);
			playerConnection.sendPacket(itemMetaDataPacket);
			
			PacketPlayOutAttachEntity attachEntityPacket = new PacketPlayOutAttachEntity(0, item, as);
			playerConnection.sendPacket(attachEntityPacket);
			
		}
		
		holoItem.setItem(item);
		return holoItem;
	}
	
	@Override
	public void removeHoloItem(Hologram hologram) {
		
		if (hologram.hasHoloItem()) {
			
			for (Player p : hologram.getPlayers()) {
				PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(((EntityItem) hologram.getHoloItem().getItem()).getId());
				((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
			}
		}
		
	}

	@Override
	public void remove(Hologram hologram) {
		
		for (Player p : hologram.getPlayers()) {
			
			PlayerConnection playerConnection = ((CraftPlayer) p).getHandle().playerConnection;
			
			if (hologram.hasHoloItem()) {
				PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(((EntityItem) hologram.getHoloItem().getItem()).getId());
				playerConnection.sendPacket(packet);
			}
			
			for (Object as : hologram.getArmorStands()) {
        		PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(((EntityArmorStand) as).getId());
        		playerConnection.sendPacket(packet);
        	}
    	}
	}

}
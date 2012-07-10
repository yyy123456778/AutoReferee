package org.mctourney.AutoReferee;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

import org.mctourney.AutoReferee.AutoReferee.eMatchStatus;
import org.mctourney.AutoReferee.util.BlockData;

public class ObjectiveTracker implements Listener 
{
	AutoReferee plugin = null;
	
	public ObjectiveTracker(Plugin p)
	{
		plugin = (AutoReferee) p;
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void blockPlace(BlockPlaceEvent event)
	{
		AutoRefMatch match = plugin.getMatch(event.getBlock().getWorld());
		if (match != null) match.checkWinConditions(null);
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void blockBreak(BlockBreakEvent event)
	{
		AutoRefMatch match = plugin.getMatch(event.getBlock().getWorld());
		if (match != null) match.checkWinConditions(event.getBlock().getLocation());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void blockInteract(PlayerInteractEvent event)
	{
		if (event.hasBlock())
		{
			AutoRefMatch match = plugin.getMatch(event.getClickedBlock().getWorld());
			if (match != null) match.checkWinConditions(null);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void entityInteract(PlayerInteractEntityEvent event)
	{
		AutoRefMatch match = plugin.getMatch(event.getRightClicked().getWorld());
		if (match != null) match.checkWinConditions(null);
	}
	
	class InventoryChangeTask implements Runnable
	{
		AutoRefPlayer apl = null;
		
		public InventoryChangeTask(AutoRefPlayer apl)
		{ this.apl = apl; }
		
		public void run()
		{ if (apl != null) apl.updateCarrying(); }
	}
	
	public void inventoryChange(HumanEntity entity)
	{
		AutoRefMatch match = plugin.getMatch(entity.getWorld());
		if (match == null) return;
		
		if (match.getCurrentState() == eMatchStatus.PLAYING &&
			entity.getType() == EntityType.PLAYER)
		{
			AutoRefPlayer apl = match.getPlayer((Player) entity);
			if (apl != null) plugin.getServer().getScheduler()
				.scheduleSyncDelayedTask(plugin, new InventoryChangeTask(apl));
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void inventoryClick(InventoryClickEvent event)
	{ inventoryChange(event.getWhoClicked()); }
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void itemDrop(PlayerDropItemEvent event)
	{ inventoryChange(event.getPlayer()); }
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void itemPickup(PlayerPickupItemEvent event)
	{ inventoryChange(event.getPlayer()); }
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void playerRespawn(PlayerRespawnEvent event)
	{ inventoryChange(event.getPlayer()); }
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void blockPlaceInventory(BlockPlaceEvent event)
	{ inventoryChange(event.getPlayer()); }
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void itemCraft(CraftItemEvent event)
	{
		AutoRefMatch match = plugin.getMatch(event.getWhoClicked().getWorld());
		if (match == null) return;
		
		if (!(event.getWhoClicked() instanceof Player)) return;
		AutoRefTeam team = plugin.getTeam((Player) event.getWhoClicked());
		
		BlockData recipeTarget = BlockData.fromItemStack(event.getRecipe().getResult());
		if (team != null && team.winConditions.containsValue(recipeTarget))
			event.setCancelled(true);
	}
}
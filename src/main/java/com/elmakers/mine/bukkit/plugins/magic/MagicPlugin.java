package com.elmakers.mine.bukkit.plugins.magic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.elmakers.mine.bukkit.dao.BlockData;
import com.elmakers.mine.bukkit.utilities.borrowed.ConfigurationNode;
import com.elmakers.mine.bukkit.utilities.borrowed.MaterialAndData;

public class MagicPlugin extends JavaPlugin
{	
	/*
	 * Public API
	 */
	public Spells getSpells()
	{
		return spells;
	}

	/*
	 * Plugin interface
	 */

	public void onEnable() 
	{
		if (spells == null) {
			spells = new Spells(this);
		}
		initialize();

		BlockData.setServer(getServer());
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(spells, this);
	}

	protected void initialize()
	{
		spells.initialize(this);
	}
	
	protected void checkRunningTask()
	{
		if (runningTask != null && runningTask.isFinished()) {
			runningTask = null;
		}
	}
	
	protected void populateChests(CommandSender sender, World world, int ymax)
	{
		checkRunningTask();
		if (runningTask != null) {
			sender.sendMessage("There is already a populate job running");
			return;
		}
		runningTask= new WandChestRunnable(spells, world, ymax);
		runningTask.runTaskTimer(this, 5, 5);
	}

	@SuppressWarnings("deprecation")
	protected void handleWandCommandTab(List<String> options, PlayerSpells player, CommandSender sender, Command cmd, String alias, String[] args)
	{
		if (args.length == 0) {
			return;
		}
		if (args.length == 1) {
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "add");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "remove");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "name");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "fill");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "configure");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "combine");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "upgrade");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "describe");
			Collection<String> allWands = Wand.getWandKeys();
			for (String wandKey : allWands) {
				addIfPermissible(sender, options, "Magic.commands." + cmd.getName() + ".wand.", wandKey, true);
			}
			return;
		}
		
		if (args.length == 2) {
			String subCommand = args[0];
			String subCommandPNode = "Magic.commands." + cmd.getName() + "." + subCommand;
			
			if (!spells.hasPermission(sender, subCommandPNode)) {
				return;
			}
			
			subCommandPNode += ".";
			
			if (subCommand.equalsIgnoreCase("add")) {
				List<Spell> spellList = spells.getAllSpells();
				for (Spell spell : spellList) {
					addIfPermissible(sender, options, subCommandPNode, spell.getKey(), true);
				}
				addIfPermissible(sender, options, subCommandPNode, "material", true);
			}
			
			if (subCommand.equalsIgnoreCase("remove")) {
				Wand activeWand = player == null ? null : player.getActiveWand();
				if (activeWand != null) {
					Collection<String> spellNames = activeWand.getSpells();
					for (String spellName : spellNames) {
						options.add(spellName);
					}
					
					options.add("material");
				}
			}
			
			if (subCommand.equalsIgnoreCase("combine")) {
				Collection<String> allWands = Wand.getWandKeys();
				for (String wandKey : allWands) {
					addIfPermissible(sender, options, "Magic.commands." + cmd.getName() + ".combine.", wandKey, true);
				}
			}
		}
		
		if (args.length == 3)
		{
			String subCommand = args[0];
			String subCommand2 = args[1];
			
			String subCommandPNode = "Magic.commands." + cmd.getName() + "." + subCommand + "." + subCommand2;
			
			if (!spells.hasPermission(sender, subCommandPNode, true)) {
				return;
			}
			
			if (subCommand.equalsIgnoreCase("remove") && subCommand2.equalsIgnoreCase("material")) {
				Wand activeWand = player == null ? null : player.getActiveWand();
				if (activeWand != null) {
					Collection<String> materialNames = activeWand.getMaterialNames();
					for (String materialName : materialNames) {
						MaterialAndData materialData = ConfigurationNode.toMaterialAndData(materialName);
						if (materialData != null) {
							options.add(materialData.getKey());
						}
					}
				}
			}
			
			if (subCommand.equalsIgnoreCase("add") && subCommand2.equalsIgnoreCase("material")) {
				Material[] materials = Material.values();
				for (Material material : materials) {
					// Kind of a hack..
					if (material.getId() < 256) {
						options.add(material.name().toLowerCase());
					}
				}
			}
		}
		
		// TODO : Custom completion for configure, upgrade
	}

	protected void handleCastCommandTab(List<String> options, CommandSender sender, Command cmd, String alias, String[] args)
	{
		if (args.length == 1) {
			List<Spell> spellList = spells.getAllSpells();
			for (Spell spell : spellList) {
				addIfPermissible(sender, options, "Magic." + cmd.getName() + ".", spell.getKey(), true);
			}
			
			return;
		}
		
		// TODO : Custom completion for spell parameters
	}
	
	protected void addIfPermissible(CommandSender sender, List<String> options, String permissionPrefix, String option, boolean defaultValue)
	{
		if (spells.hasPermission(sender, permissionPrefix + option, defaultValue))
		{
			options.add(option);
		}
	}
	
	protected void addIfPermissible(CommandSender sender, List<String> options, String permissionPrefix, String option)
	{
		addIfPermissible(sender, options, permissionPrefix, option, false);
	}
	
	@EventHandler
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args)
	{
		// TODO: Permission filtering!
		
		PlayerSpells playerSpells = null;
		if (sender instanceof Player) {
			playerSpells = spells.getPlayerSpells((Player)sender);
		}
		String completeCommand = args.length > 0 ? args[args.length - 1] : "";
		List<String> options = new ArrayList<String>();
		if (cmd.getName().equalsIgnoreCase("magic"))
		{
			if (args.length == 1) {
				addIfPermissible(sender, options, "Magic.commands.", "populate");
				addIfPermissible(sender, options, "Magic.commands.", "search");
				addIfPermissible(sender, options, "Magic.commands.", "cancel");
				addIfPermissible(sender, options, "Magic.commands.", "reload");
			}
		}
		else if (cmd.getName().equalsIgnoreCase("wand")) 
		{
			handleWandCommandTab(options, playerSpells, sender, cmd, alias, args);
		}
		else if (cmd.getName().equalsIgnoreCase("wandp")) 
		{
			if (args.length == 1) {
				options.addAll(Spells.getPlayerNames());
			} else if (args.length > 1) {
				playerSpells = spells.getPlayerSpells(args[0]);
				String[] args2 = Arrays.copyOfRange(args, 1, args.length);
				handleWandCommandTab(options, playerSpells, sender, cmd, alias, args2);
			}
		}
		else if (cmd.getName().equalsIgnoreCase("cast")) 
		{
			handleCastCommandTab(options, sender, cmd, alias, args);
		}
		else if (cmd.getName().equalsIgnoreCase("castp")) 
		{
			if (args.length == 1) {
				options.addAll(Spells.getPlayerNames());
			} else if (args.length > 1) {
				String[] args2 = Arrays.copyOfRange(args, 1, args.length);
				handleCastCommandTab(options, sender, cmd, alias, args2);
			}
		}
		
		if (completeCommand.length() > 0) {
			completeCommand = completeCommand.toLowerCase();
			List<String> allOptions = options;
			options = new ArrayList<String>();
			for (String option : allOptions) {
				String lowercase = option.toLowerCase();
				if (lowercase.startsWith(completeCommand)) {
					options.add(option);
				}
			}
		}
		
		Collections.sort(options);
		
		return options;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		String commandName = cmd.getName();

		if (commandName.equalsIgnoreCase("magic") && args.length > 0)
		{
			String subCommand = args[0];
			if (sender instanceof Player)
			{
				if (!spells.hasPermission((Player)sender, "Magic.commands.magic." + subCommand)) return false;
			}
			if (subCommand.equalsIgnoreCase("reload"))
			{
				spells.clear();
				spells.load();
				return true;
			}
			if (subCommand.equalsIgnoreCase("populate") || subCommand.equalsIgnoreCase("search"))
			{   
				World world = null;
				int ymax = 50;
				if (sender instanceof Player) {
					world = ((Player)sender).getWorld();
					if (args.length > 1) {
						ymax = Integer.parseInt(args[1]);
					}
				} else {
					if (args.length > 1) {
						String worldName = args[1];
						world = Bukkit.getWorld(worldName);
					}
					if (args.length > 2) {
						ymax = Integer.parseInt(args[2]);
					}
				}
				if (world == null) {
					getLogger().warning("Usage: magic " + subCommand + " <world> <ymax>");
					return true;
				}
				if (subCommand.equalsIgnoreCase("search")) {
					ymax = 0;
				}
				populateChests(sender, world, ymax);
				return true;
			}
			if (subCommand.equalsIgnoreCase("cancel"))
			{ 
				checkRunningTask();
				if (runningTask != null) {
					runningTask.cancel();
					runningTask = null;
					sender.sendMessage("Job cancelled");
				} else {
					sender.sendMessage("There is no job running");
				}
				return true;
			}
		}

		if (commandName.equalsIgnoreCase("wandp"))
		{
			if (args.length == 0) {
				sender.sendMessage("Usage: /wandp [player] [wand name/command]");
				return true;
			}
			Player player = Bukkit.getPlayer(args[0]);
			if (player == null) {
				sender.sendMessage("Can't find player " + args[0]);
				return true;
			}
			if (!player.isOnline()) {
				sender.sendMessage("Player " + args[0] + " is not online");
				return true;
			}
			String[] args2 = Arrays.copyOfRange(args, 1, args.length);
			return processWandCommand("wandp", sender, player, args2);
		}

		if (commandName.equalsIgnoreCase("castp"))
		{
			if (args.length == 0) {
				sender.sendMessage("Usage: /castp [player] [spell] <parameters>");
				return true;
			}
			Player player = Bukkit.getPlayer(args[0]);
			if (player == null) {
				sender.sendMessage("Can't find player " + args[0]);
				return true;
			}
			if (!player.isOnline()) {
				sender.sendMessage("Player " + args[0] + " is not online");
				return true;
			}
			String[] args2 = Arrays.copyOfRange(args, 1, args.length);
			return processCastCommand(sender, player, args2);
		}

		if (!(sender instanceof Player)) {
			if (commandName.equalsIgnoreCase("spells"))
			{
				listSpells(sender, -1, null);
				return true;
			}
			if (commandName.equalsIgnoreCase("wand") && args.length > 0 && args[0].equalsIgnoreCase("list"))
			{
				onWandList(sender);
				return true;
			}
			
			return false;
		}

		// Everything beyond this point is is-game only
		Player player = (Player)sender;
		if (commandName.equalsIgnoreCase("wand"))
		{
			return processWandCommand("wand", sender, player, args);
		}

		if (commandName.equalsIgnoreCase("cast"))
		{
			if (!spells.hasPermission(player, "Magic.commands.cast")) return false;
			return processCastCommand(player, player, args);

		}

		if (commandName.equalsIgnoreCase("spells"))
		{
			if (!spells.hasPermission(player, "Magic.commands.spells")) return false;
			return onSpells(player, args);
		}

		return false;
	}
	
	protected boolean processWandCommand(String command, CommandSender sender, Player player, String[] args)
	{
		String subCommand = "";
		String[] args2 = args;

		if (args.length > 0) {
			subCommand = args[0];
			args2 = new String[args.length - 1];
			for (int i = 1; i < args.length; i++) {
				args2[i - 1] = args[i];
			}
		}
		if (subCommand.equalsIgnoreCase("list"))
		{
			if (!spells.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandList(sender);
			return true;
		}
		if (subCommand.equalsIgnoreCase("add"))
		{
			if (!spells.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;
			if (args2.length > 0 && args2[0].equals("material") && !spells.hasPermission(sender,"Magic.commands.wand.add." + args2[0], true)) return true;
			if (args2.length > 0 && !spells.hasPermission(sender,"Magic.commands.wand.add.spell." + args2[0], true)) return true;
			onWandAdd(sender, player, args2);
			return true;
		}
		if (subCommand.equalsIgnoreCase("configure"))
		{
			if (!spells.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandConfigure(sender, player, args2, false);
			return true;
		}
		if (subCommand.equalsIgnoreCase("combine"))
		{
			if (!spells.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;
			if (args.length > 0 && !spells.hasPermission(sender,"Magic.commands." + command + ".combine." + args[0], true)) return true;
			
			onWandCombine(sender, player, args2);
			return true;
		}
		if (subCommand.equalsIgnoreCase("describe"))
		{
			if (!spells.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandDescribe(sender, player);
			return true;
		}
		if (subCommand.equalsIgnoreCase("upgrade"))
		{
			if (!spells.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandConfigure(sender, player, args2, true);
			return true;
		}
		if (subCommand.equalsIgnoreCase("fill"))
		{
			if (!spells.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandFill(sender, player);
			return true;
		}
		if (subCommand.equalsIgnoreCase("remove"))
		{   
			if (!spells.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandRemove(sender, player, args2);
			return true;
		}

		if (subCommand.equalsIgnoreCase("name"))
		{
			if (!spells.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandName(sender, player, args2);
			return true;
		}

		if (!spells.hasPermission(sender, "Magic.commands." + command + "")) return true;
		if (subCommand.length() > 0 && !spells.hasPermission(sender,"Magic.commands." + command +".wand." + subCommand, true)) return true;
		
		return onWand(sender, player, args);
	}

	public boolean onWandList(CommandSender sender) {
		Collection<ConfigurationNode> templates = Wand.getWandTemplates();
		Map<String, ConfigurationNode> nameMap = new TreeMap<String, ConfigurationNode>();
		for (ConfigurationNode templateConfig : templates)
		{
			nameMap.put(templateConfig.getString("key"), templateConfig);
		}
		for (ConfigurationNode templateConfig : nameMap.values())
		{
			String key = templateConfig.getString("key");
			String name = templateConfig.getString("name");
			String description = templateConfig.getString("description");
			description = ChatColor.YELLOW + description; 
			if (!name.equals(key)) {
				description = ChatColor.BLUE + name + ChatColor.WHITE + " : " + description;
			}
			sender.sendMessage(ChatColor.AQUA + key + ChatColor.WHITE + " : " + description);
		}

		return true;
	}

	public boolean onWandDescribe(CommandSender sender, Player player) {
		PlayerSpells playerSpells = spells.getPlayerSpells(player);
		Wand wand = playerSpells.getActiveWand();
		if (wand == null) {
			if (sender != player) {
				sender.sendMessage(player.getName() + " isn't holding a wand");
			} else {
				player.sendMessage("Equip a wand first");
			}
			return true;
		}
		
		wand.describe(sender);

		return true;
	}
	
	public boolean onWandConfigure(CommandSender sender, Player player, String[] parameters, boolean safe)
	{
		if (parameters.length < 2) {
			sender.sendMessage("Use: /wand configure <property> <value>");
			sender.sendMessage("Properties: cost_reduction, uses, health_regeneration, hunger_regeneration, xp_regeneration,");
			sender.sendMessage("  xp_max, protection, protection_physical, protection_projectiles,");
			sender.sendMessage("  protection_falling,protection_fire, protection_explosions, haste, power");
			
			return true;
		}

		PlayerSpells playerSpells = spells.getPlayerSpells(player);
		Wand wand = playerSpells.getActiveWand();
		if (wand == null) {
			player.sendMessage("Equip a wand first");
			if (sender != player) {
				sender.sendMessage(player.getName() + " isn't holding a wand");
			}
			return true;
		}
		ConfigurationNode node = new ConfigurationNode();
		node.setProperty(parameters[0], parameters[1]);
		wand.deactivate();
		wand.configureProperties(node, safe);
		wand.activate(playerSpells);
		player.sendMessage("Wand reconfigured");
		if (sender != player) {
			sender.sendMessage(player.getName() + ",s wand reconfigured");
		}
		return true;
	}

	public boolean onWandCombine(CommandSender sender, Player player, String[] parameters)
	{
		if (parameters.length < 1) {
			sender.sendMessage("Use: /wand combine <wandname>");
			return true;
		}

		PlayerSpells playerSpells = spells.getPlayerSpells(player);
		Wand wand = playerSpells.getActiveWand();
		if (wand == null) {
			player.sendMessage("Equip a wand first");
			if (sender != player) {
				sender.sendMessage(player.getName() + " isn't holding a wand");
			}
			return true;
		}

		String wandName = parameters[0];
		Wand newWand = Wand.createWand(spells, wandName);
		if (newWand == null) {
			sender.sendMessage("Unknown wand name " + wandName);
			return true;
		}
		wand.deactivate();
		wand.add(newWand);
		wand.activate(playerSpells);
		
		player.sendMessage("Wand upgraded");
		if (sender != player) {
			sender.sendMessage(player.getName() + ",s wand upgraded");
		}
		return true;
	}

	public boolean onWandFill(CommandSender sender, Player player)
	{
		PlayerSpells playerSpells = spells.getPlayerSpells(player);
		Wand wand = playerSpells.getActiveWand();
		if (wand == null) {
			player.sendMessage("Equip a wand first");
			if (sender != player) {
				sender.sendMessage(player.getName() + " isn't holding a wand");
			}
			return true;
		}
		
		fillWand(wand, player);
		player.sendMessage("Your wand now contains all the spells you know");
		if (sender != player) {
			sender.sendMessage(player.getName() + "'s wand filled");
		}
		
		return true;
	}
	
	public boolean onWandAdd(CommandSender sender, Player player, String[] parameters)
	{
		if (parameters.length < 1) {
			sender.sendMessage("Use: /wand add <spell|material> [material:data]");
			return true;
		}

		PlayerSpells playerSpells = spells.getPlayerSpells(player);
		Wand wand = playerSpells.getActiveWand();
		if (wand == null) {
			player.sendMessage("Equip a wand first");
			if (sender != player) {
				sender.sendMessage(player.getName() + " isn't holding a wand");
			}
			return true;
		}
		if (!wand.isModifiable()) {
			player.sendMessage("This wand can not be modified");
			if (sender != player) {
				sender.sendMessage(player.getName() + "'s wand can't be modified");
			}
			return true;
		}

		String spellName = parameters[0];
		if (spellName.equals("material")) {
			if (parameters.length < 2) {
				sender.sendMessage("Use: /wand add material <material:data>");
				return true;
			}
			String[] pieces = StringUtils.split(parameters[1], ':');
			String materialName = pieces[0];
			byte data = 0;
			Material material = Material.AIR;
			if (materialName.equals("erase")) {
				material = Wand.EraseMaterial;
			} else if (materialName.equals("clone") || materialName.equals("copy")) {
				material = Wand.CopyMaterial;
			} else{
				material = ConfigurationNode.toMaterial(materialName);
				if (material == null || material == Material.AIR) {
					sender.sendMessage(materialName + " is not a valid material");
					return true;
				}
				if (pieces.length > 1) {
					data = Byte.parseByte(pieces[1]);
				}
			}
			if (wand.addMaterial(material, data, true, false)) {
				player.sendMessage("Material '" + materialName + "' has been added to your wand");
				if (sender != player) {
					sender.sendMessage("Added material '" + materialName + "' to " + player.getName() + "'s wand");
				}
			} else {
				player.sendMessage("Material activated: " + materialName);
				if (sender != player) {
					sender.sendMessage(player.getName() + "'s wand already has material " + materialName);
				}
			}
			return true;
		}
		Spell spell = playerSpells.getSpell(spellName);
		if (spell == null)
		{
			sender.sendMessage("Spell '" + spellName + "' unknown, Use /spells for spell list");
			return true;
		}

		if (wand.addSpell(spellName, true)) {
			player.sendMessage("Spell '" + spell.getName() + "' has been added to your wand");
			if (sender != player) {
				sender.sendMessage("Added '" + spell.getName() + "' to " + player.getName() + "'s wand");
			}
		} else {
			player.sendMessage(spell.getName() + " activated");
			if (sender != player) {
				sender.sendMessage(player.getName() + "'s wand already has " + spell.getName());
			}
		}

		return true;
	}

	public boolean onWandRemove(CommandSender sender, Player player, String[] parameters)
	{
		if (parameters.length < 1) {
			sender.sendMessage("Use: /wand remove <spell|material> [material:data]");
			return true;
		}

		PlayerSpells playerSpells = spells.getPlayerSpells(player);
		Wand wand = playerSpells.getActiveWand();
		if (wand == null) {
			player.sendMessage("Equip a wand first");
			if (sender != player) {
				sender.sendMessage(player.getName() + " isn't holding a wand");
			}
			return true;
		}
		if (!wand.isModifiable()) {
			player.sendMessage("This wand can not be modified");
			if (sender != player) {
				sender.sendMessage(player.getName() + "'s wand can't be modified");
			}
			return true;
		}

		String spellName = parameters[0];	
		if (spellName.equals("material")) {
			if (parameters.length < 2) {
				sender.sendMessage("Use: /wand remove material <material:data>");
				return true;
			}
			String[] pieces = StringUtils.split(parameters[1], ':');
			String materialName = pieces[0];
			Material material = Material.AIR;
			byte data = 0;
			if (materialName.equals("erase")) {
				material = Wand.EraseMaterial;
			} else if (materialName.equals("copy") || materialName.equals("clone")) {
				material = Wand.CopyMaterial;
			} else {
				material = ConfigurationNode.toMaterial(materialName);
				if (pieces.length > 1) {
					data = Byte.parseByte(pieces[1]);
				}
			}
			if (wand.removeMaterial(material, data)) {
				player.sendMessage("Material '" + materialName + "' has been removed from your wand");
				if (sender != player) {
					sender.sendMessage("Removed material '" + materialName + "' from " + player.getName() + "'s wand");
				}
			} else {
				if (sender != player) {
					sender.sendMessage(player.getName() + "'s wand does not have material " + materialName);
				}
			}
			return true;
		}
		if (wand.removeSpell(spellName)) {
			player.sendMessage("Spell '" + spellName + "' has been removed from your wand");
			if (sender != player) {
				sender.sendMessage("Removed '" + spellName + "' from " + player.getName() + "'s wand");
			}
		} else {
			if (sender != player) {
				sender.sendMessage(player.getName() + "'s wand does not have " + spellName);
			}
		}

		return true;
	}

	public boolean onWandName(CommandSender sender, Player player, String[] parameters)
	{
		if (parameters.length < 1) {
			sender.sendMessage("Use: /wand name <name>");
			return true;
		}
		
		PlayerSpells playerSpells = spells.getPlayerSpells(player);
		Wand wand = playerSpells.getActiveWand();
		if (wand == null) {
			player.sendMessage("Equip a wand first");
			if (sender != player) {
				sender.sendMessage(player.getName() + " isn't holding a wand");
			}
			return true;
		}
		
		wand.setName(StringUtils.join(parameters, " "));
		sender.sendMessage("Wand renamed");

		return true;
	}

	public boolean onWand(CommandSender sender, Player player, String[] parameters)
	{
		String wandName = null;
		if (parameters.length > 0)
		{
			wandName = parameters[0];
		}

		PlayerSpells playerSpells = spells.getPlayerSpells(player);
		Wand currentWand =  playerSpells.getActiveWand();
		if (currentWand != null) {
			currentWand.closeInventory();
		}
	
		Wand wand = Wand.createWand(spells, wandName);
		if (wand == null) {
			sender.sendMessage("No wand defined with key " + wandName);
			return true;
		}
		
		// Check for special "fill wands" configuration
		if (spells.fillWands() && parameters.length == 0) {
			fillWand(wand, player);
		}
	
		// Place directly in hand if possible
		PlayerInventory inventory = player.getInventory();
		ItemStack inHand = inventory.getItemInHand();
		if (inHand == null || inHand.getType() == Material.AIR) {
			inventory.setItem(inventory.getHeldItemSlot(), wand.getItem());
			wand.activate(playerSpells);
		} else {
			player.getInventory().addItem(wand.getItem());
		}
		if (sender != player) {
			sender.sendMessage("Gave wand " + wand.getName() + " to " + player.getName());
		}
		return true;
	}
	
	protected void fillWand(Wand wand, Player player) {
		List<Spell> allSpells = spells.getAllSpells();

		for (Spell spell : allSpells)
		{
			if (spell.hasSpellPermission(player))
			{
				wand.addSpell(spell.getKey());
			}
		}
	}
	
	public boolean processCastCommand(CommandSender sender, Player player, String[] castParameters)
	{
		if (castParameters.length < 1) return false;

		String spellName = castParameters[0];
		String[] parameters = new String[castParameters.length - 1];
		for (int i = 1; i < castParameters.length; i++)
		{
			parameters[i - 1] = castParameters[i];
		}

		Player usePermissions = (sender instanceof Player) ? (Player)sender : player;
		PlayerSpells playerSpells = spells.getPlayerSpells(player);
		Spell spell = playerSpells.getSpell(spellName, usePermissions);
		if (spell == null)
		{
			sender.sendMessage("Spell " + spellName + " unknown");
			return false;
		}

		// Make it free and skip cooldowns, if configured to do so.
		spells.toggleCastCommandOverrides(playerSpells, true);
		spell.cast(parameters);
		spells.toggleCastCommandOverrides(playerSpells, false);
		if (sender != player) {
			sender.sendMessage("Cast " + spellName + " on " + player.getName());
		}

		return true;
	}

	public boolean onReload(CommandSender sender, String[] parameters)
	{
		spells.load();
		sender.sendMessage("Configuration reloaded.");
		return true;
	}

	public boolean onSpells(Player player, String[] parameters)
	{
		int pageNumber = 1;
		String category = null;
		if (parameters.length > 0)
		{
			try
			{
				pageNumber = Integer.parseInt(parameters[0]);
			}
			catch (NumberFormatException ex)
			{
				pageNumber = 1;
				category = parameters[0];
			}
		}
		listSpells(player, pageNumber, category);

		return true;
	}


	/* 
	 * Help commands
	 */

	public void listSpellsByCategory(CommandSender sender, String category)
	{
		List<Spell> categorySpells = new ArrayList<Spell>();
		List<Spell> spellVariants = spells.getAllSpells();
		Player player = sender instanceof Player ? (Player)sender : null;
		for (Spell spell : spellVariants)
		{
			if (spell.getCategory().equalsIgnoreCase(category) && (player == null || spell.hasSpellPermission(player)))
			{
				categorySpells.add(spell);
			}
		}

		if (categorySpells.size() == 0)
		{
			player.sendMessage("You don't know any spells");
			return;
		}

		Collections.sort(categorySpells);
		for (Spell spell : categorySpells)
		{
			String name = spell.getName();
			String description = spell.getDescription();
			if (!name.equals(spell.getKey())) {
				description = name + " : " + description;
			}
			player.sendMessage(ChatColor.AQUA + spell.getKey() + ChatColor.BLUE + " [" + spell.getIcon().getMaterial().name().toLowerCase() + "] : " + ChatColor.YELLOW + description);
		}
	}

	public void listCategories(Player player)
	{
		HashMap<String, Integer> spellCounts = new HashMap<String, Integer>();
		List<String> spellGroups = new ArrayList<String>();
		List<Spell> spellVariants = spells.getAllSpells();

		for (Spell spell : spellVariants)
		{
			if (!spell.hasSpellPermission(player)) continue;

			Integer spellCount = spellCounts.get(spell.getCategory());
			if (spellCount == null || spellCount == 0)
			{
				spellCounts.put(spell.getCategory(), 1);
				spellGroups.add(spell.getCategory());
			}
			else
			{
				spellCounts.put(spell.getCategory(), spellCount + 1);
			}
		}
		if (spellGroups.size() == 0)
		{
			player.sendMessage("You don't know any spells");
			return;
		}

		Collections.sort(spellGroups);
		for (String group : spellGroups)
		{
			player.sendMessage(group + " [" + spellCounts.get(group) + "]");
		}
	}

	public void listSpells(CommandSender sender, int pageNumber, String category)
	{
		if (category != null)
		{
			listSpellsByCategory(sender, category);
			return;
		}
		Player player = sender instanceof Player ? (Player)sender : null;

		HashMap<String, SpellGroup> spellGroups = new HashMap<String, SpellGroup>();
		List<Spell> spellVariants = spells.getAllSpells();

		int spellCount = 0;
		for (Spell spell : spellVariants)
		{
			if (player != null && !spell.hasSpellPermission(player))
			{
				continue;
			}
			spellCount++;
			SpellGroup group = spellGroups.get(spell.getCategory());
			if (group == null)
			{
				group = new SpellGroup();
				group.groupName = spell.getCategory();
				spellGroups.put(group.groupName, group);	
			}
			group.spells.add(spell);
		}

		List<SpellGroup> sortedGroups = new ArrayList<SpellGroup>();
		sortedGroups.addAll(spellGroups.values());
		Collections.sort(sortedGroups);

		int maxLines = -1;
		if (pageNumber >= 0) {
			maxLines = 5;
			int maxPages = spellCount / maxLines + 1;
			if (pageNumber > maxPages)
			{
				pageNumber = maxPages;
			}

			sender.sendMessage("You know " + spellCount + " spells. [" + pageNumber + "/" + maxPages + "]");
		} else {
			sender.sendMessage("Listing " + spellCount + " spells.");	
		}

		int currentPage = 1;
		int lineCount = 0;
		int printedCount = 0;
		for (SpellGroup group : sortedGroups)
		{
			if (printedCount > maxLines && maxLines > 0) break;

			boolean isFirst = true;
			Collections.sort(group.spells);
			for (Spell spell : group.spells)
			{
				if (printedCount > maxLines && maxLines > 0) break;

				if (currentPage == pageNumber || maxLines < 0)
				{
					if (isFirst)
					{
						sender.sendMessage(group.groupName + ":");
						isFirst = false;
					}
					String name = spell.getName();
					String description = spell.getDescription();
					if (!name.equals(spell.getKey())) {
						description = name + " : " + description;
					}
					sender.sendMessage(ChatColor.AQUA + spell.getKey() + ChatColor.BLUE + " [" + spell.getIcon().getMaterial().name().toLowerCase() + "] : " + ChatColor.YELLOW + description);
					printedCount++;
				}
				lineCount++;
				if (lineCount == maxLines)
				{
					lineCount = 0;
					currentPage++;
				}	
			}
		}
	}

	public void onDisable() 
	{
		spells.save();
		spells.clear();
	}

	/*
	 * Private data
	 */	
	private Spells spells = null;
	private WandChestRunnable runningTask = null;
}

package fr.axel.llamadupe;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.ShulkerBox;

import java.util.*;

public class LlamaDupePlugin extends JavaPlugin implements Listener {
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private final Map<UUID, UUID> playerLlamaMap = new HashMap<>();
    private final Map<UUID, ItemStack[]> llamaInventories = new HashMap<>();
    private boolean dupeEnabled = true;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("llamadupe").setExecutor((sender, command, label, args) -> {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("enable")) {
                    dupeEnabled = true;
                    sender.sendMessage("§aLlamaDupe activé !");
                    return true;
                } else if (args[0].equalsIgnoreCase("disable")) {
                    dupeEnabled = false;
                    sender.sendMessage("§cLlamaDupe désactivé !");
                    return true;
                }
            }
            sender.sendMessage("§eUsage: /llamadupe <enable|disable>");
            return true;
        });
        this.getCommand("llamadupe").setTabCompleter((sender, command, alias, args) -> {
            if (args.length == 1) {
                return Arrays.asList("enable", "disable");
            }
            return Collections.emptyList();
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!dupeEnabled) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Vérifier si le joueur est sur un lama
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof Llama llama) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(llama.getInventory())) {
                // Le joueur a interagi avec l'inventaire du lama
                playerLlamaMap.put(player.getUniqueId(), llama.getUniqueId());
                
                // Sauvegarder l'inventaire après un petit délai pour que l'item soit placé
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    ItemStack[] contents = llama.getInventory().getContents();
                    llamaInventories.put(llama.getUniqueId(), cloneInventoryContents(contents));
                    
                    // Debug : compter les items non-null
                    int itemCount = 0;
                    for (ItemStack item : contents) {
                        if (item != null && item.getType() != Material.AIR) {
                            itemCount++;
                            getLogger().info("Item sauvegardé: " + item.getType() + " x" + item.getAmount());
                        }
                    }
                    getLogger().info("Inventaire du lama sauvegardé pour " + player.getName() + " - " + itemCount + " items trouvés");
                }, 1L); // 1 tick de délai
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!dupeEnabled) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        
        // Vérifier si le joueur était sur un lama et a fermé l'inventaire du lama
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof Llama llama) {
            if (event.getInventory().equals(llama.getInventory())) {
                // Sauvegarder l'inventaire final du lama
                playerLlamaMap.put(player.getUniqueId(), llama.getUniqueId());
                ItemStack[] contents = llama.getInventory().getContents();
                llamaInventories.put(llama.getUniqueId(), cloneInventoryContents(contents));
                
                // Debug : compter les items
                int itemCount = 0;
                for (ItemStack item : contents) {
                    if (item != null && item.getType() != Material.AIR) {
                        itemCount++;
                        getLogger().info("Item final sauvegardé: " + item.getType() + " x" + item.getAmount());
                    }
                }
                getLogger().info("Inventaire final du lama sauvegardé pour " + player.getName() + " - " + itemCount + " items");
            }
        }
    }

    @EventHandler
    public void onEntityMount(EntityMountEvent event) {
        if (!dupeEnabled) return;
        if (event.getEntity() instanceof Player player && event.getMount() instanceof Llama llama) {
            UUID playerId = player.getUniqueId();
            UUID llamaId = llama.getUniqueId();
            
            // Vérifier si ce joueur a déjà interagi avec ce lama
            if (playerLlamaMap.containsKey(playerId) && playerLlamaMap.get(playerId).equals(llamaId)) {
                Long lastTime = playerCooldowns.get(playerId);
                
                // Vérifier le cooldown de 15 secondes
                if (lastTime != null && System.currentTimeMillis() - lastTime >= 15000) {
                    getLogger().info("Déclenchement de la duplication pour " + player.getName());
                    dupeLlamaInventory(player, llama);
                    
                    // Nettoyer les données
                    playerCooldowns.remove(playerId);
                    playerLlamaMap.remove(playerId);
                    llamaInventories.remove(llamaId);
                    
                    player.sendMessage("§a§lDuplication effectuée !");
                } else if (lastTime != null) {
                    long remaining = 15000 - (System.currentTimeMillis() - lastTime);
                    player.sendMessage("§c§lCooldown actif ! Attendez encore " + (remaining / 1000) + " secondes.");
                }
            }
        }
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        if (!dupeEnabled) return;
        if (event.getEntity() instanceof Player player && event.getDismounted() instanceof Llama llama) {
            UUID playerId = player.getUniqueId();
            UUID llamaId = llama.getUniqueId();
            
            // Sauvegarder l'inventaire du lama au moment du démontage (au cas où)
            ItemStack[] contents = llama.getInventory().getContents();
            
            // Compter les items au démontage
            int itemCount = 0;
            for (ItemStack item : contents) {
                if (item != null && item.getType() != Material.AIR && !isPlayerHead(item)) {
                    itemCount++;
                    getLogger().info("Item au démontage: " + item.getType() + " x" + item.getAmount());
                }
            }
            
            // Seulement envoyer des messages et démarrer le cooldown s'il y a des items
            if (itemCount > 0) {
                playerLlamaMap.put(playerId, llamaId);
                llamaInventories.put(llamaId, cloneInventoryContents(contents));
                getLogger().info("Inventaire sauvegardé au démontage pour " + player.getName() + " - " + itemCount + " items");
                
                // Démarrer le cooldown
                playerCooldowns.put(playerId, System.currentTimeMillis());
                player.sendMessage("§e§lCooldown de 15 secondes démarré ! Remontez sur le lama après ce délai pour dupliquer.");
                getLogger().info("Cooldown démarré pour " + player.getName());
            } else {
                getLogger().info("Aucun item trouvé dans le lama de " + player.getName() + " - pas de cooldown");
            }
        }
    }

    private void dupeLlamaInventory(Player player, Llama llama) {
        if (!dupeEnabled) return;
        
        ItemStack[] originalItems = llamaInventories.get(llama.getUniqueId());
        if (originalItems == null) {
            player.sendMessage("§c§lErreur : Aucun inventaire sauvegardé trouvé !");
            getLogger().warning("Aucun inventaire sauvegardé trouvé pour le lama " + llama.getUniqueId());
            return;
        }
        
        getLogger().info("Début de duplication - Inventaire sauvegardé contient " + originalItems.length + " slots");
        
        List<ItemStack> itemsToGive = new ArrayList<>();
        
        // Debug : vérifier le contenu de l'inventaire sauvegardé
        int savedItemCount = 0;
        for (int i = 0; i < originalItems.length; i++) {
            ItemStack item = originalItems[i];
            if (item != null && item.getType() != Material.AIR) {
                savedItemCount++;
                getLogger().info("Slot " + i + ": " + item.getType() + " x" + item.getAmount());
            }
        }
        getLogger().info("Items trouvés dans l'inventaire sauvegardé: " + savedItemCount);
        
        // Préparer tous les items à donner (SEULEMENT les items dupliqués)
        for (ItemStack item : originalItems) {
            if (item == null || item.getType() == Material.AIR) continue;
            
            // Ignorer les têtes de joueur
            if (isPlayerHead(item)) {
                getLogger().info("Tête de joueur ignorée: " + item.getType());
                continue;
            }
            
            getLogger().info("Duplication de l'item: " + item.getType() + " x" + item.getAmount());
            
            // Créer et ajouter SEULEMENT l'item dupliqué (pas l'original)
            if (item.getType() == Material.SHULKER_BOX) {
                // Pour les shulkers, on duplique la shulker (1→2) avec le même contenu
                // Première shulker (copie exacte)
                ItemStack firstShulker = item.clone();
                itemsToGive.add(firstShulker);
                
                // Deuxième shulker (copie exacte)
                ItemStack secondShulker = item.clone();
                itemsToGive.add(secondShulker);
                
                getLogger().info("Shulker box dupliquée (2 shulkers créées avec le même contenu)");
            } else {
                // Calculer la quantité totale après duplication
                int originalAmount = item.getAmount();
                int totalAmount = originalAmount * 2;
                int maxStackSize = item.getMaxStackSize();
                
                getLogger().info("Duplication: " + originalAmount + " -> " + totalAmount + " (max stack: " + maxStackSize + ")");
                
                // Si l'item est non-stackable (maxStackSize = 1) ou si la quantité dépasse la taille max
                if (maxStackSize == 1) {
                    // Items non-stackables : créer autant d'items individuels que nécessaire
                    for (int i = 0; i < totalAmount; i++) {
                        ItemStack singleItem = item.clone();
                        singleItem.setAmount(1);
                        itemsToGive.add(singleItem);
                    }
                    getLogger().info("Items non-stackables dupliqués: " + totalAmount + " items individuels créés");
                } else if (totalAmount > maxStackSize) {
                    // Items stackables qui dépassent la limite : créer plusieurs stacks
                    int remainingAmount = totalAmount;
                    while (remainingAmount > 0) {
                        ItemStack stack = item.clone();
                        int stackAmount = Math.min(remainingAmount, maxStackSize);
                        stack.setAmount(stackAmount);
                        itemsToGive.add(stack);
                        remainingAmount -= stackAmount;
                        getLogger().info("Stack créé: " + stack.getType() + " x" + stackAmount);
                    }
                } else {
                    // Quantité normale qui tient dans un seul stack
                    ItemStack duplicated = item.clone();
                    duplicated.setAmount(totalAmount);
                    itemsToGive.add(duplicated);
                    getLogger().info("Item dupliqué: " + duplicated.getType() + " x" + duplicated.getAmount());
                }
            }
        }
        
        getLogger().info("Total d'items à donner: " + itemsToGive.size());
        
        // Seulement procéder s'il y a des items à donner
        if (itemsToGive.isEmpty()) {
            player.sendMessage("§c§lAucun item à dupliquer trouvé !");
            getLogger().info("Aucun item à dupliquer pour " + player.getName());
            return;
        }
        
        // Donner les items au joueur
        giveItemsToPlayer(player, itemsToGive);
        
        // Vider l'inventaire du lama après la duplication
        llama.getInventory().clear();
        
        getLogger().info("Duplication terminée pour " + player.getName() + " - " + itemsToGive.size() + " items donnés");
    }

    private boolean isPlayerHead(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.getType() == Material.PLAYER_HEAD) return true;
        
        // Vérifier dans les shulkers
        if (item.getType() == Material.SHULKER_BOX) {
            try {
                BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                if (meta != null && meta.getBlockState() instanceof ShulkerBox shulker) {
                    for (ItemStack inside : shulker.getInventory().getContents()) {
                        if (isPlayerHead(inside)) return true;
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Erreur lors de la vérification des têtes dans la shulker box : " + e.getMessage());
            }
        }
        return false;
    }

    private ItemStack dupeShulkerBox(ItemStack shulker) {
        try {
            if (!(shulker.getItemMeta() instanceof BlockStateMeta meta)) return null;
            if (!(meta.getBlockState() instanceof ShulkerBox shulkerBox)) return null;
            
            // Créer une nouvelle shulker box
            ItemStack newShulkerItem = shulker.clone();
            BlockStateMeta newMeta = (BlockStateMeta) newShulkerItem.getItemMeta();
            ShulkerBox newShulkerBox = (ShulkerBox) newMeta.getBlockState();
            
            // Vider la nouvelle shulker box
            newShulkerBox.getInventory().clear();
            
            // Dupliquer le contenu (doubler la quantité, gérer les items non-stackables)
            for (ItemStack inside : shulkerBox.getInventory().getContents()) {
                if (inside == null || inside.getType() == Material.AIR) continue;
                if (isPlayerHead(inside)) continue;
                
                int originalAmount = inside.getAmount();
                int totalAmount = originalAmount * 2;
                int maxStackSize = inside.getMaxStackSize();
                
                if (maxStackSize == 1) {
                    // Items non-stackables : ajouter chaque item individuellement
                    for (int i = 0; i < totalAmount; i++) {
                        ItemStack singleItem = inside.clone();
                        singleItem.setAmount(1);
                        newShulkerBox.getInventory().addItem(singleItem);
                    }
                } else if (totalAmount > maxStackSize) {
                    // Items stackables qui dépassent la limite : créer plusieurs stacks
                    int remainingAmount = totalAmount;
                    while (remainingAmount > 0) {
                        ItemStack stack = inside.clone();
                        int stackAmount = Math.min(remainingAmount, maxStackSize);
                        stack.setAmount(stackAmount);
                        newShulkerBox.getInventory().addItem(stack);
                        remainingAmount -= stackAmount;
                    }
                } else {
                    // Quantité normale qui tient dans un seul stack
                    ItemStack duplicated = inside.clone();
                    duplicated.setAmount(totalAmount);
                    newShulkerBox.getInventory().addItem(duplicated);
                }
            }
            
            // Appliquer les changements
            newMeta.setBlockState(newShulkerBox);
            newShulkerItem.setItemMeta(newMeta);
            
            return newShulkerItem;
        } catch (Exception e) {
            getLogger().warning("Erreur lors de la duplication de la shulker box : " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Clone en profondeur le contenu d'un inventaire
     */
    private ItemStack[] cloneInventoryContents(ItemStack[] original) {
        if (original == null) return new ItemStack[0];
        
        ItemStack[] cloned = new ItemStack[original.length];
        for (int i = 0; i < original.length; i++) {
            if (original[i] != null) {
                cloned[i] = original[i].clone();
            }
        }
        return cloned;
    }
    
    /**
     * Donne une liste d'items au joueur, droppe au sol si l'inventaire est plein
     */
    private void giveItemsToPlayer(Player player, List<ItemStack> items) {
        if (items.isEmpty()) return;
        
        List<ItemStack> remainingItems = new ArrayList<>();
        int itemsGiven = 0;
        int itemsDropped = 0;
        
        // Essayer d'ajouter chaque item dans l'inventaire du joueur
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) continue;
            
            // Essayer d'ajouter l'item dans l'inventaire
            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
            
            if (leftOver.isEmpty()) {
                // L'item a été ajouté avec succès
                itemsGiven++;
            } else {
                // L'inventaire est plein, ajouter les items restants à la liste de drop
                remainingItems.addAll(leftOver.values());
            }
        }
        
        // Dropper les items qui n'ont pas pu être ajoutés
        if (!remainingItems.isEmpty()) {
            player.sendMessage("§e§lInventaire plein ! " + remainingItems.size() + " items droppés à vos pieds.");
            
            Location playerLocation = player.getLocation();
            World world = player.getWorld();
            
            for (ItemStack item : remainingItems) {
                dropItemSafely(world, playerLocation, item);
                itemsDropped++;
            }
        }
        
        // Messages de confirmation
        if (itemsGiven > 0) {
            player.sendMessage("§a§l" + itemsGiven + " items ajoutés à votre inventaire !");
        }
        if (itemsDropped > 0) {
            player.sendMessage("§e§l" + itemsDropped + " items droppés au sol !");
        }
        
        getLogger().info("Items donnés à " + player.getName() + " : " + itemsGiven + " dans l'inventaire, " + itemsDropped + " droppés");
    }
    
    /**
     * Droppe un item de manière sécurisée au sol
     */
    private void dropItemSafely(World world, Location location, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        
        try {
            // Ajouter un petit décalage aléatoire pour éviter que tous les items se superposent
            Location dropLoc = location.clone().add(
                (Math.random() - 0.5) * 1.5, // -0.75 à 0.75 sur X
                0.2, // Légèrement au-dessus du sol
                (Math.random() - 0.5) * 1.5  // -0.75 à 0.75 sur Z
            );
            
            Item droppedItem = world.dropItem(dropLoc, item);
            droppedItem.setPickupDelay(10); // 0.5 seconde avant de pouvoir être ramassé
        } catch (Exception e) {
            getLogger().warning("Erreur lors du drop d'un item : " + e.getMessage());
        }
    }
}

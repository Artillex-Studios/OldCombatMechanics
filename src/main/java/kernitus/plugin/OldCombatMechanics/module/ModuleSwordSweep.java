/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kernitus.plugin.OldCombatMechanics.module;

import kernitus.plugin.OldCombatMechanics.OCMMain;
import kernitus.plugin.OldCombatMechanics.utilities.damage.NewWeaponDamage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * A module to disable the sweep attack.
 */
public class ModuleSwordSweep extends OCMModule {

    private final List<Location> sweepLocations = new ArrayList<>();
    private EntityDamageEvent.DamageCause sweepDamageCause;
    private BukkitRunnable task;

    public ModuleSwordSweep(OCMMain plugin) {
        super(plugin, "disable-sword-sweep");

        try {
            // Will be available from some 1.11 version onwards
            sweepDamageCause = EntityDamageEvent.DamageCause.valueOf("ENTITY_SWEEP_ATTACK");
        } catch (IllegalArgumentException e) {
            sweepDamageCause = null;
        }

    }

    @Override
    public void reload() {
        // we didn't set anything up in the first place
        if (sweepDamageCause != null) return;

        if (task != null) task.cancel();

        task = new BukkitRunnable() {
            @Override
            public void run() {
                sweepLocations.clear();
            }
        };

        task.runTaskTimer(plugin, 0, 1);
    }

    //Changed from HIGHEST to LOWEST to support DamageIndicator plugin
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamaged(EntityDamageByEntityEvent e) {
        final Entity damager = e.getDamager();
        final World world = damager.getWorld();

        if (!isEnabled(world)) return;

        if (!(damager instanceof Player)) return;

        if (sweepDamageCause != null) {
            if (e.getCause() == sweepDamageCause) {
                e.setCancelled(true);
                debug("Sweep cancelled", damager);
            }
            // sweep attack detected or not, we do not need to fall back to the guessing implementation
            return;
        }

        final Player attacker = (Player) e.getDamager();
        final ItemStack weapon = attacker.getInventory().getItemInMainHand();

        if (isHoldingSword(weapon.getType()))
            onSwordAttack(e, attacker, weapon);
    }

    private void onSwordAttack(EntityDamageByEntityEvent e, Player attacker, ItemStack weapon) {
        //Disable sword sweep
        final Location attackerLocation = attacker.getLocation();

        int level = 0;

        try { //In a try catch for servers that haven't updated
            level = weapon.getEnchantmentLevel(Enchantment.SWEEPING_EDGE);
        } catch (NoSuchFieldError ignored) {
        }

        final float damage = NewWeaponDamage.getDamage(weapon.getType()) * level / (level + 1) + 1;

        if (e.getDamage() == damage) {
            // Possibly a sword-sweep attack
            if (sweepLocations.contains(attackerLocation)) {
                debug("Cancelling sweep...", attacker);
                e.setCancelled(true);
            }
        } else {
            sweepLocations.add(attackerLocation);
        }
    }

    private boolean isHoldingSword(Material mat) {
        return mat.toString().endsWith("_SWORD");
    }

}

package me.ebonjaeger.perworldinventory.serialization

import me.ebonjaeger.perworldinventory.data.PlayerProfile
import net.minidev.json.JSONArray
import net.minidev.json.JSONObject
import org.bukkit.GameMode
import org.bukkit.util.NumberConversions

object PlayerSerializer {

    fun deserialize(data: JSONObject, playerName: String, inventorySize: Int, eChestSize: Int): PlayerProfile {
        // Get the data format being used
        var format = 3
        if (data.containsKey("data-format")) {
            format = data["data-format"] as Int
        }

        val inventory = data["inventory"] as JSONObject
        val items = InventoryHelper.deserialize(
            (inventory["inventory"] ?: JSONArray()) as JSONArray,
            inventorySize,
            format
        )
        val armor = InventoryHelper.deserialize((inventory["armor"] ?: JSONArray()) as JSONArray, 4, format)
        val enderChest = InventoryHelper.deserialize(
            (data["ender-chest"] ?: JSONArray()) as JSONArray,
            eChestSize,
            format
        )
        val stats = StatSerializer.validateStats((data["stats"] ?: JSONObject()) as JSONObject, playerName)
        val potEff = if (stats.containsKey("potion-effects") && stats["potion-effects"] is JSONArray) {
            stats["potion-effects"] as JSONArray
        } else {
            JSONArray()
        }
        val potionEffects = PotionSerializer.deserialize(potEff)
        val balance = if (data.containsKey("economy")) {
            EconomySerializer.deserialize(data["economy"] as JSONObject)
        } else {
            0.0
        }

        return PlayerProfile(
            armor,
            enderChest,
            items,
            stats["can-fly"] as Boolean,
            stats["display-name"] as String,
            stats["exhaustion"] as Float,
            stats["exp"] as Float,
            stats["flying"] as Boolean,
            stats["food"] as Int,
            NumberConversions.toDouble(stats["max-health"]),
            NumberConversions.toDouble(stats["health"]),
            GameMode.valueOf(stats["gamemode"] as String),
            stats["level"] as Int,
            stats["saturation"] as Float,
            potionEffects,
            stats["fallDistance"] as Float,
            stats["fireTicks"] as Int,
            stats["maxAir"] as Int,
            stats["remainingAir"] as Int,
            balance
        )
    }
}

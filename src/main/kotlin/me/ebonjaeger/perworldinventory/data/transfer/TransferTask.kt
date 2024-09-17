package me.ebonjaeger.perworldinventory.data.transfer

import me.ebonjaeger.perworldinventory.ConsoleLogger
import me.ebonjaeger.perworldinventory.Group
import me.ebonjaeger.perworldinventory.data.DataSource
import me.ebonjaeger.perworldinventory.data.ProfileKey
import org.bukkit.GameMode
import org.bukkit.OfflinePlayer
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

const val ENDER_CHEST_SLOTS = 27
const val INVENTORY_SLOTS = 41
const val MAX_TRANSFERS_PER_TICK = 10

class TransferTask(
    private val transferService: TransferService,
    private val offlinePlayers: Array<out OfflinePlayer>,
    private val from: DataSource,
    private val to: DataSource,
    private val groups: Collection<Group>
) : BukkitRunnable() {

    private val transferQueue: Queue<OfflinePlayer> = LinkedList<OfflinePlayer>()

    private var index = 0
    private var transferredPlayers = 0
    private var transferred = 0
    private var failed = 0;

    override fun run() {
        // Calculate our stopping index for this run
        val stopIndex =
            if (index + MAX_TRANSFERS_PER_TICK < offlinePlayers.size) { // Use index + constant if the result isn't more than the total number of players
                index + MAX_TRANSFERS_PER_TICK
            } else { // Index would be greater than number of players, so just use the size of the array
                offlinePlayers.size
            }

        if (index >= offlinePlayers.size) { // No more players to transfer
            transferService.finishTransfer(transferred, transferredPlayers, failed)
            cancel()
        }

        // Add players to a queue to be transferred
        while (index < stopIndex) {
            transferQueue.offer(offlinePlayers[index])
            index++
        }

        while (transferQueue.isNotEmpty()) { // Iterate over the queue
            val player = transferQueue.poll()
            transfer(player)
        }

        if (index % 100 == 0) { // Print migration status every 100 players (about every 5 seconds)
            ConsoleLogger.info("Migration progress: $index/${offlinePlayers.size}")
        }
    }

    @Suppress("UNCHECKED_CAST") // Safe to assume our own Map types
    private fun transfer(player: OfflinePlayer) {
        val name = player.name

        if (!player.hasPlayedBefore() || name == null) { // It is likely that this player has never actually joined the server
            return
        }

        for (group in groups) { // Loop through all groups
            for (gameMode in GameMode.values()) { // Loop through all GameMode's
                if (gameMode == GameMode.SPECTATOR) { // Spectator mode doesn't have an inventory
                    continue
                }

                val key = ProfileKey(player.uniqueId, group, gameMode)
                val groupID = getGroupID(key)

                ConsoleLogger.info("[$index/${offlinePlayers.size}] Player $name (${key.uuid}) $groupID");

                try {

                    val dat = from.getPlayer(key, player, name, INVENTORY_SLOTS, ENDER_CHEST_SLOTS)
                    if (dat != null) {
                        to.savePlayer(key, dat);
                        transferred++;
                    }
                } catch (ex: Exception) {
                    failed++;
                    ConsoleLogger.severe("FAILED TO TRANSFER INV FOR: $name (${player.uniqueId}) - $groupID", ex)
                } catch (er: Error) {
                    failed++;
                    ConsoleLogger.severe("FAILED TO TRANSFER INV FOR: $name (${player.uniqueId}) - $groupID", er)
                }
            }
        }
        transferredPlayers++;
    }

    private fun getGroupID(key: ProfileKey): String {
        return when (key.gameMode) {
            GameMode.ADVENTURE -> key.group.name + "_adventure"
            GameMode.CREATIVE -> key.group.name + "_creative"
            GameMode.SPECTATOR -> key.group.name + "_spectator"
            GameMode.SURVIVAL -> key.group.name
        }
    }

}

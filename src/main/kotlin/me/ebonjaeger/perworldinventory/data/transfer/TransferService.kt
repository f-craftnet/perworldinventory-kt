package me.ebonjaeger.perworldinventory.data.transfer

import me.ebonjaeger.perworldinventory.ConsoleLogger
import me.ebonjaeger.perworldinventory.GroupManager
import me.ebonjaeger.perworldinventory.PerWorldInventory
import me.ebonjaeger.perworldinventory.data.FlatFile
import me.ebonjaeger.perworldinventory.data.MySQL
import me.ebonjaeger.perworldinventory.initialization.DataDirectory
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import java.io.File
import javax.inject.Inject

class TransferService @Inject constructor(
    private val groupManager: GroupManager,
    private val plugin: PerWorldInventory,
    @DataDirectory private val dataDirectory: File,
    private val flat: FlatFile,
    private val sql: MySQL
) {

    private var transferring = false
    private var sender: CommandSender? = null

    fun isTransferring(): Boolean {
        return transferring
    }

    fun beginTransfer(sender: CommandSender) {
        val offlinePlayers = Bukkit.getOfflinePlayers()

        sql.setSilent(true);

        if (transferring) {
            return
        }

        this.sender = sender

        if (sender !is ConsoleCommandSender) { // No need to send a message to console when console did the command
            ConsoleLogger.info("Beginning data migration to new format.")
        }

        transferring = true

        val task = TransferTask(this, offlinePlayers, flat, sql, groupManager.groups.values)
        task.runTaskTimerAsynchronously(plugin, 0, 20)
    }

    /**
     * Alerts that the migration completed.
     *
     * @param numTransferred The number of profiles transferred
     */
    fun finishTransfer(numTransferred: Int, playersTransfered: Int, failed: Int) {
        transferring = false
        ConsoleLogger.info("Data transfer has been completed! Transferred $numTransferred profiles.")
        if (sender != null && sender is Player) {
            if ((sender as Player).isOnline) {
                (sender as Player).sendMessage("${ChatColor.GREEN}» ${ChatColor.GRAY}Data Transfer has been completed!\nTransferred Players: $playersTransfered\nTransferred Profiles: $numTransferred\nFailed: $failed")
            }
        }
    }
}

package me.ebonjaeger.perworldinventory.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Description
import co.aikar.commands.annotation.Subcommand
import me.ebonjaeger.perworldinventory.data.transfer.TransferService
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import javax.inject.Inject

@CommandAlias("perworldinventory|pwi")
class TransferCommand @Inject constructor(private val transferService: TransferService) : BaseCommand() {

    @Subcommand("transfer")
    @CommandPermission("perworldinventory.command.transfer")
    @Description("Transfer flat data to the latest data format")
    fun onMigrate(sender: CommandSender) {
        if (transferService.isTransferring()) {
            sender.sendMessage("${ChatColor.DARK_RED}» ${ChatColor.GRAY}A data transfer is already in progress!")
            return
        }

        sender.sendMessage("${ChatColor.BLUE}» ${ChatColor.GRAY}Beginning data transfer to sql!")
        transferService.beginTransfer(sender)
    }
}

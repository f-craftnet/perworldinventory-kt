package me.ebonjaeger.perworldinventory.data

import com.dumptruckman.bukkit.configuration.util.SerializationHelper
import me.ebonjaeger.perworldinventory.ConsoleLogger
import me.ebonjaeger.perworldinventory.configuration.MySQLSettings
import me.ebonjaeger.perworldinventory.configuration.Settings
import me.ebonjaeger.perworldinventory.serialization.LocationSerializer
import me.ebonjaeger.perworldinventory.serialization.PlayerSerializer
import net.minidev.json.JSONObject
import net.minidev.json.JSONStyle
import net.minidev.json.parser.JSONParser
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import javax.inject.Inject

class MySQL @Inject constructor(private val settings: Settings) : DataSource {

    private val hostname: String = settings.getProperty(MySQLSettings.HOSTNAME);
    private val database: String = settings.getProperty(MySQLSettings.DATABASE);
    private val username: String = settings.getProperty(MySQLSettings.USERNAME);
    private val password: String = settings.getProperty(MySQLSettings.PASSWORD);
    private val table: String = settings.getProperty(MySQLSettings.TABLE);
    private val port: Int = settings.getProperty(MySQLSettings.PORT);
    private val rowsToKeep: Int = settings.getProperty(MySQLSettings.ROWS_TO_KEEP);

    private var connection: Connection? = null;

    private var silent: Boolean = false;

    private fun openConnection(): Connection {
        if (checkConnection()) {
            return connection!!;
        }
        var connectionURL = "jdbc:mysql://$hostname:$port"
        connectionURL = "$connectionURL/$database"
        Class.forName("com.mysql.jdbc.Driver")
        val props = Properties()
        props.setProperty("user", username)
        props.setProperty("password", password)
        props.setProperty("autoReconnect", "true")
        connection = DriverManager.getConnection(connectionURL, props)
        if (connection == null) {
            ConsoleLogger.severe("Filed to connect to DB")
            throw Exception("Failed to connect to DB");
//            log.warning("Failed to open DB Connection")
        } else {
            ConsoleLogger.fine("Connected to DB")
        }
        return connection!!
    }

    public fun setSilent(sil: Boolean) {
        silent = sil
    }


    @Throws(SQLException::class)
    private fun checkConnection(): Boolean {
        return (connection != null && !connection!!.isClosed)
    }

    @Throws(SQLException::class)
    fun closeConnection(): Boolean {
        if (connection == null) return false
        else connection!!.close()
        return true
    }

    override fun savePlayer(key: ProfileKey, player: PlayerProfile) {
        val map = SerializationHelper.serialize(player)
        val json = JSONObject(map as Map<String, *>)


        val insert = "INSERT INTO `$table` (`playerUUID`,`group`,`type`,`data`) VALUES (?, ?, 'inv', ?)";
        val delete =
            "DELETE FROM `$table` WHERE `id` <= (SELECT `id` FROM `$table` WHERE `playerUUID` = ? AND `group` = ? AND `type` = 'inv' ORDER BY `id` DESC LIMIT 1 OFFSET $rowsToKeep) AND `playerUUID` = ? AND `group` = ? AND `type` = 'inv'";
        val groupID = getGroupID(key)

        try {
            val con = openConnection();
            val stmt = con.prepareStatement(insert);

            stmt.setString(1, key.uuid.toString());
            stmt.setString(2, groupID);
            stmt.setString(3, json.toJSONString(JSONStyle.LT_COMPRESS));

            val rows = stmt.executeUpdate();
            if (rows == 1) {

                val stmt = con.prepareStatement(delete);
                stmt.setString(1, key.uuid.toString());
                stmt.setString(2, groupID);
                stmt.setString(3, key.uuid.toString());
                stmt.setString(4, groupID);

                val del = stmt.executeUpdate()

                if (!silent) ConsoleLogger.fine("Saved inventory for : ${player.displayName} and deleted $del rows")
            } else {
                throw Exception("Failed to save inventory for ${player.displayName} on $groupID? No Rows updated");
            }


        } catch (ex: SQLException) {
            ConsoleLogger.severe("Could not write inventory data to DB:", ex)
        } catch (ex: Exception) {
            ConsoleLogger.severe("Failed to write inventory data to DB:", ex)
        }
    }

    override fun saveLogout(player: OfflinePlayer, location: Location) {
        val type = "last-logout";

        val insert = "INSERT INTO `$table` (`playerUUID`,`type`,`data`) VALUES (?, '$type', ?)"
        val delete =
            "DELETE FROM `$table` WHERE `id` <= (SELECT `id` FROM `$table` WHERE `playerUUID` = ? AND `type` = '$type' ORDER BY `id` DESC LIMIT 1 OFFSET $rowsToKeep) AND `playerUUID` = ? AND `type` = '$type'";

        try {
            val con = openConnection();
            val stmt = con.prepareStatement(insert);
            val json = LocationSerializer.serialize(location)

            stmt.setString(1, player.uniqueId.toString());
            stmt.setString(2, json.toJSONString(JSONStyle.LT_COMPRESS));

            val rows = stmt.executeUpdate();
            if (rows == 1) {

                val stmt = con.prepareStatement(delete);
                stmt.setString(1, player.uniqueId.toString());
                stmt.setString(2, player.uniqueId.toString());
                val del = stmt.executeUpdate()

                if (!silent) ConsoleLogger.fine("Saved logout location for : ${player.name} and deleted $del rows")
            } else {
                throw Exception("Failed to save logout location? No Rows updated");
            }

        } catch (ex: SQLException) {
            ConsoleLogger.severe("Could not write logout data to DB:", ex)
        } catch (ex: Exception) {
            ConsoleLogger.severe("Failed to write logout data to DB:", ex)
        }
    }

    override fun saveLogout(player: Player) {
        saveLogout(player, player.location)
    }

    override fun saveLocation(player: OfflinePlayer, location: Location) {
        throw NotImplementedError("TODO MYSQL SAVE LOCATION")
    }

    override fun getPlayer(key: ProfileKey, player: Player): PlayerProfile? {
        return getPlayer(key, player, player.name, player.inventory.size, player.enderChest.size)
    }

    override fun getPlayer(
        key: ProfileKey,
        player: OfflinePlayer,
        name: String,
        inventorySize: Int,
        enderSize: Int
    ): PlayerProfile? {
        val groupID = getGroupID(key)

        val select =
            "SELECT `data` FROM `$table` WHERE `playerUUID` = ? AND `group` = ? and `type` = 'inv' ORDER BY id desc LIMIT 1"

        try {
            val con = openConnection();
            val stmt = con.prepareStatement(select);

            stmt.setString(1, key.uuid.toString());
            stmt.setString(2, groupID);

            val result = stmt.executeQuery();

            if (result.next()) {
                val raw = result.getString("data");
                val parser = JSONParser(JSONParser.USE_INTEGER_STORAGE)
                val data = parser.parse(raw) as JSONObject

                return if (data.containsKey("==")) { // Data is from ConfigurationSerialization
                    SerializationHelper.deserialize(data) as PlayerProfile
                } else { // Old data format and methods
                    PlayerSerializer.deserialize(data, name, inventorySize, enderSize)
                }
            }

        } catch (ex: SQLException) {
            ConsoleLogger.severe("Could not read inventory data to DB:", ex)
        } catch (ex: Exception) {
            ConsoleLogger.severe("Failed to read inventory data to DB:", ex)
        }

        return null
    }

    override fun getLogout(player: OfflinePlayer): Location? {

        val select = "SELECT `data` FROM `$table` WHERE `playerUUID` = ? and `type` = 'logout' ORDER BY id desc LIMIT 1"

        try {
            val con = openConnection();
            val stmt = con.prepareStatement(select);

            stmt.setString(1, player.uniqueId.toString());

            val result = stmt.executeQuery();

            if (result.next()) {
                val raw = result.getString("data");

                val parser = JSONParser(JSONParser.USE_INTEGER_STORAGE)
                val data = parser.parse(raw) as JSONObject

                return LocationSerializer.deserialize(data)
            }

        } catch (ex: SQLException) {
            ConsoleLogger.severe("Could not read inventory data to DB:", ex)
        } catch (ex: Exception) {
            ConsoleLogger.severe("Failed to read inventory data to DB:", ex)
        }

        return null
    }

    override fun getLocation(player: OfflinePlayer, world: String): Location? {
        throw NotImplementedError("TODO MYSQL GET LOCATION")
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
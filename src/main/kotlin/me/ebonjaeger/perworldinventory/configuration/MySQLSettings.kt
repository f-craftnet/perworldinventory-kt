package me.ebonjaeger.perworldinventory.configuration

import ch.jalu.configme.Comment
import ch.jalu.configme.SettingsHolder
import ch.jalu.configme.properties.Property
import ch.jalu.configme.properties.PropertyInitializer.newProperty

object MySQLSettings : SettingsHolder {

    @JvmField
    @Comment(
        "Is MySQL Support enabled",
    )
    val ENABLED: Property<Boolean> = newProperty("mysql.enabled", false)

    @JvmField
    @Comment(
        "Hostname for MySQL Driver"
    )
    val HOSTNAME: Property<String> = newProperty("mysql.hostname", "localhost")

    @JvmField
    @Comment(
        "Database for MySQL Driver"
    )
    val DATABASE: Property<String> = newProperty("mysql.database", "mcserverdata")

    @JvmField
    @Comment(
        "Username for MySQL Driver"
    )
    val USERNAME: Property<String> = newProperty("mysql.username", "")

    @JvmField
    @Comment(
        "Password for MySQL Driver"
    )
    val PASSWORD: Property<String> = newProperty("mysql.password", "")

    @JvmField
    @Comment(
        "TABLE for MySQL Driver"
    )
    val TABLE: Property<String> = newProperty("mysql.table", "player_inventories")

    @JvmField
    @Comment(
        "Port for MySQL Driver"
    )
    val PORT: Property<Int> = newProperty("mysql.port", 3306)


    @JvmField
    @Comment(
        "Number of rows to keep"
    )
    val ROWS_TO_KEEP: Property<Int> = newProperty("mysql.rows-to-keep", 10)

}
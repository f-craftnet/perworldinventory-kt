package me.ebonjaeger.perworldinventory.data

import ch.jalu.injector.factory.SingletonStore
import me.ebonjaeger.perworldinventory.ConsoleLogger
import me.ebonjaeger.perworldinventory.configuration.MySQLSettings
import me.ebonjaeger.perworldinventory.configuration.Settings
import javax.inject.Inject
import javax.inject.Provider

class DataSourceProvider @Inject constructor(
    private val dataSourceStore: SingletonStore<DataSource>,
    private val settings: Settings
) : Provider<DataSource> {

    override fun get(): DataSource
    {
        try
        {
            return createDataSource()
        } catch (ex: Exception)
        {
            ConsoleLogger.severe("Unable to create data source:", ex)
            throw IllegalStateException("Error during initialization of data source", ex)
        }
    }

    private fun createDataSource(): DataSource
    {
        // Later on we will have logic here to differentiate between flatfile and MySQL.
        val mysql = settings.getProperty(MySQLSettings.ENABLED);
        //if(mysql) ConsoleLogger.info("Loading MYSQL+FLAT DataSource")
        return if (mysql) {
            dataSourceStore.getSingleton(MySQL::class.java)
        } else {
            dataSourceStore.getSingleton(FlatFile::class.java)
        }
    }
}

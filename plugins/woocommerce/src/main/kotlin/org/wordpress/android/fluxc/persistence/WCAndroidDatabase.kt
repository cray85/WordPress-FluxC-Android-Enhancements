package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.entities.LineItemEntity
import org.wordpress.android.fluxc.persistence.converters.BigDecimalConverter
import org.wordpress.android.fluxc.persistence.converters.LocalIdConverter
import org.wordpress.android.fluxc.persistence.converters.LongListConverter
import org.wordpress.android.fluxc.persistence.converters.RemoteIdConverter
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.AddonOptionEntity
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupEntity
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_3_4
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_4_5
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_5_6
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_6_7

@Database(
        version = 7,
        entities = [
            AddonEntity::class,
            AddonOptionEntity::class,
            GlobalAddonGroupEntity::class,
            LineItemEntity::class,
            WCOrderModel::class,
        ]
)
@TypeConverters(
        value = [
            LocalIdConverter::class,
            LongListConverter::class,
            RemoteIdConverter::class,
            BigDecimalConverter::class
        ]
)
abstract class WCAndroidDatabase : RoomDatabase() {
    internal abstract fun addonsDao(): AddonsDao
    abstract fun ordersDao(): OrdersDao

    companion object {
        fun buildDb(applicationContext: Context) = Room.databaseBuilder(
                applicationContext,
                WCAndroidDatabase::class.java,
                "wc-android-database"
        ).allowMainThreadQueries()
                .fallbackToDestructiveMigrationOnDowngrade()
                .fallbackToDestructiveMigrationFrom(1, 2)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .build()
    }
}

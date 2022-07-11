package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

/**
 * The OrderMetaDataEntity table is used to store viewable order metadata. Order metadata
 * can potentially be quite large, so we keep it separate from the order.
 */
@Entity(
    tableName = "OrderMetaData",
    indices = [Index(
        value = ["localSiteId", "orderId"]
    )],
    primaryKeys = ["localSiteId", "orderId", "id"]
)
data class OrderMetaDataEntity(
    @ColumnInfo(name = "localSiteId")
    val localSiteId: LocalId,
    val id: Long,
    val orderId: Long,
    val key: String,
    val value: String,
    val displayKey: String?,
    val displayValue: String?
)
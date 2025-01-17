package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

data class InboxNoteWithActions(
    @Embedded val inboxNote: InboxNoteEntity,
    @Relation(
        parentColumn = "localId",
        entityColumn = "inboxNoteLocalId"
    )
    val noteActions: List<InboxNoteActionEntity>
)

@Entity(
    tableName = "InboxNotes",
    indices = [Index(
        value = ["remoteId", "siteId"],
        unique = true
    )]
)
data class InboxNoteEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val remoteId: Long,
    val siteId: Long,
    val name: String,
    val title: String,
    val content: String,
    val dateCreated: String,
    val status: LocalInboxNoteStatus,
    val source: String?,
    val type: String?,
    val dateReminder: String?
) {
    enum class LocalInboxNoteStatus {
        Unactioned,
        Actioned,
        Snoozed,
        Unknown
    }
}

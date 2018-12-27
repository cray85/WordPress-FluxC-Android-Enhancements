package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_notifications.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.NotificationAction.FETCH_NOTIFICATION
import org.wordpress.android.fluxc.action.NotificationAction.FETCH_NOTIFICATIONS
import org.wordpress.android.fluxc.action.NotificationAction.MARK_NOTIFICATIONS_SEEN
import org.wordpress.android.fluxc.action.NotificationAction.MARK_NOTIFICATIONS_READ
import org.wordpress.android.fluxc.action.NotificationAction.UPDATE_NOTIFICATION
import org.wordpress.android.fluxc.example.NotificationTypeSubtypeDialog.Listener
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NotificationModel.Subkind
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationPayload
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationsPayload
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsReadPayload
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsSeenPayload
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

class NotificationsFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var notificationStore: NotificationStore
    @Inject internal lateinit var siteStore: SiteStore

    private var typeSelectionDialog: NotificationTypeSubtypeDialog? = null
    private var selectedSite: SiteModel? = null
    private var selectedPos: Int = -1

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_notifications, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notifs_fetch_all.setOnClickListener {
            prependToLog("Fetching all notifications from the api...\n")
            dispatcher.dispatch(NotificationActionBuilder.newFetchNotificationsAction(FetchNotificationsPayload()))
        }

        notifs_fetch_for_site.setOnClickListener {
            prependToLog("Getting all notifications for the first site...\n")
            selectedSite?.let { site ->
                val notifs = notificationStore.getNotificationsForSite(site)
                prependToLog("SUCCESS! ${notifs.size} pulled from the database for ${site.name}")
            } ?: prependToLog("No site selected!")
        }

        notifs_by_type_subtype.setOnClickListener {
            showNotificationTypeSubtypeDialog(object : Listener {
                override fun onSubmitted(type: String, subtype: String) {
                    prependToLog("Fetching notifications matching $type or $subtype...\n")
                    val notifs = notificationStore.getNotifications(listOf(type), listOf(subtype))
                    val groups = notifs.groupingBy { notif ->
                        notif.subtype?.name?.takeIf { subtype -> subtype != Subkind.UNKNOWN.name } ?: notif.type.name
                    }.fold(0) { acc, _ -> acc + 1 }
                    prependToLog("SUCCESS! Total records matching filtered selections:" +
                            "\n- $type: ${groups[type] ?: 0}\n- $subtype: ${groups[subtype] ?: 0}")
                }
            })
        }

        notifs_mark_seen.setOnClickListener {
            prependToLog("Setting notifications last seen time to now\n")
            dispatcher.dispatch(NotificationActionBuilder
                    .newMarkNotificationsSeenAction(MarkNotificationsSeenPayload(Date().time)))
        }

        notifs_fetch_first.setOnClickListener {
            selectedSite?.let {
                val note = notificationStore.getNotificationsForSite(it).first()
                prependToLog("Fetching a single notification with remoteNoteId = ${note.remoteNoteId}\n")
                dispatcher.dispatch(NotificationActionBuilder
                        .newFetchNotificationAction(FetchNotificationPayload(note.remoteNoteId)))
            } ?: prependToLog("No site selected!")
        }

        notifs_mark_read.setOnClickListener {
            selectedSite?.let {
                val note = notificationStore.getNotificationsForSite(it).first()
                prependToLog("Setting notification with remoteNoteId of ${note.remoteNoteId} as read\n")
                dispatcher.dispatch(NotificationActionBuilder
                        .newMarkNotificationsReadAction(MarkNotificationsReadPayload(listOf(note))))
            } ?: prependToLog("No site selected!")
        }

        notifs_mark_all_read.setOnClickListener {
            // Fetch only unread notifications from the database for the first site
            selectedSite?.let { site ->
                notificationStore.getNotificationsForSite(site).filter { note -> !note.read }
                        .takeIf { list -> list.isNotEmpty() }?.let { notes ->
                            prependToLog("Marking [${notes.size}] unread notifications as read...\n")
                            dispatcher.dispatch(NotificationActionBuilder
                                    .newMarkNotificationsReadAction(MarkNotificationsReadPayload(notes)))
                        } ?: prependToLog("No unread notifications found!\n")
            } ?: prependToLog("No site selected!")
        }

        notifs_update_first.setOnClickListener {
            selectedSite?.let {
                val note = notificationStore.getNotificationsForSite(it).first()
                note.read = !note.read
                prependToLog("Updating notification with remoteNoteId " +
                        "of ${note.remoteNoteId} to [read = ${note.read}]\n")
                dispatcher.dispatch(NotificationActionBuilder.newUpdateNotificationAction(note))
            } ?: prependToLog("No site selected!")
        }

        notifs_select_site.setOnClickListener {
            showSiteSelectorDialog(selectedPos, object : SiteSelectorDialog.Listener {
                override fun onSiteSelected(site: SiteModel, pos: Int) {
                    selectedSite = site
                    selectedPos = pos
                    toggleSiteDependentButtons(true)
                    notif_selected_site.text = site.name ?: site.displayName
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
        typeSelectionDialog?.dismiss()
        typeSelectionDialog = null
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        if (event.isError) {
            prependToLog("Error from ${event.causeOfChange} - error: ${event.error.type}")
            return
        }

        when (event.causeOfChange) {
            FETCH_NOTIFICATIONS -> {
                val notifs = notificationStore.getNotifications()
                prependToLog("SUCCESS! - Fetched ${notifs.size} notifications from the API")
            }
            FETCH_NOTIFICATION -> {
                val localNoteId = event.changedNotificationLocalIds[0]
                notificationStore.getNotificationByLocalId(localNoteId)?.let {
                    prependToLog("SUCCESS! ${it.toLogString()}")
                } ?: prependToLog("Error! Notification not found in db!")
            }
            UPDATE_NOTIFICATION -> {
                val localNoteId = event.changedNotificationLocalIds[0]
                notificationStore.getNotificationByLocalId(localNoteId)?.let {
                    prependToLog("SUCCESS! ${it.toLogString()}")
                }
            }
            MARK_NOTIFICATIONS_READ -> {
                event.changedNotificationLocalIds.forEach {
                    notificationStore.getNotificationByLocalId(it)?.let { notif ->
                        prependToLog("SUCCESS! ${notif.toLogString()}")
                    }
                }
            }
            MARK_NOTIFICATIONS_SEEN -> {
                val lastSeenDate = event.lastSeenTime?.let {
                    DateTimeUtils.iso8601FromTimestamp(it)
                } ?: ""
                prependToLog("SUCCESS! Last seen set to $lastSeenDate")
            }
            else -> {}
        }
    }

    private fun showNotificationTypeSubtypeDialog(listener: NotificationTypeSubtypeDialog.Listener) {
        fragmentManager?.let { fm ->
            val dialog = NotificationTypeSubtypeDialog.newInstance(listener)
            dialog.show(fm, "NotificationFragment")
        }
    }

    private fun showSiteSelectorDialog(selectedPos: Int, listener: SiteSelectorDialog.Listener) {
        fragmentManager?.let { fm ->
            val dialog = SiteSelectorDialog.newInstance(listener, selectedPos)
            dialog.show(fm, "SiteSelectorDialog")
        }
    }

    private fun toggleSiteDependentButtons(enabled: Boolean) {
        notifs_fetch_for_site.isEnabled = enabled
        notifs_mark_all_read.isEnabled = enabled
        notifs_fetch_first.isEnabled = enabled
        notifs_mark_read.isEnabled = enabled
        notifs_update_first.isEnabled = enabled
    }
}

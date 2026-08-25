package io.github.meko123456.kharji.data.sms

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.meko123456.kharji.data.Entry
import io.github.meko123456.kharji.data.KharjiDatabase
import io.github.meko123456.kharji.domain.sms.BankSmsParser
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Opt-in capture of bank transaction notifications.
 *
 * The user must explicitly grant notification access in system settings — nothing is
 * captured otherwise. Text is parsed by the pure [BankSmsParser] and stored as a
 * **pending** entry for the user to confirm; nothing is ever sent off the device.
 */
class BankNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (text.isBlank()) return

        // The sender can appear as the notification title or the posting app's name.
        val sender = listOf(title, sbn.packageName).firstOrNull { candidate ->
            BankSmsParser.all.any { it.matchesSender(candidate) }
        } ?: return

        val tx = BankSmsParser.parse(sender, "$title $text") ?: return

        scope.launch {
            val dao = KharjiDatabase.get(applicationContext).dao()
            dao.insert(
                Entry(
                    amountMinor = tx.amountMinor,
                    currency = tx.currency.code,
                    merchant = tx.merchant,
                    epochDay = LocalDate.now().toEpochDay(),
                    createdAtMillis = System.currentTimeMillis(),
                    source = tx.source,
                    pending = true,
                ),
            )
        }
    }
}

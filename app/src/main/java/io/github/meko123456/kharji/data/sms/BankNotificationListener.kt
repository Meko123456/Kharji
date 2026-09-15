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

        // Two ways a bank message arrives, and they need different strictness. The bank's own app
        // is identified by its package name, which is exact because any app can choose a package
        // id containing "bog". An SMS forwarded by the messaging app is identified by the title,
        // which carries the sender id and is matched on word boundaries rather than as a substring.
        val parser = BankSmsParser.forPackage(sbn.packageName)
            ?: BankSmsParser.forSender(title)
            ?: return

        val tx = parser.parse("$title $text") ?: return

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

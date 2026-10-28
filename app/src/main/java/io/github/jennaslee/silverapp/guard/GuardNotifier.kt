package io.github.jennaslee.silverapp.guard

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.jennaslee.silverapp.MainActivity
import io.github.jennaslee.silverapp.R

/** 어르신께 보여주는 경고 알림 */
object GuardNotifier {
    private const val CHANNEL_ID = "guard_alerts"
    const val EXTRA_RECORD_ID = "record_id"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "위험한 문자 알림", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "사기로 보이는 문자나 카톡이 오면 알려드려요"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notify(context: Context, record: GuardRecord) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)

        val title = when (record.action) {
            GuardRecord.Action.HIDDEN -> "사기 문자를 치웠어요"
            GuardRecord.Action.WARNED -> "조심하세요, 의심스러운 문자예요"
        }
        val open = PendingIntent.getActivity(
            context,
            record.id.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_RECORD_ID, record.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(record.reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(record.reason))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(record.id.hashCode(), notification)
    }
}

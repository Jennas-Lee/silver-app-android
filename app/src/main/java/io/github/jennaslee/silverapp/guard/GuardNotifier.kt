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
import io.github.jennaslee.silverapp.link.LinkRecord

/** 어르신께 보여주는 경고 알림 */
object GuardNotifier {
    private const val CHANNEL_ID = "guard_alerts"
    const val EXTRA_RECORD_ID = "record_id"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "위험 알림", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "사기로 보이는 문자나 카톡이 오거나, 위험한 주소에 들어가려 하면 알려드려요"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** 링크 지킴이가 막거나 경고한 주소 알림. 누르면 실버 도우미 첫 화면이 열린다 */
    fun notifyLink(context: Context, record: LinkRecord) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val title = when (record.action) {
            LinkRecord.Action.BLOCKED -> "위험한 곳이라 막았어요"
            LinkRecord.Action.WARNED -> "조심하세요, 의심스러운 주소예요"
        }
        val open = PendingIntent.getActivity(
            context,
            record.id.hashCode(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(record.reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${record.reason}\n주소: ${record.host}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(record.id.hashCode(), notification)
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

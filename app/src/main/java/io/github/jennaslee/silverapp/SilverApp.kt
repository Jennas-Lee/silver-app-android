package io.github.jennaslee.silverapp

import android.app.Application
import io.github.jennaslee.silverapp.guard.GuardStore
import io.github.jennaslee.silverapp.guard.RecordSync
import io.github.jennaslee.silverapp.link.LinkStore
import io.github.jennaslee.silverapp.session.SessionStore

class SilverApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 문자 수신·알림 감시처럼 화면 없이 깨어나는 경우에도 저장소가 준비되도록 여기서 연다
        SessionStore.init(this)
        GuardStore.init(this)
        LinkStore.init(this)
        RecordSync.syncSoon()
    }
}

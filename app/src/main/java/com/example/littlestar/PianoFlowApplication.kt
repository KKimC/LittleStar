package com.example.littlestar

import android.app.Application
import com.example.littlestar.di.AppContainer

/**
 * 앱 전역 상태(의존성 컨테이너)를 보관한다.
 * 프로세스 시작 시 한 번만 실행되므로, 여기서 RUNNING 세션 복구도 처리한다.
 */
class PianoFlowApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.recoverRunningSessions()
    }
}

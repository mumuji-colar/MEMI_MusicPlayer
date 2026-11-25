package com.mmc.merimusicplayer.ui.debug

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.mmc.merimusicplayer.core.crash.ExceptionHandler
import com.mmc.merimusicplayer.ui.screen.debug.DebugCrashTestType
import com.mmc.merimusicplayer.util.crash.AnrWatchdog
import com.mmc.merimusicplayer.util.crash.NativeCrashHandler

internal class AppDebugCrashActionOwner(
    private val crashMessage: () -> String,
    private val actions: Map<DebugCrashTestType, (String) -> Unit>
) {
    val supportedTypes: Set<DebugCrashTestType> get() = actions.keys

    fun dispatch(type: DebugCrashTestType) {
        actions.getValue(type).invoke(crashMessage())
    }
}

internal fun appDebugCrashActionOwner(
    context: Context,
    crashMessage: () -> String
): AppDebugCrashActionOwner = AppDebugCrashActionOwner(
    crashMessage,
    mapOf(
        DebugCrashTestType.JvmHandled to { message ->
            ExceptionHandler.safeExecute("DebugTestHandled") {
                throw RuntimeException(message)
            }
        },
        DebugCrashTestType.JvmUncaughtMain to { message ->
            Handler(Looper.getMainLooper()).post {
                throw RuntimeException(message)
            }
        },
        DebugCrashTestType.JvmUncaughtWorker to { message ->
            Thread { throw RuntimeException(message) }.start()
        },
        DebugCrashTestType.MainThreadAnr to { _ ->
            AnrWatchdog.triggerTestAnr(context)
        },
        DebugCrashTestType.NativeSigSegv to { _ ->
            Handler(Looper.getMainLooper()).post {
                NativeCrashHandler.triggerTestCrash(
                    context = context,
                    crashType = NativeCrashHandler.TestCrashType.SigSegv
                )
            }
        },
        DebugCrashTestType.NativeSigAbrt to { _ ->
            Handler(Looper.getMainLooper()).post {
                NativeCrashHandler.triggerTestCrash(
                    context = context,
                    crashType = NativeCrashHandler.TestCrashType.SigAbrt
                )
            }
        }
    )
)

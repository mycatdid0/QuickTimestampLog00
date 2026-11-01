package kr.mycatdid0.quicktimestamplog00

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kr.mycatdid0.quicktimestamplog00.ui.CalendarApp
import kr.mycatdid0.quicktimestamplog00.ui.theme.QuickTimestampLog00Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startWithQuickEntry = intent?.action == ACTION_OPEN_QUICK_ENTRY
        setContent {
            QuickTimestampLog00Theme {
                CalendarApp(startWithQuickEntry = startWithQuickEntry, onFinish = ::finish)
            }
        }
    }

    companion object {
        const val ACTION_OPEN_QUICK_ENTRY = "kr.mycatdid0.quicktimestamplog00.OPEN_QUICK_ENTRY"
    }
}
package kr.mycatdid0.quicktimestamplog00.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.mycatdid0.quicktimestamplog00.data.AppPreferences
import kr.mycatdid0.quicktimestamplog00.data.CalendarOption
import kr.mycatdid0.quicktimestamplog00.data.CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarApp(startWithQuickEntry: Boolean, onFinish: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val screenStateHolder = rememberSaveableStateHolder()
    var showQuickEntry by rememberSaveable { mutableStateOf(startWithQuickEntry) }
    var quickEntryOpenedFromSettings by rememberSaveable { mutableStateOf(false) }
    var settingsHasQuickDraft by rememberSaveable { mutableStateOf(false) }
    var permissionRefresh by remember { mutableStateOf(0) }
    var hasAllCalendarPermissions by remember {
        mutableStateOf(CalendarRepository.hasCalendarPermissions(context))
    }
    var calendars by remember { mutableStateOf(emptyList<CalendarOption>()) }
    var defaultCalendarId by remember {
        mutableLongStateOf(preferences.getDefaultCalendarId() ?: NO_CALENDAR_ID)
    }
    var recentTitles by remember { mutableStateOf(preferences.getRecentTitles()) }
    var loadingCalendars by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasAllCalendarPermissions = CalendarRepository.hasCalendarPermissions(context)
        permissionRefresh = if (permissionRefresh == Int.MAX_VALUE) 0 else permissionRefresh + 1
        if (!hasAllCalendarPermissions) {
            Toast.makeText(context, "캘린더 권한이 있어야 캘린더를 조회하고 일정을 저장할 수 있습니다.", Toast.LENGTH_LONG).show()
        }
    }

    fun requestCalendarPermissions() {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        )
    }

    LaunchedEffect(hasAllCalendarPermissions, permissionRefresh) {
        hasAllCalendarPermissions = CalendarRepository.hasCalendarPermissions(context)
        if (CalendarRepository.hasReadPermission(context)) {
            loadingCalendars = true
            calendars = withContext(Dispatchers.IO) {
                runCatching { CalendarRepository.loadWritableCalendars(context) }
                    .getOrDefault(emptyList())
            }
            loadingCalendars = false
        } else {
            calendars = emptyList()
        }
    }

    LaunchedEffect(showQuickEntry, hasAllCalendarPermissions) {
        if (showQuickEntry && !hasAllCalendarPermissions) {
            requestCalendarPermissions()
        }
    }

    screenStateHolder.SaveableStateProvider(if (showQuickEntry) "quick-entry" else "settings") {
        if (showQuickEntry) {
            QuickEntryScreen(
            calendars = calendars,
            defaultCalendarId = defaultCalendarId.takeIf { it != NO_CALENDAR_ID },
            recentTitles = recentTitles,
            hasPermissions = hasAllCalendarPermissions,
            loadingCalendars = loadingCalendars,
            onRequestPermissions = ::requestCalendarPermissions,
            onSettings = {
                settingsHasQuickDraft = true
                showQuickEntry = false
            },
            onCancel = {
                if (quickEntryOpenedFromSettings) {
                    showQuickEntry = false
                } else {
                    onFinish()
                }
            },
            onDeleteRecentTitle = { title ->
                preferences.removeRecentTitle(title)
                recentTitles = preferences.getRecentTitles()
            },
            onSave = { title, calendarId, start, end, onResult ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            CalendarRepository.insertEvent(
                                context = context,
                                calendarId = calendarId,
                                title = title,
                                startMillis = start,
                                endMillis = end
                            )
                        }
                        preferences.setDefaultCalendarId(calendarId)
                        defaultCalendarId = calendarId
                        preferences.addRecentTitle(title)
                        recentTitles = preferences.getRecentTitles()
                        onResult(true)
                        Toast.makeText(context, "캘린더에 기록했습니다.", Toast.LENGTH_SHORT).show()
                        if (quickEntryOpenedFromSettings) {
                            showQuickEntry = false
                        } else {
                            onFinish()
                        }
                    } catch (exception: Exception) {
                        onResult(false)
                        Toast.makeText(
                            context,
                            "일정을 저장하지 못했습니다: ${exception.localizedMessage ?: "오류"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            )
        } else {
            SettingsScreen(
            calendars = calendars,
            defaultCalendarId = defaultCalendarId.takeIf { it != NO_CALENDAR_ID },
            recentTitles = recentTitles,
            hasReadPermission = CalendarRepository.hasReadPermission(context),
            hasWritePermission = CalendarRepository.hasWritePermission(context),
            loadingCalendars = loadingCalendars,
            onRequestPermissions = ::requestCalendarPermissions,
            onDefaultCalendarSelected = { calendarId ->
                defaultCalendarId = calendarId
                preferences.setDefaultCalendarId(calendarId)
            },
            onDeleteRecentTitle = { title ->
                preferences.removeRecentTitle(title)
                recentTitles = preferences.getRecentTitles()
            },
            onClearRecentTitles = {
                preferences.clearRecentTitles()
                recentTitles = emptyList()
            },
                resumeQuickEntry = settingsHasQuickDraft,
                onResumeQuickEntry = {
                    quickEntryOpenedFromSettings = true
                    settingsHasQuickDraft = false
                    showQuickEntry = true
                },
                onOpenQuickEntry = {
                    quickEntryOpenedFromSettings = true
                    settingsHasQuickDraft = false
                    showQuickEntry = true
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    calendars: List<CalendarOption>,
    defaultCalendarId: Long?,
    recentTitles: List<String>,
    hasReadPermission: Boolean,
    hasWritePermission: Boolean,
    loadingCalendars: Boolean,
    onRequestPermissions: () -> Unit,
    onDefaultCalendarSelected: (Long) -> Unit,
    onDeleteRecentTitle: (String) -> Unit,
    onClearRecentTitles: () -> Unit,
    resumeQuickEntry: Boolean,
    onResumeQuickEntry: () -> Unit,
    onOpenQuickEntry: () -> Unit
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("빠른 기록 설정") },
                actions = {
                    if (resumeQuickEntry) {
                        TextButton(onClick = onResumeQuickEntry) { Text("입력으로 돌아가기") }
                    } else {
                        TextButton(onClick = onOpenQuickEntry) { Text("빠른 기록") }
                    }
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("캘린더 접근", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (hasReadPermission && hasWritePermission) {
                            "캘린더 목록을 읽고 선택한 캘린더에 일정을 저장할 수 있습니다."
                        } else {
                            "캘린더 목록을 불러오고 일정을 저장하려면 캘린더 권한이 필요합니다."
                        }
                    )
                    if (!hasReadPermission || !hasWritePermission) {
                        Button(onClick = onRequestPermissions) { Text("캘린더 권한 허용") }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("기본 캘린더", style = MaterialTheme.typography.titleMedium)
                if (loadingCalendars) {
                    CircularProgressIndicator()
                } else if (calendars.isEmpty()) {
                    Text("사용 가능한 캘린더가 없습니다. 계정 동기화와 권한을 확인해 주세요.")
                } else {
                    CalendarSelector(
                        calendars = calendars,
                        selectedCalendarId = defaultCalendarId,
                        enabled = hasReadPermission,
                        label = "기본 캘린더 선택",
                        onSelected = onDefaultCalendarSelected
                    )
                }
                Text(
                    "선택한 캘린더는 이후 빠른 기록 화면의 기본값으로 사용됩니다.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Google 캘린더와 동기화하려면 Google 계정에 연결된 캘린더를 선택하세요. 다른 계정이나 기기 캘린더는 해당 계정의 동기화 설정을 따릅니다.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("최근 제목", style = MaterialTheme.typography.titleMedium)
                if (recentTitles.isNotEmpty()) {
                    TextButton(onClick = { showClearConfirmation = true }) { Text("전체 삭제") }
                }
            }
            if (recentTitles.isEmpty()) {
                Text("저장한 제목이 아직 없습니다.")
            } else {
                recentTitles.forEach { title ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(title, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onDeleteRecentTitle(title) }) { Text("삭제") }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("위젯 사용", style = MaterialTheme.typography.titleMedium)
                    Text("홈 화면을 길게 눌러 위젯 메뉴에서 ‘Quick Timestamp Log’를 추가하세요. 위젯을 누르면 빠른 기록 화면이 열립니다.")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("최근 제목을 모두 삭제할까요?") },
            text = { Text("삭제한 제목은 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearRecentTitles()
                    showClearConfirmation = false
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("취소") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun QuickEntryScreen(
    calendars: List<CalendarOption>,
    defaultCalendarId: Long?,
    recentTitles: List<String>,
    hasPermissions: Boolean,
    loadingCalendars: Boolean,
    onRequestPermissions: () -> Unit,
    onSettings: () -> Unit,
    onCancel: () -> Unit,
    onDeleteRecentTitle: (String) -> Unit,
    onSave: (String, Long, Long, Long, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var startDateTimeText by rememberSaveable { mutableStateOf(LocalDateTime.now().toString()) }
    var endDateTimeText by rememberSaveable { mutableStateOf(startDateTimeText) }
    val startDateTime = LocalDateTime.parse(startDateTimeText)
    val endDateTime = LocalDateTime.parse(endDateTimeText)
    var endTimeEdited by rememberSaveable { mutableStateOf(false) }
    var selectedCalendarId by rememberSaveable {
        mutableLongStateOf(defaultCalendarId ?: NO_CALENDAR_ID)
    }
    var titleToDelete by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(defaultCalendarId, calendars) {
        if (calendars.isNotEmpty() && calendars.none { it.id == selectedCalendarId }) {
            selectedCalendarId = defaultCalendarId?.takeIf { id -> calendars.any { it.id == id } }
                ?: calendars.firstOrNull { it.isPrimary }?.id
                ?: calendars.first().id
        }
    }

    fun changeStart(updated: LocalDateTime) {
        startDateTimeText = updated.toString()
        if (!endTimeEdited) endDateTimeText = updated.toString()
    }

    val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endMillis = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val validRange = endMillis >= startMillis

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("빠른 기록") },
                actions = {
                    TextButton(onClick = onSettings, enabled = !saving) { Text("설정") }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !saving
                ) { Text("취소") }
                Button(
                    onClick = {
                        if (!hasPermissions) {
                            onRequestPermissions()
                        } else if (title.isBlank()) {
                            Toast.makeText(context, "제목을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                        } else if (selectedCalendarId == NO_CALENDAR_ID) {
                            Toast.makeText(context, "저장할 캘린더를 선택해 주세요.", Toast.LENGTH_SHORT).show()
                        } else if (!validRange) {
                            Toast.makeText(context, "종료 시각은 시작 시각보다 빠를 수 없습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            saving = true
                            onSave(title.trim(), selectedCalendarId, startMillis, endMillis) {
                                saving = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !saving
                ) {
                    if (saving) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    else Text("완료")
                }
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("제목") },
                singleLine = true
            )

            if (!hasPermissions) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("일정을 저장하려면 캘린더 권한이 필요합니다.", modifier = Modifier.weight(1f))
                        TextButton(onClick = onRequestPermissions) { Text("허용") }
                    }
                }
            }

            if (loadingCalendars) {
                CircularProgressIndicator()
            } else {
                CalendarSelector(
                    calendars = calendars,
                    selectedCalendarId = selectedCalendarId.takeIf { it != NO_CALENDAR_ID },
                    enabled = hasPermissions && calendars.isNotEmpty(),
                    label = "저장할 캘린더",
                    onSelected = { selectedCalendarId = it }
                )
            }
            Text(
                "Google 캘린더와 동기화하려면 Google 계정 캘린더를 선택하세요.",
                style = MaterialTheme.typography.bodySmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateTimePickerButton(
                    label = "시작 날짜",
                    value = startDateTime.format(DATE_FORMATTER),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                changeStart(
                                    LocalDateTime.of(
                                        year, month + 1, day,
                                        startDateTime.hour, startDateTime.minute,
                                        startDateTime.second, startDateTime.nano
                                    )
                                )
                            },
                            startDateTime.year,
                            startDateTime.monthValue - 1,
                            startDateTime.dayOfMonth
                        ).show()
                    }
                )
                DateTimePickerButton(
                    label = "시작 시간",
                    value = startDateTime.format(TIME_FORMATTER),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showTimePicker(context, startDateTime) { hour, minute ->
                            changeStart(startDateTime.withHour(hour).withMinute(minute).withSecond(0).withNano(0))
                        }
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateTimePickerButton(
                    label = "종료 날짜",
                    value = endDateTime.format(DATE_FORMATTER),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                endDateTimeText = LocalDateTime.of(
                                    year, month + 1, day,
                                    endDateTime.hour, endDateTime.minute,
                                    endDateTime.second, endDateTime.nano
                                ).toString()
                                endTimeEdited = true
                            },
                            endDateTime.year,
                            endDateTime.monthValue - 1,
                            endDateTime.dayOfMonth
                        ).show()
                    }
                )
                DateTimePickerButton(
                    label = "종료 시간",
                    value = endDateTime.format(TIME_FORMATTER),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showTimePicker(context, endDateTime) { hour, minute ->
                            endDateTimeText = endDateTime.withHour(hour).withMinute(minute)
                                .withSecond(0).withNano(0).toString()
                            endTimeEdited = true
                        }
                    }
                )
            }
            if (!validRange) {
                Text(
                    "종료 시각은 시작 시각보다 빠를 수 없습니다.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text("최근 제목 · 한 번 눌러 입력 · 길게 눌러 삭제", style = MaterialTheme.typography.titleSmall)
            if (recentTitles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("저장한 제목이 여기에 최신순으로 표시됩니다.")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    recentTitles.forEach { recentTitle ->
                        Text(
                            text = recentTitle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { title = recentTitle },
                                    onLongClick = { titleToDelete = recentTitle }
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    titleToDelete?.let { selectedTitle ->
        AlertDialog(
            onDismissRequest = { titleToDelete = null },
            title = { Text("최근 제목 삭제") },
            text = { Text("‘$selectedTitle’을 최근 제목 목록에서 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteRecentTitle(selectedTitle)
                    titleToDelete = null
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { titleToDelete = null }) { Text("취소") } }
        )
    }
}

@Composable
private fun CalendarSelector(
    calendars: List<CalendarOption>,
    selectedCalendarId: Long?,
    enabled: Boolean,
    label: String,
    onSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCalendar = calendars.firstOrNull { it.id == selectedCalendarId }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "$label: ${selectedCalendar?.displayName ?: "선택 안 됨"}",
                modifier = Modifier.fillMaxWidth()
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            calendars.forEach { calendar ->
                DropdownMenuItem(
                    text = { Text(calendar.displayName) },
                    onClick = {
                        onSelected(calendar.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DateTimePickerButton(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, fontWeight = FontWeight.Medium)
        }
    }
}

private fun showTimePicker(
    context: Context,
    dateTime: LocalDateTime,
    onTimeSelected: (Int, Int) -> Unit
) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onTimeSelected(hour, minute) },
        dateTime.hour,
        dateTime.minute,
        DateFormat.is24HourFormat(context)
    ).show()
}

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private const val NO_CALENDAR_ID = -1L

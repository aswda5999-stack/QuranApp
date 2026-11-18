// ============================================================
// MainActivity.kt (في أسفل ملف MainActivity4.kt)
// ============================================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifs.ensureChannels(this)
        Reminders.rescheduleAll(this)
        setContent { QuranApp() }
    }
}

@Composable
fun QuranApp() {
    val ctx = LocalContext.current
    val view = LocalView.current
    val st = remember { AppState(ctx.applicationContext) }
    val fx = remember { Fx(view, ctx, st) }
    QuranTheme(dark = st.nightMode) {
        CompositionLocalProvider(LocalApp provides st, LocalFx provides fx) { AppRoot() }
    }
}

@Composable
private fun AppRoot() {
    val st = LocalApp.current
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val view = LocalView.current
    val stack = remember { mutableStateListOf(Screen.Home) }
    val nav = remember { Nav(stack, st) }
    val ps by PlayerService.state.collectAsState()
    var showFollowDialog by remember { mutableStateOf(false) }

    SideEffect {
        val window = (ctx as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !st.askedNotif) {
            st.askedNotif = true
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = stack.size > 1) { nav.back() }

    val showBars = nav.current != Screen.Reader

    Box(Modifier.fillMaxSize().background(RoyalForest)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            Box(Modifier.weight(1f).fillMaxWidth().background(p.bg)) {
                when (nav.current) {
                    Screen.Home -> HomeScreen(nav)
                    Screen.Reader -> ReaderScreen(nav)
                    Screen.Index -> IndexScreen(nav)
                    Screen.Audio -> AudioScreen(nav)
                    Screen.Tasbih -> TasbihScreen(nav)
                    Screen.Favorites -> FavoritesScreen(nav)
                    Screen.Prayer -> PrayerScreen(nav)
                    Screen.Hadith -> HadithScreen(nav)
                    Screen.Stats -> StatsScreen(nav)
                    Screen.Settings -> SettingsScreen(nav)
                }
            }
            if (showBars) {
                if (ps.surah > 0 && nav.current != Screen.Audio) MiniPlayer(ps) { nav.push(Screen.Audio) }
                AppBottomBar(nav = nav, onFollowClick = { showFollowDialog = true })
            } else {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }

    if (showFollowDialog) {
        FollowUsDialog(onDismiss = { showFollowDialog = false })
    }
}

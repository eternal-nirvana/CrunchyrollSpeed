package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Locale
import kotlin.math.hypot

/**
 * MainActivity hosts the full-screen Crunchyroll WebView with floating speed controls,
 * HTML5 fullscreen video handling, dynamic JavaScript speed enforcement, and clean termination.
 */
class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { SpeedPrefs(this) }
    private val viewModel: SpeedViewModel by viewModels { SpeedViewModel.Factory(prefs) }

    // HTML5 Fullscreen video management
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    // Drag detection for floating speed pill
    private var touchDownRawX = 0f
    private var touchDownRawY = 0f
    private var initialPillX = 0f
    private var initialPillY = 0f
    private var isDragging = false
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }

    // Request notification permission launcher for Android 13+
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Service continues running regardless; notification is displayed once granted
            if (isGranted) {
                SpeedService.start(this, viewModel.currentSpeed.value)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeActivityRef = WeakReference(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep screen awake while watching video
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setupBackPressHandling()
        setupWebView()
        setupPillDragging()
        setupPanelInteractions()
        setupTopLeftRestoreZone()
        observeViewModel()
        handleNotificationPermissionAndService()

        // Check if opened with SHOW_PILL action
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == SpeedService.ACTION_SHOW_PILL) {
            viewModel.setPillVisible(true)
            Toast.makeText(this, R.string.bring_pill_back_tip, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleNotificationPermissionAndService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        SpeedService.start(this, viewModel.currentSpeed.value)
    }

    /**
     * Configures the WebView with desktop Chrome user-agent, full HTML5 support,
     * restricted navigation to Crunchyroll and authorized CDNs, and custom WebChromeClient.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webSettings = binding.webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.mediaPlaybackRequiresUserGesture = false
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.allowFileAccess = false
        webSettings.allowContentAccess = false

        // Desktop-like Chrome User Agent to load the full player and avoid mobile redirects
        webSettings.userAgentString =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        // Enable cookies including third-party cookies (essential for Crunchyroll login/sessions)
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(binding.webView, true)
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return false
                val host = url.host?.lowercase(Locale.ROOT) ?: ""

                // Allow Crunchyroll domains, authentication services, video CDNs, and data/blob URLs
                val isAllowed = host.endsWith("crunchyroll.com") ||
                        host.endsWith("cr-play-service.com") ||
                        host.endsWith("akamaized.net") ||
                        host.endsWith("cloudfront.net") ||
                        host.endsWith("google.com") ||
                        host.endsWith("gstatic.com") ||
                        host.endsWith("recaptcha.net") ||
                        url.scheme == "blob" ||
                        url.scheme == "data"

                return if (isAllowed) {
                    false // Proceed inside WebView
                } else {
                    // Open third-party external links (e.g. social or external ads) in external browser
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, url))
                    } catch (e: Exception) {
                        // Fallback if no external browser
                    }
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // Inject early speed script on page navigation
                injectSpeedScript(viewModel.currentSpeed.value)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject full script with MutationObserver after DOM is ready
                injectSpeedScript(viewModel.currentSpeed.value)
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                customView = view
                customViewCallback = callback

                // Hide standard WebView and show custom view in fullscreen container
                binding.webView.visibility = View.GONE
                binding.fullscreenContainer.visibility = View.VISIBLE
                binding.fullscreenContainer.addView(
                    view,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                // Ensure pill container stays on top during fullscreen playback
                binding.pillContainer.bringToFront()

                // Hide system bars for immersive video viewing
                setImmersiveFullscreen(true)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }

            override fun onHideCustomView() {
                val view = customView ?: return

                binding.fullscreenContainer.removeView(view)
                binding.fullscreenContainer.visibility = View.GONE
                binding.webView.visibility = View.VISIBLE

                customViewCallback?.onCustomViewHidden()
                customView = null
                customViewCallback = null

                // Restore system bars and standard orientation
                setImmersiveFullscreen(false)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        // Load Crunchyroll home
        binding.webView.loadUrl("https://www.crunchyroll.com")
    }

    /**
     * Controls system bar visibility for fullscreen video playback.
     */
    private fun setImmersiveFullscreen(fullscreen: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (fullscreen) {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    /**
     * Injects the player speed manipulation script into the WebView.
     */
    private fun injectSpeedScript(speed: Float) {
        binding.webView.evaluateJavascript(PlayerJs.getSetSpeedScript(speed), null)
    }

    /**
     * Configures the draggable pill touch listener and saves coordinates.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupPillDragging() {
        // Initial positioning after layout
        binding.pillContainer.post {
            val (savedX, savedY) = viewModel.getSavedPillPosition()
            val parentWidth = binding.pillContainer.width
            val parentHeight = binding.pillContainer.height

            if (savedX >= 0 && savedY >= 0 && parentWidth > 0 && parentHeight > 0) {
                binding.speedPill.x = savedX.coerceIn(0f, (parentWidth - binding.speedPill.width).toFloat())
                binding.speedPill.y = savedY.coerceIn(0f, (parentHeight - binding.speedPill.height).toFloat())
            } else {
                // Default position: top right corner with padding
                val defaultX = (parentWidth - binding.speedPill.width - 48).coerceAtLeast(16).toFloat()
                val defaultY = 96f
                binding.speedPill.x = defaultX
                binding.speedPill.y = defaultY
                viewModel.savePillPosition(defaultX, defaultY)
            }
        }

        binding.speedPill.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownRawX = event.rawX
                    touchDownRawY = event.rawY
                    initialPillX = view.x
                    initialPillY = view.y
                    isDragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - touchDownRawX
                    val deltaY = event.rawY - touchDownRawY
                    val distance = hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat()

                    if (distance > touchSlop) {
                        isDragging = true
                    }

                    if (isDragging) {
                        val parent = view.parent as? ViewGroup ?: return@setOnTouchListener true
                        val maxX = (parent.width - view.width).toFloat().coerceAtLeast(0f)
                        val maxY = (parent.height - view.height).toFloat().coerceAtLeast(0f)

                        view.x = (initialPillX + deltaX).coerceIn(0f, maxX)
                        view.y = (initialPillY + deltaY).coerceIn(0f, maxY)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        viewModel.savePillPosition(view.x, view.y)
                    } else {
                        // It was a tap! Toggle speed panel
                        viewModel.setPanelExpanded(!viewModel.isPanelExpanded.value)
                    }
                    true
                }

                else -> false
            }
        }
    }

    /**
     * Configures preset buttons, custom speed input, and panel actions.
     */
    private fun setupPanelInteractions() {
        binding.btnClosePanel.setOnClickListener {
            viewModel.setPanelExpanded(false)
        }

        // Preset speed click listeners
        val presetButtons = mapOf(
            1.0f to binding.btnPreset100,
            1.5f to binding.btnPreset150,
            1.75f to binding.btnPreset175,
            2.0f to binding.btnPreset200,
            2.25f to binding.btnPreset225,
            2.5f to binding.btnPreset250
        )

        presetButtons.forEach { (speed, button) ->
            button.setOnClickListener {
                applyAndAutoCollapse(speed)
            }
        }

        // Custom speed increment/decrement
        binding.btnMinusSpeed.setOnClickListener {
            val current = binding.etCustomSpeed.text.toString().toFloatOrNull() ?: viewModel.currentSpeed.value
            val next = (current - SpeedPrefs.SPEED_STEP).coerceIn(SpeedPrefs.MIN_SPEED, SpeedPrefs.MAX_SPEED)
            binding.etCustomSpeed.setText(String.format(Locale.US, "%.2f", next))
        }

        binding.btnPlusSpeed.setOnClickListener {
            val current = binding.etCustomSpeed.text.toString().toFloatOrNull() ?: viewModel.currentSpeed.value
            val next = (current + SpeedPrefs.SPEED_STEP).coerceIn(SpeedPrefs.MIN_SPEED, SpeedPrefs.MAX_SPEED)
            binding.etCustomSpeed.setText(String.format(Locale.US, "%.2f", next))
        }

        // Apply custom speed
        binding.btnApplyCustomSpeed.setOnClickListener {
            val input = binding.etCustomSpeed.text.toString().toFloatOrNull()
            if (input == null || input < SpeedPrefs.MIN_SPEED || input > SpeedPrefs.MAX_SPEED) {
                Toast.makeText(this, R.string.invalid_speed_range, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            applyAndAutoCollapse(input)
        }

        // Hide pill button
        binding.btnHidePill.setOnClickListener {
            viewModel.setPillVisible(false)
            Toast.makeText(this, R.string.bring_pill_back_tip, Toast.LENGTH_LONG).show()
        }

        // Quit button
        binding.btnQuitApp.setOnClickListener {
            performFullShutdown()
        }
    }

    /**
     * Long press on the top-left corner restores the speed pill if hidden.
     */
    private fun setupTopLeftRestoreZone() {
        binding.topLeftRecoveryZone.setOnLongClickListener {
            if (!viewModel.isPillVisible.value) {
                viewModel.setPillVisible(true)
                Toast.makeText(this, "Speed pill restored", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }
    }

    private fun applyAndAutoCollapse(speed: Float) {
        val success = viewModel.setSpeed(speed)
        if (success) {
            injectSpeedScript(speed)
            SpeedService.updateSpeed(this, speed)
            viewModel.setPanelExpanded(false)
        } else {
            Toast.makeText(this, R.string.invalid_speed_range, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Observes ViewModel state flows to update UI and presets highlights.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentSpeed.collect { speed ->
                        val formatted = String.format(Locale.US, "%.2fx", speed)
                        binding.tvPillSpeed.text = formatted
                        binding.etCustomSpeed.setText(String.format(Locale.US, "%.2f", speed))
                        updatePresetButtonsSelection(speed)
                        injectSpeedScript(speed)
                    }
                }

                launch {
                    viewModel.isPillVisible.collect { visible ->
                        binding.speedPill.visibility = if (visible) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.isPanelExpanded.collect { expanded ->
                        binding.speedPanel.visibility = if (expanded) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun updatePresetButtonsSelection(currentSpeed: Float) {
        val presetButtons: Map<Float, Button> = mapOf(
            1.0f to binding.btnPreset100,
            1.5f to binding.btnPreset150,
            1.75f to binding.btnPreset175,
            2.0f to binding.btnPreset200,
            2.25f to binding.btnPreset225,
            2.5f to binding.btnPreset250
        )

        presetButtons.forEach { (presetSpeed, button) ->
            val isSelected = kotlin.math.abs(currentSpeed - presetSpeed) < 0.02f
            button.setBackgroundResource(
                if (isSelected) R.drawable.bg_preset_chip_active else R.drawable.bg_preset_chip
            )
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    viewModel.isPanelExpanded.value -> {
                        // Collapse expanded speed panel first
                        viewModel.setPanelExpanded(false)
                    }
                    customView != null -> {
                        // Exit fullscreen video
                        binding.webView.webChromeClient?.onHideCustomView()
                    }
                    binding.webView.canGoBack() -> {
                        // Navigate back inside Crunchyroll
                        binding.webView.goBack()
                    }
                    else -> {
                        // Exit activity
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    /**
     * Performs complete shutdown sequence:
     * 1. Stops SpeedService and cancels notification.
     * 2. Clears WebView (loadUrl about:blank, stopLoading, destroy).
     * 3. Calls finishAndRemoveTask().
     * 4. Kills process (Process.killProcess(Process.myPid())).
     */
    fun performFullShutdown() {
        try {
            // Stop service
            stopService(Intent(this, SpeedService::class.java))

            // Teardown WebView
            binding.webView.apply {
                loadUrl("about:blank")
                stopLoading()
                clearHistory()
                removeAllViews()
                destroy()
            }

            // Remove keepScreenOn
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Finish activity and remove from recents
            finishAndRemoveTask()
        } catch (e: Exception) {
            // Ensure exit
            finishAffinity()
        } finally {
            // Complete process termination
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    override fun onDestroy() {
        if (activeActivityRef?.get() == this) {
            activeActivityRef = null
        }
        super.onDestroy()
    }

    companion object {
        private var activeActivityRef: WeakReference<MainActivity>? = null

        /**
         * Called by SpeedService when user taps "Quit" from the notification.
         */
        fun requestFullShutdown(context: Context) {
            val activity = activeActivityRef?.get()
            if (activity != null && !activity.isFinishing) {
                activity.runOnUiThread {
                    activity.performFullShutdown()
                }
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }
}

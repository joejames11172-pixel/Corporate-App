package com.peostectica.hereshift

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.location.Geocoder
import java.util.Locale
import android.provider.Settings
import android.view.*
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import android.widget.*
import kotlin.math.roundToInt

/** Native, dependency-light initial release: stationary location, 40 paired display themes,
 * 20 audio themes. No remote control, unapproved billing, telemetry, or integrity evasion. */
class MainActivity : Activity() {
    private enum class Page { HOME, CIRCLE, DISPLAY, AUDIO, TRUST, TESTING }
    private lateinit var engine: SettingsEngine
    private lateinit var audio: AudioFeedback
    private var page: Page = Page.HOME
    private var scroll: ScrollView? = null
    private val prefs by lazy { getSharedPreferences("hereShift", MODE_PRIVATE) }
    private var latitudeInput: EditText? = null
    private var longitudeInput: EditText? = null
    private val legacyPermissions = 404
    private val notificationPermissionRequest = 405
    private var pendingPointAfterNotificationPermission: TestPoint? = null
    private var modernBack: OnBackInvokedCallback? = null
    private fun navigateBack() {
        if (page == Page.HOME) finish() else { audio.stopPreview(); show(Page.HOME) }
    }

    private inner class AndroidPrefs : PreferenceStore {
        override fun get(key: String): String? = prefs.getString("ui_$key", null)
        override fun put(key: String, value: String) { prefs.edit().putString("ui_$key", value).apply() }
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
    private fun shade(hex: String): Int = Color.parseColor(hex)
    private val systemDark: Boolean get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    private val palette: ColorRoles get() = engine.currentPalette(systemDark)
    private fun rounded(color: Int, radius: Int = 14, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }
    private fun colorText(size: Float, strong: Boolean = false): TextView = TextView(this).apply {
        setTextColor(shade(palette.onBackground));textSize=size
        if (strong) typeface = Typeface.DEFAULT_BOLD
    }
    private fun topMargin(v: View, space: Int = 10): View = v.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(space) }
    }
    private fun column(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }
    private fun innerCard(padding: Int = 18): LinearLayout = column().apply {
        background = rounded(shade(palette.surface), 18, shade(palette.primary))
        setPadding(dp(padding),dp(padding),dp(padding),dp(padding))
    }
    private fun addText(parent: LinearLayout, value: String, size: Float = 15f, bold: Boolean = false, margin: Int = 0): TextView = colorText(size, bold).apply {
        text = value
        if (margin>0) topMargin(this,margin)
        parent.addView(this)
    }
    private fun button(label: String, secondary: Boolean = false, action: () -> Unit): Button = Button(this).apply {
        text=label; isAllCaps=false; textSize=15f
        setTextColor(shade(if (secondary) palette.onSurface else palette.onPrimary))
        background=rounded(shade(if (secondary) palette.surface else palette.primary), 13, if (secondary) shade(palette.primary) else null)
        setPadding(dp(10),dp(12),dp(10),dp(12)); minHeight=dp(48)
        setOnClickListener { audio.interaction(); action() }
    }
    private fun field(label: String, value: String): EditText = EditText(this).apply {
        setSingleLine(true);hint=label;setText(value)
        textSize=16f;setTextColor(shade(palette.onSurface));setHintTextColor(shade(palette.onSurface))
        inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        background=rounded(shade(palette.background), 10, shade(palette.primary))
        setPadding(dp(14),dp(13),dp(14),dp(13));minHeight=dp(50)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine=SettingsEngine(AndroidPrefs())
        audio=AudioFeedback(this,engine)
        if (engine.audioId!="none") audio.loadTheme(engine.audioId)
        if (Build.VERSION.SDK_INT >= 33) {
            val callback = OnBackInvokedCallback { navigateBack() }
            modernBack = callback
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
        }
        show(Page.HOME)
    }
    override fun onResume() { super.onResume(); if (::engine.isInitialized && page == Page.HOME) show(Page.HOME) }
    override fun onPause() { super.onPause(); if (::audio.isInitialized) audio.stopPreview() }
    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= 33) modernBack?.let { onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
        if (::audio.isInitialized) audio.close()
        super.onDestroy()
    }
    @Deprecated("API 26-32 back dispatch only; Android 13+ uses OnBackInvokedCallback")
    override fun onBackPressed() { navigateBack() }

    private fun show(next: Page, preserveScroll: Boolean = false) {
        val prevPos = if (preserveScroll) scroll?.scrollY ?: 0 else 0
        page=next
        val surface=palette
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(shade(surface.background)) }
        val header=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(11),dp(16),dp(11))}
        if (next!=Page.HOME) {
            val back=button("← Back",true){ show(Page.HOME) }
            header.addView(back,LinearLayout.LayoutParams(dp(82),dp(47)))
        }
        val logo=ImageView(this).apply {setImageResource(if (effectiveDark()) R.drawable.hereshift_icon_dark else R.drawable.hereshift_icon_light);contentDescription="HereShift selected icon"; scaleType=ImageView.ScaleType.FIT_CENTER}
        header.addView(logo,LinearLayout.LayoutParams(dp(36),dp(36)).apply {marginStart=dp(8)})
        val heading=colorText(19f,true).apply { text=when(next){Page.HOME->"HereShift";Page.CIRCLE->"Circle and sharing";Page.DISPLAY->"Display settings";Page.AUDIO->"Audio settings";Page.TRUST->"Privacy and testing";Page.TESTING->"Development mode"};setPadding(dp(8),0,0,0) }
        header.addView(heading,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
        root.addView(header)
        scroll=ScrollView(this).apply {isFillViewport=false;clipToPadding=false}
        val content=column().apply {setPadding(dp(16),0,dp(16),dp(34))}
        when(next) { Page.HOME->renderHome(content); Page.CIRCLE->renderCircle(content); Page.DISPLAY->renderDisplay(content); Page.AUDIO->renderAudio(content);Page.TRUST->renderTrust(content);Page.TESTING->renderTesting(content) }
        scroll!!.addView(content)
        root.addView(scroll,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f))
        // Persistent native navigation: visible on every screen, independent of scroll position.
        val navigationBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(7),dp(8),dp(7),dp(8))
            background = rounded(shade(palette.surface), 16, shade(palette.primary))
        }
        listOf(
            Triple("Home", Page.HOME, "Home"),
            Triple("Circle", Page.CIRCLE, "Circle"),
            Triple("Display", Page.DISPLAY, "Display"),
            Triple("Audio", Page.AUDIO, "Audio"),
            Triple("Privacy", Page.TRUST, "Privacy")
        ).forEach { (_, destination, caption) ->
            val navButton = button(caption, destination != next) {
                if (destination != page) {
                    if (page == Page.HOME) saveCoordinates()
                    show(destination)
                }
            }
            navButton.contentDescription = "Navigate to " + caption
            navigationBar.addView(navButton, LinearLayout.LayoutParams(dp(92), dp(52)).apply {
                marginStart = dp(3); marginEnd = dp(3)
            })
        }
        val navigationScroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = true
            contentDescription = "HereShift page navigation"
            addView(navigationBar)
        }
        root.addView(navigationScroller, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        setContentView(root)
        // Match system bar icons to the currently selected light/dark palette. Android 15+
        // enforces edge-to-edge, so setting statusBarColor alone is not sufficient.
        if (Build.VERSION.SDK_INT >= 30) {
            val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (effectiveDark()) 0 else mask, mask)
        } else {
            window.decorView.systemUiVisibility = if (effectiveDark()) 0 else
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener { view, insets ->
                val sys=insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(sys.left,sys.top,sys.right,sys.bottom)
                insets
            }
            root.requestApplyInsets()
        }
        if (preserveScroll) scroll?.post {scroll?.scrollTo(0,prevPos)}
    }
    private fun effectiveDark():Boolean= when(engine.appearance) {
        Appearance.FOLLOW_DEVICE->systemDark; Appearance.DARK->true;Appearance.LIGHT->false
    }
    private fun renderHome(c: LinearLayout) {
        addText(c,"Choose your virtual location",24f,true)
        addText(c,"Your chosen point is synthetic or manually entered. The app never reads your real GPS coordinates, records routes, or sends data to a server.",14f,margin=8)
        val editor=innerCard().apply {
            addText(this,"Enter your coordinates",20f,true)
            addText(this,"Type any valid latitude and longitude. Example cities are optional shortcuts.",14f,margin=7)
            latitudeInput=field("Latitude",prefs.getString("lat","41.8781") ?: "41.8781")
            longitudeInput=field("Longitude",prefs.getString("lon","-87.6298") ?: "-87.6298")
            addView(topMargin(latitudeInput!!,12));addView(topMargin(longitudeInput!!,8))
            addView(topMargin(button("Save my coordinates") {
                val lat = latitudeInput?.text?.toString()?.trim().orEmpty()
                val lon = longitudeInput?.text?.toString()?.trim().orEmpty()
                val point = ScenarioRules.parse(lat, lon)
                if (point == null) {
                    latitudeInput?.error = "Latitude must be between -90 and 90"
                    longitudeInput?.error = "Longitude must be between -180 and 180"
                    Toast.makeText(this@MainActivity, "Correct both coordinates before saving", Toast.LENGTH_LONG).show()
                } else {
                    prefs.edit().putString("lat", lat).putString("lon", lon).apply()
                    Toast.makeText(this@MainActivity, "Coordinates saved", Toast.LENGTH_SHORT).show()
                }
            }, 12))
            addText(this, "Other location sources", 17f, true, margin = 18)
            val addressInput = EditText(this@MainActivity).apply {
                hint = "Street address, city, state"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
                setSingleLine(true)
                setPadding(dp(14),dp(13),dp(14),dp(13))
                background = rounded(shade(palette.background),10,shade(palette.primary))
                setTextColor(shade(palette.onSurface))
            }
            addView(topMargin(addressInput, 9))
            addView(topMargin(button("Confirm address", true) {
                if (addressInput.text.toString().trim().isEmpty()) addressInput.error = "Enter an address"
                else resolveStreetAddress(addressInput.text.toString().trim())
            }, 7))
            val zipInput = EditText(this@MainActivity).apply {
                hint = "U.S. ZIP code"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(android.text.InputFilter.LengthFilter(5))
                setSingleLine(true)
                setPadding(dp(14),dp(13),dp(14),dp(13))
                background = rounded(shade(palette.background),10,shade(palette.primary))
                setTextColor(shade(palette.onSurface))
            }
            addView(topMargin(zipInput, 10))
            addView(topMargin(button("Select ZIP area", true) {
                val entered = zipInput.text.toString().trim()
                if (!Regex("[0-9]{5}").matches(entered)) zipInput.error = "Enter a five-digit ZIP"
                else resolveZipArea(entered)
            }, 7))
            addView(topMargin(button("Example: Chicago city center",true){setPoint("41.8781","-87.6298")},8))
            addView(topMargin(button("Example: San Francisco city center",true){setPoint("37.7749","-122.4194")},8))
            addView(topMargin(button("Example: New York city center",true){setPoint("40.7128","-74.0060")},8))
            addView(topMargin(button("Preview my selected point",false){previewPoint()},12))
        }
        c.addView(topMargin(editor,16))
        val navigation=innerCard().apply {
            addText(this,"Settings",18f,true)
            addView(topMargin(button("Display · 40 color themes") {saveCoordinates();show(Page.DISPLAY)},12))
            addView(topMargin(button("Audio · 20 sound themes") {saveCoordinates();show(Page.AUDIO)},8))
            addView(topMargin(button("Privacy and responsible use",true) {saveCoordinates();show(Page.TRUST)},8))
        }
        c.addView(topMargin(navigation,18))
        val physical=innerCard().apply {
            addText(this,"2 · Android developer test mode",18f,true)
            addText(this,"For testing apps you own or are authorized to test, Android can provide a MOCK location. Other applications can detect that it is simulated. The Android service displays an active notification with Stop.",14f,margin=7)
            addView(topMargin(button("Open Developer Options",true){openDeveloperSettings()},10))
            addView(topMargin(button("Start authorized mock-location session") {startMock()},8))
            addView(topMargin(button("Stop simulation",true){stopMock()},8))
            val status=prefs.getString(MockLocationService.PREF_STATUS,"Not running") ?: "Not running"
            addText(this,"Service: $status",13f,margin=12)
            addText(this,"Android may require granting precise location for a location-type foreground service. HereShift does not read the device's real position.",12f,margin=8)
        }
        c.addView(topMargin(physical,16))
        val premium=innerCard().apply {
            addText(this,"Premium status",19f,true)
            val indicator = addText(this,"✕  Premium inactive · Free",17f,true,margin=8)
            indicator.setTextColor(Color.rgb(190, 48, 48))
            addText(this,"Premium requires a verified Google Play subscription. No subscription is active or billed in this build. Free mode cannot access Premium-only Circle sharing.",13f,margin=7)
            addView(topMargin(button("Get Premium · availability",true) {
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Premium is not available yet")
                    .setMessage("Google Play Billing and verified subscription activation are not connected. This button cannot purchase Premium or grant access. No payment will be taken.")
                    .setPositiveButton("OK",null).show()
            },10))
        }
        c.addView(topMargin(premium,16))
    }
    private fun renderCircle(c: LinearLayout) {
        addText(c, "Circle · contact sharing", 24f, true)
        addText(c, "Choose one consenting contact at a time. A Circle invitation alone never grants location access. Remote location sharing is unavailable until authenticated approval and revocation are connected.", 14f, margin = 9)
        val status = innerCard().apply {
            addText(this, "Sharing status", 19f, true)
            addText(this, "Not connected · no contact location is being received or published", 14f, margin = 8)
            addText(this, "In-App Virtual Location and Android Device-Wide Mock Location are separate modes. Both require explicit consent. The latter additionally requires Android Developer Options authorization.", 14f, margin = 10)
            addView(topMargin(button("Request contact permission", true) {
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Contact approval required")
                    .setMessage("Authenticated Circle invitations and contact-approved live sharing are not connected in this version. No invitation was sent and no contact has been authorized.")
                    .setPositiveButton("OK", null).show()
            }, 12))
            addView(topMargin(button("Stop / revoke my sharing", true) {
                stopMock()
                prefs.edit().remove("circle_selected_contact").apply()
                Toast.makeText(this@MainActivity, "Local session stopped; no remote sharing was active", Toast.LENGTH_LONG).show()
            }, 8))
        }
        c.addView(topMargin(status, 18))
        val policy = innerCard().apply {
            addText(this, "Required permission rules", 18f, true)
            addText(this, "Contact selection must be authenticated, owner-approved, time-limited, and immediately revocable. Offline or stale authorization must fail closed. A Premium subscription never overrides the contact's consent.", 14f, margin = 8)
        }
        c.addView(topMargin(policy, 14))
    }

    private fun resolveZipArea(zip: String) {
        if (!Geocoder.isPresent()) {
            Toast.makeText(this, "ZIP lookup unavailable on this device", Toast.LENGTH_LONG).show()
            return
        }
        Thread {
            val results = try {
                @Suppress("DEPRECATION")
                Geocoder(this@MainActivity, Locale.US).getFromLocationName("$zip, USA", 8).orEmpty()
            } catch (_: Exception) { emptyList() }
            val matches = results.filter { item ->
                item.countryCode.equals("US", ignoreCase = true) &&
                    item.postalCode == zip &&
                    ScenarioRules.parse(item.latitude.toString(), item.longitude.toString()) != null
            }
            Handler(Looper.getMainLooper()).post {
                if (isFinishing || isDestroyed) return@post
                if (matches.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No matching verified ZIP area. Location unchanged.", Toast.LENGTH_LONG).show()
                    return@post
                }
                val match = matches.first()
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Confirm approximate ZIP area")
                    .setMessage("ZIP: $zip\\nArea: ${match.locality.orEmpty()}, ${match.adminArea.orEmpty()}\\n\\nApproximate geocoded area only, NOT a verified street address.\\nCoordinates: ${match.latitude}, ${match.longitude}")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Use approximate area") { _, _ ->
                        setPoint(match.latitude.toString(), match.longitude.toString())
                    }.show()
            }
        }.start()
    }

    private fun resolveStreetAddress(query: String) {
        if (!Geocoder.isPresent()) {
            Toast.makeText(this, "Address geocoding is unavailable on this device", Toast.LENGTH_LONG).show()
            return
        }
        // Geocoding may use a platform network service: never block the UI thread.
        Thread {
            val results = try {
                @Suppress("DEPRECATION")
                Geocoder(this@MainActivity, Locale.US).getFromLocationName(query, 5).orEmpty()
            } catch (_: Exception) { emptyList() }
            Handler(Looper.getMainLooper()).post {
                if (isFinishing || isDestroyed) return@post
                val candidates = results.filter {
                    ScenarioRules.parse(it.latitude.toString(), it.longitude.toString()) != null
                }
                if (candidates.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No verified address match. Location unchanged.", Toast.LENGTH_LONG).show()
                    return@post
                }
                val labels = candidates.map { result ->
                    val label = (0..result.maxAddressLineIndex).joinToString(", ") {
                        result.getAddressLine(it).orEmpty()
                    }.ifBlank { "Unnamed geocoded result" }
                    "$label\n${result.latitude}, ${result.longitude}"
                }.toTypedArray()
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Select and confirm a geocoded address")
                    .setSingleChoiceItems(labels, -1) { dialog, selected ->
                        dialog.dismiss()
                        val result = candidates[selected]
                        android.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle("Use this location?")
                            .setMessage(labels[selected] + "\n\nThis uses the geocoder's result; confirm that it is correct.")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Use location") { _, _ ->
                                setPoint(result.latitude.toString(), result.longitude.toString())
                            }.show()
                    }
                    .setNegativeButton("Cancel", null).show()
            }
        }.start()
    }

    private fun setPoint(lat:String,lon:String) {
        latitudeInput?.setText(lat);longitudeInput?.setText(lon); saveCoordinates()
        Toast.makeText(this,"Selected illustrative point",Toast.LENGTH_SHORT).show()
    }
    private fun saveCoordinates() {
        val lat = latitudeInput?.text?.toString()?.trim() ?: return
        val lon = longitudeInput?.text?.toString()?.trim() ?: return
        if (ScenarioRules.parse(lat, lon) == null) return
        prefs.edit().putString("lat", lat).putString("lon", lon).apply()
    }
    private fun currentPoint():TestPoint? {
        // Always validate the visible edits. Never fall back to a previously saved
        // point when an invalid unsaved coordinate is on screen.
        val latitude = latitudeInput?.text?.toString()?.trim()
            ?: prefs.getString("lat", "")?.trim().orEmpty()
        val longitude = longitudeInput?.text?.toString()?.trim()
            ?: prefs.getString("lon", "")?.trim().orEmpty()
        val point = ScenarioRules.parse(latitude, longitude) ?: return null
        prefs.edit().putString("lat", latitude).putString("lon", longitude).apply()
        return point
    }
    private fun previewPoint() {
        val point=currentPoint()
        if(point==null) Toast.makeText(this,"Enter valid latitude [-90,90] and longitude [-180,180]",Toast.LENGTH_LONG).show()
        else android.app.AlertDialog.Builder(this).setTitle("Virtual location · in-app preview")
            .setMessage("Latitude: ${point.latitude}\nLongitude: ${point.longitude}\n\nIn-app preview only. This does not change other applications. No real GPS location collected.")
            .setPositiveButton("OK",null).show()
    }
    private fun openDeveloperSettings(){
        try {startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))}
        catch (_: Exception){Toast.makeText(this,"Open Android Settings → Developer options → Select mock location app → HereShift",Toast.LENGTH_LONG).show()}
    }
    private fun startMock() {
        val point=currentPoint()
        if(point==null){Toast.makeText(this,"Choose valid coordinates first",Toast.LENGTH_LONG).show();return}
        android.app.AlertDialog.Builder(this).setTitle("Confirm authorized test simulation")
            .setMessage("Only use with software you are authorized to test. Android marks these coordinates as MOCK.\n\nWhen active, HereShift displays a persistent notification and a Stop action.\n\nContinue?")
            .setNegativeButton("Cancel",null).setPositiveButton("Continue") {_,_->
                if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),legacyPermissions)
                else launchService(point)
            }.show()
    }
    private fun launchService(point:TestPoint) {
        // Ensure the always-visible foreground Stop control can be displayed. Do not race
        // the notification-permission dialog; only start after the user grants permission.
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingPointAfterNotificationPermission = point
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationPermissionRequest)
            return
        }
        startAuthorizedService(point)
    }
    private fun startAuthorizedService(point:TestPoint) {
        try {
            val intent=Intent(this,MockLocationService::class.java).setAction(MockLocationService.ACTION_START)
                .putExtra(MockLocationService.EXTRA_LAT,point.latitude).putExtra(MockLocationService.EXTRA_LON,point.longitude)
            startForegroundService(intent)
            prefs.edit().putString(MockLocationService.PREF_STATUS,"Starting; check Android notification or Developer Options").apply()
            show(Page.HOME)
        } catch (ex:Exception){Toast.makeText(this,"Could not start: ${ex.javaClass.simpleName}",Toast.LENGTH_LONG).show()}
    }
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        when (requestCode) {
            legacyPermissions -> {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                    currentPoint()?.let { launchService(it) }
                } else Toast.makeText(this,"Permission denied; in-app preview remains available",Toast.LENGTH_LONG).show()
            }
            notificationPermissionRequest -> {
                val savedPoint = pendingPointAfterNotificationPermission
                pendingPointAfterNotificationPermission = null
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED && savedPoint != null) {
                    startAuthorizedService(savedPoint)
                } else {
                    Toast.makeText(this,"Enable notifications for a visible Stop action; simulation was not started",Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun stopMock(){stopService(Intent(this,MockLocationService::class.java));prefs.edit().putString(MockLocationService.PREF_STATUS,"Stop requested").apply();show(Page.HOME)}
    private fun renderDisplay(c:LinearLayout) {
        addText(c,"40 approved paired themes",20f,true)
        addText(c,"Choose one appearance family. Light and dark colors are paired automatically. Choose system default, light, or dark.",14f,margin=7)
        val appearance=Spinner(this)
        val options=listOf("Follow device","Light","Dark")
        appearance.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,options)
        appearance.setSelection(engine.appearance.ordinal)
        var armed=false
        appearance.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p:android.widget.AdapterView<*>?){}
            override fun onItemSelected(p:android.widget.AdapterView<*>?,v:View?,pos:Int,id:Long){if(!armed){armed=true;return};engine.chooseAppearance(Appearance.values()[pos]);audio.interaction();show(Page.DISPLAY,true)}
        }
        c.addView(topMargin(appearance,12))
        for(theme in Catalog.pairs){
            val card=innerCard(12)
            val line=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
            val radio=RadioButton(this).apply {isChecked=engine.themeId==theme.id;buttonTintList=android.content.res.ColorStateList.valueOf(shade(palette.primary))}
            line.addView(radio,LinearLayout.LayoutParams(dp(50),dp(52)))
            val name=colorText(16f,true).apply{text="${theme.id} · ${theme.name}"}
            line.addView(name,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
            for(colors in listOf(theme.light,theme.dark)){
                val chip=View(this).apply{background=rounded(shade(colors.background),8,shade(colors.primary));contentDescription=if(colors===theme.light)"Light preview" else "Dark preview"}
                line.addView(chip,LinearLayout.LayoutParams(dp(37),dp(37)).apply{marginStart=dp(7)})
            }
            card.addView(line)
            card.contentDescription="Theme ${theme.id}: ${theme.name}";card.isClickable=true
            val applyTheme= {engine.chooseTheme(theme.id);audio.interaction();show(Page.DISPLAY,true)}
            card.setOnClickListener{applyTheme()};radio.setOnClickListener{applyTheme()}
            if(engine.themeId==theme.id){card.background=rounded(shade(palette.surface),16,shade(palette.primary))}
            c.addView(topMargin(card,8))
        }
        c.addView(topMargin(button("← Back to HereShift",true){show(Page.HOME)},18))
    }
    private fun renderAudio(c:LinearLayout) {
        addText(c,"Audio themes",20f,true)
        addText(c,"None disables effects. Other choices rotate through twelve original sounds when you tap, swipe, or select controls. Previews loop until stopped.",14f,margin=7)
        val volume=SeekBar(this).apply{max=100;progress=engine.volumePercent;contentDescription="Sound-effect volume"}
        volume.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onStartTrackingTouch(s:SeekBar?){}
            override fun onStopTrackingTouch(s:SeekBar?){engine.setVolume(s?.progress ?: 0);audio.interaction()}
            override fun onProgressChanged(s:SeekBar?,v:Int,user:Boolean){if(user)engine.setVolume(v)}
        })
        addText(c,"Volume",15f,true,12);c.addView(volume)
        c.addView(topMargin(button("Stop audio preview",true){audio.stopPreview()},8))
        val names=listOf("none" to "None · sound effects off")+Catalog.audio.map{it.id to "${it.id} · ${it.name}"}
        for((id,title) in names){
            val card=innerCard(10)
            val line=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
            val radio=RadioButton(this).apply{isChecked=engine.audioId==id;buttonTintList=android.content.res.ColorStateList.valueOf(shade(palette.primary))}
            line.addView(radio,LinearLayout.LayoutParams(dp(50),dp(53)))
            line.addView(colorText(15f,true).apply{text=title},LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
            if(id!="none")line.addView(button("▶ Play",true){audio.preview(id)},LinearLayout.LayoutParams(dp(90),dp(43)))
            card.addView(line)
            card.setOnClickListener {audio.stopPreview();engine.chooseAudio(id);audio.loadTheme(id);show(Page.AUDIO,true)}
            radio.setOnClickListener{audio.stopPreview();engine.chooseAudio(id);audio.loadTheme(id);show(Page.AUDIO,true)}
            c.addView(topMargin(card,8))
        }
        c.addView(topMargin(button("← Back to HereShift",true){audio.stopPreview();show(Page.HOME)},18))
    }
    private val uiTestTasks = listOf(
        "Home and navigation", "Display themes and contrast", "Audio themes and None",
        "Volume and sound preview", "Coordinate validation and preview",
        "Privacy information", "Back navigation", "Settings persistence after restart"
    )
    private fun renderTesting(c: LinearLayout) {
        addText(c,"DEVELOPMENT · Real-user UI testing",20f,true)
        addText(c,"Explore the interface and record feedback. This screen does not start location simulation, send telemetry, or make purchases.",14f,margin=8)
        addText(c,"Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}",13f,margin=8)
        val shortcuts=innerCard().apply {
            addText(this,"Explore screens under test",18f,true)
            addText(this,"Return to Development Mode from Home. Checklist results stay saved.",13f,margin=6)
            addView(topMargin(button("Test display settings"){show(Page.DISPLAY)},10))
            addView(topMargin(button("Test audio settings"){show(Page.AUDIO)},8))
            addView(topMargin(button("Test privacy screen"){show(Page.TRUST)},8))
            addView(topMargin(button("Test location preview",true){show(Page.HOME)},8))
        }
        c.addView(topMargin(shortcuts,14))
        val p=innerCard().apply {
            addText(this,"Guided interface checklist",18f,true)
            for ((index, task) in uiTestTasks.withIndex()) {
                val key="ui_test_$index"
                addText(this,"${index+1}. $task",15f,true,12)
                val options=listOf("Not tested","Pass","Fail","Skip")
                val spinner=Spinner(this@MainActivity)
                spinner.adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,options)
                spinner.setSelection(prefs.getInt(key,0).coerceIn(0,3))
                spinner.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent:android.widget.AdapterView<*>?){}
                    override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:View?,position:Int,id:Long) {
                        prefs.edit().putInt(key,position).apply()
                    }
                }
                addView(topMargin(spinner,4))
            }
        }
        c.addView(topMargin(p,14))
        val feedback=innerCard().apply {
            addText(this,"Tester feedback",18f,true)
            addText(this,"Rate overall usability from 1 (difficult) to 5 (easy).",14f,margin=8)
            val rating=RatingBar(this@MainActivity).apply {
                numStars=5;stepSize=1f;rating=prefs.getFloat("ui_test_rating",0f)
                setOnRatingBarChangeListener { _, value, _ -> prefs.edit().putFloat("ui_test_rating",value).apply() }
            }
            addView(topMargin(rating,8))
            val notes=EditText(this@MainActivity).apply {
                hint="Screen, steps to reproduce, expected result, actual result, or suggestion"
                setText(prefs.getString("ui_test_notes",""))
                addTextChangedListener(object:android.text.TextWatcher {
                    override fun beforeTextChanged(text:CharSequence?,start:Int,count:Int,after:Int){}
                    override fun onTextChanged(text:CharSequence?,start:Int,before:Int,count:Int){
                        prefs.edit().putString("ui_test_notes",text?.toString() ?: "").apply()
                    }
                    override fun afterTextChanged(text:android.text.Editable?){}
                })
                minLines=4;maxLines=9
                inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                setTextColor(shade(palette.onSurface))
                setHintTextColor(shade(palette.onSurface))
            }
            addView(topMargin(notes,8))
            addView(topMargin(button("Save feedback",true){
                prefs.edit().putString("ui_test_notes",notes.text.toString()).apply()
                Toast.makeText(this@MainActivity,"Feedback saved on this phone",Toast.LENGTH_SHORT).show()
            },8))
            addView(topMargin(button("Review and share test report"){
                prefs.edit().putString("ui_test_notes",notes.text.toString()).apply()
                val report=buildString {
                    appendLine("HereShift UI development test report")
                    appendLine("Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
                    appendLine("Usability rating: ${prefs.getFloat("ui_test_rating",0f)}/5")
                    val statuses=listOf("Not tested","Pass","Fail","Skip")
                    uiTestTasks.forEachIndexed { i, task -> appendLine("${i+1}. $task: ${statuses[prefs.getInt("ui_test_$i",0).coerceIn(0,3)]}") }
                    appendLine("Feedback: ${prefs.getString("ui_test_notes","")}")
                }
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Share UI test report?")
                    .setMessage("The report contains your checklist, rating, device version, and written feedback. It stays on this phone unless you choose Share.")
                    .setNegativeButton("Cancel",null)
                    .setPositiveButton("Share"){_,_->
                        val intent=Intent(Intent.ACTION_SEND).apply { type="text/plain";putExtra(Intent.EXTRA_TEXT,report) }
                        startActivity(Intent.createChooser(intent,"Share HereShift UI feedback"))
                    }.show()
            },8))
            addView(topMargin(button("Delete local test feedback",true){
                android.app.AlertDialog.Builder(this@MainActivity).setTitle("Delete test feedback?")
                    .setMessage("Remove all saved UI checklist results, rating, and notes from this phone?")
                    .setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->
                        val edit=prefs.edit()
                        uiTestTasks.indices.forEach { edit.remove("ui_test_$it") }
                        edit.remove("ui_test_rating").remove("ui_test_notes").apply()
                        show(Page.TESTING)
                    }.show()
            },8))
        }
        c.addView(topMargin(feedback,16))
        c.addView(topMargin(button("← Back to HereShift",true){show(Page.HOME)},16))
    }

    private fun renderTrust(c:LinearLayout) {
        addText(c,"Our customer commitment",21f,true)
        val p=innerCard().apply{
            addText(this,"HereShift is for legitimate, authorized location simulation and testing.",18f,true)
            addText(this,"This initial build uses manual or illustrative stationary locations. It has no networking permission, no analytics, no microphone access, no contact access, and does not request or record your physical GPS position.",15f,margin=12)
            addText(this,"For an Android foreground location-type service, the operating system requires a precise-location permission even though our code does not call the real location APIs. It only writes test-provider coordinates that you choose.",14f,margin=12)
            addText(this,"Developer Options authorization is always required to provide device test locations. These are marked as mock locations, are not guaranteed to work in third-party applications, and do not establish legal eligibility for location-restricted services.",14f,margin=12)
            addText(this,"The persistent notification provides an immediate Stop control. Audio themes respect the device ringer's silent and vibrate modes and can be disabled with None.",14f,margin=12)
            addText(this,"Real-money wagering geolocation controls must not be circumvented. Paid subscriptions, live friend sharing and TapVoyage remote control are not active in this build.",14f,margin=12)
        }
        c.addView(topMargin(p,15));c.addView(topMargin(button("← Back",true){show(Page.HOME)},16))
    }
}
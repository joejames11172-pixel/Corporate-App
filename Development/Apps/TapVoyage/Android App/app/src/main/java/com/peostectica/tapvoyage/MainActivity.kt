package com.peostectica.tapvoyage

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Spinner
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.peostectica.tapvoyage.core.HiddenContacts
import kotlin.math.roundToInt

/** Native development client; no simulated contact is represented as authenticated. */
class MainActivity : Activity() {
    private enum class Page(val label: String) {
        CIRCLE("My Circle"), GET("Get Permission"), GIVE("Give Permission"), PREMIUM("Premium"), SETTINGS("Settings"), AUDIO("Audio")
    }
    private val prefs by lazy { getSharedPreferences("tapvoyage_ui", MODE_PRIVATE) }
    private var page = Page.CIRCLE
    private var selectedContact: String? = null // user-picked local display name only
    private var selectedContactId: String? = null // Android local Contacts Provider ID, NOT remote identity
    private val hiddenContacts by lazy {
        // Local-only visibility preferences; no contact name, phone number or backend grant is stored.
        HiddenContacts(prefs.getStringSet("hidden_contact_ids", emptySet()).orEmpty())
    }
    private var showHidden = false // display-only; never changes grants or revocation
    private val audioThemes by lazy {
        JSONObject(assets.open("audio/audio_themes_manifest.json").bufferedReader().use { it.readText() })
            .getJSONArray("themes")
    }
    private val selectedAudioTheme: String get() =
        prefs.getString("audio_theme", "none") ?: "none"
    private var audioPlayer: MediaPlayer? = null
    private var audioPreviewToken = 0
    private var feedbackPlayer: MediaPlayer? = null
    private var feedbackIndex = 0
    private val soundLevel: Float get() = prefs.getInt("audio_volume", 60).coerceIn(0,100)/100f
    private fun rememberHiddenContacts() {
        prefs.edit().putStringSet("hidden_contact_ids", hiddenContacts.ids()).apply()
    }
    private val appearance: String get() = prefs.getString("appearance", "Auto") ?: "Auto"
    private fun night(): Boolean = when (appearance) {
        "Dark" -> true
        "Light" -> false
        else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
    // These tokens come from the corporate CEO-approved 40 paired light/dark themes.
    private val themes by lazy {
        val raw=assets.open("themes.40.approved.json").bufferedReader().use { it.readText() }
        JSONObject(raw).getJSONArray("presets")
    }
    private val themeId: String get() = prefs.getString("theme_id","01") ?: "01"
    private val colors: JSONObject get() {
        var selected=themes.getJSONObject(0)
        for(i in 0 until themes.length()) {
            val candidate=themes.getJSONObject(i)
            if(candidate.getString("id")==themeId) {selected=candidate;break}
        }
        return selected.getJSONObject(if(night()) "dark" else "light")
    }
    private fun themeColor(name:String):Int=Color.parseColor(colors.getString(name))
    private val bg: Int get()=themeColor("background")
    private val surface: Int get()=themeColor("surface")
    private val ink: Int get()=themeColor("onBackground")
    private val muted: Int get()=themeColor("outline")
    private val primary: Int get()=themeColor("primary")
    private fun dp(x: Int) = (x*resources.displayMetrics.density).roundToInt()
    private fun rounded(color: Int, radius: Int=15, border: Int?=null): GradientDrawable = GradientDrawable().apply {
        shape=GradientDrawable.RECTANGLE;setColor(color);cornerRadius=dp(radius).toFloat()
        if (border!=null) setStroke(dp(1),border)
    }
    private fun vertical() = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
    private fun text(value: String, size: Float=15f, bold: Boolean=false, subdued: Boolean=false): TextView =
        TextView(this).apply {
            setText(value); textSize=size; setTextColor(if (subdued) muted else ink)
            if (bold) typeface=Typeface.DEFAULT_BOLD
            setLineSpacing(dp(2).toFloat(),1f)
        }
    private fun button(value: String, action: ()->Unit): Button = Button(this).apply {
        text=value; isAllCaps=false; textSize=13f; minHeight=dp(48)
        setTextColor(themeColor("onPrimary"))
        background=rounded(primary,10)
        setOnClickListener { if (page != Page.AUDIO) playUiFeedback(); action() }
        contentDescription=value
    }
    private fun add(v: LinearLayout, item: View, top:Int=8) {
        v.addView(item,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {topMargin=dp(top)})
    }
    private fun card(parent:LinearLayout, title:String, subtitle:String, body:(LinearLayout)->Unit) {
        val c=vertical().apply { background=rounded(surface,17);setPadding(dp(18),dp(17),dp(18),dp(18)) }
        add(c,text(title,20f,true),0);add(c,text(subtitle,14f,false,true),7);body(c)
        add(parent,c,13)
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        // UI presentation state only: never restore prior remote grants or sessions.
        savedInstanceState?.let { saved ->
            page=Page.values().firstOrNull { it.name==saved.getString("page_name") } ?: Page.CIRCLE
            selectedContactId=saved.getString("local_contact_id")
            selectedContact=saved.getString("local_contact_label")
            showHidden=saved.getBoolean("show_hidden",false)
        }
        render()
    }
    override fun onSaveInstanceState(outState:Bundle) {
        outState.putString("page_name",page.name)
        outState.putString("local_contact_id",selectedContactId)
        outState.putString("local_contact_label",selectedContact)
        outState.putBoolean("show_hidden",showHidden)
        super.onSaveInstanceState(outState)
    }
    private fun render() {
        stopAudioPreview()
        val root=vertical().apply {setBackgroundColor(bg)}
        val header=vertical().apply {setPadding(dp(16),dp(18),dp(16),dp(14));setBackgroundColor(surface)}
        add(header,text("PEOSTECTICA · TAP VOYAGE",12f,true,true),0)
        add(header,text("Tap Voyage",26f,true),5)
        add(header,text("Development build · Not connected to other devices",13f,false,true),7)
        root.addView(header)
        val scroller=ScrollView(this).apply {isFillViewport=true}
        val content=vertical().apply {setPadding(dp(16),dp(12),dp(16),dp(28))}
        when(page) {
            Page.CIRCLE -> showCircle(content)
            Page.GET -> showGet(content)
            Page.GIVE -> showGive(content)
            Page.PREMIUM -> showPremium(content)
            Page.SETTINGS -> showSettings(content)
            Page.AUDIO -> showAudio(content)
        }
        scroller.addView(content)
        root.addView(scroller,LinearLayout.LayoutParams(-1,0,1f))
        val nav=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL;setBackgroundColor(surface);setPadding(dp(8),dp(6),dp(8),dp(6))}
        for (target in Page.values()) {
            val b=button(target.label) {page=target;render()}
            b.contentDescription="Navigate to ${target.label}"
            if(target==page)b.setTypeface(null,Typeface.BOLD)
            nav.addView(b,LinearLayout.LayoutParams(dp(109),dp(52)).apply {marginStart=dp(3);marginEnd=dp(3)})
        }
        root.addView(HorizontalScrollView(this).apply {isHorizontalScrollBarEnabled=false;addView(nav)},LinearLayout.LayoutParams(-1,-2))
        setContentView(root)
    }
    private fun showCircle(c:LinearLayout) {
        add(c,text("My Circle",24f,true),5)
        add(c,text("A Circle contact can exist with zero approved apps. Permissions always belong to the device owner.",14f,false,true),9)
        card(c,"↗ Get Permission","Choose a person, then request only the access that person approves.") {v->
            val selectedId = selectedContactId
            val visible = selectedId != null && hiddenContacts.visible(selectedId, showHidden)
            add(v,text(if (visible) "Selected locally: ${selectedContact.orEmpty()}" else
                if (selectedId != null) "Selected contact hidden locally" else "No verified Circle contacts yet",15f,true),15)
            add(v,button(if (showHidden) "Show Hidden: ON" else "Show Hidden: OFF") {
                showHidden = !showHidden
                render()
            },8)
            if (selectedId != null) {
                if (hiddenContacts.visible(selectedId, false)) {
                    add(v,button("Hide Me") {
                        hiddenContacts.hide(selectedId)
                        rememberHiddenContacts()
                        render()
                    },8)
                } else if (showHidden) {
                    add(v,button("Unhide") {
                        hiddenContacts.unhide(selectedId)
                        rememberHiddenContacts()
                        render()
                    },8)
                }
            }
            add(v,text("Hide Me changes local visibility only. It does not invite contacts, grant access or revoke owner permissions.",13f,false,true),9)
            add(v,button("Choose a phone contact") {chooseContact()},10)
            add(v,button("View Get Permission") {page=Page.GET;render()},8)
        }
        card(c,"↙ Give Permission","You decide which requester can access your device and can revoke at any time.") {v->
            add(v,text("No authenticated incoming permission requests",15f,false,true),13)
            add(v,button("Review Give Permission") {page=Page.GIVE;render()},9)
        }
    }
    private fun showGet(c:LinearLayout) {
        add(c,text("Get Permission",24f,true),5)
        card(c,"Select a person","A contact-list entry is not a device identity or consent.") {v->
            add(v,text(selectedContactId?.let { id ->
                if (hiddenContacts.visible(id,showHidden)) selectedContact else "Contact hidden locally"
            } ?: "No contact selected",16f,true),13)
            add(v,button("Choose contact") {chooseContact()},9)
        }
        card(c,"Authorized and unauthorized apps","Only owner-approved inventory may be disclosed. Free includes the oldest approved app per owner.") {v->
            add(v,text("No remotely verified app inventory available",15f,false,true),13)
            add(v,button("Request Full Device Access") {unsupported("Full-device request requires a verified owner identity and an explicit scope approval. No request was sent.")},10)
        }
    }
    private fun showGive(c:LinearLayout) {
        add(c,text("Give Permission",24f,true),5)
        card(c,"Owner permissions","No one can access apps before accepting a Circle invitation, sharing inventory and approving the specific capability.") {v->
            add(v,text("No authenticated requests received",15f,false,true),13)
            add(v,button("Review permissions") {unsupported("Account/device verification is not connected. No remote grants exist in this app build.")},10)
        }
        card(c,"Revoke access","A revocation must terminate pending and live remote sessions on the server as well as on the owner device.") {v->
            add(v,text("No live session is connected",15f,false,true),12)
        }
    }
    private fun showPremium(c:LinearLayout) {
        add(c,text("Premium",24f,true),5)
        card(c,"✕ Free · not subscribed","One oldest currently authorized app per approved Circle owner. Additional permissions remain stored but locked.") {v->
            add(v,text("$1.99 / week",23f,true),14)
            add(v,text("$4.99 / month",17f,false,true),7)
            add(v,button("Get Premium") {unsupported("Google Play Billing is not configured; no purchase was started and no charge was made.")},12)
        }
    }
    private fun showSettings(c:LinearLayout) {
        add(c,text("Settings",24f,true),5)
        card(c,"Display preferences","Appearance is local to this client, never an owner-device permission.") {v->
            add(v,text("Selected: $appearance",15f,true),12)
            val row=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL}
            for (choice in listOf("Auto","Light","Dark")) {
                row.addView(button(choice) {prefs.edit().putString("appearance",choice).apply();render()},LinearLayout.LayoutParams(0,dp(52),1f).apply {marginEnd=dp(4)})
            }
            add(v,row,9)
        }
        card(c,"Display themes · 40 choices","All 40 preset pairs follow your selected Auto/Light/Dark appearance.") { v ->
            val ids=(0 until themes.length()).map { themes.getJSONObject(it).getString("id") }
            val names=(0 until themes.length()).map {
                val theme=themes.getJSONObject(it)
                theme.getString("id") + " · " + theme.getString("name")
            }
            val spinner=Spinner(this).apply {
                adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,names)
                setSelection(ids.indexOf(themeId).coerceAtLeast(0))
                onItemSelectedListener=object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent:AdapterView<*>?) {}
                    override fun onItemSelected(parent:AdapterView<*>?, view:View?, position:Int, id:Long) {
                        val next=ids[position]
                        if(next!=themeId) {prefs.edit().putString("theme_id",next).apply();render()}
                    }
                }
            }
            spinner.contentDescription="Select one of 40 display color themes"
            add(v,spinner,11)
        }
        card(c,"Original audio themes · 20 choices","Preview the approved original 12-cue themes or turn sound effects off.") {v ->
            add(v,text("Selection: " + selectedAudioTheme,15f,true),10)
            add(v,button("Open Audio Settings") {page=Page.AUDIO;render()},9)
        }
        card(c,"Privacy and security","No contacts are uploaded, no remote sessions are initiated, and no payment is collected by this development app.") { }
    }
    private fun playUiFeedback() {
        if (selectedAudioTheme == "none" || audioPlayer != null) return
        val am=getSystemService(AUDIO_SERVICE) as AudioManager
        if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL || soundLevel<=0f) return
        try {
            val theme=(0 until audioThemes.length()).map { audioThemes.getJSONObject(it) }
                .firstOrNull { it.getString("id")==selectedAudioTheme } ?: return
            val sounds=theme.getJSONArray("sounds")
            val sample=sounds.getJSONObject(feedbackIndex % sounds.length()).getString("filename")
            feedbackIndex=(feedbackIndex+1)%sounds.length()
            feedbackPlayer?.release()
            feedbackPlayer=null
            val fd=assets.openFd("audio/sounds/" + sample)
            val player=MediaPlayer()
            feedbackPlayer=player
            try {
                player.setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                player.setDataSource(fd.fileDescriptor,fd.startOffset,fd.length)
                player.setVolume(soundLevel,soundLevel)
            } finally { fd.close() }
            player.setOnCompletionListener { finished ->
                if (feedbackPlayer === finished) feedbackPlayer=null
                finished.release()
            }
            player.prepare()
            player.start()
        } catch (_: Exception) {
            feedbackPlayer?.release()
            feedbackPlayer=null
        }
    }
    private fun stopAudioPreview() {
        audioPreviewToken++
        audioPlayer?.let { player ->
            try { player.stop() } catch (_: IllegalStateException) { }
            player.release()
        }
        audioPlayer = null
    }
    private fun startAudioPreview() {
        stopAudioPreview()
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            android.widget.Toast.makeText(this, "Silent mode: audio preview is muted", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val id=selectedAudioTheme
        if (id=="none") return
        val item=(0 until audioThemes.length())
            .map { audioThemes.getJSONObject(it) }
            .firstOrNull { it.getString("id")==id } ?: return
        val sounds=item.getJSONArray("sounds")
        val token=audioPreviewToken
        val handler=Handler(Looper.getMainLooper())
        fun playNext(index:Int) {
            if (token != audioPreviewToken || isFinishing || isDestroyed) return
            try {
                val filename=sounds.getJSONObject(index % sounds.length()).getString("filename")
                val fd=assets.openFd("audio/sounds/" + filename)
                val player=MediaPlayer()
                audioPlayer=player
                try {
                    player.setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    player.setDataSource(fd.fileDescriptor,fd.startOffset,fd.length)
                    player.setVolume(soundLevel,soundLevel)
                } finally { fd.close() }
                player.setOnCompletionListener { finished ->
                    if (audioPlayer === finished) audioPlayer=null
                    finished.release()
                    if (token==audioPreviewToken) handler.postDelayed({playNext(index+1)},160)
                }
                audioPlayer=player
                player.prepare()
                player.start()
            } catch (_: Exception) {
                stopAudioPreview()
                android.widget.Toast.makeText(this@MainActivity,
                    "Approved audio sample unavailable",android.widget.Toast.LENGTH_LONG).show()
            }
        }
        playNext(0)
    }
    override fun onPause() {
        stopAudioPreview()
        feedbackPlayer?.release()
        feedbackPlayer=null
        super.onPause()
    }
    override fun onDestroy() {
        stopAudioPreview()
        feedbackPlayer?.release()
        feedbackPlayer=null
        super.onDestroy()
    }
    private fun showAudio(c:LinearLayout) {
        add(c,text("Audio settings",24f,true),5)
        add(c,text("Choose one of 20 original Peostectica sound themes. None is the default. Preview plays the theme's 12 cues until stopped or the app is backgrounded.",14f,false,true),9)
        card(c,"Sound theme","Music and audio previews respect device silent mode.") {v ->
            val labels=mutableListOf("None · Sound effects off")
            val ids=mutableListOf("none")
            for (i in 0 until audioThemes.length()) {
                val theme=audioThemes.getJSONObject(i)
                ids.add(theme.getString("id"))
                labels.add(theme.getString("id")+" · "+theme.getString("name"))
            }
            val selector=Spinner(this).apply {
                adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,labels)
                setSelection(ids.indexOf(selectedAudioTheme).coerceAtLeast(0))
                onItemSelectedListener=object: AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent:AdapterView<*>?) { }
                    override fun onItemSelected(parent:AdapterView<*>?, view:View?, position:Int, id:Long) {
                        if (ids[position]!=selectedAudioTheme) {
                            prefs.edit().putString("audio_theme",ids[position]).apply()
                            render()
                        }
                    }
                }
                contentDescription="Select one of 20 original sound themes or None"
            }
            add(v,selector,10)
            add(v,text("Sound effect volume · "+prefs.getInt("audio_volume",60).coerceIn(0,100)+"%",14f),12)
            val volumeSlider=SeekBar(this).apply {
                max=100
                progress=prefs.getInt("audio_volume",60).coerceIn(0,100)
                contentDescription="Audio effect volume"
                setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(view:SeekBar?, value:Int, user:Boolean) {
                        if (user) {
                            prefs.edit().putInt("audio_volume",value).apply()
                            audioPlayer?.setVolume(soundLevel,soundLevel)
                        }
                    }
                    override fun onStartTrackingTouch(view:SeekBar?) {}
                    override fun onStopTrackingTouch(view:SeekBar?) {}
                })
            }
            add(v,volumeSlider,8)
            add(v,button("Play / stop 12-cue preview") {
                if (audioPlayer!=null) stopAudioPreview() else startAudioPreview()
            },10)
        }
    }

    private fun unsupported(message:String) {
        AlertDialog.Builder(this).setTitle("Not connected to production")
            .setMessage(message).setPositiveButton("OK",null).show()
    }
    private fun chooseContact() {
        val intent=Intent(Intent.ACTION_PICK,ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        try {startActivityForResult(intent,1001)}
        catch (_:Exception) {unsupported("No compatible local contact picker is available.")}
    }
    @Deprecated("Uses system picker to grant access to one selected contact")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode!=1001 || resultCode!=RESULT_OK)return
        val uri=data?.data ?: return
        try {
            contentResolver.query(uri,arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),null,null,null)?.use { cursor ->
                if(cursor.moveToFirst()) {
                    val id = cursor.getString(0)?.trim()
                    val name = cursor.getString(1)?.trim()?.take(120)
                    if (!id.isNullOrBlank() && !name.isNullOrBlank()) {
                        selectedContactId = id
                        selectedContact = name
                    }
                }
            }
            render()
        } catch (_:Exception) {unsupported("Selected contact could not be read; no information was saved.")}
    }
}

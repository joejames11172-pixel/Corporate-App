package com.peostectica.tapvoyage

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
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
import android.widget.TextView
import com.peostectica.tapvoyage.core.TapVoyagePolicy
import kotlin.math.roundToInt

/** Native development client; no simulated contact is represented as authenticated. */
class MainActivity : Activity() {
    private enum class Page(val label: String) {
        CIRCLE("My Circle"), GET("Get Permission"), GIVE("Give Permission"), PREMIUM("Premium"), SETTINGS("Settings")
    }
    private val prefs by lazy { getSharedPreferences("tapvoyage_ui", MODE_PRIVATE) }
    private var page = Page.CIRCLE
    private var selectedContact: String? = null // in-memory only, no unapproved contact history
    private val policy = TapVoyagePolicy() // offline demo policy; never authorizes a remote connection
    private val appearance: String get() = prefs.getString("appearance", "Auto") ?: "Auto"
    private fun night(): Boolean = when (appearance) {
        "Dark" -> true
        "Light" -> false
        else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
    private val bg: Int get() = if (night()) Color.rgb(9,18,35) else Color.rgb(245,247,255)
    private val surface: Int get() = if (night()) Color.rgb(23,36,58) else Color.WHITE
    private val ink: Int get() = if (night()) Color.rgb(240,245,255) else Color.rgb(28,37,59)
    private val muted: Int get() = if (night()) Color.rgb(172,187,215) else Color.rgb(86,100,127)
    private val primary: Int get() = Color.rgb(55,86,235)
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
        setTextColor(if (night()) Color.WHITE else primary)
        background=rounded(if (night()) Color.rgb(33,49,87) else Color.rgb(230,236,255),10)
        setOnClickListener {action()}
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
    override fun onCreate(savedInstanceState:Bundle?) {super.onCreate(savedInstanceState); render()}
    private fun render() {
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
            add(v,text(selectedContact?.let {"Selected locally: $it"} ?: "No verified Circle contacts yet",15f,true),15)
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
            add(v,text(selectedContact ?: "No contact selected",16f,true),13)
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
        card(c,"Privacy and security","No contacts are uploaded, no remote sessions are initiated, and no payment is collected by this development app.") { }
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
            contentResolver.query(uri,arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),null,null,null)?.use { cursor ->
                if(cursor.moveToFirst())selectedContact=cursor.getString(0)?.take(120)
            }
            render()
        } catch (_:Exception) {unsupported("Selected contact could not be read; no information was saved.")}
    }
}

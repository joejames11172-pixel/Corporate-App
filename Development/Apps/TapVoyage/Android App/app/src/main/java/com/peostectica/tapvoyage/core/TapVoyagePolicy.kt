package com.peostectica.tapvoyage.core

/** Pure TapVoyage access policy. NOT authentication, billing or an Android remote-control service.
 * Production adapters MUST verify actor/device identity, entitlements and revocation durably. */
class TapVoyagePolicy(private val paidEntitlement: (String)->Boolean = { false },
                      private val clock: ()->Long = System::currentTimeMillis) {
    private enum class Link { PENDING, ACCEPTED, REVOKED }
    private data class Grant(val app: String, val id: Long)
    private data class Circle(var link: Link=Link.PENDING, var inventory: Boolean=false,
       var fullAccessRequested: Boolean=false, var fullAccessApproved: Boolean=false,
       val pending: MutableSet<String> = mutableSetOf(), val grants: LinkedHashMap<String,Grant> = linkedMapOf())
    private data class Session(val id: Long, val requester: String, val owner: String,
       val grant: Grant, val expires: Long, var state: String="PENDING")
    private val circles=mutableMapOf<Pair<String,String>,Circle>()
    private val sessions=mutableMapOf<Long,Session>()
    private var sequence=0L
    private fun key(requester:String,owner:String):Pair<String,String> {
        require(requester.isNotBlank() && owner.isNotBlank() && requester!=owner)
        return requester to owner
    }
    private fun link(requester:String,owner:String):Circle {
        val c=circles[key(requester,owner)]
        check(c!=null && c.link==Link.ACCEPTED) { "Owner has not accepted Circle" }
        return c
    }
    fun invite(requester:String,owner:String) {
        val k=key(requester,owner)
        check(circles[k]?.link !in listOf(Link.ACCEPTED,Link.PENDING)) { "Invitation already exists" }
        circles[k]=Circle()
    }
    fun ownerAccept(owner:String,requester:String) {
        val c=circles[key(requester,owner)]
        check(c?.link==Link.PENDING) { "No pending invitation" }
        c!!.link=Link.ACCEPTED
    }
    fun ownerApproveInventory(owner:String,requester:String) { link(requester,owner).inventory=true }
    fun inventoryVisible(requester:String,owner:String):Boolean =
        circles[key(requester,owner)]?.let { it.link==Link.ACCEPTED && it.inventory } ?: false
    fun requestApp(requester:String,owner:String,app:String) {
        val c=link(requester,owner)
        check(c.inventory && app.isNotBlank() && app !in c.grants) { "Not disclosed or already granted" }
        c.pending.add(app)
    }
    fun ownerApproveApp(owner:String,requester:String,app:String) {
        val c=link(requester,owner)
        check(c.pending.remove(app)) { "No pending app request" }
        c.grants[app]=Grant(app,++sequence)
    }
    /** Broad owner approval is NOT an Android OS permission, root, premium, or secret inventory grant. */
    fun requestFullDevice(requester:String,owner:String) {
        val c=link(requester,owner)
        check(c.inventory) { "Owner must approve inventory visibility first" }
        c.fullAccessRequested=true
    }
    fun ownerApproveFullDevice(owner:String,requester:String) {
        val c=link(requester,owner)
        check(c.fullAccessRequested) { "No pending full-device request" }
        c.fullAccessRequested=false;c.fullAccessApproved=true
    }
    fun fullDeviceApproved(requester:String,owner:String):Boolean =
        circles[key(requester,owner)]?.let {it.link==Link.ACCEPTED && it.fullAccessApproved} ?: false
    data class VisibleApp(val name:String,val permitted:Boolean,val order:Long)
    fun listApps(requester:String,owner:String):List<VisibleApp> {
        val grants=link(requester,owner).grants.values.toList()
        val paid=paidEntitlement(requester) // must be injected from authoritative verifier
        return grants.mapIndexed {index,g->VisibleApp(g.app,paid || index==0,g.id)}
    }
    private fun available(requester:String,owner:String,g:Grant):Boolean = try {
        link(requester,owner).grants[g.app]?.id==g.id &&
            listApps(requester,owner).any {it.name==g.app && it.order==g.id && it.permitted}
    } catch (_:IllegalStateException) {false}
    private fun invalidate(requester:String,owner:String,force:Boolean=false) {
        for (s in sessions.values) if(s.requester==requester && s.owner==owner && s.state!="STOPPED") {
            if(force || !available(requester,owner,s.grant)) s.state="STOPPED"
        }
    }
    fun entitlementChanged(requester:String) {
        for(k in circles.keys.filter {it.first==requester}) invalidate(k.first,k.second)
    }
    fun removeOwnApp(requester:String,owner:String,app:String) {
        check(link(requester,owner).grants.remove(app)!=null) { "Grant missing" }
        invalidate(requester,owner)
    }
    fun ownerRevokeApp(owner:String,requester:String,app:String) {
        val c=link(requester,owner)
        val removed=c.grants.remove(app)!=null || c.pending.remove(app)
        check(removed) { "Permission missing" }
        c.pending.remove(app)
        invalidate(requester,owner)
    }
    fun ownerRevokeFullDevice(owner:String,requester:String) {
        val c=link(requester,owner)
        c.fullAccessRequested=false;c.fullAccessApproved=false
        invalidate(requester,owner,true)
    }
    fun ownerRevokeRequester(owner:String,requester:String) {
        val c=link(requester,owner)
        c.link=Link.REVOKED;c.inventory=false;c.fullAccessRequested=false;c.fullAccessApproved=false
        c.pending.clear();c.grants.clear()
        invalidate(requester,owner,true)
    }
    fun requestSession(requester:String,owner:String,app:String,ttl:Long=300000):Long {
        require(ttl in 1..3600000)
        val grant=link(requester,owner).grants[app]
        check(grant!=null && available(requester,owner,grant)) {"No authorized app or entitlement"}
        val id=++sequence
        sessions[id]=Session(id,requester,owner,grant,clock()+ttl)
        return id
    }
    fun ownerAcceptSession(owner:String,id:Long) {
        val s=sessions[id]
        check(s!=null && s.owner==owner && s.state=="PENDING") {"No owner-controlled pending session"}
        if(clock()>=s.expires || !available(s.requester,s.owner,s.grant)) {
            s.state="STOPPED";error("Expired or revoked before session approval")
        }
        s.state="ACTIVE"
    }
    fun sessionAllowed(id:Long):Boolean {
        val s=sessions[id]?:return false
        if(s.state!="ACTIVE")return false
        if(clock()>=s.expires || !available(s.requester,s.owner,s.grant)) {
            s.state="STOPPED";return false
        }
        return true
    }
}

/** Local-only UI preference; never changes granted device authority. */
class HiddenContacts {
    private val hidden=mutableSetOf<String>()
    fun hide(id:String) {hidden.add(id)}
    fun unhide(id:String) {hidden.remove(id)}
    fun visible(id:String,showHidden:Boolean=false)=showHidden || id !in hidden
}

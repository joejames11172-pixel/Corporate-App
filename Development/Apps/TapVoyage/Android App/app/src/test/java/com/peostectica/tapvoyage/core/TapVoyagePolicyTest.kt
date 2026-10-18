package com.peostectica.tapvoyage.core
fun main() {
 var count=0
 fun test(name:String,check:()->Boolean) {require(check()) {name};count++;println("PASS: $name")}
 fun denied(action:()->Unit):Boolean = try {action();false} catch(_:IllegalStateException){true}
 var premium=false;var time=10000L
 val p=TapVoyagePolicy({premium},{time})
 p.invite("Alice","Bob")
 test("Invitation not permission") { denied{p.listApps("Alice","Bob")} }
 p.ownerAccept("Bob","Alice")
 test("Circle may contain zero apps") {p.listApps("Alice","Bob").isEmpty()}
 test("Host must disclose inventory") {denied{p.requestApp("Alice","Bob","A")}}
 p.ownerApproveInventory("Bob","Alice")
 test("Inventory disclosure explicit") {p.inventoryVisible("Alice","Bob")}
 for (a in listOf("A","B","C")) {p.requestApp("Alice","Bob",a);p.ownerApproveApp("Bob","Alice",a)}
 test("Free only oldest") {p.listApps("Alice","Bob").filter{it.permitted}.map{it.name}==listOf("A")}
 test("Free extra session denied") {denied{p.requestSession("Alice","Bob","B")}}
 premium=true
 test("Verified premium unlocks approved apps") {p.listApps("Alice","Bob").all{it.permitted}}
 val id=p.requestSession("Alice","Bob","B")
 test("Session awaits owner") {!p.sessionAllowed(id)}
 p.ownerAcceptSession("Bob",id)
 test("Session active only after owner") {p.sessionAllowed(id)}
 premium=false;p.entitlementChanged("Alice")
 test("Premium expiry terminates live extra") {!p.sessionAllowed(id)}
 val exp=p.requestSession("Alice","Bob","A",100)
 p.ownerAcceptSession("Bob",exp);time+=101
 test("Expired session denied") {!p.sessionAllowed(exp)}
 p.removeOwnApp("Alice","Bob","A")
 test("Removing first promotes next") {p.listApps("Alice","Bob").filter{it.permitted}.map{it.name}==listOf("B")}
 val pending=p.requestSession("Alice","Bob","B")
 p.ownerRevokeApp("Bob","Alice","B")
 test("Revoke invalidates pending") {denied{p.ownerAcceptSession("Bob",pending)}}
 p.requestApp("Alice","Bob","B");p.ownerApproveApp("Bob","Alice","B")
 test("Reapproved app is newest") {p.listApps("Alice","Bob").map{it.name}==listOf("C","B")}
 p.requestFullDevice("Alice","Bob")
 test("Full access request not automatic") {!p.fullDeviceApproved("Alice","Bob")}
 p.ownerApproveFullDevice("Bob","Alice")
 test("Full access owner approved") {p.fullDeviceApproved("Alice","Bob")}
 p.ownerRevokeFullDevice("Bob","Alice")
 test("Full access owner revoked") {!p.fullDeviceApproved("Alice","Bob")}
 val hidden=HiddenContacts();hidden.hide("Bob")
 test("Hide is local presentation") {!hidden.visible("Bob") && hidden.visible("Bob",true) && p.listApps("Alice","Bob").size==2}
 hidden.unhide("Bob")
 test("Unhide restores visibility") {hidden.visible("Bob")}
 p.ownerRevokeRequester("Bob","Alice")
 test("Revoke user removes grants/inventory") {denied{p.listApps("Alice","Bob")} && !p.inventoryVisible("Alice","Bob")}
 p.invite("Alice","Bob")
 test("Reinvite requires fresh acceptance") {denied{p.listApps("Alice","Bob")}}
 p.ownerAccept("Bob","Alice")
 test("Rejoin does not restore stale grants") {p.listApps("Alice","Bob").isEmpty()}
 val unavailable=TapVoyagePolicy({ throw IllegalStateException("Billing backend unavailable") })
 unavailable.invite("Carol","Dave");unavailable.ownerAccept("Dave","Carol")
 unavailable.ownerApproveInventory("Dave","Carol")
 for (app in listOf("A","B")) { unavailable.requestApp("Carol","Dave",app);unavailable.ownerApproveApp("Dave","Carol",app) }
 test("Verifier offline never unlocks Premium extras") {unavailable.listApps("Carol","Dave").filter{it.permitted}.map{it.name}==listOf("A")}
 test("Verifier offline rejects extra session") {denied{unavailable.requestSession("Carol","Dave","B")}}
 val equalNames = HiddenContacts()
 equalNames.hide("contact-id-101")
 test("Hide by local contact identity, not duplicate display name") {
     !equalNames.visible("contact-id-101") && equalNames.visible("contact-id-102")
 }
 test("Show Hidden is view-only and does not alter remote grants") {
     equalNames.visible("contact-id-101", true) && p.listApps("Alice", "Bob").isEmpty()
 }
 equalNames.unhide("contact-id-101")
 test("Unhide restores selected local contact") { equalNames.visible("contact-id-101") }
 val local = HiddenContacts(setOf("local-id-1", ""))
 test("Restored hidden IDs exclude blanks") {local.ids()==setOf("local-id-1")}
 test("Restored hidden contact remains hidden") {!local.visible("local-id-1")}
 local.hide("local-id-2")
 val snapshot=local.ids()
 test("Snapshot contains only local IDs") {snapshot==setOf("local-id-1","local-id-2")}
 local.unhide("local-id-1")
 test("Unhide updates persisted snapshot") {local.ids()==setOf("local-id-2")}
 test("Visibility state leaves owner authorization untouched") {p.listApps("Alice","Bob").isEmpty()}
 val separate=TapVoyagePolicy()
 separate.invite("Cameron","Morgan")
 separate.ownerAccept("Morgan","Cameron")
 separate.ownerApproveInventory("Morgan","Cameron")
 separate.requestApp("Cameron","Morgan","PhotoViewer")
 separate.ownerApproveApp("Morgan","Cameron","PhotoViewer")
 val individualId=separate.requestSession("Cameron","Morgan","PhotoViewer")
 separate.ownerAcceptSession("Morgan",individualId)
 separate.requestFullDevice("Cameron","Morgan")
 separate.ownerApproveFullDevice("Morgan","Cameron")
 separate.ownerRevokeFullDevice("Morgan","Cameron")
 test("Revoke broad scope preserves independent per-app session") {separate.sessionAllowed(individualId)}
 test("Revoke broad scope removes broad authorization") {!separate.fullDeviceApproved("Cameron","Morgan")}
 separate.ownerRevokeRequester("Morgan","Cameron")
 test("Revoke requester still stops independently authorized app") {!separate.sessionAllowed(individualId)}
 var broadPaid=false
 val broad=TapVoyagePolicy({broadPaid})
 broad.invite("Drew","Alexis");broad.ownerAccept("Alexis","Drew")
 broad.ownerApproveInventory("Alexis","Drew")
 broad.requestFullDevice("Drew","Alexis")
 broad.ownerApproveFullDevice("Alexis","Drew")
 test("Owner broad approval does not bypass Free gate") {
     broad.fullDeviceApproved("Drew","Alexis") && !broad.fullDeviceScopeEligible("Drew","Alexis")
 }
 broadPaid=true
 test("Premium + owner broad approval establishes scope eligibility") {
     broad.fullDeviceScopeEligible("Drew","Alexis")
 }
 broadPaid=false
 test("Premium expiry disables broad scope immediately") {
     !broad.fullDeviceScopeEligible("Drew","Alexis")
 }
 broad.ownerRevokeFullDevice("Alexis","Drew")
 broadPaid=true
 test("Owner revocation prevents broad scope even for Premium") {
     !broad.fullDeviceScopeEligible("Drew","Alexis")
 }
 val broadUnavailable=TapVoyagePolicy({ throw IllegalStateException("entitlement verifier offline") })
 broadUnavailable.invite("Drew","Alexis");broadUnavailable.ownerAccept("Alexis","Drew")
 broadUnavailable.ownerApproveInventory("Alexis","Drew")
 broadUnavailable.requestFullDevice("Drew","Alexis")
 broadUnavailable.ownerApproveFullDevice("Alexis","Drew")
 test("Premium verifier outage blocks broad scope") {
     !broadUnavailable.fullDeviceScopeEligible("Drew","Alexis")
 }
 var scopePremium=false
 var scopeTime=20000L
 val scope=TapVoyagePolicy({scopePremium},{scopeTime})
 scope.invite("Requester","Owner");scope.ownerAccept("Owner","Requester")
 scope.ownerApproveInventory("Owner","Requester")
 scope.requestApp("Requester","Owner","Maps")
 scope.ownerApproveApp("Owner","Requester","Maps")
 scope.requestFullDevice("Requester","Owner")
 scope.ownerApproveFullDevice("Owner","Requester")
 test("Free cannot request broad session despite host approval") {
     denied { scope.requestFullDeviceSession("Requester","Owner") }
 }
 scopePremium=true
 val broadSession=scope.requestFullDeviceSession("Requester","Owner")
 test("Broad session awaits fresh owner confirmation") { !scope.sessionAllowed(broadSession) }
 scope.ownerAcceptSession("Owner",broadSession)
 test("Owner accepts broad session with fresh entitlement") { scope.sessionAllowed(broadSession) }
 val individualSession=scope.requestSession("Requester","Owner","Maps")
 scope.ownerAcceptSession("Owner",individualSession)
 scope.ownerRevokeFullDevice("Owner","Requester")
 test("Broad revocation stops derived broad session") { !scope.sessionAllowed(broadSession) }
 test("Broad revocation preserves independent Maps session") { scope.sessionAllowed(individualSession) }
 test("Cannot request new broad session after revoke") {
     denied { scope.requestFullDeviceSession("Requester","Owner") }
 }
 scope.ownerRevokeRequester("Owner","Requester")
 test("Requester revoke also stops independent session") { !scope.sessionAllowed(individualSession) }
 val expiry=TapVoyagePolicy({scopePremium},{scopeTime})
 expiry.invite("Req2","Owner2");expiry.ownerAccept("Owner2","Req2")
 expiry.ownerApproveInventory("Owner2","Req2")
 expiry.requestFullDevice("Req2","Owner2")
 expiry.ownerApproveFullDevice("Owner2","Req2")
 val pendingBroad=expiry.requestFullDeviceSession("Req2","Owner2",100L)
 scopeTime+=101L
 test("Expired broad session cannot be accepted") { denied { expiry.ownerAcceptSession("Owner2",pendingBroad) } }
 val expiringBroad=expiry.requestFullDeviceSession("Req2","Owner2",100L)
 expiry.ownerAcceptSession("Owner2",expiringBroad)
 scopeTime+=101L
 test("Active broad session expires on TTL") { !expiry.sessionAllowed(expiringBroad) }
 val premiumExpiredBroad=expiry.requestFullDeviceSession("Req2","Owner2")
 expiry.ownerAcceptSession("Owner2",premiumExpiredBroad)
 scopePremium=false
 test("Broad session immediately denied when Premium expires") { !expiry.sessionAllowed(premiumExpiredBroad) }
 expiry.entitlementChanged("Req2")
 test("Broad session stays stopped after entitlement refresh") { !expiry.sessionAllowed(premiumExpiredBroad) }
 val failBroad=TapVoyagePolicy({ throw IllegalStateException("verifier down") })
 failBroad.invite("Requester","Owner");failBroad.ownerAccept("Owner","Requester")
 failBroad.ownerApproveInventory("Owner","Requester")
 failBroad.requestFullDevice("Requester","Owner")
 failBroad.ownerApproveFullDevice("Owner","Requester")
 test("Verifier failure blocks all broad sessions") { denied { failBroad.requestFullDeviceSession("Requester","Owner") } }
 val decline=TapVoyagePolicy({true})
 decline.invite("Decliner","Host")
 decline.ownerDeclineCircle("Host","Decliner")
 test("Denied Circle cannot be accepted without reinvitation") { denied { decline.ownerAccept("Host","Decliner") } }
 test("Denied Circle cannot request apps") { denied { decline.requestApp("Decliner","Host","Maps") } }
 decline.invite("Decliner","Host")
 test("Reinvite is pending, not automatically accepted") { denied { decline.ownerApproveInventory("Host","Decliner") } }
 decline.ownerAccept("Host","Decliner")
 decline.ownerApproveInventory("Host","Decliner")
 decline.requestApp("Decliner","Host","Maps")
 decline.ownerApproveApp("Host","Decliner","Maps")
 val declineAppId=decline.requestSession("Decliner","Host","Maps")
 decline.ownerAcceptSession("Host",declineAppId)
 decline.requestFullDevice("Decliner","Host")
 test("Duplicate Full Device Access request denied") { denied { decline.requestFullDevice("Decliner","Host") } }
 decline.ownerDeclineFullDevice("Host","Decliner")
 test("Decline broad request preserves independent app session") { decline.sessionAllowed(declineAppId) }
 test("Decline broad request prevents broad session") { denied { decline.requestFullDeviceSession("Decliner","Host") } }
 decline.requestFullDevice("Decliner","Host")
 decline.ownerApproveFullDevice("Host","Decliner")
 val inventoryBroadId=decline.requestFullDeviceSession("Decliner","Host")
 decline.ownerAcceptSession("Host",inventoryBroadId)
 decline.ownerRevokeInventory("Host","Decliner")
 test("Inventory revoke stops broad and per-app active sessions") {
     !decline.sessionAllowed(declineAppId) && !decline.sessionAllowed(inventoryBroadId)
 }
 test("Inventory revoke clears app grants and visibility") {
     !decline.inventoryVisible("Decliner","Host") && decline.listApps("Decliner","Host").isEmpty()
 }
 test("Inventory revoke blocks pending broad request") { denied { decline.requestFullDeviceSession("Decliner","Host") } }
 decline.ownerApproveInventory("Host","Decliner")
 test("Restored inventory permission does not resurrect app grants") {
     decline.listApps("Decliner","Host").isEmpty()
 }
 val bound=TapVoyagePolicy()
 bound.invite("ScopeRequester","ScopeOwner")
 bound.ownerAccept("ScopeOwner","ScopeRequester")
 bound.ownerApproveInventory("ScopeOwner","ScopeRequester")
 bound.requestApp("ScopeRequester","ScopeOwner","Calculator")
 bound.ownerApproveApp("ScopeOwner","ScopeRequester","Calculator")
 val boundSession=bound.requestSession("ScopeRequester","ScopeOwner","Calculator")
 bound.ownerAcceptSession("ScopeOwner",boundSession)
 test("Requester can query only their own authorized session") {
     bound.sessionAllowedForRequester("ScopeRequester",boundSession)
 }
 test("Guessing another requester's session ID never grants access") {
     !bound.sessionAllowedForRequester("OtherUser",boundSession)
 }
 test("Unknown session ID fails closed for real requester") {
     !bound.sessionAllowedForRequester("ScopeRequester",Long.MAX_VALUE)
 }
 println("SUCCESS $count Kotlin domain tests")
}

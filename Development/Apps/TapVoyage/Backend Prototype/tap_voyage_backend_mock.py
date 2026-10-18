"""Tap Voyage in-memory Google Play + Circle authorization simulator.

Python 3.4 compatible. No Google API calls, credentials, networking or persistence.
The host must approve each Circle invitation, each app and each new session.
Never use these mock entitlements in a public deployment.
"""
from __future__ import print_function
import json
import uuid


class AuthorizationError(Exception):
    pass


class MockGooglePlayAPI(object):
    """Server-side emulator of a verified Google Play entitlement response."""
    def __init__(self):
        self._premium = {}

    def set_premium_for_test(self, requester_id, isPremium):
        if type(isPremium) is not bool:
            raise ValueError('isPremium must be a boolean')
        self._premium[requester_id] = isPremium

    def get_subscription(self, requester_id):
        # False by default, including any account without verification.
        return {'source': 'MOCK_GOOGLE_PLAY', 'userId': requester_id,
                'isPremium': self._premium.get(requester_id, False)}


class TapVoyageMockBackend(object):
    """In-memory prototype only. All access checks are server-side."""
    def __init__(self, play_api):
        self.play_api = play_api
        self.circle = {}     # (requester, host) => status, app grants, pending requests
        self.sessions = {}   # session_id => owner/requester/app/status
        self._sequence = 0
        self.audit = []

    def _event(self, action, **values):
        # Avoid secrets, contacts and purchase tokens in audit logs.
        event = {'event': action}
        event.update(values)
        self.audit.append(event)

    def _pair(self, requester, host):
        if not requester or not host or requester == host:
            raise AuthorizationError('distinct requester and host required')
        return (requester, host)

    def _accepted(self, requester, host):
        p = self.circle.get(self._pair(requester, host))
        if not p or p['status'] != 'accepted':
            raise AuthorizationError('host has not accepted this Circle relationship')
        return p

    def invite(self, requester, host):
        key = self._pair(requester, host)
        previous = self.circle.get(key)
        if previous and previous['status'] in ('pending', 'accepted'):
            raise AuthorizationError('invitation already exists')
        # Revoked pairs can rejoin only after a new explicit invitation and approval.
        self.circle[key] = {'status': 'pending', 'grants': {}, 'pendingApps': set()}
        self._event('circle_invitation', requester=requester, host=host)

    def host_accept_circle(self, host, requester):
        p = self.circle.get(self._pair(requester, host))
        if not p or p['status'] != 'pending':
            raise AuthorizationError('no pending invitation for this host')
        p['status'] = 'accepted'
        self._event('circle_accepted', requester=requester, host=host)

    def request_app(self, requester, host, app_id):
        p = self._accepted(requester, host)
        if not app_id:
            raise AuthorizationError('app identifier required')
        if app_id in p['grants']:
            raise AuthorizationError('app already approved')
        p['pendingApps'].add(app_id)
        self._event('app_requested', requester=requester, host=host, app=app_id)

    def host_approve_app(self, host, requester, app_id):
        p = self._accepted(requester, host)
        if app_id not in p['pendingApps']:
            raise AuthorizationError('no outstanding app permission request')
        p['pendingApps'].remove(app_id)
        self._sequence += 1
        p['grants'][app_id] = {'appId': app_id, 'authorizationOrder': self._sequence,
                                'grantId': str(uuid.uuid4())}
        self._event('app_approved', requester=requester, host=host,
                    app=app_id, order=self._sequence)

    def _ordered(self, p):
        return sorted(p['grants'].values(),
                      key=lambda x: (x['authorizationOrder'], x['grantId']))

    def _verified_premium(self, requester):
        # MOCK-only contract. Never trust raw client booleans or truthy strings.
        # Production must replace this adapter with server-verified Play receipts.
        try:
            receipt = self.play_api.get_subscription(requester)
            return (type(receipt) is dict and
                    receipt.get('source') == 'MOCK_GOOGLE_PLAY' and
                    receipt.get('userId') == requester and
                    receipt.get('isPremium') is True)
        except Exception:
            # Outage, malformed receipt, or unverifiable identity = Free.
            return False

    def listed_apps(self, requester, host):
        p = self._accepted(requester, host)
        isPremium = self._verified_premium(requester)
        grants = self._ordered(p)
        return [{'appId': g['appId'], 'authorizationOrder': g['authorizationOrder'],
                 'grantId': g['grantId'],
                 'enabled': bool(isPremium or i == 0),
                 'reason': 'allowed' if isPremium or i == 0 else 'premium_required'}
                for i, g in enumerate(grants)]

    def _stop_invalid_sessions(self, requester=None, host=None):
        for sid, sess in self.sessions.items():
            if sess['status'] not in ('active', 'pending_host_approval'):
                continue
            if requester is not None and sess['requester'] != requester:
                continue
            if host is not None and sess['host'] != host:
                continue
            try:
                enabled = any(app['appId'] == sess['appId'] and app['enabled']
                              and app['grantId'] == sess['grantId']
                              for app in self.listed_apps(sess['requester'], sess['host']))
            except AuthorizationError:
                enabled = False
            if not enabled:
                sess['status'] = 'terminated'
                self._event('session_terminated', session=sid, reason='access_removed')

    def refresh_entitlement(self, requester):
        # In production this follows a verified server-side Play API refresh.
        self._stop_invalid_sessions(requester=requester)
        self._event('subscription_checked', requester=requester,
                    isPremium=self._verified_premium(requester))

    def remove_my_app(self, requester, host, app_id):
        p = self._accepted(requester, host)
        if app_id not in p['grants']:
            raise AuthorizationError('app not authorized')
        del p['grants'][app_id]
        p['pendingApps'].discard(app_id)
        self._stop_invalid_sessions(requester=requester, host=host)
        self._event('requester_removed_app', requester=requester, host=host, app=app_id)

    def host_revoke_app(self, host, requester, app_id):
        p = self._accepted(requester, host)
        if app_id not in p['grants'] and app_id not in p['pendingApps']:
            raise AuthorizationError('no app permission exists')
        p['grants'].pop(app_id, None)
        p['pendingApps'].discard(app_id)
        self._stop_invalid_sessions(requester=requester, host=host)
        self._event('host_revoked_app', requester=requester, host=host, app=app_id)

    def host_revoke_user(self, host, requester):
        p = self._accepted(requester, host)
        p['grants'].clear()
        p['pendingApps'].clear()
        p['status'] = 'revoked'
        self._stop_invalid_sessions(requester=requester, host=host)
        self._event('host_revoked_user', requester=requester, host=host)

    def request_session(self, requester, host, app_id):
        available = [g for g in self.listed_apps(requester, host)
                     if g['appId'] == app_id and g['enabled']]
        if not available:
            raise AuthorizationError('not permitted by host and subscription')
        session_id = str(uuid.uuid4())
        self.sessions[session_id] = {'requester': requester, 'host': host,
                                     'appId': app_id, 'grantId': available[0]['grantId'],
                                     'status': 'pending_host_approval'}
        self._event('session_requested', requester=requester, host=host, app=app_id)
        return session_id

    def host_decline_session(self, host, session_id):
        sess = self.sessions.get(session_id)
        if not sess or sess['host'] != host or sess['status'] != 'pending_host_approval':
            raise AuthorizationError('host cannot decline this pending session')
        sess['status'] = 'terminated'
        self._event('session_declined', session=session_id, host=host)

    def host_stop_session(self, host, session_id):
        sess = self.sessions.get(session_id)
        if not sess or sess['host'] != host or sess['status'] not in ('pending_host_approval', 'active'):
            raise AuthorizationError('host cannot stop this session')
        sess['status'] = 'terminated'
        self._event('session_host_stopped', session=session_id, host=host)

    def requester_stop_session(self, requester, session_id):
        sess = self.sessions.get(session_id)
        if not sess or sess['requester'] != requester or sess['status'] not in ('pending_host_approval', 'active'):
            raise AuthorizationError('requester cannot stop this session')
        sess['status'] = 'terminated'
        self._event('session_requester_stopped', session=session_id, requester=requester)

    def host_accept_session(self, host, session_id):
        sess = self.sessions.get(session_id)
        if not sess or sess['host'] != host or sess['status'] != 'pending_host_approval':
            raise AuthorizationError('host cannot approve this session')
        # Recheck access at activation to close revoke/payment races.
        available = [g for g in self.listed_apps(sess['requester'], sess['host'])
                     if g['appId'] == sess['appId'] and g['enabled']
                     and g['grantId'] == sess['grantId']]
        if not available:
            sess['status'] = 'terminated'
            raise AuthorizationError('app or premium entitlement was revoked')
        sess['status'] = 'active'
        self._event('session_accepted', session=session_id, host=host)

    def session_is_allowed(self, session_id):
        sess = self.sessions.get(session_id)
        if not sess or sess['status'] != 'active':
            return False
        self._stop_invalid_sessions(requester=sess['requester'], host=sess['host'])
        return sess['status'] == 'active'


def scenario():
    play = MockGooglePlayAPI()
    app = TapVoyageMockBackend(play)
    app.invite('alice', 'bob')
    app.host_accept_circle('bob', 'alice')
    for name in ('Maps', 'Camera', 'Music'):
        app.request_app('alice', 'bob', name)
        app.host_approve_app('bob', 'alice', name)
    result = {'host': 'bob', 'requester': 'alice', 'domain': 'tapvoyage.peostectica.com'}
    result['free_initial'] = app.listed_apps('alice', 'bob')
    play.set_premium_for_test('alice', True)
    app.refresh_entitlement('alice')
    result['premium'] = app.listed_apps('alice', 'bob')
    play.set_premium_for_test('alice', False)
    app.refresh_entitlement('alice')
    result['free_after_expiry'] = app.listed_apps('alice', 'bob')
    app.remove_my_app('alice', 'bob', 'Maps')
    result['free_after_oldest_removed'] = app.listed_apps('alice', 'bob')
    app.request_app('alice', 'bob', 'Maps')
    app.host_approve_app('bob', 'alice', 'Maps')
    result['after_reapproval'] = app.listed_apps('alice', 'bob')
    app.host_revoke_user('bob', 'alice')
    try:
        app.listed_apps('alice', 'bob')
        result['after_host_revokes_user'] = 'ERROR: unexpected access'
    except AuthorizationError:
        result['after_host_revokes_user'] = 'DENIED even if premium is later restored'
    return result


if __name__ == '__main__':
    print(json.dumps(scenario(), sort_keys=True, indent=2))

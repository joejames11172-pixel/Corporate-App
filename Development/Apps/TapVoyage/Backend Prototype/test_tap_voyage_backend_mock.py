"""Unit tests: run python test_tap_voyage_backend_mock.py."""
from __future__ import print_function
import unittest
from tap_voyage_backend_mock import MockGooglePlayAPI, TapVoyageMockBackend, AuthorizationError


class CircleBillingTests(unittest.TestCase):
    def setUp(self):
        self.play = MockGooglePlayAPI()
        self.api = TapVoyageMockBackend(self.play)
        self.api.invite('alice', 'bob')
        self.api.host_accept_circle('bob', 'alice')

    def authorize(self, app):
        self.api.request_app('alice', 'bob', app)
        self.api.host_approve_app('bob', 'alice', app)

    def enabled(self):
        return [x['appId'] for x in self.api.listed_apps('alice', 'bob') if x['enabled']]

    def test_zero_app_circle(self):
        self.assertEqual([], self.api.listed_apps('alice', 'bob'))

    def test_pending_circle_blocks_listing(self):
        self.api.invite('carol', 'bob')
        with self.assertRaises(AuthorizationError):
            self.api.listed_apps('carol', 'bob')

    def test_oldest_authorization_free(self):
        for a in ('B', 'A', 'C'):
            self.authorize(a)
        self.assertEqual(['B'], self.enabled())
        self.assertEqual(['B', 'A', 'C'], [x['appId'] for x in self.api.listed_apps('alice','bob')])

    def test_premium_unlocks_all(self):
        self.authorize('A'); self.authorize('B')
        self.play.set_premium_for_test('alice', True)
        self.assertEqual(['A', 'B'], self.enabled())

    def test_free_after_premium_keeps_first(self):
        self.authorize('A'); self.authorize('B')
        self.play.set_premium_for_test('alice', True)
        self.play.set_premium_for_test('alice', False)
        self.api.refresh_entitlement('alice')
        self.assertEqual(['A'], self.enabled())
        self.assertEqual(2, len(self.api.listed_apps('alice', 'bob')))

    def test_requester_removal_requires_new_approval_and_new_order(self):
        self.authorize('A'); self.authorize('B'); self.authorize('C')
        self.api.remove_my_app('alice', 'bob', 'A')
        self.assertEqual(['B'], self.enabled())
        self.assertEqual(['B', 'C'], [x['appId'] for x in self.api.listed_apps('alice','bob')])
        self.api.request_app('alice', 'bob', 'A')
        self.assertEqual(['B'], self.enabled())
        self.api.host_approve_app('bob','alice','A')
        self.assertEqual(['B', 'C', 'A'], [x['appId'] for x in self.api.listed_apps('alice','bob')])
        self.assertEqual(['B'], self.enabled())

    def test_cannot_self_approve_app(self):
        with self.assertRaises(AuthorizationError):
            self.api.host_approve_app('bob','alice','A')

    def test_host_revoke_individual_even_if_premium(self):
        self.authorize('A'); self.authorize('B')
        self.play.set_premium_for_test('alice', True)
        self.api.host_revoke_app('bob', 'alice', 'B')
        self.assertEqual(['A'], self.enabled())
        self.assertEqual(['A'], [x['appId'] for x in self.api.listed_apps('alice','bob')])

    def test_host_revoke_user_blocks_all_including_free(self):
        self.authorize('A')
        self.api.host_revoke_user('bob','alice')
        with self.assertRaises(AuthorizationError):
            self.api.listed_apps('alice','bob')
        self.play.set_premium_for_test('alice', True)
        with self.assertRaises(AuthorizationError):
            self.api.request_session('alice','bob','A')

    def test_session_requires_host_consent(self):
        self.authorize('A')
        sid = self.api.request_session('alice','bob','A')
        self.assertFalse(self.api.session_is_allowed(sid))
        self.api.host_accept_session('bob',sid)
        self.assertTrue(self.api.session_is_allowed(sid))

    def test_revoke_app_terminates_live_session(self):
        self.authorize('A')
        sid=self.api.request_session('alice','bob','A')
        self.api.host_accept_session('bob',sid)
        self.api.host_revoke_app('bob','alice','A')
        self.assertFalse(self.api.session_is_allowed(sid))

    def test_downgrade_terminates_extra_app_session(self):
        self.authorize('A'); self.authorize('B')
        self.play.set_premium_for_test('alice',True)
        sid=self.api.request_session('alice','bob','B')
        self.api.host_accept_session('bob',sid)
        self.play.set_premium_for_test('alice',False)
        self.api.refresh_entitlement('alice')
        self.assertFalse(self.api.session_is_allowed(sid))
        self.assertEqual(['A'], self.enabled())

    def test_host_revocation_ends_active_session(self):
        self.authorize('A')
        sid=self.api.request_session('alice','bob','A')
        self.api.host_accept_session('bob',sid)
        self.api.host_revoke_user('bob','alice')
        self.assertFalse(self.api.session_is_allowed(sid))

    def test_two_contacts_each_get_free_one(self):
        self.authorize('A');self.authorize('B')
        self.api.invite('alice','carol');self.api.host_accept_circle('carol','alice')
        self.api.request_app('alice','carol','Other')
        self.api.host_approve_app('carol','alice','Other')
        self.assertEqual(['A'], self.enabled())
        self.assertEqual(1, sum(x['enabled'] for x in self.api.listed_apps('alice','carol')))

    def test_no_fake_premium_for_unverified_user(self):
        self.authorize('A');self.authorize('B')
        self.assertFalse(self.play.get_subscription('alice')['isPremium'])
        with self.assertRaises(ValueError):
            self.play.set_premium_for_test('alice','true')

    def test_bad_actor_cannot_revoke_other_hosts_permissions(self):
        self.authorize('A')
        with self.assertRaises(AuthorizationError):
            self.api.host_revoke_app('carol','alice','A')
        self.assertEqual(['A'], self.enabled())

    def test_rejoin_after_host_revocation_requires_new_acceptance(self):
        self.authorize('A')
        self.api.host_revoke_user('bob','alice')
        self.api.invite('alice','bob')
        with self.assertRaises(AuthorizationError):
            self.api.request_app('alice','bob','A')
        self.api.host_accept_circle('bob','alice')
        self.assertEqual([], self.api.listed_apps('alice','bob'))

    def test_revoke_user_terminates_pending_session(self):
        self.authorize('A')
        sid = self.api.request_session('alice','bob','A')
        self.api.host_revoke_user('bob','alice')
        self.assertEqual('terminated', self.api.sessions[sid]['status'])
        with self.assertRaises(AuthorizationError):
            self.api.host_accept_session('bob',sid)

    def test_approval_race_revoked_before_session_accept(self):
        self.authorize('A')
        sid=self.api.request_session('alice','bob','A')
        self.api.host_revoke_app('bob','alice','A')
        with self.assertRaises(AuthorizationError):
            self.api.host_accept_session('bob',sid)


    def test_entitlement_string_true_must_not_unlock_extra_app(self):
        self.authorize('A'); self.authorize('B')
        self.play.get_subscription=lambda uid: {'source':'MOCK_GOOGLE_PLAY','userId':uid,'isPremium':'true'}
        self.assertEqual(['A'],self.enabled())
        with self.assertRaises(AuthorizationError):
            self.api.request_session('alice','bob','B')

    def test_entitlement_wrong_account_or_unverified_source_fails_closed(self):
        self.authorize('A'); self.authorize('B')
        bad=[
            {'source':'MOCK_GOOGLE_PLAY','userId':'mallory','isPremium':True},
            {'source':'CLIENT_ASSERTED','userId':'alice','isPremium':True},
            {'source':'MOCK_GOOGLE_PLAY','userId':'alice','isPremium':1},
            {'source':'MOCK_GOOGLE_PLAY','userId':'alice','isPremium':'true'}
        ]
        for response in bad:
            self.play.get_subscription=lambda uid, response=response: response
            self.assertEqual(['A'], self.enabled(), response)

    def test_entitlement_outage_fails_closed_and_stops_premium_only_session(self):
        self.authorize('A'); self.authorize('B')
        self.play.set_premium_for_test('alice',True)
        sid=self.api.request_session('alice','bob','B')
        self.api.host_accept_session('bob',sid)
        self.assertTrue(self.api.session_is_allowed(sid))
        def offline(uid): raise RuntimeError('verifier temporarily offline')
        self.play.get_subscription=offline
        self.assertEqual(['A'],self.enabled())
        self.assertFalse(self.api.session_is_allowed(sid))

    def test_host_declines_pending_session_without_disturbing_other_grants(self):
        self.authorize('A')
        rejected = self.api.request_session('alice', 'bob', 'A')
        surviving = self.api.request_session('alice', 'bob', 'A')
        self.api.host_accept_session('bob', surviving)
        self.api.host_decline_session('bob', rejected)
        self.assertFalse(self.api.session_is_allowed(rejected))
        self.assertTrue(self.api.session_is_allowed(surviving))
        with self.assertRaises(AuthorizationError):
            self.api.host_accept_session('bob', rejected)

    def test_requester_cancel_pending_session_prevents_later_acceptance(self):
        self.authorize('A')
        sid=self.api.request_session('alice','bob','A')
        self.api.requester_stop_session('alice',sid)
        with self.assertRaises(AuthorizationError):
            self.api.host_accept_session('bob',sid)
        self.assertEqual('terminated',self.api.sessions[sid]['status'])

    def test_host_stops_one_live_session_without_revoking_app(self):
        self.authorize('A')
        first=self.api.request_session('alice','bob','A')
        second=self.api.request_session('alice','bob','A')
        self.api.host_accept_session('bob',first)
        self.api.host_accept_session('bob',second)
        self.api.host_stop_session('bob',first)
        self.assertFalse(self.api.session_is_allowed(first))
        self.assertTrue(self.api.session_is_allowed(second))
        self.assertEqual(['A'],self.enabled())

    def test_requester_ends_their_active_session(self):
        self.authorize('A')
        sid=self.api.request_session('alice','bob','A')
        self.api.host_accept_session('bob',sid)
        self.api.requester_stop_session('alice',sid)
        self.assertFalse(self.api.session_is_allowed(sid))
        with self.assertRaises(AuthorizationError):
            self.api.requester_stop_session('alice',sid)

    def test_wrong_actor_cannot_decline_stop_or_cancel_session(self):
        self.authorize('A')
        pending=self.api.request_session('alice','bob','A')
        with self.assertRaises(AuthorizationError):
            self.api.host_decline_session('charlie',pending)
        with self.assertRaises(AuthorizationError):
            self.api.host_stop_session('charlie',pending)
        with self.assertRaises(AuthorizationError):
            self.api.requester_stop_session('charlie',pending)
        self.api.host_accept_session('bob',pending)
        self.assertTrue(self.api.session_is_allowed(pending))

    def test_decline_is_terminal_and_audited(self):
        self.authorize('A')
        sid=self.api.request_session('alice','bob','A')
        self.api.host_decline_session('bob',sid)
        self.assertFalse(self.api.session_is_allowed(sid))
        self.assertIn('session_declined',[e['event'] for e in self.api.audit])
        with self.assertRaises(AuthorizationError):
            self.api.host_decline_session('bob',sid)


if __name__ == '__main__':
    unittest.main(verbosity=2)

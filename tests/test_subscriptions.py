import copy
import importlib.machinery
import importlib.util
import json
import pathlib
import subprocess
import tempfile
import unittest
from unittest.mock import patch

loader = importlib.machinery.SourceFileLoader('subscriptions', str(pathlib.Path(__file__).parents[1] / 'vpnx/bin/vpnx-subscriptions'))
spec = importlib.util.spec_from_loader(loader.name, loader)
module = importlib.util.module_from_spec(spec)
loader.exec_module(module)


class SubscriptionsTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = pathlib.Path(self.directory.name)
        for key, value in [('ROOT', self.root), ('STATE', self.root / 'state.json'),
                           ('REGISTRY', self.root / 'registry.json'), ('NODES', self.root / 'nodes')]:
            patcher = patch.object(module, key, value)
            patcher.start()
            self.addCleanup(patcher.stop)
        self.addCleanup(self.directory.cleanup)
        self.profile = {'remarks': 'BIRD Дания', 'inbounds': [],
                        'outbounds': [{'protocol': 'vless', 'streamSettings': {'network': 'xhttp', 'xhttpSettings': {'mode': 'auto'}}}],
                        'routing': {'balancers': [{'tag': 'auto', 'selector': ['p1', 'p2']}]},
                        'observatory': {'subjectSelector': ['p1']}}

    def test_unknown_country_and_full_transport_are_preserved(self):
        original = copy.deepcopy(self.profile)
        runtime = module.normalize(self.profile)
        self.assertEqual(runtime['routing'], original['routing'])
        self.assertEqual(runtime['observatory'], original['observatory'])
        self.assertEqual(runtime['outbounds'], original['outbounds'])
        self.assertEqual(self.profile, original)
        self.assertEqual([i['port'] for i in runtime['inbounds']], [10808, 10809])

    def test_invalid_xray_never_replaces_last_good(self):
        module.NODES.mkdir()
        prior = module.NODES / 'bird-old.json'
        prior.write_text('{"old":true}')
        states = {'bird': {'profiles': [{'tag': 'bird-old', 'name': 'old'}], 'last_success': 123}}
        with patch.object(module, 'fetch', return_value=([self.profile], 456)), patch.object(module.subprocess, 'run', return_value=subprocess.CompletedProcess([], 1)):
            self.assertFalse(module.sync_one({'id': 'bird', 'name': 'BIRD'}, states))
        self.assertEqual(prior.read_text(), '{"old":true}')
        self.assertEqual(states['bird']['last_success'], 123)
        self.assertEqual(len(list(module.NODES.iterdir())), 1)

    def test_routing_modes_preserve_balancer_and_source(self):
        source = module.normalize(self.profile)
        source['outbounds'][0]['tag'] = 'proxy'
        source['outbounds'].append({'tag': 'direct', 'protocol': 'freedom'})
        source['routing']['rules'] = [{'type': 'field', 'network': 'tcp,udp', 'balancerTag': 'auto'}]
        original = copy.deepcopy(source)
        global_config = module.routed(source, 'global')
        ru_config = module.routed(source, 'ru-direct')
        self.assertEqual(global_config['routing']['rules'][-1]['balancerTag'], 'auto')
        self.assertEqual(ru_config['routing']['rules'][-1]['balancerTag'], 'auto')
        self.assertTrue(any('geoip:ru' in r.get('ip', []) for r in ru_config['routing']['rules']))
        self.assertFalse(any('geoip:ru' in r.get('ip', []) for r in global_config['routing']['rules']))
        self.assertEqual(source, original)

    def test_latency_is_invalidated_when_profile_changes(self):
        import hashlib
        import time
        module.NODES.mkdir()
        path = module.NODES / 'manual.json'
        path.write_text('{}')
        module.atomic(self.root / 'profile-latency.json', {'manual': {
            'hash': hashlib.sha256(path.read_bytes()).hexdigest(), 'mode': 'subscription',
            'checked_at': time.time(), 'ok': True, 'ms': 123}})
        self.assertEqual(module.status()['profiles'][0]['latency']['ms'], 123)
        path.write_text('{"changed":true}')
        self.assertNotIn('latency', module.status()['profiles'][0])

    def test_unchanged_snapshot_does_not_restart_and_manual_nodes_survive(self):
        module.NODES.mkdir()
        (module.ROOT / 'disabled').touch()
        manual = module.NODES / 'FI-BPC-1.json'
        manual.write_text('{}')
        states = {}
        sub = {'id': 'bird', 'name': 'BIRD', 'kind': 'mirror'}
        with patch.object(module, 'fetch', return_value=([self.profile], 456)), patch.object(module.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0)), patch.object(module, 'command') as commands:
            self.assertTrue(module.sync_one(sub, states))
            self.assertTrue(module.sync_one(sub, states))
            commands.assert_not_called()
        self.assertTrue(manual.exists())
        self.assertEqual(states['bird']['snapshot_updated_at'], 456)
        self.assertEqual(len(states['bird']['profiles']), 1)


if __name__ == '__main__':
    unittest.main()

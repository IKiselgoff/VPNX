#!/usr/bin/env python3
"""Install a release bundle without replacing the existing compatible Xray."""
import os
import plistlib
import shutil
import subprocess
import time
from pathlib import Path

base = Path(__file__).resolve().parents[1]
root = Path.home() / '.vpnx'
agents = Path.home() / 'Library/LaunchAgents'
app = Path.home() / 'Applications/VPNXMenu.app'
backup = root / ('backup-ui-' + time.strftime('%Y%m%d-%H%M%S'))
backup.mkdir(parents=True)
agents.mkdir(parents=True, exist_ok=True)
(root / 'bin').mkdir(parents=True, exist_ok=True)
runtime = base / 'runtime'
if runtime.exists():
    subprocess.run([str(runtime / 'xray'), 'version'], check=True, capture_output=True)
    for name in ['xray', 'libVPNXSecurity.dylib']:
        target = root / name
        if target.exists():
            shutil.copy2(str(target), str(backup / name))
        temporary = root / (name + '.update')
        shutil.copy2(str(runtime / name), str(temporary))
        temporary.chmod(0o700)
        os.replace(str(temporary), str(target))

for name in ['vpnx', 'vpnx-subscriptions', 'vpnx-bird-sync']:
    target = root / 'bin' / name
    if target.exists():
        shutil.copy2(str(target), str(backup / name))
    shutil.copy2(str(base / 'vpnx/bin' / name), str(target))
    target.chmod(0o700)

uid = os.getuid()
for label in ['local.vpnx.bird-sync', 'local.vpnx.autostart', 'local.vpnx.menu']:
    subprocess.run(['launchctl', 'bootout', 'gui/{}/{}'.format(uid, label)], capture_output=True)
    path = agents / (label + '.plist')
    if path.exists():
        shutil.copy2(str(path), str(backup / path.name))

# Migration is checked before the background agents resume.
subprocess.run(['/usr/bin/python3', str(root / 'bin/vpnx-subscriptions'), 'sync'], check=True)

if app.exists():
    shutil.copytree(str(app), str(backup / app.name))
    subprocess.run(['pkill', '-f', '^' + str(app / 'Contents/MacOS/VPNXMenu') + '$'], capture_output=True)
    shutil.rmtree(str(app))
app.parent.mkdir(parents=True, exist_ok=True)
shutil.copytree(str(base / 'VPNXMenu.app'), str(app))

specs = {
    'local.vpnx.bird-sync': ([str(root / 'bin/vpnx-subscriptions'), 'sync', '--due'], 60),
    'local.vpnx.autostart': ([str(root / 'bin/vpnx'), 'watchdog'], 60),
    'local.vpnx.menu': ([str(app / 'Contents/MacOS/VPNXMenu')], None),
}
for label, (arguments, interval) in specs.items():
    spec = {'Label': label, 'ProgramArguments': arguments, 'RunAtLoad': True,
            'StandardOutPath': str(root / (label + '.out.log')),
            'StandardErrorPath': str(root / (label + '.err.log'))}
    if interval:
        spec['StartInterval'] = interval
    path = agents / (label + '.plist')
    with path.open('wb') as handle:
        plistlib.dump(spec, handle)
    subprocess.run(['launchctl', 'bootstrap', 'gui/{}'.format(uid), str(path)], check=True)
print('Installed VPNX 2.0; backup:', backup)

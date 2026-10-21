#!/usr/bin/env python3
"""Package one Mac installer; private enrollment material stays outside Git."""

import argparse
import os
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--identity", required=True)
    parser.add_argument("--port", required=True, type=int)
    parser.add_argument("--enrollment-key", required=True, type=Path)
    parser.add_argument("--host-pins", required=True, type=Path)
    parser.add_argument("--operator-key", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    import re
    if not re.fullmatch(r"[a-z0-9-]{1,64}", args.identity) or not 22084 <= args.port <= 22999:
        parser.error("invalid identity or port")
    if args.output.exists():
        parser.error("output already exists")
    template = Path(__file__).resolve().parents[1] / "vpnx/macos/connect-mac.sh.in"
    values = {
        "IDENTITY": args.identity,
        "PORT": str(args.port),
        "ENROLLMENT_KEY": args.enrollment_key.read_text().strip(),
        "HOST_PINS": args.host_pins.read_text().strip(),
        "OPERATOR_KEY": " ".join(args.operator_key.read_text().split()[:2]),
    }
    if not re.fullmatch(r"ssh-ed25519 [A-Za-z0-9+/=]+", values["OPERATOR_KEY"]):
        parser.error("invalid operator public key")
    pin_types = {line.split()[1] for line in values["HOST_PINS"].splitlines() if len(line.split()) >= 3}
    if not {"ssh-ed25519", "ecdsa-sha2-nistp256"}.issubset(pin_types):
        parser.error("verified VPS Ed25519 and ECDSA pins are both required")
    content = template.read_text()
    for key, value in values.items():
        content = content.replace("@@" + key + "@@", value)
    if "@@" in content:
        parser.error("unresolved template marker")
    fd = os.open(args.output, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o700)
    with os.fdopen(fd, "w") as output:
        output.write(content)
    print(f"Created {args.output}; reverse port {args.port}")


if __name__ == "__main__":
    main()

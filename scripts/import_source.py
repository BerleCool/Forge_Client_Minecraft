#!/usr/bin/env python3
"""One-time verified transport of the authored source through the text-only connector.

The runner commits the expanded, reviewable source before building it. This is
not used by the mod, and subsequent builds do not require the transport archive.
"""
import base64
import hashlib
import io
import pathlib
import tarfile

root = pathlib.Path(__file__).resolve().parents[1]
parts = [root / ('build-support/source.part%02d' % i) for i in range(6)]
encoded = ''.join(path.read_text(encoding='ascii') for path in parts)
if len(encoded) != 61112:
    raise SystemExit('Unexpected source transport length')
raw = base64.b64decode(encoded, validate=True)
expected = 'afd9b1669a6924024540319294693632bd32943e2e89897b8e5bd23f973d60e3'
if hashlib.sha256(raw).hexdigest() != expected:
    raise SystemExit('Source digest mismatch; refusing to extract')
pending = {}
with tarfile.open(fileobj=io.BytesIO(raw), mode='r:xz') as archive:
    members = archive.getmembers()
    if len(members) != 45 or sum(m.size for m in members) > 4 * 1024 * 1024:
        raise SystemExit('Unexpected source archive size')
    for member in members:
        path = pathlib.PurePosixPath(member.name)
        if (not member.isfile() or path.is_absolute() or '..' in path.parts
                or '\\' in member.name or path.parts[0] not in ('src', 'scripts', 'docs')
                or member.size > 1024 * 1024 or member.name in pending):
            raise SystemExit('Unsafe source archive entry: ' + member.name)
        target = root / path
        if target.is_symlink() or any(p.is_symlink() for p in target.parents if p != root.parent):
            raise SystemExit('Symlink in source destination')
        pending[member.name] = archive.extractfile(member).read()
# Correct the interface spelling against MinecraftForge's 1.8.9 IModGuiFactory.
factory = 'src/main/java/dev/forgeclient/minecraft/ForgeGuiFactory.java'
old = b'public RuntimeGuiHandler getHandlerFor('
if pending[factory].count(old) != 1:
    raise SystemExit('Unexpected factory source; refusing blind patch')
pending[factory] = pending[factory].replace(old, b'public RuntimeOptionGuiHandler getHandlerFor(')
for name, content in pending.items():
    target = root / name
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(content)
    target.chmod(0o755 if name.endswith('.sh') else 0o644)
for path in parts:
    path.unlink()
print('Imported %d authored source/resource files; verified XZ SHA-256 %s' % (len(pending), expected))

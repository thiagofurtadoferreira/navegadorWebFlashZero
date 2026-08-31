#!/usr/bin/env python3
from pathlib import Path
import base64
import gzip
import hashlib

EXPECTED_SHA256 = "ea6aba20aed1a79fdd36f38282c144a0710945132b860ce46a1ec1fbadad92f6"

here = Path(__file__).resolve().parent
parts = sorted(here.glob("MainActivity.java.gz.b64.part*"))
if not parts:
    raise SystemExit("Nenhuma parte do snapshot MainActivity foi encontrada.")

encoded = "".join("".join(p.read_text(encoding="utf-8").split()) for p in parts)
try:
    compressed = base64.b64decode(encoded, validate=True)
    source = gzip.decompress(compressed)
except Exception as exc:
    raise SystemExit(f"Falha ao reconstruir o snapshot v0.4.18: {exc}") from exc

actual = hashlib.sha256(source).hexdigest()
if actual != EXPECTED_SHA256:
    raise SystemExit(
        "SHA-256 do MainActivity restaurado não confere. "
        f"Esperado={EXPECTED_SHA256} obtido={actual}"
    )

repo_root = here.parents[2]
out = repo_root / "android/app/src/main/java/org/navegadorwebdozero/preview/MainActivity.java"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_bytes(source)
print(f"MainActivity v0.4.18 restaurado: {out}")
print(f"SHA-256: {actual}")

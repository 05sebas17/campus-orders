"""Include a non-secret revision marker in the exact web artifact being deployed."""
import json
import os
import re
from pathlib import Path

revision = os.environ.get("CODEBUILD_RESOLVED_SOURCE_VERSION", "")
if not re.fullmatch(r"[0-9a-fA-F]{40}", revision):
    raise SystemExit("Expected the Git commit SHA in CODEBUILD_RESOLVED_SOURCE_VERSION")
destination = Path("frontend/dist/version.json")
if not destination.parent.is_dir():
    raise SystemExit("Build frontend/dist before writing the revision marker")
destination.write_text(json.dumps({"commit": revision.lower()}) + "\n", encoding="utf-8")

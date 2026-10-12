"""Check release input rejection without keys, production IDs, or producing a release."""
import os
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parent.parent
base = {key: value for key, value in os.environ.items() if not key.startswith("ZEROSTAR_")}
base["ZEROSTAR_APPLICATION_ID"] = "org.example.releaseguard"
cases = [
    ("missing version code", {}, "Set the owner-approved positive ZEROSTAR_VERSION_CODE."),
    ("missing version name", {"ZEROSTAR_VERSION_CODE": "1"}, "Set the owner-approved ZEROSTAR_VERSION_NAME."),
    ("version inputs advance to the next guard", {"ZEROSTAR_VERSION_CODE": "1", "ZEROSTAR_VERSION_NAME": "0.1.0"}, "ZEROSTAR_ADMOB_APP_ID must be a production ID."),
]
failed = 0
for name, overrides, expected in cases:
    result = subprocess.run(
        [str(root / "gradlew.bat"), "-g", ".tools/gradle-network", ":app:verifyReleaseConfiguration", "--max-workers=2"],
        cwd=root, env=base | overrides, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, encoding="utf-8", errors="replace",
    )
    passed = result.returncode != 0 and expected in result.stdout
    print(f"{'PASS' if passed else 'FAIL'}: {name}")
    if not passed:
        failed += 1
        print("Expected rejection was not observed; inspect verifyReleaseConfiguration.")
raise SystemExit(1 if failed else 0)

"""AC-01/AC-06: kizárólag Android-emulátoron futó offline/újraindítási próba.

Előfeltétel: assembleDebug és assembleDebugAndroidTest. A debug appot és tesztjét
telepíti az emulátorra, tesztadatokat hoz létre, az airplane módot visszaállítja.
Használat: python tools/verify_offline_restart.py --adb <SDK/platform-tools/adb.exe>
"""

import argparse
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", default="emulator-5554")
    args = parser.parse_args()
    if not args.serial.startswith("emulator-"):
        raise SystemExit("Ez a teszt csak emulátoron engedélyezett.")
    project = Path(__file__).resolve().parents[1]
    evidence = project / "tmp" / "acceptance"
    evidence.mkdir(parents=True, exist_ok=True)

    def adb(*command):
        result = subprocess.run(
            [args.adb, "-s", args.serial, *command],
            check=True, capture_output=True, encoding="utf-8", timeout=120,
        )
        return result.stdout.strip()

    package = "hu.kyokushin.trainer"
    adb("install", "-r", str(project / "app/build/outputs/apk/debug/app-debug.apk"))
    adb("install", "-r", str(project / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"))
    test = adb(
        "shell", "am", "instrument", "-w", "-r", "-e", "class",
        package + ".AppFlowTest#completeQuizWithRotation_TR06_FR18_FR19_FR20",
        package + ".test/androidx.test.runner.AndroidJUnitRunner",
    )
    (evidence / "instrumentation.txt").write_text(test, encoding="utf-8")
    assert "OK (1 test)" in test, test

    def tree(name):
        adb("shell", "uiautomator", "dump", "/sdcard/kyo-acceptance.xml")
        xml = adb("shell", "cat", "/sdcard/kyo-acceptance.xml")
        (evidence / f"{name}.xml").write_text(xml, encoding="utf-8")
        return ET.fromstring(xml)

    def open_progress(name):
        adb("shell", "am", "start", "-W", "-n", package + "/.MainActivity")
        time.sleep(1)
        library = tree(name + "-library")
        assert any("Keresés romaji" in n.get("text", "") for n in library.iter("node"))
        tab = next(n for n in library.iter("node") if n.get("text") == "Haladás")
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", tab.get("bounds")))
        adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
        time.sleep(1)
        progress = tree(name + "-progress")
        texts = [n.get("text") for n in progress.iter("node")
                 if n.get("package") == package and n.get("text")]
        scores = [t for t in texts if re.fullmatch(r"\d+ / \d+ helyes", t)]
        assert scores and any(int(s.split(" / ")[1].split()[0]) >= 5 for s in scores), texts
        return texts

    old_airplane = adb("shell", "settings", "get", "global", "airplane_mode_on")
    try:
        adb("shell", "cmd", "connectivity", "airplane-mode", "enable")
        assert adb("shell", "settings", "get", "global", "airplane_mode_on") == "1"
        adb("shell", "am", "force-stop", package)
        before = open_progress("before")
        adb("shell", "am", "force-stop", package)
        after = open_progress("after")
        assert before == after, (before, after)
        report = {"AC-01": "PASS", "AC-06": "PASS", "progress": after,
                  "device": args.serial, "airplane_mode_during_test": True}
        (evidence / "result.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print("AC-01 PASS; AC-06 PASS; evidence: tmp/acceptance/result.json")
    finally:
        adb("shell", "cmd", "connectivity", "airplane-mode", "enable" if old_airplane == "1" else "disable")


if __name__ == "__main__":
    main()

from pathlib import Path
import re

root = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"
for p in root.glob("values*/strings.xml"):
    t = p.read_text(encoding="utf-8")
    nt = re.sub(r"\n\s*<string name=\"update_no_root\">.*?</string>", "", t, flags=re.S)
    if nt != t:
        p.write_text(nt, encoding="utf-8")
        print("stripped", p)
    else:
        print("skip", p)

import re
from pathlib import Path

root = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"
for p in root.glob("values*/strings.xml"):
    t = p.read_text(encoding="utf-8")
    nt = re.sub(
        r'<string name="app_name">.*?</string>',
        '<string name="app_name">Onyx Gestures</string>',
        t,
        count=1,
    )
    # Keep notification titles aligned with brand without changing other copy heavily
    nt = re.sub(
        r'<string name="notification_title">.*?</string>',
        '<string name="notification_title">Onyx Gestures</string>',
        nt,
        count=1,
    )
    p.write_text(nt, encoding="utf-8")
    print("updated", p)

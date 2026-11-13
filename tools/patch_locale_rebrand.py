"""Patch critical user-facing strings across locales for Onyx Gestures rebrand."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"

REPLACEMENTS = {
    "camera_launch_channel": {
        "default": "Camera gesture",
        "pt": "Gesto da câmera",
        "es": "Gesto de cámara",
        "zh-rCN": "相机手势",
        "zh-rTW": "相機手勢",
        "hi": "कैमरा जेस्चर",
        "ru": "Жест камеры",
    },
    "app_tagline": {
        "default": "An independent Android gesture-control project for compatible devices. Calibrate on-device.",
        "pt": "Projeto independente de gestos Android para dispositivos compatíveis. Calibre no aparelho.",
        "es": "Proyecto independiente de gestos Android para dispositivos compatibles. Calibra en el dispositivo.",
        "zh-rCN": "面向兼容设备的独立 Android 手势控制项目。请在设备上校准。",
        "zh-rTW": "面向相容裝置的獨立 Android 手勢控制專案。請在裝置上校正。",
        "hi": "संगत उपकरणों के लिए स्वतंत्र Android जेस्चर प्रोजेक्ट। डिवाइस पर कैलिब्रेट करें।",
        "ru": "Независимый проект жестов Android для совместимых устройств. Калибруйте на устройстве.",
    },
    "toggle_chop": {
        "default": "Double chopping motion → flashlight",
        "pt": "Dois golpes → lanterna",
        "es": "Doble golpe → linterna",
        "zh-rCN": "双击挥砍动作 → 手电筒",
        "zh-rTW": "雙擊揮砍動作 → 手電筒",
        "hi": "दो बार चॉप → फ्लैशलाइट",
        "ru": "Двойной удар → фонарик",
    },
    "toggle_twist": {
        "default": "Double wrist rotation → camera",
        "pt": "Duas rotações do pulso → câmera",
        "es": "Doble rotación de muñeca → cámara",
        "zh-rCN": "手腕双向转动 → 相机",
        "zh-rTW": "手腕雙向轉動 → 相機",
        "hi": "कलाई का दोहरा घुमाव → कैमरा",
        "ru": "Двойной поворот запястья → камера",
    },
    "btn_open_kernelsu": {
        "default": "Open KernelSU Manager",
        "pt": "Abrir KernelSU Manager",
        "es": "Abrir KernelSU Manager",
        "zh-rCN": "打开 KernelSU Manager",
        "zh-rTW": "開啟 KernelSU Manager",
        "hi": "KernelSU Manager खोलें",
        "ru": "Открыть KernelSU Manager",
    },
    "update_root_denied": {
        "default": "This operation needs root access. Authorize Onyx Gestures in KernelSU Manager and try again.",
        "pt": "Esta operação precisa de root. Autorize o Onyx Gestures no KernelSU Manager e tente novamente.",
        "es": "Esta operación necesita root. Autorice Onyx Gestures en KernelSU Manager e inténtelo de nuevo.",
        "zh-rCN": "此操作需要 root。请在 KernelSU Manager 中授权 Onyx Gestures 后重试。",
        "zh-rTW": "此操作需要 root。請在 KernelSU Manager 中授權 Onyx Gestures 後重試。",
        "hi": "इस क्रिया के लिए root चाहिए। KernelSU Manager में Onyx Gestures को अनुमति दें और फिर कोशिश करें।",
        "ru": "Нужен root. Разрешите Onyx Gestures в KernelSU Manager и повторите.",
    },
}

FOLDER_KEY = {
    "values": "default",
    "values-pt": "pt",
    "values-es": "es",
    "values-zh-rCN": "zh-rCN",
    "values-zh-rTW": "zh-rTW",
    "values-hi": "hi",
    "values-ru": "ru",
}


def set_str(xml: str, name: str, value: str) -> str:
    pattern = rf'<string name="{re.escape(name)}">.*?</string>'
    repl = f'<string name="{name}">{value}</string>'
    if re.search(pattern, xml, flags=re.DOTALL):
        return re.sub(pattern, repl, xml, count=1, flags=re.DOTALL)
    # insert before </resources>
    return xml.replace("</resources>", f"    {repl}\n</resources>")


for folder, key in FOLDER_KEY.items():
    path = ROOT / folder / "strings.xml"
    if not path.exists():
        continue
    text = path.read_text(encoding="utf-8")
    for name, variants in REPLACEMENTS.items():
        text = set_str(text, name, variants.get(key, variants["default"]))
    # ensure disclaimer exists
    if 'name="disclaimer_short"' not in text and key == "default":
        pass
    elif 'name="disclaimer_short"' not in text:
        disc = {
            "pt": "Projeto comunitário independente — sem vínculo com Motorola, Lenovo, Xiaomi ou outros fabricantes.",
            "es": "Proyecto comunitario independiente — no afiliado a Motorola, Lenovo, Xiaomi u otros fabricantes.",
            "zh-rCN": "独立社区项目 — 与摩托罗拉、联想、小米或其他厂商无关。",
            "zh-rTW": "獨立社群專案 — 與摩托羅拉、聯想、小米或其他廠商無關。",
            "hi": "स्वतंत्र सामुदायिक प्रोजेक्ट — Motorola, Lenovo, Xiaomi या अन्य निर्माताओं से संबद्ध नहीं।",
            "ru": "Независимый проект сообщества — не связан с Motorola, Lenovo, Xiaomi или другими производителями.",
        }.get(key)
        if disc:
            text = set_str(text, "disclaimer_short", disc)
    for extra in ("btn_open_releases", "kernelsu_not_found", "update_root_unavailable"):
        if f'name="{extra}"' not in text and key != "default":
            # English fallbacks already in values/; skip forcing every locale
            pass
    path.write_text(text, encoding="utf-8")
    print("patched", path)

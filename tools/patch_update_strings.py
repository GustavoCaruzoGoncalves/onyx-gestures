from pathlib import Path

blocks = {
    "pt": """    <string name="section_updates">Atualizações</string>
    <string name="btn_check_updates">Buscar atualizações</string>
    <string name="btn_install_update">Instalar última do GitHub</string>
    <string name="btn_open_kernelsu">Abrir KernelSU</string>
    <string name="update_checking">Consultando GitHub…</string>
    <string name="update_downloading">Baixando %1$s…</string>
    <string name="update_installing">Instalando módulo com root…</string>
    <string name="update_up_to_date">Você já está na versão mais recente (%1$d).</string>
    <string name="update_available">Atualização disponível: %1$s (código %2$d)</string>
    <string name="update_failed">Falha na atualização: %1$s</string>
    <string name="update_installed">Módulo atualizado para %1$s. Reinicie para aplicar.</string>
    <string name="update_reboot_title">Reiniciar agora?</string>
    <string name="update_reboot_message">O módulo KernelSU foi instalado. Reinicie para aplicar o overlay e manter tudo sincronizado.</string>
    <string name="update_reboot_yes">Reiniciar</string>
    <string name="update_reboot_later">Depois</string>
    <string name="update_no_root">Root / ksud necessários para instalar pelo app. Use o KernelSU Manager.</string>
    <string name="update_hint">O KernelSU também pode atualizar este módulo via updateJson das releases do GitHub.</string>
""",
    "es": """    <string name="section_updates">Actualizaciones</string>
    <string name="btn_check_updates">Buscar actualizaciones</string>
    <string name="btn_install_update">Instalar la última de GitHub</string>
    <string name="btn_open_kernelsu">Abrir KernelSU</string>
    <string name="update_checking">Consultando GitHub…</string>
    <string name="update_downloading">Descargando %1$s…</string>
    <string name="update_installing">Instalando módulo con root…</string>
    <string name="update_up_to_date">Ya tienes la última versión (%1$d).</string>
    <string name="update_available">Actualización disponible: %1$s (código %2$d)</string>
    <string name="update_failed">Error al actualizar: %1$s</string>
    <string name="update_installed">Módulo actualizado a %1$s. Reinicia para aplicar.</string>
    <string name="update_reboot_title">¿Reiniciar ahora?</string>
    <string name="update_reboot_message">Se instaló el módulo KernelSU. Reinicia para aplicar el overlay y mantener todo sincronizado.</string>
    <string name="update_reboot_yes">Reiniciar</string>
    <string name="update_reboot_later">Más tarde</string>
    <string name="update_no_root">Se necesita root / ksud para instalar desde la app. Usa KernelSU Manager.</string>
    <string name="update_hint">KernelSU también puede actualizar este módulo vía updateJson de las releases de GitHub.</string>
""",
    "zh-rCN": """    <string name="section_updates">更新</string>
    <string name="btn_check_updates">检查更新</string>
    <string name="btn_install_update">从 GitHub 安装最新版</string>
    <string name="btn_open_kernelsu">打开 KernelSU</string>
    <string name="update_checking">正在查询 GitHub…</string>
    <string name="update_downloading">正在下载 %1$s…</string>
    <string name="update_installing">正在以 root 安装模块…</string>
    <string name="update_up_to_date">已是最新版本（%1$d）。</string>
    <string name="update_available">发现更新：%1$s（版本号 %2$d）</string>
    <string name="update_failed">更新失败：%1$s</string>
    <string name="update_installed">模块已更新到 %1$s。请重启以生效。</string>
    <string name="update_reboot_title">立即重启？</string>
    <string name="update_reboot_message">KernelSU 模块已安装。重启以应用 systemless 覆盖并保持同步。</string>
    <string name="update_reboot_yes">重启</string>
    <string name="update_reboot_later">稍后</string>
    <string name="update_no_root">应用内安装需要 root / ksud。请改用 KernelSU Manager。</string>
    <string name="update_hint">KernelSU 也可通过 GitHub releases 的 updateJson 更新此模块。</string>
""",
    "zh-rTW": """    <string name="section_updates">更新</string>
    <string name="btn_check_updates">檢查更新</string>
    <string name="btn_install_update">從 GitHub 安裝最新版</string>
    <string name="btn_open_kernelsu">開啟 KernelSU</string>
    <string name="update_checking">正在查詢 GitHub…</string>
    <string name="update_downloading">正在下載 %1$s…</string>
    <string name="update_installing">正在以 root 安裝模組…</string>
    <string name="update_up_to_date">已是最新版本（%1$d）。</string>
    <string name="update_available">發現更新：%1$s（版本號 %2$d）</string>
    <string name="update_failed">更新失敗：%1$s</string>
    <string name="update_installed">模組已更新到 %1$s。請重新開機以套用。</string>
    <string name="update_reboot_title">立即重新開機？</string>
    <string name="update_reboot_message">KernelSU 模組已安裝。重新開機以套用 systemless 覆蓋並保持同步。</string>
    <string name="update_reboot_yes">重新開機</string>
    <string name="update_reboot_later">稍後</string>
    <string name="update_no_root">應用程式內安裝需要 root / ksud。請改用 KernelSU Manager。</string>
    <string name="update_hint">KernelSU 也可透過 GitHub releases 的 updateJson 更新此模組。</string>
""",
    "hi": """    <string name="section_updates">अपडेट</string>
    <string name="btn_check_updates">अपडेट खोजें</string>
    <string name="btn_install_update">GitHub से नवीनतम इंस्टॉल करें</string>
    <string name="btn_open_kernelsu">KernelSU खोलें</string>
    <string name="update_checking">GitHub जाँच…</string>
    <string name="update_downloading">डाउनलोड %1$s…</string>
    <string name="update_installing">root से मॉड्यूल इंस्टॉल…</string>
    <string name="update_up_to_date">आप नवीनतम संस्करण पर हैं (%1$d)।</string>
    <string name="update_available">अपडेट उपलब्ध: %1$s (कोड %2$d)</string>
    <string name="update_failed">अपडेट विफल: %1$s</string>
    <string name="update_installed">मॉड्यूल %1$s पर अपडेट। लागू करने के लिए रीबूट करें।</string>
    <string name="update_reboot_title">अभी रीबूट?</string>
    <string name="update_reboot_message">KernelSU मॉड्यूल इंस्टॉल हो गया। Overlay लागू करने के लिए रीबूट करें।</string>
    <string name="update_reboot_yes">रीबूट</string>
    <string name="update_reboot_later">बाद में</string>
    <string name="update_no_root">ऐप से इंस्टॉल के लिए root / ksud चाहिए। KernelSU Manager उपयोग करें।</string>
    <string name="update_hint">KernelSU GitHub releases के updateJson से भी इस मॉड्यूल को अपडेट कर सकता है।</string>
""",
    "ru": """    <string name="section_updates">Обновления</string>
    <string name="btn_check_updates">Проверить обновления</string>
    <string name="btn_install_update">Установить последнюю с GitHub</string>
    <string name="btn_open_kernelsu">Открыть KernelSU</string>
    <string name="update_checking">Проверка GitHub…</string>
    <string name="update_downloading">Загрузка %1$s…</string>
    <string name="update_installing">Установка модуля через root…</string>
    <string name="update_up_to_date">У вас последняя версия (%1$d).</string>
    <string name="update_available">Доступно обновление: %1$s (код %2$d)</string>
    <string name="update_failed">Ошибка обновления: %1$s</string>
    <string name="update_installed">Модуль обновлён до %1$s. Перезагрузите устройство.</string>
    <string name="update_reboot_title">Перезагрузить сейчас?</string>
    <string name="update_reboot_message">Модуль KernelSU установлен. Перезагрузка применит systemless-оверлей.</string>
    <string name="update_reboot_yes">Перезагрузить</string>
    <string name="update_reboot_later">Позже</string>
    <string name="update_no_root">Для установки из приложения нужны root / ksud. Используйте KernelSU Manager.</string>
    <string name="update_hint">KernelSU также может обновлять модуль через updateJson релизов GitHub.</string>
""",
}

base = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"
mapping = {
    "pt": base / "values-pt" / "strings.xml",
    "es": base / "values-es" / "strings.xml",
    "zh-rCN": base / "values-zh-rCN" / "strings.xml",
    "zh-rTW": base / "values-zh-rTW" / "strings.xml",
    "hi": base / "values-hi" / "strings.xml",
    "ru": base / "values-ru" / "strings.xml",
}

for key, path in mapping.items():
    text = path.read_text(encoding="utf-8")
    if 'name="section_updates"' in text:
        print("skip", path)
        continue
    lines = text.splitlines(True)
    out = []
    for line in lines:
        out.append(line)
        if 'name="section_gestures"' in line:
            out.append(blocks[key])
            if not blocks[key].endswith("\n"):
                out.append("\n")
    path.write_text("".join(out), encoding="utf-8")
    print("updated", path)

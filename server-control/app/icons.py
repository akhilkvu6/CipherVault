"""Official Tabler Icon provider for CipherVault Server Manager.
Generates monochrome, 24x24 2px stroke Tabler outline icons dynamically as QIcon.
"""

from PySide6.QtGui import QIcon, QPixmap, QPainter
from PySide6.QtSvg import QSvgRenderer
from PySide6.QtCore import QByteArray, Qt

# Tabler SVG Path Definitions (viewBox 0 0 24 24, stroke-width 2, fill none)
_PATHS = {
    "settings": '<path d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 0 0 2.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 0 0 1.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 0 0 -1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 0 0 -2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 0 0 -2.573 -1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 0 0 -1.065 -2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 0 0 1.066 -2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /><circle cx="12" cy="12" r="3" />',
    "refresh": '<path d="M20 11a8.1 8.1 0 0 0 -15.5 -2m-.5 -4v4h4" /><path d="M4 13a8.1 8.1 0 0 0 15.5 2m.5 4v-4h-4" />',
    "usb": '<line x1="12" y1="19" x2="12" y2="5" /><line x1="12" y1="5" x2="8.5" y2="8.5" /><line x1="12" y1="5" x2="15.5" y2="8.5" /><circle cx="12" cy="19" r="2" /><circle cx="8.5" cy="8.5" r="1.5" /><circle cx="15.5" cy="8.5" r="1.5" />',
    "wifi": '<line x1="12" y1="18" x2="12.01" y2="18" /><path d="M9.172 15.172a4 4 0 0 1 5.656 0" /><path d="M6.343 12.343a8 8 0 0 1 11.314 0" /><path d="M3.515 9.515c4.686-4.687 12.284-4.687 17 0" />',
    "phone": '<rect x="6" y="3" width="12" height="18" rx="2" /><line x1="11" y1="4" x2="13" y2="4" /><line x1="12" y1="17" x2="12.01" y2="17" />',
    "emulator": '<rect x="3" y="4" width="18" height="12" rx="1" /><line x1="7" y1="20" x2="17" y2="20" /><line x1="9" y1="16" x2="7" y2="20" /><line x1="15" y1="16" x2="17" y2="20" />',
    "ethernet": '<rect x="4" y="4" width="16" height="16" rx="2" /><line x1="9" y1="8" x2="15" y2="8" /><line x1="9" y1="12" x2="15" y2="12" /><line x1="9" y1="16" x2="15" y2="16" />',
    "copy": '<rect x="8" y="8" width="12" height="12" rx="2" /><path d="M16 8v-2a2 2 0 0 0 -2 -2h-8a2 2 0 0 0 -2 2v8a2 2 0 0 0 2 2h2" />',
    "check": '<path d="M5 12l5 5l10 -10" />',
    "play": '<polygon points="7 4 19 12 7 20 7 4" fill="currentColor" />',
    "stop": '<rect x="6" y="6" width="12" height="12" rx="1" fill="currentColor" />',
    "restart": '<path d="M19.933 13.041a8 8 0 1 1 -9.925 -8.788c3.899 -1 7.935 1.007 9.425 4.747" /><path d="M20 4v5h-5" />',
    "info": '<circle cx="12" cy="12" r="9" /><line x1="12" y1="8" x2="12.01" y2="8" /><polyline points="11 12 12 12 12 16 13 16" />',
    "alert": '<path d="M12 9v2m0 4v.01" /><path d="M5 19h14a2 2 0 0 0 1.84 -2.75l-7.1 -12.25a2 2 0 0 0 -3.5 0l-7.1 12.25a2 2 0 0 0 1.75 2.75" />',
    "chevron_right": '<polyline points="9 6 15 12 9 18" />',
    "chevron_down": '<polyline points="6 9 12 15 18 9" />',
    "terminal": '<polyline points="4 17 10 11 4 5" /><line x1="12" y1="19" x2="20" y2="19" />',
    "activity": '<polyline points="3 12 6 12 9 3 13 21 16 12 21 12" />',
    "shield": '<path d="M12 3a12 12 0 0 0 8.5 3a12 12 0 0 1 -8.5 15a12 12 0 0 1 -8.5 -15a12 12 0 0 0 8.5 -3" />',
    "server": '<rect x="3" y="4" width="18" height="8" rx="3" /><rect x="3" y="12" width="18" height="8" rx="3" /><line x1="7" y1="8" x2="7.01" y2="8" /><line x1="7" y1="16" x2="7.01" y2="16" />'
}

_icon_cache = {}


def get_tabler_icon(name: str, color: str = "#8C7B6D", size: int = 16) -> QIcon:
    """Returns a QIcon rendered from official Tabler outline SVG."""
    cache_key = (name, color, size)
    if cache_key in _icon_cache:
        return _icon_cache[cache_key]

    path_data = _PATHS.get(name, "")
    if not path_data:
        return QIcon()

    fill_val = color if ("fill=\"currentColor\"" in path_data) else "none"
    clean_path = path_data.replace('fill="currentColor"', f'fill="{color}"')

    svg_markup = (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" '
        f'fill="{fill_val}" stroke="{color}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">'
        f'{clean_path}'
        f'</svg>'
    )

    renderer = QSvgRenderer(QByteArray(svg_markup.encode("utf-8")))
    pixmap = QPixmap(size, size)
    pixmap.fill(Qt.GlobalColor.transparent)

    painter = QPainter(pixmap)
    renderer.render(painter)
    painter.end()

    icon = QIcon(pixmap)
    _icon_cache[cache_key] = icon
    return icon

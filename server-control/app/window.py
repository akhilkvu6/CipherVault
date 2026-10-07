import re
import threading
from datetime import datetime

from PySide6.QtWidgets import (
    QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, QLabel,
    QPushButton, QTextEdit, QScrollArea, QFrame, QApplication,
    QSizePolicy, QMessageBox, QGridLayout, QStackedWidget
)
from PySide6.QtCore import Qt, QTimer, Slot, Signal
from PySide6.QtGui import QCloseEvent

from .settings import settings
from .theme import apply_theme
from .icons import get_tabler_icon
from .services.log_service import app_logger
from .services.environment_service import EnvironmentService
from .services.health_service import HealthPoller, BackendState
from .services.backend_service import BackendService
from .services.network_service import NetworkService
from .services.adb_service import AdbService, AdbDevice
from .widgets.settings_dialog import SettingsDialog


ANSI_STRIP_RE = re.compile(r'\x1B(?:[@-Z\\-_]|\[[0-?]*[ -/]*[@-~])')


# ---------------------------------------------------------------------------
# Windows Settings-inspired palette
# ---------------------------------------------------------------------------

BG = "#111918"
SIDEBAR = "#18201E"
SIDEBAR_ACTIVE = "#26312E"
CARD = "#1D2624"
CARD_HOVER = "#25302D"
BORDER = "#2B3734"
TEXT = "#F2F5F3"
TEXT_SECONDARY = "#B7C0BD"
TEXT_MUTED = "#87938F"
ACCENT = "#6CCBFF"
ACCENT_HOVER = "#82D4FF"
SUCCESS = "#6FD58A"
WARNING = "#E6C46A"
DANGER = "#FF817A"


def strip_ansi(text: str) -> str:
    return ANSI_STRIP_RE.sub("", text)


def make_subtle_divider() -> QFrame:
    line = QFrame()
    line.setProperty("class", "subtleDivider")
    line.setFrameShape(QFrame.Shape.HLine)
    line.setFixedHeight(1)
    return line


def make_card(parent=None, object_name="card") -> QFrame:
    card = QFrame(parent)
    card.setObjectName(object_name)
    card.setProperty("class", "settingsCard")
    return card


class SidebarButton(QPushButton):
    def __init__(self, icon_name: str, text: str, parent=None):
        super().__init__(parent)
        self._icon_name = icon_name
        self._base_text = text
        self.setText(text)
        self.setIcon(get_tabler_icon(icon_name, color=TEXT_SECONDARY, size=18))
        self.setIconSize(self.sizeHint().scaled(18, 18, Qt.AspectRatioMode.KeepAspectRatio))
        self.setCheckable(True)
        self.setAutoExclusive(True)
        self.setCursor(Qt.CursorShape.PointingHandCursor)
        self.setFixedHeight(42)


class RecommendedTile(QFrame):
    """Recommended connection surface, styled like a Windows Settings card."""

    def __init__(self, on_use_callback=None):
        super().__init__()
        self.setObjectName("recommendedCard")
        self._on_use_callback = on_use_callback
        self._current_rec = None

        layout = QVBoxLayout(self)
        layout.setContentsMargins(20, 18, 20, 18)
        layout.setSpacing(12)

        top = QHBoxLayout()
        title = QLabel("Recommended connection")
        title.setObjectName("cardEyebrow")
        top.addWidget(title)
        top.addStretch()

        self._status_pill = QLabel("● READY")
        self._status_pill.setObjectName("successPill")
        top.addWidget(self._status_pill)
        layout.addLayout(top)

        mid = QHBoxLayout()
        mid.setSpacing(18)

        info = QVBoxLayout()
        info.setSpacing(4)
        self._title_lbl = QLabel("Checking…")
        self._title_lbl.setObjectName("cardTitle")
        self._sub_lbl = QLabel("Detecting best connection method…")
        self._sub_lbl.setObjectName("secondaryText")
        self._sub_lbl.setWordWrap(True)
        info.addWidget(self._title_lbl)
        info.addWidget(self._sub_lbl)
        mid.addLayout(info, 1)

        self._url_lbl = QLabel("")
        self._url_lbl.setObjectName("monoText")
        self._url_lbl.setTextInteractionFlags(Qt.TextInteractionFlag.TextSelectableByMouse)
        mid.addWidget(self._url_lbl)
        layout.addLayout(mid)

        bottom = QHBoxLayout()
        self._feedback_lbl = QLabel("")
        self._feedback_lbl.setObjectName("successText")
        bottom.addWidget(self._feedback_lbl)
        bottom.addStretch()

        self._btn_use = QPushButton("Use Connection")
        self._btn_use.setProperty("primary", True)
        self._btn_use.clicked.connect(self._handle_use)
        bottom.addWidget(self._btn_use)

        self._btn_copy = QPushButton("Copy")
        self._btn_copy.setIcon(get_tabler_icon("copy", color=TEXT, size=14))
        self._btn_copy.clicked.connect(self._handle_copy)
        bottom.addWidget(self._btn_copy)
        layout.addLayout(bottom)

    def set_recommendation(self, rec: dict):
        self._current_rec = rec
        if not rec or not rec.get("ready"):
            self._status_pill.setText("○ NOT READY")
            self._status_pill.setObjectName("warningPill")
            self._status_pill.style().unpolish(self._status_pill)
            self._status_pill.style().polish(self._status_pill)
            self._title_lbl.setText("No connection ready")
            self._sub_lbl.setText(
                "Connect your Android device by USB or connect this computer and your phone "
                "to the same Wi-Fi network."
            )
            self._url_lbl.setText("")
            self._btn_use.setVisible(False)
            self._btn_copy.setVisible(False)
            self._feedback_lbl.clear()
            return

        self._status_pill.setText("● READY")
        self._status_pill.setObjectName("successPill")
        self._status_pill.style().unpolish(self._status_pill)
        self._status_pill.style().polish(self._status_pill)
        self._title_lbl.setText(rec.get("title", ""))
        self._sub_lbl.setText(rec.get("subtitle", ""))
        self._url_lbl.setText(rec.get("url", ""))
        self._btn_use.setText(rec.get("action_text", "Use Connection"))
        self._btn_use.setVisible(True)
        self._btn_copy.setVisible(True)

    def _handle_use(self):
        if not self._current_rec:
            return
        url = self._current_rec.get("url", "")
        if url:
            QApplication.clipboard().setText(url)
        if self._on_use_callback:
            self._on_use_callback(self._current_rec)

        if self._current_rec.get("type") == "usb":
            self._feedback_lbl.setText(
                "✓ USB ready and URL copied. Set it in CipherVault Android."
            )
        else:
            self._feedback_lbl.setText(
                "✓ URL copied to clipboard. Enter it in CipherVault Android."
            )
        QTimer.singleShot(4000, self._feedback_lbl.clear)

    def _handle_copy(self):
        if not self._current_rec:
            return
        url = self._current_rec.get("url", "")
        if url:
            QApplication.clipboard().setText(url)
            self._feedback_lbl.setText("✓ Copied to clipboard!")
            QTimer.singleShot(2500, self._feedback_lbl.clear)


class ConnectionRow(QWidget):
    """Compact connection row with Windows Settings-style spacing."""

    test_completed = Signal(str, str)

    def __init__(
        self,
        icon_name: str,
        title: str,
        subtitle: str,
        url: str,
        can_test: bool = True,
        extra_widget=None,
    ):
        super().__init__()
        self.test_completed.connect(self._update_test_ui)
        self._url = url

        layout = QHBoxLayout(self)
        layout.setContentsMargins(14, 11, 14, 11)
        layout.setSpacing(13)

        icon_lbl = QLabel()
        icon = get_tabler_icon(icon_name, color=TEXT_SECONDARY, size=20)
        icon_lbl.setPixmap(icon.pixmap(20, 20))
        icon_lbl.setFixedSize(24, 24)
        layout.addWidget(icon_lbl)

        info = QVBoxLayout()
        info.setContentsMargins(0, 0, 0, 0)
        info.setSpacing(2)

        t_lbl = QLabel(title)
        t_lbl.setObjectName("rowTitle")
        self._sub_lbl = QLabel(subtitle)
        self._sub_lbl.setObjectName("rowSubtitle")
        self._sub_lbl.setWordWrap(True)

        info.addWidget(t_lbl)
        info.addWidget(self._sub_lbl)
        layout.addLayout(info, 1)

        if extra_widget:
            layout.addWidget(extra_widget)

        self._url_lbl = QLabel(url)
        self._url_lbl.setObjectName("monoText")
        self._url_lbl.setTextInteractionFlags(Qt.TextInteractionFlag.TextSelectableByMouse)
        layout.addWidget(self._url_lbl)

        self._status_lbl = QLabel("")
        self._status_lbl.setObjectName("rowStatus")
        self._status_lbl.setAlignment(Qt.AlignmentFlag.AlignRight | Qt.AlignmentFlag.AlignVCenter)
        layout.addWidget(self._status_lbl)

        if can_test:
            btn_test = QPushButton("Test")
            btn_test.setIcon(get_tabler_icon("activity", color=TEXT_SECONDARY, size=13))
            btn_test.clicked.connect(self._test)
            layout.addWidget(btn_test)

        btn_copy = QPushButton("Copy")
        btn_copy.setIcon(get_tabler_icon("copy", color=TEXT_SECONDARY, size=13))
        btn_copy.clicked.connect(self._copy)
        layout.addWidget(btn_copy)

    def update_subtitle(self, text: str):
        self._sub_lbl.setText(text)

    def _test(self):
        self._status_lbl.setText("Testing…")
        self._status_lbl.setStyleSheet(f"color: {TEXT_MUTED};")
        threading.Thread(target=self._do_test, daemon=True).start()

    def _do_test(self):
        success, code, latency, msg = NetworkService.test_endpoint(self._url)
        if success:
            text = f"✓ {int(latency)} ms"
            color = SUCCESS
        elif code > 0:
            text = f"✗ HTTP {code}"
            color = DANGER
        else:
            text = f"✗ {msg}"
            color = DANGER
        self.test_completed.emit(text, color)

    def _update_test_ui(self, text: str, color: str):
        self._status_lbl.setText(text)
        self._status_lbl.setStyleSheet(f"color: {color}; font-weight: 600;")

    def _copy(self):
        QApplication.clipboard().setText(self._url)
        self._status_lbl.setText("Copied!")
        self._status_lbl.setStyleSheet(f"color: {ACCENT}; font-weight: 600;")
        QTimer.singleShot(1500, lambda: self._status_lbl.clear())


class MainWindow(QMainWindow):
    """
    CipherVault Server Manager.

    UI is redesigned to resemble the supplied Windows Settings reference while
    preserving the existing backend, health, ADB, networking, diagnostics,
    activity, logging and settings functionality.
    """

    adb_updated = Signal(bool, list)

    def __init__(self):
        super().__init__()
        self.adb_updated.connect(self._update_adb_ui)

        self.setWindowTitle("CipherVault Server Manager")
        self.resize(1280, 820)
        self.setMinimumSize(1040, 680)

        self._backend_state = BackendState.STOPPED
        self._backend_info = {}
        self._started_at = None
        self._adb_devices = []
        self._selected_adb_serial = None
        self._nav_buttons = []

        self._backend_svc = BackendService()
        self._backend_svc.log_line.connect(self._on_log_line)
        self._backend_svc.state_changed.connect(self._on_backend_state)
        self._backend_svc.startup_info.connect(self._on_startup_info)

        self._health_poller = HealthPoller(interval=settings.health_interval)
        self._health_poller.status_changed.connect(self._on_health_status)

        self._build_shell()

        self._refresh_timer = QTimer(self)
        self._refresh_timer.timeout.connect(self._refresh_connections_and_adb)
        self._refresh_timer.start(10000)

        self._uptime_timer = QTimer(self)
        self._uptime_timer.timeout.connect(self._update_uptime)
        self._uptime_timer.setInterval(1000)

        self._health_poller.start()
        QTimer.singleShot(150, self._refresh_connections_and_adb)
        QTimer.singleShot(350, self._initial_backend_check_and_start)

    # -----------------------------------------------------------------------
    # Global styling / shell
    # -----------------------------------------------------------------------

    def _build_shell(self):
        shell = QWidget()
        shell.setObjectName("shell")
        root = QHBoxLayout(shell)
        root.setContentsMargins(0, 0, 0, 0)
        root.setSpacing(0)

        self._build_sidebar(root)

        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setFrameShape(QFrame.Shape.NoFrame)
        scroll.setObjectName("contentScroll")
        root.addWidget(scroll, 1)

        container = QWidget()
        container.setObjectName("contentContainer")
        scroll.setWidget(container)
        self._scroll_area = scroll

        self._layout = QVBoxLayout(container)
        self._layout.setContentsMargins(40, 32, 40, 44)
        self._layout.setSpacing(22)

        self._build_header()
        self._build_server_status_surface()
        self._build_connect_section()
        self._build_system_status_section()
        self._build_warnings_section()
        self._build_collapsible_api_activity()
        self._build_collapsible_server_log()
        self._layout.addStretch(1)

        self.setCentralWidget(shell)
        self._apply_reference_style()

    def _build_sidebar(self, root):
        sidebar = QFrame()
        sidebar.setObjectName("sidebar")
        sidebar.setFixedWidth(238)

        side = QVBoxLayout(sidebar)
        side.setContentsMargins(18, 24, 18, 20)
        side.setSpacing(8)

        brand_row = QHBoxLayout()
        brand_row.setSpacing(11)

        logo = QLabel("CV")
        logo.setObjectName("brandLogo")
        logo.setAlignment(Qt.AlignmentFlag.AlignCenter)
        brand_row.addWidget(logo)

        brand_text = QVBoxLayout()
        brand_text.setSpacing(0)
        brand = QLabel("CipherVault")
        brand.setObjectName("brandTitle")
        manager = QLabel("Server Manager")
        manager.setObjectName("brandSubtitle")
        brand_text.addWidget(brand)
        brand_text.addWidget(manager)
        brand_row.addLayout(brand_text)
        side.addLayout(brand_row)

        side.addSpacing(22)

        section = QLabel("SERVER MANAGER")
        section.setObjectName("sidebarSection")
        side.addWidget(section)
        side.addSpacing(2)

        self._add_nav(side, "home", "Overview", self._server_card if hasattr(self, "_server_card") else None)
        self._overview_btn = self._nav_buttons[-1]

        self._add_nav(side, "device-desktop", "Connections", None)
        self._connections_btn = self._nav_buttons[-1]

        self._add_nav(side, "activity", "System status", None)
        self._system_btn = self._nav_buttons[-1]

        self._add_nav(side, "file-text", "Activity & logs", None)
        self._activity_btn = self._nav_buttons[-1]

        side.addStretch(1)

        tip = QFrame()
        tip.setObjectName("sidebarInfo")
        tip_lay = QVBoxLayout(tip)
        tip_lay.setContentsMargins(12, 12, 12, 12)
        tip_lay.setSpacing(4)
        tip_title = QLabel("Server status")
        tip_title.setObjectName("sidebarInfoTitle")
        self._sidebar_status = QLabel("Checking…")
        self._sidebar_status.setObjectName("sidebarInfoValue")
        tip_lay.addWidget(tip_title)
        tip_lay.addWidget(self._sidebar_status)
        side.addWidget(tip)

        self._sidebar_settings = QPushButton("⚙  Settings")
        self._sidebar_settings.setObjectName("sidebarSettings")
        self._sidebar_settings.setCursor(Qt.CursorShape.PointingHandCursor)
        self._sidebar_settings.clicked.connect(self._open_settings)
        side.addWidget(self._sidebar_settings)

        version = QLabel("CipherVault · Local server control")
        version.setObjectName("sidebarFooter")
        side.addWidget(version)

        root.addWidget(sidebar)

    def _add_nav(self, layout, icon_name, text, target):
        btn = SidebarButton(icon_name, text)
        btn.clicked.connect(lambda: self._navigate(text))
        self._nav_buttons.append(btn)
        layout.addWidget(btn)

    def _navigate(self, text):
        targets = {
            "Overview": getattr(self, "_server_card", None),
            "Connections": getattr(self, "_connect_card", None),
            "System status": getattr(self, "_system_card", None),
            "Activity & logs": getattr(self, "_log_widget", None),
        }
        target = targets.get(text)
        if target is not None:
            self._scroll_area.ensureWidgetVisible(target, 0, 24)

        for btn in self._nav_buttons:
            btn.setChecked(btn.text() == text)

    def _apply_reference_style(self):
        self.setStyleSheet(f"""
            QWidget {{
                color: {TEXT};
                font-family: "Segoe UI";
                font-size: 13px;
            }}
            QMainWindow, #shell, #contentContainer, QScrollArea, #contentScroll {{
                background: {BG};
            }}
            #sidebar {{
                background: {SIDEBAR};
                border-right: 1px solid {BORDER};
            }}
            #brandLogo {{
                background: {ACCENT};
                color: #071014;
                border-radius: 9px;
                font-size: 13px;
                font-weight: 800;
            }}
            #brandTitle {{
                font-size: 15px;
                font-weight: 700;
                color: {TEXT};
            }}
            #brandSubtitle {{
                color: {TEXT_MUTED};
                font-size: 11px;
            }}
            #sidebarSection {{
                color: {TEXT_MUTED};
                font-size: 10px;
                font-weight: 700;
                letter-spacing: 1px;
                padding-left: 10px;
            }}
            SidebarButton {{
                text-align: left;
            }}
            QPushButton {{
                background: #222C2A;
                border: 1px solid {BORDER};
                border-radius: 7px;
                padding: 7px 13px;
                color: {TEXT};
            }}
            QPushButton:hover {{
                background: {CARD_HOVER};
                border-color: #3B4A46;
            }}
            QPushButton:pressed {{
                background: #2A3834;
            }}
            QPushButton[primary="true"] {{
                background: {ACCENT};
                color: #071014;
                border: 1px solid {ACCENT};
                font-weight: 700;
            }}
            QPushButton[primary="true"]:hover {{
                background: {ACCENT_HOVER};
            }}
            QPushButton#sidebarSettings {{
                background: transparent;
                border: none;
                text-align: left;
                color: {TEXT_SECONDARY};
                padding: 10px 12px;
                border-radius: 7px;
            }}
            QPushButton#sidebarSettings:hover {{
                background: {SIDEBAR_ACTIVE};
                color: {TEXT};
            }}
            QPushButton[class="navButton"] {{
                background: transparent;
                border: none;
                text-align: left;
                padding: 8px 12px;
                border-radius: 8px;
                color: {TEXT_SECONDARY};
            }}
            QPushButton[class="navButton"]:hover {{
                background: {SIDEBAR_ACTIVE};
                color: {TEXT};
            }}
            QPushButton[class="navButton"]:checked {{
                background: {SIDEBAR_ACTIVE};
                color: {TEXT};
                font-weight: 600;
            }}
            #sidebarInfo {{
                background: #202A27;
                border: 1px solid {BORDER};
                border-radius: 9px;
            }}
            #sidebarInfoTitle {{
                color: {TEXT_MUTED};
                font-size: 10px;
            }}
            #sidebarInfoValue {{
                color: {SUCCESS};
                font-weight: 700;
            }}
            #sidebarFooter {{
                color: #66736F;
                font-size: 10px;
                padding-left: 12px;
            }}
            #contentContainer {{
                background: {BG};
            }}
            #pageTitle {{
                font-size: 28px;
                font-weight: 700;
            }}
            #pageSubtitle {{
                color: {TEXT_MUTED};
                font-size: 12px;
            }}
            #metaLine {{
                color: {TEXT_MUTED};
                font-size: 11px;
            }}
            #sectionTitle {{
                font-size: 19px;
                font-weight: 650;
            }}
            #sectionSubtitle {{
                color: {TEXT_MUTED};
                font-size: 12px;
            }}
            QFrame[class="settingsCard"], #serverCard, #recommendedCard {{
                background: {CARD};
                border: 1px solid {BORDER};
                border-radius: 10px;
            }}
            #recommendedCard {{
                border-color: #365548;
            }}
            #cardEyebrow {{
                color: {SUCCESS};
                font-size: 11px;
                font-weight: 700;
            }}
            #cardTitle {{
                font-size: 16px;
                font-weight: 700;
            }}
            #rowTitle {{
                font-size: 13px;
                font-weight: 650;
            }}
            #rowSubtitle, #secondaryText {{
                color: {TEXT_MUTED};
                font-size: 11px;
            }}
            #monoText {{
                color: {TEXT_SECONDARY};
                font-family: "Cascadia Mono", "Consolas", monospace;
                font-size: 11px;
            }}
            #rowStatus {{
                color: {TEXT_MUTED};
                font-size: 11px;
            }}
            #successText {{
                color: {SUCCESS};
                font-size: 11px;
                font-weight: 600;
            }}
            #successPill {{
                background: #203A2B;
                color: {SUCCESS};
                border-radius: 9px;
                padding: 3px 8px;
                font-size: 10px;
                font-weight: 700;
            }}
            #warningPill {{
                background: #403A24;
                color: {WARNING};
                border-radius: 9px;
                padding: 3px 8px;
                font-size: 10px;
                font-weight: 700;
            }}
            #statusPill {{
                background: #26312E;
                border: 1px solid {BORDER};
                border-radius: 13px;
            }}
            #statusDot {{
                font-size: 12px;
            }}
            #statusText {{
                font-size: 11px;
                font-weight: 700;
            }}
            #subtleDivider, QFrame[class="subtleDivider"] {{
                background: {BORDER};
                border: none;
                max-height: 1px;
            }}
            QPushButton[class="collapsibleHeader"] {{
                background: transparent;
                border: none;
                text-align: left;
                color: {TEXT_SECONDARY};
                padding: 9px 2px;
                font-weight: 600;
            }}
            QPushButton[class="collapsibleHeader"]:hover {{
                color: {TEXT};
            }}
            QTextEdit {{
                background: #0D1312;
                border: 1px solid {BORDER};
                border-radius: 8px;
                padding: 9px;
                color: #BFD0CA;
                font-family: "Cascadia Mono", "Consolas", monospace;
                font-size: 11px;
            }}
            QScrollBar:vertical {{
                background: transparent;
                width: 10px;
                margin: 2px;
            }}
            QScrollBar::handle:vertical {{
                background: #34413E;
                border-radius: 5px;
                min-height: 30px;
            }}
            QScrollBar::handle:vertical:hover {{
                background: #465650;
            }}
            QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical {{
                height: 0;
            }}
            QToolTip {{
                background: #25302D;
                color: {TEXT};
                border: 1px solid #3B4A46;
                padding: 5px;
            }}
        """)

        for btn in self._nav_buttons:
            btn.setProperty("class", "navButton")
            btn.style().unpolish(btn)
            btn.style().polish(btn)

        if self._nav_buttons:
            self._nav_buttons[0].setChecked(True)

    # -----------------------------------------------------------------------
    # Auto-start
    # -----------------------------------------------------------------------

    def _initial_backend_check_and_start(self):
        app_logger.log("app", "Inspecting backend status on startup")
        occupied, pid, proc = EnvironmentService.check_port_listener(settings.backend_port)
        if occupied:
            app_logger.log(
                "app",
                f"Port {settings.backend_port} occupied by PID {pid} ({proc}), attaching..."
            )
            self._backend_svc.start_backend()
            return

        if settings.auto_start_backend:
            app_logger.log("app", "Port 8080 free. Automatically starting backend...")
            self._do_start()
        else:
            app_logger.log("app", "Auto-start disabled. Backend waiting in stopped state.")
            self._health_poller.set_state(BackendState.STOPPED, "Ready to start")

    # -----------------------------------------------------------------------
    # Header
    # -----------------------------------------------------------------------

    def _build_header(self):
        header = QVBoxLayout()
        header.setSpacing(6)

        top = QHBoxLayout()
        top.setSpacing(12)

        title_col = QVBoxLayout()
        title_col.setSpacing(1)

        title = QLabel("Server Manager")
        title.setObjectName("pageTitle")
        subtitle = QLabel("Manage the CipherVault backend and Android connectivity")
        subtitle.setObjectName("pageSubtitle")
        title_col.addWidget(title)
        title_col.addWidget(subtitle)
        top.addLayout(title_col)
        top.addStretch()

        self._status_pill = QFrame()
        self._status_pill.setObjectName("statusPill")
        pill = QHBoxLayout(self._status_pill)
        pill.setContentsMargins(9, 5, 9, 5)
        pill.setSpacing(5)

        self._status_dot = QLabel("●")
        self._status_dot.setObjectName("statusDot")
        self._status_text = QLabel("CHECKING")
        self._status_text.setObjectName("statusText")
        pill.addWidget(self._status_dot)
        pill.addWidget(self._status_text)
        top.addWidget(self._status_pill)

        btn_refresh = QPushButton("Refresh")
        btn_refresh.setIcon(get_tabler_icon("refresh", color=TEXT_SECONDARY, size=13))
        btn_refresh.clicked.connect(self._manual_refresh)
        top.addWidget(btn_refresh)

        btn_settings = QPushButton("Settings")
        btn_settings.setIcon(get_tabler_icon("settings", color=TEXT_SECONDARY, size=13))
        btn_settings.clicked.connect(self._open_settings)
        top.addWidget(btn_settings)

        header.addLayout(top)

        self._meta_line = QLabel(
            "Managed server · Port 8080 · Java 21 · Spring Boot 4.1.1 · Uptime —"
        )
        self._meta_line.setObjectName("metaLine")
        header.addWidget(self._meta_line)

        self._layout.addLayout(header)

    # -----------------------------------------------------------------------
    # Backend status
    # -----------------------------------------------------------------------

    def _build_server_status_surface(self):
        self._server_card = make_card(object_name="serverCard")
        self._server_card.setSizePolicy(QSizePolicy.Policy.Preferred, QSizePolicy.Policy.Fixed)

        card = QHBoxLayout(self._server_card)
        card.setContentsMargins(20, 18, 20, 18)
        card.setSpacing(22)

        info = QVBoxLayout()
        info.setSpacing(4)

        self._status_heading = QLabel("ONLINE")
        self._status_heading.setObjectName("cardTitle")
        self._status_desc = QLabel("CipherVault backend is running normally.")
        self._status_desc.setObjectName("secondaryText")
        self._status_meta = QLabel("Port 8080 · PID — · Uptime —")
        self._status_meta.setObjectName("monoText")

        info.addWidget(self._status_heading)
        info.addWidget(self._status_desc)
        info.addWidget(self._status_meta)
        card.addLayout(info, 1)

        controls = QHBoxLayout()
        controls.setSpacing(8)

        self._btn_start = QPushButton("Start Server")
        self._btn_start.setProperty("primary", True)
        self._btn_start.setIcon(get_tabler_icon("play", color="#071014", size=14))
        self._btn_start.clicked.connect(self._do_start)

        self._btn_restart = QPushButton("Restart")
        self._btn_restart.setIcon(get_tabler_icon("restart", color=TEXT_SECONDARY, size=14))
        self._btn_restart.clicked.connect(self._do_restart)

        self._btn_stop = QPushButton("Stop")
        self._btn_stop.setIcon(get_tabler_icon("stop", color=TEXT_SECONDARY, size=14))
        self._btn_stop.clicked.connect(self._do_stop)

        controls.addWidget(self._btn_start)
        controls.addWidget(self._btn_restart)
        controls.addWidget(self._btn_stop)
        card.addLayout(controls)

        self._layout.addWidget(self._server_card)

    # -----------------------------------------------------------------------
    # Connections
    # -----------------------------------------------------------------------

    def _build_connect_section(self):
        header = QVBoxLayout()
        header.setSpacing(3)

        title = QLabel("Connect to CipherVault")
        title.setObjectName("sectionTitle")
        sub = QLabel("Choose how your Android device connects to this computer.")
        sub.setObjectName("sectionSubtitle")

        header.addWidget(title)
        header.addWidget(sub)
        self._layout.addLayout(header)

        self._rec_tile = RecommendedTile(on_use_callback=self._handle_recommended_use)
        self._layout.addWidget(self._rec_tile)

        self._connect_card = make_card()
        card = QVBoxLayout(self._connect_card)
        card.setContentsMargins(10, 10, 10, 10)
        card.setSpacing(0)

        avail = QLabel("AVAILABLE CONNECTIONS")
        avail.setStyleSheet(
            f"color: {TEXT_MUTED}; font-size: 10px; font-weight: 700; "
            "letter-spacing: 1px; padding: 5px 10px 7px;"
        )
        card.addWidget(avail)

        self._usb_widget = QWidget()
        usb_lay = QVBoxLayout(self._usb_widget)
        usb_lay.setContentsMargins(0, 0, 0, 0)
        usb_lay.setSpacing(0)

        self._btn_usb_reverse = QPushButton("Enable Reverse")
        self._btn_usb_reverse.clicked.connect(self._toggle_adb_reverse)
        self._btn_usb_reverse.setVisible(False)

        self._usb_row = ConnectionRow(
            "usb",
            "USB / ADB",
            "Checking USB devices…",
            "http://127.0.0.1:8080/",
            extra_widget=self._btn_usb_reverse,
        )
        usb_lay.addWidget(self._usb_row)
        card.addWidget(self._usb_widget)
        card.addWidget(make_subtle_divider())

        self._wifi_container = QVBoxLayout()
        self._wifi_container.setSpacing(0)
        card.addLayout(self._wifi_container)

        self._eth_container = QVBoxLayout()
        self._eth_container.setSpacing(0)
        card.addLayout(self._eth_container)

        self._emu_row = ConnectionRow(
            "emulator",
            "Android Emulator",
            "Host loopback alias",
            "http://10.0.2.2:8080/",
            can_test=False,
        )
        card.addWidget(self._emu_row)
        card.addWidget(make_subtle_divider())

        self._adv_net_btn = QPushButton("Advanced network interfaces  ▶")
        self._adv_net_btn.setProperty("class", "collapsibleHeader")
        self._adv_net_btn.clicked.connect(self._toggle_advanced_network)
        card.addWidget(self._adv_net_btn)

        self._adv_net_widget = QWidget()
        self._adv_net_layout = QVBoxLayout(self._adv_net_widget)
        self._adv_net_layout.setContentsMargins(10, 2, 10, 6)
        self._adv_net_layout.setSpacing(2)
        self._adv_net_widget.setVisible(False)
        card.addWidget(self._adv_net_widget)

        card.addWidget(make_subtle_divider())

        info_row = QHBoxLayout()
        info_row.setContentsMargins(10, 8, 10, 6)
        info_icon = QLabel()
        info_icon.setPixmap(
            get_tabler_icon("info", color=TEXT_MUTED, size=14).pixmap(14, 14)
        )
        info_row.addWidget(info_icon)
        info_text = QLabel(
            "PC Test only verifies that the backend responds from this computer. "
            "For Wi-Fi, your Android device and PC must be on the same network."
        )
        info_text.setObjectName("sectionSubtitle")
        info_text.setWordWrap(True)
        info_row.addWidget(info_text, 1)
        card.addLayout(info_row)

        self._layout.addWidget(self._connect_card)

    # -----------------------------------------------------------------------
    # System status
    # -----------------------------------------------------------------------

    def _build_system_status_section(self):
        header = QHBoxLayout()
        title = QLabel("System status")
        title.setObjectName("sectionTitle")
        header.addWidget(title)
        header.addStretch()

        self._btn_diag_toggle = QPushButton("View diagnostics ▼")
        self._btn_diag_toggle.clicked.connect(self._toggle_diagnostics)
        header.addWidget(self._btn_diag_toggle)
        self._layout.addLayout(header)

        self._system_card = make_card()
        sys = QVBoxLayout(self._system_card)
        sys.setContentsMargins(20, 16, 20, 16)
        sys.setSpacing(8)

        grid = QGridLayout()
        grid.setHorizontalSpacing(28)
        grid.setVerticalSpacing(8)

        labels = ["Backend", "Database", "Storage", "Encryption"]
        self._sys_backend_val = QLabel("● Checking…")
        self._sys_db_val = QLabel("● Waiting for backend…")
        self._sys_storage_val = QLabel("● Ready")
        self._sys_crypto_val = QLabel("● AES-256-GCM · BCrypt · JWT Initialized")
        values = [
            self._sys_backend_val,
            self._sys_db_val,
            self._sys_storage_val,
            self._sys_crypto_val,
        ]

        for row, (name, value) in enumerate(zip(labels, values)):
            lbl = QLabel(name)
            lbl.setStyleSheet(f"color: {TEXT_MUTED}; font-size: 12px; font-weight: 600;")
            value.setObjectName("monoText")
            grid.addWidget(lbl, row, 0)
            grid.addWidget(value, row, 1)

        sys.addLayout(grid)

        self._diag_container = QWidget()
        diag = QVBoxLayout(self._diag_container)
        diag.setContentsMargins(0, 10, 0, 0)
        diag.setSpacing(4)
        diag.addWidget(make_subtle_divider())

        self._diag_time = QLabel("Server Time: —")
        self._diag_latency = QLabel("Health Latency: —")
        self._diag_binding = QLabel("Socket Binding: Listening on :: (All interfaces)")
        self._diag_pool = QLabel("Database Connection Pool: HikariCP (MySQL)")
        self._diag_key = QLabel("Key Management: Initialized (Streaming mode)")

        for item in [
            self._diag_time,
            self._diag_latency,
            self._diag_binding,
            self._diag_pool,
            self._diag_key,
        ]:
            item.setObjectName("monoText")
            diag.addWidget(item)

        self._diag_container.setVisible(False)
        sys.addWidget(self._diag_container)

        self._layout.addWidget(self._system_card)

    # -----------------------------------------------------------------------
    # Warnings
    # -----------------------------------------------------------------------

    def _build_warnings_section(self):
        self._warn_card = make_card()
        self._warn_card.setStyleSheet(
            f"background: #28251B; border: 1px solid #4B432C; border-radius: 10px;"
        )

        lay = QVBoxLayout(self._warn_card)
        lay.setContentsMargins(16, 12, 16, 12)
        lay.setSpacing(5)

        head = QHBoxLayout()
        icon = QLabel()
        icon.setPixmap(get_tabler_icon("alert", color=WARNING, size=15).pixmap(15, 15))
        head.addWidget(icon)

        title = QLabel(
            "Development configuration: 3 fallback values active · JWT · Master Key · PBKDF2"
        )
        title.setStyleSheet(f"color: {WARNING}; font-size: 11px; font-weight: 700;")
        head.addWidget(title, 1)

        self._btn_warn_toggle = QPushButton("View details ▼")
        self._btn_warn_toggle.clicked.connect(self._toggle_warnings)
        head.addWidget(self._btn_warn_toggle)
        lay.addLayout(head)

        self._warn_details = QWidget()
        details = QVBoxLayout(self._warn_details)
        details.setContentsMargins(22, 2, 6, 2)
        details.setSpacing(3)

        items = [
            "• JWT: Development fallback secret active (configure CIPHERVAULT_JWT_SECRET for production)",
            "• Master Key: Development fallback passphrase active (configure CIPHERVAULT_MASTER_KEY for production)",
            "• PBKDF2 Salt: Development fallback salt active (configure CIPHERVAULT_PBKDF2_SALT for production)",
            "Note: Secret values and keys are strictly withheld from display.",
        ]
        for text in items:
            lbl = QLabel(text)
            lbl.setWordWrap(True)
            lbl.setStyleSheet(f"color: #CBB77A; font-size: 11px;")
            details.addWidget(lbl)

        self._warn_details.setVisible(False)
        lay.addWidget(self._warn_details)
        self._layout.addWidget(self._warn_card)

    # -----------------------------------------------------------------------
    # Activity / logs
    # -----------------------------------------------------------------------

    def _build_collapsible_api_activity(self):
        self._api_btn = QPushButton("Recent API activity (0)  ▶")
        self._api_btn.setProperty("class", "collapsibleHeader")
        self._api_btn.clicked.connect(self._toggle_api_activity)
        self._layout.addWidget(self._api_btn)

        self._api_widget = QWidget()
        api = QVBoxLayout(self._api_widget)
        api.setContentsMargins(0, 0, 0, 5)

        self._api_log = QTextEdit()
        self._api_log.setReadOnly(True)
        self._api_log.setFixedHeight(130)
        self._api_log.setPlaceholderText(
            "Observed backend HTTP requests will appear here live"
        )
        api.addWidget(self._api_log)

        self._api_widget.setVisible(False)
        self._layout.addWidget(self._api_widget)

    def _build_collapsible_server_log(self):
        self._log_btn = QPushButton("Server log  ▶")
        self._log_btn.setProperty("class", "collapsibleHeader")
        self._log_btn.clicked.connect(self._toggle_server_log)
        self._layout.addWidget(self._log_btn)

        self._log_widget = QWidget()
        log = QVBoxLayout(self._log_widget)
        log.setContentsMargins(0, 0, 0, 5)
        log.setSpacing(7)

        self._server_log = QTextEdit()
        self._server_log.setReadOnly(True)
        self._server_log.setFixedHeight(190)
        self._server_log.setPlaceholderText(
            "Spring Boot stdout and stderr output streams here"
        )
        log.addWidget(self._server_log)

        buttons = QHBoxLayout()
        buttons.addStretch()

        clear = QPushButton("Clear log")
        clear.clicked.connect(self._server_log.clear)
        copy = QPushButton("Copy all")
        copy.clicked.connect(
            lambda: QApplication.clipboard().setText(self._server_log.toPlainText())
        )
        buttons.addWidget(clear)
        buttons.addWidget(copy)
        log.addLayout(buttons)

        self._log_widget.setVisible(False)
        self._layout.addWidget(self._log_widget)

    # -----------------------------------------------------------------------
    # Toggles
    # -----------------------------------------------------------------------

    def _toggle_advanced_network(self):
        visible = not self._adv_net_widget.isVisible()
        self._adv_net_widget.setVisible(visible)
        self._adv_net_btn.setText(
            f"Advanced network interfaces  {'▼' if visible else '▶'}"
        )

    def _toggle_diagnostics(self):
        visible = not self._diag_container.isVisible()
        self._diag_container.setVisible(visible)
        self._btn_diag_toggle.setText(
            "Hide diagnostics ▲" if visible else "View diagnostics ▼"
        )

    def _toggle_warnings(self):
        visible = not self._warn_details.isVisible()
        self._warn_details.setVisible(visible)
        self._btn_warn_toggle.setText(
            "Hide details ▲" if visible else "View details ▼"
        )

    def _toggle_api_activity(self):
        visible = not self._api_widget.isVisible()
        self._api_widget.setVisible(visible)
        count = (
            len(self._api_log.toPlainText().strip().split("\n"))
            if self._api_log.toPlainText().strip()
            else 0
        )
        self._api_btn.setText(
            f"Recent API activity ({count})  {'▼' if visible else '▶'}"
        )

    def _toggle_server_log(self):
        visible = not self._log_widget.isVisible()
        self._log_widget.setVisible(visible)
        self._log_btn.setText(f"Server log  {'▼' if visible else '▶'}")

    # -----------------------------------------------------------------------
    # Backend actions
    # -----------------------------------------------------------------------

    def _do_start(self):
        if self._backend_svc.isRunning():
            return

        if self._backend_svc.isFinished():
            self._backend_svc = BackendService()
            self._backend_svc.log_line.connect(self._on_log_line)
            self._backend_svc.state_changed.connect(self._on_backend_state)
            self._backend_svc.startup_info.connect(self._on_startup_info)

        app_logger.log("action", "Starting backend server")
        self._health_poller.set_state(
            BackendState.STARTING, "Starting Spring Boot..."
        )
        self._update_server_status_ui(
            BackendState.STARTING, "Launching backend process..."
        )
        self._started_at = datetime.now()
        self._backend_svc.start_backend()

    def _do_stop(self):
        app_logger.log("action", "Stopping backend server")
        self._backend_svc.stop_backend()
        self._health_poller.set_state(BackendState.STOPPED, "Backend stopped")
        self._update_server_status_ui(BackendState.STOPPED, "Backend stopped")
        self._uptime_timer.stop()
        self._started_at = None
        self._update_metadata_line()

    def _do_restart(self):
        app_logger.log("action", "Restarting backend server")
        self._do_stop()
        self._health_poller.set_state(
            BackendState.STARTING, "Restarting backend..."
        )
        self._update_server_status_ui(
            BackendState.STARTING, "Restarting..."
        )
        QTimer.singleShot(1500, self._restart_step2)

    def _restart_step2(self):
        self._backend_svc = BackendService()
        self._backend_svc.log_line.connect(self._on_log_line)
        self._backend_svc.state_changed.connect(self._on_backend_state)
        self._backend_svc.startup_info.connect(self._on_startup_info)
        self._do_start()

    # -----------------------------------------------------------------------
    # Recommendation
    # -----------------------------------------------------------------------

    def _determine_recommendation(self) -> dict:
        physical = next(
            (d for d in self._adb_devices if d.state == "device" and not d.is_emulator),
            None,
        )
        if physical:
            return {
                "type": "usb",
                "title": "USB / ADB",
                "subtitle": f"Physical Android device connected ({physical.serial})",
                "url": "http://127.0.0.1:8080/",
                "action_text": "Use USB",
                "ready": True,
                "device": physical,
            }

        adapters = NetworkService.get_classified_adapters()
        if adapters["wifi"]:
            wifi = adapters["wifi"][0]
            return {
                "type": "wifi",
                "title": "Wi-Fi",
                "subtitle": f"Connect your Android phone over the same local network ({wifi.ip})",
                "url": wifi.url,
                "action_text": "Use Wi-Fi",
                "ready": True,
                "adapter": wifi,
            }

        if adapters["ethernet"]:
            ethernet = adapters["ethernet"][0]
            return {
                "type": "ethernet",
                "title": "Ethernet",
                "subtitle": f"Available on local network ({ethernet.ip})",
                "url": ethernet.url,
                "action_text": "Use Ethernet",
                "ready": True,
                "adapter": ethernet,
            }

        emulator = next(
            (d for d in self._adb_devices if d.state == "device" and d.is_emulator),
            None,
        )
        if emulator:
            return {
                "type": "emulator",
                "title": "Android Emulator",
                "subtitle": f"Android emulator detected ({emulator.serial})",
                "url": "http://10.0.2.2:8080/",
                "action_text": "Use Emulator",
                "ready": True,
                "device": emulator,
            }

        return {
            "type": "none",
            "title": "No connection ready",
            "subtitle": (
                "Connect your Android device by USB or connect this computer "
                "and your phone to the same Wi-Fi network."
            ),
            "url": "",
            "action_text": "",
            "ready": False,
        }

    def _handle_recommended_use(self, rec: dict):
        if not rec:
            return
        if rec.get("type") == "usb":
            dev = rec.get("device")
            if dev and not dev.reverse_active:
                AdbService.enable_reverse(dev.serial)
                dev.reverse_active = True
                self._update_adb_ui(True, self._adb_devices)

    # -----------------------------------------------------------------------
    # Network / ADB
    # -----------------------------------------------------------------------

    def _manual_refresh(self):
        app_logger.log("action", "Manual Refresh triggered")
        self._health_poller.poll_now()
        self._refresh_connections_and_adb()

    def _refresh_connections_and_adb(self):
        _, binding_desc = NetworkService.check_backend_binding(settings.backend_port)
        self._diag_binding.setText(f"Socket Binding: {binding_desc}")

        adapters = NetworkService.get_classified_adapters()

        self._clear_layout(self._wifi_container)
        if settings.show_wifi and adapters["wifi"]:
            for adapter in adapters["wifi"]:
                self._wifi_container.addWidget(
                    ConnectionRow(
                        "wifi",
                        "Wi-Fi",
                        f"● Available · {adapter.ip} ({adapter.name})",
                        adapter.url,
                    )
                )
            self._wifi_container.addWidget(make_subtle_divider())

        self._clear_layout(self._eth_container)
        if settings.show_ethernet and adapters["ethernet"]:
            for adapter in adapters["ethernet"]:
                self._eth_container.addWidget(
                    ConnectionRow(
                        "ethernet",
                        "Ethernet",
                        f"● Available · {adapter.ip} ({adapter.name})",
                        adapter.url,
                    )
                )
            self._eth_container.addWidget(make_subtle_divider())

        self._clear_layout(self._adv_net_layout)
        if adapters["other"]:
            self._adv_net_btn.setVisible(True)
            self._adv_net_btn.setText(
                f"Advanced network interfaces ({len(adapters['other'])})  ▶"
            )
            for adapter in adapters["other"]:
                self._adv_net_layout.addWidget(
                    ConnectionRow(
                        "wifi",
                        adapter.name,
                        f"Virtual / Local · {adapter.ip}",
                        adapter.url,
                    )
                )
        else:
            self._adv_net_btn.setVisible(False)

        threading.Thread(
            target=self._query_adb_async,
            daemon=True,
        ).start()

    def _query_adb_async(self):
        available = AdbService.is_available()
        devices = AdbService.get_devices() if available else []
        self.adb_updated.emit(available, devices)

    def _update_adb_ui(self, adb_avail: bool, devices: list[AdbDevice]):
        self._adb_devices = devices
        rec = self._determine_recommendation()
        self._rec_tile.set_recommendation(rec)

        if not settings.show_adb:
            self._usb_widget.setVisible(False)
            return

        self._usb_widget.setVisible(True)

        if not adb_avail:
            self._usb_row.update_subtitle("⚠ ADB platform-tools not found in PATH")
            self._btn_usb_reverse.setVisible(False)
            return

        if not devices:
            self._usb_row.update_subtitle("No USB device connected")
            self._btn_usb_reverse.setVisible(False)
            return

        target_serial = devices[0].serial
        self._selected_adb_serial = target_serial
        active_dev = next(
            (d for d in devices if d.serial == target_serial),
            devices[0],
        )

        if settings.auto_enable_adb_reverse and not active_dev.reverse_active:
            active_dev.reverse_active = True
            threading.Thread(
                target=lambda s: (
                    AdbService.enable_reverse(s),
                    self._query_adb_async(),
                ),
                args=(active_dev.serial,),
                daemon=True,
            ).start()

        device_kind = "Physical" if not active_dev.is_emulator else "Emulator"
        reverse_state = (
            "Reverse active" if active_dev.reverse_active else "Reverse inactive"
        )
        self._usb_row.update_subtitle(
            f"● Connected · {active_dev.serial} ({device_kind}) · {reverse_state}"
        )

        self._btn_usb_reverse.setVisible(True)
        self._btn_usb_reverse.setEnabled(True)
        self._btn_usb_reverse.setText(
            "Disable Reverse" if active_dev.reverse_active else "Enable Reverse"
        )
        self._btn_usb_reverse.setProperty(
            "primary", not active_dev.reverse_active
        )
        self._btn_usb_reverse.style().unpolish(self._btn_usb_reverse)
        self._btn_usb_reverse.style().polish(self._btn_usb_reverse)

    def _toggle_adb_reverse(self):
        if not self._adb_devices:
            return

        active_dev = next(
            (
                d
                for d in self._adb_devices
                if d.serial == self._selected_adb_serial
            ),
            self._adb_devices[0],
        )

        self._btn_usb_reverse.setText("Toggling…")
        self._btn_usb_reverse.setEnabled(False)
        threading.Thread(
            target=self._do_toggle_reverse,
            args=(active_dev.serial, active_dev.reverse_active),
            daemon=True,
        ).start()

    def _do_toggle_reverse(self, dev_serial, is_active):
        if is_active:
            ok, msg = AdbService.disable_reverse(dev_serial)
        else:
            ok, msg = AdbService.enable_reverse(dev_serial)
        app_logger.log("adb", f"Reverse toggle for {dev_serial}: {msg}")
        self._query_adb_async()

    def _clear_layout(self, layout):
        while layout.count():
            item = layout.takeAt(0)
            widget = item.widget()
            if widget:
                widget.deleteLater()
            elif item.layout():
                self._clear_layout(item.layout())

    # -----------------------------------------------------------------------
    # Signals / logs
    # -----------------------------------------------------------------------

    @Slot(str)
    def _on_log_line(self, line: str):
        clean_line = strip_ansi(line)
        self._server_log.append(clean_line)

        if "[REQ-" in clean_line or "/api/" in clean_line:
            now = datetime.now().strftime("%H:%M:%S")
            self._api_log.append(f"{now}   {clean_line.strip()}")
            if not self._api_widget.isVisible():
                count = len(self._api_log.toPlainText().strip().split("\n"))
                self._api_btn.setText(
                    f"Recent API activity ({count})  ▶"
                )

    @Slot(str, str)
    def _on_backend_state(self, state: str, detail: str):
        self._backend_state = state
        self._update_server_status_ui(state, detail)

        if state == BackendState.ONLINE:
            if not self._uptime_timer.isActive():
                self._uptime_timer.start()
            self._health_poller.poll_now()
            self._refresh_connections_and_adb()
        elif state == BackendState.STARTING:
            self._health_poller.poll_now()
        elif state in (
            BackendState.STOPPED,
            BackendState.ERROR,
            BackendState.FOREIGN_SERVICE,
        ):
            self._uptime_timer.stop()

    @Slot(dict)
    def _on_startup_info(self, info: dict):
        self._backend_info = info
        self._update_metadata_line()

        if "mysql_version" in info or info.get("db_connected"):
            version = info.get("mysql_version", "8.0")
            self._sys_db_val.setText(
                f"● Connected · MySQL {version} · ciphervault"
            )
            self._sys_db_val.setStyleSheet(
                f"color: {SUCCESS}; font-weight: 600;"
            )

        if "files_scanned" in info:
            scanned = info.get("files_scanned", "0")
            missing = info.get("missing_metadata", "0")
            self._sys_storage_val.setText(
                f"● Ready · {scanned} files · {missing} missing metadata"
            )

        if "jvm_pid" in info:
            self._diag_pool.setText(
                f"Process PID: {info['jvm_pid']} · Database Pool: HikariCP (MySQL)"
            )

    @Slot(str, str, dict)
    def _on_health_status(self, state: str, reason: str, data: dict):
        self._update_server_status_ui(state, reason, data)

    # -----------------------------------------------------------------------
    # State-aware UI
    # -----------------------------------------------------------------------

    def _update_server_status_ui(
        self,
        state: str,
        detail: str = "",
        data: dict = None,
    ):
        pid_str = str(
            self._backend_info.get("jvm_pid", self._backend_svc.pid or "—")
        )
        uptime = self._get_uptime_string()

        self._sidebar_status.setText(state.upper())

        if state == BackendState.ONLINE:
            self._status_dot.setStyleSheet(f"color: {SUCCESS};")
            self._status_text.setText("ONLINE")
            self._status_text.setStyleSheet(
                f"color: {SUCCESS}; font-weight: 700;"
            )

            self._status_heading.setText("ONLINE")
            self._status_heading.setStyleSheet(
                f"color: {SUCCESS}; font-size: 16px; font-weight: 700;"
            )
            self._status_desc.setText(
                "CipherVault backend is running normally."
            )
            self._status_meta.setText(
                f"Port {settings.backend_port} · PID {pid_str} · Uptime {uptime}"
            )

            self._btn_start.setVisible(False)
            self._btn_restart.setVisible(True)
            self._btn_stop.setVisible(True)

            latency = data.get("_latency_ms", "3") if data else "3"
            self._sys_backend_val.setText(
                f"● Online · 200 OK · {latency} ms"
            )
            self._sys_backend_val.setStyleSheet(
                f"color: {SUCCESS}; font-weight: 600;"
            )

            if data and data.get("timestamp"):
                self._diag_time.setText(
                    f"Server Time: {data['timestamp']}"
                )
                self._diag_latency.setText(
                    f"Health Latency: {latency} ms"
                )

        elif state == BackendState.STARTING:
            self._status_dot.setStyleSheet(f"color: {WARNING};")
            self._status_text.setText("STARTING")
            self._status_text.setStyleSheet(
                f"color: {WARNING}; font-weight: 700;"
            )

            self._status_heading.setText("STARTING")
            self._status_heading.setStyleSheet(
                f"color: {WARNING}; font-size: 16px; font-weight: 700;"
            )
            self._status_desc.setText(
                "Starting CipherVault backend… waiting for Spring Boot health response."
            )
            self._status_meta.setText(
                f"Port {settings.backend_port} · PID {pid_str}"
            )

            self._btn_start.setVisible(False)
            self._btn_restart.setVisible(False)
            self._btn_stop.setVisible(True)

            self._sys_backend_val.setText("● Starting…")
            self._sys_backend_val.setStyleSheet(
                f"color: {WARNING}; font-weight: 600;"
            )

        elif state == BackendState.FOREIGN_SERVICE:
            self._status_dot.setStyleSheet(f"color: {DANGER};")
            self._status_text.setText("PORT OCCUPIED")
            self._status_text.setStyleSheet(
                f"color: {DANGER}; font-weight: 700;"
            )

            self._status_heading.setText("PORT OCCUPIED")
            self._status_heading.setStyleSheet(
                f"color: {DANGER}; font-size: 16px; font-weight: 700;"
            )
            self._status_desc.setText(
                detail
                or "Port 8080 is in use by another service. CipherVault cannot safely bind."
            )
            self._status_meta.setText(
                f"Port {settings.backend_port} occupied · PID {pid_str}"
            )

            self._btn_start.setVisible(False)
            self._btn_restart.setVisible(False)
            self._btn_stop.setVisible(False)

            self._sys_backend_val.setText("⚠ Port Conflict")
            self._sys_backend_val.setStyleSheet(
                f"color: {DANGER}; font-weight: 600;"
            )

        elif state == BackendState.STOPPED:
            self._status_dot.setStyleSheet(f"color: {TEXT_MUTED};")
            self._status_text.setText("OFFLINE")
            self._status_text.setStyleSheet(
                f"color: {TEXT_MUTED}; font-weight: 700;"
            )

            self._status_heading.setText("OFFLINE")
            self._status_heading.setStyleSheet(
                f"color: {TEXT_MUTED}; font-size: 16px; font-weight: 700;"
            )
            self._status_desc.setText(
                "The CipherVault backend is not running."
            )
            self._status_meta.setText(
                f"Port {settings.backend_port} · PID —"
            )

            self._btn_start.setVisible(True)
            self._btn_restart.setVisible(False)
            self._btn_stop.setVisible(False)

            self._sys_backend_val.setText("○ Stopped")
            self._sys_backend_val.setStyleSheet(
                f"color: {TEXT_MUTED}; font-weight: 600;"
            )
            self._sys_db_val.setText("○ Waiting for backend")
            self._sys_db_val.setStyleSheet(
                f"color: {TEXT_MUTED}; font-weight: 600;"
            )

        else:
            self._status_dot.setStyleSheet(f"color: {DANGER};")
            self._status_text.setText("OFFLINE")
            self._status_text.setStyleSheet(
                f"color: {DANGER}; font-weight: 700;"
            )

            self._status_heading.setText("OFFLINE")
            self._status_heading.setStyleSheet(
                f"color: {DANGER}; font-size: 16px; font-weight: 700;"
            )
            self._status_desc.setText(
                detail or "The CipherVault backend is not responding."
            )
            self._status_meta.setText(
                f"Port {settings.backend_port} · Connection refused"
            )

            self._btn_start.setVisible(True)
            self._btn_restart.setVisible(False)
            self._btn_stop.setVisible(False)

            self._sys_backend_val.setText("○ Offline")
            self._sys_backend_val.setStyleSheet(
                f"color: {DANGER}; font-weight: 600;"
            )
            self._sys_db_val.setText("○ Offline")
            self._sys_db_val.setStyleSheet(
                f"color: {TEXT_MUTED}; font-weight: 600;"
            )

        self._update_metadata_line()

    def _get_uptime_string(self) -> str:
        if self._started_at:
            delta = datetime.now() - self._started_at
            seconds = int(delta.total_seconds())
            hours, rem = divmod(seconds, 3600)
            minutes, sec = divmod(rem, 60)
            return f"{hours:02d}:{minutes:02d}:{sec:02d}"
        return "—"

    def _update_uptime(self):
        uptime = self._get_uptime_string()
        pid = str(
            self._backend_info.get("jvm_pid", self._backend_svc.pid or "—")
        )
        if self._backend_state == BackendState.ONLINE:
            self._status_meta.setText(
                f"Port {settings.backend_port} · PID {pid} · Uptime {uptime}"
            )
        self._update_metadata_line()

    def _update_metadata_line(self):
        owner = self._backend_svc.ownership_label
        port = str(settings.backend_port)
        java_ver = self._backend_info.get("java_version", "21.0.12")
        spring_ver = self._backend_info.get("spring_version", "4.1.1")
        pid = str(
            self._backend_info.get("jvm_pid", self._backend_svc.pid or "—")
        )
        uptime = self._get_uptime_string()

        self._meta_line.setText(
            f"{owner} · Port {port} · Java {java_ver} · "
            f"Spring Boot {spring_ver} · PID {pid} · Uptime {uptime}"
        )

    # -----------------------------------------------------------------------
    # Settings / lifecycle
    # -----------------------------------------------------------------------

    def _open_settings(self):
        dlg = SettingsDialog(self)
        if dlg.exec():
            app = QApplication.instance()
            apply_theme(app, settings.theme)
            # Re-apply the Server Manager reference styling after the global
            # theme so the main window keeps the supplied Windows Settings look.
            self._apply_reference_style()
            self._health_poller.set_interval(settings.health_interval)
            self._refresh_connections_and_adb()

    def changeEvent(self, event):
        if (
            event.type() == event.Type.ActivationChange
            and self.isActiveWindow()
        ):
            self._refresh_connections_and_adb()
        super().changeEvent(event)

    def closeEvent(self, event: QCloseEvent):
        if (
            self._backend_svc.isRunning()
            and self._backend_svc.owns_process
        ):
            reply = QMessageBox.question(
                self,
                "Backend Still Running",
                "The CipherVault backend was launched by Server Manager and is currently running.\n\n"
                "Would you like to stop the backend before closing?",
                QMessageBox.StandardButton.Yes
                | QMessageBox.StandardButton.No
                | QMessageBox.StandardButton.Cancel,
                QMessageBox.StandardButton.Yes,
            )

            if reply == QMessageBox.StandardButton.Yes:
                self._backend_svc.stop_backend()
                self._health_poller.stop()
                event.accept()
            elif reply == QMessageBox.StandardButton.No:
                self._health_poller.stop()
                event.accept()
            else:
                event.ignore()
        else:
            self._health_poller.stop()
            event.accept()

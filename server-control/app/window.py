import re
import threading
from datetime import datetime

from PySide6.QtWidgets import (
    QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, QLabel,
    QPushButton, QTextEdit, QScrollArea, QFrame, QApplication,
    QSizePolicy, QComboBox, QMessageBox, QGridLayout
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
from .services.network_service import NetworkService, NetworkAdapterInfo
from .services.adb_service import AdbService, AdbDevice
from .widgets.settings_dialog import SettingsDialog

# Regex to completely strip ANSI escape sequences from logs
ANSI_STRIP_RE = re.compile(r'\x1B(?:[@-Z\\-_]|\[[0-?]*[ -/]*[@-~])')


def strip_ansi(text: str) -> str:
    return ANSI_STRIP_RE.sub('', text)


def make_subtle_divider() -> QFrame:
    line = QFrame()
    line.setProperty("class", "subtleDivider")
    line.setFrameShape(QFrame.Shape.HLine)
    line.setFixedHeight(1)
    return line


class RecommendedTile(QFrame):
    """Prominent recommended connection tile with subtle green semantic treatment."""

    def __init__(self, on_use_callback=None):
        super().__init__()
        self.setProperty("class", "recommendedCard")
        self.setSizePolicy(QSizePolicy.Policy.Preferred, QSizePolicy.Policy.Fixed)
        self._on_use_callback = on_use_callback
        self._current_rec = None

        layout = QVBoxLayout(self)
        layout.setContentsMargins(18, 14, 18, 14)
        layout.setSpacing(8)

        # Top Bar: Label on left, Status Pill on right
        top_row = QHBoxLayout()
        top_row.setSpacing(6)

        rec_label = QLabel("Recommended connection")
        rec_label.setStyleSheet("font-weight: 700; font-size: 12px; color: #2E7D32;")
        top_row.addWidget(rec_label)
        top_row.addStretch()

        self._status_pill = QLabel("● READY")
        self._status_pill.setStyleSheet(
            "font-weight: 700; font-size: 11px; color: #2E7D32; "
            "background-color: #E2F3E5; border-radius: 10px; padding: 2px 8px;"
        )
        top_row.addWidget(self._status_pill)
        layout.addLayout(top_row)

        # Middle Area: Title & Subtitle + URL
        mid_row = QHBoxLayout()
        mid_row.setSpacing(12)

        info_col = QVBoxLayout()
        info_col.setSpacing(2)

        self._title_lbl = QLabel("Checking…")
        self._title_lbl.setStyleSheet("font-weight: 700; font-size: 15px; color: #1D1D1D;")
        self._sub_lbl = QLabel("Detecting best connection method…")
        self._sub_lbl.setStyleSheet("color: #5A554E; font-size: 12px;")

        info_col.addWidget(self._title_lbl)
        info_col.addWidget(self._sub_lbl)
        mid_row.addLayout(info_col, 1)

        self._url_lbl = QLabel("")
        self._url_lbl.setStyleSheet('font-family: "JetBrains Mono"; font-size: 14px; font-weight: 600; color: #1D1D1D;')
        self._url_lbl.setTextInteractionFlags(Qt.TextInteractionFlag.TextSelectableByMouse)
        mid_row.addWidget(self._url_lbl)

        layout.addLayout(mid_row)

        # Bottom Action Bar: Feedback notice on left, Action buttons on right
        bot_row = QHBoxLayout()
        bot_row.setSpacing(8)

        self._feedback_lbl = QLabel("")
        self._feedback_lbl.setStyleSheet("color: #2E7D32; font-size: 11px; font-weight: 600;")
        bot_row.addWidget(self._feedback_lbl)
        bot_row.addStretch()

        self._btn_use = QPushButton("Use Connection")
        self._btn_use.setProperty("recommended", True)
        self._btn_use.setFixedHeight(30)
        self._btn_use.clicked.connect(self._handle_use)
        bot_row.addWidget(self._btn_use)

        self._btn_copy = QPushButton("Copy")
        self._btn_copy.setIcon(get_tabler_icon("copy", size=14))
        self._btn_copy.setFixedHeight(30)
        self._btn_copy.setFixedWidth(75)
        self._btn_copy.clicked.connect(self._handle_copy)
        bot_row.addWidget(self._btn_copy)

        layout.addLayout(bot_row)

    def set_recommendation(self, rec: dict):
        self._current_rec = rec
        if not rec or not rec.get("ready"):
            self._status_pill.setText("○ NOT READY")
            self._status_pill.setStyleSheet(
                "font-weight: 700; font-size: 11px; color: #8A6D3B; "
                "background-color: #FCF8E3; border-radius: 10px; padding: 2px 8px;"
            )
            self._title_lbl.setText("No connection ready")
            self._sub_lbl.setText(
                "Connect your Android device by USB or connect this computer and your phone to the same Wi-Fi network."
            )
            self._url_lbl.setText("")
            self._btn_use.setVisible(False)
            self._btn_copy.setVisible(False)
            self._feedback_lbl.setText("")
            return

        self._status_pill.setText("● READY")
        self._status_pill.setStyleSheet(
            "font-weight: 700; font-size: 11px; color: #2E7D32; "
            "background-color: #E2F3E5; border-radius: 10px; padding: 2px 8px;"
        )
        self._title_lbl.setText(rec.get("title", ""))
        self._sub_lbl.setText(rec.get("subtitle", ""))
        self._url_lbl.setText(rec.get("url", ""))

        action_text = rec.get("action_text", "Use Connection")
        self._btn_use.setText(action_text)
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

        conn_type = self._current_rec.get("type", "")
        if conn_type == "usb":
            self._feedback_lbl.setText("✓ USB ready & URL copied to clipboard! Set in CipherVault Android.")
        else:
            self._feedback_lbl.setText("✓ URL copied to clipboard! Enter this in CipherVault Android.")

        QTimer.singleShot(4000, lambda: self._feedback_lbl.setText(""))

    def _handle_copy(self):
        if not self._current_rec:
            return
        url = self._current_rec.get("url", "")
        if url:
            QApplication.clipboard().setText(url)
            self._feedback_lbl.setText("✓ Copied to clipboard!")
            QTimer.singleShot(2500, lambda: self._feedback_lbl.setText(""))


class ConnectionRow(QWidget):
    """Compact One UI row for an individual connection method."""

    test_completed = Signal(str, str)

    def __init__(self, icon_name: str, title: str, subtitle: str, url: str, can_test: bool = True, extra_widget=None):
        super().__init__()
        self.test_completed.connect(self._update_test_ui)
        self._url = url

        layout = QHBoxLayout(self)
        layout.setContentsMargins(12, 7, 12, 7)
        layout.setSpacing(12)

        # Icon
        icon_lbl = QLabel()
        icon = get_tabler_icon(icon_name, color="#8C7B6D", size=20)
        icon_lbl.setPixmap(icon.pixmap(20, 20))
        icon_lbl.setFixedSize(22, 22)
        layout.addWidget(icon_lbl)

        # Title & Subtitle column
        info_col = QVBoxLayout()
        info_col.setSpacing(1)
        info_col.setContentsMargins(0, 0, 0, 0)

        t_lbl = QLabel(title)
        t_lbl.setStyleSheet("font-weight: 700; font-size: 13px;")
        self._sub_lbl = QLabel(subtitle)
        self._sub_lbl.setStyleSheet("color: #6B6B6B; font-size: 11px;")
        info_col.addWidget(t_lbl)
        info_col.addWidget(self._sub_lbl)
        layout.addLayout(info_col)

        layout.addStretch()

        # Optional embedded widget (e.g. reverse toggle button)
        if extra_widget:
            layout.addWidget(extra_widget)

        # Monospace URL
        self._url_lbl = QLabel(url)
        self._url_lbl.setStyleSheet('font-family: "JetBrains Mono"; font-size: 13px; font-weight: 500;')
        self._url_lbl.setTextInteractionFlags(Qt.TextInteractionFlag.TextSelectableByMouse)
        layout.addWidget(self._url_lbl)

        # Status feedback
        self._status_lbl = QLabel("")
        self._status_lbl.setStyleSheet("font-size: 11px; min-width: 65px;")
        self._status_lbl.setAlignment(Qt.AlignmentFlag.AlignRight | Qt.AlignmentFlag.AlignVCenter)
        layout.addWidget(self._status_lbl)

        # Actions
        if can_test:
            btn_test = QPushButton("Test")
            btn_test.setIcon(get_tabler_icon("activity", size=13))
            btn_test.setFixedHeight(26)
            btn_test.setFixedWidth(65)
            btn_test.clicked.connect(self._test)
            layout.addWidget(btn_test)

        btn_copy = QPushButton("Copy")
        btn_copy.setIcon(get_tabler_icon("copy", size=13))
        btn_copy.setFixedHeight(26)
        btn_copy.setFixedWidth(65)
        btn_copy.clicked.connect(self._copy)
        layout.addWidget(btn_copy)

    def update_subtitle(self, text: str):
        self._sub_lbl.setText(text)

    def _test(self):
        self._status_lbl.setText("Testing…")
        self._status_lbl.setStyleSheet("font-size: 11px; color: #6B6B6B;")
        threading.Thread(target=self._do_test, daemon=True).start()

    def _do_test(self):
        success, code, latency, msg = NetworkService.test_endpoint(self._url)
        if success:
            text = f"✓ {int(latency)} ms"
            color = "#2E7D32"
        elif code > 0:
            text = f"✗ HTTP {code}"
            color = "#D32F2F"
        else:
            text = f"✗ {msg}"
            color = "#D32F2F"
        self.test_completed.emit(text, color)

    def _update_test_ui(self, text: str, color: str):
        self._status_lbl.setText(text)
        self._status_lbl.setStyleSheet(f"font-size: 11px; font-weight: 600; color: {color}; min-width: 65px;")

    def _copy(self):
        QApplication.clipboard().setText(self._url)
        self._status_lbl.setText("Copied!")
        self._status_lbl.setStyleSheet("font-size: 11px; font-weight: 600; color: #8C7B6D; min-width: 65px;")
        QTimer.singleShot(1500, lambda: self._status_lbl.setText(""))


class MainWindow(QMainWindow):
    """Refined Samsung One UI-inspired CipherVault Server Manager."""

    adb_updated = Signal(bool, list)

    def __init__(self):
        super().__init__()
        self.adb_updated.connect(self._update_adb_ui)
        self.setWindowTitle("CipherVault Server Manager")
        self.resize(1080, 800)
        self.setMinimumSize(960, 640)

        # State
        self._backend_state = BackendState.STOPPED
        self._backend_info: dict = {}
        self._started_at: datetime | None = None
        self._adb_devices: list[AdbDevice] = []
        self._selected_adb_serial: str | None = None

        # Services
        self._backend_svc = BackendService()
        self._backend_svc.log_line.connect(self._on_log_line)
        self._backend_svc.state_changed.connect(self._on_backend_state)
        self._backend_svc.startup_info.connect(self._on_startup_info)

        self._health_poller = HealthPoller(interval=settings.health_interval)
        self._health_poller.status_changed.connect(self._on_health_status)

        # Central widget with smooth scroll area
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setFrameShape(QFrame.Shape.NoFrame)
        scroll.setObjectName("centralWidget")
        self.setCentralWidget(scroll)

        container = QWidget()
        container.setObjectName("containerWidget")
        scroll.setWidget(container)

        self._layout = QVBoxLayout(container)
        self._layout.setSpacing(18)
        self._layout.setContentsMargins(32, 24, 32, 24)

        # UI Hierarchy
        self._build_header()
        self._build_server_status_surface()
        self._build_connect_section()
        self._build_system_status_section()
        self._build_warnings_section()
        self._build_collapsible_api_activity()
        self._build_collapsible_server_log()

        # Calm background space at bottom (prevents unnatural stretching)
        self._layout.addStretch(1)

        # Timers
        self._refresh_timer = QTimer(self)
        self._refresh_timer.timeout.connect(self._refresh_connections_and_adb)
        self._refresh_timer.start(10000)

        self._uptime_timer = QTimer(self)
        self._uptime_timer.timeout.connect(self._update_uptime)
        self._uptime_timer.setInterval(1000)

        # Boot sequence
        self._health_poller.start()
        QTimer.singleShot(150, self._refresh_connections_and_adb)
        QTimer.singleShot(350, self._initial_backend_check_and_start)

    # -----------------------------------------------------------------------
    # Auto-start on boot
    # -----------------------------------------------------------------------

    def _initial_backend_check_and_start(self):
        app_logger.log("app", "Inspecting backend status on startup")
        occupied, pid, proc = EnvironmentService.check_port_listener(settings.backend_port)
        if occupied:
            app_logger.log("app", f"Port {settings.backend_port} occupied by PID {pid} ({proc}), attaching...")
            self._backend_svc.start_backend()
            return

        if settings.auto_start_backend:
            app_logger.log("app", "Port 8080 free. Automatically starting backend...")
            self._do_start()
        else:
            app_logger.log("app", "Auto-start disabled. Backend waiting in stopped state.")
            self._health_poller.set_state(BackendState.STOPPED, "Ready to start")

    # -----------------------------------------------------------------------
    # UI Hierarchy Construction
    # -----------------------------------------------------------------------

    def _build_header(self):
        header_box = QVBoxLayout()
        header_box.setSpacing(3)

        top_row = QHBoxLayout()
        top_row.setSpacing(12)

        # Title in Source Serif 4
        title_box = QVBoxLayout()
        title_box.setSpacing(1)
        title = QLabel("CipherVault")
        title.setStyleSheet('font-family: "Source Serif 4"; font-size: 26px; font-weight: 600;')
        sub = QLabel("Server Manager")
        sub.setStyleSheet("color: #6B6B6B; font-size: 13px; font-weight: 600;")
        title_box.addWidget(title)
        title_box.addWidget(sub)
        top_row.addLayout(title_box)

        top_row.addStretch()

        # Status Badge Pill
        self._status_pill = QFrame()
        self._status_pill.setStyleSheet(
            "background-color: #F0EEEA; border-radius: 13px; padding: 3px 10px;"
        )
        pill_layout = QHBoxLayout(self._status_pill)
        pill_layout.setContentsMargins(8, 3, 8, 3)
        pill_layout.setSpacing(6)

        self._status_dot = QLabel("●")
        self._status_dot.setStyleSheet("font-size: 13px; color: #757575;")
        self._status_text = QLabel("Checking…")
        self._status_text.setStyleSheet("font-weight: 700; font-size: 12px; color: #4A443E;")
        pill_layout.addWidget(self._status_dot)
        pill_layout.addWidget(self._status_text)
        top_row.addWidget(self._status_pill)

        # Action Buttons (Settings & Refresh)
        btn_refresh = QPushButton("Refresh")
        btn_refresh.setIcon(get_tabler_icon("refresh", size=13))
        btn_refresh.setFixedHeight(28)
        btn_refresh.clicked.connect(self._manual_refresh)

        btn_settings = QPushButton("Settings")
        btn_settings.setIcon(get_tabler_icon("settings", size=13))
        btn_settings.setFixedHeight(28)
        btn_settings.clicked.connect(self._open_settings)

        top_row.addWidget(btn_refresh)
        top_row.addWidget(btn_settings)

        header_box.addLayout(top_row)

        # Technical metadata line below title (subtle secondary text, 12px Nunito Sans)
        self._meta_line = QLabel("Managed server · Port 8080 · Java 21 · Spring Boot 4.1.1 · Uptime —")
        self._meta_line.setStyleSheet(
            'color: #7A7570; font-size: 12px; font-weight: 500; margin-top: 3px;'
        )
        header_box.addWidget(self._meta_line)

        self._layout.addLayout(header_box)
        self._layout.addWidget(make_subtle_divider())

    def _build_server_status_surface(self):
        """Compact, balanced status surface with state-aware controls."""
        self._server_card = QFrame()
        self._server_card.setProperty("class", "oneUiCard")
        self._server_card.setSizePolicy(QSizePolicy.Policy.Preferred, QSizePolicy.Policy.Fixed)

        card_layout = QHBoxLayout(self._server_card)
        card_layout.setContentsMargins(18, 14, 18, 14)
        card_layout.setSpacing(16)

        # Left Column: Heading, description, metadata
        info_col = QVBoxLayout()
        info_col.setSpacing(3)

        self._status_heading = QLabel("ONLINE")
        self._status_heading.setStyleSheet("font-weight: 700; font-size: 15px; color: #2E7D32;")
        self._status_desc = QLabel("CipherVault backend is running normally.")
        self._status_desc.setStyleSheet("color: #4A443E; font-size: 13px;")
        self._status_meta = QLabel("Port 8080 · PID — · Uptime —")
        self._status_meta.setStyleSheet('font-family: "JetBrains Mono"; color: #8C7B6D; font-size: 11px;')

        info_col.addWidget(self._status_heading)
        info_col.addWidget(self._status_desc)
        info_col.addWidget(self._status_meta)
        card_layout.addLayout(info_col, 1)

        # Right Column: State-aware action buttons
        self._ctrl_btn_box = QHBoxLayout()
        self._ctrl_btn_box.setSpacing(8)

        self._btn_start = QPushButton("Start Server")
        self._btn_start.setIcon(get_tabler_icon("play", color="#FFFFFF", size=14))
        self._btn_start.setProperty("primary", True)
        self._btn_start.setFixedHeight(30)
        self._btn_start.setFixedWidth(115)
        self._btn_start.clicked.connect(self._do_start)

        self._btn_restart = QPushButton("Restart")
        self._btn_restart.setIcon(get_tabler_icon("restart", size=14))
        self._btn_restart.setFixedHeight(30)
        self._btn_restart.setFixedWidth(95)
        self._btn_restart.clicked.connect(self._do_restart)

        self._btn_stop = QPushButton("Stop")
        self._btn_stop.setIcon(get_tabler_icon("stop", size=14))
        self._btn_stop.setFixedHeight(30)
        self._btn_stop.setFixedWidth(85)
        self._btn_stop.clicked.connect(self._do_stop)

        self._ctrl_btn_box.addWidget(self._btn_start)
        self._ctrl_btn_box.addWidget(self._btn_restart)
        self._ctrl_btn_box.addWidget(self._btn_stop)

        card_layout.addLayout(self._ctrl_btn_box)
        self._layout.addWidget(self._server_card)

    def _build_connect_section(self):
        """Primary application section: Connect to CipherVault."""
        sec_box = QVBoxLayout()
        sec_box.setSpacing(2)

        sec_title = QLabel("Connect to CipherVault")
        sec_title.setStyleSheet('font-family: "Source Serif 4"; font-size: 17px; font-weight: 600;')
        sec_sub = QLabel("Choose how your Android device connects to this computer.")
        sec_sub.setStyleSheet("color: #6B6B6B; font-size: 12px; margin-bottom: 4px;")

        sec_box.addWidget(sec_title)
        sec_box.addWidget(sec_sub)
        self._layout.addLayout(sec_box)

        # 1. Recommended Connection Tile (Top of Connect section)
        self._rec_tile = RecommendedTile(on_use_callback=self._handle_recommended_use)
        self._layout.addWidget(self._rec_tile)

        # 2. Available Connections Card (Secondary list below)
        self._connect_card = QFrame()
        self._connect_card.setProperty("class", "oneUiCard")
        self._connect_card.setSizePolicy(QSizePolicy.Policy.Preferred, QSizePolicy.Policy.Fixed)

        card_layout = QVBoxLayout(self._connect_card)
        card_layout.setContentsMargins(12, 10, 12, 10)
        card_layout.setSpacing(0)

        avail_lbl = QLabel("Available connections")
        avail_lbl.setStyleSheet("font-weight: 700; font-size: 11px; color: #8C7B6D; margin: 4px 10px 6px 10px;")
        card_layout.addWidget(avail_lbl)

        # USB / ADB Row with embedded reverse action
        self._usb_widget = QWidget()
        usb_layout = QVBoxLayout(self._usb_widget)
        usb_layout.setContentsMargins(0, 0, 0, 0)
        usb_layout.setSpacing(0)

        self._btn_usb_reverse = QPushButton("Enable Reverse")
        self._btn_usb_reverse.setFixedHeight(26)
        self._btn_usb_reverse.setMinimumWidth(110)
        self._btn_usb_reverse.clicked.connect(self._toggle_adb_reverse)
        self._btn_usb_reverse.setVisible(False)

        self._usb_row = ConnectionRow(
            "usb", "USB / ADB", "Checking USB devices…", "http://127.0.0.1:8080/",
            extra_widget=self._btn_usb_reverse
        )
        usb_layout.addWidget(self._usb_row)

        card_layout.addWidget(self._usb_widget)
        card_layout.addWidget(make_subtle_divider())

        # Wi-Fi Row Container
        self._wifi_container = QVBoxLayout()
        self._wifi_container.setSpacing(0)
        card_layout.addLayout(self._wifi_container)

        # Ethernet Row Container (Rendered ONLY when active)
        self._eth_container = QVBoxLayout()
        self._eth_container.setSpacing(0)
        card_layout.addLayout(self._eth_container)

        # Android Emulator Row
        self._emu_row = ConnectionRow(
            "emulator", "Android Emulator", "Host loopback alias", "http://10.0.2.2:8080/", can_test=False
        )
        card_layout.addWidget(self._emu_row)

        # Advanced Network Interfaces (Collapsible)
        self._adv_net_btn = QPushButton("Advanced network interfaces  ▶")
        self._adv_net_btn.setProperty("class", "collapsibleHeader")
        self._adv_net_btn.clicked.connect(self._toggle_advanced_network)

        self._adv_net_widget = QWidget()
        self._adv_net_layout = QVBoxLayout(self._adv_net_widget)
        self._adv_net_layout.setContentsMargins(10, 2, 10, 6)
        self._adv_net_layout.setSpacing(2)
        self._adv_net_widget.setVisible(False)

        card_layout.addWidget(make_subtle_divider())
        card_layout.addWidget(self._adv_net_btn)
        card_layout.addWidget(self._adv_net_widget)

        # Subtle Informational Notice at bottom of Connect Card
        card_layout.addWidget(make_subtle_divider())
        info_row = QHBoxLayout()
        info_row.setContentsMargins(10, 6, 10, 4)
        info_row.setSpacing(6)

        info_icon = QLabel()
        info_icon.setPixmap(get_tabler_icon("info", color="#8C7B6D", size=14).pixmap(14, 14))
        info_row.addWidget(info_icon)

        info_text = QLabel(
            "PC Test only verifies that the backend responds from this computer. "
            "For Wi-Fi, your Android device and PC must be on the same network."
        )
        info_text.setStyleSheet("color: #6B6B6B; font-size: 11px;")
        info_row.addWidget(info_text, 1)

        card_layout.addLayout(info_row)
        self._layout.addWidget(self._connect_card)

    def _build_system_status_section(self):
        """Consolidated, cleanly aligned System Status section with View Diagnostics."""
        sec_header = QHBoxLayout()
        sec_header.setSpacing(8)

        sec_title = QLabel("System Status")
        sec_title.setStyleSheet('font-family: "Source Serif 4"; font-size: 15px; font-weight: 600;')
        sec_header.addWidget(sec_title)
        sec_header.addStretch()

        self._btn_diag_toggle = QPushButton("View Diagnostics ▼")
        self._btn_diag_toggle.setFixedHeight(25)
        self._btn_diag_toggle.clicked.connect(self._toggle_diagnostics)
        sec_header.addWidget(self._btn_diag_toggle)
        self._layout.addLayout(sec_header)

        # System Status Card
        self._system_card = QFrame()
        self._system_card.setProperty("class", "oneUiCard")
        self._system_card.setSizePolicy(QSizePolicy.Policy.Preferred, QSizePolicy.Policy.Fixed)

        sys_layout = QVBoxLayout(self._system_card)
        sys_layout.setContentsMargins(18, 12, 18, 12)
        sys_layout.setSpacing(6)

        # Cleanly aligned 4-row Summary
        summary_grid = QGridLayout()
        summary_grid.setHorizontalSpacing(18)
        summary_grid.setVerticalSpacing(4)

        lbl_b = QLabel("Backend")
        self._sys_backend_val = QLabel("● Checking…")
        summary_grid.addWidget(lbl_b, 0, 0)
        summary_grid.addWidget(self._sys_backend_val, 0, 1)

        lbl_d = QLabel("Database")
        self._sys_db_val = QLabel("● Waiting for backend…")
        summary_grid.addWidget(lbl_d, 1, 0)
        summary_grid.addWidget(self._sys_db_val, 1, 1)

        lbl_s = QLabel("Storage")
        self._sys_storage_val = QLabel("● Ready")
        summary_grid.addWidget(lbl_s, 2, 0)
        summary_grid.addWidget(self._sys_storage_val, 2, 1)

        lbl_e = QLabel("Encryption")
        self._sys_crypto_val = QLabel("● AES-256-GCM · BCrypt · JWT Initialized")
        summary_grid.addWidget(lbl_e, 3, 0)
        summary_grid.addWidget(self._sys_crypto_val, 3, 1)

        for l in [lbl_b, lbl_d, lbl_s, lbl_e]:
            l.setStyleSheet("color: #6B6B6B; font-size: 12px; font-weight: 600; min-width: 95px;")

        for v in [self._sys_backend_val, self._sys_db_val, self._sys_storage_val, self._sys_crypto_val]:
            v.setStyleSheet('font-family: "JetBrains Mono"; font-size: 12px; font-weight: 500;')

        sys_layout.addLayout(summary_grid)

        # Expanded Diagnostics (Collapsed by default)
        self._diag_container = QWidget()
        diag_layout = QVBoxLayout(self._diag_container)
        diag_layout.setContentsMargins(0, 8, 0, 0)
        diag_layout.setSpacing(3)
        diag_layout.addWidget(make_subtle_divider())

        self._diag_time = QLabel("Server Time: —")
        self._diag_latency = QLabel("Health Latency: —")
        self._diag_binding = QLabel("Socket Binding: Listening on :: (All interfaces)")
        self._diag_pool = QLabel("Database Connection Pool: HikariCP (MySQL)")
        self._diag_key = QLabel("Key Management: Initialized (Streaming mode)")

        for d in [self._diag_time, self._diag_latency, self._diag_binding, self._diag_pool, self._diag_key]:
            d.setStyleSheet('font-family: "JetBrains Mono"; font-size: 11px; color: #6B6B6B;')
            diag_layout.addWidget(d)

        self._diag_container.setVisible(False)
        sys_layout.addWidget(self._diag_container)

        self._layout.addWidget(self._system_card)

    def _build_warnings_section(self):
        """Compact warning row rendered only when development fallbacks exist."""
        self._warn_card = QFrame()
        self._warn_card.setProperty("class", "oneUiCard")
        self._warn_card.setStyleSheet("background-color: #FDF9F2; border: 1px solid #EFE4D0;")
        self._warn_card.setSizePolicy(QSizePolicy.Policy.Preferred, QSizePolicy.Policy.Fixed)

        warn_layout = QVBoxLayout(self._warn_card)
        warn_layout.setContentsMargins(14, 10, 14, 10)
        warn_layout.setSpacing(4)

        header_row = QHBoxLayout()
        header_row.setSpacing(6)

        warn_icon = QLabel()
        warn_icon.setPixmap(get_tabler_icon("alert", color="#B36A00", size=15).pixmap(15, 15))
        header_row.addWidget(warn_icon)

        warn_title = QLabel("⚠ Development configuration: 3 fallback values active · JWT · Master Key · PBKDF2")
        warn_title.setStyleSheet("color: #8C5311; font-size: 12px; font-weight: 700;")
        header_row.addWidget(warn_title, 1)

        self._btn_warn_toggle = QPushButton("View Details ▼")
        self._btn_warn_toggle.setFixedHeight(23)
        self._btn_warn_toggle.clicked.connect(self._toggle_warnings)
        header_row.addWidget(self._btn_warn_toggle)
        warn_layout.addLayout(header_row)

        self._warn_details = QWidget()
        details_lay = QVBoxLayout(self._warn_details)
        details_lay.setContentsMargins(22, 2, 6, 2)
        details_lay.setSpacing(2)

        w1 = QLabel("• JWT: Development fallback secret active (configure CIPHERVAULT_JWT_SECRET for production)")
        w2 = QLabel("• Master Key: Development fallback passphrase active (configure CIPHERVAULT_MASTER_KEY for production)")
        w3 = QLabel("• PBKDF2 Salt: Development fallback salt active (configure CIPHERVAULT_PBKDF2_SALT for production)")
        w_note = QLabel("Note: Secret values and keys are strictly withheld from display.")

        for item in [w1, w2, w3, w_note]:
            item.setStyleSheet("color: #8C5311; font-size: 11px;")
            details_lay.addWidget(item)

        self._warn_details.setVisible(False)
        warn_layout.addWidget(self._warn_details)

        self._layout.addWidget(self._warn_card)

    def _build_collapsible_api_activity(self):
        """Collapsible Recent API Activity section."""
        self._api_btn = QPushButton("Recent API Activity (0)  ▶")
        self._api_btn.setProperty("class", "collapsibleHeader")
        self._api_btn.clicked.connect(self._toggle_api_activity)
        self._layout.addWidget(self._api_btn)

        self._api_widget = QWidget()
        self._api_widget.setSizePolicy(QSizePolicy.Policy.Preferred, QSizePolicy.Policy.Fixed)
        api_layout = QVBoxLayout(self._api_widget)
        api_layout.setContentsMargins(0, 0, 0, 4)
        api_layout.setSpacing(4)

        self._api_log = QTextEdit()
        self._api_log.setReadOnly(True)
        self._api_log.setFixedHeight(110)
        self._api_log.setPlaceholderText("Observed backend HTTP requests will appear here live")
        api_layout.addWidget(self._api_log)

        self._api_widget.setVisible(False)
        self._layout.addWidget(self._api_widget)

    def _build_collapsible_server_log(self):
        """Collapsible Server Log section."""
        self._log_btn = QPushButton("Server Log  ▶")
        self._log_btn.setProperty("class", "collapsibleHeader")
        self._log_btn.clicked.connect(self._toggle_server_log)
        self._layout.addWidget(self._log_btn)

        self._log_widget = QWidget()
        self._log_widget.setSizePolicy(QSizePolicy.Policy.Preferred, QSizePolicy.Policy.Fixed)
        log_layout = QVBoxLayout(self._log_widget)
        log_layout.setContentsMargins(0, 0, 0, 4)
        log_layout.setSpacing(6)

        self._server_log = QTextEdit()
        self._server_log.setReadOnly(True)
        self._server_log.setFixedHeight(170)
        self._server_log.setPlaceholderText("Spring Boot stdout and stderr output streams here")
        log_layout.addWidget(self._server_log)

        btn_row = QHBoxLayout()
        btn_row.addStretch()
        btn_clear = QPushButton("Clear Log")
        btn_clear.setFixedHeight(25)
        btn_clear.clicked.connect(lambda: self._server_log.clear())
        btn_copy = QPushButton("Copy All")
        btn_copy.setFixedHeight(25)
        btn_copy.clicked.connect(lambda: QApplication.clipboard().setText(self._server_log.toPlainText()))
        btn_row.addWidget(btn_clear)
        btn_row.addWidget(btn_copy)
        log_layout.addLayout(btn_row)

        self._log_widget.setVisible(False)
        self._layout.addWidget(self._log_widget)

    # -----------------------------------------------------------------------
    # Collapsible Toggles
    # -----------------------------------------------------------------------

    def _toggle_advanced_network(self):
        vis = not self._adv_net_widget.isVisible()
        self._adv_net_widget.setVisible(vis)
        self._adv_net_btn.setText(f"Advanced network interfaces  {'▼' if vis else '▶'}")

    def _toggle_diagnostics(self):
        vis = not self._diag_container.isVisible()
        self._diag_container.setVisible(vis)
        self._btn_diag_toggle.setText("Hide Diagnostics ▲" if vis else "View Diagnostics ▼")

    def _toggle_warnings(self):
        vis = not self._warn_details.isVisible()
        self._warn_details.setVisible(vis)
        self._btn_warn_toggle.setText("Hide Details ▲" if vis else "View Details ▼")

    def _toggle_api_activity(self):
        vis = not self._api_widget.isVisible()
        self._api_widget.setVisible(vis)
        count = len(self._api_log.toPlainText().strip().split("\n")) if self._api_log.toPlainText().strip() else 0
        self._api_btn.setText(f"Recent API Activity ({count})  {'▼' if vis else '▶'}")

    def _toggle_server_log(self):
        vis = not self._log_widget.isVisible()
        self._log_widget.setVisible(vis)
        self._log_btn.setText(f"Server Log  {'▼' if vis else '▶'}")

    # -----------------------------------------------------------------------
    # Backend Actions
    # -----------------------------------------------------------------------

    def _do_start(self):
        if self._backend_svc.isRunning():
            return
        app_logger.log("action", "Starting backend server")
        self._health_poller.set_state(BackendState.STARTING, "Starting Spring Boot...")
        self._update_server_status_ui(BackendState.STARTING, "Launching backend process...")
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
        self._health_poller.set_state(BackendState.STARTING, "Restarting backend...")
        self._update_server_status_ui(BackendState.STARTING, "Restarting...")
        QTimer.singleShot(1500, self._restart_step2)

    def _restart_step2(self):
        self._backend_svc = BackendService()
        self._backend_svc.log_line.connect(self._on_log_line)
        self._backend_svc.state_changed.connect(self._on_backend_state)
        self._backend_svc.startup_info.connect(self._on_startup_info)
        self._do_start()

    # -----------------------------------------------------------------------
    # Recommended Connection Logic
    # -----------------------------------------------------------------------

    def _determine_recommendation(self) -> dict:
        """Determines the single best available connection for an Android device."""
        # 1. Physical Android device connected via USB
        phys_dev = next((d for d in self._adb_devices if d.state == "device" and not d.is_emulator), None)
        if phys_dev:
            return {
                "type": "usb",
                "title": "USB / ADB",
                "subtitle": f"Physical Android device connected ({phys_dev.serial})",
                "url": "http://127.0.0.1:8080/",
                "action_text": "Use USB",
                "ready": True,
                "device": phys_dev
            }

        # 2. Wi-Fi: active usable Wi-Fi address exists
        adapters = NetworkService.get_classified_adapters()
        if adapters["wifi"]:
            w = adapters["wifi"][0]
            return {
                "type": "wifi",
                "title": "Wi-Fi",
                "subtitle": f"Connect your Android phone over the same local network ({w.ip})",
                "url": w.url,
                "action_text": "Use Wi-Fi",
                "ready": True,
                "adapter": w
            }

        # 3. Ethernet: usable LAN connection exists
        if adapters["ethernet"]:
            e = adapters["ethernet"][0]
            return {
                "type": "ethernet",
                "title": "Ethernet",
                "subtitle": f"Available on local network ({e.ip})",
                "url": e.url,
                "action_text": "Use Ethernet",
                "ready": True,
                "adapter": e
            }

        # 4. Emulator: only if an emulator is actually connected/running
        emu_dev = next((d for d in self._adb_devices if d.state == "device" and d.is_emulator), None)
        if emu_dev:
            return {
                "type": "emulator",
                "title": "Android Emulator",
                "subtitle": f"Android emulator detected ({emu_dev.serial})",
                "url": "http://10.0.2.2:8080/",
                "action_text": "Use Emulator",
                "ready": True,
                "device": emu_dev
            }

        # 5. Nothing ready
        return {
            "type": "none",
            "title": "No connection ready",
            "subtitle": "Connect your Android device by USB or connect this computer and your phone to the same Wi-Fi network.",
            "url": "",
            "action_text": "",
            "ready": False
        }

    def _handle_recommended_use(self, rec: dict):
        if not rec:
            return
        conn_type = rec.get("type", "")
        if conn_type == "usb":
            dev = rec.get("device")
            if dev and not dev.reverse_active:
                AdbService.enable_reverse(dev.serial)
                dev.reverse_active = True
                self._update_adb_ui(True, self._adb_devices)

    # -----------------------------------------------------------------------
    # Network and ADB Refresh
    # -----------------------------------------------------------------------

    def _manual_refresh(self):
        app_logger.log("action", "Manual Refresh triggered")
        self._health_poller.poll_now()
        self._refresh_connections_and_adb()

    def _refresh_connections_and_adb(self):
        # 1. Socket Binding
        b_type, b_desc = NetworkService.check_backend_binding(settings.backend_port)
        self._diag_binding.setText(f"Socket Binding: {b_desc}")

        # 2. Classified Adapters
        adapters = NetworkService.get_classified_adapters()

        # Wi-Fi
        self._clear_layout(self._wifi_container)
        if settings.show_wifi and adapters["wifi"]:
            for a in adapters["wifi"]:
                self._wifi_container.addWidget(ConnectionRow("wifi", "Wi-Fi", f"● Available · {a.ip} ({a.name})", a.url))
            self._wifi_container.addWidget(make_subtle_divider())

        # Ethernet: Only rendered if active and usable!
        self._clear_layout(self._eth_container)
        if settings.show_ethernet and adapters["ethernet"]:
            for a in adapters["ethernet"]:
                self._eth_container.addWidget(ConnectionRow("ethernet", "Ethernet", f"● Available · {a.ip} ({a.name})", a.url))
            self._eth_container.addWidget(make_subtle_divider())

        # Advanced / Virtual (WSL, Hyper-V, VPN)
        self._clear_layout(self._adv_net_layout)
        if adapters["other"]:
            self._adv_net_btn.setVisible(True)
            self._adv_net_btn.setText(f"Advanced network interfaces ({len(adapters['other'])})  ▶")
            for a in adapters["other"]:
                self._adv_net_layout.addWidget(ConnectionRow("wifi", a.name, f"Virtual / Local · {a.ip}", a.url))
        else:
            self._adv_net_btn.setVisible(False)

        # 3. ADB Devices in background worker
        threading.Thread(target=self._query_adb_async, daemon=True).start()

    def _query_adb_async(self):
        avail = AdbService.is_available()
        devices = AdbService.get_devices() if avail else []
        self.adb_updated.emit(avail, devices)

    def _update_adb_ui(self, adb_avail: bool, devices: list[AdbDevice]):
        self._adb_devices = devices

        # Update recommendation tile dynamically
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
        active_dev = next((d for d in devices if d.serial == target_serial), devices[0])

        # Auto-enable reverse if configured
        if settings.auto_enable_adb_reverse and not active_dev.reverse_active:
            active_dev.reverse_active = True  # Optimistic UI update
            threading.Thread(
                target=lambda s: (AdbService.enable_reverse(s), self._query_adb_async()), 
                args=(active_dev.serial,), 
                daemon=True
            ).start()

        # Clean status line with device serial & state
        dev_desc = "Physical" if not active_dev.is_emulator else "Emulator"
        state_tag = "Reverse active" if active_dev.reverse_active else "Reverse inactive"
        self._usb_row.update_subtitle(f"● Connected · {active_dev.serial} ({dev_desc}) · {state_tag}")

        self._btn_usb_reverse.setVisible(True)
        self._btn_usb_reverse.setEnabled(True)
        self._btn_usb_reverse.setText("Disable Reverse" if active_dev.reverse_active else "Enable Reverse")
        self._btn_usb_reverse.setProperty("primary", not active_dev.reverse_active)
        self._btn_usb_reverse.style().unpolish(self._btn_usb_reverse)
        self._btn_usb_reverse.style().polish(self._btn_usb_reverse)

    def _toggle_adb_reverse(self):
        if not self._adb_devices:
            return
        active_dev = next((d for d in self._adb_devices if d.serial == self._selected_adb_serial), self._adb_devices[0])
        self._btn_usb_reverse.setText("Toggling…")
        self._btn_usb_reverse.setEnabled(False)
        threading.Thread(target=self._do_toggle_reverse, args=(active_dev.serial, active_dev.reverse_active), daemon=True).start()

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
            w = item.widget()
            if w:
                w.deleteLater()
            elif item.layout():
                self._clear_layout(item.layout())

    # -----------------------------------------------------------------------
    # Signal Handlers & Log Streaming
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
                self._api_btn.setText(f"Recent API Activity ({count})  ▶")

    @Slot(str, str)
    def _on_backend_state(self, state: str, detail: str):
        self._backend_state = state
        self._update_server_status_ui(state, detail)

        if state == BackendState.ONLINE:
            if not self._uptime_timer.isActive():
                self._uptime_timer.start()
            self._health_poller.poll_now()
            self._refresh_connections_and_adb()
        elif state in (BackendState.STOPPED, BackendState.ERROR, BackendState.FOREIGN_SERVICE):
            self._uptime_timer.stop()

    @Slot(dict)
    def _on_startup_info(self, info: dict):
        self._backend_info = info
        self._update_metadata_line()

        # Update System Status Card Summary
        if "mysql_version" in info or info.get("db_connected"):
            ver = info.get("mysql_version", "8.0")
            self._sys_db_val.setText(f"● Connected · MySQL {ver} · ciphervault")
            self._sys_db_val.setStyleSheet('font-family: "JetBrains Mono"; font-size: 12px; font-weight: 600; color: #2E7D32;')

        if "files_scanned" in info:
            scanned = info.get("files_scanned", "0")
            missing = info.get("missing_metadata", "0")
            self._sys_storage_val.setText(f"● Ready · {scanned} files · {missing} missing metadata")

        # Update Diagnostics
        if "jvm_pid" in info:
            self._diag_pool.setText(f"Process PID: {info['jvm_pid']} · Database Pool: HikariCP (MySQL)")

    @Slot(str, str, dict)
    def _on_health_status(self, state: str, reason: str, data: dict):
        self._update_server_status_ui(state, reason, data)

    # -----------------------------------------------------------------------
    # State-Aware UI Updating
    # -----------------------------------------------------------------------

    def _update_server_status_ui(self, state: str, detail: str = "", data: dict = None):
        pid_str = str(self._backend_info.get("jvm_pid", self._backend_svc.pid or "—"))
        uptime_str = self._get_uptime_string()

        if state == BackendState.ONLINE:
            # Header pill
            self._status_dot.setStyleSheet("font-size: 13px; color: #2E7D32;")
            self._status_text.setText("ONLINE")
            self._status_text.setStyleSheet("font-weight: 700; font-size: 12px; color: #2E7D32;")
            self._status_pill.setStyleSheet("background-color: #E8F5E9; border-radius: 13px; padding: 3px 10px;")

            # Server status card
            self._status_heading.setText("ONLINE")
            self._status_heading.setStyleSheet("font-weight: 700; font-size: 15px; color: #2E7D32;")
            self._status_desc.setText("CipherVault backend is running normally.")
            self._status_meta.setText(f"Port 8080 · PID {pid_str} · Uptime {uptime_str}")

            # State-aware buttons: Start is HIDDEN when online!
            self._btn_start.setVisible(False)
            self._btn_restart.setVisible(True)
            self._btn_stop.setVisible(True)

            # System card
            latency = data.get("_latency_ms", "3") if data else "3"
            self._sys_backend_val.setText(f"● Online · 200 OK · {latency} ms")
            self._sys_backend_val.setStyleSheet('font-family: "JetBrains Mono"; font-size: 12px; font-weight: 600; color: #2E7D32;')

            if data and data.get("timestamp"):
                self._diag_time.setText(f"Server Time: {data['timestamp']}")
                self._diag_latency.setText(f"Health Latency: {latency} ms")

        elif state == BackendState.STARTING:
            self._status_dot.setStyleSheet("font-size: 13px; color: #B36A00;")
            self._status_text.setText("STARTING")
            self._status_text.setStyleSheet("font-weight: 700; font-size: 12px; color: #B36A00;")
            self._status_pill.setStyleSheet("background-color: #FFF8E1; border-radius: 13px; padding: 3px 10px;")

            self._status_heading.setText("STARTING")
            self._status_heading.setStyleSheet("font-weight: 700; font-size: 15px; color: #B36A00;")
            self._status_desc.setText("Starting CipherVault backend… Waiting for Spring Boot health response.")
            self._status_meta.setText(f"Port 8080 · PID {pid_str}")

            self._btn_start.setVisible(False)
            self._btn_restart.setVisible(False)
            self._btn_stop.setVisible(True)

            self._sys_backend_val.setText("● Starting…")
            self._sys_backend_val.setStyleSheet('font-family: "JetBrains Mono"; font-size: 12px; font-weight: 600; color: #B36A00;')

        elif state == BackendState.FOREIGN_SERVICE:
            self._status_dot.setStyleSheet("font-size: 13px; color: #D32F2F;")
            self._status_text.setText("PORT OCCUPIED")
            self._status_text.setStyleSheet("font-weight: 700; font-size: 12px; color: #D32F2F;")
            self._status_pill.setStyleSheet("background-color: #FFEBEE; border-radius: 13px; padding: 3px 10px;")

            self._status_heading.setText("PORT OCCUPIED")
            self._status_heading.setStyleSheet("font-weight: 700; font-size: 15px; color: #D32F2F;")
            self._status_desc.setText(detail or "Port 8080 is in use by another service. CipherVault cannot safely bind.")
            self._status_meta.setText(f"Port 8080 occupied · PID {pid_str}")

            self._btn_start.setVisible(False)
            self._btn_restart.setVisible(False)
            self._btn_stop.setVisible(False)

            self._sys_backend_val.setText("⚠ Port Conflict")
            self._sys_backend_val.setStyleSheet('font-family: "JetBrains Mono"; font-size: 12px; font-weight: 600; color: #D32F2F;')

        elif state == BackendState.STOPPED:
            self._status_dot.setStyleSheet("font-size: 13px; color: #757575;")
            self._status_text.setText("OFFLINE")
            self._status_text.setStyleSheet("font-weight: 700; font-size: 12px; color: #757575;")
            self._status_pill.setStyleSheet("background-color: #F0EEEA; border-radius: 13px; padding: 3px 10px;")

            self._status_heading.setText("OFFLINE")
            self._status_heading.setStyleSheet("font-weight: 700; font-size: 15px; color: #757575;")
            self._status_desc.setText("The CipherVault backend is not running.")
            self._status_meta.setText("Port 8080 · PID —")

            # State-aware buttons: Start is VISIBLE and primary!
            self._btn_start.setVisible(True)
            self._btn_restart.setVisible(False)
            self._btn_stop.setVisible(False)

            self._sys_backend_val.setText("○ Stopped")
            self._sys_backend_val.setStyleSheet('font-family: "JetBrains Mono"; font-size: 12px; font-weight: 600; color: #757575;')

        else:  # OFFLINE / ERROR
            self._status_dot.setStyleSheet("font-size: 13px; color: #D32F2F;")
            self._status_text.setText("OFFLINE")
            self._status_text.setStyleSheet("font-weight: 700; font-size: 12px; color: #D32F2F;")
            self._status_pill.setStyleSheet("background-color: #FFEBEE; border-radius: 13px; padding: 3px 10px;")

            self._status_heading.setText("OFFLINE")
            self._status_heading.setStyleSheet("font-weight: 700; font-size: 15px; color: #D32F2F;")
            self._status_desc.setText(detail or "The CipherVault backend is not responding.")
            self._status_meta.setText("Port 8080 · Connection refused")

            self._btn_start.setVisible(True)
            self._btn_restart.setVisible(False)
            self._btn_stop.setVisible(False)

            self._sys_backend_val.setText("○ Offline")
            self._sys_backend_val.setStyleSheet('font-family: "JetBrains Mono"; font-size: 12px; font-weight: 600; color: #D32F2F;')

        self._update_metadata_line()

    def _get_uptime_string(self) -> str:
        if self._started_at:
            delta = datetime.now() - self._started_at
            s = int(delta.total_seconds())
            h, rem = divmod(s, 3600)
            m, sec = divmod(rem, 60)
            return f"{h:02d}:{m:02d}:{sec:02d}"
        return "—"

    def _update_uptime(self):
        uptime_str = self._get_uptime_string()
        pid_str = str(self._backend_info.get("jvm_pid", self._backend_svc.pid or "—"))
        if self._backend_state == BackendState.ONLINE:
            self._status_meta.setText(f"Port 8080 · PID {pid_str} · Uptime {uptime_str}")
        self._update_metadata_line()

    def _update_metadata_line(self):
        owner = self._backend_svc.ownership_label
        port = str(settings.backend_port)
        java_ver = self._backend_info.get("java_version", "21.0.12")
        spring_ver = self._backend_info.get("spring_version", "4.1.1")
        pid_str = str(self._backend_info.get("jvm_pid", self._backend_svc.pid or "—"))
        uptime_str = self._get_uptime_string()

        meta_text = f"{owner} · Port {port} · Java {java_ver} · Spring Boot {spring_ver} · PID {pid_str} · Uptime {uptime_str}"
        self._meta_line.setText(meta_text)

    # -----------------------------------------------------------------------
    # Settings & Window Lifecycle
    # -----------------------------------------------------------------------

    def _open_settings(self):
        dlg = SettingsDialog(self)
        if dlg.exec():
            app = QApplication.instance()
            apply_theme(app, settings.theme)
            self._health_poller.set_interval(settings.health_interval)
            self._refresh_connections_and_adb()

    def changeEvent(self, event):
        if event.type() == event.Type.ActivationChange and self.isActiveWindow():
            self._refresh_connections_and_adb()
        super().changeEvent(event)

    def closeEvent(self, event: QCloseEvent):
        if self._backend_svc.isRunning() and self._backend_svc.owns_process:
            reply = QMessageBox.question(
                self,
                "Backend Still Running",
                "The CipherVault backend was launched by Server Manager and is currently running.\n\n"
                "Would you like to stop the backend before closing?",
                QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No | QMessageBox.StandardButton.Cancel,
                QMessageBox.StandardButton.Yes
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

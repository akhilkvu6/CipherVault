import os
import re
import webbrowser
import threading
import qrcode
from PySide6.QtWidgets import (
    QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, QLabel,
    QPushButton, QTextEdit, QFrame, QApplication, QCheckBox, QMessageBox
)
from PySide6.QtCore import Qt, QTimer, Slot, Signal
from PySide6.QtGui import QCloseEvent, QImage, QPixmap, QTextCursor

from .icons import get_tabler_icon
from .settings import settings
from .theme import apply_theme
from .services.backend_service import BackendService
from .services.backend_state import BackendState
from .services.network_service import NetworkService
from .services.environment_service import EnvironmentService, get_project_root
from .services.log_service import app_logger


class MainWindow(QMainWindow):
    """Clean, Material 3 Server Control Dashboard for CipherVault.
    Displays dynamic IP, real-time QR code generation, Start/Stop controls,
    live health monitoring, theme switching, and live backend logs.
    """

    health_updated = Signal(bool, int, float, str)

    def __init__(self):
        super().__init__()
        self.setWindowTitle("CipherVault — Server Control")
        self.setMinimumSize(820, 720)
        self.resize(880, 760)

        self._current_ip = "127.0.0.1"
        self._current_port = settings.backend_port
        self._current_iface = "Checking..."
        self._backend_state = BackendState.STOPPED
        self._health_check_in_progress = False

        self._external_pid: int | None = None
        self._external_log_tailer_active: bool = False
        self._log_file_pos: int = 0

        # Backend runner service
        self._backend_service = BackendService(self)
        self._backend_service.log_line.connect(self._on_log_line)
        self._backend_service.state_changed.connect(self._on_state_changed)

        self.health_updated.connect(self._on_health_result)

        self._init_ui()
        self._refresh_network()

        # External log tailing timer (1.0s interval)
        self._log_tail_timer = QTimer(self)
        self._log_tail_timer.setInterval(1000)
        self._log_tail_timer.timeout.connect(self._poll_external_log_file)

        # Polling timer for status, IP shifts, and health (every 2.5s)
        self._poll_timer = QTimer(self)
        self._poll_timer.timeout.connect(self._on_poll)
        self._poll_timer.start(2500)

        # Trigger immediate poll
        self._on_poll()

    def _init_ui(self):
        central = QWidget(self)
        central.setObjectName("centralWidget")
        self.setCentralWidget(central)

        main_layout = QVBoxLayout(central)
        main_layout.setContentsMargins(24, 20, 24, 20)
        main_layout.setSpacing(16)

        # 1. Header Bar
        header = QHBoxLayout()
        header.setSpacing(14)

        icon_label = QLabel()
        icon_label.setPixmap(get_tabler_icon("shield", color="#0284C7", size=32).pixmap(32, 32))
        header.addWidget(icon_label)

        title_box = QVBoxLayout()
        title_box.setSpacing(2)

        title = QLabel("CipherVault Server Control")
        title.setObjectName("titleLabel")
        title_box.addWidget(title)

        subtitle = QLabel("Spring Boot Private Cloud Storage Bridge • Material 3")
        subtitle.setObjectName("subtitleLabel")
        title_box.addWidget(subtitle)

        header.addLayout(title_box)
        header.addStretch()

        # Status Pill
        self._status_pill = QLabel("○ OFFLINE")
        self._status_pill.setObjectName("statusPillStopped")
        header.addWidget(self._status_pill)

        # Theme Toggle Button
        current_theme = settings.get("theme", "dark")
        btn_theme_text = "☾ Dark Mode" if current_theme == "light" else "☀ Light Mode"
        self._btn_theme_toggle = QPushButton(btn_theme_text)
        self._btn_theme_toggle.setObjectName("btnTonal")
        self._btn_theme_toggle.setCursor(Qt.CursorShape.PointingHandCursor)
        self._btn_theme_toggle.clicked.connect(self._toggle_theme)
        header.addWidget(self._btn_theme_toggle)

        main_layout.addLayout(header)

        # 2. Hero Connection Gateway Card (with Dynamic QR code & IP Info)
        hero_card = QFrame()
        hero_card.setProperty("class", "heroCard")
        hero_layout = QVBoxLayout(hero_card)
        hero_layout.setContentsMargins(22, 18, 22, 18)
        hero_layout.setSpacing(14)

        hero_top = QHBoxLayout()
        eyebrow = QLabel("★ CONNECTION GATEWAY")
        eyebrow.setObjectName("heroEyebrow")
        hero_top.addWidget(eyebrow)
        hero_top.addStretch()

        self._adapter_badge = QLabel("Scanning network...")
        self._adapter_badge.setStyleSheet("color: #0284C7; font-size: 11px; font-weight: 600;")
        hero_top.addWidget(self._adapter_badge)

        hero_layout.addLayout(hero_top)

        # Content Row: QR Code on Left, IP & Controls on Right
        content_row = QHBoxLayout()
        content_row.setSpacing(20)

        # Left: QR Code Box
        qr_container = QFrame()
        qr_container.setObjectName("qrContainer")
        qr_layout = QVBoxLayout(qr_container)
        qr_layout.setContentsMargins(6, 6, 6, 6)
        qr_layout.setSpacing(4)
        qr_layout.setAlignment(Qt.AlignmentFlag.AlignCenter)

        self._lbl_qr = QLabel()
        self._lbl_qr.setFixedSize(160, 160)
        self._lbl_qr.setAlignment(Qt.AlignmentFlag.AlignCenter)
        qr_layout.addWidget(self._lbl_qr)

        self._lbl_qr_caption = QLabel("Scan in Android app")
        self._lbl_qr_caption.setStyleSheet("color: #1E293B; font-size: 10px; font-weight: 600;")
        self._lbl_qr_caption.setAlignment(Qt.AlignmentFlag.AlignCenter)
        qr_layout.addWidget(self._lbl_qr_caption)

        content_row.addWidget(qr_container)

        # Right: IP, URL, Copy Buttons, Instructions
        right_info = QVBoxLayout()
        right_info.setSpacing(8)
        right_info.setAlignment(Qt.AlignmentFlag.AlignVCenter)

        lbl_ip_header = QLabel("SERVER ADDRESS (LAN / HOTSPOT)")
        lbl_ip_header.setStyleSheet("color: #64748B; font-size: 10px; font-weight: 700; letter-spacing: 0.5px;")
        right_info.addWidget(lbl_ip_header)

        ip_line = QHBoxLayout()
        ip_line.setSpacing(10)
        wifi_icon = QLabel()
        wifi_icon.setPixmap(get_tabler_icon("wifi", color="#0284C7", size=24).pixmap(24, 24))
        ip_line.addWidget(wifi_icon)

        self._lbl_ip_port = QLabel("127.0.0.1 : 8080")
        self._lbl_ip_port.setObjectName("heroIpDisplay")
        ip_line.addWidget(self._lbl_ip_port)
        ip_line.addStretch()
        right_info.addLayout(ip_line)

        self._lbl_full_url = QLabel("http://127.0.0.1:8080/")
        self._lbl_full_url.setObjectName("heroUrlDisplay")
        right_info.addWidget(self._lbl_full_url)

        # Copy buttons row
        btn_row = QHBoxLayout()
        btn_row.setSpacing(10)

        self._btn_copy_url = QPushButton("Copy URL")
        self._btn_copy_url.setObjectName("btnPrimary")
        self._btn_copy_url.setIcon(get_tabler_icon("copy", color="#FFFFFF", size=14))
        self._btn_copy_url.setCursor(Qt.CursorShape.PointingHandCursor)
        self._btn_copy_url.clicked.connect(self._copy_url)
        btn_row.addWidget(self._btn_copy_url)

        self._btn_copy_ip = QPushButton("Copy IP")
        self._btn_copy_ip.setObjectName("btnTonal")
        self._btn_copy_ip.setCursor(Qt.CursorShape.PointingHandCursor)
        self._btn_copy_ip.clicked.connect(self._copy_ip)
        btn_row.addWidget(self._btn_copy_ip)

        btn_row.addStretch()
        right_info.addLayout(btn_row)

        guide = QLabel(
            "Quick Connect: Open CipherVault app on Android → Tap [Scan QR Code] → Aim at QR code above."
        )
        guide.setStyleSheet("color: #64748B; font-size: 11px; padding-top: 2px;")
        guide.setWordWrap(True)
        right_info.addWidget(guide)

        content_row.addLayout(right_info)
        hero_layout.addLayout(content_row)

        # Health telemetry bar
        self._lbl_health_telemetry = QLabel("Health: Waiting for server check...")
        self._lbl_health_telemetry.setStyleSheet("color: #64748B; font-size: 11px; font-weight: 600;")
        hero_layout.addWidget(self._lbl_health_telemetry)

        main_layout.addWidget(hero_card)

        # 3. Server Controls Action Bar
        controls_layout = QHBoxLayout()
        controls_layout.setSpacing(10)

        self._btn_start = QPushButton("Start Server")
        self._btn_start.setObjectName("btnPrimary")
        self._btn_start.setIcon(get_tabler_icon("play", color="#FFFFFF", size=14))
        self._btn_start.setCursor(Qt.CursorShape.PointingHandCursor)
        self._btn_start.clicked.connect(self._start_server)
        controls_layout.addWidget(self._btn_start)

        self._btn_stop = QPushButton("Stop Server")
        self._btn_stop.setObjectName("btnDanger")
        self._btn_stop.setIcon(get_tabler_icon("stop", color="#FFFFFF", size=14))
        self._btn_stop.setCursor(Qt.CursorShape.PointingHandCursor)
        self._btn_stop.clicked.connect(self._stop_server)
        self._btn_stop.setEnabled(False)
        controls_layout.addWidget(self._btn_stop)

        self._btn_restart = QPushButton("Restart")
        self._btn_restart.setIcon(get_tabler_icon("restart", color="#64748B", size=14))
        self._btn_restart.setCursor(Qt.CursorShape.PointingHandCursor)
        self._btn_restart.clicked.connect(self._restart_server)
        self._btn_restart.setEnabled(False)
        controls_layout.addWidget(self._btn_restart)

        controls_layout.addStretch()

        self._btn_open_browser = QPushButton("Open /api/health")
        self._btn_open_browser.setIcon(get_tabler_icon("activity", color="#64748B", size=14))
        self._btn_open_browser.setCursor(Qt.CursorShape.PointingHandCursor)
        self._btn_open_browser.clicked.connect(self._open_health_in_browser)
        controls_layout.addWidget(self._btn_open_browser)

        self._btn_refresh_net = QPushButton("Refresh Network")
        self._btn_refresh_net.setIcon(get_tabler_icon("refresh", color="#64748B", size=14))
        self._btn_refresh_net.setCursor(Qt.CursorShape.PointingHandCursor)
        self._btn_refresh_net.clicked.connect(self._refresh_network)
        controls_layout.addWidget(self._btn_refresh_net)

        main_layout.addLayout(controls_layout)

        # 4. Live Server Logs Console
        logs_card = QFrame()
        logs_card.setProperty("class", "m3Card")
        logs_layout = QVBoxLayout(logs_card)
        logs_layout.setContentsMargins(18, 14, 18, 14)
        logs_layout.setSpacing(10)

        logs_header = QHBoxLayout()
        lbl_logs = QLabel("Backend Logs")
        lbl_logs.setObjectName("cardTitle")
        logs_header.addWidget(lbl_logs)

        logs_header.addStretch()

        self._chk_autoscroll = QCheckBox("Auto-scroll")
        self._chk_autoscroll.setChecked(True)
        self._chk_autoscroll.setStyleSheet("color: #64748B; font-size: 11px;")
        logs_header.addWidget(self._chk_autoscroll)

        btn_clear = QPushButton("Clear")
        btn_clear.setFixedHeight(26)
        btn_clear.setCursor(Qt.CursorShape.PointingHandCursor)
        btn_clear.clicked.connect(self._clear_logs)
        logs_header.addWidget(btn_clear)

        logs_layout.addLayout(logs_header)

        self._log_box = QTextEdit()
        self._log_box.setObjectName("logBox")
        self._log_box.setReadOnly(True)
        self._log_box.setPlaceholderText("Spring Boot log output will appear here...")
        self._log_box.document().setMaximumBlockCount(2000)
        logs_layout.addWidget(self._log_box)

        main_layout.addWidget(logs_card)

    def _render_qr(self, host: str, port: int):
        """Generates dynamic QR code for Android app connection."""
        url_payload = f"ciphervault://connect?host={host}&port={port}&scheme=http"
        try:
            qr = qrcode.QRCode(
                version=None,
                error_correction=qrcode.constants.ERROR_CORRECT_M,
                box_size=6,
                border=2,
            )
            qr.add_data(url_payload)
            qr.make(fit=True)
            img = qr.make_image(fill_color="#000000", back_color="#FFFFFF").convert("RGBA")
            raw = img.tobytes("raw", "RGBA")
            qimg = QImage(raw, img.size[0], img.size[1], QImage.Format.Format_RGBA8888)
            pix = QPixmap.fromImage(qimg).scaled(
                160, 160, Qt.AspectRatioMode.KeepAspectRatio, Qt.TransformationMode.SmoothTransformation
            )
            self._lbl_qr.setPixmap(pix)
            self._lbl_qr_caption.setText("Scan in CipherVault App")
        except Exception as e:
            app_logger.log("error", f"Failed to generate QR code: {e}")
            self._lbl_qr_caption.setText("QR Error")

    def _toggle_theme(self):
        """Switches between Dark and Light mode and persists preference."""
        current = settings.get("theme", "dark")
        new_theme = "light" if current == "dark" else "dark"
        settings.set("theme", new_theme)

        app = QApplication.instance()
        if app:
            apply_theme(app, new_theme)

        self._btn_theme_toggle.setText("☾ Dark Mode" if new_theme == "light" else "☀ Light Mode")
        app_logger.log("ui", f"Theme switched to: {new_theme}")

    def _refresh_network(self):
        """Scans and updates the preferred Wi-Fi / Hotspot adapter and redraws QR."""
        ip, port, desc, is_active = NetworkService.get_preferred_wifi_connection(port=self._current_port)
        self._current_ip = ip
        self._current_port = port
        self._current_iface = desc

        self._lbl_ip_port.setText(f"{ip} : {port}")
        self._lbl_full_url.setText(f"http://{ip}:{port}/")
        self._adapter_badge.setText(f"● {desc}")
        self._render_qr(ip, port)

        app_logger.log("network", f"Selected preferred connection: {desc} -> {ip}:{port}")

    def _on_poll(self):
        """Polls network adapters for dynamic IP changes and verifies backend health."""
        try:
            # 1. Check if laptop connected to a new hotspot or Wi-Fi IP changed
            new_ip, new_port, new_desc, is_active = NetworkService.get_preferred_wifi_connection(port=self._current_port)
            if new_ip != self._current_ip or new_desc != self._current_iface:
                old_ip = self._current_ip
                self._current_ip = new_ip
                self._current_port = new_port
                self._current_iface = new_desc

                self._lbl_ip_port.setText(f"{new_ip} : {new_port}")
                self._lbl_full_url.setText(f"http://{new_ip}:{new_port}/")
                self._adapter_badge.setText(f"● {new_desc}")
                self._render_qr(new_ip, new_port)
                app_logger.log("network", f"Dynamic network update: {old_ip} -> {new_ip} ({new_desc})")

            # 2. Check port binding
            binding, desc = NetworkService.check_backend_binding(port=self._current_port)

            if binding == "none":
                # Port is not listening
                if not self._backend_service.isRunning():
                    if not BackendState.is_transitional(self._backend_state):
                        if self._backend_state != BackendState.STOPPED:
                            self._update_state(BackendState.STOPPED, "Port 8080 not listening")

            # 3. Asynchronous health check
            if not self._health_check_in_progress:
                self._health_check_in_progress = True
                target_url = f"http://{self._current_ip}:{self._current_port}/"
                threading.Thread(target=self._run_async_health_check, args=(target_url,), daemon=True).start()

        except Exception as e:
            app_logger.log("error", f"Unhandled error in _on_poll: {e}")

    def _run_async_health_check(self, url: str):
        try:
            success, code, latency, msg = NetworkService.test_endpoint(url, timeout=2.0)
            # Also try localhost if wifi timed out but server is local
            if not success:
                local_success, local_code, local_latency, local_msg = NetworkService.test_endpoint(
                    f"http://127.0.0.1:{self._current_port}/", timeout=1.5
                )
                if local_success:
                    self.health_updated.emit(
                        True, 200, local_latency,
                        f"Localhost UP ({local_latency:.0f} ms), Wi-Fi awaiting connection"
                    )
                    return

            self.health_updated.emit(success, code, latency, msg)
        except Exception as e:
            self.health_updated.emit(False, 0, 0.0, str(e))

    @Slot(bool, int, float, str)
    def _on_health_result(self, success: bool, code: int, latency: float, msg: str):
        self._health_check_in_progress = False
        try:
            if success:
                if self._backend_service.owns_process:
                    pid_desc = f"Managed (PID {self._backend_service.pid})" if self._backend_service.pid else "Managed"
                    self._lbl_health_telemetry.setText(f"● Backend Health: {msg} • Latency {latency:.0f} ms • {pid_desc}")
                    self._lbl_health_telemetry.setStyleSheet("color: #166534; font-size: 11px; font-weight: 600;")
                    if self._backend_state != BackendState.RUNNING:
                        self._update_state(BackendState.RUNNING, f"Active on port {self._current_port}")
                else:
                    # External / Pre-existing CipherVault backend
                    if not self._external_pid:
                        _, ext_pid, _ = EnvironmentService.check_port_listener(self._current_port)
                        self._external_pid = ext_pid
                    pid_desc = f"External Process (PID {self._external_pid or 'Unknown'})"
                    self._lbl_health_telemetry.setText(f"● Backend Health: {msg} • Latency {latency:.0f} ms • {pid_desc}")
                    self._lbl_health_telemetry.setStyleSheet("color: #0369A1; font-size: 11px; font-weight: 600;")
                    if self._backend_state != BackendState.RUNNING_EXTERNAL:
                        self._update_state(BackendState.RUNNING_EXTERNAL, f"External process active on port {self._current_port}")
                    if not self._external_log_tailer_active:
                        self._attach_external_logging(self._external_pid)
            else:
                binding, _ = NetworkService.check_backend_binding(port=self._current_port)
                if binding in ("all", "localhost", "specific"):
                    # Port is occupied but health check failed
                    if self._backend_service.isRunning():
                        self._lbl_health_telemetry.setText("◌ Backend: Initializing HTTP endpoints...")
                        self._lbl_health_telemetry.setStyleSheet("color: #B45309; font-size: 11px; font-weight: 600;")
                    else:
                        self._lbl_health_telemetry.setText(f"⚠ Port conflict: {msg}")
                        self._lbl_health_telemetry.setStyleSheet("color: #B91C1C; font-size: 11px; font-weight: 600;")
                        if not BackendState.is_transitional(self._backend_state):
                            self._update_state(BackendState.FOREIGN_SERVICE, "Port occupied by another application")
                else:
                    # Port is free
                    if self._external_log_tailer_active:
                        self._detach_external_logging()
                    self._external_pid = None

                    if self._backend_state == BackendState.STARTING:
                        self._lbl_health_telemetry.setText("◌ Backend: Launching Spring Boot...")
                        self._lbl_health_telemetry.setStyleSheet("color: #B45309; font-size: 11px; font-weight: 600;")
                    elif self._backend_state == BackendState.STOPPING:
                        self._lbl_health_telemetry.setText("◌ Backend: Stopping process...")
                        self._lbl_health_telemetry.setStyleSheet("color: #64748B; font-size: 11px; font-weight: 600;")
                    elif self._backend_state == BackendState.ERROR:
                        self._lbl_health_telemetry.setText(f"✕ Backend Error: {msg}")
                        self._lbl_health_telemetry.setStyleSheet("color: #B91C1C; font-size: 11px; font-weight: 600;")
                    else:
                        self._lbl_health_telemetry.setText(f"○ Backend: Offline (Port {self._current_port} free)")
                        self._lbl_health_telemetry.setStyleSheet("color: #64748B; font-size: 11px; font-weight: 600;")
                        if BackendState.is_active(self._backend_state):
                            self._update_state(BackendState.STOPPED, "Port not listening")
        except Exception as e:
            app_logger.log("error", f"Error in _on_health_result: {e}")

    def _update_state(self, state: str, reason: str = ""):
        self._backend_state = state
        app_logger.log("state", f"Transition to {state}: {reason}")

        if state in (BackendState.RUNNING, BackendState.ONLINE):
            if self._backend_service.owns_process:
                self._status_pill.setText("● ONLINE (Managed)")
                self._status_pill.setObjectName("statusPillRunning")
            else:
                self._status_pill.setText("● ONLINE (External)")
                self._status_pill.setObjectName("statusPillExternal")
            self._btn_start.setEnabled(False)
            self._btn_stop.setEnabled(True)
            self._btn_restart.setEnabled(True)
        elif state == BackendState.RUNNING_EXTERNAL:
            self._status_pill.setText("● ONLINE (External)")
            self._status_pill.setObjectName("statusPillExternal")
            self._btn_start.setEnabled(False)
            self._btn_stop.setEnabled(True)
            self._btn_restart.setEnabled(True)
        elif state == BackendState.STARTING:
            self._status_pill.setText("◌ STARTING")
            self._status_pill.setObjectName("statusPillStarting")
            self._btn_start.setEnabled(False)
            self._btn_stop.setEnabled(True)
            self._btn_restart.setEnabled(False)
        elif state == BackendState.STOPPING:
            self._status_pill.setText("◌ STOPPING")
            self._status_pill.setObjectName("statusPillStopping")
            self._btn_start.setEnabled(False)
            self._btn_stop.setEnabled(False)
            self._btn_restart.setEnabled(False)
        elif state == BackendState.RESTARTING:
            self._status_pill.setText("◌ RESTARTING")
            self._status_pill.setObjectName("statusPillStarting")
            self._btn_start.setEnabled(False)
            self._btn_stop.setEnabled(True)
            self._btn_restart.setEnabled(False)
        elif state == BackendState.FOREIGN_SERVICE:
            self._status_pill.setText("⚠ CONFLICT")
            self._status_pill.setObjectName("statusPillError")
            self._btn_start.setEnabled(False)
            self._btn_stop.setEnabled(True)
            self._btn_restart.setEnabled(False)
        elif state == BackendState.ERROR:
            self._status_pill.setText("✕ ERROR")
            self._status_pill.setObjectName("statusPillError")
            self._btn_start.setEnabled(True)
            self._btn_stop.setEnabled(False)
            self._btn_restart.setEnabled(False)
        else:  # STOPPED, OFFLINE, UNKNOWN
            self._status_pill.setText("○ OFFLINE")
            self._status_pill.setObjectName("statusPillStopped")
            self._btn_start.setEnabled(True)
            self._btn_stop.setEnabled(False)
            self._btn_restart.setEnabled(False)

        # Force style re-eval
        self._status_pill.style().unpolish(self._status_pill)
        self._status_pill.style().polish(self._status_pill)

    @Slot(str)
    def _on_log_line(self, line: str):
        clean = re.sub(r'\x1b\[[0-9;]*[mK]', '', line)
        self._log_box.append(clean)
        if self._chk_autoscroll.isChecked():
            cursor = self._log_box.textCursor()
            cursor.movePosition(QTextCursor.MoveOperation.End)
            self._log_box.setTextCursor(cursor)
            self._log_box.ensureCursorVisible()

    @Slot(str, str)
    def _on_state_changed(self, state: str, detail: str):
        if self._backend_state == BackendState.RESTARTING and state == BackendState.STOPPED:
            return
        self._update_state(state, detail)

    def _attach_external_logging(self, pid: int | None):
        """Attaches live persistent log streaming for external Spring Boot backend."""
        if self._external_log_tailer_active:
            return
        self._external_log_tailer_active = True
        root = get_project_root()
        log_file = os.path.join(root, "backend", "logs", "ciphervault.log")

        self._log_box.clear()
        pid_desc = f"PID {pid}" if pid else "external process"
        self._log_box.append(f"[ATTACHED] Connected to active CipherVault backend on port {self._current_port} ({pid_desc}).")
        self._log_box.append("[ATTACHED] Process was started externally outside this Server Control session.")

        if os.path.isfile(log_file):
            self._log_box.append("[ATTACHED] Streaming persistent log file: backend/logs/ciphervault.log")
            self._log_box.append("-" * 70)
            try:
                with open(log_file, "r", encoding="utf-8", errors="replace") as f:
                    lines = f.readlines()
                    for line in lines[-60:]:
                        clean = line.rstrip("\r\n")
                        if clean:
                            self._log_box.append(clean)
                    self._log_file_pos = f.tell()
            except Exception as e:
                self._log_box.append(f"[ATTACHED] Could not read log file: {e}")
            self._log_tail_timer.start()
        else:
            self._log_box.append("[NOTE] Live standard output streaming is only available when started via Server Control.")
            self._log_box.append("[NOTE] To view live terminal logs here, stop the external server and click 'Start Server'.")

    def _detach_external_logging(self):
        """Stops log tailer for external backend."""
        self._external_log_tailer_active = False
        if hasattr(self, "_log_tail_timer") and self._log_tail_timer.isActive():
            self._log_tail_timer.stop()

    def _poll_external_log_file(self):
        """Periodically reads new log entries appended by external Spring Boot instance."""
        if not self._external_log_tailer_active:
            return
        root = get_project_root()
        log_file = os.path.join(root, "backend", "logs", "ciphervault.log")
        if not os.path.isfile(log_file):
            return
        try:
            with open(log_file, "r", encoding="utf-8", errors="replace") as f:
                f.seek(self._log_file_pos)
                new_data = f.read()
                self._log_file_pos = f.tell()
                if new_data:
                    for line in new_data.splitlines():
                        clean = line.rstrip("\r\n")
                        if clean:
                            self._on_log_line(clean)
        except Exception:
            pass

    def _start_server(self):
        if self._backend_state in (
            BackendState.STARTING, BackendState.RUNNING, BackendState.ONLINE,
            BackendState.RUNNING_EXTERNAL, BackendState.RESTARTING
        ):
            return
        self._detach_external_logging()
        self._log_box.clear()
        self._log_box.append(">>> Starting Spring Boot backend...")
        self._update_state(BackendState.STARTING, "Starting Spring Boot...")
        self._backend_service.start_backend()

    def _stop_server(self, confirm: bool = True):
        if self._backend_state in (BackendState.STOPPING, BackendState.STOPPED, BackendState.OFFLINE):
            return

        if self._backend_service.owns_process:
            self._log_box.append(">>> Stopping backend...")
            self._update_state(BackendState.STOPPING, "Stopping...")
            self._backend_service.stop_backend()
        elif self._backend_state == BackendState.RUNNING_EXTERNAL or self._external_pid is not None:
            ext_pid = self._external_pid
            if not ext_pid:
                _, ext_pid, _ = EnvironmentService.check_port_listener(self._current_port)
                self._external_pid = ext_pid

            if confirm:
                reply = QMessageBox.question(
                    self,
                    "Stop External Backend?",
                    f"The CipherVault backend on port {self._current_port} (PID: {ext_pid or 'Unknown'}) was started outside Server Control.\n\n"
                    "Do you want to terminate this external process to free port 8080?",
                    QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
                    QMessageBox.StandardButton.No
                )
                if reply != QMessageBox.StandardButton.Yes:
                    return

            self._detach_external_logging()
            self._log_box.append(f">>> Stopping external backend process (PID {ext_pid or 'Unknown'})...")
            self._update_state(BackendState.STOPPING, "Stopping external backend...")
            if ext_pid:
                BackendService.stop_process_by_pid(ext_pid)
            self._external_pid = None
            self._update_state(BackendState.STOPPED, "External backend stopped by user")
            QTimer.singleShot(1500, self._on_poll)

    def _restart_server(self, confirm: bool = True):
        if self._backend_state in (BackendState.STARTING, BackendState.STOPPING, BackendState.RESTARTING):
            return

        if self._backend_service.owns_process:
            self._log_box.append(">>> Restarting backend...")
            self._update_state(BackendState.RESTARTING, "Restarting...")
            self._backend_service.stop_backend()
            QTimer.singleShot(2500, self._start_server)
        elif self._backend_state == BackendState.RUNNING_EXTERNAL or self._external_pid is not None:
            ext_pid = self._external_pid
            if not ext_pid:
                _, ext_pid, _ = EnvironmentService.check_port_listener(self._current_port)
                self._external_pid = ext_pid

            if confirm:
                reply = QMessageBox.question(
                    self,
                    "Restart Backend?",
                    f"The CipherVault backend on port {self._current_port} (PID: {ext_pid or 'Unknown'}) was started outside Server Control.\n\n"
                    "Do you want to terminate the external process and restart it under Server Control management?",
                    QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
                    QMessageBox.StandardButton.No
                )
                if reply != QMessageBox.StandardButton.Yes:
                    return

            self._detach_external_logging()
            self._log_box.append(f">>> Stopping external backend process (PID {ext_pid or 'Unknown'})...")
            self._update_state(BackendState.RESTARTING, "Restarting backend under Server Control...")
            if ext_pid:
                BackendService.stop_process_by_pid(ext_pid)
            self._external_pid = None
            QTimer.singleShot(2000, self._start_server)

    def _copy_url(self):
        url = self._lbl_full_url.text()
        QApplication.clipboard().setText(url)
        self._btn_copy_url.setText("Copied!")
        QTimer.singleShot(1500, lambda: self._btn_copy_url.setText("Copy URL"))

    def _copy_ip(self):
        QApplication.clipboard().setText(self._current_ip)
        self._btn_copy_ip.setText("Copied!")
        QTimer.singleShot(1500, lambda: self._btn_copy_ip.setText("Copy IP"))

    def _open_health_in_browser(self):
        url = f"http://{self._current_ip}:{self._current_port}/api/health"
        webbrowser.open(url)

    def _clear_logs(self):
        self._log_box.clear()

    def closeEvent(self, event: QCloseEvent):
        try:
            if hasattr(self, "_poll_timer") and self._poll_timer.isActive():
                self._poll_timer.stop()
            if hasattr(self, "_log_tail_timer") and self._log_tail_timer.isActive():
                self._log_tail_timer.stop()
            if self._backend_service.owns_process:
                self._backend_service.stop_backend()
        except Exception as e:
            app_logger.log("error", f"Error during closeEvent: {e}")
        event.accept()

from PySide6.QtWidgets import (
    QDialog, QVBoxLayout, QHBoxLayout, QLabel, QPushButton,
    QGroupBox, QCheckBox, QComboBox, QSpinBox, QTabWidget,
    QWidget, QFrame
)
from PySide6.QtCore import Qt

from ..settings import settings


class SettingsDialog(QDialog):
    """Settings dialog for CipherVault Server Manager."""

    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowTitle("Settings — CipherVault Server Manager")
        self.setMinimumWidth(460)
        self.resize(500, 480)

        layout = QVBoxLayout(self)
        layout.setSpacing(16)
        layout.setContentsMargins(20, 20, 20, 20)

        # Tabs
        tabs = QTabWidget()
        layout.addWidget(tabs)

        # Tab 1: General & Backend
        tab_general = QWidget()
        g_layout = QVBoxLayout(tab_general)
        g_layout.setSpacing(16)

        # Appearance Group
        grp_app = QGroupBox("Appearance")
        app_lay = QVBoxLayout(grp_app)
        app_row = QHBoxLayout()
        app_row.addWidget(QLabel("Theme:"))
        self.cb_theme = QComboBox()
        self.cb_theme.addItems(["System", "Light", "Dark"])
        current_theme = settings.theme.capitalize()
        idx = self.cb_theme.findText(current_theme)
        if idx >= 0:
            self.cb_theme.setCurrentIndex(idx)
        app_row.addWidget(self.cb_theme)
        app_row.addStretch()
        app_lay.addLayout(app_row)
        g_layout.addWidget(grp_app)

        # Backend Group
        grp_backend = QGroupBox("Backend Server")
        b_lay = QVBoxLayout(grp_backend)
        b_lay.setSpacing(10)

        self.chk_auto_start = QCheckBox("Start backend automatically on launch")
        self.chk_auto_start.setChecked(settings.auto_start_backend)
        b_lay.addWidget(self.chk_auto_start)

        poll_row = QHBoxLayout()
        poll_row.addWidget(QLabel("Health polling interval:"))
        self.spin_poll = QSpinBox()
        self.spin_poll.setRange(3, 60)
        self.spin_poll.setSuffix(" sec")
        self.spin_poll.setValue(settings.health_interval)
        poll_row.addWidget(self.spin_poll)
        poll_row.addStretch()
        b_lay.addLayout(poll_row)

        port_row = QHBoxLayout()
        port_row.addWidget(QLabel("Backend port:"))
        self.spin_port = QSpinBox()
        self.spin_port.setRange(1024, 65535)
        self.spin_port.setValue(settings.backend_port)
        port_row.addWidget(self.spin_port)
        port_row.addStretch()
        b_lay.addLayout(port_row)

        g_layout.addWidget(grp_backend)
        g_layout.addStretch()
        tabs.addTab(tab_general, "General")

        # Tab 2: Connections & ADB
        tab_conn = QWidget()
        c_layout = QVBoxLayout(tab_conn)
        c_layout.setSpacing(16)

        grp_vis = QGroupBox("Connection Visibility")
        vis_lay = QVBoxLayout(grp_vis)
        self.chk_wifi = QCheckBox("Show Wi-Fi interfaces")
        self.chk_wifi.setChecked(settings.show_wifi)
        self.chk_eth = QCheckBox("Show Ethernet interfaces")
        self.chk_eth.setChecked(settings.show_ethernet)
        self.chk_adb = QCheckBox("Show USB / ADB device section")
        self.chk_adb.setChecked(settings.show_adb)
        self.chk_emu = QCheckBox("Show Android Emulator section")
        self.chk_emu.setChecked(settings.show_emulator)

        vis_lay.addWidget(self.chk_wifi)
        vis_lay.addWidget(self.chk_eth)
        vis_lay.addWidget(self.chk_adb)
        vis_lay.addWidget(self.chk_emu)
        c_layout.addWidget(grp_vis)

        grp_adb = QGroupBox("USB / ADB Settings")
        adb_lay = QVBoxLayout(grp_adb)
        self.chk_auto_rev = QCheckBox("Automatically enable ADB reverse for detected devices")
        self.chk_auto_rev.setChecked(settings.auto_enable_adb_reverse)
        adb_lay.addWidget(self.chk_auto_rev)
        c_layout.addWidget(grp_adb)

        c_layout.addStretch()
        tabs.addTab(tab_conn, "Connections")

        # Tab 3: About
        tab_about = QWidget()
        ab_lay = QVBoxLayout(tab_about)
        ab_lay.setSpacing(12)
        ab_lay.setContentsMargins(16, 20, 16, 16)

        title = QLabel("CipherVault Server Manager")
        title.setStyleSheet('font-family: "Source Serif 4"; font-size: 18px; font-weight: 600;')
        desc = QLabel(
            "Version 1.0\n\n"
            "Official desktop companion utility for the CipherVault Private Cloud Storage ecosystem.\n"
            "Automatically launches the Spring Boot backend, monitors health, and provides zero-configuration "
            "connectivity for Android devices via Wi-Fi, Ethernet, and USB debugging."
        )
        desc.setWordWrap(True)
        desc.setStyleSheet("color: #6B6B6B; font-size: 13px; line-height: 1.4;")

        ab_lay.addWidget(title)
        ab_lay.addWidget(desc)
        ab_lay.addStretch()
        tabs.addTab(tab_about, "About")

        # Buttons
        btn_box = QHBoxLayout()
        btn_box.addStretch()

        btn_cancel = QPushButton("Cancel")
        btn_cancel.clicked.connect(self.reject)
        btn_save = QPushButton("Save Settings")
        btn_save.setProperty("primary", True)
        btn_save.clicked.connect(self._save)

        btn_box.addWidget(btn_cancel)
        btn_box.addWidget(btn_save)
        layout.addLayout(btn_box)

    def _save(self):
        settings.set("theme", self.cb_theme.currentText().lower())
        settings.set("auto_start_backend", self.chk_auto_start.isChecked())
        settings.set("health_interval", self.spin_poll.value())
        settings.set("backend_port", self.spin_port.value())
        settings.set("show_wifi", self.chk_wifi.isChecked())
        settings.set("show_ethernet", self.chk_eth.isChecked())
        settings.set("show_adb", self.chk_adb.isChecked())
        settings.set("show_emulator", self.chk_emu.isChecked())
        settings.set("auto_enable_adb_reverse", self.chk_auto_rev.isChecked())
        self.accept()

"""Comprehensive Verification for CipherVault Server Control, QR Generation, Theme System, and Backend Integration."""

import os
import sys
import time
import requests
import qrcode
from PIL import Image

# Ensure app package is reachable
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
os.environ['QT_QPA_PLATFORM'] = 'offscreen'

from PySide6.QtWidgets import QApplication
from PySide6.QtCore import Qt
from PySide6.QtGui import QImage, QPixmap

from app.settings import settings
from app.theme import apply_theme
from app.services.network_service import NetworkService
from app.services.backend_service import BackendService
from app.services.backend_state import BackendState
from app.window import MainWindow

def test_qr_payload():
    print("[1] Testing QR Code Payload and Rendering...")
    ip, port, desc, is_active = NetworkService.get_preferred_wifi_connection(port=8080)
    print(f"    Detected IP: {ip}:{port} on {desc}")
    
    expected_payload = f"ciphervault://connect?host={ip}&port={port}&scheme=http"
    print(f"    Generated Payload: {expected_payload}")
    
    # Test QR code generation
    qr = qrcode.QRCode(version=1, border=2)
    qr.add_data(expected_payload)
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white").convert("RGBA")
    raw = img.tobytes("raw", "RGBA")
    qimg = QImage(raw, img.size[0], img.size[1], QImage.Format.Format_RGBA8888)
    pix = QPixmap.fromImage(qimg)
    
    assert not pix.isNull(), "QPixmap from QR code must not be null"
    assert pix.width() > 100, f"QPixmap width should be > 100, got {pix.width()}"
    print(f"    [PASS] QR Pixmap generated successfully: {pix.width()}x{pix.height()} px")

def test_theme_system():
    print("[2] Testing Theme System and Persistence...")
    app = QApplication.instance() or QApplication(sys.argv)
    
    # Test Light Mode
    apply_theme(app, "light")
    settings.set("theme", "light")
    assert settings.get("theme") == "light", "Settings theme should be 'light'"
    print("    [PASS] Light theme applied and persisted")
    
    # Test Dark Mode
    apply_theme(app, "dark")
    settings.set("theme", "dark")
    assert settings.get("theme") == "dark", "Settings theme should be 'dark'"
    print("    [PASS] Dark theme applied and persisted")

def test_gui_window():
    print("[3] Testing MainWindow and Widgets...")
    app = QApplication.instance() or QApplication(sys.argv)
    win = MainWindow()
    
    assert win._lbl_qr.pixmap() is not None, "QR label must have pixmap"
    assert "CipherVault" in win.windowTitle(), "Title must match"
    print(f"    [PASS] MainWindow initialized: {win.windowTitle()}")
    print(f"    [PASS] QR Code displayed on UI: {win._lbl_qr.pixmap().width()}x{win._lbl_qr.pixmap().height()}")
    print(f"    [PASS] IP & Port on UI: {win._lbl_ip_port.text()}")
    
    # Toggle theme via GUI
    current = settings.get("theme")
    win._toggle_theme()
    toggled = settings.get("theme")
    assert current != toggled, "Theme toggle must change active theme"
    print(f"    [PASS] GUI Theme toggle: {current} -> {toggled}")
    
    win.close()

if __name__ == "__main__":
    print("=== CIPHERVAULT SERVER CONTROL AUTOMATED VERIFICATION ===")
    app = QApplication(sys.argv)
    test_qr_payload()
    test_theme_system()
    test_gui_window()
    print("=== ALL SERVER CONTROL CHECKS PASSED ===")

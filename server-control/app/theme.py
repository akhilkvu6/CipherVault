"""Material 3 Theme definition for CipherVault Server Control.
Provides modern Material 3 styling aligned with the CipherVault Android application,
supporting both Dark and Light themes with dynamic switching.
"""

from PySide6.QtWidgets import QApplication

MATERIAL3_DARK_QSS = """
/* Material 3 Dark Palette — CipherVault Specification */
* {
    font-family: "Segoe UI", "Roboto", sans-serif;
    font-size: 13px;
    color: #E1E3E5;
}

QMainWindow, QDialog {
    background-color: #101416;
}

QWidget#centralWidget {
    background-color: #101416;
}

/* Material 3 Surface Card */
QFrame.m3Card {
    background-color: #1D2022;
    border: 1px solid #2E3133;
    border-radius: 18px;
}

/* Hero Gateway Card */
QFrame.heroCard {
    background-color: #002B38;
    border: 1px solid #004D64;
    border-radius: 20px;
}

/* QR Code Container Frame (white bg so QR code scans cleanly) */
QFrame#qrContainer {
    background-color: #FFFFFF;
    border: 1px solid #004D64;
    border-radius: 14px;
    padding: 6px;
}

/* Labels */
QLabel {
    background: transparent;
}

QLabel#titleLabel {
    font-size: 20px;
    font-weight: 700;
    color: #FFFFFF;
}

QLabel#subtitleLabel {
    font-size: 12px;
    color: #8A9296;
}

QLabel#cardTitle {
    font-size: 15px;
    font-weight: 700;
    color: #FFFFFF;
}

QLabel#heroTitle {
    font-size: 15px;
    font-weight: 700;
    color: #BEE9FF;
}

QLabel#heroEyebrow {
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1px;
    color: #63D3FF;
}

QLabel#heroIpDisplay {
    font-family: "Consolas", "Courier New", monospace;
    font-size: 24px;
    font-weight: 700;
    color: #FFFFFF;
}

QLabel#heroUrlDisplay {
    font-family: "Consolas", "Courier New", monospace;
    font-size: 12px;
    color: #BEE9FF;
}

QLabel#statusPillRunning {
    background-color: #163824;
    border: 1px solid #2E7D32;
    border-radius: 12px;
    color: #6FD58A;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 10px;
}

QLabel#statusPillStopped {
    background-color: #381A1B;
    border: 1px solid #852221;
    border-radius: 12px;
    color: #FF817A;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 10px;
}

QLabel#statusPillStarting {
    background-color: #382C13;
    border: 1px solid #8C6D1F;
    border-radius: 12px;
    color: #FACC15;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 10px;
}

QLabel#statusPillStopping {
    background-color: #382C13;
    border: 1px solid #8C6D1F;
    border-radius: 12px;
    color: #FACC15;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 10px;
}

QLabel#statusPillError {
    background-color: #441A1B;
    border: 1px solid #BA1A1A;
    border-radius: 12px;
    color: #FFB4AB;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 10px;
}

/* Material 3 Buttons */
QPushButton {
    border-radius: 18px;
    padding: 7px 16px;
    font-weight: 600;
    font-size: 12px;
    background-color: #272B2D;
    border: 1px solid #40484C;
    color: #E1E3E5;
}

QPushButton:hover {
    background-color: #323538;
    border-color: #70787D;
}

QPushButton:pressed {
    background-color: #1D2022;
}

QPushButton#btnPrimary {
    background-color: #63D3FF;
    border: none;
    color: #003546;
    font-weight: 700;
}

QPushButton#btnPrimary:hover {
    background-color: #8CE0FF;
}

QPushButton#btnPrimary:pressed {
    background-color: #4CBCEB;
}

QPushButton#btnDanger {
    background-color: #93000A;
    border: 1px solid #BA1A1A;
    color: #FFDAD6;
}

QPushButton#btnDanger:hover {
    background-color: #BA1A1A;
}

QPushButton#btnTonal {
    background-color: #004D64;
    border: none;
    color: #BEE9FF;
    font-weight: 600;
}

QPushButton#btnTonal:hover {
    background-color: #00607D;
}

/* Console Log Box */
QTextEdit#logBox {
    background-color: #0B0F11;
    border: 1px solid #1D2022;
    border-radius: 12px;
    color: #BFC8CC;
    font-family: "Consolas", "Courier New", monospace;
    font-size: 11px;
    padding: 10px;
    selection-background-color: #004D64;
}

/* Scrollbars */
QScrollBar:vertical {
    background: transparent;
    width: 6px;
    margin: 0px;
}

QScrollBar::handle:vertical {
    background: #323538;
    border-radius: 3px;
    min-height: 20px;
}

QScrollBar::handle:vertical:hover {
    background: #40484C;
}

QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical {
    height: 0px;
}
"""

MATERIAL3_LIGHT_QSS = """
/* Material 3 Light Palette — CipherVault Specification */
* {
    font-family: "Segoe UI", "Roboto", sans-serif;
    font-size: 13px;
    color: #1F2937;
}

QMainWindow, QDialog {
    background-color: #F8FAFC;
}

QWidget#centralWidget {
    background-color: #F8FAFC;
}

/* Material 3 Surface Card */
QFrame.m3Card {
    background-color: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 18px;
}

/* Hero Gateway Card */
QFrame.heroCard {
    background-color: #F0F9FF;
    border: 1px solid #BAE6FD;
    border-radius: 20px;
}

/* QR Code Container Frame */
QFrame#qrContainer {
    background-color: #FFFFFF;
    border: 1px solid #BAE6FD;
    border-radius: 14px;
    padding: 6px;
}

/* Labels */
QLabel {
    background: transparent;
}

QLabel#titleLabel {
    font-size: 20px;
    font-weight: 700;
    color: #0F172A;
}

QLabel#subtitleLabel {
    font-size: 12px;
    color: #64748B;
}

QLabel#cardTitle {
    font-size: 15px;
    font-weight: 700;
    color: #0F172A;
}

QLabel#heroTitle {
    font-size: 15px;
    font-weight: 700;
    color: #0369A1;
}

QLabel#heroEyebrow {
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1px;
    color: #0284C7;
}

QLabel#heroIpDisplay {
    font-family: "Consolas", "Courier New", monospace;
    font-size: 24px;
    font-weight: 700;
    color: #0F172A;
}

QLabel#heroUrlDisplay {
    font-family: "Consolas", "Courier New", monospace;
    font-size: 12px;
    color: #0369A1;
}

QLabel#statusPillRunning {
    background-color: #DCFCE7;
    border: 1px solid #86EFAC;
    border-radius: 12px;
    color: #166534;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 10px;
}

QLabel#statusPillStopped {
    background-color: #FEE2E2;
    border: 1px solid #FCA5A5;
    border-radius: 12px;
    color: #991B1B;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 10px;
}

QLabel#statusPillStarting {
    background-color: #FEF3C7;
    border: 1px solid #FCD34D;
    border-radius: 12px;
    color: #92400E;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 10px;
}

QLabel#statusPillStopping {
    background-color: #FEF3C7;
    border: 1px solid #FCD34D;
    border-radius: 12px;
    color: #92400E;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 10px;
}

QLabel#statusPillError {
    background-color: #FEE2E2;
    border: 1px solid #F87171;
    border-radius: 12px;
    color: #991B1B;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 10px;
}

/* Material 3 Buttons */
QPushButton {
    border-radius: 18px;
    padding: 7px 16px;
    font-weight: 600;
    font-size: 12px;
    background-color: #F1F5F9;
    border: 1px solid #CBD5E1;
    color: #1E293B;
}

QPushButton:hover {
    background-color: #E2E8F0;
    border-color: #94A3B8;
}

QPushButton:pressed {
    background-color: #CBD5E1;
}

QPushButton#btnPrimary {
    background-color: #0284C7;
    border: none;
    color: #FFFFFF;
    font-weight: 700;
}

QPushButton#btnPrimary:hover {
    background-color: #0369A1;
}

QPushButton#btnPrimary:pressed {
    background-color: #075985;
}

QPushButton#btnDanger {
    background-color: #EF4444;
    border: 1px solid #DC2626;
    color: #FFFFFF;
}

QPushButton#btnDanger:hover {
    background-color: #DC2626;
}

QPushButton#btnTonal {
    background-color: #E0F2FE;
    border: 1px solid #BAE6FD;
    color: #0369A1;
    font-weight: 600;
}

QPushButton#btnTonal:hover {
    background-color: #BAE6FD;
}

/* Console Log Box */
QTextEdit#logBox {
    background-color: #0F172A;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    color: #E2E8F0;
    font-family: "Consolas", "Courier New", monospace;
    font-size: 11px;
    padding: 10px;
    selection-background-color: #0284C7;
}

/* Scrollbars */
QScrollBar:vertical {
    background: transparent;
    width: 6px;
    margin: 0px;
}

QScrollBar::handle:vertical {
    background: #CBD5E1;
    border-radius: 3px;
    min-height: 20px;
}

QScrollBar::handle:vertical:hover {
    background: #94A3B8;
}

QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical {
    height: 0px;
}
"""

def apply_theme(app_or_window, theme_name: str = "dark"):
    """Applies unified Material 3 theme to the desktop application or window."""
    is_light = (theme_name.lower() == "light")
    qss = MATERIAL3_LIGHT_QSS if is_light else MATERIAL3_DARK_QSS
    app_or_window.setStyleSheet(qss)

from PySide6.QtWidgets import QApplication
from PySide6.QtGui import QPalette

COMMON_STYLES = """
QScrollBar:vertical {
    width: 6px;
    background: transparent;
    margin: 0px;
}
QScrollBar::handle:vertical {
    background: #C8C4C0;
    border-radius: 3px;
    min-height: 20px;
}
QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical {
    height: 0px;
}
QTabWidget::pane {
    border: 1px solid #E6E4E0;
    border-radius: 8px;
    background: #FFFFFF;
    top: -1px;
}
QTabBar::tab {
    padding: 8px 16px;
    margin-right: 4px;
    border-top-left-radius: 8px;
    border-top-right-radius: 8px;
    background: #F0EEE9;
    color: #6B6B6B;
    font-weight: 600;
    font-size: 13px;
}
QTabBar::tab:selected {
    background: #FFFFFF;
    color: #1D1D1D;
    border: 1px solid #E6E4E0;
    border-bottom: none;
}
QCheckBox {
    spacing: 8px;
    font-size: 13px;
}
QComboBox, QSpinBox {
    padding: 5px 10px;
    border: 1px solid #D0CCC8;
    border-radius: 6px;
    background: #FFFFFF;
    font-family: "JetBrains Mono";
    font-size: 13px;
}
"""

LIGHT_QSS = COMMON_STYLES + """
* {
    font-family: "Nunito Sans";
    font-size: 13px;
    color: #1D1D1D;
}

QMainWindow, QDialog {
    background-color: #F7F6F3;
}

QWidget#centralWidget, QScrollArea QWidget#containerWidget {
    background-color: #F7F6F3;
}

/* One UI Card Surface */
QFrame.oneUiCard {
    background-color: #FFFFFF;
    border: 1px solid #EAE7E2;
    border-radius: 14px;
}

/* Recommended Tile Surface (Light Mode) */
QFrame.recommendedCard {
    background-color: #F3FAF4;
    border: 1px solid #D2EBD5;
    border-radius: 14px;
}

QLabel { background: transparent; }

/* Buttons */
QPushButton {
    background-color: #FFFFFF;
    border: 1px solid #D6D2CC;
    border-radius: 8px;
    padding: 5px 14px;
    font-family: "Nunito Sans";
    font-weight: 600;
    font-size: 12px;
    color: #2D2D2D;
}
QPushButton:hover {
    background-color: #F4F2EE;
    border-color: #C8C3BC;
}
QPushButton:pressed {
    background-color: #ECE9E4;
}
QPushButton:disabled {
    color: #A8A49E;
    border-color: #E6E3DE;
    background-color: #F8F7F5;
}

/* Primary Button (Start / Accent) */
QPushButton[primary="true"] {
    background-color: #8C7B6D;
    color: #FFFFFF;
    border: 1px solid #8C7B6D;
}
QPushButton[primary="true"]:hover {
    background-color: #79695C;
    border-color: #79695C;
}
QPushButton[primary="true"]:pressed {
    background-color: #685A4E;
}
QPushButton[primary="true"]:disabled {
    background-color: #C5BCB3;
    border-color: #C5BCB3;
    color: #EFECE8;
}

/* Recommended Action Button (Green Accent) */
QPushButton[recommended="true"] {
    background-color: #2E7D32;
    color: #FFFFFF;
    border: 1px solid #2E7D32;
    border-radius: 8px;
    padding: 5px 16px;
    font-family: "Nunito Sans";
    font-weight: 700;
    font-size: 12px;
}
QPushButton[recommended="true"]:hover {
    background-color: #256829;
    border-color: #256829;
}
QPushButton[recommended="true"]:pressed {
    background-color: #1D5421;
}

/* Collapsible Section Header */
QPushButton.collapsibleHeader {
    text-align: left;
    background-color: transparent;
    border: none;
    font-family: "Nunito Sans";
    font-weight: 700;
    font-size: 13px;
    color: #4A443E;
    padding: 6px 4px;
}
QPushButton.collapsibleHeader:hover {
    color: #1D1D1D;
    background-color: rgba(0, 0, 0, 0.03);
    border-radius: 6px;
}

/* Text Editors */
QTextEdit {
    background-color: #FAFAF8;
    border: 1px solid #E8E5E0;
    border-radius: 8px;
    font-family: "JetBrains Mono";
    font-size: 12px;
    padding: 8px;
    color: #1D1D1D;
}

QFrame.subtleDivider {
    color: #EAE7E2;
    background-color: #EAE7E2;
    max-height: 1px;
    border: none;
}
"""

DARK_QSS = COMMON_STYLES + """
* {
    font-family: "Nunito Sans";
    font-size: 13px;
    color: #E4E4E4;
}

QMainWindow, QDialog {
    background-color: #121212;
}

QWidget#centralWidget, QScrollArea QWidget#containerWidget {
    background-color: #121212;
}

/* One UI Card Surface (Dark) */
QFrame.oneUiCard {
    background-color: #1E1E1E;
    border: 1px solid #2A2A2A;
    border-radius: 14px;
}

/* Recommended Tile Surface (Dark Mode) */
QFrame.recommendedCard {
    background-color: #162418;
    border: 1px solid #28442B;
    border-radius: 14px;
}

QTabWidget::pane {
    border: 1px solid #2E2E2E;
    background: #1E1E1E;
}
QTabBar::tab {
    background: #181818;
    color: #888888;
}
QTabBar::tab:selected {
    background: #1E1E1E;
    color: #E4E4E4;
    border: 1px solid #2E2E2E;
}
QComboBox, QSpinBox {
    border: 1px solid #3A3A3A;
    background: #1E1E1E;
    color: #E4E4E4;
}

QLabel { background: transparent; color: #E4E4E4; }

/* Buttons (Dark) */
QPushButton {
    background-color: #1E1E1E;
    border: 1px solid #363636;
    border-radius: 8px;
    padding: 5px 14px;
    font-family: "Nunito Sans";
    font-weight: 600;
    font-size: 12px;
    color: #E4E4E4;
}
QPushButton:hover {
    background-color: #282828;
    border-color: #444444;
}
QPushButton:pressed {
    background-color: #303030;
}
QPushButton:disabled {
    color: #555555;
    border-color: #242424;
    background-color: #161616;
}

/* Primary Button (Dark) */
QPushButton[primary="true"] {
    background-color: #A39183;
    color: #FFFFFF;
    border: 1px solid #A39183;
}
QPushButton[primary="true"]:hover {
    background-color: #B5A599;
    border-color: #B5A599;
}
QPushButton[primary="true"]:pressed {
    background-color: #928174;
}
QPushButton[primary="true"]:disabled {
    background-color: #4A423B;
    border-color: #4A423B;
    color: #7A726B;
}

/* Recommended Action Button (Dark Mode) */
QPushButton[recommended="true"] {
    background-color: #388E3C;
    color: #FFFFFF;
    border: 1px solid #388E3C;
    border-radius: 8px;
    padding: 5px 16px;
    font-family: "Nunito Sans";
    font-weight: 700;
    font-size: 12px;
}
QPushButton[recommended="true"]:hover {
    background-color: #2E7D32;
    border-color: #2E7D32;
}
QPushButton[recommended="true"]:pressed {
    background-color: #256829;
}

/* Collapsible Section Header (Dark) */
QPushButton.collapsibleHeader {
    text-align: left;
    background-color: transparent;
    border: none;
    font-family: "Nunito Sans";
    font-weight: 700;
    font-size: 13px;
    color: #C8C3BC;
    padding: 6px 4px;
}
QPushButton.collapsibleHeader:hover {
    color: #FFFFFF;
    background-color: rgba(255, 255, 255, 0.05);
    border-radius: 6px;
}

/* Text Editors (Dark) */
QTextEdit {
    background-color: #181818;
    border: 1px solid #282828;
    border-radius: 8px;
    font-family: "JetBrains Mono";
    font-size: 12px;
    padding: 8px;
    color: #D6D6D6;
}

QScrollBar::handle:vertical {
    background: #383838;
}

QFrame.subtleDivider {
    color: #282828;
    background-color: #282828;
    max-height: 1px;
    border: none;
}
"""


def apply_theme(app: QApplication, preference: str = "system"):
    if preference == "dark":
        app.setStyleSheet(DARK_QSS)
    elif preference == "light":
        app.setStyleSheet(LIGHT_QSS)
    else:
        palette = app.palette()
        bg_lightness = palette.color(QPalette.ColorRole.Window).lightness()
        if bg_lightness < 128:
            app.setStyleSheet(DARK_QSS)
        else:
            app.setStyleSheet(LIGHT_QSS)

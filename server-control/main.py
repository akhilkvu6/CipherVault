import os
import sys

# Ensure server-control directory is in sys.path
_BASE_DIR = os.path.dirname(os.path.abspath(__file__))
if _BASE_DIR not in sys.path:
    sys.path.insert(0, _BASE_DIR)

from PySide6.QtWidgets import QApplication

from app.settings import settings
from app.theme import apply_theme
from app.window import MainWindow
from app.services.log_service import app_logger


def main():
    app_logger.log("app", "CipherVault Server Manager starting")

    app = QApplication(sys.argv)
    app.setApplicationName("CipherVault Server Manager")
    app.setOrganizationName("CipherVault")

    # Apply configured theme
    apply_theme(app, settings.theme)

    window = MainWindow()
    window.show()

    exit_code = app.exec()
    app_logger.log("app", f"CipherVault Server Manager exiting with code {exit_code}")
    sys.exit(exit_code)


if __name__ == "__main__":
    main()

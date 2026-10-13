"""Automated Forensic Test Suite for CipherVault Server Control.

Validates state machine transitions, settings resilience, network services,
environment checks, ADB integration, log sanitization, and GUI responsiveness.
"""

import os
import sys
import json
import tempfile
import unittest

# Ensure server-control root is in path
CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
SERVER_CONTROL_DIR = os.path.dirname(CURRENT_DIR)
if SERVER_CONTROL_DIR not in sys.path:
    sys.path.insert(0, SERVER_CONTROL_DIR)

from PySide6.QtWidgets import QApplication

# Create single shared QApplication for headless testing
app = QApplication.instance()
if app is None:
    app = QApplication(sys.argv)

from app.services.backend_state import BackendState
from app.services.environment_service import EnvironmentService, get_project_root
from app.services.network_service import NetworkService
from app.services.adb_service import AdbService
from app.services.log_service import LogService
from app.settings import SettingsManager
from app.window import MainWindow


class TestBackendState(unittest.TestCase):
    """Verifies that the BackendState model is complete and robust."""

    def test_all_states_defined(self):
        required_states = [
            "STOPPED", "STARTING", "RUNNING", "ONLINE",
            "STOPPING", "RESTARTING", "OFFLINE", "ERROR",
            "FOREIGN_SERVICE", "UNKNOWN"
        ]
        for s in required_states:
            self.assertTrue(hasattr(BackendState, s), f"Missing state {s}")
            val = getattr(BackendState, s)
            self.assertIsInstance(val, str)

    def test_helper_predicates(self):
        # is_active
        self.assertTrue(BackendState.is_active(BackendState.RUNNING))
        self.assertTrue(BackendState.is_active(BackendState.ONLINE))
        self.assertTrue(BackendState.is_active(BackendState.RUNNING_EXTERNAL))
        self.assertFalse(BackendState.is_active(BackendState.STOPPED))
        self.assertFalse(BackendState.is_active(BackendState.STARTING))

        # is_inactive
        self.assertTrue(BackendState.is_inactive(BackendState.STOPPED))
        self.assertTrue(BackendState.is_inactive(BackendState.OFFLINE))
        self.assertFalse(BackendState.is_inactive(BackendState.RUNNING))

        # is_transitional
        self.assertTrue(BackendState.is_transitional(BackendState.STARTING))
        self.assertTrue(BackendState.is_transitional(BackendState.STOPPING))
        self.assertTrue(BackendState.is_transitional(BackendState.RESTARTING))
        self.assertFalse(BackendState.is_transitional(BackendState.RUNNING))
        self.assertFalse(BackendState.is_transitional(BackendState.STOPPED))

        # is_error
        self.assertTrue(BackendState.is_error(BackendState.ERROR))
        self.assertTrue(BackendState.is_error(BackendState.FOREIGN_SERVICE))
        self.assertFalse(BackendState.is_error(BackendState.RUNNING))


class TestSettingsResilience(unittest.TestCase):
    """Verifies SettingsManager recovers gracefully from corrupted or missing files."""

    def test_defaults(self):
        mgr = SettingsManager()
        self.assertEqual(mgr.backend_port, 8080)
        self.assertFalse(mgr.auto_start_backend)
        self.assertIn(mgr.theme, ["system", "dark", "light"])

    def test_corrupted_json_handling(self):
        with tempfile.NamedTemporaryFile("w", delete=False, suffix=".json") as f:
            f.write("{ invalid json : bad syntax")
            tmp_path = f.name

        try:
            # Should not crash on invalid JSON
            import app.settings as s_mod
            old_file = s_mod.SETTINGS_FILE
            s_mod.SETTINGS_FILE = tmp_path
            mgr = SettingsManager()
            self.assertEqual(mgr.backend_port, 8080)
            s_mod.SETTINGS_FILE = old_file
        finally:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)


class TestEnvironmentService(unittest.TestCase):
    """Verifies environment discovery on the workstation."""

    def test_project_root_resolution(self):
        root = get_project_root()
        self.assertTrue(os.path.isdir(root))
        self.assertTrue(os.path.isdir(os.path.join(root, "backend")))
        self.assertTrue(os.path.isfile(os.path.join(root, "backend", "mvnw.cmd")))

    def test_check_java(self):
        ok, ver = EnvironmentService.check_java()
        self.assertTrue(ok, f"Java should be installed and on PATH: {ver}")

    def test_check_maven_wrapper(self):
        ok, path = EnvironmentService.check_maven_wrapper()
        self.assertTrue(ok, f"mvnw.cmd should exist: {path}")

    def test_check_mysql(self):
        ok, msg = EnvironmentService.check_mysql()
        self.assertTrue(ok, f"MySQL should be active on 3306: {msg}")


class TestNetworkService(unittest.TestCase):
    """Verifies network adapter detection and binding analysis."""

    def test_classified_adapters(self):
        adapters = NetworkService.get_classified_adapters()
        self.assertIn("wifi", adapters)
        self.assertIn("ethernet", adapters)
        self.assertIn("other", adapters)

    def test_preferred_wifi(self):
        ip, port, desc, is_active = NetworkService.get_preferred_wifi_connection(port=8080)
        self.assertEqual(port, 8080)
        self.assertTrue(len(ip) >= 7)  # e.g. 10.x.x.x or 127.0.0.1


class TestLogSanitization(unittest.TestCase):
    """Verifies sensitive secrets are never written to server-manager logs."""

    def test_secret_scrubbing(self):
        logger = LogService()
        sanitized = logger._sanitize("Connecting with password=SecretPassword123!")
        self.assertEqual(sanitized, "[FILTERED SENSITIVE LOG ENTRY]")

        normal = logger._sanitize("Java version: 21.0.12")
        self.assertEqual(normal, "Java version: 21.0.12")


class TestMainWindowStateTransitions(unittest.TestCase):
    """Verifies that all GUI state transitions update buttons, pills, and labels correctly."""

    def setUp(self):
        self.win = MainWindow()

    def tearDown(self):
        self.win.close()

    def test_initial_state(self):
        self.assertEqual(self.win._backend_state, BackendState.STOPPED)
        self.assertTrue(self.win._btn_start.isEnabled())
        self.assertFalse(self.win._btn_stop.isEnabled())
        self.assertFalse(self.win._btn_restart.isEnabled())
        self.assertIn("OFFLINE", self.win._status_pill.text())

    def test_transition_starting(self):
        self.win._update_state(BackendState.STARTING, "Testing Starting")
        self.assertEqual(self.win._backend_state, BackendState.STARTING)
        self.assertFalse(self.win._btn_start.isEnabled())
        self.assertTrue(self.win._btn_stop.isEnabled())
        self.assertFalse(self.win._btn_restart.isEnabled())
        self.assertIn("STARTING", self.win._status_pill.text())

    def test_transition_running(self):
        self.win._update_state(BackendState.RUNNING, "Testing Running")
        self.assertEqual(self.win._backend_state, BackendState.RUNNING)
        self.assertFalse(self.win._btn_start.isEnabled())
        self.assertTrue(self.win._btn_stop.isEnabled())
        self.assertTrue(self.win._btn_restart.isEnabled())
        self.assertIn("ONLINE", self.win._status_pill.text())

    def test_transition_stopping(self):
        self.win._update_state(BackendState.STOPPING, "Testing Stopping")
        self.assertEqual(self.win._backend_state, BackendState.STOPPING)
        self.assertFalse(self.win._btn_start.isEnabled())
        self.assertFalse(self.win._btn_stop.isEnabled())
        self.assertFalse(self.win._btn_restart.isEnabled())
        self.assertIn("STOPPING", self.win._status_pill.text())

    def test_transition_error(self):
        self.win._update_state(BackendState.ERROR, "Process exited with code 1")
        self.assertEqual(self.win._backend_state, BackendState.ERROR)
        self.assertTrue(self.win._btn_start.isEnabled())  # Allow retry
        self.assertFalse(self.win._btn_stop.isEnabled())
        self.assertFalse(self.win._btn_restart.isEnabled())
        self.assertIn("ERROR", self.win._status_pill.text())

    def test_transition_foreign_service(self):
        self.win._update_state(BackendState.FOREIGN_SERVICE, "Port 8080 occupied")
        self.assertEqual(self.win._backend_state, BackendState.FOREIGN_SERVICE)
        self.assertFalse(self.win._btn_start.isEnabled())
        self.assertTrue(self.win._btn_stop.isEnabled())  # Allow kill
        self.assertFalse(self.win._btn_restart.isEnabled())
        self.assertIn("CONFLICT", self.win._status_pill.text())

    def test_race_condition_guard(self):
        # Setting state to STARTING, then clicking start again should be a no-op
        self.win._update_state(BackendState.STARTING, "Starting...")
        # Simulating start while starting
        self.win._start_server()
        # State should still be STARTING and not crash or spawn duplicate
        self.assertEqual(self.win._backend_state, BackendState.STARTING)

    def test_transition_running_managed_vs_external(self):
        # 1. Managed process
        self.win._backend_service._owns_process = True
        self.win._update_state(BackendState.RUNNING, "Managed server online")
        self.assertEqual(self.win._backend_state, BackendState.RUNNING)
        self.assertIn("Managed", self.win._status_pill.text())
        self.assertFalse(self.win._btn_start.isEnabled())
        self.assertTrue(self.win._btn_stop.isEnabled())
        self.assertTrue(self.win._btn_restart.isEnabled())

        # 2. External process
        self.win._backend_service._owns_process = False
        self.win._update_state(BackendState.RUNNING_EXTERNAL, "External server detected")
        self.assertEqual(self.win._backend_state, BackendState.RUNNING_EXTERNAL)
        self.assertIn("External", self.win._status_pill.text())
        self.assertFalse(self.win._btn_start.isEnabled())
        self.assertTrue(self.win._btn_stop.isEnabled())
        self.assertTrue(self.win._btn_restart.isEnabled())

    def test_log_line_scrubbing_and_exit_code(self):
        # Verify ANSI codes are scrubbed and text is displayed
        ansi_line = "\x1b[32m[INFO] Tomcat started on port 8080\x1b[0m"
        self.win._on_log_line(ansi_line)
        log_text = self.win._log_box.toPlainText()
        self.assertIn("[INFO] Tomcat started on port 8080", log_text)
        self.assertNotIn("\x1b[32m", log_text)

        # Verify exit code line is appended cleanly
        exit_line = ">>> Backend process terminated (Exit code: 0)"
        self.win._on_log_line(exit_line)
        self.assertIn(exit_line, self.win._log_box.toPlainText())

    def test_safe_external_stop_confirm_bypass(self):
        # Verify stopping external process with confirm=False updates state to STOPPED
        self.win._update_state(BackendState.RUNNING_EXTERNAL, "External backend")
        self.win._external_pid = None  # None mock
        self.win._stop_server(confirm=False)
        self.assertEqual(self.win._backend_state, BackendState.STOPPED)
        self.assertTrue(self.win._btn_start.isEnabled())
        self.assertFalse(self.win._btn_stop.isEnabled())

    def test_external_health_detection_attaches_and_tails(self):
        # When health succeeds but owns_process is False:
        self.win._backend_service._owns_process = False
        self.win._on_health_result(True, 200, 12.0, "UP")
        self.assertEqual(self.win._backend_state, BackendState.RUNNING_EXTERNAL)
        self.assertIn("External", self.win._status_pill.text())
        self.assertTrue(self.win._external_log_tailer_active)
        self.assertIn("[ATTACHED]", self.win._log_box.toPlainText())
        self.assertFalse(self.win._btn_start.isEnabled())
        self.assertTrue(self.win._btn_stop.isEnabled())
        self.assertTrue(self.win._btn_restart.isEnabled())

    def test_restart_transitions(self):
        # 1. Managed restart
        self.win._backend_service._owns_process = True
        self.win._update_state(BackendState.RUNNING, "Managed server")
        self.win._restart_server(confirm=False)
        self.assertEqual(self.win._backend_state, BackendState.RESTARTING)

        # 2. External restart
        self.win._backend_service._owns_process = False
        self.win._update_state(BackendState.RUNNING_EXTERNAL, "External server")
        self.win._external_pid = None
        self.win._restart_server(confirm=False)
        self.assertEqual(self.win._backend_state, BackendState.RESTARTING)


if __name__ == "__main__":
    unittest.main()

import os
import re
import subprocess
import time
import requests
import psutil
from PySide6.QtCore import QThread, Signal

from .environment_service import get_project_root, EnvironmentService
from .health_service import BackendState
from .log_service import app_logger


class BackendService(QThread):
    """Manages the lifecycle of the Spring Boot backend process."""

    log_line = Signal(str)            # Raw stdout/stderr line
    state_changed = Signal(str, str)  # (BackendState, reason/detail)
    startup_info = Signal(dict)       # Parsed startup info dict

    def __init__(self, parent=None):
        super().__init__(parent)
        self._process: subprocess.Popen | None = None
        self._running = False
        self._owns_process = False   # True ONLY if Server Manager launched it
        self._pid: int | None = None
        self._info: dict = {}

    @property
    def pid(self) -> int | None:
        return self._pid

    @property
    def owns_process(self) -> bool:
        return self._owns_process

    @property
    def ownership_label(self) -> str:
        if self._owns_process:
            return "Managed by Server Manager"
        elif self._pid is not None:
            return "External process"
        return "Not running"

    def start_backend(self):
        if self.isRunning():
            return
        self._running = True
        self.start()

    def stop_backend(self):
        """Stops the backend process if owned by Server Manager. Non-blocking."""
        self._running = False
        if self._process and self._owns_process:
            app_logger.log("backend", f"Terminating managed backend tree (PID {self._process.pid})")
            self._kill_process_tree(self._process.pid)
            self._owns_process = False
            self._pid = None
            self.state_changed.emit(BackendState.STOPPED, "Backend stopped by user")

    def run(self):
        """Worker thread entry point."""
        # 1. Pre-check port 8080
        occupied, existing_pid, proc_name = EnvironmentService.check_port_listener(8080)
        if occupied:
            self._handle_existing_port(existing_pid, proc_name)
            return

        # 2. Check environment
        root = get_project_root()
        backend_dir = os.path.join(root, "backend")
        mvnw = os.path.join(backend_dir, "mvnw.cmd")

        if not os.path.isfile(mvnw):
            msg = f"Maven wrapper not found: {mvnw}"
            app_logger.log("error", msg)
            self.state_changed.emit(BackendState.ERROR, msg)
            return

        java_ok, java_ver = EnvironmentService.check_java()
        if not java_ok:
            msg = f"Java check failed: {java_ver}"
            app_logger.log("error", msg)
            self.state_changed.emit(BackendState.ERROR, msg)
            return

        self.log_line.emit(f"Java version: {java_ver}")
        self.log_line.emit(f"Maven wrapper: {mvnw}")
        self.log_line.emit(f"Backend directory: {backend_dir}")
        self.log_line.emit("Starting Spring Boot backend...")
        app_logger.log("backend", f"Launching mvnw spring-boot:run in {backend_dir}")

        self.state_changed.emit(BackendState.STARTING, "Launching Spring Boot...")

        # 3. Launch process with cmd /c on Windows
        try:
            self._process = subprocess.Popen(
                ["cmd", "/c", mvnw, "spring-boot:run"],
                cwd=backend_dir,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                encoding="utf-8",
                errors="replace",
                creationflags=subprocess.CREATE_NO_WINDOW
            )
            self._owns_process = True
            self._pid = self._process.pid
            self.log_line.emit(f"Launcher PID: {self._pid}")
            app_logger.log("backend", f"Backend process launched with PID {self._pid}")

        except Exception as e:
            msg = f"Failed to execute backend process: {e}"
            app_logger.log("error", msg)
            self.state_changed.emit(BackendState.ERROR, msg)
            return

        # 4. Stream stdout until exit
        self._stream_output()

        # 5. Process finished
        exit_code = self._process.poll()
        if exit_code is None:
            exit_code = 0

        self._owns_process = False
        self._pid = None

        if exit_code == 0 or not self._running:
            self.state_changed.emit(BackendState.STOPPED, "Backend stopped")
            app_logger.log("backend", "Backend stopped normally")
        else:
            self.state_changed.emit(BackendState.ERROR, f"Backend exited with error code {exit_code}")
            app_logger.log("backend", f"Backend exited with code {exit_code}")

    def _stream_output(self):
        try:
            for line in self._process.stdout:
                if not self._running:
                    break
                clean_line = line.rstrip("\r\n")
                if clean_line:
                    self.log_line.emit(clean_line)
                    self._parse_log_line(clean_line)
        except Exception:
            pass

    def _parse_log_line(self, line: str):
        if "Tomcat started on port" in line or "Tomcat started on ports" in line:
            self.state_changed.emit(BackendState.STARTING, "Tomcat started, checking health...")

        m = re.search(r"Starting CiphervaultApplication.*PID (\d+)", line)
        if m:
            self._info["jvm_pid"] = int(m.group(1))

        m = re.search(r"Starting CiphervaultApplication using Java ([\d.]+)", line)
        if m:
            self._info["java_version"] = m.group(1)

        m = re.search(r"Host\s+:\s+(.+)", line)
        if m:
            self._info["host"] = m.group(1).strip()

        m = re.search(r"spring-boot:([\d.]+):run", line)
        if m:
            self._info["spring_version"] = m.group(1)

        m = re.search(r"Building CipherVault ([\d.\-A-Z]+)", line)
        if m:
            self._info["app_version"] = m.group(1)

        m = re.search(r"Database version: ([\d.]+)", line)
        if m:
            self._info["mysql_version"] = m.group(1)

        if "Connection        : [OK]" in line:
            self._info["db_connected"] = True

        if "JWT               : [OK]" in line:
            self._info["jwt_ok"] = True
        if "Password encoder  : [OK] BCrypt" in line:
            self._info["bcrypt_ok"] = True
        if "Key manager       : [OK]" in line:
            self._info["key_mgr_ok"] = True

        if "Algorithm         : AES-256-GCM" in line:
            self._info["crypto_algo"] = "AES-256-GCM"
        if "Mode              : STREAMING" in line:
            self._info["crypto_mode"] = "STREAMING"

        if "[WARN] Development JWT fallback" in line:
            self._info["warn_jwt"] = True
        if "[WARN] Development master key fallback" in line:
            self._info["warn_master_key"] = True
        if "[WARN] Development PBKDF2 salt fallback" in line:
            self._info["warn_pbkdf2"] = True

        if "Storage system    : [OK] READY" in line:
            self._info["storage_ready"] = True

        m = re.search(r"Files scanned\s+:\s+(\d+)", line)
        if m:
            self._info["files_scanned"] = m.group(1)
        m = re.search(r"Missing metadata\s+:\s+(\d+)", line)
        if m:
            self._info["missing_metadata"] = m.group(1)

        m = re.search(r"Process ID\s+:\s+(\d+)", line)
        if m:
            self._info["jvm_pid"] = int(m.group(1))

        m = re.search(r"Port\s+:\s+(\d+)", line)
        if m:
            self._info["port"] = m.group(1)

        m = re.search(r"Environment\s+:\s+(\S+)", line)
        if m:
            self._info["environment"] = m.group(1)

        if self._info:
            self.startup_info.emit(dict(self._info))

    def _handle_existing_port(self, pid: int | None, proc_name: str | None):
        self.log_line.emit(f"Port 8080 is already occupied (PID: {pid or 'Unknown'}, Process: {proc_name or 'Unknown'})")
        # Probe health endpoint
        for url in ["http://localhost:8080/api/health", "http://127.0.0.1:8080/api/health"]:
            try:
                resp = requests.get(url, timeout=3.0)
                if resp.status_code == 200:
                    data = resp.json()
                    svc = data.get("service", "")
                    if "CipherVault" in svc or data.get("status") == "UP":
                        self.log_line.emit(f"Existing CipherVault backend detected (PID: {pid})")
                        app_logger.log("backend", f"Attached to existing CipherVault backend (PID {pid})")
                        self._pid = pid
                        self._owns_process = False
                        self._info["jvm_pid"] = pid
                        self._info["external"] = True
                        self.startup_info.emit(dict(self._info))
                        self.state_changed.emit(BackendState.ONLINE, f"Attached to existing backend (PID {pid})")
                        return
            except Exception:
                pass

        # Foreign service occupant
        msg = f"Port 8080 is in use by another service (PID: {pid or 'Unknown'}). CipherVault cannot safely bind."
        app_logger.log("error", msg)
        self.log_line.emit(f"[ERROR] {msg}")
        self.state_changed.emit(BackendState.FOREIGN_SERVICE, msg)

    @staticmethod
    def _kill_process_tree(pid: int):
        try:
            parent = psutil.Process(pid)
            children = parent.children(recursive=True)
            for child in children:
                try:
                    child.kill()
                except Exception:
                    pass
            parent.kill()
        except psutil.NoSuchProcess:
            pass
        except Exception:
            pass

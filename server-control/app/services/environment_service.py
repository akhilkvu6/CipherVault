import os
import re
import subprocess
import psutil

def get_project_root() -> str:
    """Resolve repository root dynamically from this file location:
    <root>/server-control/app/services/environment_service.py -> <root>
    """
    services_dir = os.path.dirname(os.path.abspath(__file__))
    app_dir = os.path.dirname(services_dir)
    server_control_dir = os.path.dirname(app_dir)
    return os.path.dirname(server_control_dir)


class EnvironmentService:
    """Pre-checks the environment for running the CipherVault backend."""

    @staticmethod
    def check_java() -> tuple[bool, str]:
        """Returns (available, version_string)."""
        try:
            res = subprocess.run(
                ["java", "-version"],
                capture_output=True,
                text=True,
                creationflags=subprocess.CREATE_NO_WINDOW
            )
            out = res.stderr or res.stdout
            m = re.search(r'version "([^"]+)"', out)
            if m:
                return True, m.group(1)
            # Java 9+ style
            m = re.search(r'java (\d+(\.\d+)*)', out)
            if m:
                return True, m.group(1)
            return True, "Detected"
        except FileNotFoundError:
            return False, "Java is not installed or not in system PATH"
        except Exception as e:
            return False, str(e)

    @staticmethod
    def check_maven_wrapper() -> tuple[bool, str]:
        root = get_project_root()
        mvnw = os.path.join(root, "backend", "mvnw.cmd")
        if os.path.isfile(mvnw):
            return True, mvnw
        return False, f"Maven wrapper not found at {mvnw}"

    @staticmethod
    def check_backend_dir() -> tuple[bool, str]:
        root = get_project_root()
        b_dir = os.path.join(root, "backend")
        pom = os.path.join(b_dir, "pom.xml")
        if os.path.isdir(b_dir) and os.path.isfile(pom):
            return True, b_dir
        return False, f"Backend directory or pom.xml not found at {b_dir}"

    @staticmethod
    def check_port_listener(port: int = 8080) -> tuple[bool, int | None, str | None]:
        """Returns (is_listening, pid, process_name)."""
        try:
            for conn in psutil.net_connections(kind="inet"):
                if conn.laddr.port == port and conn.status == psutil.CONN_LISTEN:
                    pid = conn.pid
                    name = None
                    if pid:
                        try:
                            name = psutil.Process(pid).name()
                        except Exception:
                            pass
                    return True, pid, name
        except Exception:
            pass
        return False, None, None

    @staticmethod
    def check_mysql() -> tuple[bool, str]:
        """Checks if local MySQL port 3306 is listening."""
        try:
            for conn in psutil.net_connections(kind="inet"):
                if conn.laddr.port == 3306 and conn.status == psutil.CONN_LISTEN:
                    return True, "Local MySQL service listening on port 3306"
        except Exception:
            pass
        return False, "Port 3306 not currently active on host"

    @classmethod
    def run_full_precheck(cls) -> dict:
        java_ok, java_ver = cls.check_java()
        mvn_ok, mvn_path = cls.check_maven_wrapper()
        dir_ok, dir_path = cls.check_backend_dir()
        port_occ, port_pid, port_proc = cls.check_port_listener(8080)
        mysql_ok, mysql_msg = cls.check_mysql()

        return {
            "java_ok": java_ok,
            "java_version": java_ver,
            "mvn_ok": mvn_ok,
            "mvn_path": mvn_path,
            "backend_ok": dir_ok,
            "backend_dir": dir_path,
            "port_occupied": port_occ,
            "port_pid": port_pid,
            "port_process": port_proc,
            "mysql_listening": mysql_ok,
            "mysql_detail": mysql_msg
        }

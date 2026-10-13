import socket
import time
import requests
import psutil

class NetworkAdapterInfo:
    def __init__(self, name: str, ip: str, category: str):
        self.name = name
        self.ip = ip
        self.category = category  # "wifi", "ethernet", "other"
        self.url = f"http://{ip}:8080/"

    @property
    def category_title(self) -> str:
        if self.category == "wifi":
            return "Wi-Fi"
        elif self.category == "ethernet":
            return "Ethernet"
        return "Other Network Interface"


class NetworkService:
    """Discovers, classifies, and tests network adapters and backend bindings."""

    @classmethod
    def get_classified_adapters(cls) -> dict:
        """Returns {
            'wifi': [NetworkAdapterInfo, ...],
            'ethernet': [NetworkAdapterInfo, ...],
            'other': [NetworkAdapterInfo, ...]
        }
        Only includes valid IPv4 non-loopback, non-APIPA addresses.
        """
        results = {
            "wifi": [],
            "ethernet": [],
            "other": []
        }

        virtual_keywords = [
            "wsl", "hyper-v", "vethernet", "virtual", "vmware",
            "virtualbox", "bluetooth", "tap", "tun", "vpn", "pseudo"
        ]

        wifi_keywords = ["wi-fi", "wifi", "wlan", "wireless"]
        ethernet_keywords = ["ethernet", "eth", "local area connection"]

        for iface_name, addrs in psutil.net_if_addrs().items():
            lower_name = iface_name.lower()

            for addr in addrs:
                if addr.family == socket.AF_INET:
                    ip = addr.address

                    # Filter loopback and APIPA (auto-assigned 169.254.x.x when disconnected)
                    if ip.startswith("127.") or ip.startswith("169.254."):
                        continue

                    is_virtual = any(k in lower_name for k in virtual_keywords)
                    if is_virtual:
                        results["other"].append(NetworkAdapterInfo(iface_name, ip, "other"))
                    elif any(k in lower_name for k in wifi_keywords):
                        results["wifi"].append(NetworkAdapterInfo(iface_name, ip, "wifi"))
                    elif any(k in lower_name for k in ethernet_keywords):
                        results["ethernet"].append(NetworkAdapterInfo(iface_name, ip, "ethernet"))
                    else:
                        results["other"].append(NetworkAdapterInfo(iface_name, ip, "other"))

        return results

    @classmethod
    def get_preferred_wifi_connection(cls, port: int = 8080) -> tuple[str, int, str, bool]:
        """
        Returns (ip, port, description, is_active):
        - Prioritizes mobile hotspot (192.168.43.x).
        - Then prioritizes any active Wi-Fi adapter.
        - Then falls back to Ethernet / LAN.
        - Finally falls back to 127.0.0.1.
        """
        adapters = cls.get_classified_adapters()

        # 1. Check for specific Android Hotspot IP in any adapter (often 192.168.43.x)
        all_adapters = adapters["wifi"] + adapters["ethernet"] + adapters["other"]
        for adapter in all_adapters:
            if adapter.ip.startswith("192.168.43."):
                return adapter.ip, port, f"Phone Hotspot ({adapter.name})", True

        # 2. Prefer any Wi-Fi adapter
        if adapters["wifi"]:
            adapter = adapters["wifi"][0]
            return adapter.ip, port, f"Wi-Fi Adapter ({adapter.name})", True

        # 3. Fallback to Ethernet
        if adapters["ethernet"]:
            adapter = adapters["ethernet"][0]
            return adapter.ip, port, f"Ethernet ({adapter.name})", True

        # 4. Fallback to other valid LAN interface
        if adapters["other"]:
            adapter = adapters["other"][0]
            return adapter.ip, port, f"Local Interface ({adapter.name})", True

        # 5. Localhost offline fallback
        return "127.0.0.1", port, "Localhost (No Wi-Fi Connected)", False

    @classmethod
    def check_backend_binding(cls, port: int = 8080) -> tuple[str, str]:
        """Returns (binding_type, description):
        - 'all': Bound to 0.0.0.0 or :: (accessible from LAN / Hotspot)
        - 'localhost': Bound to 127.0.0.1 or ::1 only (NOT accessible from phone)
        - 'none': Not currently listening
        """
        try:
            for conn in psutil.net_connections(kind="inet"):
                if conn.laddr.port == port and conn.status == psutil.CONN_LISTEN:
                    addr = conn.laddr.ip
                    if addr in ("0.0.0.0", "::"):
                        return "all", f"Listening on all interfaces ({addr}:{port})"
                    elif addr in ("127.0.0.1", "::1"):
                        return "localhost", f"Bound to localhost only ({addr}:{port})"
                    else:
                        return "specific", f"Bound to specific IP {addr}:{port}"
        except Exception:
            pass
        return "none", "Port not currently listening"

    @classmethod
    def test_endpoint(cls, base_url: str, timeout: float = 3.5) -> tuple[bool, int, float, str]:
        """Tests base_url + 'api/health'.
        Returns (success, http_code, latency_ms, message).
        """
        url = base_url.strip()
        if not url.endswith("/api/health"):
            url = url.rstrip("/") + "/api/health"
        target_url = url

        try:
            t0 = time.monotonic()
            resp = requests.get(target_url, timeout=timeout)
            latency = (time.monotonic() - t0) * 1000

            if resp.status_code == 200:
                try:
                    data = resp.json()
                    status = data.get("status", "UP")
                    return True, 200, latency, f"Online ({status})"
                except Exception:
                    return True, 200, latency, "Online"
            else:
                return False, resp.status_code, latency, f"HTTP {resp.status_code}"

        except requests.exceptions.ConnectionError:
            return False, 0, 0, "Connection refused"
        except requests.exceptions.Timeout:
            return False, -1, 0, "Request timed out"
        except Exception as e:
            return False, 0, 0, str(e)

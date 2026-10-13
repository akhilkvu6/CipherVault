"""CipherVault Backend State Machine Specification.

Authoritative state definitions and helper methods for backend lifecycle management.
"""


class BackendState:
    """Authoritative states for CipherVault backend process and network lifecycle."""

    STOPPED = "STOPPED"
    STARTING = "STARTING"
    RUNNING = "RUNNING"
    ONLINE = "ONLINE"                # Semantic synonym for RUNNING
    STOPPING = "STOPPING"
    RESTARTING = "RESTARTING"
    OFFLINE = "OFFLINE"              # Semantic synonym for STOPPED
    ERROR = "ERROR"
    FOREIGN_SERVICE = "FOREIGN_SERVICE"
    UNKNOWN = "UNKNOWN"

    ALL_STATES = {
        STOPPED,
        STARTING,
        RUNNING,
        ONLINE,
        STOPPING,
        RESTARTING,
        OFFLINE,
        ERROR,
        FOREIGN_SERVICE,
        UNKNOWN
    }

    @classmethod
    def is_active(cls, state: str) -> bool:
        """Returns True if backend is active and healthy."""
        return state in (cls.RUNNING, cls.ONLINE)

    @classmethod
    def is_inactive(cls, state: str) -> bool:
        """Returns True if backend is stopped or offline."""
        return state in (cls.STOPPED, cls.OFFLINE)

    @classmethod
    def is_transitional(cls, state: str) -> bool:
        """Returns True if backend is transitioning between states."""
        return state in (cls.STARTING, cls.STOPPING, cls.RESTARTING)

    @classmethod
    def is_error(cls, state: str) -> bool:
        """Returns True if backend encountered a failure or conflict."""
        return state in (cls.ERROR, cls.FOREIGN_SERVICE)

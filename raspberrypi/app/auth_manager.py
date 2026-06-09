import aiohttp
import logging

logger = logging.getLogger(__name__)

class AuthManager:
    """Manages JWT bearer-token authentication against the webapp."""

    def __init__(self, server_url: str, username: str, password: str):
        self.server_url = server_url
        self.username = username
        self.password = password
        self.token: str | None = None

    async def login(self):
        """POST credentials to /api/login and store the returned bearer token."""
        url = f"{self.server_url}/api/login"
        timeout = aiohttp.ClientTimeout(total=10)

        async with aiohttp.ClientSession(timeout=timeout) as session:
            async with session.post(url, json={
                "username": self.username,
                "password": self.password
                }) as response:
                response.raise_for_status()
                data = await response.json()
                self.token = data["bearerToken"]
                logger.info("[Auth] Successfully authenticated with webapp.")

    def get_headers(self) -> dict:
        """Return Authorization + Content-Type headers for an authenticated request."""
        if not self.token:
            raise RuntimeError("Not authenticated. Call login() first.")
        return {
                "Authorization": f"Bearer {self.token}",
                "Content-Type": "application/json"
                }

    async def refresh_if_needed(self):
        """Re-authenticate when a 401 response is received."""
        logger.warning("[Auth] Token expired or invalid, re-authenticating...")
        await self.login()

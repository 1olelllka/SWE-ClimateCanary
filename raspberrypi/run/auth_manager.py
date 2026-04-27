import aiohttp
import asyncio
import logging

logger = logging.getLogger(__name__)

class AuthManager:
    def __init__(self, server_url: str, username: str, password: str):
        self.server_url = server_url
        self.username = username
        self.password = password
        self.token: str | None = None

    async def login(self):
        """Log in and store the JWT token in memory."""
        url = f"{self.server_url}/api/auth/login"
        timeout = aiohttp.ClientTimeout(total=10)

        async with aiohttp.ClientSession(timeout=timeout) as session:
            async with session.post(url, json={
                "username": self.username,
                "password": self.password
            }) as response:
                response.raise_for_status()
                data = await response.json()
                self.token = data["token"]
                logger.info("[Auth] Successfully authenticated with webapp.")

    def get_headers(self) -> dict:
        """Returns auth headers for any request to the webapp."""
        if not self.token:
            raise RuntimeError("Not authenticated. Call login() first.")
        return {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json"
        }

    async def refresh_if_needed(self):
        """If a request returns 401 — re-login and retry."""
        logger.warning("[Auth] Token expired or invalid, re-authenticating...")
        await self.login()

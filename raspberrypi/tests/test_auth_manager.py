import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from aiohttp import ClientResponseError
from app.auth_manager import AuthManager


@pytest.fixture
def auth():
    return AuthManager(
        server_url="http://test-server:8080",
        username="test-user",
        password="test-password",
    )


class TestInit:
    """AuthManager initialises with correct defaults."""

    def test_initial_token_is_none(self, auth):
        assert auth.token is None

    def test_stores_credentials(self, auth):
        assert auth.server_url == "http://test-server:8080"
        assert auth.username == "test-user"
        assert auth.password == "test-password"


class TestGetHeaders:
    """get_headers() builds the Authorization header or raises when unauthenticated."""

    def test_returns_auth_header_when_token_set(self, auth):
        auth.token = "my-token"
        headers = auth.get_headers()
        assert headers["Authorization"] == "Bearer my-token"
        assert headers["Content-Type"] == "application/json"

    def test_raises_when_no_token(self, auth):
        with pytest.raises(RuntimeError, match="Not authenticated"):
            auth.get_headers()


class TestLogin:
    """login() POSTs credentials and stores the bearer token."""

    def _make_session(self, response_json=None, raise_for_status_effect=None):
        """Helper to build a fake aiohttp session."""
        mock_response = MagicMock()
        mock_response.raise_for_status = MagicMock(side_effect=raise_for_status_effect)
        mock_response.json = AsyncMock(return_value=response_json or {})
        mock_response.__aenter__ = AsyncMock(return_value=mock_response)
        mock_response.__aexit__ = AsyncMock(return_value=False)

        mock_session = MagicMock()
        mock_session.post = MagicMock(return_value=mock_response)  
        mock_session.__aenter__ = AsyncMock(return_value=mock_session)
        mock_session.__aexit__ = AsyncMock(return_value=False)

        return mock_session, mock_response

    async def test_sets_token_on_success(self, auth):
        mock_session, _ = self._make_session(response_json={"bearerToken": "abc123"})
        with patch("app.auth_manager.aiohttp.ClientSession", return_value=mock_session):
            await auth.login()
        assert auth.token == "abc123"

    async def test_posts_to_correct_url(self, auth):
        mock_session, _ = self._make_session(response_json={"bearerToken": "tok"})
        with patch("app.auth_manager.aiohttp.ClientSession", return_value=mock_session):
            await auth.login()
        mock_session.post.assert_called_once_with(
            "http://test-server:8080/api/login",
            json={"username": "test-user", "password": "test-password"},
        )

    async def test_raises_on_bad_status(self, auth):
        mock_session, _ = self._make_session(
            raise_for_status_effect=ClientResponseError(
                request_info=MagicMock(), history=(), status=401
            )
        )
        with patch("app.auth_manager.aiohttp.ClientSession", return_value=mock_session):
            with pytest.raises(ClientResponseError):
                await auth.login()
        assert auth.token is None

    async def test_token_overwritten_on_second_login(self, auth):
        auth.token = "old-token"
        mock_session, _ = self._make_session(response_json={"bearerToken": "new-token"})
        with patch("app.auth_manager.aiohttp.ClientSession", return_value=mock_session):
            await auth.login()
        assert auth.token == "new-token"


class TestRefreshIfNeeded:
    """refresh_if_needed() always delegates to login()."""

    async def test_calls_login(self, auth):
        auth.login = AsyncMock()
        await auth.refresh_if_needed()
        auth.login.assert_awaited_once()

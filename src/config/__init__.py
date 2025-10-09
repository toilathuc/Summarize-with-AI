"""
Configuration package for project-wide settings.

This module exposes a `settings` instance that centralises access to
environment-driven configuration values (API keys, URLs, paths, etc.).
"""

from .settings import Settings, settings

__all__ = ["Settings", "settings"]

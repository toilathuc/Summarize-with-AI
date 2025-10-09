"""
Batching utilities.
"""

from __future__ import annotations

from typing import Iterable, Iterator, Sequence, TypeVar

T = TypeVar("T")


def chunked(seq: Sequence[T], size: int) -> Iterator[Sequence[T]]:
    """Yield fixed-size chunks from a sequence."""
    if size <= 0:
        raise ValueError("Chunk size must be greater than zero.")
    for start in range(0, len(seq), size):
        yield seq[start : start + size]


export function initKeyboardShortcuts(searchInput, onClear) {
  document.addEventListener("keydown", (event) => {
    if (!searchInput) {
      return;
    }

    // Ctrl/Cmd + F focuses the search input without browser find overlay
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "f") {
      event.preventDefault();
      searchInput.focus();
    }

    // Escape clears the search field
    if (event.key === "Escape") {
      if (searchInput.value) {
        searchInput.value = "";
        onClear?.();
      }
      searchInput.blur();
    }
  });
}

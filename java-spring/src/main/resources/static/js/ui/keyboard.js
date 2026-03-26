export function initKeyboardShortcuts(searchInput, onClear) {
  document.addEventListener("keydown", (event) => {
    if (!searchInput) {
      return;
    }

    
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "f") {
      event.preventDefault();
      searchInput.focus();
    }

    
    if (event.key === "Escape") {
      if (searchInput.value) {
        searchInput.value = "";
        onClear?.();
      }
      searchInput.blur();
    }
  });
}

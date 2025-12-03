import { useEffect } from "react";

export function useKeyboardShortcuts(searchRef, onClear) {
  useEffect(() => {
    function handle(event) {
      const input = searchRef?.current;
      if (!input) return;

      // Ctrl/Cmd + F
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "f") {
        event.preventDefault();
        input.focus();
      }

      // Escape
      if (event.key === "Escape") {
        if (input.value) {
          input.value = "";
          onClear?.();
        }
        input.blur();
      }
    }

    document.addEventListener("keydown", handle);
    return () => document.removeEventListener("keydown", handle);
  }, [searchRef, onClear]);
}

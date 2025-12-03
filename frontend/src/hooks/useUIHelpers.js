import { useState } from "react";

export function useUIHelpers() {
  const [overlay, setOverlay] = useState(false);
  const [error, setError] = useState(null);

  function showOverlay(message = null) {
    setOverlay(message || "Đang tải...");
  }

  function hideOverlay() {
    setOverlay(false);
  }

  function showError(message) {
    setError(message);
  }

  function clearError() {
    setError(null);
  }

  return {
    overlay,
    error,
    showOverlay,
    hideOverlay,
    showError,
    clearError,
  };
}

import { useState } from "react";

export function useNotification() {
  const [notification, setNotification] = useState(null);

  function show(message, type = "info") {
    setNotification({ message, type });

    setTimeout(() => setNotification(null), 3000);
  }

  return {
    notification,
    showSuccess: (msg) => show(msg, "success"),
    showError: (msg) => show(msg, "error"),
    showInfo: (msg) => show(msg, "info"),
  };
}

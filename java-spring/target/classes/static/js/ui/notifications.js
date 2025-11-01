const NOTIFICATION_STYLE_ID = "notification-style";

export function showNotification(message, type = "info") {
  removeExistingNotification();
  ensureStyles();

  const notification = document.createElement("div");
  notification.className = `notification notification-${type}`;
  notification.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    background: ${getBackground(type)};
    color: white;
    padding: 1rem 1.5rem;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    z-index: 1000;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    min-width: 300px;
    animation: slideIn 0.3s ease-out;
  `;

  notification.innerHTML = `
    <i class="fas ${getIcon(type)}"></i>
    <span>${message}</span>
    <button class="notification-close" type="button">
      <i class="fas fa-times"></i>
    </button>
  `;

  notification
    .querySelector(".notification-close")
    .addEventListener("click", () => {
      notification.remove();
    });

  document.body.appendChild(notification);

  setTimeout(() => {
    if (notification.parentElement) {
      notification.remove();
    }
  }, 3000);
}

export function showRefreshSuccess(message) {
  showNotification(message, "success");
}

export function showRefreshError(message) {
  showNotification(message, "error");
}

function removeExistingNotification() {
  const existing = document.querySelector(".notification");
  if (existing) {
    existing.remove();
  }
}

function ensureStyles() {
  if (document.getElementById(NOTIFICATION_STYLE_ID)) {
    return;
  }

  const style = document.createElement("style");
  style.id = NOTIFICATION_STYLE_ID;
  style.textContent = `
    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }
    .notification-close {
      background: none;
      border: none;
      color: white;
      cursor: pointer;
      padding: 0.25rem;
      margin-left: auto;
    }
  `;

  document.head.appendChild(style);
}

function getBackground(type) {
  switch (type) {
    case "success":
      return "#10b981";
    case "error":
      return "#ef4444";
    default:
      return "#3b82f6";
  }
}

function getIcon(type) {
  switch (type) {
    case "success":
      return "fa-check-circle";
    case "error":
      return "fa-exclamation-circle";
    default:
      return "fa-info-circle";
  }
}

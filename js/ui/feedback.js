import { escapeHtml } from "../utils/html.js";

export function showLoading(loadingElement, container, show) {
  if (loadingElement) {
    loadingElement.style.display = show ? "block" : "none";
  }
  if (container) {
    container.style.display = show ? "none" : "block";
  }
}

export function showNoResults(noResultsElement, container, show) {
  if (noResultsElement) {
    noResultsElement.style.display = show ? "block" : "none";
  }
  if (container) {
    container.style.display = show ? "none" : "block";
  }
}

export function showLoadingOverlay(overlayElement, show, customMessage = null) {
  if (!overlayElement) {
    return;
  }

  const body = document.body;

  if (show) {
    overlayElement.classList.add("show");
    if (body) {
      body.style.overflow = "hidden";
    }

    const loadingTextElement = overlayElement.querySelector(".loading-text");
    if (loadingTextElement) {
      const message =
        customMessage || getRandomLoadingMessage();
      loadingTextElement.textContent = message;
    }
  } else {
    overlayElement.classList.remove("show");
    if (body) {
      body.style.overflow = "";
    }
  }
}

export function showError(container, message) {
  if (!container) {
    return;
  }

  container.innerHTML = `
    <div class="error-message" style="text-align: center; padding: 2rem; color: #e53e3e;">
      <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 1rem;"></i>
      <h3>Đã xảy ra lỗi</h3>
      <p>${escapeHtml(message)}</p>
      <button
        type="button"
        style="margin-top: 1rem; padding: 0.5rem 1rem; background: #667eea; color: white; border: none; border-radius: 5px; cursor: pointer;"
        onclick="location.reload()"
      >
        Tải lại trang
      </button>
    </div>
  `;
}

function getRandomLoadingMessage() {
  const loadingTexts = [
    "Đang lấy tin tức mới nhất...",
    "Đang cập nhật dữ liệu...",
    "Đang tải nội dung...",
    "Đang xử lý thông tin...",
  ];
  const randomIndex = Math.floor(Math.random() * loadingTexts.length);
  return loadingTexts[randomIndex];
}

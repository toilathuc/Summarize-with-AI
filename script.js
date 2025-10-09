// Global variables
let newsData = [];
let filteredData = [];

// DOM elements
const newsContainer = document.getElementById("news-container");
const loadingElement = document.getElementById("loading");
const noResultsElement = document.getElementById("no-results");
const searchInput = document.getElementById("search-input");
const typeFilter = document.getElementById("type-filter");
const totalItemsElement = document.getElementById("total-items");
const lastUpdatedElement = document.getElementById("last-updated");
const refreshBtn = document.getElementById("refresh-btn");
const loadingOverlay = document.getElementById("loading-overlay");

// Initialize the application
document.addEventListener("DOMContentLoaded", function () {
  loadNewsData();
  setupEventListeners();
  updateLastUpdated();
});

// Load news data from summaries.json
async function loadNewsData() {
  try {
    showLoading(true);
    const response = await fetch("./summaries.json");
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const data = await response.json();
    newsData = data.items || [];
    filteredData = [...newsData];

    renderNews();
    updateStats();
    showLoading(false);
  } catch (error) {
    console.error("Error loading news data:", error);
    showError("Không thể tải dữ liệu tin tức. Vui lòng thử lại sau.");
    showLoading(false);
  }
}

// Setup event listeners
function setupEventListeners() {
  searchInput.addEventListener("input", debounce(handleSearch, 300));
  typeFilter.addEventListener("change", handleFilter);
  refreshBtn.addEventListener("click", handleRefresh);
}

// Handle search functionality
function handleSearch() {
  const searchTerm = searchInput.value.toLowerCase().trim();
  applyFilters(searchTerm, typeFilter.value);
}

// Handle type filter
function handleFilter() {
  const searchTerm = searchInput.value.toLowerCase().trim();
  applyFilters(searchTerm, typeFilter.value);
}

// Handle refresh button
async function handleRefresh() {
  const icon = refreshBtn.querySelector(".refresh-icon");

  // Show loading overlay with specific message
  showLoadingOverlay(true, "Đang khởi tạo cập nhật dữ liệu...");

  // Set button loading state
  refreshBtn.classList.add("loading");
  refreshBtn.disabled = true;

  try {
    // Start the actual data refresh
    const startTime = Date.now();

    // Call the refresh API
    console.log("Starting data refresh...");
    showLoadingOverlay(true, "Đang kết nối với server...");

    const refreshResponse = await fetch("/api/refresh");
    const refreshResult = await refreshResponse.json();

    if (!refreshResponse.ok) {
      throw new Error(refreshResult.message || "Failed to start refresh");
    }

    console.log("Refresh started:", refreshResult);
    showLoadingOverlay(true, "Đang lấy dữ liệu mới từ Techmeme...");

    // Poll for completion
    let completed = false;
    let success = false;
    let attempts = 0;
    const maxAttempts = 60; // 5 minutes max

    while (!completed && attempts < maxAttempts) {
      await new Promise((resolve) => setTimeout(resolve, 5000)); // Wait 5 seconds

      try {
        const statusResponse = await fetch("/api/refresh/status");
        const status = await statusResponse.json();

        console.log("Refresh status:", status);

        // Update progress message
        if (attempts < 10) {
          showLoadingOverlay(true, "Đang xử lý dữ liệu với AI...");
        } else if (attempts < 20) {
          showLoadingOverlay(true, "Đang tạo tóm tắt nội dung...");
        } else {
          showLoadingOverlay(true, "Đang hoàn thiện dữ liệu...");
        }

        completed = status.completed;
        success = status.success;

        attempts++;
      } catch (e) {
        console.error("Error checking status:", e);
        attempts++;
      }
    }

    if (!completed) {
      throw new Error("Refresh timeout - taking too long");
    }

    if (!success) {
      throw new Error("Refresh failed on server");
    }

    // Now fetch the updated data
    showLoadingOverlay(true, "Đang tải dữ liệu đã cập nhật...");
    const timestamp = new Date().getTime();
    const response = await fetch(`./summaries.json?t=${timestamp}`);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();

    // Ensure minimum loading time of 2 seconds for visual feedback
    const elapsedTime = Date.now() - startTime;
    const minLoadTime = 2000;
    if (elapsedTime < minLoadTime) {
      showLoadingOverlay(true, "Đang hoàn tất...");
      await new Promise((resolve) =>
        setTimeout(resolve, minLoadTime - elapsedTime)
      );
    }

    // Check if data is actually new
    const newDataLength = data.items?.length || 0;
    const currentDataLength = newsData.length;
    const lastUpdated = data.last_updated;

    newsData = data.items || [];
    filteredData = [...newsData];

    renderNews();
    updateStats();
    updateLastUpdated();

    // Button success state
    refreshBtn.classList.remove("loading");
    refreshBtn.classList.add("success");
    icon.className = "fas fa-check";

    // Hide loading overlay
    showLoadingOverlay(false);

    // Show success message
    const message = `Dữ liệu đã được cập nhật thành công! Tìm thấy ${newDataLength} bài viết`;

    showRefreshSuccess(message);

    // Reset button after 3 seconds
    setTimeout(() => {
      refreshBtn.classList.remove("success");
      icon.className = "fas fa-sync-alt refresh-icon";
      refreshBtn.disabled = false;
    }, 3000);
  } catch (error) {
    console.error("Error refreshing data:", error);

    // Hide loading overlay
    showLoadingOverlay(false);

    // Button error state
    refreshBtn.classList.remove("loading");
    refreshBtn.classList.add("error");
    icon.className = "fas fa-exclamation-triangle";

    // Show specific error message
    let errorMessage = "Không thể cập nhật dữ liệu. ";
    if (error.message.includes("timeout")) {
      errorMessage += "Quá trình cập nhật mất quá nhiều thời gian.";
    } else if (error.message.includes("Failed to start")) {
      errorMessage += "Không thể khởi tạo quá trình cập nhật.";
    } else if (error.message.includes("failed on server")) {
      errorMessage += "Lỗi xử lý dữ liệu trên server.";
    } else {
      errorMessage += "Vui lòng thử lại sau.";
    }

    showRefreshError(errorMessage);

    // Reset button after 4 seconds
    setTimeout(() => {
      refreshBtn.classList.remove("error");
      icon.className = "fas fa-sync-alt refresh-icon";
      refreshBtn.disabled = false;
    }, 4000);
  }
}

// Apply search and filter
function applyFilters(searchTerm, typeValue) {
  filteredData = newsData.filter((item) => {
    const matchesSearch =
      !searchTerm ||
      item.title.toLowerCase().includes(searchTerm) ||
      item.bullets.some((bullet) =>
        bullet.toLowerCase().includes(searchTerm)
      ) ||
      item.why_it_matters.toLowerCase().includes(searchTerm);

    const matchesType = !typeValue || item.type === typeValue;

    return matchesSearch && matchesType;
  });

  renderNews();
  updateStats();
}

// Render news items
function renderNews() {
  if (filteredData.length === 0) {
    showNoResults(true);
    return;
  }

  showNoResults(false);

  const newsHTML = filteredData
    .map((item) => createNewsItemHTML(item))
    .join("");
  newsContainer.innerHTML = newsHTML;

  // Add click handlers for news items
  const newsItems = document.querySelectorAll(".news-item");
  newsItems.forEach((item, index) => {
    item.addEventListener("click", () => {
      const url = filteredData[index].url;
      if (url) {
        window.open(url, "_blank", "noopener,noreferrer");
      }
    });
  });
}

// Create HTML for a single news item
function createNewsItemHTML(item) {
  const typeIcon = getTypeIcon(item.type);
  const typeColor = getTypeColor(item.type);

  return `
        <article class="news-item" data-type="${
          item.type
        }" style="--type-color: ${typeColor}">
            <div class="news-header">
                <h2 class="news-title">${escapeHtml(item.title)}</h2>
                <span class="news-type">
                    <i class="${typeIcon}"></i>
                    ${getTypeLabel(item.type)}
                </span>
            </div>
            
            <div class="news-bullets">
                <ul>
                    ${item.bullets
                      .map((bullet) => `<li>${escapeHtml(bullet)}</li>`)
                      .join("")}
                </ul>
            </div>
            
            ${
              item.why_it_matters
                ? `
                <div class="why-matters">
                    <div class="why-matters-title">Tại sao quan trọng?</div>
                    <div class="why-matters-text">${escapeHtml(
                      item.why_it_matters
                    )}</div>
                </div>
            `
                : ""
            }
            
            <div class="news-footer">
                <a href="${
                  item.url
                }" class="read-more" target="_blank" rel="noopener noreferrer" onclick="event.stopPropagation()">
                    Đọc bài gốc <i class="fas fa-external-link-alt"></i>
                </a>
            </div>
        </article>
    `;
}

// Get icon for news type
function getTypeIcon(type) {
  const icons = {
    news: "fas fa-newspaper",
    announcement: "fas fa-bullhorn",
    video: "fas fa-play-circle",
    howto: "fas fa-question-circle",
    troubleshooting: "fas fa-tools",
  };
  return icons[type] || "fas fa-file-text";
}

// Get color for news type
function getTypeColor(type) {
  const colors = {
    news: "#3b82f6",
    announcement: "#10b981",
    video: "#f59e0b",
    howto: "#8b5cf6",
    troubleshooting: "#ef4444",
  };
  return colors[type] || "#667eea";
}

// Get label for news type
function getTypeLabel(type) {
  const labels = {
    news: "Tin tức",
    announcement: "Thông báo",
    video: "Video",
    howto: "Hướng dẫn",
    troubleshooting: "Khắc phục",
  };
  return labels[type] || "Khác";
}

// Update statistics
function updateStats() {
  const total = filteredData.length;
  const totalText = total === 1 ? "1 bài viết" : `${total} bài viết`;
  totalItemsElement.textContent = totalText;
}

// Update last updated time
function updateLastUpdated() {
  const now = new Date();
  const timeString = now.toLocaleString("vi-VN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
  lastUpdatedElement.textContent = `Cập nhật: ${timeString}`;
}

// Show/hide loading state
function showLoading(show) {
  loadingElement.style.display = show ? "block" : "none";
  newsContainer.style.display = show ? "none" : "block";
}

// Show/hide no results state
function showNoResults(show) {
  noResultsElement.style.display = show ? "block" : "none";
  newsContainer.style.display = show ? "none" : "block";
}

// Show/hide loading overlay
function showLoadingOverlay(show, customMessage = null) {
  if (show) {
    loadingOverlay.classList.add("show");
    document.body.style.overflow = "hidden"; // Prevent scrolling

    // Update loading text
    let message;
    if (customMessage) {
      message = customMessage;
    } else {
      const loadingTexts = [
        "Đang lấy tin tức mới nhất...",
        "Đang cập nhật dữ liệu...",
        "Đang tải nội dung...",
        "Đang xử lý thông tin...",
      ];
      message = loadingTexts[Math.floor(Math.random() * loadingTexts.length)];
    }

    const loadingText = loadingOverlay.querySelector(".loading-text");
    if (loadingText) {
      loadingText.textContent = message;
    }
  } else {
    loadingOverlay.classList.remove("show");
    document.body.style.overflow = ""; // Restore scrolling
  }
}

// Show error message
function showError(message) {
  newsContainer.innerHTML = `
        <div class="error-message" style="text-align: center; padding: 2rem; color: #e53e3e;">
            <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 1rem;"></i>
            <h3>Đã xảy ra lỗi</h3>
            <p>${escapeHtml(message)}</p>
            <button onclick="location.reload()" style="margin-top: 1rem; padding: 0.5rem 1rem; background: #667eea; color: white; border: none; border-radius: 5px; cursor: pointer;">
                Tải lại trang
            </button>
        </div>
    `;
}

// Utility functions
function escapeHtml(text) {
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func.apply(this, args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

// Keyboard shortcuts
document.addEventListener("keydown", function (e) {
  // Focus search on Ctrl+F
  if (e.ctrlKey && e.key === "f") {
    e.preventDefault();
    searchInput.focus();
  }

  // Clear search on Escape
  if (e.key === "Escape") {
    searchInput.value = "";
    handleSearch();
    searchInput.blur();
  }
});

// Add smooth scroll to top functionality
function scrollToTop() {
  window.scrollTo({
    top: 0,
    behavior: "smooth",
  });
}

// Add scroll to top button if page is long enough
window.addEventListener("scroll", function () {
  if (window.scrollY > 500) {
    if (!document.querySelector(".scroll-to-top")) {
      const scrollButton = document.createElement("button");
      scrollButton.className = "scroll-to-top";
      scrollButton.innerHTML = '<i class="fas fa-chevron-up"></i>';
      scrollButton.style.cssText = `
                position: fixed;
                bottom: 2rem;
                right: 2rem;
                width: 50px;
                height: 50px;
                border-radius: 50%;
                background: #667eea;
                color: white;
                border: none;
                cursor: pointer;
                box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                z-index: 1000;
                transition: all 0.3s ease;
            `;
      scrollButton.addEventListener("click", scrollToTop);
      document.body.appendChild(scrollButton);
    }
  } else {
    const scrollButton = document.querySelector(".scroll-to-top");
    if (scrollButton) {
      scrollButton.remove();
    }
  }
});

// Show refresh success message
function showRefreshSuccess(message) {
  showNotification(message, "success");
}

// Show refresh error message
function showRefreshError(message) {
  showNotification(message, "error");
}

// Generic notification function
function showNotification(message, type = "info") {
  // Remove existing notifications
  const existing = document.querySelector(".notification");
  if (existing) {
    existing.remove();
  }

  const notification = document.createElement("div");
  notification.className = `notification notification-${type}`;
  notification.innerHTML = `
    <i class="fas ${
      type === "success"
        ? "fa-check-circle"
        : type === "error"
        ? "fa-exclamation-circle"
        : "fa-info-circle"
    }"></i>
    <span>${message}</span>
    <button class="notification-close" onclick="this.parentElement.remove()">
      <i class="fas fa-times"></i>
    </button>
  `;

  // Add styles
  notification.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    background: ${
      type === "success" ? "#10b981" : type === "error" ? "#ef4444" : "#3b82f6"
    };
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

  // Add animation styles
  const style = document.createElement("style");
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

  document.body.appendChild(notification);

  // Auto remove after 3 seconds
  setTimeout(() => {
    if (notification.parentElement) {
      notification.remove();
    }
  }, 3000);
}

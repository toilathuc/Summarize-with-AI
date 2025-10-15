import { fetchNewsData, refreshNews } from "./services/newsService.js";
import { applyFilters } from "./filters.js";
import { renderNews } from "./ui/render.js";
import { updateStats, updateLastUpdated } from "./ui/stats.js";
import {
  showLoading,
  showNoResults,
  showLoadingOverlay,
  showError,
} from "./ui/feedback.js";
import {
  showRefreshSuccess,
  showRefreshError,
} from "./ui/notifications.js";
import { debounce } from "./utils/debounce.js";
import { initScrollToTop } from "./ui/scroll.js";
import { initKeyboardShortcuts } from "./ui/keyboard.js";

const state = {
  newsData: [],
  filteredData: [],
  lastUpdated: null,
};

document.addEventListener("DOMContentLoaded", () => {
  const elements = getDomElements();

  initScrollToTop();
  initKeyboardShortcuts(elements.searchInput, () =>
    handleSearch(elements),
  );

  setupEventListeners(elements);
  bootstrap(elements);
});

function getDomElements() {
  return {
    newsContainer: document.getElementById("news-container"),
    loadingElement: document.getElementById("loading"),
    noResultsElement: document.getElementById("no-results"),
    searchInput: document.getElementById("search-input"),
    typeFilter: document.getElementById("type-filter"),
    totalItemsElement: document.getElementById("total-items"),
    lastUpdatedElement: document.getElementById("last-updated"),
    refreshBtn: document.getElementById("refresh-btn"),
    loadingOverlay: document.getElementById("loading-overlay"),
  };
}

function setupEventListeners(elements) {
  if (elements.searchInput) {
    elements.searchInput.addEventListener(
      "input",
      debounce(() => handleSearch(elements), 300),
    );
  }

  if (elements.typeFilter) {
    elements.typeFilter.addEventListener("change", () =>
      handleSearch(elements),
    );
  }

  if (elements.refreshBtn) {
    elements.refreshBtn.addEventListener("click", () =>
      handleRefresh(elements),
    );
  }
}

async function bootstrap(elements) {
  try {
    showLoading(elements.loadingElement, elements.newsContainer, true);

    const { items, lastUpdated } = await fetchNewsData();

    state.newsData = items;
    state.lastUpdated = lastUpdated;

    const initialSearch = elements.searchInput?.value || "";
    const initialType = elements.typeFilter?.value || "";
    state.filteredData = applyFilters(
      state.newsData,
      initialSearch,
      initialType,
    );

    renderCurrentData(elements);
    updateLastUpdated(elements.lastUpdatedElement, state.lastUpdated);
  } catch (error) {
    console.error("Error loading news data:", error);
    showError(
      elements.newsContainer,
      "Không thể tải dữ liệu tin tức. Vui lòng thử lại sau.",
    );
  } finally {
    showLoading(elements.loadingElement, elements.newsContainer, false);
  }
}

function handleSearch(elements) {
  const searchTerm = elements.searchInput?.value || "";
  const typeValue = elements.typeFilter?.value || "";

  state.filteredData = applyFilters(
    state.newsData,
    searchTerm,
    typeValue,
  );

  renderCurrentData(elements);
}

async function handleRefresh(elements) {
  const { refreshBtn, loadingOverlay } = elements;
  if (!refreshBtn) {
    return;
  }

  const icon = refreshBtn.querySelector(".refresh-icon");

  showLoadingOverlay(
    loadingOverlay,
    true,
    "Đang khởi tạo cập nhật dữ liệu...",
  );

  refreshBtn.classList.add("loading");
  refreshBtn.disabled = true;

  try {
    const { data } = await refreshNews({
      onProgress: (message) =>
        showLoadingOverlay(loadingOverlay, true, message),
    });

    state.newsData = data.items;
    state.lastUpdated = data.lastUpdated;

    const searchTerm = elements.searchInput?.value || "";
    const typeValue = elements.typeFilter?.value || "";
    state.filteredData = applyFilters(
      state.newsData,
      searchTerm,
      typeValue,
    );

    renderCurrentData(elements);
    updateLastUpdated(elements.lastUpdatedElement, state.lastUpdated);

    showLoadingOverlay(loadingOverlay, false);

    refreshBtn.classList.remove("loading");
    refreshBtn.classList.add("success");
    if (icon) {
      icon.className = "fas fa-check";
    }

    const totalItems = state.newsData.length;
    showRefreshSuccess(
      `Dữ liệu đã được cập nhật thành công! Tìm thấy ${totalItems} bài viết`,
    );

    setTimeout(() => {
      refreshBtn.classList.remove("success");
      if (icon) {
        icon.className = "fas fa-sync-alt refresh-icon";
      }
      refreshBtn.disabled = false;
    }, 3000);
  } catch (error) {
    console.error("Error refreshing data:", error);
    showLoadingOverlay(loadingOverlay, false);

    refreshBtn.classList.remove("loading");
    refreshBtn.classList.add("error");
    if (icon) {
      icon.className = "fas fa-exclamation-triangle";
    }

    let errorMessage = "Không thể cập nhật dữ liệu. ";
    const message = (error && error.message) || "";

    if (message.includes("timeout")) {
      errorMessage += "Quá trình cập nhật mất quá nhiều thời gian.";
    } else if (message.includes("Failed to start")) {
      errorMessage += "Không thể khởi tạo quá trình cập nhật.";
    } else if (message.includes("failed on server")) {
      errorMessage += "Lỗi xử lý dữ liệu trên server.";
    } else {
      errorMessage += "Vui lòng thử lại sau.";
    }

    if (error.detail) {
      errorMessage += ` (${error.detail})`;
    }

    showRefreshError(errorMessage);

    setTimeout(() => {
      refreshBtn.classList.remove("error");
      if (icon) {
        icon.className = "fas fa-sync-alt refresh-icon";
      }
      refreshBtn.disabled = false;
    }, 4000);
  }
}

function renderCurrentData(elements) {
  if (state.filteredData.length === 0) {
    renderNews(elements.newsContainer, []);
    showNoResults(elements.noResultsElement, elements.newsContainer, true);
  } else {
    showNoResults(elements.noResultsElement, elements.newsContainer, false);
    renderNews(elements.newsContainer, state.filteredData);
  }

  updateStats(elements.totalItemsElement, state.filteredData.length);
}

import { fetchNewsData, refreshNewsFast } from "./services/newsService.js";
import { applyFilters } from "./filters.js";
import { renderNews } from "./ui/render.js";
import { updateStats, updateLastUpdated } from "./ui/stats.js";
import {
  showLoading,
  showNoResults,
  showLoadingOverlay,
  showError,
} from "./ui/feedback.js";
import { showRefreshSuccess, showRefreshError } from "./ui/notifications.js";
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
  initKeyboardShortcuts(elements.searchInput, () => handleSearch(elements));

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
      debounce(() => handleSearch(elements), 300)
    );
  }

  if (elements.typeFilter) {
    elements.typeFilter.addEventListener("change", () =>
      handleSearch(elements)
    );
  }

  if (elements.refreshBtn) {
    elements.refreshBtn.addEventListener("click", () =>
      handleRefresh(elements)
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
      initialType
    );

    renderCurrentData(elements);
    updateLastUpdated(elements.lastUpdatedElement, state.lastUpdated);
  } catch (error) {
    console.error("Error loading news data:", error);
    showError(
      elements.newsContainer,
      "Unable to load news data. Please try again later."
    );
  } finally {
    showLoading(elements.loadingElement, elements.newsContainer, false);
  }
}

function handleSearch(elements) {
  const searchTerm = elements.searchInput?.value || "";
  const typeValue = elements.typeFilter?.value || "";

  state.filteredData = applyFilters(state.newsData, searchTerm, typeValue);

  renderCurrentData(elements);
}

async function handleRefresh(elements) {
  const { refreshBtn, loadingOverlay } = elements;
  if (!refreshBtn) {
    console.error("Refresh button not found");
    return;
  }

  const icon = refreshBtn.querySelector(".refresh-icon");

  // Prevent multiple simultaneous refreshes
  if (refreshBtn.disabled) {
    console.warn("Refresh already in progress");
    return;
  }

  showLoadingOverlay(loadingOverlay, true, "Loading new data...");

  refreshBtn.classList.add("loading");
  refreshBtn.disabled = true;

  try {
    console.log("Starting fast refresh...");

    // Use fast refresh by default
    const data = await refreshNewsFast();

    console.log("Fast refresh completed:", {
      items: data.items?.length,
      freshness: data.freshness,
      isStale: data.isStale,
    });

    // Update state
    state.newsData = data.items || [];
    state.lastUpdated = data.lastUpdated;

    // Apply current filters
    const searchTerm = elements.searchInput?.value || "";
    const typeValue = elements.typeFilter?.value || "";
    state.filteredData = applyFilters(state.newsData, searchTerm, typeValue);

    // Update UI
    renderCurrentData(elements);
    updateLastUpdated(elements.lastUpdatedElement, state.lastUpdated);

    showLoadingOverlay(loadingOverlay, false);

    // Success animation
    refreshBtn.classList.remove("loading");
    refreshBtn.classList.add("success");
    if (icon) {
      icon.className = "fas fa-check";
    }

    const totalItems = state.newsData.length;
    let successMessage = `Loaded ${totalItems} articles`;

    // Show freshness info
    if (data.freshness) {
      successMessage += ` (updated ${data.freshness})`;
    }

    // Add stale indicator if needed
    if (data.isStale) {
      successMessage += ` data is stale`;
    } else {
      successMessage += ` ✅`;
    }

    showRefreshSuccess(successMessage);

    // Reset button after 3 seconds
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

    showRefreshError(
      error.message || "Unable to update data. Please try again later."
    );

    // Reset button after 5 seconds on error
    setTimeout(() => {
      refreshBtn.classList.remove("error");
      if (icon) {
        icon.className = "fas fa-sync-alt refresh-icon";
      }
      refreshBtn.disabled = false;
    }, 5000);
  }
}

/**
 * Handle full backend update - fetch from Techmeme + AI summarization.
 * This is the slow path (30-90 seconds).
 */
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

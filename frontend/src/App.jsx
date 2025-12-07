import { useEffect, useRef, useState } from "react";

import { fetchNewsData, fetchRefreshStatus, refreshNewsFast, triggerFullRefresh } from "./services/newsService";
import { applyFilters } from "./utils/filters";

import NewsList from "./components/NewsList";
import StatsBar from "./components/StatsBar";
import ScrollToTop from "./components/ScrollToTop";
import Notification from "./components/Notification";

import { useNotification } from "./hooks/useNotification";
import { useKeyboardShortcuts } from "./hooks/useKeyboardShortcuts";
import { debounce } from "./utils/debounce";

import "./styles/style.css";

export default function App() {
  const [newsData, setNewsData] = useState([]);
  const [filteredData, setFilteredData] = useState([]);
  const [lastUpdated, setLastUpdated] = useState("--");
  const [loading, setLoading] = useState(true);
  const [isStartingRefresh, setIsStartingRefresh] = useState(false);
  const [refreshStatus, setRefreshStatus] = useState({
    running: false,
    reason: "never_run",
    lastRunAt: null,
  });

  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("");

  const searchRef = useRef(null);
  const statusTimer = useRef(null);
  const wasRunning = useRef(false);

  const { notification, showSuccess, showError } = useNotification();

  // Keyboard shortcuts
  useKeyboardShortcuts(searchRef, () => {
    setSearch("");
    setFilteredData(newsData);
  });

  // Load data on mount
  useEffect(() => {
    loadInitialData();
    refreshStatusOnce();

    return () => {
      if (statusTimer.current) {
        clearTimeout(statusTimer.current);
      }
    };
  }, []);

  async function loadInitialData() {
    try {
      setLoading(true);
      const { items, lastUpdated } = await fetchNewsData();

      setNewsData(items);
      setLastUpdated(lastUpdated);

      const filtered = applyFilters(items, search, typeFilter);
      setFilteredData(filtered);
    } catch {
      showError("Không thể tải dữ liệu.");
    } finally {
      setLoading(false);
    }
  }

  async function refreshStatusOnce() {
    const status = await fetchRefreshStatus();
    setRefreshStatus(status);
    wasRunning.current = status.running;
    if (status.running) {
      startStatusPolling(800);
    }
  }

  // Search debounced
  const debouncedSearch = debounce((keyword) => {
    const filtered = applyFilters(newsData, keyword, typeFilter);
    setFilteredData(filtered);
  }, 300);

  function onSearchChange(e) {
    const value = e.target.value;
    setSearch(value);
    debouncedSearch(value);
  }

  function onTypeFilterChange(e) {
    const value = e.target.value;
    setTypeFilter(value);
    const filtered = applyFilters(newsData, search, value);
    setFilteredData(filtered);
  }

  // Full refresh
  async function handleFullRefresh() {
    if (refreshStatus.running || isStartingRefresh) {
      showError("Đang có tiến trình làm mới, vui lòng đợi.");
      return;
    }

    setIsStartingRefresh(true);

    try {
      const res = await triggerFullRefresh();

      if (res.status === "rate_limited") {
        const retry = res.retry_after_seconds
          ? ` Thử lại sau ${res.retry_after_seconds}s.`
          : "";
        showError(`Bạn bị rate limit (scope: ${res.scope}).${retry}`);
        return;
      }

      if (res.status === "running") {
        showError("Đang chạy một phiên làm mới khác.");
        setRefreshStatus((prev) => ({
          ...prev,
          running: true,
          reason: "running",
        }));
        startStatusPolling(500);
        return;
      }

      // Started new job
      setRefreshStatus((prev) => ({
        ...prev,
        running: true,
        reason: "manual_trigger",
      }));

      showSuccess("Đang làm mới nguồn tin…");
      startStatusPolling(700);
    } catch (err) {
      console.error(err);
      showError("Không thể làm mới dữ liệu.");
    } finally {
      setIsStartingRefresh(false);
    }
  }

  function startStatusPolling(delayMs = 1200) {
    if (statusTimer.current) {
      clearTimeout(statusTimer.current);
    }

    statusTimer.current = setTimeout(async () => {
      const status = await fetchRefreshStatus();
      setRefreshStatus(status);

      // Detect transition running -> idle to auto reload
      if (wasRunning.current && !status.running) {
        await loadAfterRefresh();
      }
      wasRunning.current = status.running;

      if (status.running) {
        startStatusPolling(Math.min(delayMs + 400, 4000));
      }
    }, delayMs);
  }

  async function loadAfterRefresh() {
    try {
      const data = await refreshNewsFast();
      setNewsData(data.items || []);
      setLastUpdated(data.lastUpdated || lastUpdated);

      const filtered = applyFilters(data.items || [], search, typeFilter);
      setFilteredData(filtered);

      showSuccess("Đã làm mới nguồn tin.");
    } catch (err) {
      console.error("Failed to reload after refresh", err);
      showError("Làm mới xong nhưng tải dữ liệu thất bại.");
    }
  }

  const refreshLabel = refreshStatus.running
    ? "Đang làm mới…"
    : "Làm mới nguồn tin";

  return (
    <main className="dashboard">

      {/* HEADER */}
      <section className="panel masthead">

        <div className="masthead__header">
          <div className="masthead__content">
            <p className="eyebrow">Bản tin mỗi ngày</p>

            <div className="masthead__title">
              <h1>Tổng hợp tin công nghệ</h1>
              <p>Skim Techmeme cực nhanh cùng phần tóm tắt AI.</p>
            </div>
          </div>

          <div className="masthead__actions">
            <button
              type="button"
              className={`refresh-btn ${refreshStatus.running ? "loading" : ""}`}
              onClick={handleFullRefresh}
              disabled={refreshStatus.running || isStartingRefresh}
            >
              <i className="fas fa-sync-alt refresh-icon"></i>
              <span>{refreshLabel}</span>
            </button>
          </div>
        </div>

        <StatsBar lastUpdated={lastUpdated} count={filteredData.length} />

        {/* FILTERS */}
        <form className="filters__form">

          <div className="field">
            <label className="field__label">Tìm kiếm</label>
            <div className="field__input">
              <i className="fas fa-search"></i>
              <input
                ref={searchRef}
                type="text"
                placeholder="Nhập từ khóa..."
                value={search}
                onChange={onSearchChange}
              />
            </div>
          </div>

          <div className="field">
            <label className="field__label">Loại tin</label>
            <select value={typeFilter} onChange={onTypeFilterChange}>
              <option value="">Tất cả loại tin</option>
              <option value="news">Tin tức</option>
              <option value="announcement">Thông báo</option>
              <option value="video">Video</option>
              <option value="howto">Hướng dẫn</option>
              <option value="troubleshooting">Khắc phục</option>
            </select>
          </div>

        </form>
      </section>

      {/* LOADING */}
      {loading && (
        <section className="panel loading-state">
          <i className="fas fa-spinner fa-spin"></i>
          <span>Đang tải các bài viết mới nhất…</span>
        </section>
      )}

      {/* FEED */}
      {!loading && (
        <section className="feed">

          {/* EMPTY STATE OUTSIDE FEED GRID */}
          {filteredData.length === 0 ? (
            <section className="panel empty-state">
              <i className="fas fa-search"></i>
              <h3>Không tìm thấy kết quả</h3>
              <p>Hãy thử xoá từ khóa hoặc chọn loại tin khác.</p>
            </section>
          ) : (
            <div className="feed__grid">
              <NewsList items={filteredData} />
            </div>
          )}

        </section>
      )}

      {/* FOOTER */}
      <footer className="site-footer">
        <small>
          © 2025 Tổng hợp tin công nghệ · Dữ liệu từ{" "}
          <a href="https://www.techmeme.com" target="_blank" rel="noreferrer">
            Techmeme RSS
          </a>
        </small>
      </footer>

      {/* NOTIFICATION */}
      {notification && (
        <Notification message={notification.message} type={notification.type} />
      )}

      <ScrollToTop />
    </main>
  );
}

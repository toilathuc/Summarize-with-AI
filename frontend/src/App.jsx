import { useEffect, useRef, useState } from "react";

import { fetchNewsData, refreshNewsFast, triggerFullRefresh } from "./services/newsService";
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
  const [overlay, setOverlay] = useState(null);

  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("");

  const searchRef = useRef(null);

  const { notification, showSuccess, showError } = useNotification();

  // Keyboard shortcuts
  useKeyboardShortcuts(searchRef, () => {
    setSearch("");
    setFilteredData(newsData);
  });

  // Load data on mount
  useEffect(() => {
    loadInitialData();
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
    if (overlay) return;

    setOverlay("Đang thu thập bài viết mới từ Techmeme…");

    try {
      await triggerFullRefresh();
      const data = await refreshNewsFast();

      setNewsData(data.items || []);
      setLastUpdated(data.lastUpdated);

      const filtered = applyFilters(data.items || [], search, typeFilter);
      setFilteredData(filtered);

      showSuccess(`Đã tải ${data.items?.length || 0} bài viết mới`);
    } catch {
      showError("Không thể làm mới dữ liệu.");
    } finally {
      setOverlay(null);
    }
  }

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
            <button type="button" className="refresh-btn" onClick={handleFullRefresh}>
              <i className="fas fa-sync-alt refresh-icon"></i>
              <span>Làm mới nguồn tin</span>
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

      {/* LOADING OVERLAY */}
      {overlay && (
        <div className="loading-overlay show">
          <div className="loading-dialog">
            <div className="loading-spinner"></div>
            <p className="loading-heading">Đang làm mới nguồn tin</p>
            <p className="loading-copy">{overlay}</p>
            <div className="loading-progress">
              <div className="loading-progress__bar"></div>
            </div>
          </div>
        </div>
      )}

      {/* NOTIFICATION */}
      {notification && (
        <Notification message={notification.message} type={notification.type} />
      )}

      <ScrollToTop />
    </main>
  );
}

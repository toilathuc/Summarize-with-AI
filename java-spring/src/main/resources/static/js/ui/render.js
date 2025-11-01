import { escapeHtml } from "../utils/html.js";

const TYPE_ICONS = {
  news: "fas fa-newspaper",
  announcement: "fas fa-bullhorn",
  video: "fas fa-play-circle",
  howto: "fas fa-question-circle",
  troubleshooting: "fas fa-tools",
};

const TYPE_COLORS = {
  news: "#3b82f6",
  announcement: "#10b981",
  video: "#f59e0b",
  howto: "#8b5cf6",
  troubleshooting: "#ef4444",
};

const TYPE_LABELS = {
  news: "Tin tức",
  announcement: "Thông báo",
  video: "Video",
  howto: "Hướng dẫn",
  troubleshooting: "Khắc phục",
};

export function renderNews(container, items) {
  if (!container) {
    return;
  }

  if (!Array.isArray(items) || items.length === 0) {
    container.innerHTML = "";
    return;
  }

  container.innerHTML = items.map(createNewsItemHTML).join("");

  const newsItems = container.querySelectorAll(".news-item");
  newsItems.forEach((element, index) => {
    element.addEventListener("click", () => {
      const url = items[index]?.url;
      if (url) {
        window.open(url, "_blank", "noopener,noreferrer");
      }
    });
  });
}

function createNewsItemHTML(item) {
  const type = item.type || "news";
  const typeIcon = TYPE_ICONS[type] || "fas fa-file-text";
  const typeColor = TYPE_COLORS[type] || "#667eea";
  const typeLabel = TYPE_LABELS[type] || "Khác";

  const bullets = Array.isArray(item.bullets) ? item.bullets : [];
  const whyItMatters = item.why_it_matters || "";

  return `
    <article class="news-item" data-type="${escapeHtml(type)}" style="--type-color: ${typeColor}">
      <div class="news-header">
        <h2 class="news-title">${escapeHtml(item.title || "")}</h2>
        <span class="news-type">
          <i class="${typeIcon}"></i>
          ${typeLabel}
        </span>
      </div>
      <div class="news-bullets">
        <ul>
          ${bullets
            .map((bullet) => `<li>${escapeHtml(bullet)}</li>`)
            .join("")}
        </ul>
      </div>
      ${
        whyItMatters
          ? `
        <div class="why-matters">
          <div class="why-matters-title">Tại sao quan trọng?</div>
          <div class="why-matters-text">${escapeHtml(whyItMatters)}</div>
        </div>
      `
          : ""
      }
      <div class="news-footer">
        <a href="${escapeHtml(item.url || "#")}"
           class="read-more"
           target="_blank"
           rel="noopener noreferrer"
           onclick="event.stopPropagation()">
          Đọc bài gốc <i class="fas fa-external-link-alt"></i>
        </a>
      </div>
    </article>
  `;
}

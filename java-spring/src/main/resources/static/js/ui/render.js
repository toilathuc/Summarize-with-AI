import { escapeHtml } from "../utils/html.js";
import { extractDomain } from "../utils/url.js";

const TYPE_ICONS = {
  news: "fas fa-newspaper",
  announcement: "fas fa-bullhorn",
  video: "fas fa-play-circle",
  howto: "fas fa-question-circle",
  troubleshooting: "fas fa-tools",
};

const TYPE_COLORS = {
  news: "#60a5fa",
  announcement: "#34d399",
  video: "#fbbf24",
  howto: "#a78bfa",
  troubleshooting: "#f87171",
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

  const newsItems = container.querySelectorAll(".news-card");
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
  const typeLabel = TYPE_LABELS[type] || "Loại khác";

  const bullets = Array.isArray(item.bullets) ? item.bullets : [];
  const whyItMatters = item.why_it_matters || "";
  const domain = extractDomain(item.url);
  const source = domain || item.source || item.publisher || "Techmeme";

  return `
    <article class="news-card" data-type="${escapeHtml(
      type
    )}" style="--type-color: ${typeColor}">
      <header class="news-card__header">
        <span class="news-chip">
          <span class="news-chip__dot"></span>
          ${typeLabel}
        </span>
        <i class="${typeIcon} news-card__icon" aria-hidden="true"></i>
      </header>
      <h2 class="news-card__title">${escapeHtml(item.title || "")}</h2>
      ${
        bullets.length
          ? `<ul class="news-card__bullets">
          ${bullets
            .map((bullet) => `<li>${escapeHtml(bullet)}</li>`)
            .join("")}
        </ul>`
          : ""
      }
      ${
        whyItMatters
          ? `
        <div class="insight-card">
          <p class="insight-label">Vì sao quan trọng</p>
          <p>${escapeHtml(whyItMatters)}</p>
        </div>
      `
          : ""
      }
      <footer class="news-card__footer">
        <span class="news-card__source">${escapeHtml(source)}</span>
        <a href="${escapeHtml(item.url || "#")}"
           class="news-card__cta"
           target="_blank"
           rel="noopener noreferrer"
           onclick="event.stopPropagation()">
          Mở bài gốc <i class="fas fa-arrow-up-right-from-square"></i>
        </a>
      </footer>
    </article>
  `;
}

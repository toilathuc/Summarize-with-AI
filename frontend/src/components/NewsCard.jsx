import { escapeHtml } from "../utils/html";
import { extractDomain } from "../utils/url";

export default function NewsCard({ item }) {
  const type = item.type || "news";

  const TYPE_ICONS = {
    news: "fas fa-newspaper",
    announcement: "fas fa-bullhorn",
    video: "fas fa-play-circle",
    howto: "fas fa-question-circle",
    troubleshooting: "fas fa-tools",
  };

  const TYPE_LABELS = {
    news: "Tin tức",
    announcement: "Thông báo",
    video: "Video",
    howto: "Hướng dẫn",
    troubleshooting: "Khắc phục",
  };

  const icon = TYPE_ICONS[type] || "fas fa-file-alt";
  const label = TYPE_LABELS[type] || "Loại khác";

  const domain = extractDomain(item.url);

  return (
    <article className="news-card" style={{ cursor: "pointer" }}
      onClick={() => window.open(item.url, "_blank")}
    >
      <header className="news-card__header">
        <span className="news-chip">
          <span className="news-chip__dot"></span>
          {label}
        </span>
        <i className={icon}></i>
      </header>

      <h2 className="news-card__title">{escapeHtml(item.title)}</h2>

      {item.bullets?.length > 0 && (
        <ul className="news-card__bullets">
          {item.bullets.map((b, i) => (
            <li key={i}>{escapeHtml(b)}</li>
          ))}
        </ul>
      )}

      {item.why_it_matters && (
        <div className="insight-card">
          <p className="insight-label">Vì sao quan trọng</p>
          <p>{escapeHtml(item.why_it_matters)}</p>
        </div>
      )}

      <footer className="news-card__footer">
        <span className="news-card__source">{domain}</span>
        <a
          onClick={(e) => e.stopPropagation()}
          href={item.url}
          target="_blank"
          rel="noopener noreferrer"
          className="news-card__cta"
        >
          Mở bài gốc →
        </a>
      </footer>
    </article>
  );
}

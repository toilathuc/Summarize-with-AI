export default function NewsList({ items }) {
  return (
    <>
      {items.map((item, idx) => (
        <article
          key={idx}
          className="news-card"
          data-type={item.type || "news"}
          onClick={() => window.open(item.url, "_blank")}
        >
          <header className="news-card__header">
            <span className="news-chip">
              <span className="news-chip__dot"></span>
              {item.type_label || "Tin tức"}
            </span>
            <i className="news-card__icon fas fa-newspaper"></i>
          </header>

          <h2 className="news-card__title">{item.title}</h2>

          {item.bullets?.length > 0 && (
            <ul className="news-card__bullets">
              {item.bullets.map((b, i) => (
                <li key={i}>{b}</li>
              ))}
            </ul>
          )}

          {item.why_it_matters && (
            <div className="insight-card">
              <p className="insight-label">Vì sao quan trọng</p>
              <p>{item.why_it_matters}</p>
            </div>
          )}

          <footer className="news-card__footer">
            <span className="news-card__source">{item.source}</span>
            <a
              href={item.url}
              className="news-card__cta"
              target="_blank"
              rel="noreferrer"
              onClick={(e) => e.stopPropagation()}
            >
              Mở bài gốc <i className="fas fa-arrow-up-right-from-square"></i>
            </a>
          </footer>
        </article>
      ))}
    </>
  );
}

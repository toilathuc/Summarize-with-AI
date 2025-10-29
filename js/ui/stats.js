export function updateStats(element, totalItems) {
  if (!element) {
    return;
  }

  const totalText = totalItems === 1 ? "1 Article" : `${totalItems} Articles`;
  element.textContent = totalText;
}

export function updateLastUpdated(element, sourceTimestamp = null) {
  if (!element) {
    return;
  }

  let date = null;

  if (sourceTimestamp) {
    const parsed = new Date(sourceTimestamp);
    if (!Number.isNaN(parsed.getTime())) {
      date = parsed;
    }
  }

  if (!date) {
    date = new Date();
  }

  const timeString = date.toLocaleString("vi-VN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });

  element.textContent = `Last updated: ${timeString}`;
}

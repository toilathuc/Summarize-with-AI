function normalizeText(value) {
  return (value || "").toString().toLowerCase();
}

export function applyFilters(items, searchTerm, typeValue) {
  const normalizedSearch = normalizeText(searchTerm).trim();
  const normalizedType = typeValue || "";

  if (!normalizedSearch && !normalizedType) {
    return [...items];
  }

  return items.filter((item) => {
    const matchesType =
      !normalizedType || item.type === normalizedType;

    if (normalizedSearch) {
      const titleMatches = normalizeText(item.title).includes(
        normalizedSearch,
      );

      const bulletMatches = Array.isArray(item.bullets)
        ? item.bullets.some((bullet) =>
            normalizeText(bullet).includes(normalizedSearch),
          )
        : false;

      const whyMatches = normalizeText(item.why_it_matters).includes(
        normalizedSearch,
      );

      return matchesType && (titleMatches || bulletMatches || whyMatches);
    }

    return matchesType;
  });
}

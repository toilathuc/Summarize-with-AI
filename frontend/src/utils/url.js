export function extractDomain(url) {
  if (!url || typeof url !== "string") {
    return null;
  }

  try {
    const normalized = url.startsWith("http") ? url : `https://${url}`;
    const { hostname } = new URL(normalized);
    return hostname.replace(/^www\./, "");
  } catch (error) {
    return null;
  }
}

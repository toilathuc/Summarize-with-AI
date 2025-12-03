export default function StatsBar({ lastUpdated, count }) {
  return (
    <div className="stats-bar">
      <span>Cập nhật lần cuối: {lastUpdated}</span>
      <span>{count} bài viết</span>
    </div>
  );
}

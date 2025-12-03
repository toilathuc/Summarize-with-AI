export default function Notification({ message, type }) {
  if (!message) return null;

  const color =
    type === "success" ? "#10b981" :
    type === "error"   ? "#ef4444" :
                         "#3b82f6";

  return (
    <div
      style={{
        position: "fixed",
        top: 20,
        right: 20,
        background: color,
        color: "white",
        padding: "1rem 1.5rem",
        borderRadius: 8,
        boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
        zIndex: 1000,
        animation: "slideIn 0.3s ease-out"
      }}
    >
      {message}
    </div>
  );
}

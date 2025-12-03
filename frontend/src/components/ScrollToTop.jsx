import { useEffect, useState } from "react";

export default function ScrollToTop() {
  const [show, setShow] = useState(false);

  useEffect(() => {
    function onScroll() {
      setShow(window.scrollY > 500);
    }
    window.addEventListener("scroll", onScroll);
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  if (!show) return null;

  return (
    <button
      style={{
        position: "fixed",
        bottom: "2rem",
        right: "2rem",
        width: 50,
        height: 50,
        borderRadius: "50%",
        background: "#667eea",
        color: "white",
        border: "none",
        cursor: "pointer",
        zIndex: 1000
      }}
      onClick={() =>
        window.scrollTo({ top: 0, behavior: "smooth" })
      }
    >
      ↑
    </button>
  );
}

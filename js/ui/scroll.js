const SCROLL_BUTTON_CLASS = "scroll-to-top";

export function initScrollToTop() {
  window.addEventListener("scroll", handleScroll);
}

function handleScroll() {
  if (window.scrollY > 500) {
    ensureScrollButton();
  } else {
    removeScrollButton();
  }
}

function ensureScrollButton() {
  if (document.querySelector(`.${SCROLL_BUTTON_CLASS}`)) {
    return;
  }

  const scrollButton = document.createElement("button");
  scrollButton.className = SCROLL_BUTTON_CLASS;
  scrollButton.innerHTML = '<i class="fas fa-chevron-up"></i>';
  scrollButton.style.cssText = `
    position: fixed;
    bottom: 2rem;
    right: 2rem;
    width: 50px;
    height: 50px;
    border-radius: 50%;
    background: #667eea;
    color: white;
    border: none;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    z-index: 1000;
    transition: all 0.3s ease;
  `;
  scrollButton.addEventListener("click", scrollToTop);
  document.body.appendChild(scrollButton);
}

function removeScrollButton() {
  const scrollButton = document.querySelector(`.${SCROLL_BUTTON_CLASS}`);
  if (scrollButton) {
    scrollButton.remove();
  }
}

function scrollToTop() {
  window.scrollTo({
    top: 0,
    behavior: "smooth",
  });
}

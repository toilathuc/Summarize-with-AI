export function debounce(func, wait = 300) {
  let timeout;

  return function debounced(...args) {
    const context = this;

    clearTimeout(timeout);
    timeout = setTimeout(() => {
      timeout = null;
      func.apply(context, args);
    }, wait);
  };
}

/**
 * Global 3D interaction layer, installed once from main.tsx.
 *
 * - [data-tilt]: the element rotates towards the cursor and gets a moving glare
 *   (CSS reads --rx / --ry / --mx / --my). One delegated pointer listener for the
 *   whole page, so it also works for elements rendered later.
 * - [data-reveal]: sections flip/rise into view the first time they are scrolled to.
 *
 * Purely decorative: with prefers-reduced-motion or a coarse (touch) pointer nothing
 * is installed and the CSS shows everything in its normal state.
 */
const MAX_TILT_DEG = 9;

export function installEffects(): void {
  if (typeof window === "undefined") return;
  const reduced = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
  if (reduced) return;
  installReveal();
  if (window.matchMedia?.("(pointer: fine)").matches) installTilt();
}

function installTilt(): void {
  let active: HTMLElement | null = null;
  let frame = 0;
  let lastEvent: PointerEvent | null = null;

  const reset = (el: HTMLElement) => {
    el.classList.remove("is-tilting");
    el.style.removeProperty("--rx");
    el.style.removeProperty("--ry");
  };

  const apply = () => {
    frame = 0;
    if (!active || !lastEvent) return;
    const rect = active.getBoundingClientRect();
    const x = (lastEvent.clientX - rect.left) / rect.width;   // 0..1
    const y = (lastEvent.clientY - rect.top) / rect.height;  // 0..1
    active.style.setProperty("--ry", `${((x - 0.5) * 2 * MAX_TILT_DEG).toFixed(2)}deg`);
    active.style.setProperty("--rx", `${((0.5 - y) * 2 * MAX_TILT_DEG).toFixed(2)}deg`);
    active.style.setProperty("--mx", `${(x * 100).toFixed(1)}%`);
    active.style.setProperty("--my", `${(y * 100).toFixed(1)}%`);
  };

  document.addEventListener("pointermove", event => {
    const target = (event.target as Element | null)?.closest?.<HTMLElement>("[data-tilt]") ?? null;
    if (target !== active) {
      if (active) reset(active);
      active = target;
      active?.classList.add("is-tilting");
    }
    lastEvent = event;
    if (active && !frame) frame = requestAnimationFrame(apply);
  }, {passive: true});

  document.addEventListener("pointerleave", () => {
    if (active) reset(active);
    active = null;
  });
}

function installReveal(): void {
  if (!("IntersectionObserver" in window)) return;
  document.documentElement.classList.add("fx-reveal");
  const observer = new IntersectionObserver(entries => {
    for (const entry of entries) {
      if (entry.isIntersecting) {
        entry.target.classList.add("revealed");
        observer.unobserve(entry.target);
      }
    }
  }, {rootMargin: "0px 0px -8% 0px", threshold: 0.08});

  const scan = (root: ParentNode) => {
    root.querySelectorAll?.("[data-reveal]:not(.revealed)").forEach(el => observer.observe(el));
  };
  scan(document);
  // React mounts pages after this runs; watch for new [data-reveal] nodes.
  new MutationObserver(mutations => {
    for (const m of mutations) {
      m.addedNodes.forEach(node => {
        if (node instanceof Element) {
          if (node.matches("[data-reveal]:not(.revealed)")) observer.observe(node);
          scan(node);
        }
      });
    }
  }).observe(document.body, {childList: true, subtree: true});
}

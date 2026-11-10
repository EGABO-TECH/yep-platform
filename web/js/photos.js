// Background photographs for the public screens (landing, log in, create account).
// Young people in Lagos, Accra and Nairobi, from Unsplash (Unsplash License).
// Each photo matches a word of the YEP tagline. They crossfade slowly behind a navy scrim.
import { esc } from "./core.js";

export const SLIDES = [
  { word: "Learn", src: "img/hero/learn", place: "Lagos, Nigeria", credit: "disruptxn", url: "https://unsplash.com/photos/IgUR1iX0mqM" },
  { word: "Connect", src: "img/hero/connect", place: "Accra, Ghana", credit: "Creab ThePolymath", url: "https://unsplash.com/photos/jDXMQf3LPX8" },
  { word: "Grow", src: "img/hero/grow", place: "Nairobi, Kenya", credit: "Oscar Omondi", url: "https://unsplash.com/photos/BZbPR9JbalA" },
];
const HOLD = 6500;

/** The photo layer and scrim. Place it first inside a `.photo-screen` element. */
export function photoBackdrop(start = 0) {
  return `<div class="ph-media" aria-hidden="true">
    ${SLIDES.map((s, i) => `<img class="ph-slide${i === start ? " is-on" : ""}" src="${s.src}.jpg"
      srcset="${s.src}-sm.jpg 900w, ${s.src}.jpg 1920w" sizes="100vw" alt=""
      ${i === start ? 'fetchpriority="high"' : 'loading="lazy"'} decoding="async">`).join("")}
  </div><div class="ph-scrim" aria-hidden="true"></div>`;
}

/** The location and photographer of the photo on screen. */
export const photoCredit = (s) => `${esc(s.place)}. Photo: <a href="${s.url}" target="_blank" rel="noopener">${esc(s.credit)}</a>, Unsplash`;

/**
 * Starts the crossfade inside `screen`. onChange(index, slide) runs for every photo shown,
 * including the first. Stops by itself when the screen leaves the page. Holds still for
 * people who ask for reduced motion.
 */
export function runSlideshow(screen, { start = 0, onChange = () => {} } = {}) {
  const slides = [...screen.querySelectorAll(".ph-slide")];
  const show = (i) => { slides.forEach((el, k) => el.classList.toggle("is-on", k === i)); onChange(i, SLIDES[i]); };
  show(start);
  if (matchMedia("(prefers-reduced-motion: reduce)").matches) { screen.classList.add("is-still"); return; }
  let i = start;
  const timer = setInterval(() => {
    if (!screen.isConnected) { clearInterval(timer); return; }
    if (document.hidden) return;
    i = (i + 1) % SLIDES.length;
    show(i);
  }, HOLD);
}

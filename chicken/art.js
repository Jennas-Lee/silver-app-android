// 치킨 가게 그림 (SVG). 버거 가게 그림 중 같이 쓸 것은 ../art.js 에서 가져온다.
import { art as baseArt, burger, cup, fries, inner, svg } from "../art.js";

const RED = "#E4002B";
const ORIGINAL = ["#D9902F", "#A9631A", "#F2C27A"];
const HOT = ["#C4521C", "#8A300C", "#E98A4A"];

const CRUMBS = [[80, 70], [110, 58], [132, 84], [96, 96], [72, 102], [120, 112], [144, 66], [100, 76], [86, 118]];

function crust(c, pts = CRUMBS, hot = false) {
  return pts
    .map(([x, y], i) => {
      const r = 6 + (i % 3) * 2;
      const spot = hot && i % 2 ? `<circle cx="${x + 5}" cy="${y + 4}" r="2.4" fill="#B3140F"/>` : "";
      return `<ellipse cx="${x}" cy="${y}" rx="${r}" ry="${r - 2}" fill="${c[1]}" opacity=".35"/><ellipse cx="${x - 2}" cy="${y - 3}" rx="${r - 4}" ry="${r - 5}" fill="${c[2]}" opacity=".6"/>${spot}`;
    })
    .join("");
}

/** 북채(다리) 한 조각 */
function drumstick(c, hot) {
  return `<path d="M88 126 L56 166" stroke="#F3E6D0" stroke-width="15" stroke-linecap="round"/>
    <circle cx="48" cy="166" r="10" fill="#F3E6D0"/><circle cx="58" cy="176" r="10" fill="#F3E6D0"/>
    <path d="M70 132 C38 110 42 52 92 40 C142 30 170 72 154 104 C144 124 118 132 102 136 Z" fill="${c[0]}"/>
    ${crust(c, CRUMBS, hot)}`;
}

/** 넓적다리 한 조각 */
function thigh(c, hot) {
  return `<path d="M40 120 C28 70 80 40 122 48 C168 56 174 112 152 140 C130 168 58 168 40 120 Z" fill="${c[0]}"/>
    ${crust(c, [[70, 80], [100, 66], [132, 78], [150, 108], [118, 100], [86, 112], [64, 130], [104, 140], [134, 140]], hot)}`;
}

const g = (x, y, s, r, body) => `<g transform="translate(${x} ${y}) rotate(${r} 100 100) scale(${s})">${body}</g>`;
const shadow = (cy = 176, rx = 72) => `<ellipse cx="100" cy="${cy}" rx="${rx}" ry="8" fill="#000" opacity=".1"/>`;

function pieces(n, hot = false) {
  const c = hot ? HOT : ORIGINAL;
  const L = {
    1: [[0, 10, 1, 0, "d"]],
    2: [[-8, 0, 0.78, -12, "t"], [36, 34, 0.74, 10, "d"]],
    3: [[-14, -6, 0.66, -10, "t"], [48, -2, 0.62, 16, "d"], [16, 44, 0.68, 0, "d"]],
  }[Math.min(n, 3)];
  return svg(shadow() + L.map(([x, y, s, r, k]) => g(x, y, s, r, k === "d" ? drumstick(c, hot) : thigh(c, hot))).join(""));
}

/** 반반: 오리지널 + 매운맛 */
function half() {
  return svg(shadow() + g(-12, 4, 0.72, -12, drumstick(ORIGINAL)) + g(40, 26, 0.72, 14, drumstick(HOT, true)));
}

function tenders(n = 3) {
  const strips = Array.from({ length: n }, (_, i) => {
    const x = 30 + i * (120 / n);
    const r = -20 + i * 10;
    return `<g transform="rotate(${r} ${x + 20} 110)"><rect x="${x}" y="40" width="40" height="130" rx="20" fill="${i % 2 ? "#DC963A" : "#E8A54A"}"/>
      <path d="M${x + 10} 70 l8 4 M${x + 22} 100 l8 4 M${x + 10} 130 l8 4" stroke="#B06F1C" stroke-width="3" stroke-linecap="round"/></g>`;
  }).join("");
  return svg(shadow(180, 70) + strips);
}

function wings(n = 4) {
  const spots = [[-30, 10, -20], [40, 4, 18], [6, 50, -4], [56, 52, 26], [-36, 60, -30]].slice(0, Math.min(n, 5));
  return svg(shadow() + spots.map(([x, y, r]) => g(x, y, 0.55, r, drumstick(HOT, true))).join(""));
}

function bucket(n = 9, hot = false) {
  const c = hot ? HOT : ORIGINAL;
  const top = [
    n >= 9 ? g(4, 22, 0.42, -40, thigh(c, hot)) : "",
    n >= 9 ? g(96, 24, 0.42, 34, drumstick(c, hot)) : "",
    g(18, 10, 0.5, -24, drumstick(c, hot)),
    g(70, 8, 0.5, 20, thigh(c, hot)),
    g(44, -2, 0.52, 0, drumstick(c, hot)),
  ].join("");
  return svg(`${shadow(186, 66)}${top}
    <path d="M34 78 H166 L150 184 H50 Z" fill="${RED}"/>
    <rect x="30" y="70" width="140" height="16" rx="6" fill="#B8001F"/>
    <path d="M40 112 H160 L156 140 H44 Z" fill="#fff"/>
    <text x="100" y="134" text-anchor="middle" font-size="22" font-weight="900" fill="${RED}" font-family="Pretendard, sans-serif">${n}조각</text>`);
}

function combo() {
  return svg(`${shadow(184, 88)}
    ${g(-34, 6, 0.74, 0, inner(bucket(6)))}
    ${g(58, 40, 0.58, 0, inner(fries(RED)))}
    ${g(92, 30, 0.5, 0, inner(cup(RED)))}`);
}

function box() {
  return svg(`${shadow(186, 88)}
    <path d="M14 104 H186 L176 182 H24 Z" fill="${RED}"/>
    <path d="M14 104 L40 70 H160 L186 104 Z" fill="#B8001F"/>
    ${g(10, 18, 0.42, 0, inner(burger(["top", "lettuce", "chicken", "bottom"])))}
    ${g(70, 12, 0.4, 18, drumstick(ORIGINAL))}
    ${g(110, 8, 0.38, 0, inner(fries(RED)))}
    <rect x="62" y="128" width="76" height="26" rx="13" fill="#fff"/>
    <text x="100" y="147" text-anchor="middle" font-size="18" font-weight="900" fill="${RED}" font-family="Pretendard, sans-serif">BOX</text>`);
}

function eggtart(n = 1) {
  const one = (x, y, s) => g(x, y, s, 0, `
    <ellipse cx="100" cy="122" rx="74" ry="34" fill="#C98A3E"/>
    <path d="M26 112 q8 -12 16 0 t16 0 t16 0 t16 0 t16 0 t16 0 t16 0 t16 0 t16 0 V126 H26 Z" fill="#E2AE62"/>
    <ellipse cx="100" cy="110" rx="60" ry="24" fill="#FFD45C"/>
    <ellipse cx="84" cy="104" rx="12" ry="6" fill="#B8651E" opacity=".75"/><ellipse cx="114" cy="114" rx="9" ry="5" fill="#B8651E" opacity=".6"/><ellipse cx="122" cy="100" rx="6" ry="3" fill="#B8651E" opacity=".5"/>`);
  const body = n > 1 ? one(-40, -20, 0.62) + one(36, -24, 0.62) + one(-40, 34, 0.62) + one(36, 30, 0.62) : one(0, 0, 1);
  return svg(shadow(164, 76) + body);
}

function biscuit() {
  return svg(`${shadow(168, 70)}
    <path d="M36 140 Q30 80 100 72 Q170 80 164 140 Z" fill="#E7A957"/>
    <path d="M44 104 Q100 70 156 104" stroke="#F6CD8A" stroke-width="12" fill="none" stroke-linecap="round"/>
    <rect x="36" y="132" width="128" height="22" rx="10" fill="#C9883A"/>
    <rect x="140" y="132" width="44" height="34" rx="6" fill="#fff" stroke="#ddd" stroke-width="2"/><rect x="146" y="138" width="32" height="14" rx="3" fill="#D7263D"/>`);
}

function cheeseball() {
  const balls = [[66, 120], [112, 124], [88, 88], [134, 92]]
    .map(([x, y]) => `<circle cx="${x}" cy="${y}" r="28" fill="#E8A043"/><circle cx="${x - 8}" cy="${y - 8}" r="8" fill="#F6C97E"/>`)
    .join("");
  return svg(`${shadow(164, 70)}${balls}<path d="M112 124 q14 18 30 4" stroke="#FFE9A6" stroke-width="6" fill="none" stroke-linecap="round"/>`);
}

function cornsalad() {
  const dots = Array.from({ length: 14 }, (_, i) => `<circle cx="${62 + (i % 7) * 13}" cy="${84 + Math.floor(i / 7) * 10}" r="6" fill="#FFD23F"/>`).join("");
  return svg(`${shadow(182, 56)}${dots}
    <path d="M42 92 H158 L144 178 H56 Z" fill="#fff" stroke="#E2E2E2" stroke-width="2"/>
    <rect x="58" y="120" width="84" height="20" rx="4" fill="#FFC21A"/>`);
}

const ART = {
  logo: () => svg(g(0, 4, 1, 0, drumstick(ORIGINAL))),
  original1: () => pieces(1),
  original2: () => pieces(2),
  original3: () => pieces(3),
  hot1: () => pieces(1, true),
  hot2: () => pieces(2, true),
  hot3: () => pieces(3, true),
  half,
  tenders3: () => tenders(3),
  tenders5: () => tenders(5),
  wings4: () => wings(4),
  wings8: () => wings(5),
  bucket6: () => bucket(6),
  bucket9: () => bucket(9),
  bucket12: () => bucket(12),
  bucketHot: () => bucket(9, true),
  bucketTender: () => svg(`${shadow(186, 66)}${g(40, -6, 0.6, 0, inner(tenders(5)))}<path d="M34 78 H166 L150 184 H50 Z" fill="${RED}"/><rect x="30" y="70" width="140" height="16" rx="6" fill="#B8001F"/><path d="M40 112 H160 L156 140 H44 Z" fill="#fff"/>`),
  combo,
  box,
  eggtart1: () => eggtart(1),
  eggtart4: () => eggtart(4),
  biscuit,
  cheeseball,
  cornsalad,
  classicBurger: () => burger(["top", "lettuce", "chicken", "bottom"]),
  spicyBurger: () => burger(["top", "lettuce", "tomato", "chicken", "bottom"]),
  towerBurger: () => burger(["top", "lettuce", "cheese", "hash", "chicken", "bottom"]),
  cheeseBurger: () => burger(["top", "cheese", "chicken", "bottom"]),
  doubleBurger: () => burger(["top", "lettuce", "chicken", "cheese", "chicken", "bottom"]),
  bulgogiBurger: () => burger(["top", "lettuce", "onion", "chicken", "bottom"]),
  friesRed: () => fries(RED),
};

export function art(kind) {
  return ART[kind] ? ART[kind]() : baseArt(kind);
}

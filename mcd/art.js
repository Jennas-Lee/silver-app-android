// 맥도날드 그림 (SVG). 공통 그림(버거, 컵 등)은 ../art.js 에서 가져온다.
import { art as baseArt, burger, cup, inner, svg } from "../art.js";

const RED = "#DB0007";
const YELLOW = "#FFBC0D";

/** 골든 아치(M). 100×88 크기를 (x, y)에 s배로 그린다. */
export function arches(x, y, s, color = YELLOW) {
  return `<path transform="translate(${x} ${y}) scale(${s})" fill="${color}" d="M0 88 C0 34 11 0 25 0 C38 0 46 28 50 56 C54 28 62 0 75 0 C89 0 100 34 100 88 H85 C85 44 81 15 75 15 C67 15 60 46 57 88 H43 C40 46 33 15 25 15 C19 15 15 44 15 88 Z"/>`;
}

const shadow = (cy = 184, rx = 60) => `<ellipse cx="100" cy="${cy}" rx="${rx}" ry="7" fill="#000" opacity=".1"/>`;

/** 빨간 종이 상자에 담긴 감자튀김 */
function fries(size = "M") {
  const k = { S: 0.8, M: 0.9, L: 1 }[size];
  const sticks = [44, 58, 72, 86, 100, 114, 128, 142, 154]
    .map((x, i) => `<rect x="${x}" y="${30 + (i % 3) * 10}" width="12" height="90" rx="3" fill="#FFCE3A" transform="rotate(${(i - 4) * 3} ${x} 120)"/>`)
    .join("");
  return svg(`${shadow()}<g transform="translate(${100 - 100 * k} ${190 - 190 * k}) scale(${k})">${sticks}
    <path d="M40 88 Q100 104 160 88 L148 182 H52 Z" fill="${RED}"/>
    <path d="M40 88 Q100 104 160 88 L158 100 Q100 116 42 100 Z" fill="#B30006"/>
    ${arches(76, 120, 0.48)}</g>`);
}

/** 탄산음료 컵: 흰 물결 띠 */
function soda(color) {
  const c = cup(color, { mark: null });
  return c.replace("</svg>", `<path d="M60 128 q20 -18 40 -2 t40 -6 l-2 14 q-20 -4 -38 8 t-42 0 Z" fill="#fff" opacity=".9"/></svg>`);
}

/** 맥도날드 컵: 흰 컵에 아치 */
function mccup(liquid = "#4A2A14") {
  return svg(`${shadow(186, 48)}
    <rect x="114" y="12" width="9" height="66" rx="3" fill="#fff" stroke="#ddd" stroke-width="2" transform="rotate(10 118 60)"/>
    <rect x="46" y="52" width="108" height="12" rx="5" fill="#fff" stroke="#ccc" stroke-width="2"/>
    <path d="M52 64 H148 L136 182 H64 Z" fill="#EAF4FB" stroke="#C8DCE8" stroke-width="2"/>
    <path d="M56 90 H144 L136 180 H64 Z" fill="${liquid}"/>
    <rect x="70" y="112" width="60" height="44" rx="6" fill="#fff" opacity=".92"/>${arches(84, 118, 0.32)}`);
}

/** 맥카페 따뜻한 컵 */
function cafe(sleeve = "#5B3A26") {
  return svg(`${shadow(186, 46)}
    <path d="M86 40 q-8 -12 2 -22 M106 40 q-8 -12 2 -22" stroke="#bbb" stroke-width="4" fill="none" stroke-linecap="round" opacity=".7"/>
    <rect x="50" y="48" width="100" height="18" rx="6" fill="#2B2B2B"/>
    <path d="M54 66 H146 L134 182 H66 Z" fill="#FAFAFA" stroke="#E2E2E2" stroke-width="2"/>
    <path d="M58 98 H142 L138 146 H62 Z" fill="${sleeve}"/>
    <text x="100" y="128" text-anchor="middle" font-size="15" font-weight="800" fill="#fff" font-family="Pretendard, sans-serif">McCafé</text>`);
}

/** 쉐이크: 반구 뚜껑 + 흰 컵 */
function shake(color) {
  return svg(`${shadow(186, 48)}
    <rect x="112" y="6" width="10" height="64" rx="3" fill="${color}" transform="rotate(10 116 60)"/>
    <path d="M50 64 Q100 18 150 64 Z" fill="#fff" opacity=".9" stroke="#ddd" stroke-width="2"/>
    <path d="M52 64 H148 L136 182 H64 Z" fill="#fff" stroke="#E2E2E2" stroke-width="2"/>
    <path d="M55 92 H145 L139 150 H61 Z" fill="${color}"/>${arches(84, 106, 0.32, "#fff")}`);
}

/** 맥플러리: 흰 컵 + 과자 가루 + 숟가락 */
function mcflurry(top = "#2B2B2B", sauce = null) {
  const crumbs = [[70, 60], [92, 52], [116, 58], [132, 66], [82, 74], [106, 70], [124, 80], [64, 78]]
    .map(([x, y], i) => `<rect x="${x}" y="${y}" width="${8 + (i % 3) * 3}" height="7" rx="2" fill="${top}" transform="rotate(${i * 23} ${x} ${y})"/>`)
    .join("");
  return svg(`${shadow(186, 52)}
    <rect x="118" y="14" width="10" height="80" rx="4" fill="#fff" stroke="#ccc" stroke-width="2" transform="rotate(18 122 60)"/>
    <path d="M54 88 Q50 50 100 46 Q150 50 146 88 Z" fill="#FFFDF5" stroke="#EDE6D6" stroke-width="2"/>
    ${sauce ? `<path d="M64 70 q12 10 22 0 q12 10 24 0 q12 10 24 0" stroke="${sauce}" stroke-width="7" fill="none" stroke-linecap="round"/>` : ""}${crumbs}
    <path d="M50 88 H150 L138 182 H62 Z" fill="#fff" stroke="#E2E2E2" stroke-width="2"/>
    <path d="M56 116 H144 L140 150 H60 Z" fill="${RED}"/>${arches(85, 120, 0.3)}`);
}

/** 상하이 치킨 스낵랩 */
function wrap() {
  return svg(`${shadow(178, 56)}
    <g transform="rotate(-18 100 100)">
      <path d="M78 34 Q100 22 122 34 L132 70 H68 Z" fill="#D9932F"/>
      <path d="M70 66 q8 -10 16 0 t16 0 t16 0 t16 0 V76 H70 Z" fill="#5DB83F"/>
      <path d="M64 70 H136 L120 176 H80 Z" fill="#F6E7C6"/>
      <path d="M64 70 L120 176 H96 L64 104 Z" fill="#EBD5A6"/>
      <path d="M70 120 H130 L124 160 H76 Z" fill="${RED}"/>
      ${arches(88, 128, 0.24)}
    </g>`);
}

/** 한입 초코 츄러스 */
function churros() {
  const stick = (x, r) => `<g transform="rotate(${r} 100 120)"><rect x="${x}" y="40" width="22" height="120" rx="11" fill="#C98A3E"/>
    <path d="M${x + 4} 60 v90 M${x + 11} 56 v98 M${x + 18} 60 v90" stroke="#9B5E1E" stroke-width="2"/>
    <rect x="${x - 1}" y="40" width="24" height="36" rx="11" fill="#5B3417"/></g>`;
  return svg(`${shadow(172, 60)}${stick(60, -14)}${stick(88, 0)}${stick(116, 14)}`);
}

/** 맥너겟 상자 */
function nuggets(n = 4) {
  const spots = [[70, 112], [120, 108], [94, 88], [142, 86], [62, 84], [110, 128], [150, 120], [80, 132], [128, 64], [76, 62]].slice(0, Math.min(n, 10));
  const ng = spots
    .map(([x, y], i) => `<path d="M${x - 22} ${y} q4 -18 22 -16 q20 -2 22 14 q2 18 -20 18 q-26 2 -24 -16 Z" fill="${i % 2 ? "#D9932F" : "#E5A443"}"/><path d="M${x - 8} ${y - 6} l5 -2 M${x + 6} ${y + 2} l5 -2" stroke="#B0701E" stroke-width="2.5" stroke-linecap="round"/>`)
    .join("");
  return svg(`${shadow(182, 66)}${ng}
    <path d="M30 110 H170 L160 178 H40 Z" fill="${RED}"/><path d="M30 110 H170 L166 122 H34 Z" fill="#B30006"/>
    ${arches(80, 132, 0.4)}`);
}

/** 해피밀 상자 */
function happyMeal(kind = "burger") {
  const peek = {
    burger: `<g transform="translate(40 -6) scale(.6)">${inner(burger(["top", "cheese", "beef", "bottom"]))}</g>`,
    nugget: `<g transform="translate(46 4) scale(.54)">${inner(nuggets(6))}</g>`,
  }[kind];
  return svg(`${shadow(186, 74)}${peek}
    <path d="M44 72 Q44 32 64 32 Q84 32 84 72" stroke="${YELLOW}" stroke-width="10" fill="none"/>
    <path d="M116 72 Q116 32 136 32 Q156 32 156 72" stroke="${YELLOW}" stroke-width="10" fill="none"/>
    <path d="M28 76 H172 L162 182 H38 Z" fill="${RED}"/>
    <path d="M28 76 H172 L170 90 H30 Z" fill="#B30006"/>
    <circle cx="80" cy="122" r="7" fill="#fff"/><circle cx="120" cy="122" r="7" fill="#fff"/>
    <circle cx="80" cy="122" r="3" fill="#222"/><circle cx="120" cy="122" r="3" fill="#222"/>
    <path d="M66 140 Q100 176 134 140" stroke="${YELLOW}" stroke-width="9" fill="none" stroke-linecap="round"/>`);
}

/** 종이 봉투 */
function bag() {
  return svg(`${shadow(186, 60)}
    <path d="M44 56 H156 L150 182 H50 Z" fill="#C9A06A"/>
    <path d="M44 56 l8 -10 h96 l8 10 Z" fill="#A87E52"/>
    <path d="M44 56 H156 L156 64 H44 Z" fill="#B48A58"/>
    ${arches(64, 92, 0.72)}`);
}

/** 쟁반: 버거 + 감자튀김 + 컵 */
function tray(large = false) {
  return svg(`<ellipse cx="100" cy="156" rx="92" ry="14" fill="#000" opacity=".08"/>
    <g transform="translate(6 40) scale(.52)">${inner(burger(["top", "lettuce", "cheese", "beef", "mid", "lettuce", "beef", "bottom"]))}</g>
    <g transform="translate(${large ? 80 : 84} ${large ? 16 : 24}) scale(${large ? 0.56 : 0.5})">${inner(fries(large ? "L" : "M"))}</g>
    <g transform="translate(${large ? 122 : 126} ${large ? 30 : 40}) scale(${large ? 0.5 : 0.44})">${inner(soda(RED))}</g>
    <path d="M10 130 H190 L178 156 H22 Z" fill="#7A5230"/><path d="M10 130 H190 L188 136 H12 Z" fill="#94653D"/>`);
}

/** 맥모닝 세트: 머핀 + 해시브라운 + 커피 */
function morningSet() {
  return svg(`<ellipse cx="100" cy="156" rx="92" ry="14" fill="#000" opacity=".08"/>
    <g transform="translate(4 44) scale(.5)">${inner(burger(["top", "egg", "cheese", "bottom"], { muffin: true }))}</g>
    <g transform="translate(78 64) scale(.42)">${inner(baseArt("hashbrown"))}</g>
    <g transform="translate(118 30) scale(.48)">${inner(cafe())}</g>
    <path d="M10 130 H190 L178 156 H22 Z" fill="#7A5230"/><path d="M10 130 H190 L188 136 H12 Z" fill="#94653D"/>`);
}

const B = (layers, opts) => () => burger(layers, opts);

const ART = {
  logo: () => svg(arches(14, 24, 1.72)),
  // 버거
  bigmac: B(["top", "lettuce", "cheese", "beef", "mid", "lettuce", "onion", "beef", "bottom"]),
  shanghai: B(["top", "lettuce", "tomato", "chicken", "bottom"]),
  b1955: B(["top", "lettuce", "tomato", "onion", "bacon", "beef", "bottom"]),
  qpc: B(["top", "onion", "cheese", "beef", "cheese", "bottom"]),
  dqpc: B(["top", "onion", "cheese", "beef", "cheese", "beef", "bottom"]),
  crispyDeluxe: B(["top", "lettuce", "tomato", "chicken", "bottom"]),
  crispyClassic: B(["top", "lettuce", "chicken", "bottom"]),
  btd: B(["top", "lettuce", "tomato", "bacon", "cheese", "beef", "bottom"]),
  tcb: B(["top", "lettuce", "tomato", "cheese", "beef", "bottom"]),
  mcchicken: B(["top", "lettuce", "chicken", "bottom"]),
  mcchickenMozza: B(["top", "lettuce", "cheese", "chicken", "bottom"]),
  bulgogi: B(["top", "lettuce", "beef", "bottom"]),
  bulgogi2: B(["top", "lettuce", "beef", "beef", "bottom"]),
  cheese: B(["top", "onion", "cheese", "beef", "bottom"]),
  cheese2: B(["top", "cheese", "beef", "cheese", "beef", "bottom"]),
  hamburger: B(["top", "onion", "beef", "bottom"]),
  shubi: B(["top", "lettuce", "shrimp", "beef", "bottom"]),
  shushu: B(["top", "lettuce", "shrimp", "bottom"]),
  // 맥모닝
  eggMuffin: B(["top", "egg", "cheese", "bottom"], { muffin: true }),
  sausageEggMuffin: B(["top", "egg", "cheese", "sausage", "bottom"], { muffin: true }),
  btMuffin: B(["top", "tomato", "bacon", "egg", "bottom"], { muffin: true }),
  chickenMuffin: B(["top", "cheese", "chicken", "bottom"], { muffin: true }),
  morningSet,
  // 사이드 · 디저트
  friesS: () => fries("S"),
  friesM: () => fries("M"),
  friesL: () => fries("L"),
  fries: () => fries("M"),
  nuggets4: () => nuggets(4),
  nuggets6: () => nuggets(6),
  nuggets10: () => nuggets(10),
  wrap,
  churros,
  mcflurryOreo: () => mcflurry("#2B2B2B"),
  mcflurryBerry: () => mcflurry("#2B2B2B", "#E2405A"),
  mcflurryChoco: () => mcflurry("#2B2B2B", "#5B3417"),
  // 음료
  coke: () => soda("#C8102E"),
  cokeZero: () => soda("#1A1A1A"),
  sprite: () => soda("#00A94F"),
  fanta: () => soda("#F7811E"),
  shakeVanilla: () => shake("#F3E3C3"),
  shakeBerry: () => shake("#F49AB0"),
  shakeChoco: () => shake("#6B4226"),
  dripIced: () => mccup("#3B2210"),
  icedAmericano: () => mccup("#4A2A14"),
  americano: () => cafe(),
  latte: () => cafe("#A9744A"),
  icedLatte: () => mccup("#B98B5E"),
  cappuccino: () => cafe("#7A4A2A"),
  vanillaLatte: () => mccup("#D8B383"),
  oj: () => mccup("#FFA726"),
  // 해피밀 · 꾸밈
  happyBurger: () => happyMeal("burger"),
  happyNugget: () => happyMeal("nugget"),
  bag,
  tray: () => tray(false),
  trayL: () => tray(true),
};

export function art(kind) {
  return ART[kind] ? ART[kind]() : baseArt(kind);
}

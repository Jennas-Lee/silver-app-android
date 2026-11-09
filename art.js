// 메뉴 그림 (SVG). 실제 사진 대신 그린 그림이라 저작권 걱정이 없다.
// art(kind) → <svg> 문자열. viewBox 0 0 200 200.

const BUN = "#E09A3E";
const BUN_DARK = "#B8722A";

function svg(body) {
  return `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">${body}</svg>`;
}

// 버거: 위에서 아래 순서의 층 목록
const LAYER_H = { hash: 18, top: 50, mid: 16, bottom: 26, beef: 20, chicken: 26, shrimp: 24, cheese: 8, lettuce: 10, tomato: 9, bacon: 8, onion: 6, egg: 14, sausage: 16 };

function burger(layers, opts = {}) {
  const h = layers.reduce((s, l) => s + LAYER_H[l], 0);
  let y = 100 - h / 2 + 6;
  const out = [`<ellipse cx="100" cy="${100 + h / 2 + 14}" rx="78" ry="9" fill="#000" opacity=".10"/>`];
  const draw = [];
  for (const l of layers) {
    const lh = LAYER_H[l];
    draw.push([l, y, lh]);
    y += lh;
  }
  // 아래 층부터 그려야 위 층이 덮는다
  for (const [l, y0, lh] of draw.reverse()) out.push(layer(l, y0, lh, opts));
  return svg(out.join(""));
}

function layer(l, y, h, opts) {
  switch (l) {
    case "top": {
      const muffin = opts.muffin;
      const fill = muffin ? "#F1D9A8" : BUN;
      const seeds = muffin
        ? ""
        : [[70, 0.42], [95, 0.3], [122, 0.36], [84, 0.62], [110, 0.58], [140, 0.6], [58, 0.7]]
            .map(([x, t]) => `<ellipse cx="${x}" cy="${y + h * t}" rx="4" ry="2.2" fill="#FFF3D6" transform="rotate(-20 ${x} ${y + h * t})"/>`)
            .join("");
      return `<path d="M26 ${y + h} Q26 ${y - 4} 100 ${y - 4} Q174 ${y - 4} 174 ${y + h} Z" fill="${fill}"/>
        <path d="M44 ${y + h * 0.45} Q60 ${y + 6} 96 ${y + 4}" stroke="#fff" stroke-opacity=".35" stroke-width="7" fill="none" stroke-linecap="round"/>
        <rect x="26" y="${y + h - 6}" width="148" height="6" fill="${muffin ? "#D9B97A" : BUN_DARK}" opacity=".5"/>${seeds}`;
    }
    case "mid":
      return `<rect x="30" y="${y}" width="140" height="${h}" rx="7" fill="${BUN}"/><rect x="30" y="${y + h - 5}" width="140" height="5" fill="${BUN_DARK}" opacity=".5"/>`;
    case "bottom":
      return `<path d="M30 ${y} H170 V${y + h - 10} Q170 ${y + h} 158 ${y + h} H42 Q30 ${y + h} 30 ${y + h - 10} Z" fill="${opts.muffin ? "#F1D9A8" : BUN}"/>
        <rect x="30" y="${y}" width="140" height="5" fill="#fff" opacity=".25"/>`;
    case "beef":
      return `<rect x="24" y="${y}" width="152" height="${h}" rx="10" fill="#5A341C"/>
        <path d="M40 ${y + 7} h22 M76 ${y + 12} h26 M118 ${y + 7} h30" stroke="#3E2210" stroke-width="3" stroke-linecap="round"/>`;
    case "chicken":
      return `<path d="M22 ${y + 6} Q30 ${y - 2} 60 ${y + 2} Q100 ${y - 4} 140 ${y + 1} Q172 ${y - 2} 178 ${y + 8} Q182 ${y + h} 150 ${y + h} H50 Q18 ${y + h} 22 ${y + 6} Z" fill="#D9932F"/>
        <path d="M38 ${y + 10} l6 -3 M64 ${y + 14} l7 -4 M98 ${y + 9} l6 -3 M128 ${y + 15} l7 -4 M156 ${y + 10} l5 -3" stroke="#A86A1A" stroke-width="3" stroke-linecap="round"/>`;
    case "shrimp":
      return `<rect x="24" y="${y}" width="152" height="${h}" rx="12" fill="#EE8A45"/>
        <path d="M44 ${y + 8} q8 8 16 0 M84 ${y + 12} q8 8 16 0 M126 ${y + 8} q8 8 16 0" stroke="#C9612A" stroke-width="3" fill="none"/>`;
    case "sausage":
      return `<ellipse cx="100" cy="${y + h / 2}" rx="74" ry="${h / 2}" fill="#8A4A26"/><path d="M50 ${y + 6} h40 M110 ${y + 9} h36" stroke="#6A3418" stroke-width="3" stroke-linecap="round"/>`;
    case "egg":
      return `<ellipse cx="100" cy="${y + h / 2}" rx="68" ry="${h / 2 + 1}" fill="#FFFDF4"/><ellipse cx="96" cy="${y + h / 2}" rx="22" ry="${h / 2 - 2}" fill="#FFB81C"/>`;
    case "cheese":
      return `<path d="M26 ${y} H174 V${y + h} L162 ${y + h} L150 ${y + h + 12} L140 ${y + h} H92 L80 ${y + h + 14} L68 ${y + h} H26 Z" fill="#FFC21A"/>`;
    case "lettuce":
      return `<path d="M20 ${y + 4} q10 -8 20 0 t20 0 t20 0 t20 0 t20 0 t20 0 t20 0 t20 0 V${y + h} H20 Z" fill="#5DB83F"/>
        <path d="M20 ${y + h} q10 6 20 0 t20 0 t20 0 t20 0 t20 0 t20 0 t20 0 t20 0" fill="#4A9E30"/>`;
    case "tomato":
      return `<rect x="34" y="${y}" width="60" height="${h}" rx="4" fill="#E2412E"/><rect x="104" y="${y}" width="62" height="${h}" rx="4" fill="#E2412E"/>`;
    case "bacon":
      return `<path d="M28 ${y + 4} q12 -6 24 0 t24 0 t24 0 t24 0 t24 0 t24 0 V${y + h} q-12 6 -24 0 t-24 0 t-24 0 t-24 0 t-24 0 t-24 0 Z" fill="#B5442B"/>
        <path d="M28 ${y + 4} q12 -6 24 0 t24 0 t24 0 t24 0 t24 0 t24 0" stroke="#F3B7A0" stroke-width="2" fill="none"/>`;
    case "hash":
      return `<rect x="30" y="${y}" width="140" height="${h}" rx="9" fill="#E3A43C"/><path d="M50 ${y + 7} l8 3 M90 ${y + 10} l8 3 M130 ${y + 6} l8 3" stroke="#B37218" stroke-width="3" stroke-linecap="round"/>`;
    case "onion":
      return `<rect x="30" y="${y}" width="140" height="${h}" rx="3" fill="#F1E6F3"/>`;
    default:
      return "";
  }
}

function fries(color = "#D52B1E") {
  const sticks = [44, 58, 72, 86, 100, 114, 128, 142, 154]
    .map((x, i) => `<rect x="${x}" y="${30 + (i % 3) * 10}" width="12" height="90" rx="3" fill="#FFCE3A" transform="rotate(${(i - 4) * 3} ${x} 120)"/>`)
    .join("");
  return svg(`<ellipse cx="100" cy="184" rx="60" ry="7" fill="#000" opacity=".1"/>${sticks}
    <path d="M40 88 L160 88 L148 180 H52 Z" fill="${color}"/>
    <path d="M40 88 Q100 118 160 88" fill="#fff" opacity=".12"/>
    <circle cx="100" cy="136" r="17" fill="#FFC21A"/><circle cx="100" cy="136" r="8" fill="${color}"/>`);
}

function nuggets(n = 4) {
  const spots = [[70, 120], [120, 116], [92, 92], [140, 90], [62, 88], [110, 140], [150, 132], [80, 146], [128, 66], [76, 64]].slice(0, Math.min(n, 10));
  const box = `<path d="M30 110 H170 L160 176 H40 Z" fill="#FFC21A"/><path d="M30 110 H170 L164 124 H36 Z" fill="#E8A800"/>`;
  const ng = spots
    .map(([x, y], i) => `<path d="M${x - 22} ${y} q4 -18 22 -16 q20 -2 22 14 q2 18 -20 18 q-26 2 -24 -16 Z" fill="${i % 2 ? "#D9932F" : "#E5A443"}"/><path d="M${x - 8} ${y - 6} l5 -2 M${x + 6} ${y + 2} l5 -2" stroke="#B0701E" stroke-width="2.5" stroke-linecap="round"/>`)
    .join("");
  return svg(`<ellipse cx="100" cy="182" rx="66" ry="7" fill="#000" opacity=".1"/>${ng}${box}`);
}

function cup(color, opts = {}) {
  const straw = opts.straw === false ? "" : `<rect x="112" y="14" width="9" height="56" rx="3" fill="#fff" stroke="#ddd" stroke-width="2" transform="rotate(12 116 60)"/>`;
  const lid = opts.dome
    ? `<path d="M50 62 Q100 18 150 62 Z" fill="#fff" opacity=".9" stroke="#ddd" stroke-width="2"/>`
    : `<rect x="46" y="54" width="108" height="14" rx="6" fill="#F3F3F3" stroke="#ddd" stroke-width="2"/>`;
  return svg(`<ellipse cx="100" cy="186" rx="50" ry="6" fill="#000" opacity=".1"/>${straw}${lid}
    <path d="M52 68 H148 L136 182 H64 Z" fill="${color}"/>
    <path d="M60 68 H76 L72 182 H66 Z" fill="#fff" opacity=".18"/>
    ${opts.mark ? `<circle cx="100" cy="120" r="20" fill="#fff" opacity=".85"/><circle cx="100" cy="120" r="11" fill="${opts.mark}"/>` : ""}`);
}

function iced(liquid, opts = {}) {
  const ice = [[78, 92], [108, 100], [92, 120]].map(([x, y]) => `<rect x="${x}" y="${y}" width="22" height="22" rx="4" fill="#fff" opacity=".55" transform="rotate(15 ${x} ${y})"/>`).join("");
  const foam = opts.milk ? `<path d="M56 96 H144 L142 112 H58 Z" fill="#F5E6CF"/>` : "";
  return svg(`<ellipse cx="100" cy="186" rx="48" ry="6" fill="#000" opacity=".1"/>
    <rect x="114" y="10" width="9" height="70" rx="3" fill="#2E7D32" transform="rotate(10 118 60)"/>
    <rect x="46" y="50" width="108" height="12" rx="5" fill="#fff" stroke="#ccc" stroke-width="2"/>
    <path d="M52 62 H148 L136 182 H64 Z" fill="#EAF4FB" stroke="#C8DCE8" stroke-width="2"/>
    <path d="M56 ${opts.milk ? 112 : 90} H144 L136 180 H64 Z" fill="${liquid}"/>${foam}${ice}`);
}

function hotcup(sleeve = "#7A4A2A") {
  return svg(`<ellipse cx="100" cy="186" rx="46" ry="6" fill="#000" opacity=".1"/>
    <path d="M86 40 q-8 -12 2 -22 M106 40 q-8 -12 2 -22" stroke="#bbb" stroke-width="4" fill="none" stroke-linecap="round" opacity=".7"/>
    <rect x="50" y="48" width="100" height="18" rx="6" fill="#3A3A3A"/>
    <path d="M54 66 H146 L134 182 H66 Z" fill="#FAFAFA" stroke="#E2E2E2" stroke-width="2"/>
    <path d="M58 100 H142 L138 140 H62 Z" fill="${sleeve}"/>`);
}

function cone() {
  return svg(`<path d="M74 92 L100 188 L126 92 Z" fill="#E7B568"/>
    <path d="M80 104 L118 142 M88 128 L112 108 M78 100 L122 100" stroke="#C48A3C" stroke-width="3"/>
    <path d="M66 96 Q60 70 84 64 Q78 40 100 36 Q102 18 116 24 Q110 34 120 44 Q142 50 134 70 Q148 84 134 96 Z" fill="#FFFDF5" stroke="#EDE6D6" stroke-width="2"/>`);
}

function sundae(sauce) {
  return svg(`<ellipse cx="100" cy="186" rx="52" ry="6" fill="#000" opacity=".1"/>
    <path d="M58 80 Q54 50 80 46 Q82 26 104 26 Q126 26 124 46 Q148 50 142 80 Z" fill="#FFFDF5" stroke="#EDE6D6" stroke-width="2"/>
    <path d="M62 70 q14 14 26 0 q12 14 24 0 q12 14 26 0 l2 12 H60 Z" fill="${sauce}"/>
    <path d="M52 82 H148 L134 182 H66 Z" fill="#fff" stroke="#E2E2E2" stroke-width="2"/>
    <path d="M60 110 H140 L136 130 H64 Z" fill="${sauce}" opacity=".9"/>`);
}

function pie() {
  return svg(`<ellipse cx="100" cy="160" rx="74" ry="8" fill="#000" opacity=".1"/>
    <rect x="30" y="74" width="140" height="78" rx="18" fill="#E1A04A"/>
    <rect x="30" y="74" width="140" height="20" rx="10" fill="#F0B862"/>
    <path d="M62 104 l14 10 M92 104 l14 10 M122 104 l14 10" stroke="#9B5E1E" stroke-width="5" stroke-linecap="round"/>`);
}

function hashbrown() {
  return svg(`<ellipse cx="100" cy="160" rx="70" ry="8" fill="#000" opacity=".1"/>
    <rect x="34" y="70" width="132" height="82" rx="40" fill="#E3A43C"/>
    <path d="M56 96 l8 4 M84 112 l8 4 M118 92 l8 4 M134 120 l8 4 M70 130 l8 4" stroke="#B37218" stroke-width="4" stroke-linecap="round"/>`);
}

function hotcake() {
  const cakes = [130, 106, 82].map((y) => `<ellipse cx="100" cy="${y + 10}" rx="72" ry="16" fill="#C9822F"/><ellipse cx="100" cy="${y}" rx="72" ry="16" fill="#EBB062"/>`).join("");
  return svg(`<ellipse cx="100" cy="160" rx="84" ry="12" fill="#fff" stroke="#E6E6E6" stroke-width="2"/>${cakes}
    <rect x="86" y="66" width="28" height="14" rx="3" fill="#FFF2B8"/><path d="M60 82 q20 10 40 2 q20 -8 46 6" stroke="#8A4B12" stroke-width="5" fill="none" opacity=".7"/>`);
}

function coleslaw() {
  return svg(`<ellipse cx="100" cy="182" rx="56" ry="6" fill="#000" opacity=".1"/>
    <path d="M46 92 Q100 60 154 92 Z" fill="#E9F3D7"/>
    <circle cx="80" cy="84" r="6" fill="#8BC34A"/><circle cx="104" cy="78" r="5" fill="#FF8A3D"/><circle cx="122" cy="86" r="6" fill="#8BC34A"/>
    <path d="M42 92 H158 L144 178 H56 Z" fill="#fff" stroke="#E2E2E2" stroke-width="2"/>
    <rect x="58" y="120" width="84" height="20" rx="4" fill="#5DB83F"/>`);
}

function cheesestick() {
  const s = (y, r) => `<g transform="rotate(${r} 100 ${y})"><rect x="34" y="${y - 16}" width="132" height="32" rx="16" fill="#E39A33"/><path d="M60 ${y - 6} l6 3 M96 ${y + 4} l6 3 M130 ${y - 6} l6 3" stroke="#B26C17" stroke-width="3" stroke-linecap="round"/></g>`;
  return svg(`<ellipse cx="100" cy="160" rx="72" ry="8" fill="#000" opacity=".1"/>${s(126, -8)}${s(88, 6)}`);
}

function bottle() {
  return svg(`<ellipse cx="100" cy="188" rx="30" ry="5" fill="#000" opacity=".1"/>
    <rect x="88" y="16" width="24" height="16" rx="3" fill="#1E88E5"/>
    <path d="M86 32 H114 Q114 52 130 66 V180 Q130 186 124 186 H76 Q70 186 70 180 V66 Q86 52 86 32 Z" fill="#E3F2FD" stroke="#B3D4EE" stroke-width="2"/>
    <rect x="70" y="100" width="60" height="40" fill="#42A5F5"/>`);
}

function milk() {
  return svg(`<ellipse cx="100" cy="186" rx="44" ry="6" fill="#000" opacity=".1"/>
    <path d="M62 60 L80 34 H120 L138 60 Z" fill="#E8F0FA"/>
    <rect x="62" y="60" width="76" height="122" fill="#fff" stroke="#DDE6F0" stroke-width="2"/>
    <rect x="62" y="110" width="76" height="34" fill="#2F7BD8"/>`);
}

function bag() {
  return svg(`<path d="M70 70 V54 Q70 30 100 30 Q130 30 130 54 V70" stroke="#8D6E63" stroke-width="7" fill="none"/>
    <path d="M44 66 H156 L148 180 H52 Z" fill="#C49A6C"/><path d="M44 66 H156 L154 84 H46 Z" fill="#A87E52"/>
    <circle cx="100" cy="126" r="24" fill="#FFC21A"/><circle cx="100" cy="126" r="13" fill="#D52B1E"/>`);
}

const inner = (s) => s.replace(/^<svg[^>]*>/, "").replace(/<\/svg>$/, "");

function tray() {
  return svg(`<ellipse cx="100" cy="156" rx="92" ry="14" fill="#000" opacity=".08"/>
    <g transform="translate(8 40) scale(.52)">${inner(burger(["top", "lettuce", "cheese", "beef", "bottom"]))}</g>
    <g transform="translate(84 22) scale(.5)">${inner(fries())}</g>
    <g transform="translate(124 38) scale(.44)">${inner(cup("#D52B1E"))}</g>
    <path d="M10 130 H190 L178 156 H22 Z" fill="#8B5A2B"/><path d="M10 130 H190 L188 136 H12 Z" fill="#A86E38"/>`);
}

const ART = {
  bulgogi: () => burger(["top", "lettuce", "beef", "bottom"]),
  bulgogi2: () => burger(["top", "lettuce", "beef", "beef", "bottom"]),
  cheese: () => burger(["top", "onion", "cheese", "beef", "bottom"]),
  cheese2: () => burger(["top", "cheese", "beef", "cheese", "beef", "bottom"]),
  big: () => burger(["top", "lettuce", "cheese", "beef", "mid", "lettuce", "beef", "bottom"]),
  chicken: () => burger(["top", "lettuce", "chicken", "bottom"]),
  spicy: () => burger(["top", "lettuce", "tomato", "chicken", "bottom"]),
  shrimp: () => burger(["top", "lettuce", "shrimp", "bottom"]),
  bacon: () => burger(["top", "lettuce", "tomato", "bacon", "cheese", "beef", "bottom"]),
  eggmuffin: () => burger(["top", "egg", "cheese", "bottom"], { muffin: true }),
  sausagemuffin: () => burger(["top", "egg", "sausage", "bottom"], { muffin: true }),
  hotcake,
  hashbrown,
  fries: () => fries(),
  nuggets4: () => nuggets(4),
  nuggets6: () => nuggets(7),
  coleslaw,
  cheesestick,
  cone,
  sundaeChoco: () => sundae("#5B3417"),
  sundaeBerry: () => sundae("#E2405A"),
  pie,
  shake: () => cup("#F7E7C9", { dome: true }),
  cola: () => cup("#D52B1E", { mark: "#D52B1E" }),
  zero: () => cup("#222", { mark: "#222" }),
  cider: () => cup("#2E9E4F", { mark: "#2E9E4F" }),
  orangesoda: () => cup("#F57C00", { mark: "#F57C00" }),
  oj: () => iced("#FFA726"),
  water: bottle,
  milk,
  americano: () => hotcup(),
  icedAmericano: () => iced("#4A2A14"),
  latte: () => hotcup("#C49A6C"),
  icedLatte: () => iced("#B98B5E", { milk: true }),
  vanillaLatte: () => iced("#D8B383", { milk: true }),
  bag,
  tray,
};

export function art(kind) {
  return (ART[kind] ?? ART.bulgogi)();
}

export { svg, inner, burger, fries, cup, iced, hotcup, cone, coleslaw };

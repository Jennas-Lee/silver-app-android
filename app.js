// 어떤 가게 화면인지: <body data-kit="./chicken"> 처럼 정한다 (없으면 이 폴더의 버거 가게)
const KIT = document.body.dataset.kit ?? ".";
const { art } = await import(`${KIT}/art.js`);
const { BRAND, CATEGORIES, ITEMS, RECOMMENDED, SETS, UPSELL, UI } = await import(`${KIT}/menu.js`);

// ───────── 설정 ─────────
const params = new URLSearchParams(location.search);
/** 아무것도 누르지 않으면 물어보기까지의 초 (?idle=0 이면 끔) */
const IDLE_SEC = Number(params.get("idle") ?? 180);
const IDLE_COUNTDOWN = 30;

// ───────── 상태 ─────────
const initial = () => ({
  screen: "start", // start | menu | review | pay | card | simple | gift | processing | approved | number
  dine: null, // eat | take
  cat: "rec",
  cart: [],
  wiz: null, // 메뉴 고르는 창
  modal: null, // { type, ... }
  pay: null, // card | simple | gift
  gift: "",
  orderNo: null,
  receipt: false,
});
let S = initial();

const $screen = document.getElementById("screen");
const $overlay = document.getElementById("overlay");
const $toast = document.getElementById("toast");
const byId = Object.fromEntries(ITEMS.map((i) => [i.id, i]));

const won = (n) => `${n.toLocaleString("ko-KR")}원`;
const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
const total = () => S.cart.reduce((s, l) => s + l.unit * l.qty, 0);
const count = () => S.cart.reduce((s, l) => s + l.qty, 0);
/** 받침에 맞춰 을/를 */
const eul = (w) => {
  const c = w.charCodeAt(w.length - 1) - 0xac00;
  return w + (c >= 0 && c <= 11171 && c % 28 ? "을" : "를");
};
const DINE = {
  eat: { title: "매장에서 먹기", sub: "매장 식사", chip: "매장 식사", ...UI.dine?.eat },
  take: { title: "포장하기", sub: "가지고 가기", chip: "포장", ...UI.dine?.take },
};
const dineLabel = () => (S.dine === "take" ? DINE.take.chip : DINE.eat.chip);
/** 세트 종류별 추가 금액. 메뉴에 sets: { set: 7600 } 처럼 값이 있으면 그 가격을 쓴다. */
const modeDelta = (item, m) => (item.sets?.[m.id] != null ? item.sets[m.id] - item.price : m.delta);
const setGroup = (item) => item.set && SETS[item.set];

// ───────── 화면 크기: 1080×1920 키오스크 화면을 창에 맞춰 줄인다 ─────────
const $stage = document.getElementById("stage");
function fit() {
  const s = Math.min(innerWidth / 1080, innerHeight / 1920);
  $stage.style.transform = `translate(-50%, -50%) scale(${s})`;
}
addEventListener("resize", fit);
fit();

// ───────── 그리기 ─────────
function render() {
  // 다시 그려도 목록 위치가 그대로 있게
  const keep = {};
  $screen.querySelectorAll("[data-keep]").forEach((el) => (keep[el.dataset.keep] = el.scrollTop));
  $screen.innerHTML = SCREENS[S.screen]();
  $screen.dataset.screen = S.screen;
  $screen.querySelectorAll("[data-keep]").forEach((el) => {
    if (keep[el.dataset.keep] != null) el.scrollTop = keep[el.dataset.keep];
  });
  renderOverlay();
}

function renderOverlay() {
  const html = S.wiz ? wizardHtml() : S.modal ? MODALS[S.modal.type]() : "";
  $overlay.innerHTML = html;
  $overlay.classList.toggle("on", Boolean(html));
}

let toastTimer;
function toast(msg) {
  $toast.textContent = msg;
  $toast.classList.add("on");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => $toast.classList.remove("on"), 2600);
}

const logo = (big = false) => `
  <div class="logo ${big ? "big" : ""}">
    <span class="logo-mark">${art(UI.logoArt)}</span>
    <span class="logo-text">${BRAND.name}</span>
  </div>`;

const header = () => `
  <header class="top">
    ${logo()}
    <div class="dine-chip">${S.dine === "take" ? "🛍️" : "🍽️"} ${dineLabel()}</div>
    <div class="top-actions">
      <button class="ghost" data-a="staff">🔔 직원 호출</button>
      <button class="ghost" data-a="home">⌂ 처음으로</button>
    </div>
  </header>`;

const SCREENS = {
  start: () => `
    <section class="start">
      <div class="start-top">${logo(true)}</div>
      <div class="start-hero">
        <div class="hero-badge">${UI.hero.tag}</div>
        <div class="hero-art">${art(UI.hero.art)}</div>
        <div class="hero-copy">
          <p class="hero-small">${UI.hero.small}</p>
          <h1>${UI.hero.title}</h1>
          <p class="hero-sub">${UI.hero.sub}</p>
        </div>
      </div>
      <div class="start-ask">
        <h2>어디에서 드시겠어요?</h2>
        <p>아래에서 골라 주세요</p>
      </div>
      <div class="start-choices">
        <button class="dine-btn" data-a="dine" data-v="eat">
          <span class="dine-art">${art("tray")}</span>
          <strong>${DINE.eat.title}</strong>
          <small>${DINE.eat.sub}</small>
        </button>
        <button class="dine-btn" data-a="dine" data-v="take">
          <span class="dine-art">${art("bag")}</span>
          <strong>${DINE.take.title}</strong>
          <small>${DINE.take.sub}</small>
        </button>
      </div>
      <div class="start-bottom">
        <div class="langs">
          <button class="lang on">한국어</button>
          <button class="lang" data-a="lang">English</button>
          <button class="lang" data-a="lang">中文</button>
          <button class="lang" data-a="lang">日本語</button>
        </div>
        <button class="ghost light" data-a="staff">🔔 직원 호출</button>
      </div>
      <p class="demo-note">시연용 화면이에요. 실제로 주문하거나 돈이 나가지 않아요.</p>
    </section>`,

  menu: () => {
    const cat = CATEGORIES.find((c) => c.id === S.cat);
    const items = S.cat === "rec" ? RECOMMENDED.map((id) => byId[id]) : ITEMS.filter((i) => i.cat === S.cat);
    return `
    ${header()}
    ${
      UI.layout === "tabs"
        ? `<nav class="tabs">${CATEGORIES.map((c) => `<button class="tab ${c.id === S.cat ? "on" : ""}" data-a="cat" data-v="${c.id}">${c.name}</button>`).join("")}</nav>`
        : ""
    }
    <div class="menu-body">
      ${
        UI.layout === "tabs"
          ? ""
          : `<nav class="rail">
        ${CATEGORIES.map(
          (c) => `<button class="rail-btn ${c.id === S.cat ? "on" : ""}" data-a="cat" data-v="${c.id}">
            <span class="rail-art">${art(c.art)}</span><span>${c.name}</span></button>`,
        ).join("")}
      </nav>`
      }
      <main class="grid-wrap" data-keep="grid-${S.cat}">
        ${
          S.cat === "rec"
            ? `<div class="promo" data-a="item" data-v="${UI.promo.item}">
                <div class="promo-copy"><span class="promo-tag">${UI.promo.tag}</span><h3>${UI.promo.title}</h3><p>${UI.promo.text}</p></div>
                <div class="promo-art">${art(UI.promo.art ?? byId[UI.promo.item].art)}</div></div>`
            : ""
        }
        <h2 class="cat-title">${cat.name}</h2>
        ${cat.note ? `<p class="cat-note">⏰ ${cat.note}</p>` : ""}
        <div class="grid">
          ${items.map(itemCard).join("")}
        </div>
      </main>
    </div>
    ${cartPanel()}`;
  },

  review: () => `
    ${header()}
    <section class="page">
      <h1 class="page-title">주문 내역을 확인해 주세요</h1>
      <div class="review-dine">
        <span>드시는 곳</span><strong>${dineLabel()}</strong>
        <button class="outline small" data-a="toggleDine">바꾸기</button>
      </div>
      <div class="review-list" data-keep="review">
        ${S.cart.map((l, i) => cartLine(l, i, true)).join("")}
      </div>
      <div class="review-total">
        <div><span>주문 수량</span><strong>${count()}개</strong></div>
        <div class="sum"><span>결제할 금액</span><strong>${won(total())}</strong></div>
      </div>
      <div class="page-actions">
        <button class="secondary xl" data-a="toMenu">＋ 추가 주문</button>
        <button class="primary xl" data-a="toPay">결제하기</button>
      </div>
    </section>`,

  pay: () => `
    ${header()}
    <section class="page">
      <h1 class="page-title">결제 방법을 골라 주세요</h1>
      <div class="amount-box"><span>결제할 금액</span><strong>${won(total())}</strong></div>
      <div class="pay-grid">
        <button class="pay-btn" data-a="payWith" data-v="card">
          <span class="pay-ico">💳</span><strong>카드 결제</strong><small>신용카드 · 체크카드</small>
        </button>
        <button class="pay-btn" data-a="payWith" data-v="simple">
          <span class="pay-ico">📱</span><strong>간편 결제</strong><small>휴대폰 페이 · QR · 바코드</small>
        </button>
        <button class="pay-btn" data-a="payWith" data-v="gift">
          <span class="pay-ico">🎁</span><strong>모바일 상품권</strong><small>상품권 번호 입력</small>
        </button>
        <button class="pay-btn disabled" data-a="noCash">
          <span class="pay-ico">💵</span><strong>현금 결제</strong><small>카운터에서 해 주세요</small>
        </button>
      </div>
      <div class="page-actions">
        <button class="secondary xl" data-a="toReview">이전</button>
      </div>
    </section>`,

  card: () => `
    <section class="pay-step">
      <h1>카드를 아래 투입구에<br>끝까지 꽂아 주세요</h1>
      <p class="pay-sub">IC칩이 있는 쪽을 위로 해서 넣어 주세요</p>
      <div class="amount-box"><span>결제할 금액</span><strong>${won(total())}</strong></div>
      <button class="reader" data-a="cardIn" aria-label="카드 꽂기">
        <div class="card-anim"><div class="card"><span class="chip"></span><span class="card-stripe"></span></div></div>
        <div class="slot"><span></span></div>
        <div class="arrow">▼ ▼ ▼</div>
        <p class="reader-label">화면 아래쪽 카드 투입구</p>
      </button>
      <p class="demo-hint">시연: 위의 그림을 누르면 카드를 꽂은 것으로 해요</p>
      <div class="page-actions">
        <button class="secondary xl" data-a="cancelPay">결제 취소</button>
      </div>
    </section>`,

  simple: () => `
    <section class="pay-step">
      <h1>휴대폰의 바코드나 QR을<br>아래 스캐너에 대 주세요</h1>
      <p class="pay-sub">휴대폰에서 페이 앱을 열어 결제 화면을 띄워 주세요</p>
      <div class="amount-box"><span>결제할 금액</span><strong>${won(total())}</strong></div>
      <button class="scanner" data-a="scan" aria-label="바코드 대기">
        <div class="phone"><div class="qr">${qrSvg()}</div><div class="barcode"></div></div>
        <div class="scan-light"></div>
        <p class="reader-label">화면 아래쪽 바코드 스캐너</p>
      </button>
      <p class="demo-hint">시연: 위의 그림을 누르면 바코드를 댄 것으로 해요</p>
      <div class="page-actions">
        <button class="secondary xl" data-a="cancelPay">결제 취소</button>
      </div>
    </section>`,

  gift: () => {
    const g = S.gift.padEnd(12, " ");
    const shown = [0, 4, 8].map((i) => g.slice(i, i + 4)).map((part) => `<span>${esc(part.replace(/ /g, "_"))}</span>`).join("<em>-</em>");
    const keys = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "del", "0", "clear"];
    return `
    <section class="pay-step">
      <h1>모바일 상품권 번호<br>12자리를 눌러 주세요</h1>
      <div class="amount-box"><span>결제할 금액</span><strong>${won(total())}</strong></div>
      <div class="gift-input">${shown}</div>
      <div class="keypad">
        ${keys
          .map((k) =>
            k === "del"
              ? `<button data-a="key" data-v="del">⌫ 지우기</button>`
              : k === "clear"
                ? `<button data-a="key" data-v="clear">모두 지우기</button>`
                : `<button data-a="key" data-v="${k}">${k}</button>`,
          )
          .join("")}
      </div>
      <div class="page-actions">
        <button class="secondary xl" data-a="cancelPay">이전</button>
        <button class="primary xl" data-a="giftOk" ${S.gift.length === 12 ? "" : "disabled"}>확인</button>
      </div>
    </section>`;
  },

  processing: () => `
    <section class="pay-step center">
      <div class="spinner"></div>
      <h1>${S.pay === "gift" ? "상품권을 확인하고 있어요" : "결제하고 있어요"}</h1>
      <p class="pay-sub big">${S.pay === "card" ? "카드를 빼지 마세요" : "잠시만 기다려 주세요"}</p>
    </section>`,

  approved: () => `
    <section class="pay-step center">
      <div class="check">✓</div>
      <h1>결제가 끝났어요</h1>
      <p class="pay-sub big">${S.pay === "card" ? "카드를 빼 주세요" : `${won(total())} 결제되었어요`}</p>
      ${S.pay === "card" ? `<div class="card-out"><div class="card"><span class="chip"></span><span class="card-stripe"></span></div></div>` : ""}
    </section>`,

  number: () => `
    <section class="number">
      ${logo(true)}
      <h1>주문이 끝났어요</h1>
      <div class="order-no"><span>주문 번호</span><strong>${S.orderNo}</strong></div>
      <p class="number-msg">위쪽 화면에 이 번호가 나오면<br>카운터에서 ${S.dine === "take" ? "포장된 음식을" : "음식을"} 받아 가세요</p>
      ${
        S.receipt
          ? `<div class="receipt"><div class="paper">
              <b>${BRAND.name}</b><small>주문 번호 ${S.orderNo} · ${dineLabel()}</small><hr>
              ${S.cart.map((l) => `<div class="r-line"><span>${esc(l.name)} ×${l.qty}</span><span>${won(l.unit * l.qty)}</span></div>`).join("")}
              <hr><div class="r-line total"><span>합계</span><span>${won(total())}</span></div>
              <small>시연용 영수증</small></div></div>
            <p class="receipt-msg">영수증을 챙겨 주세요</p>`
          : ""
      }
      <p class="countdown">잠시 뒤 처음 화면으로 돌아가요 (<span id="back-sec">20</span>초)</p>
      <button class="primary xl" data-a="reset">처음으로</button>
    </section>`,
};

function setLabel(i) {
  const modes = setGroup(i)?.modes ?? [];
  const set = modes.find((m) => m.id === "set");
  return set && modes.some((m) => m.id === "single")
    ? `<span class="item-set">세트 ${won(i.price + modeDelta(i, set))}</span>`
    : `<span class="item-kcal">${i.kcal} kcal</span>`;
}

function itemCard(i) {
  return `
    <button class="card-item ${i.soldOut ? "sold" : ""}" data-a="item" data-v="${i.id}">
      ${i.badge ? `<span class="badge ${i.badge === "매운맛" ? "hot" : i.badge === "NEW" ? "new" : ""}">${i.badge}</span>` : ""}
      <span class="item-art">${art(i.art)}</span>
      <span class="item-name">${i.name}</span>
      <span class="item-price">${won(i.price)}</span>
      ${setLabel(i)}
      ${i.soldOut ? `<span class="sold-tag">품절</span>` : ""}
    </button>`;
}

function cartLine(l, i, big = false) {
  return `
    <div class="line ${big ? "big" : ""}">
      <span class="line-art">${art(l.art)}</span>
      <div class="line-info"><strong>${esc(l.name)}</strong>${l.sub ? `<small>${esc(l.sub)}</small>` : ""}</div>
      <div class="stepper">
        <button data-a="qty" data-v="${i}" data-d="-1" aria-label="하나 빼기">−</button>
        <span>${l.qty}</span>
        <button data-a="qty" data-v="${i}" data-d="1" aria-label="하나 더하기">＋</button>
      </div>
      <span class="line-price">${won(l.unit * l.qty)}</span>
      <button class="line-del" data-a="del" data-v="${i}" aria-label="빼기">✕</button>
    </div>`;
}

function cartPanel() {
  const empty = S.cart.length === 0;
  return `
    <footer class="cart">
      <div class="cart-list" data-keep="cart">
        <div class="cart-head">주문 내역 <b>${count()}</b></div>
        ${empty ? `<p class="cart-empty">메뉴를 눌러서 담아 주세요</p>` : S.cart.map((l, i) => cartLine(l, i)).join("")}
      </div>
      <div class="cart-side">
        <div class="cart-sum"><span>총 주문 금액</span><strong>${won(total())}</strong></div>
        <button class="secondary" data-a="clearCart" ${empty ? "disabled" : ""}>전체 취소</button>
        <button class="primary" data-a="checkout" ${empty ? "disabled" : ""}>주문 완료</button>
      </div>
    </footer>`;
}

function qrSvg() {
  // 그럴듯해 보이는 가짜 QR
  let cells = "";
  let seed = 7;
  const rnd = () => ((seed = (seed * 9301 + 49297) % 233280) / 233280);
  for (let y = 0; y < 21; y++)
    for (let x = 0; x < 21; x++) {
      const finder = (x < 7 && y < 7) || (x > 13 && y < 7) || (x < 7 && y > 13);
      if (finder) continue;
      if (rnd() > 0.52) cells += `<rect x="${x}" y="${y}" width="1" height="1"/>`;
    }
  const finder = (x, y) => `<rect x="${x}" y="${y}" width="7" height="7"/><rect x="${x + 1}" y="${y + 1}" width="5" height="5" fill="#fff"/><rect x="${x + 2}" y="${y + 2}" width="3" height="3"/>`;
  return `<svg viewBox="0 0 21 21" shape-rendering="crispEdges">${finder(0, 0)}${finder(14, 0)}${finder(0, 14)}${cells}</svg>`;
}

// ───────── 메뉴 고르는 창 ─────────
function openItem(id) {
  const item = byId[id];
  if (!item) return;
  if (item.soldOut) return toast("죄송해요. 지금은 품절이에요.");
  const modes = setGroup(item)?.modes;
  S.wiz = { item, step: null, mode: !modes ? "single" : modes.length === 1 ? modes[0].id : null, extra: null, side: null, drink: null, opts: [], qty: 1 };
  S.wiz.step = wizSteps(S.wiz)[0];
  renderOverlay();
}

function wizSteps(w) {
  const steps = [];
  const set = w.item.set && SETS[w.item.set];
  if (set && set.modes.length > 1) steps.push("mode");
  if (set && w.mode && w.mode !== "single") {
    if (set.modes.find((m) => m.id === w.mode)?.extra) steps.push("extra");
    if (set.sides.length > 1) steps.push("side");
    steps.push("drink");
  }
  (w.item.options ?? []).forEach((_, k) => steps.push(`opt${k}`));
  steps.push("confirm");
  return steps;
}

function wizNext() {
  const w = S.wiz;
  const steps = wizSteps(w);
  w.step = steps[steps.indexOf(w.step) + 1] ?? "confirm";
  renderOverlay();
}

function wizBack() {
  const w = S.wiz;
  const steps = wizSteps(w);
  const i = steps.indexOf(w.step);
  if (i <= 0) {
    S.wiz = null;
  } else {
    w.step = steps[i - 1];
  }
  renderOverlay();
}

function wizLine(w) {
  const item = w.item;
  const set = item.set && SETS[item.set];
  const mode = set?.modes.find((m) => m.id === w.mode);
  const side = w.mode !== "single" && set ? (set.sides.length === 1 ? set.sides[0] : set.sides.find((s) => s.id === w.side)) : null;
  const drink = w.mode !== "single" && set ? set.drinks.find((d) => d.id === w.drink) : null;
  const opts = (item.options ?? []).map((o, k) => o.choices[w.opts[k] ?? 0]);
  const extra = w.mode !== "single" && mode?.extra ? mode.extra.choices[w.extra ?? 0] : null;
  const large = Boolean(mode?.large);
  const unit = item.price + (mode ? modeDelta(item, mode) : 0) + (extra?.delta ?? 0) + (side?.delta ?? 0) + (drink?.delta ?? 0) + opts.reduce((s, c) => s + c.delta, 0);
  const name = item.name + (mode?.suffix ?? "");
  const sub = [extra?.label, side && side.label + (large && side.id === "fries" ? " 라지" : ""), drink && drink.label + (large ? " 라지" : ""), ...opts.map((c) => c.label)]
    .filter(Boolean)
    .join(" · ");
  const artKind = opts.find((c) => c.art)?.art ?? item.art;
  return { id: item.id, key: `${item.id}|${w.mode}|${w.extra}|${side?.id}|${drink?.id}|${w.opts.join(",")}`, name, sub, unit, art: artKind, qty: w.qty };
}

function wizardHtml() {
  const w = S.wiz;
  const item = w.item;
  const set = item.set && SETS[item.set];
  const steps = wizSteps(w);
  const idx = steps.indexOf(w.step);
  const progress = `<div class="dots">${steps.map((_, i) => `<i class="${i <= idx ? "on" : ""}"></i>`).join("")}</div>`;
  const head = (title, sub = "") => `
    <div class="sheet-head">
      <span class="sheet-art">${art(wizLine(w).art)}</span>
      <div><p class="sheet-item">${esc(item.name)}</p><h2>${title}</h2>${sub ? `<p class="sheet-sub">${sub}</p>` : ""}</div>
    </div>${progress}`;
  const foot = (extra = "") => `
    <div class="sheet-foot">
      <button class="secondary" data-a="wizBack">${idx === 0 ? "취소" : "이전"}</button>${extra}
    </div>`;
  const choice = (a, v, label, desc, price, artKind, on = false) => `
    <button class="choice ${on ? "on" : ""}" data-a="${a}" data-v="${v}">
      ${artKind ? `<span class="choice-art">${art(artKind)}</span>` : ""}
      <strong>${esc(label)}</strong>${desc ? `<small>${esc(desc)}</small>` : ""}
      ${price != null ? `<span class="choice-price">${price}</span>` : ""}
    </button>`;

  let body = "";
  if (w.step === "mode") {
    body = `${head(set.modeTitle ?? "세트로 드시겠어요?", set.modeSub ?? "세트를 고르면 사이드와 음료를 함께 드려요")}
      <div class="choices ${set.modes.length === 3 ? "three" : "two"}">
        ${set.modes
          .map((m) => choice("wizMode", m.id, m.label, m.desc, won(item.price + modeDelta(item, m)), m.id === "single" ? item.art : (m.art ?? "tray"), w.mode === m.id))
          .join("")}
      </div>${foot()}`;
  } else if (w.step === "extra") {
    const ex = set.modes.find((m) => m.id === w.mode).extra;
    body = `${head(ex.title)}
      <div class="choices ${ex.choices.length === 3 ? "three" : "two"}">
        ${ex.choices.map((c, ci) => choice("wizExtra", ci, c.label, c.desc ?? "", c.delta ? `+${won(c.delta)}` : "추가 금액 없음", c.art, w.extra === ci)).join("")}
      </div>${foot()}`;
  } else if (w.step === "side") {
    body = `${head("사이드를 골라 주세요", set.modes.find((m) => m.id === w.mode)?.large ? (set.largeSideNote ?? "감자튀김은 라지 크기로 드려요") : "")}
      <div class="choices four">
        ${set.sides.map((s) => choice("wizSide", s.id, s.label, "", s.delta ? `+${won(s.delta)}` : "추가 금액 없음", s.art, w.side === s.id)).join("")}
      </div>${foot()}`;
  } else if (w.step === "drink") {
    body = `${head("음료를 골라 주세요", set.modes.find((m) => m.id === w.mode)?.large ? "음료는 라지 크기로 드려요" : "")}
      <div class="choices four">
        ${set.drinks.map((d) => choice("wizDrink", d.id, d.label, "", d.delta ? `+${won(d.delta)}` : "추가 금액 없음", d.art, w.drink === d.id)).join("")}
      </div>${foot()}`;
  } else if (w.step.startsWith("opt")) {
    const k = Number(w.step.slice(3));
    const o = item.options[k];
    body = `${head(o.title)}
      <div class="choices ${o.choices.length === 3 ? "three" : "two"}">
        ${o.choices.map((c, ci) => choice("wizOpt", `${k}:${ci}`, c.label, "", won(item.price + c.delta), c.art ?? item.art, w.opts[k] === ci)).join("")}
      </div>${foot()}`;
  } else {
    const line = wizLine(w);
    body = `${head("이대로 담을까요?")}
      <div class="confirm">
        <span class="confirm-art">${art(line.art)}</span>
        <div class="confirm-info">
          <strong>${esc(line.name)}</strong>
          ${line.sub ? `<small>${esc(line.sub)}</small>` : ""}
          <span class="confirm-unit">${won(line.unit)}</span>
        </div>
        <div class="stepper big">
          <button data-a="wizQty" data-d="-1" aria-label="하나 빼기">−</button>
          <span>${w.qty}</span>
          <button data-a="wizQty" data-d="1" aria-label="하나 더하기">＋</button>
        </div>
      </div>
      <div class="confirm-total"><span>합계</span><strong>${won(line.unit * w.qty)}</strong></div>
      ${foot(`<button class="primary" data-a="wizAdd">주문 내역에 담기</button>`)}`;
  }
  return `<div class="sheet" role="dialog" aria-modal="true">${body}</div>`;
}

function addLine(line) {
  const same = S.cart.find((l) => l.key === line.key);
  if (same) same.qty = Math.min(20, same.qty + line.qty);
  else S.cart.push(line);
}

// ───────── 작은 창 ─────────
const MODALS = {
  home: () => `
    <div class="dialog">
      <h2>처음 화면으로 갈까요?</h2>
      <p>담아 둔 메뉴가 모두 지워져요.</p>
      <div class="dialog-actions">
        <button class="secondary" data-a="close">계속 주문하기</button>
        <button class="primary" data-a="reset">처음으로</button>
      </div>
    </div>`,
  clear: () => `
    <div class="dialog">
      <h2>담은 메뉴를 모두 지울까요?</h2>
      <div class="dialog-actions">
        <button class="secondary" data-a="close">아니요</button>
        <button class="primary" data-a="clearYes">모두 지우기</button>
      </div>
    </div>`,
  upsell: () => `
    <div class="dialog wide">
      <h2>함께 드시면 좋은 메뉴예요</h2>
      <p>누르면 바로 담겨요</p>
      <div class="upsell">
        ${UPSELL.map((id) => {
          const i = byId[id];
          const n = S.cart.filter((l) => l.id === id).reduce((s, l) => s + l.qty, 0);
          return `<button class="upsell-item ${n ? "on" : ""}" data-a="upsellAdd" data-v="${id}">
            <span class="item-art">${art(i.art)}</span><strong>${i.name}</strong><span>${won(i.price)}</span>
            ${n ? `<em>${n}개 담음</em>` : ""}</button>`;
        }).join("")}
      </div>
      <div class="dialog-actions">
        <button class="secondary" data-a="toReview">추가 안 함</button>
        <button class="primary" data-a="toReview">다음</button>
      </div>
    </div>`,
  receipt: () => `
    <div class="dialog">
      <div class="receipt-ico">🧾</div>
      <h2>영수증을 출력할까요?</h2>
      <div class="dialog-actions">
        <button class="secondary" data-a="receipt" data-v="no">출력 안 함</button>
        <button class="primary" data-a="receipt" data-v="yes">영수증 출력</button>
      </div>
    </div>`,
  idle: () => `
    <div class="dialog">
      <h2>주문을 계속하시겠어요?</h2>
      <p><b id="idle-sec">${S.modal.left}</b>초 뒤 처음 화면으로 돌아가요.</p>
      <div class="dialog-actions">
        <button class="secondary" data-a="reset">처음으로</button>
        <button class="primary" data-a="close">계속 주문하기</button>
      </div>
    </div>`,
};

// ───────── 동작 ─────────
let flowTimer;
function later(ms, fn) {
  clearTimeout(flowTimer);
  flowTimer = setTimeout(fn, ms);
}

function go(screen) {
  $toast.classList.remove("on");
  S.screen = screen;
  S.modal = null;
  render();
}

function reset() {
  clearTimeout(flowTimer);
  clearInterval(backTimer);
  S = initial();
  render();
}

function nextOrderNo() {
  let n = 100 + Math.floor(Math.random() * 300);
  try {
    n = Number(localStorage.getItem(`kiosk-order-no-${KIT}`) ?? n) + 1;
    if (n > 999) n = 101;
    localStorage.setItem(`kiosk-order-no-${KIT}`, String(n));
  } catch {}
  return n;
}

function startPayment() {
  go("processing");
  later(2600, () => {
    go("approved");
    later(S.pay === "card" ? 3000 : 2000, () => {
      S.modal = { type: "receipt" };
      renderOverlay();
    });
  });
}

let backTimer;
function finish(receipt) {
  S.receipt = receipt;
  S.orderNo = nextOrderNo();
  go("number");
  let left = 20;
  clearInterval(backTimer);
  backTimer = setInterval(() => {
    left -= 1;
    const el = document.getElementById("back-sec");
    if (el) el.textContent = left;
    if (left <= 0 || S.screen !== "number") {
      clearInterval(backTimer);
      if (S.screen === "number") reset();
    }
  }, 1000);
}

const ACTIONS = {
  dine: (v) => {
    S.dine = v;
    go("menu");
  },
  lang: () => toast("시연용이라 한국어만 쓸 수 있어요"),
  staff: () => toast("직원을 불렀어요. 잠시만 기다려 주세요."),
  home: () => {
    if (S.cart.length === 0) return reset();
    S.modal = { type: "home" };
    renderOverlay();
  },
  reset,
  close: () => {
    S.modal = null;
    renderOverlay();
  },
  cat: (v) => {
    S.cat = v;
    render();
    const g = $screen.querySelector(".grid-wrap");
    if (g) g.scrollTop = 0;
  },
  item: (v) => openItem(v),
  wizBack,
  wizMode: (v) => {
    S.wiz.mode = v;
    wizNext();
  },
  wizExtra: (v) => {
    S.wiz.extra = Number(v);
    wizNext();
  },
  wizSide: (v) => {
    S.wiz.side = v;
    wizNext();
  },
  wizDrink: (v) => {
    S.wiz.drink = v;
    wizNext();
  },
  wizOpt: (v) => {
    const [k, ci] = v.split(":").map(Number);
    S.wiz.opts[k] = ci;
    wizNext();
  },
  wizQty: (_, d) => {
    S.wiz.qty = Math.max(1, Math.min(20, S.wiz.qty + d));
    renderOverlay();
  },
  wizAdd: () => {
    const line = wizLine(S.wiz);
    addLine(line);
    S.wiz = null;
    render();
    toast(`${eul(line.name)} 담았어요`);
  },
  qty: (v, d) => {
    const l = S.cart[Number(v)];
    if (!l) return;
    l.qty += d;
    if (l.qty <= 0) S.cart.splice(Number(v), 1);
    if (l.qty > 20) l.qty = 20;
    if (S.screen === "review" && S.cart.length === 0) return go("menu");
    render();
  },
  del: (v) => {
    S.cart.splice(Number(v), 1);
    if (S.screen === "review" && S.cart.length === 0) return go("menu");
    render();
  },
  clearCart: () => {
    S.modal = { type: "clear" };
    renderOverlay();
  },
  clearYes: () => {
    S.cart = [];
    S.modal = null;
    render();
  },
  checkout: () => {
    if (S.cart.length === 0) return;
    S.modal = { type: "upsell" };
    renderOverlay();
  },
  upsellAdd: (v) => {
    const i = byId[v];
    addLine({ id: i.id, key: `${i.id}|single|||`, name: i.name, sub: "", unit: i.price, art: i.art, qty: 1 });
    renderOverlay();
    render();
    toast(`${eul(i.name)} 담았어요`);
  },
  toReview: () => go("review"),
  toMenu: () => go("menu"),
  toggleDine: () => {
    S.dine = S.dine === "take" ? "eat" : "take";
    render();
  },
  toPay: () => go("pay"),
  noCash: () => toast("현금은 카운터에서 결제해 주세요"),
  payWith: (v) => {
    S.pay = v;
    S.gift = "";
    go(v);
  },
  cancelPay: () => {
    clearTimeout(flowTimer);
    go("pay");
  },
  cardIn: () => {
    $screen.querySelector(".reader")?.classList.add("inserted");
    later(900, startPayment);
  },
  scan: () => {
    $screen.querySelector(".scanner")?.classList.add("scanned");
    later(900, startPayment);
  },
  key: (v) => {
    if (v === "del") S.gift = S.gift.slice(0, -1);
    else if (v === "clear") S.gift = "";
    else if (S.gift.length < 12) S.gift += v;
    render();
  },
  giftOk: () => {
    if (S.gift.length === 12) startPayment();
  },
  receipt: (v) => finish(v === "yes"),
};

document.addEventListener("click", (e) => {
  const el = e.target.closest("[data-a]");
  if (!el || el.disabled) return;
  const fn = ACTIONS[el.dataset.a];
  if (fn) fn(el.dataset.v, Number(el.dataset.d ?? 0));
});

// 창 바깥을 누르면 닫기 (메뉴 고르는 창만)
$overlay.addEventListener("click", (e) => {
  if (e.target === $overlay && S.wiz) {
    S.wiz = null;
    renderOverlay();
  }
});

// ───────── 오래 안 누르면 처음으로 ─────────
let lastInput = Date.now();
addEventListener("pointerdown", () => (lastInput = Date.now()), true);
setInterval(() => {
  if (!IDLE_SEC) return;
  const busy = ["start", "number", "processing", "approved"].includes(S.screen);
  if (busy) return;
  if (S.modal?.type === "idle") {
    S.modal.left -= 1;
    const el = document.getElementById("idle-sec");
    if (el) el.textContent = S.modal.left;
    if (S.modal.left <= 0) reset();
    return;
  }
  if ((Date.now() - lastInput) / 1000 >= IDLE_SEC) {
    S.wiz = null;
    S.modal = { type: "idle", left: IDLE_COUNTDOWN };
    renderOverlay();
  }
}, 1000);

// 미리보기용: ?screen=menu&cat=burger 처럼 바로 열기
const jump = params.get("screen");
if (jump && SCREENS[jump]) {
  S.dine = "eat";
  S.cat = params.get("cat") ?? "rec";
  const d = byId[RECOMMENDED[0]];
  if (jump !== "menu") addLine({ id: d.id, key: "demo", name: d.name, sub: "", unit: d.price, art: d.art, qty: 1 });
  S.screen = jump;
  if (jump === "number") S.orderNo = 123;
}
const openParam = params.get("item");
render();
if (openParam) openItem(openParam);

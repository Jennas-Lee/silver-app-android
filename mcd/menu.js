// 맥도날드 메뉴 (로컬 시연용).
// 가격은 2026년 2월 가격 인상 이후 알려진 값을 기준으로 했고, 알려지지 않은 것은 비슷하게 맞췄다.
// 매장과 시기에 따라 실제 가격과 다를 수 있다.

export const BRAND = { name: "맥도날드", slogan: "i'm lovin' it" };

export const UI = {
  layout: "rail",
  logoArt: "logo",
  hero: { art: "b1955", tag: "BEST", small: "맥도날드 대표 버거", title: "1955 버거", sub: "세트 8,400원" },
  promo: { item: "bigmac", tag: "BEST", title: "빅맥", text: "두 장의 순 쇠고기 패티와 특별한 빅맥 소스" },
  dine: {
    eat: { title: "매장에서 식사", sub: "Eat-in", chip: "매장 식사" },
    take: { title: "포장 주문", sub: "Take-out", chip: "포장" },
  },
};

export const CATEGORIES = [
  { id: "rec", name: "추천 메뉴", art: "bigmac" },
  { id: "burger", name: "버거 & 세트", art: "qpc" },
  { id: "lunch", name: "맥런치", art: "tray", note: "맥런치는 오전 10시 30분부터 오후 2시까지 할인된 가격의 세트예요" },
  { id: "morning", name: "맥모닝", art: "eggMuffin", note: "맥모닝은 새벽 4시부터 오전 10시 30분까지 주문할 수 있어요" },
  { id: "snack", name: "해피 스낵", art: "wrap", note: "가볍게 즐기는 간식 메뉴예요" },
  { id: "side", name: "사이드", art: "friesM" },
  { id: "dessert", name: "디저트", art: "mcflurryOreo" },
  { id: "cafe", name: "맥카페", art: "americano" },
  { id: "drink", name: "음료", art: "coke" },
  { id: "happy", name: "해피밀", art: "happyBurger", note: "해피밀에는 장난감이 1개 들어 있어요" },
];

const SIZE_ML = (l = 500) => ({ title: "크기를 골라 주세요", choices: [{ label: "미디엄", delta: 0 }, { label: "라지", delta: l }] });
const NUGGET_SAUCE = {
  title: "소스를 골라 주세요",
  choices: [{ label: "스위트 앤 사워 소스", delta: 0 }, { label: "스위트 칠리 소스", delta: 0 }, { label: "케이준 소스", delta: 0 }],
};
const HOT_ICE = (hot, ice, l = 500) => [
  { title: "따뜻한 것과 차가운 것 중에 골라 주세요", choices: [{ label: "따뜻하게 (HOT)", delta: 0, art: hot }, { label: "차갑게 (ICE)", delta: 0, art: ice }] },
  SIZE_ML(l),
];
const HAPPY_DRINK = {
  title: "해피밀 음료를 골라 주세요",
  choices: [{ label: "우유", delta: 0, art: "milk" }, { label: "오렌지 주스", delta: 0, art: "oj" }, { label: "코카콜라", delta: 0, art: "coke" }],
};

/** 버거: 단품 가격, sets = { 세트, 라지세트 } 가격 */
const burger = (id, name, price, set, art, extra = {}) => ({ id, cat: "burger", name, price, sets: { set, large: set + 700 }, set: "burger", art, ...extra });
/** 맥런치: 세트 가격 그대로 */
const lunch = (id, name, price, art, kcal) => ({ id: `lunch-${id}`, cat: "lunch", name, price, sets: { large: price + 700 }, set: "lunch", art, kcal, badge: "맥런치" });
/** 맥모닝 */
const morning = (id, name, price, art, kcal, extra = {}) => ({ id, cat: "morning", name, price, set: "morning", art, kcal, ...extra });

export const ITEMS = [
  // 버거 & 세트
  burger("bigmac", "빅맥", 5700, 7600, "bigmac", { kcal: 583, badge: "인기", desc: "100% 순 쇠고기 패티 두 장에 빅맥 소스" }),
  burger("shanghai", "맥스파이시 상하이 버거", 5900, 7700, "shanghai", { kcal: 501, badge: "매운맛", desc: "매콤한 통닭다리살 패티" }),
  burger("b1955", "1955 버거", 6600, 8400, "b1955", { kcal: 545, badge: "인기", desc: "그릴에 구운 양파와 베이컨" }),
  burger("qpc", "쿼터파운더 치즈", 5900, 7900, "qpc", { kcal: 536, desc: "두툼한 쿼터파운더 패티와 치즈 두 장" }),
  burger("dqpc", "더블 쿼터파운더 치즈", 7800, 9600, "dqpc", { kcal: 770, desc: "쿼터파운더 패티가 두 장" }),
  burger("crispyDeluxe", "맥크리스피 디럭스 버거", 6800, 8400, "crispyDeluxe", { kcal: 586, desc: "바삭한 통닭가슴살과 토마토" }),
  burger("crispyClassic", "맥크리스피 클래식 버거", 5900, 7500, "crispyClassic", { kcal: 510, desc: "바삭한 통닭가슴살 그대로" }),
  burger("btd", "베이컨 토마토 디럭스", 6400, 8000, "btd", { kcal: 545, desc: "베이컨과 토마토를 듬뿍" }),
  burger("tcb", "토마토 치즈 비프 버거", 4300, 5900, "tcb", { kcal: 408, desc: "순 쇠고기에 토마토와 치즈" }),
  burger("shubi", "슈비 버거", 6300, 8000, "shubi", { kcal: 563, desc: "통새우와 쇠고기 패티를 함께" }),
  burger("shushu", "슈슈 버거", 5300, 7000, "shushu", { kcal: 436, desc: "통새우가 씹히는 새우 버거" }),
  burger("mcchickenMozza", "맥치킨 모짜렐라", 5000, 6800, "mcchickenMozza", { kcal: 616, desc: "맥치킨에 쭉 늘어나는 모짜렐라" }),
  burger("mcchicken", "맥치킨", 3500, 5600, "mcchicken", { kcal: 487, desc: "부드러운 치킨 패티" }),
  burger("bulgogi2", "더블 불고기 버거", 5100, 6800, "bulgogi2", { kcal: 636, desc: "불고기 패티가 두 장" }),
  burger("bulgogi", "불고기 버거", 3800, 5500, "bulgogi", { kcal: 409, desc: "달콤한 불고기 소스" }),
  burger("cheese2", "더블 치즈버거", 5000, 6300, "cheese2", { kcal: 477, desc: "패티 두 장에 치즈 두 장" }),
  burger("cheese", "치즈버거", 2800, 4900, "cheese", { kcal: 317, desc: "기본에 충실한 치즈버거" }),

  // 맥런치 (세트만)
  lunch("bigmac", "빅맥", 6500, "bigmac", 1023),
  lunch("shanghai", "맥스파이시 상하이 버거", 6600, "shanghai", 941),
  lunch("b1955", "1955 버거", 7300, "b1955", 985),
  lunch("crispyDeluxe", "맥크리스피 디럭스 버거", 7500, "crispyDeluxe", 1026),
  lunch("btd", "베이컨 토마토 디럭스", 7300, "btd", 985),
  lunch("bulgogi2", "더블 불고기 버거", 6300, "bulgogi2", 1076),
  lunch("tcb", "토마토 치즈 비프 버거", 5200, "tcb", 848),

  // 맥모닝
  morning("eggMuffin", "에그 맥머핀", 3500, "eggMuffin", 290, { badge: "인기", desc: "달걀과 치즈, 캐나디안 베이컨" }),
  morning("sausageEggMuffin", "소시지 에그 맥머핀", 4000, "sausageEggMuffin", 440, { desc: "소시지 패티와 달걀" }),
  morning("btMuffin", "베이컨 토마토 머핀", 4500, "btMuffin", 330, { desc: "베이컨과 토마토, 달걀" }),
  morning("chickenMuffin", "치킨 치즈 머핀", 4300, "chickenMuffin", 410, { desc: "바삭한 치킨과 치즈" }),
  morning("hotcake", "핫케익 3조각", 4200, "hotcake", 560, { desc: "버터와 메이플 시럽" }),
  { id: "hashbrown", cat: "morning", name: "해시 브라운", price: 1500, kcal: 145, art: "hashbrown", desc: "바삭한 감자" },

  // 해피 스낵
  { id: "snack-wrap", cat: "snack", name: "상하이 치킨 스낵랩", price: 2700, kcal: 293, art: "wrap", badge: "인기", desc: "매콤한 치킨을 또띠아에 쏙" },
  { id: "snack-nugget", cat: "snack", name: "맥너겟 4조각", price: 2200, kcal: 170, art: "nuggets4", options: [NUGGET_SAUCE] },
  { id: "snack-churros", cat: "snack", name: "한입 초코 츄러스 3조각", price: 2200, kcal: 240, art: "churros", desc: "겉바속촉 초코 츄러스" },
  { id: "snack-shake", cat: "snack", name: "바닐라 쉐이크 미디엄", price: 2200, kcal: 315, art: "shakeVanilla" },
  { id: "snack-drip", cat: "snack", name: "아이스 드립 커피", price: 1500, kcal: 5, art: "dripIced", options: [SIZE_ML(500)] },

  // 사이드
  {
    id: "fries", cat: "side", name: "후렌치 후라이", price: 1900, kcal: 230, art: "friesM", badge: "인기", desc: "갓 튀겨 바삭한 감자",
    options: [{ title: "크기를 골라 주세요", choices: [{ label: "스몰", delta: 0, art: "friesS" }, { label: "미디엄", delta: 700, art: "friesM" }, { label: "라지", delta: 1300, art: "friesL" }] }],
  },
  {
    id: "nuggets", cat: "side", name: "맥너겟", price: 2800, kcal: 170, art: "nuggets4", desc: "한입 크기 치킨",
    options: [{ title: "몇 조각 드릴까요?", choices: [{ label: "4조각", delta: 0, art: "nuggets4" }, { label: "6조각", delta: 1000, art: "nuggets6" }, { label: "10조각", delta: 2800, art: "nuggets10" }] }, NUGGET_SAUCE],
  },
  { id: "wrap", cat: "side", name: "상하이 치킨 스낵랩", price: 3100, kcal: 293, art: "wrap", desc: "매콤한 치킨을 또띠아에 쏙" },
  { id: "cheesestick", cat: "side", name: "골든 모짜렐라 치즈스틱 2조각", price: 3100, kcal: 160, art: "cheesestick", desc: "쭉 늘어나는 치즈" },
  { id: "coleslaw", cat: "side", name: "코울슬로", price: 2400, kcal: 175, art: "coleslaw", desc: "아삭한 양배추 샐러드" },
  { id: "hashbrown2", cat: "side", name: "해시 브라운", price: 1500, kcal: 145, art: "hashbrown", desc: "바삭한 감자" },

  // 디저트
  { id: "cone", cat: "dessert", name: "아이스크림 콘", price: 1500, kcal: 155, art: "cone", badge: "인기", desc: "부드러운 바닐라 아이스크림" },
  { id: "mcflurryOreo", cat: "dessert", name: "오레오 맥플러리", price: 3600, kcal: 333, art: "mcflurryOreo", desc: "오레오 쿠키를 듬뿍" },
  { id: "mcflurryBerry", cat: "dessert", name: "딸기 오레오 맥플러리", price: 3600, kcal: 351, art: "mcflurryBerry", soldOut: true, desc: "새콤달콤 딸기 시럽" },
  { id: "mcflurryChoco", cat: "dessert", name: "초코 오레오 맥플러리", price: 3600, kcal: 372, art: "mcflurryChoco", desc: "진한 초코 시럽" },
  { id: "sundaeChoco", cat: "dessert", name: "초코 선데이 아이스크림", price: 3100, kcal: 316, art: "sundaeChoco" },
  { id: "sundaeBerry", cat: "dessert", name: "딸기 선데이 아이스크림", price: 3100, kcal: 288, art: "sundaeBerry" },
  { id: "churros", cat: "dessert", name: "한입 초코 츄러스 3조각", price: 2500, kcal: 240, art: "churros" },

  // 맥카페
  { id: "americano", cat: "cafe", name: "아메리카노", price: 2700, kcal: 10, art: "americano", badge: "인기", options: HOT_ICE("americano", "icedAmericano") },
  { id: "latte", cat: "cafe", name: "카페라떼", price: 3300, kcal: 160, art: "latte", options: HOT_ICE("latte", "icedLatte") },
  { id: "cappuccino", cat: "cafe", name: "카푸치노", price: 3300, kcal: 140, art: "cappuccino", options: [SIZE_ML(500)] },
  { id: "vanillaLatte", cat: "cafe", name: "바닐라 라떼", price: 3900, kcal: 230, art: "vanillaLatte", options: HOT_ICE("latte", "vanillaLatte") },
  { id: "drip", cat: "cafe", name: "아이스 드립 커피", price: 1700, kcal: 5, art: "dripIced", options: [SIZE_ML(500)] },

  // 음료
  { id: "coke", cat: "drink", name: "코카콜라", price: 2000, kcal: 133, art: "coke", options: [SIZE_ML(500)] },
  { id: "cokeZero", cat: "drink", name: "코카콜라 제로", price: 2000, kcal: 0, art: "cokeZero", options: [SIZE_ML(500)] },
  { id: "sprite", cat: "drink", name: "스프라이트", price: 2000, kcal: 145, art: "sprite", options: [SIZE_ML(500)] },
  { id: "fanta", cat: "drink", name: "환타", price: 2000, kcal: 150, art: "fanta", options: [SIZE_ML(500)] },
  { id: "shakeVanilla", cat: "drink", name: "바닐라 쉐이크", price: 3300, kcal: 315, art: "shakeVanilla", options: [SIZE_ML(600)] },
  { id: "shakeBerry", cat: "drink", name: "딸기 쉐이크", price: 3300, kcal: 330, art: "shakeBerry", options: [SIZE_ML(600)] },
  { id: "shakeChoco", cat: "drink", name: "초코 쉐이크", price: 3300, kcal: 345, art: "shakeChoco", options: [SIZE_ML(600)] },
  { id: "oj", cat: "drink", name: "오렌지 주스", price: 2900, kcal: 110, art: "oj" },
  { id: "water", cat: "drink", name: "생수", price: 1500, kcal: 0, art: "water" },

  // 해피밀
  { id: "happy-bulgogi", cat: "happy", name: "불고기 버거 해피밀", price: 5200, kcal: 640, art: "happyBurger", desc: "불고기 버거 + 후렌치 후라이 스몰 + 음료 + 장난감", options: [HAPPY_DRINK] },
  { id: "happy-cheese", cat: "happy", name: "치즈버거 해피밀", price: 4700, kcal: 548, art: "happyBurger", desc: "치즈버거 + 후렌치 후라이 스몰 + 음료 + 장난감", options: [HAPPY_DRINK] },
  { id: "happy-hamburger", cat: "happy", name: "햄버거 해피밀", price: 4500, kcal: 497, art: "happyBurger", desc: "햄버거 + 후렌치 후라이 스몰 + 음료 + 장난감", options: [HAPPY_DRINK] },
  { id: "happy-nugget", cat: "happy", name: "맥너겟 4조각 해피밀", price: 4700, kcal: 401, art: "happyNugget", desc: "맥너겟 4조각 + 후렌치 후라이 스몰 + 음료 + 장난감", options: [HAPPY_DRINK] },
];

export const RECOMMENDED = ["bigmac", "shanghai", "b1955", "qpc", "crispyDeluxe", "btd", "nuggets", "mcflurryOreo", "cone"];

/** 주문 완료 전에 권하는 메뉴 */
export const UPSELL = ["cone", "mcflurryOreo", "wrap", "cheesestick"];

const SIDES = [
  { id: "fries", label: "후렌치 후라이", art: "friesM", delta: 0 },
  { id: "coleslaw", label: "코울슬로", art: "coleslaw", delta: 0 },
  { id: "cheesestick", label: "골든 모짜렐라 치즈스틱 2조각", art: "cheesestick", delta: 1300 },
  { id: "wrap", label: "상하이 치킨 스낵랩", art: "wrap", delta: 1500 },
];

const DRINKS = [
  { id: "coke", label: "코카콜라", art: "coke", delta: 0 },
  { id: "cokeZero", label: "코카콜라 제로", art: "cokeZero", delta: 0 },
  { id: "sprite", label: "스프라이트", art: "sprite", delta: 0 },
  { id: "fanta", label: "환타", art: "fanta", delta: 0 },
  { id: "drip", label: "아이스 드립 커피", art: "dripIced", delta: 0 },
  { id: "icedAmericano", label: "아이스 아메리카노", art: "icedAmericano", delta: 600 },
  { id: "shakeVanilla", label: "바닐라 쉐이크", art: "shakeVanilla", delta: 1300 },
  { id: "oj", label: "오렌지 주스", art: "oj", delta: 900 },
];

export const SETS = {
  burger: {
    modes: [
      { id: "single", label: "단품", desc: "버거만", delta: 0 },
      { id: "set", label: "세트", desc: "후렌치 후라이 미디엄 + 음료 미디엄", delta: 1900, suffix: " 세트", art: "tray" },
      { id: "large", label: "라지세트", desc: "후렌치 후라이 라지 + 음료 라지", delta: 2600, suffix: " 라지세트", large: true, art: "trayL" },
    ],
    sides: SIDES,
    largeSideNote: "후렌치 후라이는 라지 크기로 드려요",
    drinks: DRINKS,
  },
  lunch: {
    modeTitle: "세트 크기를 골라 주세요",
    modeSub: "맥런치는 세트로만 주문할 수 있어요",
    modes: [
      { id: "set", label: "맥런치 세트", desc: "후렌치 후라이 미디엄 + 음료 미디엄", delta: 0, suffix: " 맥런치 세트", art: "tray" },
      { id: "large", label: "맥런치 라지세트", desc: "후렌치 후라이 라지 + 음료 라지", delta: 700, suffix: " 맥런치 라지세트", large: true, art: "trayL" },
    ],
    sides: SIDES,
    largeSideNote: "후렌치 후라이는 라지 크기로 드려요",
    drinks: DRINKS,
  },
  morning: {
    modes: [
      { id: "single", label: "단품", desc: "메뉴만", delta: 0 },
      { id: "set", label: "세트", desc: "해시 브라운 + 커피 또는 음료", delta: 1300, suffix: " 세트", art: "morningSet" },
    ],
    sides: [{ id: "hashbrown", label: "해시 브라운", art: "hashbrown", delta: 0 }],
    drinks: [
      { id: "americano", label: "아메리카노 (따뜻하게)", art: "americano", delta: 0 },
      { id: "icedAmericano", label: "아이스 아메리카노", art: "icedAmericano", delta: 0 },
      { id: "drip", label: "아이스 드립 커피", art: "dripIced", delta: 0 },
      { id: "latte", label: "카페라떼", art: "latte", delta: 600 },
      { id: "oj", label: "오렌지 주스", art: "oj", delta: 500 },
      { id: "coke", label: "코카콜라", art: "coke", delta: 0 },
    ],
  },
};

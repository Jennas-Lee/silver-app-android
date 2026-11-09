// 치킨 가게 메뉴. 가게 이름과 메뉴는 시연용으로 지어낸 것이다.

export const BRAND = { name: "바삭치킨", slogan: "갓 튀겨 바삭한 치킨" };

export const UI = {
  layout: "tabs",
  logoArt: "logo",
  hero: { art: "bucket9", tag: "인기", small: "온 가족이 함께 나눠 먹는", title: "치킨 9조각 버킷", sub: "24,900원" },
  promo: { item: "tower", tag: "NEW", title: "통살 타워 버거", text: "바삭한 통살에 감자 튀김 한 장을 더했어요" },
};

export const CATEGORIES = [
  { id: "rec", name: "추천 메뉴" },
  { id: "chicken", name: "치킨" },
  { id: "burger", name: "버거" },
  { id: "bucket", name: "버킷", note: "여럿이 나눠 먹기 좋은 큰 통이에요" },
  { id: "side", name: "사이드" },
  { id: "dessert", name: "디저트" },
  { id: "drink", name: "음료" },
];

const FLAVOR = (arts) => ({
  title: "치킨 맛을 골라 주세요",
  choices: [
    { label: "오리지널", delta: 0, art: arts[0] },
    { label: "매운맛", delta: 0, art: arts[1] },
    { label: "반반", delta: 0, art: "half" },
  ],
});
const PIECES = (hot) => ({
  title: "몇 조각 드릴까요?",
  choices: [
    { label: "1조각", delta: 0, art: hot ? "hot1" : "original1" },
    { label: "2조각", delta: 3100, art: hot ? "hot2" : "original2" },
    { label: "3조각", delta: 6000, art: hot ? "hot3" : "original3" },
  ],
});
const DRINK_SIZE = { title: "음료 크기를 골라 주세요", choices: [{ label: "미디엄", delta: 0 }, { label: "라지", delta: 500 }] };

export const ITEMS = [
  // 치킨
  { id: "original", cat: "chicken", name: "오리지널 치킨", price: 3300, kcal: 250, art: "original1", badge: "인기", options: [PIECES(false)], desc: "11가지 양념으로 튀긴 기본 치킨" },
  { id: "hot", cat: "chicken", name: "매운맛 크리스피 치킨", price: 3300, kcal: 280, art: "hot1", badge: "매운맛", options: [PIECES(true)], desc: "매콤하고 더 바삭한 치킨" },
  {
    id: "tenders", cat: "chicken", name: "순살 텐더", price: 4900, kcal: 300, art: "tenders3", desc: "뼈 없는 닭가슴살 튀김",
    options: [{ title: "몇 조각 드릴까요?", choices: [{ label: "3조각", delta: 0, art: "tenders3" }, { label: "5조각", delta: 2900, art: "tenders5" }] }],
  },
  {
    id: "wings", cat: "chicken", name: "매운 날개", price: 4900, kcal: 320, art: "wings4", badge: "매운맛", desc: "매콤한 닭 날개",
    options: [{ title: "몇 조각 드릴까요?", choices: [{ label: "4조각", delta: 0, art: "wings4" }, { label: "8조각", delta: 4400, art: "wings8" }] }],
  },

  // 버거
  { id: "tower", cat: "burger", name: "통살 타워 버거", price: 7600, kcal: 690, art: "towerBurger", badge: "NEW", set: "burger", desc: "통살에 감자 튀김과 치즈를 더한 높은 버거" },
  { id: "spicy", cat: "burger", name: "매콤 통살 버거", price: 5900, kcal: 520, art: "spicyBurger", badge: "인기", set: "burger", desc: "매콤한 통닭다리살 버거" },
  { id: "classic", cat: "burger", name: "통살 클래식 버거", price: 5600, kcal: 480, art: "classicBurger", set: "burger", desc: "바삭한 통닭다리살 그대로" },
  { id: "cheese", cat: "burger", name: "치즈 통살 버거", price: 6300, kcal: 560, art: "cheeseBurger", set: "burger", desc: "고소한 치즈 두 장" },
  { id: "double", cat: "burger", name: "더블 통살 버거", price: 7900, kcal: 820, art: "doubleBurger", set: "burger", desc: "통살 두 장으로 든든하게" },
  { id: "bulgogi", cat: "burger", name: "치킨 불고기 버거", price: 4900, kcal: 450, art: "bulgogiBurger", set: "burger", desc: "달콤한 불고기 소스" },

  // 버킷
  { id: "bucket6", cat: "bucket", name: "치킨 6조각 버킷", price: 17900, kcal: 1500, art: "bucket6", options: [FLAVOR(["bucket6", "bucketHot"])], desc: "두세 명이 먹기 좋아요" },
  { id: "bucket9", cat: "bucket", name: "치킨 9조각 버킷", price: 24900, kcal: 2250, art: "bucket9", badge: "인기", options: [FLAVOR(["bucket9", "bucketHot"])], desc: "온 가족이 함께" },
  { id: "bucket12", cat: "bucket", name: "치킨 12조각 버킷", price: 31900, kcal: 3000, art: "bucket12", options: [FLAVOR(["bucket12", "bucketHot"])], desc: "여럿이 모였을 때" },
  { id: "tenderBucket", cat: "bucket", name: "순살 텐더 10조각 버킷", price: 15900, kcal: 1000, art: "bucketTender", desc: "뼈 없이 편하게" },
  { id: "combo", cat: "bucket", name: "2인 콤보", price: 21900, kcal: 1600, art: "combo", badge: "인기", options: [FLAVOR(["combo", "bucketHot"])], desc: "치킨 4조각 + 감자튀김 2 + 콜라 2" },

  // 사이드
  {
    id: "fries", cat: "side", name: "감자튀김", price: 1900, kcal: 280, art: "friesRed", desc: "짭조름한 감자튀김",
    options: [{ title: "감자튀김 크기를 골라 주세요", choices: [{ label: "레귤러", delta: 0 }, { label: "라지", delta: 900 }] }],
  },
  { id: "biscuit", cat: "side", name: "비스킷", price: 2400, kcal: 230, art: "biscuit", badge: "인기", desc: "따뜻한 비스킷과 딸기잼" },
  { id: "coleslaw", cat: "side", name: "코울슬로", price: 2300, kcal: 170, art: "coleslaw", desc: "아삭한 양배추 샐러드" },
  { id: "cornsalad", cat: "side", name: "콘샐러드", price: 2300, kcal: 160, art: "cornsalad", desc: "달콤한 옥수수" },
  { id: "cheeseball", cat: "side", name: "치즈볼 4개", price: 3300, kcal: 290, art: "cheeseball", soldOut: true, desc: "쭉 늘어나는 치즈" },

  // 디저트
  {
    id: "eggtart", cat: "dessert", name: "에그타르트", price: 2300, kcal: 220, art: "eggtart1", badge: "인기", desc: "겉은 바삭, 속은 부드러운 달걀 크림",
    options: [{ title: "몇 개 드릴까요?", choices: [{ label: "1개", delta: 0, art: "eggtart1" }, { label: "4개", delta: 6300, art: "eggtart4" }] }],
  },
  { id: "cone", cat: "dessert", name: "바닐라 콘", price: 1200, kcal: 150, art: "cone", desc: "부드러운 우유 아이스크림" },

  // 음료
  { id: "cola", cat: "drink", name: "콜라", price: 2200, kcal: 140, art: "cola", options: [DRINK_SIZE] },
  { id: "zero", cat: "drink", name: "제로 콜라", price: 2200, kcal: 0, art: "zero", options: [DRINK_SIZE] },
  { id: "cider", cat: "drink", name: "사이다", price: 2200, kcal: 140, art: "cider", options: [DRINK_SIZE] },
  { id: "oj", cat: "drink", name: "오렌지 주스", price: 2900, kcal: 110, art: "oj" },
  {
    id: "americano", cat: "drink", name: "아메리카노", price: 2300, kcal: 10, art: "americano",
    options: [{ title: "따뜻한 것과 차가운 것 중에 골라 주세요", choices: [{ label: "따뜻하게", delta: 0, art: "americano" }, { label: "차갑게", delta: 0, art: "icedAmericano" }] }],
  },
  { id: "water", cat: "drink", name: "생수", price: 1500, kcal: 0, art: "water" },
];

export const RECOMMENDED = ["tower", "spicy", "original", "hot", "bucket9", "combo", "eggtart", "biscuit", "tenders"];

export const UPSELL = ["eggtart", "biscuit", "cornsalad", "cone"];

const DRINKS = [
  { id: "cola", label: "콜라", art: "cola", delta: 0 },
  { id: "zero", label: "제로 콜라", art: "zero", delta: 0 },
  { id: "cider", label: "사이다", art: "cider", delta: 0 },
  { id: "icedAmericano", label: "아이스 아메리카노", art: "icedAmericano", delta: 500 },
  { id: "oj", label: "오렌지 주스", art: "oj", delta: 900 },
  { id: "water", label: "생수", art: "water", delta: 0 },
];

export const SETS = {
  burger: {
    modes: [
      { id: "single", label: "단품", desc: "버거만", delta: 0 },
      { id: "set", label: "세트", desc: "버거 + 감자튀김 + 음료", delta: 2300, suffix: " 세트", art: "tray" },
      {
        id: "box", label: "박스", desc: "버거 + 치킨 1조각 + 감자튀김 + 음료", delta: 5000, suffix: " 박스", art: "box",
        extra: {
          title: "박스에 넣을 치킨을 골라 주세요",
          choices: [
            { label: "오리지널 치킨", art: "original1", delta: 0 },
            { label: "매운맛 치킨", art: "hot1", delta: 0 },
          ],
        },
      },
    ],
    sides: [
      { id: "fries", label: "감자튀김", art: "friesRed", delta: 0 },
      { id: "coleslaw", label: "코울슬로", art: "coleslaw", delta: 0 },
      { id: "cornsalad", label: "콘샐러드", art: "cornsalad", delta: 0 },
      { id: "biscuit", label: "비스킷", art: "biscuit", delta: 500 },
    ],
    drinks: DRINKS,
  },
};

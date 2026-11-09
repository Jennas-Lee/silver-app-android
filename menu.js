// 메뉴 데이터. 가게 이름과 메뉴는 시연용으로 지어낸 것이다.

export const BRAND = { name: "버거하우스", slogan: "오늘도 맛있게, 버거하우스" };

export const CATEGORIES = [
  { id: "rec", name: "추천 메뉴", art: "bacon" },
  { id: "burger", name: "버거", art: "big" },
  { id: "morning", name: "아침 메뉴", art: "eggmuffin", note: "아침 메뉴는 오전 10시 30분까지 주문할 수 있어요" },
  { id: "side", name: "사이드", art: "fries" },
  { id: "dessert", name: "디저트", art: "cone" },
  { id: "drink", name: "음료", art: "cola" },
  { id: "coffee", name: "커피", art: "americano" },
];

const DRINK_SIZE = { title: "음료 크기를 골라 주세요", choices: [{ label: "미디엄", delta: 0 }, { label: "라지", delta: 500 }] };
const COFFEE_TEMP = { title: "따뜻한 것과 차가운 것 중에 골라 주세요", choices: [{ label: "따뜻하게", delta: 0, art: "americano" }, { label: "차갑게", delta: 0, art: "icedAmericano" }] };

export const ITEMS = [
  // 버거
  { id: "bacon", cat: "burger", name: "베이컨 토마토 버거", price: 6500, kcal: 640, art: "bacon", badge: "NEW", set: "burger", desc: "두툼한 소고기에 바삭한 베이컨과 생토마토" },
  { id: "big", cat: "burger", name: "빅 클래식 버거", price: 6200, kcal: 583, art: "big", badge: "인기", set: "burger", desc: "소고기 두 장, 빵 세 겹의 대표 버거" },
  { id: "bulgogi", cat: "burger", name: "불고기 버거", price: 4000, kcal: 400, art: "bulgogi", set: "burger", desc: "달콤 짭짤한 불고기 소스" },
  { id: "bulgogi2", cat: "burger", name: "더블 불고기 버거", price: 5400, kcal: 620, art: "bulgogi2", set: "burger", desc: "불고기 소고기가 두 장" },
  { id: "cheese", cat: "burger", name: "치즈버거", price: 3800, kcal: 314, art: "cheese", set: "burger", desc: "고소한 치즈 한 장" },
  { id: "cheese2", cat: "burger", name: "더블 치즈버거", price: 5600, kcal: 470, art: "cheese2", set: "burger", desc: "소고기 두 장에 치즈 두 장" },
  { id: "chicken", cat: "burger", name: "치킨 버거", price: 5500, kcal: 507, art: "chicken", set: "burger", desc: "통닭다리살을 바삭하게 튀겼어요" },
  { id: "spicy", cat: "burger", name: "매콤 치킨 버거", price: 5700, kcal: 524, art: "spicy", badge: "매운맛", set: "burger", desc: "매콤한 양념의 닭다리살" },
  { id: "shrimp", cat: "burger", name: "새우 버거", price: 5200, kcal: 470, art: "shrimp", set: "burger", desc: "통새우가 씹히는 새우 버거" },

  // 아침
  { id: "eggmuffin", cat: "morning", name: "에그 머핀", price: 3900, kcal: 290, art: "eggmuffin", set: "morning", desc: "달걀과 치즈를 넣은 아침 머핀" },
  { id: "sausagemuffin", cat: "morning", name: "소시지 에그 머핀", price: 4200, kcal: 420, art: "sausagemuffin", set: "morning", desc: "소시지와 달걀을 함께" },
  { id: "hotcake", cat: "morning", name: "핫케이크 3단", price: 4500, kcal: 560, art: "hotcake", set: "morning", desc: "버터와 시럽을 곁들인 핫케이크" },
  { id: "hashbrown", cat: "morning", name: "해시브라운", price: 1700, kcal: 145, art: "hashbrown", desc: "바삭한 감자 튀김 한 조각" },

  // 사이드
  {
    id: "fries", cat: "side", name: "감자튀김", price: 1500, kcal: 230, art: "fries", desc: "갓 튀긴 감자튀김",
    options: [{ title: "감자튀김 크기를 골라 주세요", choices: [{ label: "스몰", delta: 0 }, { label: "미디엄", delta: 700 }, { label: "라지", delta: 1300 }] }],
  },
  {
    id: "nuggets", cat: "side", name: "치킨 너겟", price: 2800, kcal: 170, art: "nuggets4", badge: "인기", desc: "한입 크기 치킨",
    options: [{ title: "몇 조각으로 드릴까요?", choices: [{ label: "4조각", delta: 0, art: "nuggets4" }, { label: "6조각", delta: 1200, art: "nuggets6" }] }],
  },
  { id: "cheesestick", cat: "side", name: "치즈스틱 2조각", price: 3200, kcal: 160, art: "cheesestick", desc: "쭉 늘어나는 치즈" },
  { id: "coleslaw", cat: "side", name: "코울슬로", price: 2300, kcal: 170, art: "coleslaw", desc: "아삭한 양배추 샐러드" },
  { id: "hashbrown2", cat: "side", name: "해시브라운", price: 1700, kcal: 145, art: "hashbrown", desc: "바삭한 감자 튀김 한 조각" },

  // 디저트
  { id: "cone", cat: "dessert", name: "바닐라 콘", price: 1100, kcal: 150, art: "cone", badge: "인기", desc: "부드러운 우유 아이스크림" },
  { id: "sundaeChoco", cat: "dessert", name: "초코 선데", price: 2900, kcal: 300, art: "sundaeChoco", desc: "진한 초콜릿 소스" },
  { id: "sundaeBerry", cat: "dessert", name: "딸기 선데", price: 2900, kcal: 290, art: "sundaeBerry", soldOut: true, desc: "새콤달콤 딸기 소스" },
  { id: "pie", cat: "dessert", name: "사과 파이", price: 1800, kcal: 250, art: "pie", desc: "따뜻한 사과 조림이 가득" },
  { id: "shake", cat: "dessert", name: "바닐라 쉐이크", price: 3300, kcal: 370, art: "shake", options: [DRINK_SIZE], desc: "진하고 시원한 쉐이크" },

  // 음료
  { id: "cola", cat: "drink", name: "콜라", price: 1900, kcal: 140, art: "cola", options: [DRINK_SIZE] },
  { id: "zero", cat: "drink", name: "제로 콜라", price: 1900, kcal: 0, art: "zero", options: [DRINK_SIZE] },
  { id: "cider", cat: "drink", name: "사이다", price: 1900, kcal: 140, art: "cider", options: [DRINK_SIZE] },
  { id: "orangesoda", cat: "drink", name: "오렌지 탄산", price: 1900, kcal: 150, art: "orangesoda", options: [DRINK_SIZE] },
  { id: "oj", cat: "drink", name: "오렌지 주스", price: 2800, kcal: 110, art: "oj" },
  { id: "water", cat: "drink", name: "생수", price: 1500, kcal: 0, art: "water" },
  { id: "milk", cat: "drink", name: "우유", price: 1500, kcal: 130, art: "milk" },

  // 커피
  { id: "americano", cat: "coffee", name: "아메리카노", price: 2000, kcal: 10, art: "americano", badge: "인기", options: [COFFEE_TEMP, DRINK_SIZE] },
  { id: "latte", cat: "coffee", name: "카페라떼", price: 3000, kcal: 160, art: "latte", options: [{ ...COFFEE_TEMP, choices: [{ label: "따뜻하게", delta: 0, art: "latte" }, { label: "차갑게", delta: 0, art: "icedLatte" }] }, DRINK_SIZE] },
  { id: "vanillaLatte", cat: "coffee", name: "바닐라 라떼", price: 3500, kcal: 220, art: "vanillaLatte", badge: "NEW", options: [DRINK_SIZE] },
];

export const RECOMMENDED = ["bacon", "big", "bulgogi", "chicken", "nuggets", "cone", "americano", "shake", "spicy"];

/** 함께 즐기면 좋은 메뉴 (주문 완료 전에 권함) */
export const UPSELL = ["cone", "pie", "cheesestick", "hashbrown2"];

/** 세트 구성 */
export const SETS = {
  burger: {
    modes: [
      { id: "single", label: "단품", desc: "버거만", delta: 0 },
      { id: "set", label: "세트", desc: "버거 + 감자튀김 + 음료", delta: 2000 },
      { id: "large", label: "라지 세트", desc: "버거 + 감자튀김 라지 + 음료 라지", delta: 2700 },
    ],
    sides: [
      { id: "fries", label: "감자튀김", art: "fries", delta: 0 },
      { id: "coleslaw", label: "코울슬로", art: "coleslaw", delta: 0 },
      { id: "cheesestick", label: "치즈스틱 2조각", art: "cheesestick", delta: 1300 },
      { id: "nuggets", label: "치킨 너겟 4조각", art: "nuggets4", delta: 1000 },
    ],
    drinks: [
      { id: "cola", label: "콜라", art: "cola", delta: 0 },
      { id: "zero", label: "제로 콜라", art: "zero", delta: 0 },
      { id: "cider", label: "사이다", art: "cider", delta: 0 },
      { id: "orangesoda", label: "오렌지 탄산", art: "orangesoda", delta: 0 },
      { id: "icedAmericano", label: "아이스 아메리카노", art: "icedAmericano", delta: 0 },
      { id: "oj", label: "오렌지 주스", art: "oj", delta: 900 },
      { id: "shake", label: "바닐라 쉐이크", art: "shake", delta: 1500 },
      { id: "water", label: "생수", art: "water", delta: 0 },
    ],
  },
  morning: {
    modes: [
      { id: "single", label: "단품", desc: "메뉴만", delta: 0 },
      { id: "set", label: "세트", desc: "메뉴 + 해시브라운 + 커피 또는 음료", delta: 1800 },
    ],
    sides: [{ id: "hashbrown", label: "해시브라운", art: "hashbrown", delta: 0 }],
    drinks: [
      { id: "americano", label: "아메리카노 (따뜻하게)", art: "americano", delta: 0 },
      { id: "icedAmericano", label: "아이스 아메리카노", art: "icedAmericano", delta: 0 },
      { id: "latte", label: "카페라떼", art: "latte", delta: 700 },
      { id: "oj", label: "오렌지 주스", art: "oj", delta: 500 },
      { id: "cola", label: "콜라", art: "cola", delta: 0 },
      { id: "milk", label: "우유", art: "milk", delta: 0 },
    ],
  },
};

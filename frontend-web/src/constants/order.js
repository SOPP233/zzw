export const ORDER_STATUS_MAP = {
  0: "草稿",
  1: "审核中",
  2: "待排产",
  3: "生产中",
  4: "部分入库",
  5: "已发货",
  6: "已完结"
};

export const ORDER_STATUS_OPTIONS = [0, 1, 2, 3, 4, 5, 6].map((value) => ({
  value,
  label: ORDER_STATUS_MAP[value]
}));

// 严格对齐数据库 detail_status：0待排产，1待审核，2织造中
export const ORDER_DETAIL_STATUS_MAP = {
  0: "待排产",
  1: "待审核",
  2: "织造中"
};

// 严格对齐数据库 weaving_mode_status：0待合批，1合批织造，2单网织造
export const WEAVING_MODE_STATUS_MAP = {
  0: "待合批",
  1: "合批织造",
  2: "单网织造"
};

export const ORDER_STATUS_MAP = {
  0: "新建草稿",
  1: "业务审核中",
  2: "待排产",
  3: "生产中",
  4: "部分入库",
  5: "已发货",
  6: "已完结"
};

export const ORDER_STATUS_OPTIONS = Object.entries(ORDER_STATUS_MAP).map(([value, label]) => ({
  value: Number(value),
  label
}));

export const ORDER_DETAIL_STATUS_MAP = {
  0: "草稿",
  1: "审核中",
  2: "待排产",
  3: "生产中",
  4: "部分入库",
  5: "已发货",
  6: "已完结"
};


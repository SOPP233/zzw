export const ORDER_STATUS_MAP = {
  1: "业务审核中",
  2: "待排产",
  3: "生产中",
  4: "部分入库",
  5: "已完结",
  // 兼容历史字典值
  0: "新建草稿",
  6: "已发货"
};

export const ORDER_STATUS_OPTIONS = [1, 2, 3, 4, 5].map((value) => ({
  value,
  label: ORDER_STATUS_MAP[value]
}));

export const ORDER_DETAIL_STATUS_MAP = {
  1: "业务审核中",
  2: "待排产",
  3: "生产中",
  4: "部分入库",
  5: "已完结",
  // 兼容历史字典值
  0: "新建草稿",
  6: "已发货"
};

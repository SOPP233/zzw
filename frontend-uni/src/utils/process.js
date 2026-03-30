export const PROCESS_OPTIONS = [
  { key: "weaving", label: "织造", batchField: "weaving_batch_no", listUrl: "/api/production/weaving-orders" },
  { key: "setting", label: "定型", batchField: "setting_batch_no", listUrl: "/api/production/setting-orders" },
  { key: "cutting", label: "裁网", batchField: "cut_batch_no", listUrl: "/api/production/cutting-orders" },
  { key: "splicing", label: "插接", batchField: "splice_batch_no", listUrl: "/api/production/splicing-orders" },
  { key: "sec-setting", label: "二次定型", batchField: "final_batch_no", listUrl: "/api/production/sec-setting-orders" }
];

export function getProcessOption(processKey) {
  return PROCESS_OPTIONS.find((item) => item.key === processKey) || PROCESS_OPTIONS[0];
}

export function getReportPostUrl(processKey, batchNo) {
  if (processKey === "weaving") return `/api/production/weaving-orders/${batchNo}/report`;
  if (processKey === "setting") return `/api/production/setting-orders/${batchNo}/report`;
  if (processKey === "cutting") return `/api/production/cutting-orders/${batchNo}/report`;
  if (processKey === "splicing") return `/api/production/splicing-orders/${batchNo}/report`;
  return `/api/production/sec-setting-orders/${batchNo}/report`;
}

export function getReportGetUrl(processKey, batchNo) {
  return getReportPostUrl(processKey, batchNo);
}

export function getDefaultForm(processKey, username) {
  const operator = username || "";
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, "0");
  const dd = String(now.getDate()).padStart(2, "0");
  const hh = String(now.getHours()).padStart(2, "0");
  const mi = String(now.getMinutes()).padStart(2, "0");
  const ss = String(now.getSeconds()).padStart(2, "0");
  const nowText = `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`;

  if (processKey === "weaving") {
    return {
      machineId: "",
      operatorId: operator,
      materialBatchNo: "",
      tensionParams: "",
      actualLength: "",
      actualStartTime: nowText
    };
  }
  if (processKey === "setting") {
    return {
      operatorId: operator,
      actualTemp: "",
      settingDuration: "",
      shrinkRate: ""
    };
  }
  if (processKey === "cutting") {
    return {
      operatorId: operator,
      actualCutLen: "",
      actualCutWid: "",
      wasteArea: ""
    };
  }
  if (processKey === "splicing") {
    return {
      operatorId: operator,
      spliceType: "",
      jointStrength: ""
    };
  }
  return {
    operatorId: operator,
    finalLength: "",
    finalWidth: "",
    meshDefectInfo: "",
    qcTriggerFlag: 1
  };
}

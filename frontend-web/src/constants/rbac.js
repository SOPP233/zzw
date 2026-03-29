export const ROLE_CODE = {
  SYSTEM_ADMIN: "SYSTEM_ADMIN",
  LEADER: "LEADER",
  SALES: "SALES",
  DIRECTOR: "DIRECTOR",
  WORKER: "WORKER",
  INVENTORY_ADMIN: "INVENTORY_ADMIN"
};

export const ROLE_OPTIONS = [
  { label: "System Admin", value: ROLE_CODE.SYSTEM_ADMIN },
  { label: "Leader", value: ROLE_CODE.LEADER },
  { label: "Sales", value: ROLE_CODE.SALES },
  { label: "Workshop Director", value: ROLE_CODE.DIRECTOR },
  { label: "Operator", value: ROLE_CODE.WORKER },
  { label: "Inventory Admin", value: ROLE_CODE.INVENTORY_ADMIN }
];

export const ROUTE_ROLE_MAP = {
  "/orders/tracking": [
    ROLE_CODE.SYSTEM_ADMIN,
    ROLE_CODE.LEADER,
    ROLE_CODE.SALES,
    ROLE_CODE.DIRECTOR,
    ROLE_CODE.INVENTORY_ADMIN
  ],
  "/orders/manage": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.SALES],
  "/production/workbench": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR],
  "/production/review": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR],
  "/production/weaving-orders": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR, ROLE_CODE.WORKER],
  "/production/setting-orders": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR, ROLE_CODE.WORKER],
  "/production/cutting-orders": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR, ROLE_CODE.WORKER],
  "/production/splicing-orders": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR, ROLE_CODE.WORKER],
  "/production/sec-setting-orders": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR, ROLE_CODE.WORKER],
  "/basic/products": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR],
  "/basic/equipments": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR],
  "/inventory/ledger": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.SALES, ROLE_CODE.INVENTORY_ADMIN],
  "/inventory/raw-materials": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.SALES, ROLE_CODE.INVENTORY_ADMIN],
  "/inventory/wip": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.SALES, ROLE_CODE.INVENTORY_ADMIN],
  "/inventory/finished-goods": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.SALES, ROLE_CODE.INVENTORY_ADMIN],
  "/stats/dashboard": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER]
};

export const DEFAULT_ROUTE_BY_ROLE = {
  [ROLE_CODE.SYSTEM_ADMIN]: "/orders/manage",
  [ROLE_CODE.LEADER]: "/stats/dashboard",
  [ROLE_CODE.SALES]: "/orders/manage",
  [ROLE_CODE.DIRECTOR]: "/production/workbench",
  [ROLE_CODE.WORKER]: "/production/weaving-orders",
  [ROLE_CODE.INVENTORY_ADMIN]: "/inventory/raw-materials"
};

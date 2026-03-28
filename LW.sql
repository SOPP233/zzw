-- =========================================================
-- 离散制造 MES 核心库表 DDL（MySQL 8.0）
-- 字符集：utf8mb4，排序规则：utf8mb4_0900_ai_ci
-- 说明：
-- 1) 包含核心业务表 + RBAC 基础权限表
-- 2) 包含主键、外键、检查约束及关键索引
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------
-- 0. 按依赖顺序清表（可重复执行）
-- -----------------------------
DROP TABLE IF EXISTS `process_task`;
DROP TABLE IF EXISTS `plan_detail_relation`;
DROP TABLE IF EXISTS `production_plan`;
DROP TABLE IF EXISTS `order_detail`;
DROP TABLE IF EXISTS `order_master`;
DROP TABLE IF EXISTS `contract_master`;
DROP TABLE IF EXISTS `material_inventory`;
DROP TABLE IF EXISTS `sys_role_menu`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_menu`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `customer`;

-- -----------------------------
-- 1. 基础主数据
-- -----------------------------
CREATE TABLE `customer` (
  `customer_id` VARCHAR(64) NOT NULL COMMENT '客户ID（主键）',
  `customer_name` VARCHAR(120) NOT NULL COMMENT '客户名称',
  `contact_name` VARCHAR(64) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '客户状态：1有效，0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`customer_id`),
  KEY `idx_customer_name` (`customer_name`),
  CONSTRAINT `ck_customer_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='客户主数据表';

CREATE TABLE `contract_master` (
  `contract_id` VARCHAR(30) NOT NULL COMMENT '合同号（主键）',
  `customer_id` VARCHAR(64) NOT NULL COMMENT '客户ID（外键）',
  `contract_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '合同总金额',
  `sign_date` DATE NOT NULL COMMENT '签订日期',
  `delivery_address` VARCHAR(255) DEFAULT NULL COMMENT '交付地址',
  `contract_status` TINYINT NOT NULL DEFAULT 1 COMMENT '合同状态：0作废，1生效，2已完成',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`contract_id`),
  KEY `idx_contract_master_customer_id` (`customer_id`),
  KEY `idx_contract_master_status_sign_date` (`contract_status`, `sign_date`),
  CONSTRAINT `fk_contract_master_customer_id`
    FOREIGN KEY (`customer_id`) REFERENCES `customer` (`customer_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `ck_contract_master_status` CHECK (`contract_status` IN (0, 1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='合同主表（订单录入依据）';

-- -----------------------------
-- 2. RBAC 权限模型
-- -----------------------------
CREATE TABLE `sys_role` (
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码（主键）',
  `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
  `role_desc` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '角色状态：1启用，0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_code`),
  UNIQUE KEY `uk_sys_role_name` (`role_name`),
  CONSTRAINT `ck_sys_role_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统角色表';

CREATE TABLE `sys_user` (
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID（主键）',
  `username` VARCHAR(64) NOT NULL COMMENT '登录名（唯一）',
  `password_hash` VARCHAR(100) NOT NULL COMMENT '密码哈希（预留Bcrypt/Argon2等）',
  `real_name` VARCHAR(64) NOT NULL COMMENT '真实姓名',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1正常，0禁用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_status` (`status`),
  CONSTRAINT `ck_sys_user_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户表（密码禁止明文存储）';

CREATE TABLE `sys_menu` (
  `menu_id` VARCHAR(64) NOT NULL COMMENT '菜单/权限ID（主键）',
  `parent_id` VARCHAR(64) DEFAULT NULL COMMENT '父级菜单ID，根节点可为空',
  `menu_name` VARCHAR(100) NOT NULL COMMENT '菜单名称',
  `menu_type` TINYINT NOT NULL COMMENT '类型：1目录，2菜单，3按钮',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '前端路由路径',
  `component` VARCHAR(200) DEFAULT NULL COMMENT '前端组件路径',
  `permission_code` VARCHAR(100) DEFAULT NULL COMMENT '权限标识（如 order:read）',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '同级排序号',
  `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见：1是，0否',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`menu_id`),
  UNIQUE KEY `uk_sys_menu_permission_code` (`permission_code`),
  KEY `idx_sys_menu_parent_id` (`parent_id`),
  KEY `idx_sys_menu_type_status` (`menu_type`, `status`),
  CONSTRAINT `fk_sys_menu_parent_id`
    FOREIGN KEY (`parent_id`) REFERENCES `sys_menu` (`menu_id`)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT `ck_sys_menu_type` CHECK (`menu_type` IN (1, 2, 3)),
  CONSTRAINT `ck_sys_menu_visible` CHECK (`visible` IN (0, 1)),
  CONSTRAINT `ck_sys_menu_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='菜单权限表';

CREATE TABLE `sys_user_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_role` (`user_id`, `role_code`),
  KEY `idx_sys_user_role_role_code` (`role_code`),
  CONSTRAINT `fk_sys_user_role_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_sys_user_role_role_code`
    FOREIGN KEY (`role_code`) REFERENCES `sys_role` (`role_code`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户-角色关联表';

CREATE TABLE `sys_role_menu` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `menu_id` VARCHAR(64) NOT NULL COMMENT '菜单/权限ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_menu` (`role_code`, `menu_id`),
  KEY `idx_sys_role_menu_menu_id` (`menu_id`),
  CONSTRAINT `fk_sys_role_menu_role_code`
    FOREIGN KEY (`role_code`) REFERENCES `sys_role` (`role_code`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_sys_role_menu_menu_id`
    FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`menu_id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='角色-菜单权限关联表';

-- -----------------------------
-- 3. 订单管理
-- -----------------------------
CREATE TABLE `order_master` (
  `order_id` VARCHAR(30) NOT NULL COMMENT '订单号（主键，示例：ORD20260327001）',
  `contract_id` VARCHAR(30) NOT NULL COMMENT '合同编号（外键）',
  `customer_id` VARCHAR(64) NOT NULL COMMENT '客户ID（外键）',
  `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '合同总金额',
  `expected_date` DATE NOT NULL COMMENT '预期交期',
  `order_status` TINYINT NOT NULL DEFAULT 1 COMMENT '订单状态：1审核中，2待排产，3生产中，4部分入库，5已完结',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  KEY `idx_order_master_contract_id` (`contract_id`),
  KEY `idx_order_master_customer_id` (`customer_id`),
  KEY `idx_order_master_status_expected` (`order_status`, `expected_date`),
  CONSTRAINT `fk_order_master_contract_id`
    FOREIGN KEY (`contract_id`) REFERENCES `contract_master` (`contract_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_order_master_customer_id`
    FOREIGN KEY (`customer_id`) REFERENCES `customer` (`customer_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `ck_order_master_status` CHECK (`order_status` IN (1, 2, 3, 4, 5))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='订单主表';

CREATE TABLE `order_detail` (
  `detail_id` VARCHAR(30) NOT NULL COMMENT '订单明细ID（主键）',
  `order_id` VARCHAR(30) NOT NULL COMMENT '订单号（外键关联order_master.order_id）',
  `product_model` VARCHAR(30) NOT NULL COMMENT '产品型号',
  `air_permeability` INT NOT NULL DEFAULT 0 COMMENT '透气量',
  `length_req` INT NOT NULL COMMENT '定制长度要求',
  `width_req` INT NOT NULL COMMENT '定制宽度要求',
  `craft_req` VARCHAR(200) DEFAULT NULL COMMENT '特殊工艺要求',
  `detail_status` TINYINT NOT NULL DEFAULT 1 COMMENT '明细状态（可按业务字典扩展）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_order_detail_order_id` (`order_id`),
  KEY `idx_order_detail_model_status` (`product_model`, `detail_status`),
  KEY `idx_order_detail_model_permeability_status` (`product_model`, `air_permeability`, `detail_status`),
  CONSTRAINT `fk_order_detail_order_id`
    FOREIGN KEY (`order_id`) REFERENCES `order_master` (`order_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='订单明细表（合批排产关键来源）';

-- -----------------------------
-- 4. 生产计划与工序流转
-- -----------------------------
CREATE TABLE `production_plan` (
  `batch_id` VARCHAR(64) NOT NULL COMMENT '生产批次号（主键，示例：BAT260327001）',
  `machine_id` VARCHAR(64) NOT NULL COMMENT '织机/设备编号',
  `plan_start_date` DATE NOT NULL COMMENT '计划开工日期',
  `actual_start_time` DATETIME DEFAULT NULL COMMENT '实际开工时间',
  `plan_status` TINYINT NOT NULL DEFAULT 0 COMMENT '计划状态：0等待领料，1加工中，2已完结，9异常阻塞',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '计划创建人（可关联sys_user.user_id）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`batch_id`),
  KEY `idx_production_plan_machine_status` (`machine_id`, `plan_status`),
  KEY `idx_production_plan_start_date` (`plan_start_date`),
  CONSTRAINT `ck_production_plan_status` CHECK (`plan_status` IN (0, 1, 2, 9))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='生产批次计划表';

CREATE TABLE `plan_detail_relation` (
  `relation_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关系ID（自增主键）',
  `batch_id` VARCHAR(64) NOT NULL COMMENT '批次号（外键）',
  `detail_id` VARCHAR(30) NOT NULL COMMENT '订单明细ID（外键）',
  `allocated_qty` DECIMAL(18,3) DEFAULT NULL COMMENT '该明细分配到该批次的数量',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`relation_id`),
  UNIQUE KEY `uk_plan_detail_batch_detail` (`batch_id`, `detail_id`),
  KEY `idx_plan_detail_detail_id` (`detail_id`),
  KEY `idx_plan_detail_batch_id` (`batch_id`),
  CONSTRAINT `fk_plan_detail_relation_batch_id`
    FOREIGN KEY (`batch_id`) REFERENCES `production_plan` (`batch_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_plan_detail_relation_detail_id`
    FOREIGN KEY (`detail_id`) REFERENCES `order_detail` (`detail_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='批次与订单明细映射表（支持合批/拆批）';

CREATE TABLE `process_task` (
  `task_id` VARCHAR(64) NOT NULL COMMENT '工序任务ID（主键）',
  `batch_id` VARCHAR(64) NOT NULL COMMENT '批次号（外键）',
  `process_type` TINYINT NOT NULL COMMENT '工序类型：1织造，2定型，3裁网，4插接，5二次定型',
  `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作员工号（外键关联sys_user.user_id）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '任务状态：0待接收，1执行中，2完工审批，3已闭环',
  `output_data` JSON DEFAULT NULL COMMENT '工序输出数据(JSON，存放工序差异化参数)',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_process_task_batch_id` (`batch_id`),
  KEY `idx_process_task_operator_status` (`operator_id`, `status`),
  KEY `idx_process_task_type_status` (`process_type`, `status`),
  CONSTRAINT `fk_process_task_batch_id`
    FOREIGN KEY (`batch_id`) REFERENCES `production_plan` (`batch_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_process_task_operator_id`
    FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`user_id`)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT `ck_process_task_type` CHECK (`process_type` IN (1, 2, 3, 4, 5)),
  CONSTRAINT `ck_process_task_status` CHECK (`status` IN (0, 1, 2, 3)),
  CONSTRAINT `ck_process_task_time` CHECK (`end_time` IS NULL OR `start_time` IS NULL OR `end_time` >= `start_time`),
  CONSTRAINT `ck_process_task_output_json` CHECK (`output_data` IS NULL OR JSON_VALID(`output_data`))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='多态工序任务流转表（用JSON承载差异化工艺数据）';

-- -----------------------------
-- 5. 库存账本
-- -----------------------------
CREATE TABLE `material_inventory` (
  `material_id` VARCHAR(64) NOT NULL COMMENT '物料ID（主键）',
  `material_type` VARCHAR(64) NOT NULL COMMENT '物料分类',
  `current_stock` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '当前可用库存',
  `frozen_stock` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '冻结库存（已锁定未领用）',
  `min_stock` INT NOT NULL DEFAULT 0 COMMENT '安全库存阈值',
  `version_no` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '最后更新人',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`material_id`),
  KEY `idx_material_inventory_type` (`material_type`),
  KEY `idx_material_inventory_min_stock` (`min_stock`),
  CONSTRAINT `ck_material_inventory_stock_non_negative`
    CHECK (`current_stock` >= 0 AND `frozen_stock` >= 0 AND `min_stock` >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='物料库存账本表（建议更新时使用 version_no 做乐观锁）';

SET FOREIGN_KEY_CHECKS = 1;

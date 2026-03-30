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
DROP TABLE IF EXISTS `order_detail`;
DROP TABLE IF EXISTS `prd_sec_setting_report`;
DROP TABLE IF EXISTS `prd_sec_setting_process`;
DROP TABLE IF EXISTS `prd_splicing_process`;
DROP TABLE IF EXISTS `prd_splicing_report`;
DROP TABLE IF EXISTS `prd_cutting_record`;
DROP TABLE IF EXISTS `prd_cutting_task`;
DROP TABLE IF EXISTS `map_order_weaving`;
DROP TABLE IF EXISTS `prd_setting_report`;
DROP TABLE IF EXISTS `prd_weaving_report`;
DROP TABLE IF EXISTS `prd_setting_process`;
DROP TABLE IF EXISTS `prd_weaving_process`;
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
  `phone` VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  `dept_id` VARCHAR(64) DEFAULT NULL COMMENT '所属部门标识',
  `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '账号启停状态：1正常，0禁用',
  `open_id` VARCHAR(64) DEFAULT NULL COMMENT '微信OpenID（小程序静默登录绑定）',
  `union_id` VARCHAR(64) DEFAULT NULL COMMENT '微信UnionID（可选）',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  UNIQUE KEY `uk_sys_user_open_id` (`open_id`),
  UNIQUE KEY `uk_sys_user_union_id` (`union_id`),
  KEY `idx_sys_user_is_active` (`is_active`),
  KEY `idx_sys_user_dept_id` (`dept_id`),
  CONSTRAINT `ck_sys_user_is_active` CHECK (`is_active` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户表（鉴权+员工信息+小程序绑定）';

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
  `order_no` VARCHAR(32) NOT NULL COMMENT '内部生产订单全局唯一标识',
  `contract_no` VARCHAR(32) NOT NULL COMMENT '所属合同编号（外键）',
  `expected_date` DATE DEFAULT NULL COMMENT '客户期望整体完工交付日期',
  `order_status` TINYINT(3) NOT NULL DEFAULT 0 COMMENT '宏观状态：0草稿，1审核中，2待排产，3生产中，4部分入库，5已发货，6已完结',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_no`),
  KEY `idx_order_master_contract_no` (`contract_no`),
  KEY `idx_order_master_status_expected` (`order_status`, `expected_date`),
  CONSTRAINT `fk_order_master_contract_no`
    FOREIGN KEY (`contract_no`) REFERENCES `contract_master` (`contract_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `ck_order_master_status` CHECK (`order_status` IN (0, 1, 2, 3, 4, 5, 6))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='订单主表（承载批次级全局属性）';

CREATE TABLE `order_detail` (
  `detail_id` VARCHAR(32) NOT NULL COMMENT '订单明细唯一编码（主键）',
  `order_no` VARCHAR(32) NOT NULL COMMENT '所属订单编号（外键）',
  `product_model` VARCHAR(50) NOT NULL COMMENT '客户定制造纸网型号',
  `air_permeability` INT(11) NOT NULL DEFAULT 0 COMMENT '透气量',
  `req_length` DECIMAL(10,2) NOT NULL COMMENT '要求长度（米）',
  `req_width` DECIMAL(10,2) NOT NULL COMMENT '要求宽度（米）',
  `detail_status` TINYINT(3) NOT NULL DEFAULT 0 COMMENT '流转状态：0待排产，1待审核，2织造中',
  `weaving_mode_status` TINYINT(3) NOT NULL DEFAULT 0 COMMENT '织造显示状态：0待合批，1合批织造，2单网织造',
  `delivered_qty` INT(11) NOT NULL DEFAULT 0 COMMENT '实际已完成入库并交货数量',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_order_detail_order_no` (`order_no`),
  KEY `idx_order_detail_model_status` (`product_model`, `detail_status`),
  KEY `idx_order_detail_model_air_status` (`product_model`, `air_permeability`, `detail_status`),
  CONSTRAINT `fk_order_detail_order_no`
    FOREIGN KEY (`order_no`) REFERENCES `order_master` (`order_no`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `ck_order_detail_status` CHECK (`detail_status` IN (0, 1, 2)),
  CONSTRAINT `ck_order_detail_weaving_mode_status` CHECK (`weaving_mode_status` IN (0, 1, 2)),
  CONSTRAINT `ck_order_detail_delivered_qty` CHECK (`delivered_qty` >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='订单明细表（最小不可分割执行单元）';

-- -----------------------------
-- 4. 工序流转与合批拆批执行模型
-- -----------------------------
CREATE TABLE `prd_weaving_process` (
  `weaving_batch_no` VARCHAR(32) NOT NULL COMMENT '织造大网批次号（主键）',
  `machine_id` VARCHAR(32) NOT NULL COMMENT '执行织造任务的设备编号',
  `operator_id` VARCHAR(32) DEFAULT NULL COMMENT '操作员工号',
  `actual_length` DECIMAL(10,2) DEFAULT NULL COMMENT '实际总下机长度（米）',
  `actual_width` DECIMAL(10,2) DEFAULT NULL COMMENT '实际总下机宽度（米）',
  `process_status` TINYINT(3) NOT NULL COMMENT '工序状态：1待生产，2已完工',
  `completed_at` DATETIME DEFAULT NULL COMMENT '实际织造完工时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`weaving_batch_no`),
  KEY `idx_prd_weaving_machine_status` (`machine_id`, `process_status`),
  KEY `idx_prd_weaving_operator` (`operator_id`),
  CONSTRAINT `ck_prd_weaving_status` CHECK (`process_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='织造执行表（承载大网母卷从开机到下线的数据）';

CREATE TABLE `prd_weaving_report` (
  `weaving_batch_no` VARCHAR(32) NOT NULL COMMENT '扫码获取的织造大网批号（主键）',
  `machine_id` VARCHAR(32) NOT NULL COMMENT '实际运行机台号',
  `operator_id` VARCHAR(32) NOT NULL COMMENT '一线员工扫码开工工号',
  `material_batch_no` VARCHAR(64) DEFAULT NULL COMMENT '经纬线投料批次号（原料追溯）',
  `tension_params` VARCHAR(100) DEFAULT NULL COMMENT '独立送经轴张力参数',
  `actual_length` DECIMAL(10,2) DEFAULT NULL COMMENT '织网完成后实际产出米数',
  `actual_start_time` DATETIME NOT NULL COMMENT '实际开工时间（扫码开工写入）',
  `actual_end_time` DATETIME DEFAULT NULL COMMENT '完工报工时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`weaving_batch_no`),
  KEY `idx_prd_weaving_report_operator` (`operator_id`),
  KEY `idx_prd_weaving_report_machine` (`machine_id`),
  CONSTRAINT `fk_prd_weaving_report_batch_no`
    FOREIGN KEY (`weaving_batch_no`) REFERENCES `prd_weaving_process` (`weaving_batch_no`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_prd_weaving_report_operator_id`
    FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`user_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `ck_prd_weaving_report_time` CHECK (`actual_end_time` IS NULL OR `actual_end_time` >= `actual_start_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='织造部门报工表';

CREATE TABLE `prd_setting_process` (
  `setting_batch_no` VARCHAR(32) NOT NULL COMMENT '定型工序批次号（主键）',
  `weaving_batch_no` VARCHAR(32) NOT NULL COMMENT '上游织造批次号（外键）',
  `operator_id` VARCHAR(32) DEFAULT NULL COMMENT '定型监控员工工号',
  `process_status` TINYINT(3) NOT NULL COMMENT '工序状态：1待生产，2已完工',
  `completed_at` DATETIME DEFAULT NULL COMMENT '定型完工时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`setting_batch_no`),
  UNIQUE KEY `uk_prd_setting_weaving_batch_no` (`weaving_batch_no`),
  KEY `idx_prd_setting_operator` (`operator_id`),
  CONSTRAINT `fk_prd_setting_weaving_batch_no`
    FOREIGN KEY (`weaving_batch_no`) REFERENCES `prd_weaving_process` (`weaving_batch_no`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `ck_prd_setting_status` CHECK (`process_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='定型执行表（与织造母卷一对一流转）';

CREATE TABLE `prd_setting_report` (
  `setting_batch_no` VARCHAR(32) NOT NULL COMMENT '定型工序批次号（主键）',
  `weaving_batch_no` VARCHAR(32) NOT NULL COMMENT '接收上一工序产出的半成品码（唯一）',
  `operator_id` VARCHAR(32) NOT NULL COMMENT '负责定型监控的员工工号',
  `actual_temp` DECIMAL(8,2) NOT NULL COMMENT '实际加热温度记录值',
  `setting_duration` INT(11) NOT NULL COMMENT '实际定型时长（分钟）',
  `shrink_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '定型后网面收缩率',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`setting_batch_no`),
  UNIQUE KEY `uk_prd_setting_report_weaving_batch_no` (`weaving_batch_no`),
  KEY `idx_prd_setting_report_operator` (`operator_id`),
  CONSTRAINT `fk_prd_setting_report_setting_batch_no`
    FOREIGN KEY (`setting_batch_no`) REFERENCES `prd_setting_process` (`setting_batch_no`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_prd_setting_report_weaving_batch_no`
    FOREIGN KEY (`weaving_batch_no`) REFERENCES `prd_weaving_process` (`weaving_batch_no`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_prd_setting_report_operator_id`
    FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`user_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='定型部门报工表';

CREATE TABLE `map_order_weaving` (
  `map_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '映射主键（自增）',
  `detail_id` VARCHAR(32) NOT NULL COMMENT '来源订单明细ID（外键）',
  `weaving_batch_no` VARCHAR(32) NOT NULL COMMENT '去向织造批次号（外键）',
  `map_qty` INT(11) NOT NULL COMMENT '分配到该织造批次的数量',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`map_id`),
  UNIQUE KEY `uk_map_order_weaving_detail_batch` (`detail_id`, `weaving_batch_no`),
  KEY `idx_map_order_weaving_batch` (`weaving_batch_no`),
  CONSTRAINT `fk_map_order_weaving_detail_id`
    FOREIGN KEY (`detail_id`) REFERENCES `order_detail` (`detail_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_map_order_weaving_weaving_batch_no`
    FOREIGN KEY (`weaving_batch_no`) REFERENCES `prd_weaving_process` (`weaving_batch_no`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `ck_map_order_weaving_qty` CHECK (`map_qty` > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='大网织造合批映射表（订单明细到织造批次 N:1）';

CREATE TABLE `prd_cutting_task` (
  `task_id` VARCHAR(32) NOT NULL COMMENT '裁网任务单号（主键）',
  `setting_batch_no` VARCHAR(32) NOT NULL COMMENT '来源定型批号（外键）',
  `source_length` DECIMAL(10,2) DEFAULT NULL COMMENT '接收大网总长度',
  `source_width` DECIMAL(10,2) DEFAULT NULL COMMENT '接收大网总宽度',
  `task_status` TINYINT(3) NOT NULL DEFAULT 1 COMMENT '任务状态：1待裁切，2裁切中，3裁切完工',
  `receive_time` DATETIME DEFAULT NULL COMMENT '接收时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_prd_cutting_task_setting_batch_no` (`setting_batch_no`),
  KEY `idx_prd_cutting_task_status` (`task_status`),
  CONSTRAINT `fk_prd_cutting_task_setting_batch_no`
    FOREIGN KEY (`setting_batch_no`) REFERENCES `prd_setting_process` (`setting_batch_no`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `ck_prd_cutting_task_status` CHECK (`task_status` IN (1, 2, 3))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='裁网任务主表（大网工单）';

CREATE TABLE `prd_cutting_record` (
  `cut_batch_no` VARCHAR(32) NOT NULL COMMENT '小网批次号（主键）',
  `task_id` VARCHAR(32) NOT NULL COMMENT '关联裁网任务单号（外键）',
  `detail_id` VARCHAR(32) NOT NULL COMMENT '映射订单明细ID（外键）',
  `actual_cut_len` DECIMAL(10,2) NOT NULL COMMENT '实际裁切长度',
  `actual_cut_wid` DECIMAL(10,2) NOT NULL COMMENT '实际裁切宽度',
  `operator_id` VARCHAR(32) DEFAULT NULL COMMENT '操作工ID',
  `waste_area` DECIMAL(10,2) DEFAULT NULL COMMENT '损耗面积',
  `process_status` TINYINT(3) NOT NULL DEFAULT 1 COMMENT '工序状态：1待生产，2已完工',
  `cut_time` DATETIME DEFAULT NULL COMMENT '报工时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`cut_batch_no`),
  KEY `idx_prd_cutting_record_task_id` (`task_id`),
  KEY `idx_prd_cutting_record_detail_id` (`detail_id`),
  KEY `idx_prd_cutting_record_operator` (`operator_id`),
  CONSTRAINT `fk_prd_cutting_record_task_id`
    FOREIGN KEY (`task_id`) REFERENCES `prd_cutting_task` (`task_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_prd_cutting_record_detail_id`
    FOREIGN KEY (`detail_id`) REFERENCES `order_detail` (`detail_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_prd_cutting_record_operator_id`
    FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`user_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `ck_prd_cutting_record_status` CHECK (`process_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='裁网执行明细表（小网报工）';

CREATE TABLE `prd_splicing_process` (
  `splice_batch_no` VARCHAR(32) NOT NULL COMMENT '插接工序批次号（主键）',
  `cut_batch_no` VARCHAR(32) NOT NULL COMMENT '裁网批次号（外键且唯一）',
  `operator_id` VARCHAR(32) NOT NULL COMMENT '插接操作员工号',
  `splice_type` VARCHAR(50) DEFAULT NULL COMMENT '插接工艺分类',
  `process_status` TINYINT(3) NOT NULL COMMENT '工序状态：1待生产，2已完工',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`splice_batch_no`),
  UNIQUE KEY `uk_prd_splicing_cut_batch_no` (`cut_batch_no`),
  KEY `idx_prd_splicing_operator` (`operator_id`),
  CONSTRAINT `fk_prd_splicing_cut_batch_no`
    FOREIGN KEY (`cut_batch_no`) REFERENCES `prd_cutting_record` (`cut_batch_no`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `ck_prd_splicing_status` CHECK (`process_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='插接执行表（单张小网 1:1 流转）';

CREATE TABLE `prd_splicing_report` (
  `splice_batch_no` VARCHAR(32) NOT NULL COMMENT '插接工序批号（主键）',
  `cut_batch_no` VARCHAR(32) NOT NULL COMMENT '接收的裁切小网批号（唯一）',
  `operator_id` VARCHAR(32) NOT NULL COMMENT '强绑定操作员工号',
  `splice_type` VARCHAR(50) NOT NULL COMMENT '接头类型',
  `joint_strength` VARCHAR(50) DEFAULT NULL COMMENT '接头强度检测标识',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`splice_batch_no`),
  UNIQUE KEY `uk_prd_splicing_report_cut_batch_no` (`cut_batch_no`),
  KEY `idx_prd_splicing_report_operator` (`operator_id`),
  CONSTRAINT `fk_prd_splicing_report_splice_batch_no`
    FOREIGN KEY (`splice_batch_no`) REFERENCES `prd_splicing_process` (`splice_batch_no`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_prd_splicing_report_cut_batch_no`
    FOREIGN KEY (`cut_batch_no`) REFERENCES `prd_cutting_record` (`cut_batch_no`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_prd_splicing_report_operator_id`
    FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`user_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='插接部门报工表';

CREATE TABLE `prd_sec_setting_process` (
  `final_batch_no` VARCHAR(32) NOT NULL COMMENT '最终成品批次号（主键）',
  `splice_batch_no` VARCHAR(32) NOT NULL COMMENT '插接批次号（外键且唯一）',
  `final_length` DECIMAL(10,2) NOT NULL COMMENT '最终固化后长度',
  `final_width` DECIMAL(10,2) NOT NULL COMMENT '最终固化后宽度',
  `process_status` TINYINT(3) NOT NULL COMMENT '工序状态：1待生产，2已完工',
  `completed_at` DATETIME DEFAULT NULL COMMENT '二次定型完工时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`final_batch_no`),
  UNIQUE KEY `uk_prd_sec_setting_splice_batch_no` (`splice_batch_no`),
  CONSTRAINT `fk_prd_sec_setting_splice_batch_no`
    FOREIGN KEY (`splice_batch_no`) REFERENCES `prd_splicing_process` (`splice_batch_no`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `ck_prd_sec_setting_status` CHECK (`process_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='二次定型与入库前置表（最终 1:1 固化流程）';

CREATE TABLE `prd_sec_setting_report` (
  `final_batch_no` VARCHAR(32) NOT NULL COMMENT '最终产成品批次号（入库码）',
  `splice_batch_no` VARCHAR(32) NOT NULL COMMENT '接收的插接半成品批号（唯一）',
  `operator_id` VARCHAR(32) NOT NULL COMMENT '操作员工号',
  `final_length` DECIMAL(10,2) NOT NULL COMMENT '最终成型尺寸参数（长度）',
  `final_width` DECIMAL(10,2) NOT NULL COMMENT '最终成型尺寸参数（宽度）',
  `mesh_defect_info` VARCHAR(255) DEFAULT NULL COMMENT '网病参数记录',
  `qc_trigger_flag` TINYINT(3) NOT NULL COMMENT '质检申请触发标识',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`final_batch_no`),
  UNIQUE KEY `uk_prd_sec_setting_report_splice_batch_no` (`splice_batch_no`),
  KEY `idx_prd_sec_setting_report_operator` (`operator_id`),
  KEY `idx_prd_sec_setting_report_qc_trigger` (`qc_trigger_flag`),
  CONSTRAINT `fk_prd_sec_setting_report_final_batch_no`
    FOREIGN KEY (`final_batch_no`) REFERENCES `prd_sec_setting_process` (`final_batch_no`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_prd_sec_setting_report_splice_batch_no`
    FOREIGN KEY (`splice_batch_no`) REFERENCES `prd_splicing_process` (`splice_batch_no`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_prd_sec_setting_report_operator_id`
    FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`user_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='二次定型（成品检测）报工表';

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

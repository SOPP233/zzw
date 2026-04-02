\begin{table}[htbp]
    \centering
    \caption{客户信息表}
    \label{tab:auto_1}
    \small
    \begin{tabular}{p{2.4cm}p{3.4cm}p{5.6cm}p{1.2cm}p{1.8cm}}
        \toprule
        \textbf{字段名称} & \textbf{数据类型(长度)} & \textbf{字段描述} & \textbf{主键} & \textbf{可否为空} \\
        \midrule
        customer\_id & varchar(20) & 客户号 & 是 & 否 \\
        \midrule
        customer\_name & varchar(30) & 客户名 & 否 & 否 \\
        \midrule
        contact\_name & varchar(10) & 联系人 & 否 & 是 \\
        \midrule
        contact\_phone & varchar(30) & 联系电话 & 否 & 是 \\
        \midrule
        contact\_email & varchar(30) & 联系邮箱 & 否 & 是 \\
        \midrule
        address & varchar(50) & 地址 & 否 & 是 \\
        \midrule
        created\_at & datetime & 创建时间 & 否 & 否 \\
        \midrule
        updated\_at & datetime & 更新时间 & 否 & 否 \\
        \bottomrule
    \end{tabular}
\end{table}

\begin{table}[htbp]
    \centering
    \caption{订单主表}
    \label{tab:auto_2}
    \small
    \begin{tabular}{p{2.4cm}p{3.4cm}p{5.6cm}p{1.2cm}p{1.8cm}}
        \toprule
        \textbf{字段名称} & \textbf{数据类型(长度)} & \textbf{字段描述} & \textbf{主键} & \textbf{可否为空} \\
        \midrule
        order\_id & varchar(30) & 订单号 & 是 & 否 \\
        \midrule
        contract\_id & varchar(30) & 关联的合同号 & 否 & 否 \\
        \midrule
        expected\_date & date & 预期交货时间 & 否 & 是 \\
        \midrule
        order\_status & tinyint(3) & 订单状态(参照状态字典) & 否 & 否 \\
        \midrule
        created\_at & datetime & 创建时间 & 否 & 否 \\
        \midrule
        updated\_at & datetime & 更新时间 & 否 & 否 \\
        \bottomrule
    \end{tabular}
\end{table}

\begin{table}[htbp]
    \centering
    \caption{订单明细表}
    \label{tab:auto_3}
    \small
    \begin{tabular}{p{2.4cm}p{3.4cm}p{5.6cm}p{1.2cm}p{1.8cm}}
        \toprule
        \textbf{字段名称} & \textbf{数据类型(长度)} & \textbf{字段描述} & \textbf{主键} & \textbf{可否为空} \\
        \midrule
        detail\_id & varchar(30) & 明细编号 & 是 & 否 \\
        \midrule
        order\_id & varchar(30) & 关联的主订单号 & 否 & 否 \\
        \midrule
        product\_model & varchar(30) & 定制的产品型号 & 否 & 否 \\
        \midrule
        length\_req & int(11) & 长度需求 & 否 & 否 \\
        \midrule
        width\_req & int(11) & 宽度需求 & 否 & 否 \\
        \midrule
        craft\_req & varchar(200) & 工艺需求 & 否 & 是 \\
        \midrule
        package\_req & varchar(200) & 包装需求 & 否 & 是 \\
        \bottomrule
    \end{tabular}
\end{table}

\begin{table}[htbp]
    \centering
    \caption{物料库存表}
    \label{tab:auto_4}
    \small
    \begin{tabular}{p{2.4cm}p{3.4cm}p{5.6cm}p{1.2cm}p{1.8cm}}
        \toprule
        \textbf{字段名称} & \textbf{数据类型(长度)} & \textbf{字段描述} & \textbf{主键} & \textbf{可否为空} \\
        \midrule
        material\_id & varchar(50) & 线材/物料型号 & 是 & 否 \\
        \midrule
        material\_type & varchar(200) & 材质分类 & 否 & 否 \\
        \midrule
        diameter & float & 直径参数 & 否 & 否 \\
        \midrule
        min\_stock & int(11) & 最小库存(预警临界值) & 否 & 否 \\
        \midrule
        created\_at & datetime & 创建时间 & 否 & 否 \\
        \midrule
        updated\_at & datetime & 更新时间 & 否 & 否 \\
        \bottomrule
    \end{tabular}
\end{table}

\begin{table}[htbp]
    \centering
    \caption{产品信息表}
    \label{tab:auto_5}
    \small
    \begin{tabular}{p{2.4cm}p{3.4cm}p{5.6cm}p{1.2cm}p{1.8cm}}
        \toprule
        \textbf{字段名称} & \textbf{数据类型(长度)} & \textbf{字段描述} & \textbf{主键} & \textbf{可否为空} \\
        \midrule
        product\_model & varchar(30) & 产品型号 & 是 & 否 \\
        \midrule
        min\_permeability & int(11) & 最小透气度参数 & 否 & 否 \\
        \midrule
        max\_permeability & int(11) & 最大透气度参数 & 否 & 否 \\
        \midrule
        created\_at & datetime & 创建时间 & 否 & 否 \\
        \midrule
        updated\_at & datetime & 更新时间 & 否 & 否 \\
        \bottomrule
    \end{tabular}
\end{table}

\begin{table}[htbp]
    \centering
    \caption{工艺参数设计表}
    \label{tab:auto_6}
    \small
    \begin{tabular}{p{2.4cm}p{3.4cm}p{5.6cm}p{1.2cm}p{1.8cm}}
        \toprule
        \textbf{字段名称} & \textbf{数据类型(长度)} & \textbf{字段描述} & \textbf{主键} & \textbf{可否为空} \\
        \midrule
        process\_id & varchar(30) & 整经工艺单号 & 是 & 否 \\
        \midrule
        product\_model & varchar(30) & 关联的网型号 & 否 & 否 \\
        \midrule
        material\_used & varchar(200) & 线材使用说明 & 否 & 否 \\
        \midrule
        machine\_id & varchar(30) & 指定使用的整经机编号 & 否 & 否 \\
        \midrule
        thread\_count & int(11) & 经盘线根数设定 & 否 & 否 \\
        \midrule
        pan\_count & int(11) & 经盘数量 & 否 & 否 \\
        \midrule
        pan\_width & int(11) & 经盘外宽度 & 否 & 否 \\
        \midrule
        single\_tension & int(11) & 单丝张力控制值 & 否 & 否 \\
        \midrule
        total\_tension & int(11) & 总张力控制值 & 否 & 否 \\
        \midrule
        warping\_length & int(11) & 整经长度 & 否 & 否 \\
        \midrule
        speed & int(11) & 设备运行速度设定 & 否 & 否 \\
        \midrule
        description & varchar(200) & 补充文字说明 & 否 &  \\
        \bottomrule
    \end{tabular}
\end{table}

\begin{table}[htbp]
    \centering
    \caption{仓库信息表}
    \label{tab:auto_7}
    \small
    \begin{tabular}{p{2.4cm}p{3.4cm}p{5.6cm}p{1.2cm}p{1.8cm}}
        \toprule
        \textbf{字段名称} & \textbf{数据类型(长度)} & \textbf{字段描述} & \textbf{主键} & \textbf{可否为空} \\
        \midrule
        final\_batch\_id & varchar(64) & 最终批号(入库批次唯一码) & 是 & 否 \\
        \midrule
        splice\_batch\_id & varchar(64) & 关联的前置插接批次号 & 否 & 否 \\
        \midrule
        order\_id & varchar(64) & 关联的客户销售订单号 & 否 & 否 \\
        \midrule
        final\_length & float & 实际入库量测长度 & 否 & 否 \\
        \midrule
        final\_width & float & 实际入库量测宽度 & 否 & 否 \\
        \midrule
        package\_method & varchar(100) & 包装要求及方式 & 否 & 否 \\
        \midrule
        glue\_process & varchar(100) & 加胶工艺记录 & 否 & 否 \\
        \midrule
        shape\_condition & varchar(200) & 最终二次定型情况说明 & 否 & 否 \\
        \midrule
        qc\_condition & varchar(200) & 质检放行情况及指标记录 & 否 & 否 \\
        \midrule
        inventory\_status & tinyint(3) & 仓库流转状态(如:1-在库,3-已出库) & 否 & 否 \\
        \bottomrule
    \end{tabular}
\end{table}

\begin{table}[htbp]
    \centering
    \caption{生产计划表}
    \label{tab:auto_8}
    \small
    \begin{tabular}{p{2.4cm}p{3.4cm}p{5.6cm}p{1.2cm}p{1.8cm}}
        \toprule
        \textbf{字段名称} & \textbf{数据类型(长度)} & \textbf{字段描述} & \textbf{主键} & \textbf{可否为空} \\
        \midrule
        weaving\_batch\_id & varchar(64) & 织造计划批次号 & 是 & 否 \\
        \midrule
        operator\_id & varchar(64) & 参与排产或执行的员工号 & 否 & 否 \\
        \midrule
        machine\_id & varchar(64) & 分配使用的特定织机编号 & 否 & 否 \\
        \midrule
        warp\_pan\_id & varchar(64) & 计划消耗的半成品经盘号 & 否 & 否 \\
        \midrule
        plan\_start\_date & date & 预期开工日期 & 否 & 否 \\
        \midrule
        plan\_end\_date & date & 预期完工日期 & 否 & 否 \\
        \midrule
        actual\_start\_time & datetime & 一线员工实际扫码报工开工时间 & 否 & 是 \\
        \midrule
        actual\_end\_time & datetime & 实际完工下线时间 & 否 & 是 \\
        \midrule
        plan\_status & tinyint(3) & 计划执行状态(如:等待/进行中/完成) & 否 & 否 \\
        \bottomrule
    \end{tabular}
\end{table}


































\begin{table}[htbp]
    \centering
    \caption{用户管理功能测试用例}
    \label{tab:auto_9}
    \small
    \begin{tabular}{p{1.4cm}p{3.0cm}p{5.2cm}p{3.4cm}p{2.2cm}}
        \toprule
        \textbf{编号} & \textbf{测试描述} & \textbf{操作描述} & \textbf{期望结果} & \textbf{实际结果} \\
        \midrule
        U-01 & 新增用户成功 & 管理员输入唯一账号、姓名、角色并保存 & 提示“保存成功”，列表可查询到该用户 & 保存成功 \\
        \midrule
        U-02 & 新增失败（账号重复） & 输入已存在账号后保存 & 提示“账号已存在”，不写入数据 & 新增失败 \\
        \midrule
        U-03 & 禁用账号生效 & 将用户设为禁用后尝试登录 & 提示账号不可用，登录失败 & 登录失败 \\
        \bottomrule
    \end{tabular}
\end{table}

\begin{table}[htbp]
    \centering
    \caption{用户登录功能}
    \label{tab:auto_10}
    \small
    \begin{tabular}{p{1.4cm}p{3.0cm}p{5.2cm}p{3.4cm}p{2.2cm}}
        \toprule
        \textbf{编号} & \textbf{测试描述} & \textbf{操作描述} & \textbf{期望结果} & \textbf{实际结果} \\
        \midrule
        L-01 & 用户登录成功 & 输入正确账号密码并点击登录 & 返回 token，跳转系统首页 & 登录成功 \\
        \midrule
        L-02 & 登录失败（密码错误） & 输入正确账号+错误密码 & 提示“密码错误”或“账号/密码错误” & 登录失败 \\
        \midrule
        L-03 & 未登录访问拦截 & 未登录直接访问业务页 & 跳转登录页或提示无权限 & 拦截成功 \\
        \bottomrule
    \end{tabular}
\end{table}

\begin{table}[htbp]
    \centering
    \caption{订单录入功能}
    \label{tab:auto_11}
    \small
    \begin{tabular}{p{1.4cm}p{3.0cm}p{5.2cm}p{3.4cm}p{2.2cm}}
        \toprule
        \textbf{编号} & \textbf{测试描述} & \textbf{操作描述} & \textbf{期望结果} & \textbf{实际结果} \\
        \midrule
        O-01 & 明细订单录入成功 & 选择合同并录入型号、尺寸、数量后提交 & 主单/明细写入成功，列表可见 & 录入成功 \\
        \midrule
        O-02 & 录入失败（必填缺失） & 缺少关键字段（如型号或尺寸）提交 & 提示必填项错误，不写入数据 & 录入失败 \\
        \midrule
        O-03 & 主从关系正确 & 同一合同连续录入多条明细 & 明细均归属同一订单主单 & 关联正确 \\
        \midrule
        O-04 & 明细编号规则正确 & 新增明细并检查编号 & 明细号按规则生成且不重复 & 生成正确 \\
        \bottomrule
    \end{tabular}
\end{table}

\begin{table}[htbp]
    \centering
    \caption{订单审批功能}
    \label{tab:auto_12}
    \small
    \begin{tabular}{p{1.4cm}p{3.0cm}p{5.2cm}p{3.4cm}p{2.2cm}}
        \toprule
        \textbf{编号} & \textbf{测试描述} & \textbf{操作描述} & \textbf{期望结果} & \textbf{实际结果} \\
        \midrule
        A-01 & 排产后进入审核区 & 在排产区将多张小单合并下发 & 生成织造大单并进入审核区 & 流转成功 \\
        \midrule
        A-02 & 审核通过后流入生产 & 对待审核批次点击“审核通过” & 订单进入对应工序生产区 & 审核成功 \\
        \midrule
        A-03 & 映射关系可追溯 & 查看织造大单“相关订单” & 可查询合并来源明细订单 & 查询成功 \\
        \bottomrule
    \end{tabular}
\end{table}

\begin{table}[htbp]
    \centering
    \caption{完工报工功能}
    \label{tab:auto_13}
    \small
    \begin{tabular}{p{1.4cm}p{3.0cm}p{5.2cm}p{3.4cm}p{2.2cm}}
        \toprule
        \textbf{编号} & \textbf{测试描述} & \textbf{操作描述} & \textbf{期望结果} & \textbf{实际结果} \\
        \midrule
        R-01 & 报工提交成功 & 在生产区填写参数并提交报工 & 状态变为已完工，记录写入报工表 & 提交成功 \\
        \midrule
        R-02 & 报工失败（字段为空） & 缺失关键字段提交 & 提示参数不能为空，提交失败 & 拦截成功 \\
        \midrule
        R-03 & 完工时间自动生成 & 不手动填写完工时间直接提交 & 系统自动写入时间戳 & 生成成功 \\
        \midrule
        R-04 & 工序流转触发 & 当前工序报工完成后检查下一工序 & 下一工序任务自动激活 & 激活成功 \\
        \midrule
        R-05 & 裁网大网拆分小网 & 裁网任务完工后查看完工区 & 大网隐藏，生成关联明细小网记录 & 拆分成功 \\
        \bottomrule
    \end{tabular}
\end{table}

\begin{table}[htbp]
    \centering
    \caption{订单状态追踪功能}
    \label{tab:auto_14}
    \small
    \begin{tabular}{p{1.4cm}p{3.0cm}p{5.2cm}p{3.4cm}p{2.2cm}}
        \toprule
        \textbf{编号} & \textbf{测试描述} & \textbf{操作描述} & \textbf{期望结果} & \textbf{实际结果} \\
        \midrule
        T-01 & 工序状态展示正确 & 选择一条已进入生产的订单查看追踪页 & 显示当前工序与任务状态 & 显示正确 \\
        \midrule
        T-02 & 状态随报工实时更新 & 在生产区提交当前工序报工后刷新追踪页 & 当前工序状态更新，下一工序状态同步变化 & 更新成功 \\
        \midrule
        T-03 & 裁网拆分后追踪正确 & 完成裁网报工后查看订单追踪 & 由大网状态切换为对应小网明细状态，并保持明细关联 & 追踪正确 \\
        \bottomrule
    \end{tabular}
\end{table}

package com.lw.backend.controller;

import com.lw.backend.modules.mes.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/production")
@RequiredArgsConstructor
public class ProductionOrderController {
    private static final int WEAVING_DONE=2,SETTING_PENDING=1,SETTING_DONE=2,CUTTING_PENDING=1,CUTTING_DONE=2,SPLICING_PENDING=1,SPLICING_DONE=2,SEC_PENDING=1,SEC_DONE=2,DETAIL_STATUS_REVIEWED=2;
    private static final DateTimeFormatter SPACE=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/weaving-orders") public Map<String,Object> weavingOrders(@RequestParam(defaultValue="1") long pageNo,@RequestParam(defaultValue="10") long pageSize,@RequestParam(required=false) String batchNo,@RequestParam(required=false) Integer processStatus){return reviewedWeavingPage(pageNo,pageSize,batchNo,processStatus);}    
    @GetMapping("/weaving-review-orders")
    public Map<String,Object> weavingReviewOrders(@RequestParam(defaultValue="1") long pageNo,@RequestParam(defaultValue="10") long pageSize,@RequestParam(required=false) String batchNo,@RequestParam(required=false) Integer processStatus){
        long p=Math.max(pageNo,1),s=Math.max(pageSize,1),o=(p-1)*s;
        StringBuilder w=new StringBuilder(" WHERE EXISTS (SELECT 1 FROM map_order_weaving m JOIN order_detail d ON d.detail_id=m.detail_id WHERE m.weaving_batch_no = p.weaving_batch_no AND d.detail_status = 1) ");
        List<Object>a=new ArrayList<>();
        if(StringUtils.hasText(batchNo)){w.append(" AND p.weaving_batch_no LIKE ? ");a.add("%"+batchNo.trim()+"%");}
        if(processStatus!=null){w.append(" AND p.process_status=? ");a.add(processStatus);}
        Long total=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM prd_weaving_process p "+w,a.toArray(),Long.class);
        List<Object>qa=new ArrayList<>(a);qa.add((int)s);qa.add((int)o);
        List<Map<String,Object>>records=jdbcTemplate.queryForList("SELECT p.* FROM prd_weaving_process p "+w+" ORDER BY p.created_at DESC LIMIT ? OFFSET ?",qa.toArray());
        return okPage(records,total==null?0:total,p,s);
    }
    @PostMapping("/weaving-review-orders/{batchNo}/approve")
    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> approveWeavingReview(@PathVariable String batchNo){
        proc("prd_weaving_process","weaving_batch_no",batchNo,"织造批次不存在");
        int detailAffected=jdbcTemplate.update("UPDATE order_detail d JOIN map_order_weaving m ON m.detail_id=d.detail_id SET d.detail_status=2 WHERE m.weaving_batch_no=? AND d.detail_status=1",batchNo);
        if(detailAffected<=0) throw new BizException("该织造批次无待审核明细");
        List<String>os=jdbcTemplate.queryForList("SELECT DISTINCT d.order_no FROM order_detail d JOIN map_order_weaving m ON m.detail_id=d.detail_id WHERE m.weaving_batch_no=?",String.class,batchNo);
        for(String o:os){Integer c=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM order_detail WHERE order_no=? AND detail_status>=2",Integer.class,o);jdbcTemplate.update("UPDATE order_master SET order_status=? WHERE order_no=?",c!=null&&c>0?3:1,o);}
        return ok(Map.of("weavingBatchNo",batchNo,"detailAffected",detailAffected,"orderCount",os.size()));
    }
    @GetMapping("/weaving-orders/{batchNo}/related-orders")
    public Map<String,Object> weavingRelatedOrders(@PathVariable String batchNo){
        proc("prd_weaving_process","weaving_batch_no",batchNo,"织造批次不存在");
        List<Map<String,Object>> rs = jdbcTemplate.queryForList(
            "SELECT m.detail_id,d.order_no,om.contract_no,c.customer_id,c.customer_name,d.product_model,d.air_permeability,d.req_length,d.req_width,d.detail_status,d.weaving_mode_status,m.created_at AS mapped_at " +
            "FROM map_order_weaving m " +
            "JOIN order_detail d ON d.detail_id=m.detail_id " +
            "JOIN order_master om ON om.order_no=d.order_no " +
            "LEFT JOIN contract_master cm ON cm.contract_id=om.contract_no " +
            "LEFT JOIN customer c ON c.customer_id=cm.customer_id " +
            "WHERE m.weaving_batch_no=? " +
            "ORDER BY m.created_at ASC,m.detail_id ASC",
            batchNo
        );
        return ok(rs);
    }
    @GetMapping("/setting-orders") public Map<String,Object> settingOrders(@RequestParam(defaultValue="1") long pageNo,@RequestParam(defaultValue="10") long pageSize,@RequestParam(required=false) String batchNo,@RequestParam(required=false) Integer processStatus){return page("prd_setting_process","setting_batch_no",pageNo,pageSize,batchNo,processStatus);}    
    @GetMapping("/cutting-orders")
    public Map<String,Object> cuttingOrders(@RequestParam(defaultValue="1") long pageNo,@RequestParam(defaultValue="10") long pageSize,@RequestParam(required=false) String batchNo,@RequestParam(required=false) Integer processStatus){
        long p=Math.max(pageNo,1),s=Math.max(pageSize,1),o=(p-1)*s;
        StringBuilder w=new StringBuilder(" WHERE 1=1 ");
        List<Object>a=new ArrayList<>();
        if(StringUtils.hasText(batchNo)){w.append(" AND r.cut_batch_no LIKE ? ");a.add("%"+batchNo.trim()+"%");}
        if(processStatus!=null){w.append(" AND r.process_status=? ");a.add(processStatus);}
        Long total=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM prd_cutting_record r JOIN prd_cutting_task t ON t.task_id=r.task_id "+w,a.toArray(),Long.class);
        List<Object>qa=new ArrayList<>(a);qa.add((int)s);qa.add((int)o);
        List<Map<String,Object>>records=jdbcTemplate.queryForList(
            "SELECT r.cut_batch_no,r.task_id,t.setting_batch_no,r.detail_id,r.actual_cut_len,r.actual_cut_wid,r.operator_id,r.waste_area,r.process_status,r.cut_time,r.created_at,r.updated_at " +
            "FROM prd_cutting_record r JOIN prd_cutting_task t ON t.task_id=r.task_id "+w+" ORDER BY r.created_at DESC LIMIT ? OFFSET ?",
            qa.toArray()
        );
        return okPage(records,total==null?0:total,p,s);
    }
    @GetMapping("/cutting-tasks")
    public Map<String,Object> cuttingTasks(@RequestParam(defaultValue="1") long pageNo,@RequestParam(defaultValue="10") long pageSize,@RequestParam(required=false) String batchNo,@RequestParam(required=false) Integer taskStatus){
        long p=Math.max(pageNo,1),s=Math.max(pageSize,1),o=(p-1)*s;
        StringBuilder w=new StringBuilder(" WHERE 1=1 ");
        List<Object>a=new ArrayList<>();
        if(StringUtils.hasText(batchNo)){w.append(" AND (t.task_id LIKE ? OR t.setting_batch_no LIKE ?) ");a.add("%"+batchNo.trim()+"%");a.add("%"+batchNo.trim()+"%");}
        if(taskStatus!=null){w.append(" AND t.task_status=? ");a.add(taskStatus);}
        Long total=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM prd_cutting_task t "+w,a.toArray(),Long.class);
        List<Object>qa=new ArrayList<>(a);qa.add((int)s);qa.add((int)o);
        List<Map<String,Object>>records=jdbcTemplate.queryForList(
            "SELECT t.task_id,t.setting_batch_no,t.source_length,t.source_width,t.task_status,t.receive_time,t.created_at,t.updated_at," +
                "(SELECT COUNT(1) FROM prd_cutting_record r WHERE r.task_id=t.task_id) AS small_net_count," +
                "(SELECT COUNT(1) FROM prd_cutting_record r WHERE r.task_id=t.task_id AND r.process_status=2) AS completed_small_count " +
            "FROM prd_cutting_task t "+w+" ORDER BY t.created_at DESC LIMIT ? OFFSET ?",
            qa.toArray()
        );
        return okPage(records,total==null?0:total,p,s);
    }
    @GetMapping("/cutting-tasks/{taskId}/related-orders")
    public Map<String,Object> cuttingTaskRelatedOrders(@PathVariable String taskId){
        proc("prd_cutting_task","task_id",taskId,"裁网任务不存在");
        List<Map<String,Object>> rs = jdbcTemplate.queryForList(
            "SELECT r.cut_batch_no,r.task_id,r.detail_id,d.order_no,om.contract_no,c.customer_name,d.product_model,d.air_permeability,d.req_length,d.req_width,r.actual_cut_len,r.actual_cut_wid,r.process_status,r.cut_time " +
            "FROM prd_cutting_record r " +
            "JOIN order_detail d ON d.detail_id=r.detail_id " +
            "JOIN order_master om ON om.order_no=d.order_no " +
            "LEFT JOIN contract_master cm ON cm.contract_id=om.contract_no " +
            "LEFT JOIN customer c ON c.customer_id=cm.customer_id " +
            "WHERE r.task_id=? ORDER BY r.created_at ASC,r.cut_batch_no ASC",
            taskId
        );
        return ok(rs);
    }
    @PostMapping("/cutting-tasks/{taskId}/complete")
    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> completeCuttingTask(@PathVariable String taskId,@RequestBody CuttingTaskReq r){
        ensureTable("prd_cutting_task"); ensureTable("prd_cutting_record");
        req(r.operatorId,"operatorId");
        proc("prd_cutting_task","task_id",taskId,"裁网任务不存在");
        Long total=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM prd_cutting_record WHERE task_id=?",Long.class,taskId);
        if(total==null||total<=0) throw new BizException("裁网任务未生成对应小网");
        List<String>batches=jdbcTemplate.queryForList("SELECT cut_batch_no FROM prd_cutting_record WHERE task_id=? ORDER BY cut_batch_no",String.class,taskId);
        BigDecimal totalWaste=r.wasteArea==null?BigDecimal.ZERO:r.wasteArea;
        BigDecimal avgWaste=totalWaste.divide(BigDecimal.valueOf(Math.max(batches.size(),1)),2, RoundingMode.HALF_UP);
        Timestamp now=ts(LocalDateTime.now());
        int affected=jdbcTemplate.update("UPDATE prd_cutting_record SET operator_id=?,waste_area=?,process_status=?,cut_time=? WHERE task_id=?",r.operatorId,avgWaste,CUTTING_DONE,now,taskId);
        jdbcTemplate.update("UPDATE prd_cutting_task SET task_status=? WHERE task_id=?",3,taskId);
        for(String cb:batches){activateSplicing(cb,r.operatorId);}
        return ok(Map.of("taskId",taskId,"smallNetCount",batches.size(),"affected",affected,"taskStatus",3));
    }
    @GetMapping("/splicing-orders") public Map<String,Object> splicingOrders(@RequestParam(defaultValue="1") long pageNo,@RequestParam(defaultValue="10") long pageSize,@RequestParam(required=false) String batchNo,@RequestParam(required=false) Integer processStatus){return page("prd_splicing_process","splice_batch_no",pageNo,pageSize,batchNo,processStatus);}    
    @GetMapping("/sec-setting-orders") public Map<String,Object> secSettingOrders(@RequestParam(defaultValue="1") long pageNo,@RequestParam(defaultValue="10") long pageSize,@RequestParam(required=false) String batchNo,@RequestParam(required=false) Integer processStatus){return page("prd_sec_setting_process","final_batch_no",pageNo,pageSize,batchNo,processStatus);}    

    @GetMapping("/weaving-orders/{batchNo}/report")
    public Map<String,Object> weavingReport(@PathVariable String batchNo){
        ensureTable("prd_weaving_report");
        List<Map<String,Object>> rs=jdbcTemplate.queryForList("SELECT weaving_batch_no,machine_id,operator_id,material_batch_no,tension_params,actual_length,actual_start_time,actual_end_time,created_at,updated_at FROM prd_weaving_report WHERE weaving_batch_no=?",batchNo);
        if(!rs.isEmpty()) return ok(rs.get(0));
        Map<String,Object> p=proc("prd_weaving_process","weaving_batch_no",batchNo,"织造批次不存在");
        return ok(kv("weaving_batch_no",batchNo,"machine_id",p.get("machine_id"),"operator_id",p.get("operator_id"),"actual_length",p.get("actual_length"),"actual_start_time",null,"actual_end_time",null,"material_batch_no",null,"tension_params",null));
    }

    @PostMapping("/weaving-orders/{batchNo}/report")
    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> submitWeavingReport(@PathVariable String batchNo,@RequestBody WeavingReq r){
        ensureTable("prd_weaving_report"); req(r.machineId,"machineId"); req(r.operatorId,"operatorId"); req(r.materialBatchNo,"materialBatchNo"); req(r.tensionParams,"tensionParams"); req(r.actualLength,"actualLength");
        String operatorId=normalizeOperatorId(r.operatorId);
        LocalDateTime st=dt(r.actualStartTime,"actualStartTime"); if(st==null) throw new BizException("actualStartTime不能为空");
        LocalDateTime ed=LocalDateTime.now(); if(ed.isBefore(st)) throw new BizException("actualEndTime不能早于actualStartTime");
        proc("prd_weaving_process","weaving_batch_no",batchNo,"织造批次不存在");
        upsert("prd_weaving_report","weaving_batch_no",batchNo,
            "UPDATE prd_weaving_report SET machine_id=?,operator_id=?,material_batch_no=?,tension_params=?,actual_length=?,actual_start_time=?,actual_end_time=? WHERE weaving_batch_no=?",
            new Object[]{r.machineId,operatorId,r.materialBatchNo,r.tensionParams,r.actualLength,ts(st),ts(ed),batchNo},
            "INSERT INTO prd_weaving_report (weaving_batch_no,machine_id,operator_id,material_batch_no,tension_params,actual_length,actual_start_time,actual_end_time) VALUES (?,?,?,?,?,?,?,?)",
            new Object[]{batchNo,r.machineId,operatorId,r.materialBatchNo,r.tensionParams,r.actualLength,ts(st),ts(ed)});
        jdbcTemplate.update("UPDATE prd_weaving_process SET machine_id=?,operator_id=?,actual_length=?,process_status=?,completed_at=? WHERE weaving_batch_no=?",r.machineId,operatorId,r.actualLength,WEAVING_DONE,ts(ed),batchNo);
        syncOrderStatusForWeavingBatch(batchNo); activateSetting(batchNo);
        return ok(Map.of("weavingBatchNo",batchNo,"processStatus",WEAVING_DONE,"settingActivated",true));
    }

    @GetMapping("/setting-orders/{batchNo}/report")
    public Map<String,Object> settingReport(@PathVariable String batchNo){
        ensureTable("prd_setting_report");
        List<Map<String,Object>> rs=jdbcTemplate.queryForList("SELECT setting_batch_no,weaving_batch_no,operator_id,actual_temp,setting_duration,shrink_rate,created_at,updated_at FROM prd_setting_report WHERE setting_batch_no=?",batchNo);
        if(!rs.isEmpty()) return ok(rs.get(0));
        Map<String,Object> p=proc("prd_setting_process","setting_batch_no",batchNo,"定型批次不存在");
        return ok(kv("setting_batch_no",batchNo,"weaving_batch_no",p.get("weaving_batch_no"),"operator_id",p.get("operator_id"),"actual_temp",null,"setting_duration",null,"shrink_rate",null));
    }

    @PostMapping("/setting-orders/{batchNo}/report")
    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> submitSettingReport(@PathVariable String batchNo,@RequestBody SettingReq r){
        ensureTable("prd_setting_report"); req(r.operatorId,"operatorId"); req(r.actualTemp,"actualTemp"); req(r.settingDuration,"settingDuration"); req(r.shrinkRate,"shrinkRate");
        Map<String,Object> p=proc("prd_setting_process","setting_batch_no",batchNo,"定型批次不存在"); String wb=s(p.get("weaving_batch_no"));
        upsert("prd_setting_report","setting_batch_no",batchNo,
            "UPDATE prd_setting_report SET weaving_batch_no=?,operator_id=?,actual_temp=?,setting_duration=?,shrink_rate=? WHERE setting_batch_no=?",
            new Object[]{wb,r.operatorId,r.actualTemp,r.settingDuration,r.shrinkRate,batchNo},
            "INSERT INTO prd_setting_report (setting_batch_no,weaving_batch_no,operator_id,actual_temp,setting_duration,shrink_rate) VALUES (?,?,?,?,?,?)",
            new Object[]{batchNo,wb,r.operatorId,r.actualTemp,r.settingDuration,r.shrinkRate});
        jdbcTemplate.update("UPDATE prd_setting_process SET operator_id=?,process_status=?,completed_at=? WHERE setting_batch_no=?",r.operatorId,SETTING_DONE,ts(LocalDateTime.now()),batchNo);
        activateCutting(batchNo); return ok(Map.of("settingBatchNo",batchNo,"processStatus",SETTING_DONE));
    }

    @GetMapping("/cutting-orders/{batchNo}/report")
    public Map<String,Object> cuttingReport(@PathVariable String batchNo){
        ensureTable("prd_cutting_record");
        List<Map<String,Object>> rs=jdbcTemplate.queryForList(
            "SELECT r.cut_batch_no,r.task_id,t.setting_batch_no,r.detail_id,r.operator_id,r.actual_cut_len,r.actual_cut_wid,r.waste_area,r.cut_time,r.created_at,r.updated_at " +
            "FROM prd_cutting_record r LEFT JOIN prd_cutting_task t ON t.task_id=r.task_id WHERE r.cut_batch_no=?",
            batchNo
        );
        if(!rs.isEmpty()) return ok(rs.get(0));
        Map<String,Object> p=proc("prd_cutting_record","cut_batch_no",batchNo,"裁网批次不存在");
        String taskId=s(p.get("task_id"));
        Map<String,Object> t=proc("prd_cutting_task","task_id",taskId,"裁网任务不存在");
        return ok(kv("cut_batch_no",batchNo,"task_id",taskId,"setting_batch_no",t.get("setting_batch_no"),"detail_id",p.get("detail_id"),"operator_id",p.get("operator_id"),"actual_cut_len",p.get("actual_cut_len"),"actual_cut_wid",p.get("actual_cut_wid"),"waste_area",p.get("waste_area"),"cut_time",p.get("cut_time")));
    }

    @PostMapping("/cutting-orders/{batchNo}/report")
    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> submitCuttingReport(@PathVariable String batchNo,@RequestBody CuttingReq r){
        ensureTable("prd_cutting_record"); req(r.operatorId,"operatorId"); req(r.actualCutLen,"actualCutLen"); req(r.actualCutWid,"actualCutWid"); req(r.wasteArea,"wasteArea");
        Map<String,Object> p=proc("prd_cutting_record","cut_batch_no",batchNo,"裁网批次不存在"); String taskId=s(p.get("task_id")),did=s(p.get("detail_id"));
        upsert("prd_cutting_record","cut_batch_no",batchNo,
            "UPDATE prd_cutting_record SET task_id=?,detail_id=?,operator_id=?,actual_cut_len=?,actual_cut_wid=?,waste_area=?,process_status=?,cut_time=? WHERE cut_batch_no=?",
            new Object[]{taskId,did,r.operatorId,r.actualCutLen,r.actualCutWid,r.wasteArea,CUTTING_DONE,ts(LocalDateTime.now()),batchNo},
            "INSERT INTO prd_cutting_record (cut_batch_no,task_id,detail_id,operator_id,actual_cut_len,actual_cut_wid,waste_area,process_status,cut_time) VALUES (?,?,?,?,?,?,?,?,?)",
            new Object[]{batchNo,taskId,did,r.operatorId,r.actualCutLen,r.actualCutWid,r.wasteArea,CUTTING_DONE,ts(LocalDateTime.now())});
        syncCuttingTaskStatus(taskId);
        activateSplicing(batchNo,r.operatorId); return ok(Map.of("cutBatchNo",batchNo,"processStatus",CUTTING_DONE));
    }

    @GetMapping("/splicing-orders/{batchNo}/report")
    public Map<String,Object> splicingReport(@PathVariable String batchNo){
        ensureTable("prd_splicing_report");
        List<Map<String,Object>> rs=jdbcTemplate.queryForList("SELECT splice_batch_no,cut_batch_no,operator_id,splice_type,joint_strength,created_at,updated_at FROM prd_splicing_report WHERE splice_batch_no=?",batchNo);
        if(!rs.isEmpty()) return ok(rs.get(0));
        Map<String,Object> p=proc("prd_splicing_process","splice_batch_no",batchNo,"插接批次不存在");
        return ok(kv("splice_batch_no",batchNo,"cut_batch_no",p.get("cut_batch_no"),"operator_id",p.get("operator_id"),"splice_type",p.get("splice_type"),"joint_strength",null));
    }

    @PostMapping("/splicing-orders/{batchNo}/report")
    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> submitSplicingReport(@PathVariable String batchNo,@RequestBody SplicingReq r){
        ensureTable("prd_splicing_report"); req(r.operatorId,"operatorId"); req(r.spliceType,"spliceType"); req(r.jointStrength,"jointStrength");
        Map<String,Object> p=proc("prd_splicing_process","splice_batch_no",batchNo,"插接批次不存在"); String cb=s(p.get("cut_batch_no"));
        upsert("prd_splicing_report","splice_batch_no",batchNo,
            "UPDATE prd_splicing_report SET cut_batch_no=?,operator_id=?,splice_type=?,joint_strength=? WHERE splice_batch_no=?",
            new Object[]{cb,r.operatorId,r.spliceType,r.jointStrength,batchNo},
            "INSERT INTO prd_splicing_report (splice_batch_no,cut_batch_no,operator_id,splice_type,joint_strength) VALUES (?,?,?,?,?)",
            new Object[]{batchNo,cb,r.operatorId,r.spliceType,r.jointStrength});
        jdbcTemplate.update("UPDATE prd_splicing_process SET operator_id=?,splice_type=?,process_status=? WHERE splice_batch_no=?",r.operatorId,r.spliceType,SPLICING_DONE,batchNo);
        activateSec(batchNo); return ok(Map.of("spliceBatchNo",batchNo,"processStatus",SPLICING_DONE));
    }

    @GetMapping("/sec-setting-orders/{batchNo}/report")
    public Map<String,Object> secSettingReport(@PathVariable String batchNo){
        ensureTable("prd_sec_setting_report");
        List<Map<String,Object>> rs=jdbcTemplate.queryForList("SELECT final_batch_no,splice_batch_no,operator_id,final_length,final_width,mesh_defect_info,qc_trigger_flag,created_at,updated_at FROM prd_sec_setting_report WHERE final_batch_no=?",batchNo);
        if(!rs.isEmpty()) return ok(rs.get(0));
        Map<String,Object> p=proc("prd_sec_setting_process","final_batch_no",batchNo,"二次定型批次不存在");
        return ok(kv("final_batch_no",batchNo,"splice_batch_no",p.get("splice_batch_no"),"operator_id",null,"final_length",p.get("final_length"),"final_width",p.get("final_width"),"mesh_defect_info",null,"qc_trigger_flag",1));
    }

    @PostMapping("/sec-setting-orders/{batchNo}/report")
    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> submitSecSettingReport(@PathVariable String batchNo,@RequestBody SecReq r){
        ensureTable("prd_sec_setting_report"); req(r.operatorId,"operatorId"); req(r.finalLength,"finalLength"); req(r.finalWidth,"finalWidth"); req(r.qcTriggerFlag,"qcTriggerFlag");
        Map<String,Object> p=proc("prd_sec_setting_process","final_batch_no",batchNo,"二次定型批次不存在"); String sb=s(p.get("splice_batch_no"));
        upsert("prd_sec_setting_report","final_batch_no",batchNo,
            "UPDATE prd_sec_setting_report SET splice_batch_no=?,operator_id=?,final_length=?,final_width=?,mesh_defect_info=?,qc_trigger_flag=? WHERE final_batch_no=?",
            new Object[]{sb,r.operatorId,r.finalLength,r.finalWidth,r.meshDefectInfo,r.qcTriggerFlag,batchNo},
            "INSERT INTO prd_sec_setting_report (final_batch_no,splice_batch_no,operator_id,final_length,final_width,mesh_defect_info,qc_trigger_flag) VALUES (?,?,?,?,?,?,?)",
            new Object[]{batchNo,sb,r.operatorId,r.finalLength,r.finalWidth,r.meshDefectInfo,r.qcTriggerFlag});
        jdbcTemplate.update("UPDATE prd_sec_setting_process SET final_length=?,final_width=?,process_status=?,completed_at=? WHERE final_batch_no=?",r.finalLength,r.finalWidth,SEC_DONE,ts(LocalDateTime.now()),batchNo);
        return ok(Map.of("finalBatchNo",batchNo,"processStatus",SEC_DONE));
    }

    private Map<String,Object> page(String table,String idCol,long pageNo,long pageSize,String batchNo,Integer status){long p=Math.max(pageNo,1),s=Math.max(pageSize,1),o=(p-1)*s;StringBuilder w=new StringBuilder(" WHERE 1=1 ");List<Object>a=new ArrayList<>();if(StringUtils.hasText(batchNo)){w.append(" AND ").append(idCol).append(" LIKE ? ");a.add("%"+batchNo.trim()+"%");}if(status!=null){w.append(" AND process_status=? ");a.add(status);}Long total=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM "+table+w,a.toArray(),Long.class);List<Object>qa=new ArrayList<>(a);qa.add((int)s);qa.add((int)o);List<Map<String,Object>>records=jdbcTemplate.queryForList("SELECT * FROM "+table+w+" ORDER BY created_at DESC LIMIT ? OFFSET ?",qa.toArray());return okPage(records,total==null?0:total,p,s);}
    private Map<String,Object> reviewedWeavingPage(long pageNo,long pageSize,String batchNo,Integer processStatus){long p=Math.max(pageNo,1),s=Math.max(pageSize,1),o=(p-1)*s;StringBuilder w=new StringBuilder(" WHERE EXISTS (SELECT 1 FROM map_order_weaving m WHERE m.weaving_batch_no = p.weaving_batch_no) AND NOT EXISTS (SELECT 1 FROM map_order_weaving m JOIN order_detail d ON d.detail_id=m.detail_id WHERE m.weaving_batch_no = p.weaving_batch_no AND (d.detail_status IS NULL OR d.detail_status < ?)) ");List<Object>a=new ArrayList<>();a.add(DETAIL_STATUS_REVIEWED);if(StringUtils.hasText(batchNo)){w.append(" AND p.weaving_batch_no LIKE ? ");a.add("%"+batchNo.trim()+"%");}if(processStatus!=null){w.append(" AND p.process_status=? ");a.add(processStatus);}Long total=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM prd_weaving_process p "+w,a.toArray(),Long.class);List<Object>qa=new ArrayList<>(a);qa.add((int)s);qa.add((int)o);List<Map<String,Object>>records=jdbcTemplate.queryForList("SELECT p.* FROM prd_weaving_process p "+w+" ORDER BY p.created_at DESC LIMIT ? OFFSET ?",qa.toArray());return okPage(records,total==null?0:total,p,s);}
    private Map<String,Object> okPage(List<Map<String,Object>> records,long total,long current,long size){Map<String,Object>page=new LinkedHashMap<>();page.put("records",records);page.put("total",total);page.put("current",current);page.put("size",size);Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("data",page);return r;}
    private Map<String,Object> ok(Object data){Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("data",data);return r;}
    private Map<String,Object> kv(Object... arr){Map<String,Object>m=new LinkedHashMap<>();for(int i=0;i<arr.length;i+=2)m.put(String.valueOf(arr[i]),arr[i+1]);return m;}
    private void req(Object v,String n){if(v==null||(v instanceof String s&&!StringUtils.hasText(s)))throw new BizException(n+"不能为空");}
    private Timestamp ts(LocalDateTime v){return v==null?null:Timestamp.valueOf(v);}    
    private LocalDateTime dt(String v,String f){if(!StringUtils.hasText(v))return null;try{return LocalDateTime.parse(v.trim());}catch(DateTimeParseException e){try{return LocalDateTime.parse(v.trim(),SPACE);}catch(DateTimeParseException ex){throw new BizException(f+"时间格式错误，支持: yyyy-MM-ddTHH:mm:ss 或 yyyy-MM-dd HH:mm:ss");}}}
    private String s(Object v){return v==null?null:String.valueOf(v);}    
    private BigDecimal dec(Object v){if(v==null)return BigDecimal.ZERO; if(v instanceof BigDecimal bd)return bd; return new BigDecimal(String.valueOf(v));}
    private String normalizeOperatorId(String input){String value=input==null?null:input.trim();if(!StringUtils.hasText(value))return value;Long idHit=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM sys_user WHERE user_id=? AND is_active=1",Long.class,value);if(idHit!=null&&idHit>0)return value;List<Map<String,Object>>rows=jdbcTemplate.queryForList("SELECT user_id FROM sys_user WHERE username=? AND is_active=1 LIMIT 1",value);if(!rows.isEmpty())return String.valueOf(rows.get(0).get("user_id"));return value;}
    private void ensureTable(String t){Integer c=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",Integer.class,t);if(c==null||c==0)throw new BizException("本地数据库缺少表 "+t+"，请先执行LW.sql同步数据库结构");}
    private Map<String,Object> proc(String table,String idCol,String idVal,String msg){List<Map<String,Object>>rs=jdbcTemplate.queryForList("SELECT * FROM "+table+" WHERE "+idCol+" = ?",idVal);if(rs.isEmpty())throw new BizException(msg+": "+idVal);return rs.get(0);}    
    private void upsert(String table,String idCol,String idVal,String updSql,Object[]updArgs,String insSql,Object[]insArgs){Long c=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM "+table+" WHERE "+idCol+" = ?",Long.class,idVal);if(c!=null&&c>0)jdbcTemplate.update(updSql,updArgs);else jdbcTemplate.update(insSql,insArgs);}    
    private void syncOrderStatusForWeavingBatch(String wb){jdbcTemplate.update("UPDATE order_detail d JOIN map_order_weaving m ON m.detail_id=d.detail_id SET d.detail_status=2 WHERE m.weaving_batch_no=? AND (d.detail_status IS NULL OR d.detail_status<2)",wb);List<String>os=jdbcTemplate.queryForList("SELECT DISTINCT d.order_no FROM order_detail d JOIN map_order_weaving m ON m.detail_id=d.detail_id WHERE m.weaving_batch_no=?",String.class,wb);for(String o:os){Integer c=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM order_detail WHERE order_no=? AND detail_status>=2",Integer.class,o);jdbcTemplate.update("UPDATE order_master SET order_status=? WHERE order_no=?",c!=null&&c>0?3:1,o);}}
    private void activateSetting(String wb){String sb=wb+"D";Long e=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM prd_setting_process WHERE weaving_batch_no=?",Long.class,wb);if(e!=null&&e>0)return;jdbcTemplate.update("INSERT INTO prd_setting_process (setting_batch_no,weaving_batch_no,process_status) VALUES (?,?,?)",sb,wb,SETTING_PENDING);}    
    private void activateCutting(String sb){
        String taskId=sb+"C";
        Long taskExists=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM prd_cutting_task WHERE setting_batch_no=?",Long.class,sb);
        if(taskExists==null||taskExists==0){
            List<Map<String,Object>>src=jdbcTemplate.queryForList("SELECT w.actual_length,w.actual_width FROM prd_setting_process s LEFT JOIN prd_weaving_process w ON w.weaving_batch_no=s.weaving_batch_no WHERE s.setting_batch_no=?",sb);
            BigDecimal sourceLen=src.isEmpty()?BigDecimal.ZERO:dec(src.get(0).get("actual_length"));
            BigDecimal sourceWid=src.isEmpty()?BigDecimal.ZERO:dec(src.get(0).get("actual_width"));
            jdbcTemplate.update("INSERT INTO prd_cutting_task (task_id,setting_batch_no,source_length,source_width,task_status,receive_time) VALUES (?,?,?,?,?,?)",taskId,sb,sourceLen,sourceWid,1,ts(LocalDateTime.now()));
        }else{
            List<Map<String,Object>>taskRows=jdbcTemplate.queryForList("SELECT task_id FROM prd_cutting_task WHERE setting_batch_no=? LIMIT 1",sb);
            if(!taskRows.isEmpty()) taskId=s(taskRows.get(0).get("task_id"));
        }
        List<Map<String,Object>>rs=jdbcTemplate.queryForList("SELECT m.detail_id,d.req_length,d.req_width FROM map_order_weaving m JOIN prd_setting_process s ON s.weaving_batch_no=m.weaving_batch_no JOIN order_detail d ON d.detail_id=m.detail_id WHERE s.setting_batch_no=? ORDER BY m.detail_id",sb);
        int i=1;boolean single=rs.size()==1;
        for(Map<String,Object>r:rs){
            String cb=single?taskId:taskId+"-"+String.format("%03d",i++);
            Long e=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM prd_cutting_record WHERE cut_batch_no=?",Long.class,cb);
            if(e!=null&&e>0)continue;
            jdbcTemplate.update("INSERT INTO prd_cutting_record (cut_batch_no,task_id,detail_id,actual_cut_len,actual_cut_wid,process_status) VALUES (?,?,?,?,?,?)",cb,taskId,s(r.get("detail_id")),dec(r.get("req_length")),dec(r.get("req_width")),CUTTING_PENDING);
        }
    }    
    private void syncCuttingTaskStatus(String taskId){
        Long pending=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM prd_cutting_record WHERE task_id=? AND process_status=1",Long.class,taskId);
        Long total=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM prd_cutting_record WHERE task_id=?",Long.class,taskId);
        int nextStatus=(total!=null&&total>0&&pending!=null&&pending==0)?3:2;
        jdbcTemplate.update("UPDATE prd_cutting_task SET task_status=? WHERE task_id=?",nextStatus,taskId);
    }
    private void activateSplicing(String cb,String op){Long e=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM prd_splicing_process WHERE cut_batch_no=?",Long.class,cb);if(e!=null&&e>0)return;jdbcTemplate.update("INSERT INTO prd_splicing_process (splice_batch_no,cut_batch_no,operator_id,process_status) VALUES (?,?,?,?)",cb,cb,op,SPLICING_PENDING);}    
    private void activateSec(String sb){Long e=jdbcTemplate.queryForObject("SELECT COUNT(1) FROM prd_sec_setting_process WHERE splice_batch_no=?",Long.class,sb);if(e!=null&&e>0)return;List<Map<String,Object>>rs=jdbcTemplate.queryForList("SELECT c.actual_cut_len,c.actual_cut_wid FROM prd_splicing_process s JOIN prd_cutting_record c ON c.cut_batch_no=s.cut_batch_no WHERE s.splice_batch_no=?",sb);if(rs.isEmpty())throw new BizException("插接批次关联裁网数据不存在");Map<String,Object>c=rs.get(0);jdbcTemplate.update("INSERT INTO prd_sec_setting_process (final_batch_no,splice_batch_no,final_length,final_width,process_status) VALUES (?,?,?,?,?)",sb,sb,dec(c.get("actual_cut_len")),dec(c.get("actual_cut_wid")),SEC_PENDING);}    

    public static class WeavingReq{public String machineId,operatorId,materialBatchNo,tensionParams,actualStartTime; public BigDecimal actualLength;}
    public static class SettingReq{public String operatorId; public BigDecimal actualTemp,shrinkRate; public Integer settingDuration;}
    public static class CuttingReq{public String operatorId; public BigDecimal actualCutLen,actualCutWid,wasteArea;}
    public static class CuttingTaskReq{public String operatorId; public BigDecimal wasteArea;}
    public static class SplicingReq{public String operatorId,spliceType,jointStrength;}
    public static class SecReq{public String operatorId,meshDefectInfo; public BigDecimal finalLength,finalWidth; public Integer qcTriggerFlag;}
}

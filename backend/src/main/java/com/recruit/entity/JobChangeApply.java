package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位变更申请记录表实体类
 */
@Data
@TableName("job_change_apply")
public class JobChangeApply {
    
    /**
     * 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 岗位ID - 外键(job.id)
     */
    private Long jobId;
    
    /**
     * 申请HR ID - 外键(sys_user.id)
     */
    private Long hrId;
    
    /**
     * 变更内容（JSON）
     */
    private String changeContent;
    
    /**
     * 状态：0=待审核，1=通过，2=拒绝
     */
    private Integer status;
    
    /**
     * 审核教师ID - 外键(sys_user.id)
     */
    private Long reviewTeacherId;

    /**
     * 拒绝原因（教师拒绝时填写）
     */
    private String rejectReason;
    
    /**
     * 申请时间
     */
    private LocalDateTime createTime;
}

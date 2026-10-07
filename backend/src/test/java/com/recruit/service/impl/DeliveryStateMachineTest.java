package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.recruit.entity.Delivery;
import com.recruit.entity.Job;
import com.recruit.entity.Message;
import com.recruit.entity.SysUser;
import com.recruit.mapper.DeliveryMapper;
import com.recruit.mapper.JobMapper;
import com.recruit.service.MessageService;
import com.recruit.service.UserMessageService;
import com.recruit.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 投递状态机单元测试（Mockito 打桩，不依赖数据库）
 * 规则：0待查看 → 1已查看 → 2面试 → 3录用/4不合适；只允许单向推进，终态不可变更
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeliveryStateMachineTest {

    @Mock
    private DeliveryMapper deliveryMapper;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private MessageService messageService;

    @Mock
    private UserMessageService userMessageService;

    @Mock
    private UserService userService;

    @Spy
    @InjectMocks
    private DeliveryServiceImpl service;

    private Job job() {
        Job job = new Job();
        job.setId(1L);
        job.setTitle("Java后端开发工程师");
        job.setCompanyId(1L);
        return job;
    }

    private Delivery deliveryWithStatus(Integer status) {
        Delivery d = new Delivery();
        d.setId(1L);
        d.setStudentId(6L);
        d.setJobId(1L);
        d.setStatus(status);
        return d;
    }

    @SuppressWarnings("unchecked")
    private void stubHappyPath(Delivery d) {
        doReturn(d).when(service).getById(1L);
        doReturn(true).when(service).updateById(any(Delivery.class));
        // 私有方法链的底层依赖
        when(jobMapper.selectById(1L)).thenReturn(job());
        SysUser student = new SysUser();
        student.setId(6L);
        student.setRealName("张明");
        when(userService.getById(6L)).thenReturn(student);
        // notifyHrOfJob 里的 lambdaQuery 链
        LambdaQueryChainWrapper<SysUser> chain = mock(LambdaQueryChainWrapper.class);
        when(chain.eq(any(), any())).thenReturn(chain);
        when(chain.list()).thenReturn(Collections.emptyList());
        when(userService.lambdaQuery()).thenReturn(chain);
        when(messageService.save(any(Message.class))).thenReturn(true);
        lenient().when(userMessageService.sendMessage(anyLong(), anyString(), anyString(), anyString(), anyLong())).thenReturn(null);
    }

    @Test
    @DisplayName("正常推进：0 → 1 → 2 → 3 每步成功且发通知")
    void forwardTransitionsSucceed() {
        Delivery d = deliveryWithStatus(0);
        stubHappyPath(d);
        assertTrue(service.updateDeliveryStatus(1L, 1, null));
        verify(messageService).save(any(Message.class));

        d.setStatus(1);
        assertTrue(service.updateDeliveryStatus(1L, 2, null));
        assertEquals(2, d.getStatus());
        assertNotNull(d.getInterviewTime(), "进入面试应自动设置面试时间");
        assertNotNull(d.getInterviewLocation());

        d.setStatus(2);
        assertTrue(service.updateDeliveryStatus(1L, 3, null));
        assertEquals(3, d.getStatus());
    }

    @Test
    @DisplayName("状态回退被拒绝：2 → 1 应抛异常")
    void backwardTransitionRejected() {
        Delivery d = deliveryWithStatus(2);
        stubHappyPath(d);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateDeliveryStatus(1L, 1, null));
        assertTrue(ex.getMessage().contains("不可回退"));
    }

    @Test
    @DisplayName("重复设置同一状态被拒绝")
    void sameStatusRejected() {
        Delivery d = deliveryWithStatus(1);
        stubHappyPath(d);
        assertThrows(RuntimeException.class, () -> service.updateDeliveryStatus(1L, 1, null));
    }

    @Test
    @DisplayName("终态（已录用/不合适）不可再变更")
    void terminalStateRejected() {
        Delivery d = deliveryWithStatus(3);
        stubHappyPath(d);
        RuntimeException ex3 = assertThrows(RuntimeException.class,
                () -> service.updateDeliveryStatus(1L, 2, null));
        assertTrue(ex3.getMessage().contains("已定档"));

        d.setStatus(4);
        RuntimeException ex4 = assertThrows(RuntimeException.class,
                () -> service.updateDeliveryStatus(1L, 1, null));
        assertTrue(ex4.getMessage().contains("已定档"));
    }

    @Test
    @DisplayName("越界状态（-1/5/null）被拒绝")
    void outOfRangeRejected() {
        Delivery d = deliveryWithStatus(0);
        stubHappyPath(d);
        assertThrows(RuntimeException.class, () -> service.updateDeliveryStatus(1L, -1, null));
        assertThrows(RuntimeException.class, () -> service.updateDeliveryStatus(1L, 5, null));
        assertThrows(RuntimeException.class, () -> service.updateDeliveryStatus(1L, null, null));
    }

    @Test
    @DisplayName("投递不存在抛出明确异常")
    void missingDeliveryRejected() {
        doReturn(null).when(service).getById(1L);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateDeliveryStatus(1L, 1, null));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("跳级推进允许：0 → 3 直接录用")
    void skipJumpAllowed() {
        Delivery d = deliveryWithStatus(0);
        stubHappyPath(d);
        assertTrue(service.updateDeliveryStatus(1L, 3, null));
        assertEquals(3, d.getStatus());
    }

    @Test
    @DisplayName("进入面试时 feedback 保留且面试字段自动填充")
    void feedbackPreservedOnInterview() {
        Delivery d = deliveryWithStatus(1);
        stubHappyPath(d);
        service.updateDeliveryStatus(1L, 2, "候选人技术面表现良好");
        assertEquals("候选人技术面表现良好", d.getFeedback());
        assertEquals("线上面试（腾讯会议）", d.getInterviewLocation());
        assertTrue(d.getInterviewTime().isAfter(LocalDateTime.now()));
    }
}

package com.job.biz;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.job.biz.model.QrtzTriggers;
import com.job.biz.model.Trigger;
import com.job.biz.service.ServerBuilderContext;
import com.job.quartz.service.SchedulerService;

@Controller
public class JobProcessController {

    private static final Logger log = LoggerFactory.getLogger(JobProcessController.class);

    @Value("#{prop[username]}")
    private String username;

    @Autowired
    public SchedulerService schedulerService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }

    /**
     * 进入查询控制台页面
     */
    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String getQrtzTriggers(Model model) throws ServletException, IOException {
        log.info("# 即将进入首页..");
        List<QrtzTriggers> results = this.schedulerService.getQrtzTriggers(null, null);
        model.addAttribute("list", results);
        model.addAttribute("servers", ServerBuilderContext.servers.keySet());

        System.err.println("username:"+username);
        model.addAttribute("username",username);
        return "index";
    }

    /**
     * 更新Trigger状态或删除操作
     * 
     * @param name
     *            Trigger 名称
     * @param group
     *            Trigger 组名
     * @param flag
     *            1=暂停，2=恢复，3=删除
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/trigger", method = RequestMethod.POST)
    public Boolean trigger(@RequestParam("name") String name, @RequestParam("group") String group, @RequestParam("flag") Integer flag) {
        log.info("# name={} , group={} , flag={}", name, group, flag);
        boolean result = true;
        switch (flag) {
        case 1:
            schedulerService.pauseTrigger(name, group);
            break;
        case 2:
            schedulerService.resumeTrigger(name, group);
            break;
        default:
            result = schedulerService.removeTrigdger(name, group);
            break;
        }

        return result;
    }

    /**
     * 进入新增页面
     * 
     * @return
     */
    @RequestMapping(value = "/add", method = RequestMethod.GET)
    public String toAdd(Model model) {
        model.addAttribute("servers", ServerBuilderContext.servers.keySet());
        log.info("# 进入新增页面");
        return "add";
    }

    /**
     * 进入新增页面
     * 
     * @return
     */
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public String add(Model model, @ModelAttribute Trigger trigger) {
        try {
            model.addAttribute("servers", ServerBuilderContext.servers.keySet());
            // 添加任务调试
            log.info("# triggerType={}", trigger.getTriggerType());
            switch (trigger.getTriggerType()) {
            case 1:
                // Trigger表达式模式
                log.info("# triggerName={} , triggerGroup={} , cronExpression={}", trigger.getTriggerName(), trigger.getTriggerGroup(), trigger.getCronExpression());
                schedulerService.schedule(trigger.getTriggerName(), trigger.getTriggerGroup(), trigger.getCronExpression());
                break;
            case 2:
                // 执行频率模式
                String expression = null;
                if (StringUtils.equals(trigger.getSelType(), "second")) {
                    // 每多秒执行一次
                    expression = "0/" + trigger.getIntervalTime() + " * * ? * * *";
                } else if (StringUtils.equals(trigger.getSelType(), "minute")) {
                    // 每多少分执行一次
                    expression = "0 0/" + trigger.getIntervalTime() + " * ? * * *";
                }
                log.info("# triggerName={} , triggerGroup={} , selType={} , expression={}", trigger.getTriggerName(), trigger.getTriggerGroup(), trigger.getSelType(), expression);
                schedulerService.schedule(trigger.getTriggerName(), trigger.getTriggerGroup(), expression);
                break;
            case 3:
                // 添加任务调试
                log.info("# name={} , startTimie={} , endTime={} , repeatCount={} , repeatInterval={} , group={}", trigger.getTriggerName(), trigger.getStartTime(), trigger.getEndTime(), trigger.getRepeatCount(), trigger.getRepeatInterval(),
                    trigger.getTriggerName());
                schedulerService.schedule(trigger.getTriggerName(), trigger.getStartTime(), trigger.getEndTime(), trigger.getRepeatCount(), trigger.getRepeatInterval(), trigger.getTriggerName());
                // 指定时间执行模式
                break;
            }

            log.info("# 进入新增页面");
        } catch (Exception e) {
            log.error("# 新增trigger失败 , error message={}", e.getMessage());
        }
        return "index";
    }
}

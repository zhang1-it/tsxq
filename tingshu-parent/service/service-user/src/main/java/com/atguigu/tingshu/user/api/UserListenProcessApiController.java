package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserListenProcessApiController {

    @Autowired
    private UserListenProcessService userListenProcessService;

    /**
     * 获取当前用户上次播放专辑声音记录
     *
     * @return
     */
    @GuiGuLogin
    @GetMapping("/userListenProcess/getLatelyTrack")
    public Result<Map<String, Long>> getLatelyTrack() {
        Long userId = AuthContextHolder.getUserId();
        return Result.ok(userListenProcessService.getLatelyTrack(userId));
    }

	/**
	 * 获取声音播放进度
	 * @param trackId
	 * @return
	 */
    @GuiGuLogin(required = false)
    @GetMapping("/userListenProcess/getTrackBreakSecond/{trackId}")
    public Result<BigDecimal> getTrackBreakSecond(@PathVariable Long trackId) {
        Long userId = AuthContextHolder.getUserId();
        if (userId != null) {
            BigDecimal breakSecond = userListenProcessService.getTrackBreakSecond(userId, trackId);
            return Result.ok(breakSecond);
        }
        return Result.ok();
    }


    /**
     * 更新当前用户收听声音播放进度
     * @param userListenProcessVo
     * @return
     */
    @GuiGuLogin(required = false)
    @PostMapping("/userListenProcess/updateListenProcess")
    public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo){
        Long userId = AuthContextHolder.getUserId();
            userListenProcessService.updateListenProcess(userId, userListenProcessVo);
        return Result.ok();
    }
}


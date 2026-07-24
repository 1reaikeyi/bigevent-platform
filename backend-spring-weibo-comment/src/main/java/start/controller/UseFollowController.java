package start.controller;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import common.ThreadLocalContext.ThreadLocalParam;
import common.result.Result;
import model.entity.UserFollow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import service.UserFollowService;

@RestController
@RequestMapping("/follow")
public class UseFollowController {
    @Autowired
    private UserFollowService userFollowService;
    @PostMapping("/{id}/{ifFollow}")
    public Result useFollow(@PathVariable("id") Long followUserId, @PathVariable Boolean ifFollow) {
        Long userId = ThreadLocalParam.getUserId();
        if(ifFollow){
            UserFollow userFollow = UserFollow.builder()
                    .userId(userId)
                    .followUserId(followUserId)
                    .build();
        userFollowService.save(userFollow);
        }
        if(!ifFollow){
            userFollowService.remove(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowUserId,userId)
                    .eq(UserFollow::getFollowUserId,followUserId));
        }
        return Result.success(followUserId + "::" + (BooleanUtil.isTrue(ifFollow) ? "关注" : "取关" ));
    }
    @GetMapping("/{id}")
    public Result getUserFollow(@PathVariable("id") Long followUserId) {
        Long userId = ThreadLocalParam.getUserId();
        UserFollow userFollow = userFollowService.getOne(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowUserId,userId)
                .eq(UserFollow::getFollowUserId,followUserId));
        return Result.success(userFollow == null ? "关注" : "取关" );
    }
}
